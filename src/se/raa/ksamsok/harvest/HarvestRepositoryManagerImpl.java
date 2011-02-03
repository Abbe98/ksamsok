package se.raa.ksamsok.harvest;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.sql.DataSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.SamsokContentHelper;
import se.raa.ksamsok.spatial.GMLDBWriter;
import se.raa.ksamsok.spatial.GMLUtil;

public class HarvestRepositoryManagerImpl extends DBBasedManagerImpl implements HarvestRepositoryManager {

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.HarvestRepositoryManager");

	/** parameter som pekar ut var h�mtad xml ska mellanlagras, om ej satt anv�nds tempdir */
	protected static final String D_HARVEST_SPOOL_DIR = "samsok-harvest-spool-dir";

	private static final Object SYNC = new Object(); // anv�nds f�r att synka skrivningar till solr

	private static final ContentHelper samsokContentHelper = new SamsokContentHelper();

	// antal solr-dokument som skickas per batch, f�r f� -> mycket io, f�r m�nga -> mycket minne
	private static final int solrBatchSize = 50;
	// statusrapportering sker efter uppdatering av detta antal objekt
	private static final int statusReportBatchSize = 500;

	private SAXParserFactory spf;
	private StatusService ss;
	private File spoolDir;
	private SolrServer solr;

	public HarvestRepositoryManagerImpl(DataSource ds, StatusService ss, SolrServer solr) {
		super(ds);
		spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		this.ss = ss;
		spoolDir = new File(System.getProperty(D_HARVEST_SPOOL_DIR,
				System.getProperty("java.io.tmpdir")));
		if (!spoolDir.exists() || !spoolDir.isDirectory() || !spoolDir.canWrite()) {
			throw new RuntimeException("Kan inte l�sa spoolkatalog: " + spoolDir);
		}
		this.solr = solr;
	}

	@Override
	public boolean storeHarvest(HarvestService service, ServiceMetadata sm,
			File xmlFile, Timestamp ts) throws Exception {
		Connection c = null;
		String serviceId = null;
		OAIPMHHandler h = null;
		boolean updated;
		ContentHelper.initProblemMessages();
		try {
			serviceId = service.getId();
			c = ds.getConnection();
			SAXParser p = spf.newSAXParser();
			h = new OAIPMHHandler(ss, service, getContentHelper(service), sm, c, ts);
			if (!sm.handlesPersistentDeletes()) {
				// ta bort alla gamla poster om inte denna tj�nst klarar persistenta deletes
				// inget tas egentligen bort utan posternas status s�tts till pending
				h.deleteAllFromThisService();
			} else {
				// nollst�ll status f�r att b�ttre klara en inkrementell
				// sk�rd efter en misslyckad full sk�rd
				h.resetTmpStatus();
			}
			// g� igenom xml-filen och uppdatera databasen med filens poster
			p.parse(xmlFile, h);
			// g�r utest�ende commit och uppdatera r�knare inf�r statusuppdatering
			h.commitAndUpdateCounters();
			// uppdatera status och data f�r ber�rda poster samt nollst�ll temp-kolumner
			// OBS att commit g�rs i anropet nedan nu n�r den g�r batch-commits
			h.updateTmpStatus();
			// flagga f�r att n�got har uppdaterats
			updated = (h.getDeleted() > 0 || h.getInserted() > 0 || h.getUpdated() > 0);
			ss.setStatusTextAndLog(service, "Stored harvest (i/u/d " + h.getInserted() +
					"/" + h.getUpdated() + "/" + h.getDeleted() + ")");
		} catch (Throwable e) {
			DBUtil.rollback(c);
			if (h != null) {
				ss.setStatusTextAndLog(service, "Stored part of harvest before error (i/u/d " +
						h.getInserted() + "/" + h.getUpdated() + "/" + h.getDeleted() + ")");
				// f�rs�k �terst�ll status f�r poster till normaltillst�nd
				// h�ngslen och livrem d� status ocks� s�tts om i start av denna metod
				try {
					ss.setStatusText(service, "Attempting to reset status for records");
					h.resetTmpStatus();
				} catch (Throwable t) {
					ss.setStatusTextAndLog(service, "An error occured while attempting to " +
							"reset status after harvest storing failed: " +
							t.getMessage());
				}
			}
			logger.error(serviceId + ", error when storing harvest: " + e.getMessage());
			throw new Exception(e);
		} finally {
			DBUtil.closeDBResources(null, null, c);
			if (logger.isInfoEnabled() && h != null) {
				logger.info(serviceId +
						" (committed), deleted: " + h.getDeleted() +
						", new: " + h.getInserted() +
						", changed: " + h.getUpdated());
			}
			// frig�r resurser s�som prepared statements etc
			if (h != null) {
				h.destroy();
			}
		}
		// rapportera eventuella problemmeddelanden
		reportAndClearProblemMessages(service, "storing harvest");

		return updated;
	}

	@Override
	public void updateIndex(HarvestService service, Timestamp ts) throws Exception {
		updateIndex(service, ts, null);
	}

	@Override
	public void updateIndex(HarvestService service, Timestamp ts, HarvestService enclosingService) throws Exception {
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		String serviceId = null;
		synchronized (SYNC) { // en i taget som f�r k�ra index-write
			try {
				long start = System.currentTimeMillis();
				int count = getCount(service, ts);
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() + ", updating index (" + count + " records) - start");
				}
				serviceId = service.getId();
				c = ds.getConnection();
				String sql;
				if (ts != null) {
					sql = "select uri, deleted, added, xmldata from content where serviceId = ? and changed > ?";
				} else {
					sql = "select added, xmldata from content where serviceId = ? and deleted is null";
				}
				pst = c.prepareStatement(sql);
				pst.setString(1, serviceId);
				if (ts != null) {
					pst.setTimestamp(2, ts);
				}
				pst.setFetchSize(DBUtil.FETCH_SIZE);
				rs = pst.executeQuery();
				if (ts == null) {
					solr.deleteByQuery(ContentHelper.I_IX_SERVICE + ":" + serviceId);
				}
				//String oaiURI;
				String uri;
				String xmlContent;
				Timestamp added;
				int i = 0;
				int nonI = 0;
				int deleted = 0;
				ContentHelper helper = getContentHelper(service);
				ContentHelper.initProblemMessages();
				// TODO: man skulle kunna str�mma allt i en enda request, men jag tror inte man
				//       skulle tj�na s� mycket p� det
				//       se http://wiki.apache.org/solr/Solrj#Streaming_documents_for_an_update
				List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>(solrBatchSize);
				while (rs.next()) {
					//oaiURI = rs.getString("oaiuri");
					if (ts != null) {
						uri = rs.getString("uri");
						solr.deleteById(uri);
						if (rs.getTimestamp("deleted") != null) {
							++deleted;
							// om borttagen, g� till n�sta
							continue;
						}
					}
					xmlContent = rs.getString("xmldata");
					added = rs.getTimestamp("added");
					SolrInputDocument doc = helper.createSolrDocument(service, xmlContent, added);
					if (doc == null) {
						// inget dokument betyder att tj�nsten har skickat itemForIndexing=n
						++nonI;
						continue;
					}
					docs.add(doc);
					++i;
					if (i % solrBatchSize == 0) {
						// skicka batchen
						if (logger.isDebugEnabled()) {
							logger.debug("Skickar " + docs.size() + " dokument");
						}
						solr.add(docs);
						docs.clear();
					}
					if (i % statusReportBatchSize == 0) {
						ss.checkInterrupt(service);
						if (enclosingService != null) {
							ss.checkInterrupt(enclosingService);
						}
						long deltaMillis = System.currentTimeMillis() - start;
			            long aproxMillisLeft = ContentHelper.getRemainingRunTimeMillis(
			            		deltaMillis, i, count);
						ss.setStatusText(service, "Updated " + i +
								(ts != null ? " (and deleted " + deleted + ")" : "") +
								" of " + count + " fetched records in the index" +
		            			(aproxMillisLeft >= 0 ? " (estimated time remaining: " +
		            					ContentHelper.formatRunTime(aproxMillisLeft) + ")": ""));
						if (logger.isDebugEnabled()) {
							logger.debug(service.getId() + ", has updated " + i +
									(ts != null ? " (and deleted " + deleted + ")" : "") +
									" of " + count + " fetched records in lucene" +
			            			(aproxMillisLeft >= 0 ? " (estimated time remaining: " +
			            					ContentHelper.formatRunTime(aproxMillisLeft) + ")": ""));
						}
					}
				}
				if (docs.size() > 0) {
					// skicka sista del-batchen
					if (logger.isDebugEnabled()) {
						logger.debug("Skickar de sista " + docs.size() + " dokumenten");
					}
					solr.add(docs);
					docs.clear();
				}
				solr.commit();
				long durationMillis = (System.currentTimeMillis() - start);
				String runTime = ContentHelper.formatRunTime(durationMillis);
				String speed = ContentHelper.formatSpeedPerSec(count, durationMillis);
				ss.setStatusTextAndLog(service, "Updated index, " + i + " records (" + 
						(ts == null ? "delete + insert" : "updated incl " + deleted + " deleted") +
						(nonI > 0 ? ", itemForIndexing=n: " + nonI : "") +
						"), time: " + runTime + " (" + speed + ")");
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() +
							", updated index - done, " + (ts == null ?
									"first removed all and then inserted " :
									"updated incl " + deleted + " deleted ") + i +
							" records in the index, time: " +
							runTime + " (" + speed + ")");
				}
			} catch (Exception e) {
				try {
					solr.rollback();
				} catch (Exception e2) {
					logger.warn("Error when aborting for index", e2);
				}
				logger.error(serviceId + ", error when updating index", e);
				throw e;
			} finally {
				DBUtil.closeDBResources(rs, pst, c);
			}
			// rapportera eventuella problemmeddelanden
			reportAndClearProblemMessages(service, "indexing");
		}
	}

	@Override
	public void deleteIndexData(HarvestService service) throws Exception {
		String serviceId = null;
		synchronized (SYNC) { // en i taget som f�r k�ra index-write
			try {
				long start = System.currentTimeMillis();
				int count = getCount(service);
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() + ", removing index (" + count + " records) - start");
				}
				serviceId = service.getId();
				solr.deleteByQuery(ContentHelper.I_IX_SERVICE + ":" + serviceId);
				solr.commit();
				long durationMillis = (System.currentTimeMillis() - start);
				String runTime = ContentHelper.formatRunTime(durationMillis);
				String speed = ContentHelper.formatSpeedPerSec(count, durationMillis);
				ss.setStatusTextAndLog(service, "Removed index, " + count + " records" + 
						", time: " + runTime + " (" + speed + ")");
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() +
							", removed index - done, " + "removed " + count +
							" records in the index, time: " +
							runTime + " (" + speed + ")");
				}
			} catch (Exception e) {
				try {
					solr.rollback();
				} catch (Exception e2) {
					logger.warn("Error when aborting for index", e2);
				}
				logger.error(serviceId + ", error when updating index", e);
				throw e;
			}
		}
	}

	@Override
	public void deleteData(HarvestService service) throws Exception {
		Connection c = null;
		PreparedStatement pst = null;
		String serviceId = null;
		synchronized (SYNC) { // en i taget som f�r k�ra index-write
			try {
				serviceId = service.getId();
				c = ds.getConnection();
				// rensa f�rst allt vanligt inneh�ll
				pst = c.prepareStatement("delete from content where serviceId = ?");
				pst.setString(1, serviceId);
				pst.executeUpdate();
				// och rensa ev spatial-data f�r tj�nsten
				GMLDBWriter gmlDBWriter = GMLUtil.getGMLDBWriter(service.getId(), c);
				if (gmlDBWriter != null) {
					gmlDBWriter.deleteAllForService();
				}
				solr.deleteByQuery(ContentHelper.I_IX_SERVICE + ":" + serviceId);
				// commit f�rst f�r db d� den �r troligast att den sm�ller och sen solr
				DBUtil.commit(c);
				solr.commit();
				if (logger.isInfoEnabled()) {
					logger.info("Removed all records for " + serviceId);
				}
			} catch (Exception e) {
				DBUtil.rollback(c);
				try {
					solr.rollback();
				} catch (Exception e2) {
					logger.warn("Error when aborting for index", e2);
				}
				logger.error(serviceId + ", error at delete", e);
				throw e;
			} finally {
				DBUtil.closeDBResources(null, pst, c);
			}
		}
	}

	@Override
	public String getXMLData(String uri) throws Exception {
		String xmlContent = null;
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			pst = c.prepareStatement("select xmldata from content where uri = ? and deleted is null");
			pst.setString(1, uri);
			rs = pst.executeQuery();
			if (rs.next()) {
				xmlContent = rs.getString("xmldata");
			}
		} catch (Exception e) {
			logger.error("Error when fetching xml data for uri " + uri, e);
			logger.error(e.getMessage());
			throw e;
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		return xmlContent;
	}

	@Override
	public int getCount(HarvestService service) throws Exception {
		return getCount(service, null);
	}

	/**
	 * Ger antal poster i repositoryt f�r en tj�nst. Om ts skickas in ges antal poster
	 * som �ndrats efter ts.
	 * 
	 * @param service tj�nst
	 * @param ts timestamp
	 * @return antal poster
	 * @throws Exception
	 */
	protected int getCount(HarvestService service, Timestamp ts) throws Exception {
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		String serviceId = null;
		int count = 0;
		try {
			serviceId = service.getId();
			c = ds.getConnection();
			String sql = "select count(*) from content where serviceId = ? and deleted is null";
			if (ts != null) {
				sql += " and changed > ?";
			}
			pst = c.prepareStatement(sql);
			pst.setString(1, serviceId);
			if (ts != null) {
				pst.setTimestamp(2, ts);
			}
			rs = pst.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (Exception e) {
			logger.error(serviceId + ", error when fetching number of records", e);
			throw e;
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		return count;
	}

	@Override
	public Map<String, Integer> getCounts() throws Exception {
		Map<String, Integer> countMap = new HashMap<String, Integer>();
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		String serviceId = null;
		int count = 0;
		try {
			c = ds.getConnection();
			// lite special ist�llet f�r group by f�r att f� db-index att vara med och slippa full table scan...
			String sql = "select s.serviceId, (select count(*) from content where serviceId = s.serviceId and deleted is null) c from harvestservices s";
			pst = c.prepareStatement(sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				serviceId = rs.getString(1);
				count = rs.getInt(2);
				countMap.put(serviceId, count);
			}
		} catch (Exception e) {
			logger.error(serviceId + ", error when fetching number of records", e);
			throw e;
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		return countMap;
	}

	@Override
	public void optimizeIndex() throws Exception {
		synchronized (SYNC) { // en i taget som f�r k�ra index-write
			solr.optimize();
		}
	}

	@Override
	public void clearIndex() throws Exception {
		synchronized (SYNC) { // en i taget som f�r k�ra index-write
			solr.deleteByQuery("*:*");
			solr.commit();
		}
	}

	@Override
	public File getSpoolFile(HarvestService service) {
		return new File(spoolDir, service.getId() + "_.xml");
	}

	@Override
 	public File getZipFile(HarvestService service){
		return new File(getSpoolFile(service).getAbsolutePath() + ".gz");
	}

	@Override
	public void extractGZipToSpool(HarvestService service){
		OutputStream os = null;
		InputStream is = null;
		File outputFile = getSpoolFile(service);
		File inputFile = getZipFile(service);
		
		byte[] buf = new byte[8192];
		int c;
		try {
			is = new GZIPInputStream(new FileInputStream(inputFile));
			os = new BufferedOutputStream( new FileOutputStream(outputFile));
			while ((c = is.read(buf)) > 0) {
				os.write(buf, 0, c);
			}
			os.flush();
		}
		catch(IOException e){
			logger.error("error when unzipping harvest zip file", e);
		}
	 finally {
			closeStream(is);
			closeStream(os);
		}
	}
	
	/**
	 * Hj�lpmetod som st�nger en str�m.
	 * 
	 * @param stream str�m
	 */
	protected void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (Exception ignore) {}
		}
	}

	/**
	 * Rensa och rapportera ev problemmeddelanden till statusservicen och logga. 
	 * @param service tj�nst
	 * @param operation operation f�r loggmeddelande, kort format (eng)
	 */
	private void reportAndClearProblemMessages(HarvestService service, String operation) {
		// rapportera eventuella problemmeddelanden
		Map<String,Integer> problemMessages = ContentHelper.getAndClearProblemMessages();
		if (problemMessages != null && problemMessages.size() > 0) {
			Date nowDate = new Date();
			ss.setWarningTextAndLog(service, "Note! Problem(s) when " + operation, nowDate);
			logger.warn(service.getId() + ", got following problem(s) when " + operation + ": ");
			for (String uri: problemMessages.keySet()) {
				ss.setWarningTextAndLog(service, uri + " - " + problemMessages.get(uri) + " times", nowDate);
				logger.warn("  " + uri + " - " + problemMessages.get(uri) + " times");
			}
		}
	}

	/**
	 * H�mtar r�tt typ av ContentHelper f�r tj�nst.
	 * 
	 * @param service tj�nst
	 * @return ContentHelper
	 */
	protected static ContentHelper getContentHelper(HarvestService service) {
		// TODO: b�ttre kontroll
		if (service.getServiceType().endsWith("-SAMSOK")) {
			return samsokContentHelper;
		}
		logger.warn("ContentHelper f�r tj�nst kunde ej best�mmas, npe inkommande?");
		return null;
	}

	@Override
	public File getSpoolDir() {
		return spoolDir;
	}

}
