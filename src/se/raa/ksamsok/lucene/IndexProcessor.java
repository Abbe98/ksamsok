package se.raa.ksamsok.lucene;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;

/**
 * Klass som hanterar indexprocessning (�vers�ttning av v�rden, hur de
 * lagras av lucene etc).
 */
class IndexProcessor {
	final Map<String,String> uriValues;
	final SolrInputDocument doc;
	String[] indexNames;
	boolean lookupURI;
	RelationToIndexMapper relationHandler;
	IndexProcessor(SolrInputDocument doc, Map<String,String> uriValues, RelationToIndexMapper relationHandler) {
		this.doc = doc;
		this.uriValues = uriValues;
		this.relationHandler = relationHandler;
	}

	/**
	 * S�tter vilket index vi jobbar med fn, och ev ocks� ett prefix f�r att hantera
	 * kontext-index samt om uris ska sl�s upp. V�rdet i indexet kommer ocks� lagras i ett kontext-index.
	 * 
	 * @param indexName indexnamn
	 * @param contextPrefix kontext-prefix eller null
	 * @param lookupURI om uriv�rde ska sl�s upp
	 * @param extraIndexName ev extra index v�rdet ska in i, eller null
	 */
	void setCurrent(String indexName, String contextPrefix, boolean lookupURI, String extraIndexName) {
		if (contextPrefix != null) {
			if (extraIndexName != null) {
				setCurrent(new String[] {indexName, contextPrefix + "_" + indexName, extraIndexName }, lookupURI);
			} else {
				setCurrent(new String[] {indexName, contextPrefix + "_" + indexName, }, lookupURI);
			}
		} else {
			if (extraIndexName != null) {
				setCurrent(new String[] { indexName, extraIndexName }, lookupURI);
			} else {
				setCurrent(indexName, lookupURI);
			}
		}
	}

	/**
	 * S�tter vilket index vi jobbar med fn, och ev ocks� ett prefix f�r att hantera
	 * kontext-index. V�rdet i indexet kommer ocks� lagras i ett kontext-index.
	 * 
	 * @param indexName indexnamn
	 * @param contextPrefix kontext-prefix eller null
	 */
	void setCurrent(String indexName, String contextPrefix) {
		setCurrent(indexName, contextPrefix, true, null);
	}

	/**
	 * S�tter vilket index vi jobbar med fn.
	 * 
	 * @param indexName indexnamn
	 */
	void setCurrent(String indexName) {
		setCurrent(indexName, true);
	}

	/**
	 * S�tter vilket index vi jobbar med fn, hur/om det ska lagras av lucene och
	 * om uri-v�rden ska sl�s upp.
	 * 
	 * @param indexName indexnamn
	 * @param lookupURI om uriv�rde ska sl�s upp
	 */
	void setCurrent(String indexName, boolean lookupURI) {
		setCurrent(new String[] {indexName }, lookupURI);
	}

	/**
	 * S�tter vilka index vi jobbar med fn, hur/om dessa ska lagras av lucene, vilken
	 * typ (lucene) av index dessa �r och om uri-v�rden ska sl�s upp.
	 * 
	 * @param indexNames indexnamn
	 * @param lookupURI om v�rden ska sl�s upp
	 */
	void setCurrent(String[] indexNames, boolean lookupURI) {
		this.indexNames = indexNames;
		this.lookupURI = lookupURI;
	}

	/**
	 * L�gger till v�rdet till lucenedokumentet f�r aktuellt index. V�rdet l�ggs till
	 * f�r b�de huvudindexet och ev extraindex satta med tex
	 * {@linkplain #setCurrent(String[], boolean)}.
	 * 
	 * @param value v�rde
	 */
	void addToDoc(String value) {
		for (int i = 0; i < indexNames.length; ++i) {
			addToDoc(indexNames[i], value);
		}
	}

	/**
	 * Ger om uri-v�rden ska sl�s upp f�r aktuellt index.
	 * 
	 * @return sant om uri-v�rden ska sl�s upp
	 */
	boolean translateURI() {
		return lookupURI;
	}

	/**
	 * Sl�r ev upp ett uriv�rde och hanterar speciallistan med relationer. 
	 * 
	 * @param uri uri
	 * @param relations speciallista med relationer som fylls p�, eller null
	 * @param refUri relations-uri, eller null
	 * @return uppslaget v�rde, eller uri:n
	 * @throws Exception vid fel
	 */
	public String lookupAndHandleURIValue(String uri, List<String> relations, String refUri) throws Exception {
		String value;
		String lookedUpValue;
		// se om vi ska f�rs�ka ers�tta uri:n med en uppslagen text
		if (translateURI() && (lookedUpValue = lookupURIValue(uri)) != null) {
			value = lookedUpValue;
		} else {
			value = StringUtils.trimToNull(uri);
		}
		if (value != null && refUri != null && relations != null) {
			String relationType = relationHandler.getRelationTypeNameFromURI(refUri);
			if (relationType == null) {//!relationHandler.handleURIs(refUri, uri, relations)) {
				throw new Exception("Ok�nd/ej hanterad relation? B�rjar ej med k�nt prefix: " + refUri);
			}
			// kontrollera indexnamnet (relationstypen �r i praktiken samma som indexnamnet)
			if (!ContentHelper.indexExists(relationType)) {
				throw new Exception("Relationen med uri " + refUri + " �versattes till " +
						relationType + ", vilket inte �r ett index");
			}
			relations.add(relationType + "|" + value);
			// TODO: refUri/uri, �r det ok/bra? fixa �verlagring av dessa f�r olika protokollversioner
			//       eller n�t s�nt...
			// specialhantering av relationer
//			String relationType;
//			if (SamsokProtocol.uri_rSameAs.toString().equals(refUri)) {
//				relationType = "sameAs";
//			} else {
//				relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefixKSamsok));
//				// TODO: fixa b�ttre
//				if (relationType == null) {
//					// testa cidoc
//					relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefix_cidoc_crm));
//					if (relationType != null) {
//						// strippa sifferdelen
//						relationType = StringUtils.trimToNull(StringUtils.substringAfter(relationType, "."));
//					} else {
//						// bios
//						relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefix_bio));
//					}
//				}
//				if (relationType == null) {
//					throw new Exception("Ok�nd relation? B�rjar ej med k�nt prefix: " + refUri);
//				}
//			}
//			relations.add(relationType + "|" + value);
		}
		return value;
	}

	// f�rs�ker sl� upp uri-v�rde mha m�ngden inl�sta v�rden
	// f�r geografi-uri:s tas fn bara koden (url-suffixet)
	// om ett v�rde inte hittas lagras det i "problem"-loggen
	private String lookupURIValue(String uri) {
		String value = uriValues.get(uri);
		// TODO: padda med nollor eller strippa de med inledande nollor?
		//       det kommer ju garanterat bli fel i n�gon �nda... :)
		//       UPDATE: ser ut som om det �r strippa nollor som g�ller d�
		//       cql-parsern(?) verkar tolka saker som siffror om de inte quotas
		//       med "", ex "01"
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_county_pre, true);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_municipality_pre, true);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_province_pre);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_parish_pre, true);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_country_pre);
		if (value != null) {
			return value;
		}
		// l�gg in i thread local som ett problem
		ContentHelper.addProblemMessage("No value for " + uri);
		return null;
	}

	// hj�lpmetod som tar ut suffixet ur str�ngen om den startar med inskickad startstr�ng
	private static String restIfStartsWith(String str, String start) {
		return restIfStartsWith(str, start, false);
	}

	// hj�lpmetod som tar ut suffixet ur str�ngen om den startar med inskickad startstr�ng
	// och f�rs�ker tolka v�rdet som ett heltal om asNumber �r sant
	private static String restIfStartsWith(String str, String start, boolean asNumber) {
		String value = null;
		if (str.startsWith(start)) {
			value = str.substring(start.length());
			if (asNumber) {
				try {
					value = Long.valueOf(value).toString();
				} catch (NumberFormatException nfe) {
					ContentHelper.addProblemMessage("Could not interpret the end of " + str + " (" + value + ") as a digit");
				}
			}
		}
		return value;
	}

	// l�gger till ett index till solr-dokumentet
	boolean addToDoc(String fieldName, String value) {
		String trimmedValue = StringUtils.trimToNull(value);
		if (trimmedValue != null) {
			/*
			if (isToLowerCaseIndex(fieldName)) {
				trimmedValue = trimmedValue.toLowerCase();
			} else */
			// TODO: g�ra detta p� solr-sidan?
			if (ContentHelper.isISO8601DateYearIndex(fieldName)) {
				trimmedValue = TimeUtil.parseYearFromISO8601DateAndTransform(trimmedValue);
				if (trimmedValue == null) {
					ContentHelper.addProblemMessage("Could not interpret date value according to ISO8601 for field: " +
							fieldName + " (" + value + ")");
				}
			}
			if (trimmedValue != null) {
				doc.addField(fieldName, trimmedValue);
			}
		}
		return trimmedValue != null;
	}

}