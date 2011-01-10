package se.raa.ksamsok.harvest;

import java.io.File;
import java.sql.Timestamp;
import java.util.Map;

/**
 * Tj�nst som hanterar lagring i repository.
 */
public interface HarvestRepositoryManager {

	/**
	 * G�r igenom en h�mtad OAI-PMH-sk�rd och lagrar den i repositoryt.
	 * 
	 * @param service tj�nst
	 * @param sm metadata om sk�rdetj�nsten
	 * @param xmlFile fil med OAI-PMH-xml
	 * @param ts timestamp
	 * @return sant om n�got uppdaterades
	 * @throws Exception
	 */
	boolean storeHarvest(HarvestService service, ServiceMetadata sm, File xmlFile, Timestamp ts) throws Exception;

	/**
	 * Uppdaterar index med data fr�n repositoryt.
	 * 
	 * @param service tj�nst
	 * @param ts timestamp att uppdatera fr�n, eller null f�r allt
	 * @throws Exception
	 */
	void updateIndex(HarvestService service, Timestamp ts) throws Exception;

	/**
	 * Uppdaterar index med data fr�n repositoryt. Om enclosingService
	 * �r skilt fr�n null kommer dess avbrottsstatus att kontrolleras samtidigt
	 * som tj�nstens.
	 * 
	 * @param service tj�nst
	 * @param ts timestamp att uppdatera fr�n, eller null f�r allt
	 * @param enclosingService tj�nst som styr k�rningen
	 * @throws Exception
	 */
	void updateIndex(HarvestService service, Timestamp ts, HarvestService enclosingService) throws Exception;

	/**
	 * Tar bort index-data f�r en tj�nst (g�mmer tj�nsten).
	 * 
	 * @param service tj�nst
	 * @throws Exception
	 */
	void deleteIndexData(HarvestService service) throws Exception;
	
	/**
	 * Tar bort all data i repositoryt f�r en tj�nst.
	 * 
	 * @param service tj�nst
	 * @throws Exception
	 */
	void deleteData(HarvestService service) throws Exception;

	/**
	 * H�mtar xml (rdf) f�r en inskickad uri som identifierar en post.
	 * 
	 * @param uri identifierare
	 * @return k-sams�ks-xml
	 * @throws Exception
	 */
	String getXMLData(String uri) throws Exception;

	/**
	 * Ger antalet poster i repositoryt f�r en tj�nst.
	 * 
	 * @param service tj�nst
	 * @return antal poster
	 * @throws Exception
	 */
	int getCount(HarvestService service) throws Exception;

	/**
	 * Ger antal poster i repositoryt per tj�nst.
	 * 
	 * @return antal poster per tj�nst nycklade p� tj�nste-id
	 * @throws Exception
	 */
	Map<String, Integer> getCounts() throws Exception;

	/**
	 * Ger spoolfil f�r en tj�nst.
	 * @param service tj�nst
	 * @return spoolfil
	 */
	File getSpoolFile(HarvestService service);
	
	/**
	 * Ger spoolfil f�r en tj�nst.
	 * @param service tj�nst
	 * @return spoolfil
	 */
	File getZipFile(HarvestService service);
	
	
	/**
	 * Packar upp gzipfil med tidigare sk�rd till spool-xml-dokument
	 */
	public void extractGZipToSpool(HarvestService service);

	/**
	 *  K�r optimering av indexet.
	 * @throws Exception vid fel
	 */
	void optimizeIndex() throws Exception;

	/**
	 * Rensar indexet - OBS mycket b�ttre att stoppa tomcat och rensa indexkatalogen.
	 * 
	 * @throws Exception
	 */
	void clearIndex() throws Exception;

	/**
	 * Ger spoolkatalogen.
	 * @return spoolkatalogen
	 */
	File getSpoolDir();
}
