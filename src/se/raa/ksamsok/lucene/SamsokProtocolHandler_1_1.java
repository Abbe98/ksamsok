package se.raa.ksamsok.lucene;

import static se.raa.ksamsok.lucene.ContentHelper.IX_CHILD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTLABEL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTTYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FATHER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASCREATED;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASFORMERORCURRENTKEEPER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASFORMERORCURRENTOWNER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASRIGHTON;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ISFORMERORCURRENTKEEPEROF;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ISFORMERORCURRENTOWNEROF;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MOTHER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PARENT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_RIGHTHELDBY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_WASCREATEDBY;
import static se.raa.ksamsok.lucene.RDFUtil.extractSingleValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.context_pre;
import static se.raa.ksamsok.lucene.SamsokProtocol.uriPrefixKSamsok;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_bio_child;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_bio_father;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_bio_mother;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_bio_parent;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P105B_has_right_on;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P105F_right_held_by;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P49B_is_former_or_current_keeper_of;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P49F_has_former_or_current_keeper;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P51B_is_former_or_current_owner_of;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P51F_has_former_or_current_owner;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P94B_was_created_by;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P94F_has_created;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContextLabel;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContextType;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rSameAs;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jrdf.graph.Graph;
import org.jrdf.graph.SubjectNode;

public class SamsokProtocolHandler_1_1 extends SamsokProtocolHandler_0_TO_1_0 {

	private static final Logger classLogger = getClassLogger();

	// map med uri -> v�rde f�r indexering
	private static final Map<String,String> uriValues_1_1_TO;
	// relationsmap
	private static final Map<String, URI> relationsMap_1_1_TO;
	// kontextrelationsmap
	private static final Map<String, URI> contextRelationsMap_1_1_TO;

	// kontexttyper
	private static final Map<String, String> contextTypes_1_1_TO;
	// underkontexttyper
	private static final Map<String, String> subContextTypes_1_1_TO;

	static {
		//Object o = new String[][] { { "", "" } };
		// TODO: hantera nya kontexttyperna
		final Map<String,String> contextTypeValues = new HashMap<String,String>();
		RDFUtil.readURIValueResource(PATH + "contexttype_1.1.rdf", SamsokProtocol.uri_rContextLabel, contextTypeValues);
		contextTypes_1_1_TO = Collections.unmodifiableMap(contextTypeValues);

		// TODO: fixa ny fil/inneh�ll
		final Map<String,String> contextSubTypeValues = new HashMap<String,String>();
		RDFUtil.readURIValueResource(PATH + "contexttype_1.1.rdf", SamsokProtocol.uri_rContextLabel, contextSubTypeValues);
		subContextTypes_1_1_TO = Collections.unmodifiableMap(contextSubTypeValues);

		Map<String,String> values = new HashMap<String,String>();
		// l�s in uri-v�rden f�r uppslagning
		RDFUtil.readURIValueResource(PATH + "entitytype_1.1.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "subject.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "dataquality.rdf", SamsokProtocol.uri_r__Name, values);
		values.putAll(contextTypes_1_1_TO);
		values.putAll(subContextTypes_1_1_TO);
		//RDFUtil.readURIValueResource(PATH + "contexttype_1.1.rdf", SamsokProtocol.uri_rContextLabel, values);

		uriValues_1_1_TO = Collections.unmodifiableMap(values);

		// utg� fr�n tidigare version
		Map<String, URI> relMap = new HashMap<String, URI>(relationsMap_0_TO_1_0);
		// TODO: till�ta dessa �t det h�r h�llet? Kanske b�ttre att de bara finns i kontextet �t andra h�llet?
		// cidoc-crm
		// h�mta ut has created (01)
		relMap.put(IX_HASCREATED, uri_cidoc_P94F_has_created);
		// h�mta ut current or former owner of (01)
		relMap.put(IX_ISFORMERORCURRENTOWNEROF, uri_cidoc_P51B_is_former_or_current_owner_of);
		// h�mta ut has former or current keeper of (01)
		relMap.put(IX_ISFORMERORCURRENTKEEPEROF, uri_cidoc_P49B_is_former_or_current_keeper_of);
		// h�mta ut has right on (01)
		relMap.put(IX_HASRIGHTON, uri_cidoc_P105B_has_right_on);

		// bio
		// h�mta ut child (01)
		relMap.put(IX_CHILD, uri_bio_child);
		// h�mta ut parent (01)
		relMap.put(IX_PARENT, uri_bio_parent);
		// h�mta ut mother (01)
		relMap.put(IX_MOTHER, uri_bio_mother);
		// h�mta ut father (01)
		relMap.put(IX_FATHER, uri_bio_father);

		relationsMap_1_1_TO = Collections.unmodifiableMap(relMap);

		// kontextrelationerna
		Map<String, URI> contextRelMap = new HashMap<String, URI>();

		contextRelMap.put(IX_HASFORMERORCURRENTKEEPER, uri_cidoc_P49F_has_former_or_current_keeper);
		contextRelMap.put(IX_HASFORMERORCURRENTOWNER, uri_cidoc_P51F_has_former_or_current_owner);
		contextRelMap.put(IX_WASCREATEDBY, uri_cidoc_P94B_was_created_by);
		contextRelMap.put(IX_RIGHTHELDBY, uri_cidoc_P105F_right_held_by);

		contextRelationsMap_1_1_TO = Collections.unmodifiableMap(contextRelMap);
	}

	protected SamsokProtocolHandler_1_1(Graph graph, SubjectNode s) {
		super(graph, s);
	}

	@Override
	protected Map<String, String> getURIValues() {
		return uriValues_1_1_TO;
	}

	@Override
	protected Map<String, URI> getTopLevelRelationsMap() {
		return relationsMap_1_1_TO;
	}

	@Override
	public String getRelationTypeNameFromURI(String refUri) {
		// specialhantering av relationer
		String relationType;
		if (uri_rSameAs.toString().equals(refUri)) {
			relationType = ContentHelper.IX_SAMEAS;
		} else {
			relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, uriPrefixKSamsok));
			// TODO: fixa b�ttre/validera lite
			if (relationType == null) {
				// testa cidoc
				relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefix_cidoc_crm));
				if (relationType != null) {
					// strippa sifferdelen
					relationType = StringUtils.trimToNull(StringUtils.substringAfter(relationType, "."));
				} else {
					// bios
					relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefix_bio));
				}
			}
		}
		return relationType;
	}

	/**
	 * Extraherar och indexerar typinformation ur en kontextnod.
	 * Hanterar de index som g�ller f�r protokollversion 1.1, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param identifier identifierare
	 * @return kontexttyp, kortnamn
	 * @throws Exception vid fel
	 */
	@Override
	protected String extractContextTypeAndLabelInformation(SubjectNode cS, String identifier) throws Exception {

		// TODO: fixa till och l�gg till subtype och g�r s� att en satt label tas f�re en uppslagen + validering?

		// h�mta ut vilket kontext vi �r i
		String contextType;
		String contextTypeURI = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContextType), null);
		if (contextTypeURI != null) {
			String defaultLabel = contextTypes_1_1_TO.get(contextTypeURI);
			if (defaultLabel == null) {
				throw new Exception("The context type URI " + contextTypeURI + " is not valid for 1.1");
			}
			contextType = StringUtils.substringAfter(contextTypeURI, context_pre);
			if (StringUtils.isEmpty(contextType)) {
				throw new Exception("The context type URI " + contextTypeURI +
						" does not start with " + context_pre);
			}
			ip.setCurrent(IX_CONTEXTTYPE);
			ip.addToDoc(contextType);
			// ta f�rst en inskickad label, och annars defaultv�rdet
			String contextLabel = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContextLabel), ip);
			if (contextLabel == null) {
				contextLabel = defaultLabel;
			}
			ip.setCurrent(IX_CONTEXTLABEL);
			ip.addToDoc(contextLabel);
		} else {
			throw new Exception("No context type for node " + cS + " for " + identifier);
		}
		return contextType;
	}

	@Override
	protected void extractContextActorInformation(SubjectNode cS,
			String contextType, List<String> relations) throws Exception {
		super.extractContextActorInformation(cS, contextType, relations);
		// hantera relationer i kontexten
		extractContextRelationInformation(cS, contextType, relations);
	}

	protected void extractContextRelationInformation(SubjectNode cS, String contextType,
			List<String> relations) throws Exception {
		extractContextLevelRelations(cS, relations);
		// relationer
		// relationer, in i respektive index + i IX_RELURI
		//final String[] relIx = new String[] { null, IX_RELURI };
		// h�mta ut containsInformationAbout (0n)
		//relIx[0] = IX_CONTAINSINFORMATIONABOUT;
		// http://www.cidoc-crm.org/rdfs/cidoc-crm-english-label
		// http://www.cidoc-crm.org/rdfs/cidoc-crm
//		ip.setCurrent(IX_HASFORMERORCURRENTOWNER, contextType, false, IX_RELURI);
//		extractValue(graph, cS, getURIRef(elementFactory, uri_cidoc_P51F_has_former_or_current_owner), null, ip, relations);
//		ip.setCurrent(IX_HASFORMERORCURRENTKEEPER, contextType, false, IX_RELURI);
//		extractValue(graph, cS, getURIRef(elementFactory, uri_cidoc_P49F_has_former_or_current_keeper), null, ip, relations);
//		ip.setCurrent(IX_WASCREATEDBY, contextType, false, IX_RELURI);
//		extractValue(graph, cS, getURIRef(elementFactory, uri_cidoc_P94B_was_created_by), null, ip, relations);
//		ip.setCurrent(IX_RIGHTHELDBY, contextType, false, IX_RELURI);
//		extractValue(graph, cS, getURIRef(elementFactory, uri_cidoc_P105F_right_held_by), null, ip, relations);
	}

	/**
	 * Ger map med giltiga toppniv�relationer nycklat p� indexnamn.
	 * 
	 * �verlagra i subklasser vid behov.
	 * @return map med toppniv�relationer
	 */
	protected Map<String, URI> getContextLevelRelationsMap() {
		return contextRelationsMap_1_1_TO;
	}

	/**
	 * Extraherar och indexerar kontextniv�relationer som h�mtas via
	 * {@linkplain #getContextLevelRelationsMap()}.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param relations lista med relationer f�r specialrelationsindexet
	 * @throws Exception vid fel
	 */
	protected void extractContextLevelRelations(SubjectNode cS, List<String> relations) throws Exception {
		Map<String, URI> relationsMap = getContextLevelRelationsMap();
		extractRelationsFromNode(cS, relationsMap, relations);
	}

	@Override
	public Logger getLogger() {
		return classLogger;
	}
}
