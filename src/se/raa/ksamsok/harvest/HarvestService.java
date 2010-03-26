package se.raa.ksamsok.harvest;

import java.util.Date;

/**
 * B�na f�r en sk�rdetj�nst.
 */
public interface HarvestService {

	/**
	 * Ger tj�nstens id.
	 * 
	 * @return ett id
	 */
	String getId();

	/**
	 * S�tter id.
	 * 
	 * @param serviceId id
	 */
	void setId(String serviceId);

	/**
	 * Ger cronstr�ng (k�rschema).
	 * 
	 * @return cronstr�ng
	 */
	String getCronString();

	/**
	 * S�tter cronstr�ng (k�rschema).
	 * 
	 * @param cronString cronstr�ng
	 */
	void setCronString(String cronString);

	/**
	 * Ger tj�nstens namn.
	 * 
	 * @return tj�nstens namn
	 */
	String getName();

	/**
	 * S�tter tj�nstens namn.
	 * 
	 * @param name namn
	 */
	void setName(String name);

	/**
	 * Ger sk�rde-URL.
	 * 
	 * @return url till sk�rd
	 */
	String getHarvestURL();

	/**
	 * S�tter sk�rde-URL.
	 * 
	 * @param harvestURL url
	 */
	void setHarvestURL(String harvestURL);

	/**
	 * Ger datum/tid f�r senast lyckade sk�rd.
	 * 
	 * @return datum
	 */
	Date getLastHarvestDate();

	/**
	 * S�tter datum/tid f�r senast lyckade sk�rd.
	 * 
	 * @param date datum
	 */
	void setLastHarvestDate(Date date);

	/**
	 * Ger datum/tid f�r f�rsta lyckade indexeringen.
	 * 
	 * @return datum
	 */
	Date getFirstIndexDate();

	/**
	 * S�tter datum/tid f�r f�rsta lyckade indexeringen.
	 * 
	 * @param date datum
	 */
	void setFirstIndexDate(Date date);

	/**
	 * Ger tj�nstetyp som talar om vad denna tj�nst klarar av att sk�rda.
	 * 
	 * @return tj�nstetyp
	 */
	String getServiceType();

	/**
	 * S�tter tj�nstetyp.
	 * 
	 * @param type typ
	 */
	void setServiceType(String type);

	/**
	 * Ger om man f�r denna tj�nst alltid ska sk�rda allt och aldrig f�rs�ka att g�ra
	 * en inkrementell sk�rd.
	 * 
	 * @return sant om man alltid ska sk�rda allt
	 */
	boolean getAlwaysHarvestEverything();

	/**
	 * S�tter v�rde f�r att alltid sk�rda allt.
	 * 
	 * @param value sant/falskt
	 */
	void setAlwaysHarvestEverything(boolean value);

	/**
	 * H�mtar namn p� det set (delm�ngd) som ska sk�rdas f�r denna tj�nst.
	 * 
	 * @return setnamn eller null
	 */
	String getHarvestSetSpec();

	/**
	 * S�tter namn p� set (delm�ngd) som ska anv�ndas vid sk�rd av denna tj�nst.
	 * 
	 * @param setSpec setnamn
	 */
	void setHarvestSetSpec(String setSpec);
	
	/**
	 * Returnerar kortnamn som anv�nds f�r att koppla tj�nst till organisation
	 * @return
	 */
	String getShortName();
	
	/**
	 * S�tter kortnamn f�r tj�nst
	 * @param shortName kortnamn
	 */
	void setShortName(String shortName);
}
