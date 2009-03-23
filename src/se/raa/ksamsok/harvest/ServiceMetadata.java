package se.raa.ksamsok.harvest;

/**
 * Klass f�r att hantera metadata/capabilities f�r en oai-pmh-nod.
 */
public class ServiceMetadata {

	static final String D_NO = "no";
	static final String D_TRANSIENT = "transient";
	static final String D_PERSISTENT = "persistent";
	static final String G_DAY = "YYYY-MM-DD";
	private static final String G_DF_DAY = "yyyy-MM-dd";
	static final String G_FINE = "YYYY-MM-DDThh:mm:ssZ";
	private static final String G_DF_FINE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	
	final String deletedRecord;
	final String granularity;

	ServiceMetadata(String deletedRecord, String granularity) {
		this.deletedRecord = deletedRecord;
		this.granularity = granularity;
	}

	/**
	 * Ger om tj�nsten klarar att hantera persistenta deletes (kr�vs om inkrementell
	 * sk�rd ska kunna anv�ndas)
	 * 
	 * @return sant om persistenta deletes hanteras
	 */
	boolean handlesPersistentDeletes() {
		return D_PERSISTENT.equals(deletedRecord);
	}

	/**
	 * Ger om tj�nsten kan skicka deletes.
	 * 
	 * @return sant om tj�nsten kan skicka deletes
	 */
	boolean canSendDeletes() {
		return !D_NO.equals(deletedRecord);
	}

	/**
	 * Ger datum/tid-granularitet.
	 * 
	 * @return datum/tid-granularitet
	 */
	String getGranularity() {
		return granularity;
	}

	/**
	 * Ger datumformatstr�ng f�r aktuell tj�nsts datum/tid-granularitet.
	 * 
	 * @return datumformatstr�ng
	 */
	String getDateFormatString() {
		if (G_FINE.equals(granularity)) {
			return G_DF_FINE;
		}
		return G_DF_DAY;
	}
}
