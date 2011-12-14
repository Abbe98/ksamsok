package se.raa.ksamsok.api.method;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.Relation;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.solr.SearchService;

public class GetRelations extends AbstractAPIMethod {

	private static final Logger logger = Logger.getLogger(GetRelations.class);

	// om och hur sameAs ska hanteras
	private enum InferSameAs { yes, no, sourceOnly, targetsOnly };

	// dubbelriktade
	private static final String HAS_PART = "hasPart";
	private static final String IS_PART_OF = "isPartOf";
	private static final String CONTAINS_OBJECT = "containsObject";
	private static final String IS_CONTAINED_IN = "isContainedIn";
	private static final String IS_FOUND_IN = "isFoundIn";
	private static final String HAS_FIND = "hasFind";
	private static final String HAS_PARENT = "hasParent";
	private static final String HAS_CHILD = "hasChild";
	private static final String VISUALIZES = "visualizes";
	private static final String IS_VISUALIZED_BY = "isVisualizedBy";
	private static final String DESCRIBES = "describes";
	private static final String IS_DESCRIBED_BY = "isDescribedBy";
	private static final String CONTAINS_INFORMATION_ABOUT = "containsInformationAbout";
	private static final String IS_MENTIONED_BY = "isMentionedBy";
	private static final String HAS_OBJECT_EXAMPLE = "hasObjectExample";
	private static final String IS_OBJECT_EXAMPLE_FOR = "isObjectExampleFor";
	// enkelriktade
	private static final String HAS_BEEN_USED_IN = "hasBeenUsedIn";
	private static final String HAS_IMAGE = "hasImage";
	// samma i b�gge riktningarna
	private static final String IS_RELATED_TO = "isRelatedTo";
	private static final String SAME_AS = "sameAs";

	/** Metodnamn */
	public static final String METHOD_NAME = "getRelations";

	/** Parameternamn f�r relation */
	public static final String RELATION_PARAMETER = "relation";
	/** Parameternamn f�r objektidentifierare */
	public static final String IDENTIFIER_PARAMETER = "objectId";
	/** Parameternamn f�r max antal tr�ffar */
	public static final String MAXCOUNT_PARAMETER = "maxCount";
	/** Parameternamn f�r hantering av sameAs-relationer */
	public static final String INFERSAMEAS_PARAMETER = "inferSameAs";
	/** Parameterv�rde f�r att ange alla relationer */
	public static final String RELATION_ALL = "all";

	private static final String SOURCE_DIRECT = null; // b�rjade med v�rde h�r men kom fram till null ist
	private static final String SOURCE_REVERSE = "deduced";

	private static final String URI_PREFIX = "http://kulturarvsdata.se/";

	// datavariabler
	protected String relation;
	protected String partialIdentifier;
	protected int maxCount;
	protected InferSameAs inferSameAs;
	// hj�lpvariabler
	protected boolean isAll;

	private Set<Relation> relations = Collections.emptySet();

	/** map som h�ller �vers�ttningsinformation f�r relationer */
	protected static final Map<String, String> relationXlate;
	/** map som h�ller env�gsrelationer */
	protected static final List<String> relationOneWay;

	static {
		Map<String, String> map = new HashMap<String, String>();
		// dubbelriktade
		twoWay(map, IS_PART_OF, HAS_PART);
		twoWay(map, CONTAINS_OBJECT, IS_CONTAINED_IN);
		twoWay(map, IS_FOUND_IN, HAS_FIND);
		twoWay(map, HAS_CHILD, HAS_PARENT);
		twoWay(map, VISUALIZES, IS_VISUALIZED_BY);
		twoWay(map, IS_DESCRIBED_BY, DESCRIBES);
		twoWay(map, CONTAINS_INFORMATION_ABOUT, IS_MENTIONED_BY);
		twoWay(map, HAS_OBJECT_EXAMPLE, IS_OBJECT_EXAMPLE_FOR);
		twoWay(map, ContentHelper.IX_ISMENTIONEDBY, ContentHelper.IX_MENTIONS);

		// bio (lite special)
		map.put(ContentHelper.IX_FATHER, ContentHelper.IX_CHILD);
		map.put(ContentHelper.IX_MOTHER, ContentHelper.IX_CHILD);
		twoWay(map, ContentHelper.IX_CHILD, ContentHelper.IX_PARENT);

		// cidoc
		twoWay(map, ContentHelper.IX_HASFORMERORCURRENTOWNER, ContentHelper.IX_ISFORMERORCURRENTOWNEROF);
		twoWay(map, ContentHelper.IX_HASFORMERORCURRENTKEEPER, ContentHelper.IX_ISFORMERORCURRENTKEEPEROF);
		twoWay(map, ContentHelper.IX_HASCREATED, ContentHelper.IX_WASCREATEDBY);
		twoWay(map, ContentHelper.IX_HASRIGHTON, ContentHelper.IX_RIGHTHELDBY);
		twoWay(map, ContentHelper.IX_WASPRESENTAT, ContentHelper.IX_OCCUREDINTHEPRESENCEOF);
		twoWay(map, ContentHelper.IX_HADPARTICIPANT, ContentHelper.IX_PARTICIPATEDIN);
		twoWay(map, ContentHelper.IX_ISCURRENTORFORMERMEMBEROF, ContentHelper.IX_HASCURRENTORFORMERMEMBER);

		// roller
		// TODO: Har inte gjort index av alla inverserna vilket man kanske borde - det
		//       beror p� om man vill till�ta (och indexera) rollrelationer fr�n
		//       leverant�rer fr�n ett objekt p� toppniv�n till ett annat utan att
		//       peka fr�n ett kontext vilket d� g�r att man tappar �vrig kontext-info. 
		//       Inversen anv�nds nu bara f�r visning i tex kringla och bara
		//       n�gra f� inversindex finns, se todo i SamsokProtocolHandlers static-block
		twoWay(map, ContentHelper.IX_CLIENT, "clientOf");
		twoWay(map, ContentHelper.IX_COMPOSER, "composerOf");
		twoWay(map, ContentHelper.IX_AUTHOR, "authorOf");
		twoWay(map, ContentHelper.IX_ARCHITECT, "architectOf");
		twoWay(map, ContentHelper.IX_INVENTOR, "inventorOf");
		twoWay(map, ContentHelper.IX_SCENOGRAPHER, "scenographerOf");
		twoWay(map, ContentHelper.IX_DESIGNER, "designerOf");
		twoWay(map, ContentHelper.IX_PRODUCER, "producerOf");
		twoWay(map, ContentHelper.IX_ORGANIZER, "organizerOf");
		twoWay(map, ContentHelper.IX_DIRECTOR, "directorOf");
		twoWay(map, ContentHelper.IX_PHOTOGRAPHER, "photographerOf");
		twoWay(map, ContentHelper.IX_PAINTER, "painterOf");
		twoWay(map, ContentHelper.IX_BUILDER, "builderOf");
		twoWay(map, ContentHelper.IX_MASTERBUILDER, "masterBuilderOf");
		twoWay(map, ContentHelper.IX_CONSTRUCTIONCLIENT, "constructionClientOf");
		twoWay(map, ContentHelper.IX_ENGRAVER, "engraverOf");
		twoWay(map, ContentHelper.IX_MINTMASTER, "mintmasterOf");
		twoWay(map, ContentHelper.IX_ARTIST, "artistOf");
		twoWay(map, ContentHelper.IX_DESIGNENGINEER, "designEngineerOf");
		twoWay(map, ContentHelper.IX_CARPENTER, "carpenterOf");
		twoWay(map, ContentHelper.IX_MASON, "masonOf");
		twoWay(map, ContentHelper.IX_TECHNICIAN, "technicianOf");
		twoWay(map, ContentHelper.IX_PUBLISHER, "publisherOf");
		twoWay(map, ContentHelper.IX_PUBLICIST, "publicistOf");
		twoWay(map, ContentHelper.IX_MUSICIAN, "musicianOf");
		twoWay(map, ContentHelper.IX_ACTORACTRESS, "actorActressOf");
		twoWay(map, ContentHelper.IX_PRINTER, "printerOf");
		twoWay(map, ContentHelper.IX_SIGNER, "signerOf");
		twoWay(map, ContentHelper.IX_FINDER, "finderOf");
		twoWay(map, ContentHelper.IX_ABANDONEE, "abandoneeOf");
		twoWay(map, ContentHelper.IX_INTERMEDIARY, "intermediaryOf");
		twoWay(map, ContentHelper.IX_BUYER, "buyerOf");
		twoWay(map, ContentHelper.IX_SELLER, "sellerOf");
		twoWay(map, ContentHelper.IX_GENERALAGENT, "generalAgentOf");
		twoWay(map, ContentHelper.IX_DONOR, "donorOf");
		twoWay(map, ContentHelper.IX_DEPOSITOR, "depositorOf");
		twoWay(map, ContentHelper.IX_RESELLER, "resellerOf");
		twoWay(map, ContentHelper.IX_INVENTORYTAKER, "inventoryTakerOf");
		twoWay(map, ContentHelper.IX_EXCAVATOR, "excavatorOf");
		twoWay(map, ContentHelper.IX_EXAMINATOR, "examinatorOf");
		twoWay(map, ContentHelper.IX_CONSERVATOR, "conservatorOf");
		twoWay(map, ContentHelper.IX_ARCHIVECONTRIBUTOR, "archiveContributorOf");
		twoWay(map, ContentHelper.IX_INTERVIEWER, "interviewerOf");
		twoWay(map, ContentHelper.IX_INFORMANT, "informantOf");
		twoWay(map, ContentHelper.IX_PATENTHOLDER, "patentHolderOf");
		twoWay(map, ContentHelper.IX_USER, "userOf");
		twoWay(map, ContentHelper.IX_SCANNEROPERATOR, "scannerOperatorOf");
		twoWay(map, ContentHelper.IX_PICTUREEDITOR, "pictureEditorOf");
		twoWay(map, ContentHelper.IX_EMPLOYER, "employerOf");

		// enkelriktade
		map.put(HAS_BEEN_USED_IN, IS_RELATED_TO);
		map.put(HAS_IMAGE, IS_RELATED_TO);
		relationOneWay = Collections.unmodifiableList(Arrays.asList(
				HAS_BEEN_USED_IN, HAS_IMAGE, ContentHelper.IX_FATHER, ContentHelper.IX_MOTHER));

		// samma i b�gge riktningarna
		map.put(IS_RELATED_TO, IS_RELATED_TO);
		map.put(SAME_AS, SAME_AS);
		map.put(ContentHelper.IX_MARRIEDTO, ContentHelper.IX_MARRIEDTO);

		relationXlate = Collections.unmodifiableMap(map);
	}

	// l�gger till relationerna �t b�gge h�ll
	private static void twoWay(Map<String, String> map, String relA, String relB) {
		map.put(relA, relB);
		map.put(relB, relA);
	}

	/**
	 * Skapa ny instans.
	 * @param serviceProvider tj�nstetillhandah�llare
	 * @param writer writer
	 * @param params parametrar
	 */
	public GetRelations(APIServiceProvider serviceProvider, PrintWriter writer, Map<String, String> params) {
		super(serviceProvider, writer, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		relation = getMandatoryParameterValue(RELATION_PARAMETER, "GetRelations.extractParameters", null, false);
		isAll = RELATION_ALL.equals(relation);
		if (!isAll && !relationXlate.containsKey(relation)) {
			throw new BadParameterException("V�rdet f�r parametern " + RELATION_PARAMETER + " �r ogiltigt",
					"GetRelations.extractParameters", null, false);
		}
		partialIdentifier = getMandatoryParameterValue(IDENTIFIER_PARAMETER, "GetRelations.extractParameters", null, false);
		String maxCountStr = getOptionalParameterValue(MAXCOUNT_PARAMETER, "GetRelations.extractParameters", null, false);
		if (maxCountStr != null) {
			try {
				maxCount = Integer.parseInt(maxCountStr); 
			} catch (Exception e) {
				throw new BadParameterException("V�rdet f�r parametern " + MAXCOUNT_PARAMETER + " �r ogiltigt",
						"GetRelations.extractParameters", null, false);
			}
		} else {
			maxCount = -1;
		}
		String inferSameAsStr = getOptionalParameterValue(INFERSAMEAS_PARAMETER, "GetRelations.extractParameters", null, false);
		if (inferSameAsStr != null) {
			try {
				inferSameAs = InferSameAs.valueOf(inferSameAsStr);
			} catch (Exception e) {
				throw new BadParameterException("V�rdet f�r parametern " + INFERSAMEAS_PARAMETER + " �r ogiltigt",
						"GetRelations.extractParameters", null, false);
			}
		} else {
			inferSameAs = InferSameAs.no;
		}

	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		// om 0 g�r inget alls
		if (maxCount == 0) {
			return;
		}
		SearchService searchService = serviceProvider.getSearchService();
		final String uri = URI_PREFIX + partialIdentifier;
		Set<String> itemUris = new HashSet<String>();
		itemUris.add(uri);

		String escapedUri = ClientUtils.escapeQueryChars(uri);
		SolrQuery query = new SolrQuery();
		query.setRows(maxCount > 0 ? maxCount : Integer.MAX_VALUE); // TODO: kan det bli f�r m�nga?

		// TODO: algoritmen kan beh�va finslipas och optimeras tex f�r poster med m�nga relaterade objekt
		// algoritmen ser fn ut s� h�r - inferSameAs styr steg 1 och 3, default �r att inte utf�ra dem
		// 1. h�mta ev post f�r att f� tag p� postens sameAs
		// 2. s�k fram k�llpost(er) och alla relaterade poster (post + ev alla sameAs och deras relaterade)
		// 3. h�mta ev de relaterades sameAs och l�gg till dessa som relationer

		// h�mta uri och relationer
		query.addField(ContentHelper.I_IX_RELATIONS);
		query.addField(ContentHelper.IX_ITEMID);
		try {
			QueryResponse qr;
			SolrDocumentList docs;
			if (inferSameAs == InferSameAs.yes || inferSameAs == InferSameAs.sourceOnly) {
				// h�mta andra poster som �r samma som denna och l�gg till dem som "k�llposter"
				query.setQuery(ContentHelper.IX_ITEMID + ":"+ escapedUri);
				qr = searchService.query(query);
				docs = qr.getResults();
				for (SolrDocument doc: docs) {
					String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
					Collection<Object> values = doc.getFieldValues(ContentHelper.I_IX_RELATIONS);
					if (values != null) {
						for (Object value: values) {
							String parts[] = ((String) value).split("\\|");
							if (parts.length != 2) {
								logger.error("Fel p� v�rde f�r relationsindex f�r " + itemId + ", ej p� korrekt format: " + value);
								continue;
							}
							String typePart = parts[0];
							String uriPart = parts[1];
							if (SAME_AS.equals(typePart)) {
								itemUris.add(uriPart);
							}
						}
					}
				}
			}
			// bygg s�kstr�ng mh k�llposten/alla k�llposter
			StringBuilder searchStr = new StringBuilder();
			for (String itemId: itemUris) {
				String escapedItemId = ClientUtils.escapeQueryChars(itemId);
				if (searchStr.length() > 0) {
					searchStr.append(" OR ");
				}
				searchStr.append(ContentHelper.IX_ITEMID + ":" + escapedItemId + " OR " + ContentHelper.IX_RELURI + ":"+ escapedItemId);
			}
			// s�k fram k�llposten/-erna och alla som har relation till den/dem
			query.setQuery(searchStr.toString());

			qr = searchService.query(query);
			docs = qr.getResults();
			relations = new HashSet<Relation>();
			for (SolrDocument doc: docs) {
				String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
				boolean isSourceDoc = itemUris.contains(itemId);
				Collection<Object> values = doc.getFieldValues(ContentHelper.I_IX_RELATIONS);
				if (values != null) {
					for (Object value: values) {
						String parts[] = ((String) value).split("\\|");
						if (parts.length != 2) {
							logger.error("Fel p� v�rde f�r relationsindex f�r " + itemId + ", ej p� korrekt format: " + value);
							continue;
						}
						String orgTypePart = null; // h�ller orginaltypen om vi g�r en inversupplagning
						String typePart = parts[0];
						String uriPart = parts[1];
						if (!isSourceDoc) {
							if (!itemUris.contains(uriPart)) {
								// inte f�r aktuellt objekt
								continue;
							}
							orgTypePart = typePart;
							typePart = relationXlate.get(typePart);
							uriPart = itemId;

							// f�rs�k ta bort de som �r on�diga, dvs de som redan har en env�gs-relation
							// deras invers �r inte intressant att ha med d� den inte s�ger n�t
							if (IS_RELATED_TO.equals(typePart)) {
								boolean exists = false;
								Relation rel = null;
								for (String owRel: relationOneWay) {
									// bara typ och uri �r intressanta f�r detta
									rel = new Relation(owRel, itemId, null, null);
									if (relations.contains(rel)) {
										exists = true;
										break;
									}
								}
								if (exists) {
									if (logger.isDebugEnabled()) {
										logger.debug("Exists inversed already " + rel);
									}
									continue;
								}
							}
						}

						String source = isSourceDoc ? SOURCE_DIRECT : SOURCE_REVERSE;
						Relation rel = new Relation(typePart, uriPart, source, orgTypePart);
						if (!relations.add(rel)) {
							if (logger.isDebugEnabled()) {
								logger.debug("duplicate rel " + rel);
							}
						}
						// optimering genom att g�ra return direkt vid detta fall, annars ska man
						// hoppa ur denna loop och den utanf�r den och sen filtrera
						// funkar dock bara f�r fallet "alla"
						if (maxCount > 0 && isAll && relations.size() == maxCount) {
							return;
						}
					}
				}
			}
			if (inferSameAs == InferSameAs.yes || inferSameAs == InferSameAs.targetsOnly) {
				// s�kning p� same as f�r tr�ffarnas uri:er och skapa relation till dessa ocks�
				query.setFields(ContentHelper.IX_ITEMID); // bara itemId h�r
				for (Relation rel: new HashSet<Relation>(relations)) {
					query.setQuery(ContentHelper.IX_SAMEAS + ":"+ ClientUtils.escapeQueryChars(rel.getTargetUri()));
					qr = searchService.query(query);
					docs = qr.getResults();
					for (SolrDocument doc: docs) {
						String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
						// ta inte med min uri
						if (itemUris.contains(itemId)) {
							continue;
						}
						if (!relations.add(new Relation(rel.getRelationType(), itemId, rel.getSource(), rel.getOriginalRelationType()))) {
							if (logger.isDebugEnabled()) {
								logger.debug("duplicate rel (from same as) " + rel);
							}
						}
					}
				}
			}

			// postfilter vid s�kning p� specifik relation
			if (!isAll) {
				int matches = 0;
				Iterator<Relation> iter = relations.iterator();
				while (iter.hasNext()) {
					if (!iter.next().getRelationType().equals(relation) || maxCount > 0 && matches >= maxCount) {
						iter.remove();
					} else {
						++matches;
					}
				}
			}
		} catch (Exception e) {
			throw new DiagnosticException("Fel vid metodanrop", "GetRelations.performMethodLogic", e.getMessage(), true);
		}

	}

	@Override
	protected void writeResult() throws DiagnosticException {
		String source;
		//String originalRelationType;
		writer.println("<relations count=\"" + relations.size() + "\">");
		for (Relation rel: relations) {
			source = rel.getSource();
			//originalRelationType = rel.getOriginalRelationType();
			writer.print("<relation type=\"" + rel.getRelationType() + "\"" +
					(source != null ? " source=\"" + source + "\"" : "") +
					//(originalRelationType != null ? " originalType=\"" + originalRelationType + "\"" : "") +
					">");
			writer.print(StringEscapeUtils.escapeXml(rel.getTargetUri()));
			writer.println("</relation>");
		}
		writer.println("</relations>");
	}
}
