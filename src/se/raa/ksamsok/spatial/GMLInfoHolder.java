package se.raa.ksamsok.spatial;

import java.util.Collection;

/**
 * Enkel datah�llare f�r spatial-data i form av gml. En typisk post har
 * en identifierar-URI, en namn och noll eller flera geometrier.
 */
public class GMLInfoHolder {

	private String identifier;
	private String name;
	private Collection<String> gmlGeometries;

	/**
	 * Skapar ny tom instans.
	 */
	public GMLInfoHolder() {};

	/**
	 * Ger sant om instansen inneh�ller geometrier
	 * @return sant m�ngd med geometrier finns 
	 */
	public boolean hasGeometries() {
		return gmlGeometries != null && gmlGeometries.size() > 0;
	}

	/**
	 * Ger satt identifier.
	 * @return identifierar-URI, eller null
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * S�tter identifier.
	 * @param identifier identifierar-URI
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Ger satt namn f�r denna identifier.
	 * @return namn, eller null
	 */
	public String getName() {
		return name;
	}

	/**
	 * S�tter namn f�r denna identifier.
	 * @param name namn
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Ger m�ngd med geometrier i form av gml-str�ngar.
	 * @return m�ngd med geometrier, eller null
	 */
	public Collection<String> getGmlGeometries() {
		return gmlGeometries;
	}

	/**
	 * S�tter m�ngd med geometrier.
	 * @param gmlGeometries m�ngd med geometrier i form av gml-str�ngar
	 */
	public void setGmlGeometries(Collection<String> gmlGeometries) {
		this.gmlGeometries = gmlGeometries;
	}

}
