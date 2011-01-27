package se.raa.ksamsok.harvest;

/**
 * Enkel b�na som representerar en loggh�ndelse.
 */
public class LogEvent {

	// koder f�r meddelanden som sparas i db
	public static final int EVENT_INFO = 0;
	public static final int EVENT_ERROR = 1;
	public static final int EVENT_WARNING = 2;

	private String serviceId;
	private int eventType;
	private String eventTime;
	private String message;

	/**
	 * Skapa ny instans.
	 * @param serviceId tj�nste-id
	 * @param eventType h�ndelsetyp
	 * @param eventTime h�ndelsetid i formaterat str�ngformat
	 * @param message meddelandetext
	 */
	public LogEvent(String serviceId, int eventType, String eventTime, String message) {
		this.serviceId = serviceId;
		this.eventType = eventType;
		this.eventTime = eventTime;
		this.message = message;
	}

	/**
	 * Getter f�r tj�nste-id
	 * @return tj�nste-id
	 */
	public String getServiceId() {
		return serviceId;
	}

	/**
	 * Getter f�r h�ndelsetyp.
	 * @return h�ndelsetyp.
	 */
	public int getEventType() {
		return eventType;
	}

	/**
	 * Getter f�r h�ndelsetid.
	 * @return h�ndelsetid
	 */
	public String getEventTime() {
		return eventTime;
	}

	/**
	 * Getter f�r meddelande.
	 * @return loggmeddelande
	 */
	public String getMessage() {
		return message;
	}
}
