package se.raa.ksamsok.harvest;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.spatial.GMLDBWriter;
import se.raa.ksamsok.spatial.GMLInfoHolder;
import se.raa.ksamsok.spatial.GMLUtil;

/**
 * Handler f�r xml-parsning som lagrar poster i repositoryt och g�r commit med j�mna
 * mellanrum (f�r att inte oracle ska f� spunk, derby klarar det).
 * Formatet p� xml:en ska vara samma som f�r RawWrite, dvs i princip OAI-PMH med en
 * omslutande tagg.
 */
public class OAIPMHHandler extends DefaultHandler {

	// antal databasoperationer innan en commit g�rs
	private static final int XACT_LIMIT = 1000;

	// en generisk iso 8601-parser som klarar "alla" isoformat - egentligen ska vi bara st�dja tv� enl spec
	private static DateTimeFormatter isoDateTimeParser = ISODateTimeFormat.dateTimeParser();

	Connection c;
	GMLDBWriter gmlDBWriter;
	HarvestService service;
	ContentHelper contentHelper;
	String oaiURI;
	Timestamp datestamp;
	int mode = 0;
	private boolean deleteRecord;
	private StringBuffer buf = new StringBuffer();
	private HashMap<String,String> prefixMap = new HashMap<String, String>();
	private static final int NORMAL = 0;
	private static final int RECORD = 1;
	private static final int COPY = 2;
	
	private static final XMLOutputFactory xxmlf = XMLOutputFactory.newInstance();
	private XMLStreamWriter xxmlw;
	private StringWriter xsw;
	// antal genomf�rda databasoperationer, totalt och i nuvarande transaktion
	private int numDeleted = 0, numDeletedXact = 0;
	private int numInserted = 0, numInsertedXact = 0;
	private int numUpdated = 0, numUpdatedXact = 0;
	private ServiceMetadata sm;
	private String errorCode;
	private Timestamp ts;
	private StatusService ss;
	private PreparedStatement oai2uriPst;
	private PreparedStatement updatePst;
	private PreparedStatement deleteUpdatePst;
	private PreparedStatement insertPst;

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.OAIPMHHandler");


	public OAIPMHHandler(StatusService ss, HarvestService service, ContentHelper contentHelper,
			ServiceMetadata sm, Connection c, Timestamp ts) throws Exception {
		this.ss = ss;
		this.service = service;
		this.contentHelper = contentHelper;
		this.c = c;
		this.sm = sm;
		this.ts = ts;
		// f�rbered n�gra databas-statements som kommer anv�ndas frekvent
		this.oai2uriPst = c.prepareStatement("select uri from content where oaiuri = ?");
		this.updatePst = c.prepareStatement("update content set deleted = null, oaiuri = ?, " +
				"serviceId = ?, changed = ?, datestamp = ?, xmldata = ?, status = ? where uri = ?");
		// TODO: stoppa in xmldata = null nedan f�r att rensa on�digt gammalt postinneh�ll
		this.deleteUpdatePst = c.prepareStatement("update content set status = ?, " +
				"changed = ?, deleted = ?, datestamp = ? where serviceId = ? and oaiuri = ?");
		this.insertPst = c.prepareStatement("insert into content " +
				"(uri, oaiuri, serviceId, xmldata, changed, added, datestamp, status) " +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?)");
		gmlDBWriter = GMLUtil.getGMLDBWriter(service.getId(), c);
	}

	public void destroy() {
		DBUtil.closeDBResources(null, oai2uriPst, null);
		DBUtil.closeDBResources(null, updatePst, null);
		DBUtil.closeDBResources(null, deleteUpdatePst, null);
		DBUtil.closeDBResources(null, insertPst, null);
		oai2uriPst = null;
		updatePst = null;
		deleteUpdatePst = null;
		insertPst = null;
		if (gmlDBWriter != null) {
			gmlDBWriter.destroy();
			gmlDBWriter = null;
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		if (mode == COPY) {
			prefixMap.put(prefix, uri);
			try {
				xxmlw.setPrefix(prefix, uri);
			} catch (Exception e) {
				throw new SAXException(e);
			}

		}
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		switch (mode) {
		case NORMAL:
			if ("record".equals(name)) {
				// byt till "record-mode"
				mode = RECORD;
				deleteRecord = false;
				datestamp = null;
				oaiURI = null;
			} else if ("error".equals(name)) {
				errorCode = attributes.getValue("", "code");
			}
			break;
		case RECORD:
			if ("metadata".equals(name)) {
				// byt till "copy-mode"
				mode = COPY;
				try {
					xsw = new StringWriter();
					xxmlw = xxmlf.createXMLStreamWriter(xsw);
					// inget start doc, vi vill ha xml-fragment
					//xxmlw.writeStartDocument("UTF-8", "1.0");
				} catch (Exception e) {
					throw new SAXException(e);
				}
			} else if ("header".equals(name)) {
				// kontrollera om status s�ger deleted, d� ska denna post bort
				String status = attributes.getValue("", "status");
				if ("deleted".equals(status)) {
					if (!sm.canSendDeletes()) {
						throw new SAXException("Service is not supposed to handle deletes but did in fact send one!");
					}
					deleteRecord = true;
				}
			}
			break;
		case COPY:
			// i "copy-mode" kopiera hela taggen som den �r
			try {
				xxmlw.writeStartElement(uri, localName);
			} catch (Exception e) {
				throw new SAXException(e);
			}
			if (prefixMap.size() > 0) {
				for (String prefix: prefixMap.keySet()) {
					try {
						xxmlw.writeNamespace(prefix, prefixMap.get(prefix));
					} catch (Exception e) {
						throw new SAXException(e);
					}
				}
			}
			prefixMap.clear();
			try {
				if (attributes != null && attributes.getLength() > 0) {
					for (int i = 0; i < attributes.getLength(); ++i) {
						String aUri = attributes.getURI(i);
						String lName = attributes.getLocalName(i);
						String value = attributes.getValue(i);
						// Om ej r�tt metod anv�nds blir det ett exception
						if (aUri == null || "".equals(aUri)) {
							xxmlw.writeAttribute(lName, value);
						} else {
							xxmlw.writeAttribute(aUri, lName, value);
						}
					}
				}
			} catch (Exception e) {
				throw new SAXException(e);
			}
			break;
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		switch (mode) {
		case COPY:
			if ("metadata".equals(name)) {
				// tillbaks till "record-mode"
				mode = RECORD;
				try {
					// spara i databas
					//xxmlw.writeEndDocument(); // inget end doc, vi vill ha xml-fragment
					xxmlw.close();
					xxmlw = null;
					insertOrUpdateRecord(oaiURI, xsw.toString(), datestamp);
					xsw.close();
					xsw = null;
				} catch (Exception e) {
					throw new SAXException(e);
				}
			} else {
				// kopiera
				try {
					xxmlw.writeCharacters(buf.toString().trim());
					xxmlw.writeEndElement();
				} catch (Exception e) {
					throw new SAXException(e);
				}
			}
			// �terst�ll char-buff
			buf.setLength(0);
			break;
		case RECORD:
			if ("identifier".equals(name)) {
				// l�s ut v�rde f�r identifier
				oaiURI = buf.toString().trim();
			} else if ("datestamp".equals(name)) {
				// l�s ut och tolka v�rde f�r datum
				String datestampStr = buf.toString().trim();
				datestamp = parseDatestamp(datestampStr);
				if (datestamp == null) {
					datestamp = ts;
					ContentHelper.addProblemMessage("There was a problem parsing datestamp (" +
							datestampStr + ") for record, using 'now' instead");
				}
			} else if ("record".equals(name)) {
				// tillbaks till "normal-mode" 
				mode = NORMAL;
				if (deleteRecord) {
					// ta bort post nu om vi skulle g�ra det
					try {
						deleteRecord(oaiURI, datestamp);
					} catch (Exception e) {
						throw new SAXException(e);
					}
				}
			}
			// �terst�ll char-buff
			buf.setLength(0);
			break;
		case NORMAL:
			if ("error".equals(name)) {
				throw new SAXException("Error in request, code=" + errorCode + ", text: " + buf.toString().trim());
			}
			// �terst�ll char-buff
			buf.setLength(0);
			break;
		}
		
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		buf.append(ch, start, length);
	}

	/**
	 * Tar bort alla poster i repositoryt f�r tj�nsten vi jobbar med.
	 * I praktiken tas inget bort utan status-kolumnen s�tts till 1
	 * f�r att senare eventuellt ge att deleted-kolumnen s�tts icke null.
	 * 
	 * @throws Exception
	 */
	protected void deleteAllFromThisService() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Attempting to remove all records for service id: " + service.getId());
		}
		PreparedStatement pst = null;
		try {
			// OBS notera att geometrier inte tas bort h�r utan det g�rs inkrementellt
			// f�r varje post som dyker upp, eller i slutsteget f�r de som ej har behandlats
			pst = c.prepareStatement("update content set status = ? where serviceId = ?");
			pst.setInt(1, DBUtil.STATUS_PENDING);
			pst.setString(2, service.getId());
			long start = System.currentTimeMillis();
			int num = pst.executeUpdate();
			numDeletedXact += num;
			commitIfLimitReached(true);
			if (logger.isDebugEnabled()) {
				logger.debug("** Removed (updated status to pending) " + num + " records for service: " +
						service.getId() + " in " + ContentHelper.formatRunTime(System.currentTimeMillis() - start));
			}
		} finally {
			DBUtil.closeDBResources(null, pst, null);
		}
	}

	/**
	 * Tar bort post i repositoryt med inskickad OAI-identifierare (Obs != rdf-identifierare)
	 * @param oaiURI OAI-identifierare
	 * @throws Exception
	 */
	protected void deleteRecord(String oaiURI, Timestamp deletedAt) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Removing oaiURI=" + oaiURI + " from service with ID: " + service.getId());
		}
		ResultSet rs = null;
		try {
			// bort med ev spatialt data
			if (gmlDBWriter != null) {
				// h�mta ut uri:n d� oai-uri bara �r intern identifierare
				// select uri from content where oaiuri = ?
				oai2uriPst.setString(1, oaiURI);
				rs = oai2uriPst.executeQuery();
				if (rs.next()) {
					String uri = rs.getString("uri");
					gmlDBWriter.delete(uri);
				}
			}
			// update content set status = ?, changed = ?, deleted = ?, datestamp = ? where serviceId = ? and oaiuri = ?
			deleteUpdatePst.setInt(1, DBUtil.STATUS_NORMAL);
			deleteUpdatePst.setTimestamp(2, ts);
			deleteUpdatePst.setTimestamp(3, deletedAt);
			deleteUpdatePst.setTimestamp(4, deletedAt);
			deleteUpdatePst.setString(5, service.getId());
			deleteUpdatePst.setString(6, oaiURI);
			int num = deleteUpdatePst.executeUpdate();
			numDeletedXact += num;
			if (logger.isDebugEnabled()) {
				logger.debug("* Removed " + num + " number of oaiURI=" + oaiURI +
						" from service with ID: " + service.getId());
			}
			commitIfLimitReached();
		} finally {
			DBUtil.closeDBResources(rs, null, null);
		}
	}
	
	/**
	 * Stoppar in en ny post i repositoryt med angiven OAI-identifierare, identifierare och
	 * xml-inneh�ll.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param uri (rdf-)identifierare
	 * @param xmlContent xml-inneh�ll
	 * @param datestamp postens �ndringsdatum (fr�n oai-huvudet)
	 * @param gmlInfoHolder h�llare f�r geometrier mm
	 * @throws Exception
	 */
	protected void insertRecord(String oaiURI, String uri, String xmlContent,
			Timestamp datestamp, GMLInfoHolder gmlInfoHolder) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Entering data for oaiURI=" + oaiURI + ", uri=" +
					uri + " for service with ID: " + service.getId());
		}
		// insert into content
		//     (uri, oaiuri, serviceId, xmldata, changed, added, datestamp, status)
		//     values (?, ?, ?, ?, ?, ?, ?, ?)
		insertPst.setString(1, uri);
		insertPst.setString(2, oaiURI);
		insertPst.setString(3, service.getId());
		insertPst.setCharacterStream(4, new StringReader(xmlContent), xmlContent.length());
		insertPst.setTimestamp(5, ts);
		insertPst.setTimestamp(6, ts);
		insertPst.setTimestamp(7, datestamp);
		insertPst.setInt(8, DBUtil.STATUS_NORMAL);
		//insertPst.setString(9, nativeURL);
		insertPst.executeUpdate();
		// stoppa in ev spatialdata om vi har n�t
		if (gmlDBWriter != null && gmlInfoHolder != null && gmlInfoHolder.hasGeometries()) {
			gmlDBWriter.insert(gmlInfoHolder);
		}
		++numInsertedXact;
		if (logger.isDebugEnabled()) {
			logger.debug("* Entered data for oaiURI=" + oaiURI + ", uri=" +
					uri + " for service with ID: " + service.getId());
		}
		commitIfLimitReached();
	}

	/**
	 * Uppdaterar en post i repositoryt.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param uri (rdf-)identifierare
	 * @param xmlContent xml-inneh�ll
	 * @param datestamp postens �ndringsdatum (fr�n oai-huvudet)
	 * @param gmlInfoHolder h�llare f�r geometrier mm
	 * @throws Exception
	 */
	protected boolean updateRecord(String oaiURI, String uri, String xmlContent,
			Timestamp datestamp, GMLInfoHolder gmlInfoHolder) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Updated data for oaiURI=" + oaiURI + ", uri=" +
					uri + " for service with ID: " + service.getId());
		}
		// update content set oaiuri = ?, deleted = null,
		// serviceId = ?, changed = ?, datestamp = ?, xmldata = ?, status = ? where uri = ?
		updatePst.setString(1, oaiURI);
		updatePst.setString(2, service.getId());
		updatePst.setTimestamp(3, ts);
		updatePst.setTimestamp(4, datestamp);
		updatePst.setCharacterStream(5, new StringReader(xmlContent), xmlContent.length());
		updatePst.setInt(6, DBUtil.STATUS_NORMAL);
		//updatePst.setString(7, nativeURL);
		updatePst.setString(7, uri);
		boolean updated = updatePst.executeUpdate() > 0;
		if (updated) {
			// spara gml (obs, inget villkor p� att det finns geometrier d� det kanske
			// fanns gamla som nu ska tas bort)
			if (gmlDBWriter != null && gmlInfoHolder != null) {
				gmlDBWriter.update(gmlInfoHolder);
			}
			++numUpdatedXact;
			if (logger.isDebugEnabled()) {
				logger.debug("* Updated data for oaiURI=" + oaiURI + ", uri=" +
						uri + " for service with ID: " + service.getId());
			}
		}
		commitIfLimitReached();
		return updated;
	}

	/**
	 * Uppdaterar en befintlig eller stoppar in en ny post i repositoryt beroende p� om
	 * posten finns och om tj�nsten klarar av att skicka persistent deletes.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param xmlContent xml-inneh�ll
	 * @param datestamp postens �ndringsdatum (fr�n oai-huvudet)
	 * @throws Exception
	 */
	protected void insertOrUpdateRecord(String oaiURI, String xmlContent, Timestamp datestamp) throws Exception {
		String uri = null;
		GMLInfoHolder gmlih = null;
		String nativeURL = null;
		if (gmlDBWriter != null) {
			// om vi ska hantera spatiala data, skapa en datah�llare att fylla p�
			gmlih = new GMLInfoHolder();
		}
		try {
			ExtractedInfo info = contentHelper.extractInfo(xmlContent, gmlih);
			uri = info.getIdentifier();
			nativeURL = info.getNativeURL();

			if (uri == null) {
				return;
			}
			// g�r update och om ingen post uppdaterades stoppa in en (istf f�r att kolla om post finns f�rst)
			/*if (!updateRecord(oaiURI, uri, xmlContent, datestamp, gmlih, nativeURL)) {
				insertRecord(oaiURI, uri, xmlContent, datestamp, gmlih, nativeURL);
			}*/
			if (!updateRecord(oaiURI, uri, xmlContent, datestamp, gmlih)) {
				insertRecord(oaiURI, uri, xmlContent, datestamp, gmlih);
			}
		} catch (Exception e) {
			//logger.error("Error when storing " + (uri != null ? uri : oaiURI), e);
			ss.setStatusText(service, "Error when storing " + (uri != null ? uri : oaiURI) + " " + e.getMessage() + " --SKIPPING--");
			ss.containsErrors(service, true);
			logger.error("Error:159 problem when Storing " + service.getName() + " record " + (uri != null ? uri : oaiURI) + " " + e.getMessage());
			return;
		}
	}

	// g�r commit och r�kna up antalet lyckade �tg�rder
	private void commitIfLimitReached() throws Exception {
		commitIfLimitReached(false);
	}

	// g�r commit och r�kna up antalet lyckade �tg�rder, g�r alltid commit om argumentet �r true
	private void commitIfLimitReached(boolean forceCommit) throws Exception {
		int count = numInsertedXact + numUpdatedXact + numDeletedXact;
		if (forceCommit || count >= XACT_LIMIT) {
			commitAndUpdateCounters();
		}
	}

	/**
	 * Uppdaterar obehandlade poster baserat p� status och tj�nst.
	 * S�tter deleted och nollst�ller status.
	 * 
	 * @return antal databasf�r�ndringar
	 * @throws Exception
	 */
	protected int updateTmpStatus() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Update status for service with ID: " + service.getId());
		}
		final int BATCH_SIZE = 500;
		ss.setStatusText(service, "Attempting to update status and deleted column for pending records");
		int updated = 0;
		PreparedStatement updatePst = null;
		PreparedStatement selPst = null;
		ResultSet rs = null;
		try { 
			// h�mta kvarvarande poster under behandling och s�tt deras deleted
			// och ta bort deras geometrier i batchar
			String sql = DBUtil.fetchFirst(c,
					"select uri from content where serviceid = ? and status <> ?", BATCH_SIZE);
			selPst = c.prepareStatement(sql);
			selPst.setString(1, service.getId());
			selPst.setInt(2, DBUtil.STATUS_NORMAL);

			// beh�ll deleted om v�rdet finns, �ven f�r datestamp tas v�rdet fr�n deleted
			updatePst = c.prepareStatement("update content set changed = ?, deleted = coalesce(deleted, ?), " +
					"datestamp = coalesce(deleted, ?), status = ?, xmldata = null where uri = ?");
			updatePst.setTimestamp(1, ts);
			updatePst.setTimestamp(2, ts);
			updatePst.setTimestamp(3, ts);
			updatePst.setInt(4, DBUtil.STATUS_NORMAL);
			int totalRec = 0;
			int totalGeo = 0;
			long start = System.currentTimeMillis();
			String uri;
			int deltaRec = BATCH_SIZE;
			// n�r vi f�r mindre �n (och har behandlat dem) batch-storleken har vi n�tt slutet
			while (deltaRec == BATCH_SIZE) {
				deltaRec = 0;
				// kolla om vi ska avbryta
				ss.checkInterrupt(service);
				ss.setStatusText(service, "Have commited " + totalRec +
						" (plus " + totalGeo + " geo deletes) status and deleted column updates");
				rs = selPst.executeQuery();
				while (rs.next()) {
					uri = rs.getString("uri");
					updatePst.setString(5, uri);
					deltaRec += updatePst.executeUpdate();
					// uppdatera status/data f�r postens ev geometrier
					if (gmlDBWriter != null) {
						totalGeo += gmlDBWriter.delete(uri);
					}
				}
				// st�ng (och nollst�ll) rs f�r �teranv�ndning
				DBUtil.closeDBResources(rs, null, null);
				rs = null;
				DBUtil.commit(c);
				totalRec += deltaRec;
			}
			updated = totalRec + totalGeo;
			ss.setStatusTextAndLog(service, "Committed status and deleted column updates for " +
					totalRec + " records (plus " + totalGeo + " geo deletes) in " +
					ContentHelper.formatRunTime(System.currentTimeMillis() - start));
			if (logger.isDebugEnabled()) {
				logger.debug("Updated status och deleted column for " + totalRec + " records in " +
						ContentHelper.formatRunTime(System.currentTimeMillis() - start) +
						" for service " + service.getId());
			}
		} finally {
			DBUtil.closeDBResources(rs, selPst, null);
			DBUtil.closeDBResources(null, updatePst, null);
		}
		return updated;
	}

	/**
	 * �terst�ller status-kolumnen och d�rmed status till ursprungligt s� l�ngt det g�r.
	 * @throws Exception vid fel
	 */
	protected int resetTmpStatus() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Attempting to reset status for pending records for service " + service.getId());
		}
		ss.setStatusText(service, "Recovery: Attempting to reset status for pending records");
		int numAffected = 0;
		PreparedStatement pst = null;
		try {
			pst = c.prepareStatement("update content set status = ? where serviceId = ? and status <> ?");
			pst.setInt(1, DBUtil.STATUS_NORMAL);
			pst.setString(2, service.getId());
			pst.setInt(3, DBUtil.STATUS_NORMAL);
			long start = System.currentTimeMillis();
			numAffected = pst.executeUpdate();
			if (logger.isDebugEnabled()) {
				logger.debug("** Updated state for " + numAffected + " records for service: " + service.getId());
			}
			commitIfLimitReached(true);
			long durationMillis = System.currentTimeMillis() - start;
			ss.setStatusTextAndLog(service, "Recovery: The status for " + numAffected + " pending records was reset in " +
					ContentHelper.formatRunTime(durationMillis));
			if (logger.isDebugEnabled()) {
				logger.debug("Reset status for " + numAffected + " records in " +
						ContentHelper.formatRunTime(durationMillis) + " for service " +
						service.getId());
			}
		} finally {
			DBUtil.closeDBResources(null, pst, null);
		}
		return numAffected;
	}

	/**
	 * G�r utest�ende commit och ber�knar respektive �terst�ller antal uppdaterade
	 * total och f�r denna transaktion.
	 * 
	 * @throws Exception
	 */
	public void commitAndUpdateCounters() throws Exception {
		// kolla om vi ska avbryta, kastar exception
		ss.checkInterrupt(service);

		c.commit();
		numInserted += numInsertedXact;
		numUpdated += numUpdatedXact;
		numDeleted += numDeletedXact;
		numInsertedXact = 0;
		numUpdatedXact = 0;
		numDeletedXact = 0;

		String msg = "Committed (i/u/d " + numInserted +
			"/" + numUpdated + "/" + numDeleted + ") " +
			(numInserted + numUpdated + numDeleted) + " database changes";
		ss.setStatusText(service, msg);
		if (logger.isDebugEnabled()) {
			logger.debug(msg);
		}
	}

	/**
	 * H�mtar antal poster som stoppades in.
	 * 
	 * @return antal "nya" poster
	 */
	public int getInserted() {
		return numInserted;
	}

	/**
	 * H�mtar antalet borttagna poster.
	 * 
	 * @return antal borttagna poster
	 */
	public int getDeleted() {
		return numDeleted;
	}

	/**
	 * H�mtar antalet �ndrade poster.
	 * 
	 * @return antal �ndrade poster
	 */
	public int getUpdated() {
		return numUpdated;
	}

	/**
	 * Tolkar v�rdet som ett iso8601-datum (med ev tid).
	 * @param value str�ng att tolka
	 * @return tolkad timestamp eller null om v�rdet inte gick att tolka
	 */
	private Timestamp parseDatestamp(String value) {
		DateTime dateTime = null;
		try {
			dateTime = isoDateTimeParser.parseDateTime(value);
		} catch (Throwable t) {
			if (logger.isDebugEnabled()) {
				logger.debug("There was a problem parsing string '" +
						value + "' as an iso8601 date", t);
			}
		}
		return (dateTime != null ? new Timestamp(dateTime.getMillis()) : null);
		
	}

	public static void main(String[] args) {
		/* funkar inte riktigt fn
		Connection c = null;
		Statement st = null;
		FSDirectory dir = null;
		try {
			//File xmlFile = new File("d:/temp/oaipmh2.xml");
			//File xmlFile = new File("d:/temp/kthdiva_2694.xml");
			File xmlFile = new File("d:/temp/kthdiva.xml");
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			SAXParser p = spf.newSAXParser();
			Class.forName("org.hsqldb.jdbcDriver");
			c = DriverManager.getConnection("jdbc:hsqldb:file:d:/temp/harvestdb", "sa", "");
			dir = FSDirectory.getDirectory(new File("d:/temp/lucene-index/test"));

			OAIPMHHandler h = new OAIPMHHandler("test2", c);
			p.parse(xmlFile, h);
			DBBasedManagerImpl.commit(c);
			
			st = c.createStatement();
			st.execute("SHUTDOWN");
			Thread.sleep(3000);
			System.out.println("OK - " + h.serviceId + ": handlesDeleted: " + h.getSupportsDeleted() +
					", inserted: " + h.getInserted() +
					", deleted: " + h.getDeleted() +
					", updated: " + h.getUpdated());
		} catch (Exception e) {
			e.printStackTrace();
			DBBasedManagerImpl.rollback(c);
		} finally {
			DBBasedManagerImpl.closeDBResources(null, null, c);
		}
		
		IndexSearcher s = null;
		if (dir != null) {
			try {
				s = new IndexSearcher(dir);
				QueryParser p = new QueryParser("dc_desc", new StandardAnalyzer());
				String qs = (args.length == 0 ? "chine" : args[0]);
				Query q = p.parse(qs);
				System.err.println("S�ker med: " + qs);
				Hits hits = s.search(q);
				int antal = hits.length();
				System.out.println("Fick " + antal + " tr�ffar");
				Iterator iter = hits.iterator();
				int i = 0;
				while (iter.hasNext()) {
					Hit h = (Hit) iter.next();
					++i;
					org.apache.lucene.document.Document d = h.getDocument();
					System.out.println("## tr�ff " + i + "/" + antal + ", score: " + h.getScore() + " ##");
					System.out.println("creator: " + d.get("dc_creator"));
					System.out.println("desc: " + d.get("dc_desc"));
					System.out.println("");
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (s != null) {
					try {
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		*/
	}
}
