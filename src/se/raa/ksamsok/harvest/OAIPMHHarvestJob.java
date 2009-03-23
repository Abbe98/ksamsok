package se.raa.ksamsok.harvest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import se.raa.ksamsok.lucene.ContentHelper;

import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.ListMetadataFormats;
import ORG.oclc.oai.harvester2.verb.ListRecords;
import ORG.oclc.oai.harvester2.verb.ListSets;

/**
 * Basklass f�r att hantera sk�rd mha OAI-PMH-protokollet.
 */
public class OAIPMHHarvestJob extends HarvestJob {

	public OAIPMHHarvestJob() {
		super();
	}

	@Override
	protected List<ServiceFormat> performGetFormats(HarvestService service)
			throws Exception {
		final List<ServiceFormat> list = new ArrayList<ServiceFormat>();
		ListMetadataFormats formats = new ListMetadataFormats(service.getHarvestURL());
		NodeList nodes = formats.getNodeList("/oai20:OAI-PMH/oai20:ListMetadataFormats/oai20:metadataFormat");
		ServiceFormat f;
		for (int i = 0; i < nodes.getLength(); ++i) {
			Node n = nodes.item(i);
			f = new ServiceFormat(formats.getSingleString(n, "oai20:metadataPrefix"),
					formats.getSingleString(n, "oai20:metadataNamespace"),
					formats.getSingleString(n, "oai20:schema"));
			list.add(f);
		}
		return list;
	}

	@Override
	protected List<String> performGetSets(HarvestService service)
			throws Exception {
		final List<String> list = new ArrayList<String>();
		ListSets sets = new ListSets(service.getHarvestURL());
		NodeList nodes = sets.getNodeList("/oai20:OAI-PMH/oai20:ListSets/oai20:set");
		String setSpec;
		for (int i = 0; i < nodes.getLength(); ++i) {
			Node n = nodes.item(i);
			setSpec = sets.getSingleString(n, "oai20:setSpec");
			list.add(setSpec);
		}
		return list;
	}

	@Override
	protected ServiceMetadata performIdentify(HarvestService service)
			throws Exception {
		Identify identify = new Identify(service.getHarvestURL());
		if (!"2.0".equals(identify.getProtocolVersion())) {
			throw new Exception("St�djer ej 2.0 utan " + identify.getProtocolVersion());
		}
		String granularity = identify.getSingleString("/oai20:OAI-PMH/oai20:Identify/oai20:granularity");
		if (granularity == null) {
			throw new Exception("Hittade inte granularity");
		}
		String deletedRecord = identify.getSingleString("/oai20:OAI-PMH/oai20:Identify/oai20:deletedRecord");
		if (deletedRecord == null) {
			throw new Exception("Hittade inte deletedRecord");
		}
		if (service.getAlwaysHarvestEverything()) {
			deletedRecord = ServiceMetadata.D_TRANSIENT;
			if (logger.isInfoEnabled()) {
				logger.info(service.getId() + ", P�tvingar deletedRecord=" + deletedRecord);
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info(service.getId() + ", deletedRecord=" + deletedRecord +
					", granularity=" + granularity);
		}
		return new ServiceMetadata(deletedRecord, granularity);
	}

	@Override
	protected int performGetRecords(HarvestService service, ServiceMetadata sm, ServiceFormat f,
			File storeTo, StatusService ss) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(service.getId() + " - H�mtar " + service.getHarvestURL() + ", senaste h�mtning: " + service.getLastHarvestDate());
		}
		OutputStream os = null;
		try {
			String fromDate = null;
			// bara om tj�nsten (permanent) hanterar info om borttagna
			if (sm.handlesPersistentDeletes() && service.getLastHarvestDate() != null) {
				// YYYY-MM-DDThh:mm:ssZ eller YYYY-MM-DD
				SimpleDateFormat df = new SimpleDateFormat(sm.getDateFormatString());
				df.setTimeZone(TimeZone.getTimeZone("UTC"));
				// TODO: �ka/minska p� datum eller tid d� from/tom �r inclusive? kanske bara
				//       intressant f�r datum
				//       kontrollera om timezone-anv�ndningen �r korrekt map datumgr�nser mm
				// url-kodning av timestamp beh�vs om tid �r inblandat ocks�
				String fromDateStr = df.format(service.getLastHarvestDate());
				fromDate = URLEncoder.encode(fromDateStr, "UTF-8");
				if (ss != null) {
					ss.setStatusTextAndLog(service, "H�mtar �ndringar sen senaste sk�rden (" +
							fromDateStr + ")");
				}
			}
			os = new BufferedOutputStream(new FileOutputStream(storeTo));
			return getRecords(service.getHarvestURL(), fromDate, null, f.getPrefix(),
					service.getHarvestSetSpec(), os, logger, ss, service);
		} finally {
			closeStream(os);
		}
	}

	/**
	 * Utf�r en h�mtning/sk�rd av data via oai-pmh-protokollet.
	 * 
	 * @param url sk�rde-url
	 * @param fromDate from eller null
	 * @param toDate tom eller null
	 * @param metadataPrefix metadataprefix
	 * @param setSpec set eller null
	 * @param os str�m att skriva till
	 * @param logger logger eller null
	 * @return antal h�mtade poster
	 * @throws Exception
	 */
	public int getRecords(String url, String fromDate, String toDate, String metadataPrefix,
			String setSpec, OutputStream os, Logger logger) throws Exception {
		return getRecords(url, fromDate, toDate, metadataPrefix, setSpec, os, logger, null, null);
	}

	/**
	 * Utf�r en h�mtning/sk�rd av data via oai-pmh-protokollet.
	 * 
	 * @param url sk�rde-url
	 * @param fromDate from eller null
	 * @param toDate tom eller null
	 * @param metadataPrefix metadataprefix
	 * @param setSpec set eller null
	 * @param os str�m att skriva till
	 * @param logger logger eller null
	 * @param ss statusservice eller null
	 * @param service tj�nst eller null
	 * @return antal h�mtade poster
	 * @throws Exception
	 */
	public int getRecords(String url, String fromDate, String toDate, String metadataPrefix,
			String setSpec, OutputStream os, Logger logger, StatusService ss, HarvestService service) throws Exception {
		int c = 0;
		int tryNum = 0;
		int completeListSize = -1;
		long start = System.currentTimeMillis();
		os.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
		os.write("<harvest>\n".getBytes("UTF-8"));
        ListRecords listRecords = null;
       	while (listRecords == null) {
       		++tryNum;
       		try {
        		listRecords = new ListRecords(url, fromDate, toDate, setSpec, metadataPrefix);
            } catch (IOException ioe) {
            	failedTry(tryNum, null, ioe, ss, service);
        	}
        }
        while (listRecords != null) {
			// kolla om vi ska avbryta
        	checkInterrupt(ss, service);

            NodeList errors = listRecords.getErrors();
            if (errors != null && errors.getLength() > 0) {
            	// inga records �r inte ett "fel" egentligen
            	if ("noRecordsMatch".equals(errors.item(0).getAttributes().getNamedItem("code").getNodeValue())) {
            		c = 0;
            		break;
            	}
            	if (logger != null) {
            		logger.error("Found " + errors.getLength() + " errors");
            	}
                throw new Exception(listRecords.toString());
            }
            os.write(listRecords.toString().getBytes("UTF-8"));
            os.write("\n".getBytes("UTF-8"));
            String resumptionToken = listRecords.getResumptionToken();
            // h�mta totala antalet (om det skickas) fast bara f�rsta g�ngen
            if (completeListSize < 0 && c == 0 && resumptionToken != null) {
            	try {
            		completeListSize = Integer.parseInt(listRecords.getSingleString(
            				"/oai20:OAI-PMH/oai20:ListRecords/oai20:resumptionToken/@completeListSize"));
            	} catch (Exception e) {
            		if (logger != null && logger.isDebugEnabled()) {
            			logger.debug("Fel vid h�mtande av completeListSize", e);
            		}
            	}
            }
            // r�kna antal
            c += Integer.parseInt(listRecords.getSingleString("count(/oai20:OAI-PMH/oai20:ListRecords/oai20:record)"));
            // ber�kna ungef�r kvarvarande h�mtningstid
            long deltaMillis = System.currentTimeMillis() - start;
            long aproxMillisLeft = ContentHelper.getRemainingRunTimeMillis(
            		deltaMillis, c, completeListSize);

            if (logger != null && logger.isDebugEnabled()) {
            	logger.debug((service != null ? service.getId() + ": " : "" ) +
            			"h�mtat " + c + (completeListSize > 0 ? "/" + completeListSize : "")
            			+ " records hittills p� " + ContentHelper.formatRunTime(deltaMillis) +
            			(aproxMillisLeft >= 0 ? " (ber�knad �terst�ende tid: " +
            					ContentHelper.formatRunTime(aproxMillisLeft) + ")": "") +
            			", resumptionToken: " + resumptionToken);
            }
			if (ss != null) {

				// vi uppdaterar bara status h�r, logg �r inte intressant f�r dessa
				ss.setStatusText(service, "H�mtar data till tempfil (h�mtat " + c +
						(completeListSize > 0 ? "/" + completeListSize : "") + " records)" +
						(aproxMillisLeft >= 0 ? ", ber�knad �terst�ende tid: " +
								ContentHelper.formatRunTime(aproxMillisLeft) : ""));
			}
            if (resumptionToken == null || resumptionToken.length() == 0) {
                listRecords = null;
            } else {
            	listRecords = null;
            	tryNum = 0;
            	while (listRecords == null) {
            		++tryNum;
	            	try {
	            		listRecords = new ListRecords(url, resumptionToken);
	            	} catch (IOException ioe) {
	            		failedTry(tryNum, resumptionToken, ioe, ss, service);
	            	}
            	}
            }
        }
        os.write("</harvest>\n".getBytes("UTF-8"));
        os.flush();
    	long durationMillis = System.currentTimeMillis() - start;
    	String msg = "H�mtade " + c + " records, tid: " +
    		ContentHelper.formatRunTime(durationMillis) +
    		" (" + ContentHelper.formatSpeedPerSec(c, durationMillis) + ")";
    	if (ss != null) {
    		ss.setStatusTextAndLog(service, msg);
    	}

        if (logger != null && logger.isInfoEnabled()) {
        	logger.info((service != null ? service.getId() + ": " : "" ) + msg);
        }
        return c;
	}

	// hantering av flera f�rs�k med viss tid mellan varje f�rs�k
	private void failedTry(int tryNum, String resumptionToken, IOException ioe, StatusService ss, HarvestService service) throws Exception {
    	// TODO: b�ttre konstanter/v�rden
    	//       skilj p� connect/error?
    	//       olika v�rden per tj�nst? smh/va �r helt tillst�ndsl�sa, oiacat inte 
    	final int maxTries = 3;
    	final int waitSecs = 120;
		if (tryNum >= maxTries) {
			throw new Exception("Problem att kontakta tj�nsten, gav upp efter " + maxTries + " f�rs�k" + (resumptionToken != null ? " med token: " +
					resumptionToken : ""), ioe);
		}
		// TODO: kan message vara s� pass stor s� att 4k-gr�nsen �verskrids och ger nedanst�ende databasfel?
		//       java.sql.SQLException: ORA-01461: can bind a LONG value only for insert into a LONG column
		//       i s� fall m�ste vi begr�nsa feltexten, se:
		//       http://vsadilovskiy.wordpress.com/2007/10/19/ora-01461-can-bind-a-long-value-only-for-insert-into-a-long-column/
		String msg = "Fick exception (" + ioe.getMessage() +
			"), v�ntar " + waitSecs + "s och f�rs�ker igen";
		logger.warn((service != null ? service.getId() + ": " : "" ) + msg);
		if (ss != null) {
    		ss.setStatusTextAndLog(service, msg);
		}
		// sov totalt waitSecs sekunder, men se till att vi �r snabba p� att avbryta
		for (int i = 0; i < waitSecs; ++i) {
			Thread.sleep(1000);
        	if (ss != null) {
        		checkInterrupt(ss, service);
        	}
		}

	}

	public static void main(String[] args) {
		Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.OAIPMHHarvestJob");
		FileOutputStream fos = null;
		OAIPMHHarvestJob j = new OAIPMHHarvestJob();
		long start = System.currentTimeMillis();
		try {
			/*
			fos = new FileOutputStream(new File("d:/temp/oaipmh.xml"));
			j.getRecords("http://alcme.oclc.org/oaicat/OAIHandler", null, null, "oai_dc", null, fos, logger);
			*/
			/*
			fos = new FileOutputStream(new File("d:/temp/kthdiva.xml"));
			j.getRecords("http://www.diva-portal.org/oai/kth/OAI", null, null, "oai_dc", null, fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/sudiva.xml"));
			j.getRecords("http://www.diva-portal.org/oai/su/OAI", null, null, "oai_dc", null, fos, logger);
			*/
			
			/* funkar ej ok - otroooligt seg i alla fall?
			fos = new FileOutputStream(new File("d:/temp/usc.xml"));
			j.getRecords("http://oai.usc.edu:8085/oaidp", null, null, "oai_dc", null, fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/brighton.xml"));
			j.getRecords("http://eprints.brighton.ac.uk/perl/oai2", null, null, "oai_dc", null, fos, logger,);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/nils1.xml"));
			j.getRecords("http://172.20.6.106:8081/oaicat/OAIHandler", null, null, "ksamsok-rdf", "fmi", fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/utvnod.xml"));
			j.getRecords("http://ux-ra-utvap.raa.se:8081/oaicat/OAIHandler", null, null, "ksamsok-rdf", "fmi", fos, logger);
			*/

			fos = new FileOutputStream(new File("d:/temp/utvnod_kmb.xml"));
			j.getRecords("http://ux-ra-utvap.raa.se:8081/oaicat/OAIHandler", null, null, "ksamsok-rdf", "kmb", fos, logger);

			/*
			fos = new FileOutputStream(new File("d:/temp/utvnod_big2.xml"));
			j.getRecords("http://ux-ra-utvap.raa.se:8081/oaicat/OAIHandler", null, null, "ksamsok-rdf", "fmi_big", fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/lokal_fmi2.xml"));
			j.getRecords("http://127.0.0.1:8080/oaicat/OAIHandler", null, null, "ksamsok-rdf", "fmi", fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/shm_context.xml"));
			j.getRecords("http://mis.historiska.se/OAICat/SHM/context", null, null, "ksamsok-rdf", null, fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/shm_media.xml"));
			j.getRecords("http://mis.historiska.se/OAICat/SHM/media", null, null, "ksamsok-rdf", null, fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/va_gnm_media.xml"));
			j.getRecords("http://www9.vgregion.se/vastarvet/OAICat/gnm/media", null, null, "ksamsok-rdf", null, fos, logger);
			*/

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception ignore) {}
			}
		}
		long durationMillis = System.currentTimeMillis() - start;
		System.out.println("Tid: " + ContentHelper.formatRunTime(durationMillis));
	}

}
