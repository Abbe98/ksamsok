package se.raa.ksamsok.lucene;

import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import se.raa.ksamsok.harvest.HarvestService;

public interface SamsokProtocolHandler {

	/**
	 * Skapar ett solr-dokument och fyller det med v�rden.
	 * 
	 * @param service tj�nst
	 * @param added n�r posten lades till i k-sams�k, om k�nt
	 * @param relations lista att fyllas p� med relationer [typ|uri] f�r specialindexet
	 * @param gmlGeometries lista att fyllas p� med gml
	 * @return solr-dokument
	 * @throws Exception vid fel
	 */
	SolrInputDocument handle(HarvestService service, Date added,
			List<String> relations, List<String> gmlGeometries) throws Exception;

	/**
	 * H�mtar klasspecifik logger.
	 * 
	 * @return logger
	 */
	Logger getLogger();
	
	/**
	 * Sl�r upp ett v�rde f�r en uri, tex l�nsnamn.
	 * 
	 * @param uri uri
	 * @return v�rde eller null
	 */
	String lookupURIValue(String uri);
}
