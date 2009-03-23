package se.raa.ksamsok.harvest;

/**
 * Enkel v�rdeklass f�r att h�lla reda p� metadataprefix, namespace och schema
 * f�r OAI-PMH-noder.
 */
public class ServiceFormat {

	final String prefix;
	final String schema;
	final String namespace;

	ServiceFormat(String prefix, String namespace, String schema) {
		this.prefix = prefix;
		this.namespace = namespace;
		this.schema = schema;
	}

	/**
	 * H�mtar prefix.
	 * 
	 * @return prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * H�mtar schema-uri.
	 * 
	 * @return schema-uri
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * H�mtar namespace-uri.
	 * 
	 * @return namespace-uri
	 */
	public String getNamespace() {
		return namespace;
	}

}
