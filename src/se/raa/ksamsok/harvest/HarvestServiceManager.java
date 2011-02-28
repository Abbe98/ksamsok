package se.raa.ksamsok.harvest;

import java.util.Date;
import java.util.List;

import se.raa.ksamsok.harvest.StatusService.Step;

/**
 * Tj�nst som hanterar sk�rdetj�nster.
 */
public interface HarvestServiceManager {

	// nyckelv�rden f�r att n� managers i sk�rdejobb
	public static final String HSM_KEY = "hsm";
	public static final String HRM_KEY = "hrm";
	public static final String SS_KEY = "ss";

	// namn p� lucenespecifika interna tj�nster (eg bara cronjobb)
	// TODO: v�rdena kanske b�r �ndras d� det �r solr nu, men de ligger i db ocks�
	public static final String SERVICE_INDEX_OPTIMIZE = "_lucene_opt";
	public static final String SERVICE_INDEX_REINDEX = "_lucene_reindex";

	/**
	 * Ger lista med alla anv�ndarskapade tj�nster.
	 * 
	 * @return lista med tj�nster
	 * @throws Exception
	 */
	List<HarvestService>getServices() throws Exception;

	/**
	 * H�mtar b�na f�r tj�nst med inskickad id.
	 * 
	 * @param serviceId id
	 * @return tj�nst eller null
	 * @throws Exception
	 */
	HarvestService getService(String serviceId) throws Exception;

	/**
	 * Uppdaterar tj�nst i databasen.
	 * 
	 * @param service tj�nst
	 * @throws Exception
	 */
	void updateService(HarvestService service) throws Exception;

	/**
	 * Uppdaterar endast datumf�ltet (senaste lyckade sk�rd) f�r tj�nsten i databasen.
	 * 
	 * @param service tj�nst
	 * @param date datum
	 * @throws Exception
	 */
	void updateServiceDate(HarvestService service, Date date) throws Exception;

	/**
	 * Lagrar f�rsta g�ngen tj�nsten indexerades ok om inget v�rde finns.
	 * @param service tj�nst
	 */
	void storeFirstIndexDateIfNotSet(HarvestService service) throws Exception;

	/**
	 * Tar bort en tj�nst ur databasen. Tar �ven bort data ur repositoryt och ifr�n
	 * indexet.
	 * 
	 * @param service tj�nst
	 * @throws Exception
	 */
	void deleteService(HarvestService service) throws Exception;

	/**
	 * Skapar en ny tj�nst i databasen.
	 * 
	 * @param service tj�nst
	 * @throws Exception
	 */
	void createService(HarvestService service) throws Exception;

	/**
	 * Skapar en ny instans av en tj�nsteb�na.
	 * 
	 * @return ny tom instans
	 */
	HarvestService newServiceInstance();

	/**
	 * Triggar ig�ng en sk�rd, dvs en full k�rning av sk�rdejobbet f�r tj�nsten.
	 * 
	 * @param service tj�nst
	 * @throws Exception
	 */
	void triggerHarvest(HarvestService service) throws Exception;

	/**
	 * Triggar ig�ng en omindexering, dvs en delk�rning av sk�rdejobbet f�r tj�nsten.
	 * Omindexeringen g�rs utifr�n data i repositoryt.
	 * 
	 * @param service tj�nst
	 * @throws Exception
	 */
	void triggerReindex(HarvestService service) throws Exception;

	/**
	 * Triggar ig�ng en avindexering, dvs g�mmer tj�nsten utan att t�mma repositoryt.
	 * 
	 * @param service tj�nst
	 * @throws Exception
	 */
	void triggerRemoveindex(HarvestService service) throws Exception;

	/**
	 * Triggar ig�ng omindexering av alla tj�nster.
	 * 
	 * @throws Exception
	 */
	public void triggerReindexAll() throws Exception;

	/**
	 * Beg�r att en p�g�ende sk�rd ska avbrytas.
	 * 
	 * @param service tj�nst
	 * @return sant om cronscheduleraren tyckte att jobbet kunde avbrytas
	 * @throws Exception
	 */
	boolean interruptHarvest(HarvestService service) throws Exception;

	/**
	 * Beg�r att en p�g�ende omindexering av alla tj�nster ska avbrytas.
	 * 
	 * @return sant om cronscheduleraren tyckte att jobbet kunde avbrytas
	 * @throws Exception
	 */
	boolean interruptReindexAll() throws Exception;

	/**
	 * H�mtar senast k�nda jobbstatus.
	 * 
	 * @param service tj�nst
	 * @return senaste jobbstatus
	 */
	String getJobStatus(HarvestService service);

	/**
	 * Ger om tj�nstens jobb k�r fn.
	 * 
	 * @param service tj�nst
	 * @return sant om tj�nsten sk�rdas eller omindexeras
	 */
	boolean isRunning(HarvestService service);

	/**
	 * H�mtar senast k�nda jobbsteg.
	 * 
	 * @param service tj�nst
	 * @return senaste jobbsteg
	 */
	Step getJobStep(HarvestService service);

	/**
	 * H�mtar jobblogg f�r senaste k�rning efter omstart (bara fr�n minne).
	 * 
	 * @param service tj�nst
	 * @return lista med meddelanden
	 */
	List<String> getJobLog(HarvestService service);

	/**
	 * H�mtar jobblogg fr�n databas.
	 * 
	 * @param service tj�nst
	 * @return lista med meddelanden
	 */
	List<String> getJobLogHistory(HarvestService service);

	/**
	 * Ger om denna instans �r konfad att p�tvinga �r n�r tj�nster scheduleras
	 * @return sant om �r p�tvingas
	 */
	boolean isForceYear();
}
