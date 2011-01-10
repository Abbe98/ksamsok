package se.raa.ksamsok.harvest;

/**
 * V�rdeb�na som inneh�ller information extraherad fr�n rdf.
 */
public class ExtractedInfo {

	private String identifier;
	private String nativeURL;

	public ExtractedInfo() {
	}

	/**
	 * @return identifierare
	 */
	public String getIdentifier() {
		return identifier;
	}
	/**
	 * S�tter identifierare
	 * @param identifier identifierare
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	/**
	 * Ger url till html-representation
	 * @return html-url
	 */
	public String getNativeURL() {
		return nativeURL;
	}
	/**
	 * S�tter html-url.
	 * @param nativeURL html-url
	 */
	public void setNativeURL(String nativeURL) {
		this.nativeURL = nativeURL;
	}
}
