package se.raa.ksamsok.lucene;

import static se.raa.ksamsok.lucene.ContentHelper.IX_CHILD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTLABEL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTTYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FATHER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASFORMERORCURRENTKEEPER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASFORMERORCURRENTOWNER;
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
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P105F_right_held_by;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P49F_has_former_or_current_keeper;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P51F_has_former_or_current_owner;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P94B_was_created_by;
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
	// superkontexttyper
	private static final Map<String, String> superContextTypes_1_1_TO;

	static {
		final Map<String,String> contextTypeValues = new HashMap<String,String>();
		RDFUtil.readURIValueResource(PATH + "contexttype_1.1.rdf", SamsokProtocol.uri_rContextLabel, contextTypeValues);
		contextTypes_1_1_TO = Collections.unmodifiableMap(contextTypeValues);

		// kontextsupertyper
		final Map<String,String> contextSuperTypeValues = new HashMap<String,String>();
		RDFUtil.readURIValueResource(PATH + "contextsupertype_1.1.rdf", SamsokProtocol.uri_r__Name, contextSuperTypeValues);
		superContextTypes_1_1_TO = Collections.unmodifiableMap(contextSuperTypeValues);

		Map<String,String> values = new HashMap<String,String>();
		// l�s in uri-v�rden f�r uppslagning
		RDFUtil.readURIValueResource(PATH + "entitytype_1.1.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "entitysupertype_1.1.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "subject.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "dataquality.rdf", SamsokProtocol.uri_r__Name, values);
		values.putAll(contextTypes_1_1_TO);
		values.putAll(superContextTypes_1_1_TO);

		uriValues_1_1_TO = Collections.unmodifiableMap(values);

		// utg� fr�n tidigare version
		Map<String, URI> relMap = new HashMap<String, URI>(relationsMap_0_TO_1_0);

		// h�mta ut is mentioned by (0M)
		relMap.put(ContentHelper.IX_ISMENTIONEDBY, SamsokProtocol.uri_rIsMentionedBy);
		// h�mta ut mentions (0M)
		relMap.put(ContentHelper.IX_MENTIONS, SamsokProtocol.uri_rMentions);

		// h�mta ut current or former owner of (0M)
		relMap.put(ContentHelper.IX_ISCURRENTORFORMERMEMBEROF, SamsokProtocol.uri_cidoc_P107B_is_current_or_former_member_of);
		// h�mta ut has former or current keeper of (0M)
		relMap.put(ContentHelper.IX_HASCURRENTORFORMERMEMBER, SamsokProtocol.uri_cidoc_P107F_has_current_or_former_member);

		// h�mta ut had participant (0M)
		relMap.put(ContentHelper.IX_HADPARTICIPANT, SamsokProtocol.uri_cidoc_P11F_had_participant);
		// h�mta ut participated in (0M)
		relMap.put(ContentHelper.IX_PARTICIPATEDIN, SamsokProtocol.uri_cidoc_P11B_participated_in);
		// h�mta ut was present at (0M)
		relMap.put(ContentHelper.IX_WASPRESENTAT, SamsokProtocol.uri_cidoc_P12B_was_present_at);
		// h�mta ut occured in the presence of (0M)
		relMap.put(ContentHelper.IX_OCCUREDINTHEPRESENCEOF, SamsokProtocol.uri_cidoc_P12F_occurred_in_the_presence_of);

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
		contextRelMap.put(ContentHelper.IX_CLIENT, SamsokProtocol.uri_rClient);
		contextRelMap.put(ContentHelper.IX_AUTHOR, SamsokProtocol.uri_rAuthor);
		contextRelMap.put(ContentHelper.IX_ARCHITECT, SamsokProtocol.uri_rArchitect);
		contextRelMap.put(ContentHelper.IX_INVENTOR, SamsokProtocol.uri_rInventor);
		contextRelMap.put(ContentHelper.IX_SCENOGRAPHER, SamsokProtocol.uri_rScenographer);
		contextRelMap.put(ContentHelper.IX_DESIGNER, SamsokProtocol.uri_rDesigner);
		contextRelMap.put(ContentHelper.IX_PRODUCER, SamsokProtocol.uri_rProducer);
		contextRelMap.put(ContentHelper.IX_ORGANIZER, SamsokProtocol.uri_rOrganizer);
		contextRelMap.put(ContentHelper.IX_DIRECTOR, SamsokProtocol.uri_rDirector);
		contextRelMap.put(ContentHelper.IX_PHOTOGRAPHER, SamsokProtocol.uri_rPhotographer);
		contextRelMap.put(ContentHelper.IX_PAINTER, SamsokProtocol.uri_rPainter);
		contextRelMap.put(ContentHelper.IX_BUILDER, SamsokProtocol.uri_rBuilder);
		contextRelMap.put(ContentHelper.IX_MASTERBUILDER, SamsokProtocol.uri_rMasterBuilder);
		contextRelMap.put(ContentHelper.IX_CONSTRUCTIONCLIENT, SamsokProtocol.uri_rConstructionClient);
		contextRelMap.put(ContentHelper.IX_ENGRAVER, SamsokProtocol.uri_rEngraver);
		contextRelMap.put(ContentHelper.IX_MINTMASTER, SamsokProtocol.uri_rMintmaster);
		contextRelMap.put(ContentHelper.IX_ARTIST, SamsokProtocol.uri_rArtist);
		contextRelMap.put(ContentHelper.IX_DESIGNENGINEER, SamsokProtocol.uri_rDesignEngineer);
		contextRelMap.put(ContentHelper.IX_CARPENTER, SamsokProtocol.uri_rCarpenter);
		contextRelMap.put(ContentHelper.IX_MASON, SamsokProtocol.uri_rMason);
		contextRelMap.put(ContentHelper.IX_TECHNICIAN, SamsokProtocol.uri_rTechnician);
		contextRelMap.put(ContentHelper.IX_PUBLISHER, SamsokProtocol.uri_rPublisher);
		contextRelMap.put(ContentHelper.IX_PUBLICIST, SamsokProtocol.uri_rPublicist);
		contextRelMap.put(ContentHelper.IX_MUSICIAN, SamsokProtocol.uri_rMusician);
		contextRelMap.put(ContentHelper.IX_ACTORACTRESS, SamsokProtocol.uri_rActorActress);
		contextRelMap.put(ContentHelper.IX_PRINTER, SamsokProtocol.uri_rPrinter);
		contextRelMap.put(ContentHelper.IX_SIGNER, SamsokProtocol.uri_rSigner);
		contextRelMap.put(ContentHelper.IX_FINDER, SamsokProtocol.uri_rFinder);
		contextRelMap.put(ContentHelper.IX_ABANDONEE, SamsokProtocol.uri_rAbandonee);
		contextRelMap.put(ContentHelper.IX_INTERMEDIARY, SamsokProtocol.uri_rIntermediary);
		contextRelMap.put(ContentHelper.IX_BUYER, SamsokProtocol.uri_rBuyer);
		contextRelMap.put(ContentHelper.IX_SELLER, SamsokProtocol.uri_rSeller);
		contextRelMap.put(ContentHelper.IX_GENERALAGENT, SamsokProtocol.uri_rGeneralAgent);
		contextRelMap.put(ContentHelper.IX_DONOR, SamsokProtocol.uri_rDonor);
		contextRelMap.put(ContentHelper.IX_DEPOSITOR, SamsokProtocol.uri_rDepositor);
		contextRelMap.put(ContentHelper.IX_RESELLER, SamsokProtocol.uri_rReseller);
		contextRelMap.put(ContentHelper.IX_INVENTORYTAKER, SamsokProtocol.uri_rInventoryTaker);
		contextRelMap.put(ContentHelper.IX_EXCAVATOR, SamsokProtocol.uri_rExcavator);
		contextRelMap.put(ContentHelper.IX_EXAMINATOR, SamsokProtocol.uri_rExaminator);
		contextRelMap.put(ContentHelper.IX_CONSERVATOR, SamsokProtocol.uri_rConservator);
		contextRelMap.put(ContentHelper.IX_ARCHIVECONTRIBUTOR, SamsokProtocol.uri_rArchiveContributor);
		contextRelMap.put(ContentHelper.IX_INTERVIEWER, SamsokProtocol.uri_rInterviewer);
		contextRelMap.put(ContentHelper.IX_INFORMANT, SamsokProtocol.uri_rInformant);
		contextRelMap.put(ContentHelper.IX_PATENTHOLDER, SamsokProtocol.uri_rPatentHolder);
		contextRelMap.put(ContentHelper.IX_USER, SamsokProtocol.uri_rUser);
		contextRelMap.put(ContentHelper.IX_SCANNEROPERATOR, SamsokProtocol.uri_rScannerOperator);
		contextRelMap.put(ContentHelper.IX_PICTUREEDITOR, SamsokProtocol.uri_rPictureEditor);
		contextRelMap.put(ContentHelper.IX_EMPLOYER, SamsokProtocol.uri_rEmployer);
		contextRelMap.put(ContentHelper.IX_MARRIEDTO, SamsokProtocol.uri_rMarriedTo);

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

	@Override
	protected void extractItemInformation() throws Exception {
		super.extractItemInformation();
		// TODO: kontrollera hierarkin ocks�?
		ip.setCurrent(ContentHelper.IX_ITEMSUPERTYPE, true);
		String superType = extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rItemSuperType), ip);
		if (superType == null) {
			throw new Exception("No item supertype for item with identifier " + s.toString());
		}
		// TODO: g�ra n�gra obligatoriska eller varna om de saknas f�r agenter
		//       (supertype==agent ovan kan tex anv�ndas)
		// nya index f�r agenter p� toppniv�
		ip.setCurrent(ContentHelper.IX_NAMEAUTH);
		extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rNameAuth), ip);
		ip.setCurrent(ContentHelper.IX_NAMEID);
		extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rNameId), ip);
		// TODO: foaf:name inneh�ller �ven alternativa namn men man kanske vill ha ett separat
		//       index f�r detta? foaf inneh�ller inget s�nt tyv�rr s� det var d�rf�r jag stoppade
		//       in alternativa namn i namn-f�ltet enligt http://viaf.org/viaf/59878606/rdf.xml
		//       skos har alternativt namn som man skulle kunna anv�nda men egentligen ber�r ju det
		//       koncept, men det kommer vi ju ocks� l�gga in fram�ver s�..
		ip.setCurrent(ContentHelper.IX_NAME);
		RDFUtil.extractValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rName), null, ip);
		ip.setCurrent(ContentHelper.IX_FIRSTNAME);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rFirstName), ip);
		ip.setCurrent(ContentHelper.IX_SURNAME);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rSurname), ip);
		ip.setCurrent(ContentHelper.IX_FULLNAME);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rFullName), ip);
		ip.setCurrent(ContentHelper.IX_GENDER);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rGender), ip);
		ip.setCurrent(ContentHelper.IX_TITLE);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rTitle), ip);
		ip.setCurrent(ContentHelper.IX_ORGANIZATION);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rOrganization), ip);
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
	protected String[] extractContextTypeAndLabelInformation(SubjectNode cS, String identifier) throws Exception {

		// TODO: kontrollera hierarkin ocks� (att produce bara f�r finnas under create tex)?
		String contextSuperTypeURI = extractSingleValue(graph, cS, getURIRef(elementFactory, SamsokProtocol.uri_rContextSuperType), null);
		if (contextSuperTypeURI == null) {
			throw new Exception("No supertype for context for item with identifier " + identifier);
		}
		String contextSuperType = StringUtils.substringAfter(contextSuperTypeURI, SamsokProtocol.contextsuper_pre);
		if (StringUtils.isEmpty(contextSuperType)) {
			throw new Exception("The context supertype URI " + contextSuperTypeURI +
					" does not start with " + SamsokProtocol.contextsuper_pre +
					" for item with identifier " + identifier);
		}
		ip.setCurrent(ContentHelper.IX_CONTEXTSUPERTYPE);
		ip.addToDoc(contextSuperType);

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
						" does not start with " + context_pre +
						" for item with identifier " + identifier);
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
		return new String[] { contextSuperType, contextType };
	}

	@Override
	protected void extractContextActorInformation(SubjectNode cS,
			String[] contextTypes, List<String> relations) throws Exception {
		super.extractContextActorInformation(cS, contextTypes, relations);
		// hantera relationer i kontexten
		extractContextRelationInformation(cS, contextTypes, relations);
	}

	protected void extractContextRelationInformation(SubjectNode cS, String[] contextTypes,
			List<String> relations) throws Exception {
		extractContextLevelRelations(cS, relations);
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
