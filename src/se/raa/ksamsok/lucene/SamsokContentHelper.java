package se.raa.ksamsok.lucene;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.jrdf.JRDFFactory;
import org.jrdf.SortedMemoryJRDFFactory;
import org.jrdf.collection.MemMapFactory;
import org.jrdf.graph.AnyObjectNode;
import org.jrdf.graph.AnySubjectNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.Literal;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;
import org.jrdf.parser.rdfxml.GraphRdfXmlParser;
import org.xml.sax.InputSource;

import se.raa.ksamsok.harvest.HarvestService;

/**
 * Klass som hanterar k-sams�ksformat (xml/rdf).
 */
public class SamsokContentHelper extends ContentHelper {

	private static final Logger logger = Logger.getLogger(SamsokContentHelper.class);

	private static final String uriPrefix = "http://kulturarvsdata.se/";
	private static final String uriPrefixKSamsok = uriPrefix + "ksamsok#";

	private static final URI uri_rdfType = URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	private static final URI uri_samsokEntity = URI.create(uriPrefixKSamsok + "Entity");

	// "tagnamn"
	private static final URI uri_rServiceName = URI.create(uriPrefixKSamsok + "serviceName");
	private static final URI uri_rServiceOrganization = URI.create(uriPrefixKSamsok + "serviceOrganization");
	private static final URI uri_rItemTitle = URI.create(uriPrefixKSamsok + "itemTitle");
	private static final URI uri_rItemType = URI.create(uriPrefixKSamsok + "itemType");
	private static final URI uri_rItemClass = URI.create(uriPrefixKSamsok + "itemClass");
	private static final URI uri_rItemClassName = URI.create(uriPrefixKSamsok + "itemClassName");
	private static final URI uri_rItemName = URI.create(uriPrefixKSamsok + "itemName");
	private static final URI uri_rItemSpecification = URI.create(uriPrefixKSamsok + "itemSpecification");
	private static final URI uri_rItemKeyWord = URI.create(uriPrefixKSamsok + "itemKeyWord");
	private static final URI uri_rItemMotiveWord = URI.create(uriPrefixKSamsok + "itemMotiveWord");
	private static final URI uri_rItemMaterial = URI.create(uriPrefixKSamsok + "itemMaterial");
	private static final URI uri_rItemTechnique = URI.create(uriPrefixKSamsok + "itemTechnique");
	private static final URI uri_rItemStyle = URI.create(uriPrefixKSamsok + "itemStyle");
	private static final URI uri_rItemColor = URI.create(uriPrefixKSamsok + "itemColor");
	private static final URI uri_rItemNumber = URI.create(uriPrefixKSamsok + "itemNumber");
	private static final URI uri_rItemDescription = URI.create(uriPrefixKSamsok + "itemDescription");
	private static final URI uri_rSubject = URI.create(uriPrefixKSamsok + "subject");
	private static final URI uri_rCollection = URI.create(uriPrefixKSamsok + "collection");
	private static final URI uri_rDataQuality = URI.create(uriPrefixKSamsok + "dataQuality");
	private static final URI uri_rMediaType = URI.create(uriPrefixKSamsok + "mediaType");
	private static final URI uri_r__Desc = URI.create(uriPrefixKSamsok + "desc");
	private static final URI uri_r__Name = URI.create(uriPrefixKSamsok + "name");
	private static final URI uri_r__Spec = URI.create(uriPrefixKSamsok + "spec");
	private static final URI uri_rMaterial = URI.create(uriPrefixKSamsok + "material");
	private static final URI uri_rNumber = URI.create(uriPrefixKSamsok + "number");
	private static final URI uri_rPres = URI.create(uriPrefixKSamsok + "presentation");
	private static final URI uri_rContext = URI.create(uriPrefixKSamsok + "context");
	private static final URI uri_rContextType = URI.create(uriPrefixKSamsok + "contextType");
	private static final URI uri_rContextLabel = URI.create(uriPrefixKSamsok + "contextLabel");
	private static final URI uri_rURL = URI.create(uriPrefixKSamsok + "url");
	private static final URI uri_rMuseumdatURL = URI.create(uriPrefixKSamsok + "museumdatUrl");

	// relationer
	private static final URI uri_rContainsInformationAbout = URI.create(uriPrefixKSamsok + "containsInformationAbout");
	private static final URI uri_rContainsObject = URI.create(uriPrefixKSamsok + "containsObject");
	private static final URI uri_rHasBeenUsedIn = URI.create(uriPrefixKSamsok + "hasBeenUsedIn");
	private static final URI uri_rHasChild = URI.create(uriPrefixKSamsok + "hasChild");
	private static final URI uri_rHasFind = URI.create(uriPrefixKSamsok + "hasFind");
	private static final URI uri_rHasImage = URI.create(uriPrefixKSamsok + "hasImage");
	private static final URI uri_rHasObjectExample = URI.create(uriPrefixKSamsok + "hasObjectExample");
	private static final URI uri_rHasParent = URI.create(uriPrefixKSamsok + "hasParent");
	private static final URI uri_rHasPart = URI.create(uriPrefixKSamsok + "hasPart");
	private static final URI uri_rIsDescribedBy = URI.create(uriPrefixKSamsok + "isDescribedBy");
	private static final URI uri_rIsFoundIn = URI.create(uriPrefixKSamsok + "isFoundIn");
	private static final URI uri_rIsPartOf = URI.create(uriPrefixKSamsok + "isPartOf");
	private static final URI uri_rIsRelatedTo = URI.create(uriPrefixKSamsok + "isRelatedTo");
	private static final URI uri_rIsVisualizedBy = URI.create(uriPrefixKSamsok + "isVisualizedBy");
	private static final URI uri_rSameAs = URI.create("http://www.w3.org/2002/07/owl#sameAs"); // obs, owl
	private static final URI uri_rVisualizes = URI.create(uriPrefixKSamsok + "visualizes");

	// var-kontext
	private static final URI uri_rContinentName = URI.create(uriPrefixKSamsok + "continentName");
	private static final URI uri_rCountryName = URI.create(uriPrefixKSamsok + "countryName");
	private static final URI uri_rPlaceName = URI.create(uriPrefixKSamsok + "placeName");
	private static final URI uri_rCadastralUnit = URI.create(uriPrefixKSamsok + "cadastralUnit");
	private static final URI uri_rPlaceTermId = URI.create(uriPrefixKSamsok + "placeTermId");
	private static final URI uri_rPlaceTermAuth = URI.create(uriPrefixKSamsok + "placeTermAuth");
	private static final URI uri_rCountyName = URI.create(uriPrefixKSamsok + "countyName");
	private static final URI uri_rMunicipalityName = URI.create(uriPrefixKSamsok + "municipalityName");
	private static final URI uri_rProvinceName = URI.create(uriPrefixKSamsok + "provinceName");
	private static final URI uri_rParishName = URI.create(uriPrefixKSamsok + "parishName");
	private static final URI uri_rCountry = URI.create("http://www.mindswap.org/2003/owl/geo/geoFeatures20040307.owl#country");
	private static final URI uri_rCounty = URI.create("http://www.mindswap.org/2003/owl/geo/geoFeatures20040307.owl#county");
	private static final URI uri_rMunicipality = URI.create("http://www.mindswap.org/2003/owl/geo/geoFeatures20040307.owl#municipality");
	private static final URI uri_rProvince = URI.create("http://www.mindswap.org/2003/owl/geo/geoFeatures20040307.owl#province");
	private static final URI uri_rParish = URI.create("http://www.mindswap.org/2003/owl/geo/geoFeatures20040307.owl#parish");

	// vem-kontext
	private static final URI uri_rFirstName = URI.create("http://xmlns.com/foaf/0.1/#firstName");
	private static final URI uri_rSurname = URI.create("http://xmlns.com/foaf/0.1/#surname");
	private static final URI uri_rName = URI.create("http://xmlns.com/foaf/0.1/#name");
	private static final URI uri_rOrganization = URI.create("http://xmlns.com/foaf/0.1/#organization");
	private static final URI uri_rTitle = URI.create("http://xmlns.com/foaf/0.1/#title");
	private static final URI uri_rNameId = URI.create(uriPrefixKSamsok + "nameId");
	private static final URI uri_rNameAuth = URI.create(uriPrefixKSamsok + "nameAuth");

	// n�r-kontext
	private static final URI uri_rFromTime = URI.create(uriPrefixKSamsok + "fromTime");
	private static final URI uri_rToTime = URI.create(uriPrefixKSamsok + "toTime");
	private static final URI uri_rFromPeriodName = URI.create(uriPrefixKSamsok + "fromPeriodName");
	private static final URI uri_rToPeriodName = URI.create(uriPrefixKSamsok + "toPeriodName");
	private static final URI uri_rFromPeriodId = URI.create(uriPrefixKSamsok + "fromPeriodId");
	private static final URI uri_rToPeriodId = URI.create(uriPrefixKSamsok + "toPeriodId");
	private static final URI uri_rPeriodAuth = URI.create(uriPrefixKSamsok + "periodAuth");
	private static final URI uri_rEventName = URI.create(uriPrefixKSamsok + "eventName");
	private static final URI uri_rEventAuth = URI.create(uriPrefixKSamsok + "eventAuth");
	//private static final URI uri_rTimeText = URI.create(uriPrefixKSamsok + "timeText");
	
	// geo
	private static final String aukt_country_pre = uriPrefix + "resurser/aukt/geo/country#";
	private static final String aukt_county_pre = uriPrefix + "resurser/aukt/geo/county#";
	private static final String aukt_municipality_pre = uriPrefix + "resurser/aukt/geo/municipality#";
	private static final String aukt_province_pre = uriPrefix + "resurser/aukt/geo/province#";
	private static final String aukt_parish_pre = uriPrefix + "resurser/aukt/geo/parish#";

	// context
	private static final String context_pre = uriPrefix + "resurser/ContextType#";

	private DocumentBuilderFactory xmlFact;
	private TransformerFactory xformerFact;
    // har en close() men den g�r inget s� vi skapar bara en instans
	private static final JRDFFactory jrdfFactory = SortedMemoryJRDFFactory.getFactory();
	private static final String PATH = "/" + SamsokContentHelper.class.getPackage().getName().replace('.', '/') + "/";

	// iso8601-datumparser
	private static final DateTimeFormatter isoDateTimeFormatter = ISODateTimeFormat.dateOptionalTimeParser();

	// map med uri -> v�rde f�r indexering
	private static final Map<String,String> uriValues = new HashMap<String,String>();
	static {
		// l�s in uri-v�rden f�r uppslagning
		readURIValueResource("entitytype.rdf", uri_r__Name);
		readURIValueResource("subject.rdf", uri_r__Name);
		readURIValueResource("dataquality.rdf", uri_r__Name);
		readURIValueResource("contexttype.rdf", uri_rContextLabel);
	}

	public SamsokContentHelper() {
		xmlFact = DocumentBuilderFactory.newInstance();
	    xmlFact.setNamespaceAware(true);
	    xformerFact = TransformerFactory.newInstance();
	}

	@Override
	public Document createLuceneDocument(HarvestService service, String xmlContent) throws Exception {
		Document luceneDoc = new Document();
		StringReader r = null;
		Graph graph = null;
		String identifier = null;
		// TODO: �r detta ett bra s�tt att l�sa ut rdf?
		try {
			graph = jrdfFactory.getNewGraph();
			GraphRdfXmlParser parser = new GraphRdfXmlParser(graph, new MemMapFactory());
			r = new StringReader(xmlContent);
			parser.parse(r, ""); // baseuri?

			GraphElementFactory elementFactory = graph.getElementFactory();
			URIReference rdfType = elementFactory.createURIReference(uri_rdfType);
			URIReference samsokEntity = elementFactory.createURIReference(uri_samsokEntity);

			// "tagnamn"
			// verkar vara tvungen att skapa lokala instanser av dessa d� de ej hittas
			// korrekt om man skapar globala URIReferenceImpl-instanser
			URIReference rServiceName = elementFactory.createURIReference(uri_rServiceName);
			URIReference rServiceOrganization = elementFactory.createURIReference(uri_rServiceOrganization);
			URIReference rItemTitle = elementFactory.createURIReference(uri_rItemTitle);
			URIReference rItemType = elementFactory.createURIReference(uri_rItemType);
			URIReference rItemClass = elementFactory.createURIReference(uri_rItemClass);
			URIReference rItemClassName = elementFactory.createURIReference(uri_rItemClassName);
			URIReference rItemName = elementFactory.createURIReference(uri_rItemName);
			URIReference rItemSpecification = elementFactory.createURIReference(uri_rItemSpecification);
			URIReference rItemKeyWord = elementFactory.createURIReference(uri_rItemKeyWord);
			URIReference rItemMotiveWord = elementFactory.createURIReference(uri_rItemMotiveWord);
			URIReference rItemMaterial = elementFactory.createURIReference(uri_rItemMaterial);
			URIReference rItemTechnique = elementFactory.createURIReference(uri_rItemTechnique);
			URIReference rItemStyle = elementFactory.createURIReference(uri_rItemStyle);
			URIReference rItemColor = elementFactory.createURIReference(uri_rItemColor);
			URIReference rItemNumber = elementFactory.createURIReference(uri_rItemNumber);
			URIReference rItemDescription = elementFactory.createURIReference(uri_rItemDescription);
			URIReference rSubject = elementFactory.createURIReference(uri_rSubject);
			URIReference rCollection = elementFactory.createURIReference(uri_rCollection);
			URIReference rDataQuality = elementFactory.createURIReference(uri_rDataQuality);
			URIReference rMediaType = elementFactory.createURIReference(uri_rMediaType);
			URIReference r__Desc = elementFactory.createURIReference(uri_r__Desc);
			URIReference r__Name = elementFactory.createURIReference(uri_r__Name);
			URIReference r__Spec = elementFactory.createURIReference(uri_r__Spec);
			URIReference rMaterial = elementFactory.createURIReference(uri_rMaterial);
			URIReference rNumber = elementFactory.createURIReference(uri_rNumber);
			URIReference rPres = elementFactory.createURIReference(uri_rPres);
			URIReference rContext = elementFactory.createURIReference(uri_rContext);
			URIReference rContextType = elementFactory.createURIReference(uri_rContextType);
			URIReference rContextLabel = elementFactory.createURIReference(uri_rContextLabel);
			URIReference rURL = elementFactory.createURIReference(uri_rURL);
			URIReference rMuseumdatURL = elementFactory.createURIReference(uri_rMuseumdatURL);

			// relationer
			URIReference rContainsInformationAbout = elementFactory.createURIReference(uri_rContainsInformationAbout);
			URIReference rContainsObject = elementFactory.createURIReference(uri_rContainsObject);
			URIReference rHasBeenUsedIn = elementFactory.createURIReference(uri_rHasBeenUsedIn);
			URIReference rHasChild = elementFactory.createURIReference(uri_rHasChild);
			URIReference rHasFind = elementFactory.createURIReference(uri_rHasFind);
			URIReference rHasImage = elementFactory.createURIReference(uri_rHasImage);
			URIReference rHasObjectExample = elementFactory.createURIReference(uri_rHasObjectExample);
			URIReference rHasParent = elementFactory.createURIReference(uri_rHasParent);
			URIReference rHasPart = elementFactory.createURIReference(uri_rHasPart);
			URIReference rIsDescribedBy = elementFactory.createURIReference(uri_rIsDescribedBy);
			URIReference rIsFoundIn = elementFactory.createURIReference(uri_rIsFoundIn);
			URIReference rIsPartOf = elementFactory.createURIReference(uri_rIsPartOf);
			URIReference rIsRelatedTo = elementFactory.createURIReference(uri_rIsRelatedTo);
			URIReference rIsVisualizedBy = elementFactory.createURIReference(uri_rIsVisualizedBy);
			URIReference rSameAs = elementFactory.createURIReference(uri_rSameAs);
			URIReference rVisualizes = elementFactory.createURIReference(uri_rVisualizes);

			// var
			URIReference rPlaceName = elementFactory.createURIReference(uri_rPlaceName);
			URIReference rCadastralUnit = elementFactory.createURIReference(uri_rCadastralUnit);
			URIReference rPlaceTermId = elementFactory.createURIReference(uri_rPlaceTermId);
			URIReference rPlaceTermAuth = elementFactory.createURIReference(uri_rPlaceTermAuth);
			URIReference rContinentName = elementFactory.createURIReference(uri_rContinentName);
			URIReference rCountryName = elementFactory.createURIReference(uri_rCountryName);
			URIReference rCountyName = elementFactory.createURIReference(uri_rCountyName);
			URIReference rMunicipalityName = elementFactory.createURIReference(uri_rMunicipalityName);
			URIReference rProvinceName = elementFactory.createURIReference(uri_rProvinceName);
			URIReference rParishName = elementFactory.createURIReference(uri_rParishName);
			URIReference rCountry = elementFactory.createURIReference(uri_rCountry);
			URIReference rCounty = elementFactory.createURIReference(uri_rCounty);
			URIReference rMunicipality = elementFactory.createURIReference(uri_rMunicipality);
			URIReference rProvince = elementFactory.createURIReference(uri_rProvince);
			URIReference rParish = elementFactory.createURIReference(uri_rParish);

			// vem
			URIReference rFirstName = elementFactory.createURIReference(uri_rFirstName);
			URIReference rSurname = elementFactory.createURIReference(uri_rSurname);
			URIReference rName = elementFactory.createURIReference(uri_rName);
			URIReference rTitle = elementFactory.createURIReference(uri_rTitle);
			URIReference rOrganization = elementFactory.createURIReference(uri_rOrganization);
			URIReference rNameId = elementFactory.createURIReference(uri_rNameId);
			URIReference rNameAuth = elementFactory.createURIReference(uri_rNameAuth);

			// n�r
			URIReference rFromTime = elementFactory.createURIReference(uri_rFromTime);
			URIReference rToTime = elementFactory.createURIReference(uri_rToTime);
			URIReference rFromPeriodName = elementFactory.createURIReference(uri_rFromPeriodName);
			URIReference rToPeriodName = elementFactory.createURIReference(uri_rToPeriodName);
			URIReference rFromPeriodId = elementFactory.createURIReference(uri_rFromPeriodId);
			URIReference rToPeriodId = elementFactory.createURIReference(uri_rToPeriodId);
			URIReference rPeriodAuth = elementFactory.createURIReference(uri_rPeriodAuth);
			URIReference rEventName = elementFactory.createURIReference(uri_rEventName);
			URIReference rEventAuth = elementFactory.createURIReference(uri_rEventAuth);
			//URIReference rTimeText = elementFactory.createURIReference(uri_rTimeText);

			String pres = null;
			SubjectNode s = null;
			for (Triple triple: graph.find(AnySubjectNode.ANY_SUBJECT_NODE, rdfType, samsokEntity)) {
				if (s != null) {
					throw new Exception("Ska bara finnas en entity i rdf-grafen");
				}
				s = triple.getSubject();
			}
			if (s == null) {
				logger.error("Hittade ingen entity i rdf-grafen:\n" + graph);
				throw new Exception("Hittade ingen entity i rdf-grafen");
			}

			identifier = s.toString();
			// identifierare enligt http://www.loc.gov/standards/sru/march06-meeting/record-id.html
			luceneDoc.add(new Field(IX_ITEMID, identifier, Field.Store.YES, Field.Index.NOT_ANALYZED));
			// fr�mst f�r sru 1.2
			luceneDoc.add(new Field(CONTEXT_SET_REC + "." + IX_REC_IDENTIFIER, identifier, Field.Store.YES, Field.Index.NOT_ANALYZED));
			// tj�nst
			luceneDoc.add(new Field(I_IX_SERVICE, service.getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
			// html-url
			String url = extractSingleValue(graph, s, rURL, null);
			if (url != null) {
				if (url.toLowerCase().startsWith(uriPrefix)) {
					addProblemMessage("Html-url startar med " + uriPrefix);
				}
				luceneDoc.add(new Field(I_IX_HTML_URL, url, Field.Store.YES, Field.Index.NOT_ANALYZED));
			}
			// museumdat-url
			url = extractSingleValue(graph, s, rMuseumdatURL, null);
			if (url != null) {
				if (url.toLowerCase().startsWith(uriPrefix)) {
					addProblemMessage("Museumdat-url startar med " + uriPrefix);
				}
				luceneDoc.add(new Field(I_IX_MUSEUMDAT_URL, url, Field.Store.YES, Field.Index.NOT_ANALYZED));
			}

			StringBuffer allText = new StringBuffer();
			StringBuffer placeText = new StringBuffer();
			StringBuffer itemText = new StringBuffer();
			StringBuffer actorText = new StringBuffer();
			StringBuffer timeText = new StringBuffer();

			IndexProcessor ip = new IndexProcessor(luceneDoc);

			// h�mta ut serviceName (01, fast 11 egentligen?)
			ip.setCurrent(IX_SERVICENAME);
			appendToTextBuffer(allText, extractSingleValue(graph, s, rServiceName, ip));
			// h�mta ut serviceOrganization (01, fast 11 egentligen?)
			ip.setCurrent(IX_SERVICEORGANISATION);
			appendToTextBuffer(allText, extractSingleValue(graph, s, rServiceOrganization, ip));
			// h�mta ut itemTitle (0m)
			ip.setCurrent(IX_ITEMTITLE, Field.Store.YES);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemTitle, null, ip));
			// h�mta ut itemType (1)
			ip.setCurrent(IX_ITEMTYPE);
			extractSingleValue(graph, s, rItemType, ip);
			// h�mta ut itemClass (0m)
			ip.setCurrent(IX_ITEMCLASS, Field.Store.NO, false); // sl� inte upp uri
			extractValue(graph, s, rItemClass, null, ip);
			// h�mta ut itemClassName (0m)
			ip.setCurrent(IX_ITEMCLASSNAME);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemClassName, null, ip));
			// h�mta ut itemName (1m)
			ip.setCurrent(IX_ITEMNAME);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemName, r__Name, ip));
			// h�mta ut itemSpecification (0m)
			ip.setCurrent(IX_ITEMSPECIFICATION);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemSpecification, r__Spec, ip));
			// h�mta ut itemKeyWord (0m)
			ip.setCurrent(IX_ITEMKEYWORD);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemKeyWord, null, ip));
			// h�mta ut itemMotiveWord (0m)
			ip.setCurrent(IX_ITEMMOTIVEWORD);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemMotiveWord, null, ip));
			// h�mta ut itemMaterial (0m)
			ip.setCurrent(IX_ITEMMATERIAL);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemMaterial, rMaterial, ip));
			// h�mta ut itemTechnique (0m)
			ip.setCurrent(IX_ITEMTECHNIQUE);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemTechnique, null, ip));
			// h�mta ut itemStyle (0m)
			ip.setCurrent(IX_ITEMSTYLE);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemStyle, null, ip));
			// h�mta ut itemColor (0m)
			ip.setCurrent(IX_ITEMCOLOR);
			appendToTextBuffer(itemText, extractValue(graph, s, rItemColor, null, ip));
			// h�mta ut itemNumber (0m)
			ip.setCurrent(IX_ITEMNUMBER);
			extractValue(graph, s, rItemNumber, rNumber, ip); // in i fritext?
			// h�mta ut itemDescription, resursnod (0m)
			ip.setCurrent(IX_ITEMDESCRIPTION); // fritext
			appendToTextBuffer(itemText, extractValue(graph, s, rItemDescription, r__Desc, ip));
			// TODO: subject inte r�tt, �r bara en uri-pekare nu(?)
			// h�mta ut subject (0m)
			ip.setCurrent(IX_SUBJECT);
			extractValue(graph, s, rSubject, null, ip);
			// h�mta ut collection (0m)
			ip.setCurrent(IX_COLLECTION);
			extractValue(graph, s, rCollection, null, ip);
			// h�mta ut dataQuality (1)
			ip.setCurrent(IX_DATAQUALITY);
			extractSingleValue(graph, s, rDataQuality, ip);
			// h�mta ut mediaType (0n)
			ip.setCurrent(IX_MEDIATYPE);
			extractValue(graph, s, rMediaType, null, ip);

			// relationer, in i respektive index + i IX_RELURI
			final String[] relIx = new String[] { null, IX_RELURI };
			// h�mta ut containsInformationAbout (0n)
			relIx[0] = IX_CONTAINSINFORMATIONABOUT;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rContainsInformationAbout, null, ip);
			// h�mta ut containsObject (0n)
			relIx[0] = IX_CONTAINSOBJECT;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rContainsObject, null, ip);
			// h�mta ut hasBeenUsedIn (0n)
			relIx[0] = IX_HASBEENUSEDIN;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rHasBeenUsedIn, null, ip);
			// h�mta ut hasChild (0n)
			relIx[0] = IX_HASCHILD;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rHasChild, null, ip);
			// h�mta ut hasFind (0n)
			relIx[0] = IX_HASFIND;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rHasFind, null, ip);
			// h�mta ut hasImage (0n)
			relIx[0] = IX_HASIMAGE;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rHasImage, null, ip);
			// h�mta ut hasObjectExample (0n)
			relIx[0] = IX_HASOBJECTEXAMPLE;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rHasObjectExample, null, ip);
			// h�mta ut hasParent (0n)
			relIx[0] = IX_HASPARENT;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rHasParent, null, ip);
			// h�mta ut hasPart (0n)
			relIx[0] = IX_HASPART;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rHasPart, null, ip);
			// h�mta ut isDescribedBy (0n)
			relIx[0] = IX_ISDESCRIBEDBY;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rIsDescribedBy, null, ip);
			// h�mta ut isFoundIn (0n)
			relIx[0] = IX_ISFOUNDIN;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rIsFoundIn, null, ip);
			// h�mta ut isPartOf (0n)
			relIx[0] = IX_ISPARTOF;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rIsPartOf, null, ip);
			// h�mta ut isRelatedTo (0n)
			relIx[0] = IX_ISRELATEDTO;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rIsRelatedTo, null, ip);
			// h�mta ut isVisualizedBy (0n)
			relIx[0] = IX_ISVISUALIZEDBY;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rIsVisualizedBy, null, ip);
			// h�mta ut sameAs (0n)
			relIx[0] = IX_SAMEAS;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rSameAs, null, ip);
			// h�mta ut visualizes (0n)
			relIx[0] = IX_VISUALIZES;
			ip.setCurrent(relIx, false);
			extractValue(graph, s, rVisualizes, null, ip);

			// h�mta ut diverse data ur en kontext-nod
			// v�rden fr�n kontexten indexeras dels i angivet index och dels i
			// ett index per kontexttyp genom att skicka in ett prefix till ip.setCurrent()
			for (Triple triple: graph.find(s, rContext, AnyObjectNode.ANY_OBJECT_NODE)) {
				if (triple.getObject() instanceof SubjectNode) {
					SubjectNode cS = (SubjectNode) triple.getObject();

					// h�mta ut vilket kontext vi �r i
					// OBS! Anv�nder inte contexttype.rdf f�r uppslagning av denna utan l�gger
					// det uppslagna v�rde i contextLabel
					String contextType = extractSingleValue(graph, cS, rContextType, null);
					if (contextType != null) {
						String contextLabel = lookupURIValue(contextType);
						contextType = restIfStartsWith(contextType, context_pre);
						// TODO: verifiera fr�n lista ist�llet
						if (contextType != null) {
							if (contextType.indexOf("#") >= 0) {
								// b�rjar den inte med r�tt prefix och �r en uri kan vi
								// lika g�rna strunta i den...
								if (logger.isDebugEnabled()) {
									logger.debug("contextType med felaktig uri f�r " + identifier +
											": " + contextType);
								}
								contextType = null;
							} else {
								ip.setCurrent(IX_CONTEXTTYPE);
								ip.addToDoc(contextType);
							}
						}
						if (contextLabel != null) {
							ip.setCurrent(IX_CONTEXTLABEL);
							ip.addToDoc(contextLabel);
							appendToTextBuffer(allText, contextLabel);
						} else {
							ip.setCurrent(IX_CONTEXTLABEL);
							appendToTextBuffer(allText, extractSingleValue(graph, cS, rContextLabel, ip));
						}
					}

					// place

					// 0-m
					ip.setCurrent(IX_PLACENAME, contextType);
					appendToTextBuffer(placeText, extractValue(graph, cS, rPlaceName, null, ip));

					ip.setCurrent(IX_CADASTRALUNIT, contextType);
					extractSingleValue(graph, cS, rCadastralUnit, ip);

					ip.setCurrent(IX_PLACETERMID, contextType);
					extractSingleValue(graph, cS, rPlaceTermId, ip);

					ip.setCurrent(IX_PLACETERMAUTH, contextType);
					extractSingleValue(graph, cS, rPlaceTermAuth, ip);

					ip.setCurrent(IX_CONTINENTNAME, contextType);
					extractSingleValue(graph, cS, rContinentName, ip);

					ip.setCurrent(IX_COUNTRYNAME, contextType);
					appendToTextBuffer(placeText, extractSingleValue(graph, cS, rCountryName, ip));

					ip.setCurrent(IX_COUNTYNAME, contextType);
					appendToTextBuffer(placeText, extractSingleValue(graph, cS, rCountyName, ip));

					ip.setCurrent(IX_MUNICIPALITYNAME, contextType);
					appendToTextBuffer(placeText, extractSingleValue(graph, cS, rMunicipalityName, ip));

					ip.setCurrent(IX_PROVINCENAME, contextType);
					appendToTextBuffer(placeText, extractSingleValue(graph, cS, rProvinceName, ip));

					ip.setCurrent(IX_PARISHNAME, contextType);
					appendToTextBuffer(placeText, extractSingleValue(graph, cS, rParishName, ip));

					ip.setCurrent(IX_COUNTRY, contextType);
					extractSingleValue(graph, cS, rCountry, ip);

					ip.setCurrent(IX_COUNTY, contextType);
					extractSingleValue(graph, cS, rCounty, ip);

					ip.setCurrent(IX_MUNICIPALITY, contextType);
					extractSingleValue(graph, cS, rMunicipality, ip);

					ip.setCurrent(IX_PROVINCE, contextType);
					extractSingleValue(graph, cS, rProvince, ip);

					ip.setCurrent(IX_PARISH, contextType);
					extractSingleValue(graph, cS, rParish, ip);

					// actor

					ip.setCurrent(IX_FIRSTNAME, contextType);
					appendToTextBuffer(actorText, extractSingleValue(graph, cS, rFirstName, ip));

					ip.setCurrent(IX_SURNAME, contextType);
					appendToTextBuffer(actorText, extractSingleValue(graph, cS, rSurname, ip));

					ip.setCurrent(IX_NAME, contextType);
					appendToTextBuffer(actorText, extractSingleValue(graph, cS, rName, ip));

					ip.setCurrent(IX_ORGANIZATION, contextType);
					appendToTextBuffer(actorText, extractSingleValue(graph, cS, rOrganization, ip));

					ip.setCurrent(IX_TITLE, contextType);
					extractSingleValue(graph, cS, rTitle, ip);

					ip.setCurrent(IX_NAMEID, contextType);
					extractSingleValue(graph, cS, rNameId, ip);

					ip.setCurrent(IX_NAMEAUTH, contextType);
					extractSingleValue(graph, cS, rNameAuth, ip);

					// time

					// TODO: acceptera enligt iso 8601 men indexera bara �rtalet(?)
					//       fixa ocks� s� att minus-�r och intervall hanteras
					//       g�ller s�klart b�de fromtime och totime
					ip.setCurrent(IX_FROMTIME, contextType);
					appendToTextBuffer(timeText, extractSingleValue(graph, cS, rFromTime, ip));

					ip.setCurrent(IX_TOTIME, contextType);
					appendToTextBuffer(timeText, extractSingleValue(graph, cS, rToTime, ip));

					ip.setCurrent(IX_FROMPERIODNAME, contextType);
					appendToTextBuffer(timeText, extractSingleValue(graph, cS, rFromPeriodName, ip));

					ip.setCurrent(IX_TOPERIODNAME, contextType);
					appendToTextBuffer(timeText, extractSingleValue(graph, cS, rToPeriodName, ip));

					ip.setCurrent(IX_FROMPERIODID, contextType);
					extractSingleValue(graph, cS, rFromPeriodId, ip);

					ip.setCurrent(IX_TOPERIODID, contextType);
					extractSingleValue(graph, cS, rToPeriodId, ip);

					ip.setCurrent(IX_PERIODAUTH, contextType);
					extractSingleValue(graph, cS, rPeriodAuth, ip);

					ip.setCurrent(IX_EVENTNAME, contextType);
					appendToTextBuffer(timeText, extractSingleValue(graph, cS, rEventName, ip));

					ip.setCurrent(IX_EVENTAUTH, contextType);
					extractSingleValue(graph, cS, rEventAuth, ip);

//					ip.setCurrent(IX_TIMETEXT, contextType);
//					appendToTextBuffer(timeText, extractSingleValue(graph, cS, rTimeText, ip));

					//} else {
					//	logger.warn("Fick ok�nd contextLabel: " + contextLabel);
					//}
				} else {
					logger.warn("context borde vara en blank-nod? Ingen context-info utl�st");
				}
			}

			// nedan f�ljer fritextf�lt - ska analyseras

			// l�gg in "allt" i det stora fritextf�ltet och indexera
			allText.append(itemText).append(" ").append(placeText).append(" ").append(actorText).append(" ").append(timeText);
			luceneDoc.add(new Field(IX_TEXT, allText.toString().trim(), Field.Store.NO, Field.Index.ANALYZED));
			// fritext f�r objekt
			luceneDoc.add(new Field(IX_ITEM, itemText.toString().trim(), Field.Store.NO, Field.Index.ANALYZED));
			// fritext f�r plats
			if (placeText.length() > 0) {
				luceneDoc.add(new Field(IX_PLACE, placeText.toString().trim(), Field.Store.NO, Field.Index.ANALYZED));
			}
			// fritext f�r personer
			if (actorText.length() > 0) {
				luceneDoc.add(new Field(IX_ACTOR, actorText.toString().trim(), Field.Store.NO, Field.Index.ANALYZED));
			}
			// fritext f�r tid
			if (timeText.length() > 0) {
				luceneDoc.add(new Field(IX_TIME, timeText.toString().trim(), Field.Store.NO, Field.Index.ANALYZED));
			}


			// h�mta ut presentationsblocket
			pres = extractSingleValue(graph, s, rPres, null);
			if (pres != null && pres.length() > 0) {
				// verifiera att det �r xml
				// TODO: kontrollera korrekt schema ocks�
				org.w3c.dom.Document doc = parseDocument(pres);
				// serialisera som ett xml-fragment, dvs utan xml-deklaration
				pres = serializeDocumentAsFragment(doc);
				// lagra bin�rt, kodat i UTF-8
				luceneDoc.add(new Field(I_IX_PRES, pres.getBytes("UTF-8"), Field.Store.COMPRESS));
			}

			// TODO: ska detta lagras av lucene, vi kan annars h�mta det fr�n db
			//       ska det lagras av lucene m�ste vi helst h�r se till att det lagras utan
			//       xml-deklaration pss som f�r pres ovan
			//       lagras det i db kan data och index vara i flux...
			//luceneDoc.add(new Field(I_IX_RDF, xmlContent, Field.Store.COMPRESS, Field.Index.NO));
		} catch (Exception e) {
			// TODO: kasta exception/r�kna felen/annat?
			logger.error("Fel vid skapande av lucenedokument f�r " + identifier + ": " + e.getMessage());
			throw e;
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (Exception ignore) {}
			}
			if (graph != null) {
				graph.close();
			}
		}
		return luceneDoc;
	}

	@Override
	public String extractIdentifier(String xmlContent) throws Exception {
		StringReader r = null;
		String identifier = null;
		Graph graph = null;
		try {
			graph = jrdfFactory.getNewGraph();
			GraphRdfXmlParser parser = new GraphRdfXmlParser(graph, new MemMapFactory());
			r = new StringReader(xmlContent);
			parser.parse(r, ""); // baseuri?
	
			GraphElementFactory elementFactory = graph.getElementFactory();
			URIReference rdfType = elementFactory.createURIReference(uri_rdfType);
			URIReference samsokEntity = elementFactory.createURIReference(uri_samsokEntity);
			for (Triple triple: graph.find(AnySubjectNode.ANY_SUBJECT_NODE, rdfType, samsokEntity)) {
				if (identifier != null) {
					throw new Exception("Ska bara finnas en entity");
				}
				identifier = triple.getSubject().toString();
			}
			if (identifier == null) {
				logger.error("Kunde inte extrahera identifierare ur rdf-grafen:\n" + xmlContent);
				throw new Exception("Kunde inte extrahera identifierare ur rdf-grafen");
			}
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (Exception ignore) {}
			}
			if (graph != null) {
				graph.close();
			}
		}
		return identifier;
	}

	//private String extractValue(Graph graph, SubjectNode s, URIReference ref, URIReference refRef) throws Exception {
	//	return extractValue(graph, s, ref, refRef);
	//}

	// l�ser ut ett v�rde ur subjektnoden eller subjektnodens objektnod om denna �r en subjektnod
	// och l�gger till v�rdet mha indexprocessorn
	private String extractValue(Graph graph, SubjectNode s, URIReference ref, URIReference refRef, IndexProcessor ip) throws Exception {
		final String sep = " ";
		StringBuffer buf = new StringBuffer();
		String value;
		for (Triple t: graph.find(s, ref, AnyObjectNode.ANY_OBJECT_NODE)) {
			if (t.getObject() instanceof Literal) {
				Literal l = (Literal) t.getObject();
				if (buf.length() > 0) {
					buf.append(sep);
				}
				value = l.getValue().toString();
				buf.append(value);
				if (ip != null) {
					ip.addToDoc(value);
				}
			} else if (t.getObject() instanceof URIReference) {
				value = getReferenceValue((URIReference) t.getObject(), ip);
				// l�gg till i buffer bara om detta �r en uri vi ska sl� upp v�rde f�r
				if (value != null && ip != null && ip.translateURI()) {
					if (buf.length() > 0) {
						buf.append(sep);
					}
					buf.append(value);
				}
				if (ip != null) {
					ip.addToDoc(value);
				}
			} else if (refRef != null && t.getObject() instanceof SubjectNode) {
				SubjectNode resSub = (SubjectNode) t.getObject();
				value = extractSingleValue(graph, resSub, refRef, ip);
				if (value != null) {
					if (buf.length() > 0) {
						buf.append(sep);
					}
					buf.append(value);
				}
			}
		}
		return buf.length() > 0 ? StringUtils.trimToNull(buf.toString()) : null;
	}

	// l�ser ut ett enkelt v�rde ur subjektoden d�r objektnoden m�ste vara en literal eller en uri-referens
	// och l�gger till det mha indexprocessorn
	private String extractSingleValue(Graph graph, SubjectNode s, PredicateNode p, IndexProcessor ip) throws Exception {
		String value = null;
		for (Triple t: graph.find(s, p, AnyObjectNode.ANY_OBJECT_NODE)) {
			if (value != null) {
				throw new Exception("Fler v�rden �n ett f�r s: " + s + " p: " + p);
			}
			if (t.getObject() instanceof Literal) {
				value = StringUtils.trimToNull(((Literal) t.getObject()).getValue().toString());
			} else if (t.getObject() instanceof URIReference) {
				value = getReferenceValue((URIReference) t.getObject(), ip);
			} else {
				throw new Exception("M�ste vara literal/urireference o.class: " + t.getObject().getClass().getSimpleName() + " f�r s: " + s + " p: " + p);
			}
			if (ip != null) {
				ip.addToDoc(value);
			}
		}
		return value;
	}

	// f�rs�ker �vers�tta ett uri-v�rde till ett f�rinl�st v�rde
	private String getReferenceValue(URIReference ref, IndexProcessor ip) {
		String value = null;
		String uri = ref.getURI().toString();
		String lookedUpValue;
		// se om vi ska f�rs�ka ers�tta uri:n med en uppslagen text
		if (ip != null && ip.translateURI() && (lookedUpValue = lookupURIValue(uri)) != null) {
			value = lookedUpValue;
		} else {
			value = StringUtils.trimToNull(uri);
		}
		return value;
	}

	// l�gger till ett index till lucene-dokumentet
	private static boolean addToDoc(Document luceneDoc, String fieldName, String value,
			Field.Store storeFlag, Field.Index indexFlag) {
		String trimmedValue = StringUtils.trimToNull(value);
		if (trimmedValue != null) {
			if (isToLowerCaseIndex(fieldName)) {
				trimmedValue = trimmedValue.toLowerCase();
			} else if (isISO8601DateYearIndex(fieldName)) {
				trimmedValue = parseYearFromISO8601DateAndTransform(trimmedValue);
				if (trimmedValue == null) {
					addProblemMessage("Kunde inte tolka datumv�rde enligt iso8601 f�r f�lt: " +
							fieldName + " (" + value + ")");
				}
			}
			if (trimmedValue != null) {
				luceneDoc.add(new Field(fieldName, trimmedValue, storeFlag, indexFlag));
			}
		}
		return trimmedValue != null;
	}

	private static String parseYearFromISO8601DateAndTransform(String value) {
		String result = null;
		try {
			// tills vidare godk�nner vi ocks� "x f.kr" och "y e.kr" skiftl�gesok�nsligt h�r
			int dotkrpos = value.toLowerCase().indexOf(".kr");
			int year;
			if (dotkrpos > 0) {
				year = Integer.parseInt(value.substring(0, dotkrpos - 1).trim());
				switch (value.charAt(dotkrpos - 1)) {
				case 'F':
				case 'f':
					year = -year + 1; // 1 fkr �r �r 0
					// obs - inget break
				case 'E':
				case 'e':
					result = transformNumberToLuceneString(year);
					break;
					default:
						// m�ste vara f eller e f�r att s�tta result
				}
			} else {
				DateTime dateTime = isoDateTimeFormatter.parseDateTime(value);
				year = dateTime.getYear();
				result = transformNumberToLuceneString(year);
			}
			
		} catch (Exception ignore) {
			// l�ggs till som "problem" i addToDoc om denna metod returnerar null
		}
		return result;
	}

	// hj�lpmetod som l�gger till text till en textbuffer
	private void appendToTextBuffer(StringBuffer buffer, String text) {
		if (text == null || text.length() == 0) {
			return;
		}
		if (buffer.length() > 0) {
			buffer.append(" ");
		}
		buffer.append(text);
	}

	// hj�lpmetod som parsar ett xml-dokument och ger en dom tillbaka
	private org.w3c.dom.Document parseDocument(String xmlContent) throws Exception {
        // records m�ste matcha schema
        //xmlFact.setSchema(schema);
        DocumentBuilder builder = xmlFact.newDocumentBuilder();
        StringReader sr = null;
        org.w3c.dom.Document doc;
        try {
        	sr = new StringReader(xmlContent);
        	doc = builder.parse(new InputSource(sr));
        } finally {
        	if (sr != null) {
        		try {
        			sr.close();
        		} catch (Exception ignore) {
				}
        	}
        }
        return doc;
	}

	// hj�lpmetod som serialiserar ett dom-tr�d som xml utan xml-deklaration
	private String serializeDocumentAsFragment(org.w3c.dom.Document doc) throws Exception {
		// TODO: anv�nd samma Transformer f�r en hel serie, kr�ver refaktorering
		//       av hur ContentHelpers anv�nds map deras livscykel
		final int initialSize = 4096;
		Source source = new DOMSource(doc);
		Transformer xformer = xformerFact.newTransformer();
		// ingen xml-deklaration d� vi vill anv�nda den som ett xml-fragment
		xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		StringWriter sw = new StringWriter(initialSize);
		Result result = new StreamResult(sw);
        xformer.transform(source, result);
        sw.close();
		return sw.toString();
	}

	// klass som hanterar indexprocessning (�vers�ttning av v�rden, hur de lagras av lucene etc) 
	private static class IndexProcessor {
		final Document doc;
		String[] indexNames;
		Field.Store[] stores;
		Field.Index[] indices;
		boolean lookupURI;
		IndexProcessor(Document doc) {
			this.doc = doc;
		}
		
		/**
		 * S�tter vilket index vi jobbar med fn, och ev ocks� ett prefix f�r att hantera
		 * kontext-index. V�rdet i indexet kommer ocks� lagras i ett kontext-index.
		 * 
		 * @param indexName indexnamn
		 * @param contextPrefix kontext-prefix eller null
		 */
		void setCurrent(String indexName, String contextPrefix) {
			if (contextPrefix != null) {
				Field.Index[] indices = new Field.Index[2];
				indices[0] = isAnalyzedIndex(indexName) ? Field.Index.ANALYZED : Field.Index.NOT_ANALYZED;
				indices[1] = indices[0];
				setCurrent(new String[] {indexName, contextPrefix + "_" + indexName },
						new Field.Store[] {Field.Store.NO, Field.Store.NO}, indices, true);
			} else {
				setCurrent(indexName);
			}
		}

		/**
		 * S�tter vilket index vi jobbar med fn.
		 * 
		 * @param indexName indexnamn
		 */
		void setCurrent(String indexName) {
			setCurrent(indexName, Field.Store.NO);
		}

		/**
		 * S�tter vilket index vi jobbar med fn. V�rdet kommer att l�ggas
		 * in i alla angivna index.
		 * 
		 * @param indexNames indexnamns
		 * @param lookupURI om uri-v�rde ska sl�s upp
		 */
		void setCurrent(String[] indexNames, boolean lookupURI) {
			Field.Store[] stores = new Field.Store[indexNames.length];
			for (int i = 0; i < indexNames.length; ++i) {
				stores[i] = Field.Store.NO;
			}
			setCurrent(indexNames, stores, lookupURI);
		}

		/**
		 * S�tter vilket index vi jobbar med fn och hur/om det ska lagras av lucene.
		 * 
		 * @param indexName indexnamn
		 * @param store lucene-konstant f�r lagring
		 */
		void setCurrent(String indexName, Field.Store store) {
			setCurrent(indexName, store, true);
		}

		/**
		 * S�tter vilket index vi jobbar med fn, hur/om det ska lagras av lucene och
		 * om uri-v�rden ska sl�s upp.
		 * 
		 * @param indexName indexnamn
		 * @param store lucene-konstant f�r lagring
		 * @param lookupURI om uriv�rde ska sl�s upp
		 */
		void setCurrent(String indexName, Field.Store store, boolean lookupURI) {
			setCurrent(new String[] {indexName }, new Field.Store[] { store }, lookupURI);
		}

		/**
		 * S�tter vilka index vi jobbar med fn, hur/om dessa ska lagras av lucene och
		 * om uri-v�rden ska sl�s upp.
		 * 
		 * @param indexNames indexnamn
		 * @param stores lagringskonstanter
		 * @param lookupURI om v�rden ska sl�s upp
		 */
		void setCurrent(String[] indexNames, Field.Store[] stores, boolean lookupURI) {
			Field.Index[] indices = new Field.Index[indexNames.length];
			for (int i = 0; i < indexNames.length; ++i) {
				indices[i] = isAnalyzedIndex(indexNames[i]) ? Field.Index.ANALYZED : Field.Index.NOT_ANALYZED;
			}
			setCurrent(indexNames, stores, indices, lookupURI);
		}

		/**
		 * S�tter vilka index vi jobbar med fn, hur/om dessa ska lagras av lucene, vilken
		 * typ (lucene) av index dessa �r och om uri-v�rden ska sl�s upp.
		 * 
		 * @param indexNames indexnamn
		 * @param stores lagringskonstanter
		 * @param indices lucene-indextyp
		 * @param lookupURI om v�rden ska sl�s upp
		 */
		void setCurrent(String[] indexNames, Field.Store[] stores, Field.Index[] indices, boolean lookupURI) {
			this.indexNames = indexNames;
			this.stores = stores;
			this.indices = indices;
			this.lookupURI = lookupURI;
		}

		/**
		 * L�gger till v�rdet till lucenedokumentet f�r akutellt index.
		 * 
		 * @param value v�rde
		 */
		void addToDoc(String value) {
			for (int i = 0; i < indexNames.length; ++i) {
				SamsokContentHelper.addToDoc(doc, indexNames[i], value, stores[i], indices[i]);
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
	}

	// l�ser in en rdf-resurs och lagrar uri-v�rden och �vers�ttningsv�rden f�r uppslagning
	// alla resurser f�ruts�tts vara kodade i utf-8 och att v�rdena �r Literals
	private static void readURIValueResource(String fileName, URI predicateURI) {
		Reader r = null;
		Graph graph = null;
		try {
			r = new InputStreamReader(SamsokContentHelper.class.getResourceAsStream(PATH + fileName), "UTF-8");
			graph = jrdfFactory.getNewGraph();
			GraphRdfXmlParser parser = new GraphRdfXmlParser(graph, new MemMapFactory());
			parser.parse(r, ""); // baseuri?
			GraphElementFactory elementFactory = graph.getElementFactory();
			URIReference rName = elementFactory.createURIReference(predicateURI);
			int c = 0;
			for(Triple t: graph.find(AnySubjectNode.ANY_SUBJECT_NODE, rName, AnyObjectNode.ANY_OBJECT_NODE)) {
				uriValues.put(t.getSubject().toString(), StringUtils.trimToNull(((Literal) t.getObject()).getValue().toString()));
				++c;
			}
			if (logger.isInfoEnabled()) {
				logger.info("L�ste in " + c + " uris/v�rden fr�n " + PATH + fileName);
			}
		} catch (Exception e) {
			throw new RuntimeException("Problem att l�sa in uri-�vers�ttningsfil " +
					fileName, e);
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (Exception ignore) {
				}
			}
			if (graph != null) {
				graph.close();
			}
		}
	}

	// f�rs�ker sl� upp uri-v�rde mha m�ngden inl�sta v�rden
	// f�r geografi-uri:s tas fn bara koden (url-suffixet)
	// om ett v�rde inte hittas lagras det i "problem"-loggen
	private static String lookupURIValue(String uri) {
		String value = uriValues.get(uri);
		// TODO: padda med nollor eller strippa de med inledande nollor?
		//       det kommer ju garanterat bli fel i n�gon �nda... :)
		//       UPDATE: ser ut som om det �r strippa nollor som g�ller d�
		//       cql-parsern(?) verkar tolka saker som siffror om de inte quotas
		//       med "", ex "01"
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, aukt_county_pre, true);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, aukt_municipality_pre, true);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, aukt_province_pre);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, aukt_parish_pre, true);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, aukt_country_pre);
		if (value != null) {
			return value;
		}
		// l�gg in i thread local som ett problem
		addProblemMessage("Inget v�rde f�r " + uri);
		return null;
	}

	// hj�lpmetod som tar ut suffixet ur str�ngen om den startar med inskickad startstr�ng
	private static String restIfStartsWith(String str, String start) {
		return restIfStartsWith(str, start, false);
	}

	// hj�lpmetod som tar ut suffixet ur str�ngen om den startar med inskickad startstr�ng
	// och f�rs�ker tolka v�rdet som en str�ng om asNumber �r sant
	private static String restIfStartsWith(String str, String start, boolean asNumber) {
		String value = null;
		if (str.startsWith(start)) {
			value = str.substring(start.length());
			if (asNumber) {
				try {
					value = Long.valueOf(value).toString();
				} catch (NumberFormatException nfe) {
					addProblemMessage("Kunde inte tolka slutet av " + str + " (" + value + ") som en siffra");
				}
			}
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		SamsokContentHelper sch = new SamsokContentHelper();
		HarvestService dummyService = new se.raa.ksamsok.harvest.HarvestServiceImpl();
		dummyService.setId("dummy");
		StringBuffer buf = new StringBuffer();
		java.io.File f = new java.io.File("d:/temp/utvnod_en_post.rdf");
		java.io.BufferedReader r = null;
		try {
			r = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
			String line;
			while ((line = r.readLine()) != null) {
				buf.append(line).append('\n');
			}
			String xmlContent = buf.toString();
			Document d = sch.createLuceneDocument(dummyService, xmlContent);
			java.util.List<Field> fields = d.getFields();
			System.out.println("---- doc ----");
			for (Field field: fields) {
				System.out.println(field);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (Exception ignore) {}
			}
		}
	}
}
