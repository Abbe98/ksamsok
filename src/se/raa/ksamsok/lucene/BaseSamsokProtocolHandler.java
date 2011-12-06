package se.raa.ksamsok.lucene;

import static se.raa.ksamsok.lucene.ContentHelper.IX_ADDEDTOINDEXDATE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CADASTRALUNIT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COLLECTION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTLABEL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTTYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTINENTNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COUNTRY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COUNTRYNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COUNTY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COUNTYNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CREATEDDATE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_DATAQUALITY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_EVENTAUTH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_EVENTNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FIRSTNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FROMPERIODID;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FROMPERIODNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FROMTIME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FULLNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_GENDER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_GEODATAEXISTS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMCLASS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMCLASSNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMCOLOR;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMDESCRIPTION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMKEYWORD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMLABEL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMLICENSE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMMATERIAL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMMOTIVEWORD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMNUMBER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMSPECIFICATION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMSTYLE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMTECHNIQUE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMTITLE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMTYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_LASTCHANGEDDATE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIALICENSE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIAMOTIVEWORD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIATYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MUNICIPALITY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MUNICIPALITYNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_NAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_NAMEAUTH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_NAMEID;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ORGANIZATION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PARISH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PARISHNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PERIODAUTH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PLACENAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PLACETERMAUTH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PLACETERMID;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PROVINCE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PROVINCENAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_RELURI;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SERVICENAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SERVICEORGANISATION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SUBJECT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SURNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_THEME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_THUMBNAILEXISTS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TIMEINFOEXISTS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TITLE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOPERIODID;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOPERIODNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOTIME;
import static se.raa.ksamsok.lucene.ContentHelper.addProblemMessage;
import static se.raa.ksamsok.lucene.ContentHelper.formatDate;
import static se.raa.ksamsok.lucene.RDFUtil.extractSingleValue;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.context_pre;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCadastralUnit;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCollection;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContext;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContextLabel;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContextType;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContinentName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCoordinates;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCountry;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCountryName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCounty;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCountyName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCreatedDate;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rDataQuality;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rEventAuth;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rEventName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFirstName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFromPeriodId;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFromPeriodName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFromTime;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFullName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rGender;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rImage;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemClass;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemClassName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemColor;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemDescription;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemKeyWord;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemLabel;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemLicense;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemMaterial;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemMotiveWord;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemNumber;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemSpecification;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemStyle;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemTechnique;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemTitle;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemType;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rLastChangedDate;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMaterial;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaLicense;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaMotiveWord;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaType;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMunicipality;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMunicipalityName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rNameAuth;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rNameId;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rNumber;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rOrganization;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rParish;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rParishName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rPeriodAuth;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rPlaceName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rPlaceTermAuth;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rPlaceTermId;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rProvince;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rProvinceName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rServiceName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rServiceOrganization;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rSubject;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rSurname;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rTheme;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rThumbnail;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rTitle;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToPeriodId;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToPeriodName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToTime;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Desc;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Name;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Spec;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.jrdf.graph.AnyObjectNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.GraphException;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;

import se.raa.ksamsok.harvest.HarvestService;

public abstract class BaseSamsokProtocolHandler implements SamsokProtocolHandler, RelationToIndexMapper {

	protected final Logger logger;

	protected final Graph graph;
	protected final GraphElementFactory elementFactory;
	protected final SubjectNode s;
	protected final IndexProcessor ip;
	protected final SolrInputDocument luceneDoc;

	/**
	 * Map som h�ller ev �terkommande uri:er och uri-referenser f�r att slippa
	 * skapa dem flera g�nger eller ha variabler
	 */
	protected Map<URI, URIReference> mapper = new HashMap<URI, URIReference>();

	protected boolean timeInfoExists = false;
	protected boolean geoDataExists = false;

	protected BaseSamsokProtocolHandler(Graph graph, SubjectNode s) {
		logger = getLogger();
		this.graph = graph;
		this.elementFactory = graph.getElementFactory();
		this.s = s;
		this.luceneDoc = new SolrInputDocument();
		this.ip = new IndexProcessor(luceneDoc, getURIValues(), this);
	}

	/**
	 * Ger map med v�rden nycklade p� uri.
	 * 
	 * @return map men uri/v�rde-par
	 */
	protected abstract Map<String,String> getURIValues();

	@Override
	public String lookupURIValue(String uri) {
		return getURIValues().get(uri);
	}

	/**
	 * Skapar en URIReference f�r aktuell graf och cachar den.
	 * @param elementFactory factory
	 * @param uri uri
	 * @return en URIReference
	 * @throws GraphException vid fel
	 */
	protected URIReference getURIRef(GraphElementFactory elementFactory, URI uri) throws GraphException {
		URIReference ref = mapper.get(uri);
		if (ref == null) {
			ref = elementFactory.createURIReference(uri);
			mapper.put(uri, ref);
		}
		return ref;
	}

	@Override
	public SolrInputDocument handle(HarvestService service, Date added,
			List<String> relations, List<String> gmlGeometries)
			throws Exception {

		String identifier = s.toString();

		extractServiceInformation();

		// ta hand om "system"-datum
		extractAndHandleIndexDates(identifier, service, added);

		// item
		extractItemInformation();

		// klassificeringar
		extractClassificationInformation();

		// extrahera topniv�-relationer (objekt-objekt, ej i kontext)
		extractTopLevelRelations(relations);

		// h�mta ut diverse data ur en kontext-nod
		// v�rden fr�n kontexten indexeras dels i angivet index och dels i
		// ett index per kontexttyp genom att skicka in ett prefix till ip.setCurrent()
		extractContextNodes(identifier, relations, gmlGeometries);

		// l�s in v�rden fr�n Image-noder
		extractImageNodes();

		// l�gg in specialindex
		addSpecialIndices();

		return luceneDoc;
	}

	/**
	 * Extraherar och indexerar information som ber�r tj�nsten posten kommer ifr�n.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void extractServiceInformation() throws Exception {
		ip.setCurrent(IX_SERVICENAME);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rServiceName), ip);
		// h�mta ut serviceOrganization (01, fast 11 egentligen?)
		ip.setCurrent(IX_SERVICEORGANISATION);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rServiceOrganization), ip);
	}

	/**
	 * Extraherar och indexerar specialindex, dvs "exists"-index.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void addSpecialIndices() throws Exception {
		// l�gg in specialindex
		luceneDoc.addField(IX_GEODATAEXISTS, geoDataExists ? "j" : "n");
		luceneDoc.addField(IX_TIMEINFOEXISTS, timeInfoExists ? "j" : "n");
		// l�gg till specialindex f�r om tumnagel existerar eller ej (j/n), IndexType.TOLOWERCASE
		boolean thumbnailExists = extractSingleValue(graph, s, getURIRef(elementFactory, uri_rThumbnail), null) != null;
		luceneDoc.addField(IX_THUMBNAILEXISTS, thumbnailExists ? "j" : "n");

	}

	/**
	 * Extraherar, ev behandlar och indexerar indexdatum.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param identifier identifierare
	 * @param service tj�nst
	 * @param added datum n�r posten f�rst lades till
	 * @throws Exception vid fel
	 */
	protected void extractAndHandleIndexDates(String identifier, HarvestService service, Date added) throws Exception {
		// h�mta ut createdDate (01, fast 11 egentligen? speciellt om man vill ha ut info
		// om nya objekt i indexet)
		Date created = null;
		String createdDate = extractSingleValue(graph, s, getURIRef(elementFactory, uri_rCreatedDate), null);
		if (createdDate != null) {
			created = TimeUtil.parseAndIndexISO8601DateAsDate(identifier, IX_CREATEDDATE, createdDate, ip);
		} else {
			addProblemMessage("Value for '" + IX_CREATEDDATE +
					// troligen saknas det p� alla s� identifier inte med tillsvidare
					"' is missing"); //  f�r " + identifier);
		}
		// lite logik f�r att s�tta datum d� posten f�rst lades till i indexet
		// i normala fall �r added != null d� den s�tts n�r poster l�ggs till, men
		// det kan finnas gammalt data i repot som inte har n�t v�rde och d� anv�nder
		// vi den gamla logiken fr�n innan databaskolumnen added fanns
		Date addedToIndex = added != null ? added: calculateAddedToIndex(service.getFirstIndexDate(), created);
		ip.setCurrent(IX_ADDEDTOINDEXDATE);
		ip.addToDoc(formatDate(addedToIndex, false));
		// h�mta ut lastChangedDate (01, fast 11 egentligen?)
		String lastChangedDate = extractSingleValue(graph, s, getURIRef(elementFactory, uri_rLastChangedDate), null);
		if (lastChangedDate != null) {
			TimeUtil.parseAndIndexISO8601DateAsDate(identifier, IX_LASTCHANGEDDATE, lastChangedDate, ip);
		} else {
			// lastChanged �r inte lika viktig som createdDate s� den varnar vi inte f�r tills vidare
			// addProblemMessage("V�rde f�r '" + IX_LASTCHANGEDDATE +
			//		"' saknas f�r " + identifier);
		}
	}

	/**
	 * Extraherar och indexerar information som ber�r klassificeringar.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void extractClassificationInformation() throws Exception {
		// TODO: subject inte r�tt, �r bara en uri-pekare nu(?)
		// h�mta ut subject (0m)
		ip.setCurrent(IX_SUBJECT);
		extractValue(graph, s, getURIRef(elementFactory, uri_rSubject), null, ip);
		// h�mta ut collection (0m)
		ip.setCurrent(IX_COLLECTION);
		extractValue(graph, s, getURIRef(elementFactory, uri_rCollection), null, ip);
		// h�mta ut dataQuality (1)
		ip.setCurrent(IX_DATAQUALITY);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rDataQuality), ip);
		// h�mta ut mediaType (0n)
		ip.setCurrent(IX_MEDIATYPE);
		extractValue(graph, s, getURIRef(elementFactory, uri_rMediaType), null, ip);
		// h�mta ut tema (0n)
		ip.setCurrent(IX_THEME);
		extractValue(graph, s, getURIRef(elementFactory, uri_rTheme), null, ip);
	}

	/**
	 * Extraherar och indexerar information som ber�r "item"-index, dvs huvuddata.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void extractItemInformation() throws Exception {
		// h�mta ut itemTitle (0m)
		ip.setCurrent(IX_ITEMTITLE);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemTitle), null, ip);
		// h�mta ut itemLabel (11)
		ip.setCurrent(IX_ITEMLABEL);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rItemLabel), ip);
		// h�mta ut itemType (1)
		ip.setCurrent(IX_ITEMTYPE);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rItemType), ip);
		// h�mta ut itemClass (0m)
		ip.setCurrent(IX_ITEMCLASS, false); // sl� inte upp uri
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemClass), null, ip);
		// h�mta ut itemClassName (0m)
		ip.setCurrent(IX_ITEMCLASSNAME);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemClassName), null, ip);
		// h�mta ut itemName (1m)
		ip.setCurrent(IX_ITEMNAME);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemName), getURIRef(elementFactory, uri_r__Name), ip);
		// h�mta ut itemSpecification (0m)
		ip.setCurrent(IX_ITEMSPECIFICATION);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemSpecification), getURIRef(elementFactory, uri_r__Spec), ip);
		// h�mta ut itemKeyWord (0m)
		ip.setCurrent(IX_ITEMKEYWORD);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemKeyWord), null, ip);
		// h�mta ut itemMotiveWord (0m)
		ip.setCurrent(IX_ITEMMOTIVEWORD);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemMotiveWord), null, ip);
		// h�mta ut itemMaterial (0m)
		ip.setCurrent(IX_ITEMMATERIAL);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemMaterial), getURIRef(elementFactory, uri_rMaterial), ip);
		// h�mta ut itemTechnique (0m)
		ip.setCurrent(IX_ITEMTECHNIQUE);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemTechnique), null, ip);
		// h�mta ut itemStyle (0m)
		ip.setCurrent(IX_ITEMSTYLE);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemStyle), null, ip);
		// h�mta ut itemColor (0m)
		ip.setCurrent(IX_ITEMCOLOR);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemColor), null, ip);
		// h�mta ut itemNumber (0m)
		ip.setCurrent(IX_ITEMNUMBER);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemNumber), getURIRef(elementFactory, uri_rNumber), ip);
		// h�mta ut itemDescription, resursnod (0m)
		ip.setCurrent(IX_ITEMDESCRIPTION); // fritext
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemDescription), getURIRef(elementFactory, uri_r__Desc), ip);
		// h�mta ut itemLicense (01)
		ip.setCurrent(IX_ITEMLICENSE, false); // uri, ingen uppslagning fn
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rItemLicense), ip);
	}

	/**
	 * Tar bildnoder och extraherar och indexerar information ur dem.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void extractImageNodes() throws Exception {
		// l�s in v�rden fr�n Image-noder
		for (Triple triple: graph.find(s, getURIRef(elementFactory, uri_rImage), AnyObjectNode.ANY_OBJECT_NODE)) {
			if (triple.getObject() instanceof SubjectNode) {
				SubjectNode cS = (SubjectNode) triple.getObject();

				extractImageNodeInformation(cS);
			}
		}
	}

	/**
	 * Extraherar och indexerar information ur en bildnod.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param cS bildnod
	 * @throws Exception vid fel
	 */
	protected void extractImageNodeInformation(SubjectNode cS) throws Exception {
		ip.setCurrent(IX_MEDIALICENSE, false); // uri, ingen uppslagning fn
		extractValue(graph, cS, getURIRef(elementFactory, uri_rMediaLicense), null, ip);
		ip.setCurrent(IX_MEDIAMOTIVEWORD);
		extractValue(graph, cS, getURIRef(elementFactory, uri_rMediaMotiveWord), null, ip);
	}

	/**
	 * Tar kontextnoder och extraherar och indexerar information ur dem.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param identifier identifierare
	 * @param relations relationslista
	 * @param gmlGeometries gml-lista
	 * @throws Exception vid fel
	 */
	protected void extractContextNodes(String identifier, List<String> relations, List<String> gmlGeometries) throws Exception {
		for (Triple triple: graph.find(s, getURIRef(elementFactory, uri_rContext), AnyObjectNode.ANY_OBJECT_NODE)) {
			if (triple.getObject() instanceof SubjectNode) {
				SubjectNode cS = (SubjectNode) triple.getObject();

				extractContextNodeInformation(cS, identifier, relations, gmlGeometries);
			} else {
				logger.warn("context borde vara en blank-nod? Ingen context-info utl�st");
			}
		}
	}

	/**
	 * Extraherar och indexerar information ur en kontextnod.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param identifier identifierare
	 * @param relations relationslista
	 * @param gmlGeometries gml-lista
	 * @throws Exception vid fel
	 */
	protected void extractContextNodeInformation(SubjectNode cS, String identifier, List<String> relations, List<String> gmlGeometries) throws Exception {
		// h�mta ut vilket kontext vi �r i mm
		String[] contextTypes = extractContextTypeAndLabelInformation(cS, identifier);
		// place
		extractContextPlaceInformation(cS, contextTypes, gmlGeometries);
		// actor
		extractContextActorInformation(cS, contextTypes, relations);
		// time
		extractContextTimeInformation(cS, contextTypes);
	}

	/**
	 * Extraherar och indexerar typinformation ur en kontextnod.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param identifier identifierare
	 * @return kontexttyp, kortnamn
	 * @throws Exception vid fel
	 */
	protected String[] extractContextTypeAndLabelInformation(SubjectNode cS, String identifier) throws Exception {
		// h�mta ut vilket kontext vi �r i
		// OBS! Anv�nder inte contexttype.rdf f�r uppslagning av denna utan l�gger
		// det uppslagna v�rde i contextLabel
		String contextType = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContextType), null);
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
			} else {
				ip.setCurrent(IX_CONTEXTLABEL);
				extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContextLabel), ip);
			}
		}
		return contextType != null ? new String[] { contextType } : null;
	}

	/**
	 * Extraherar och indexerar platsinformation ur en kontextnod.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param contextTypes kontexttypnamn
	 * @throws Exception vid fel
	 */
	protected void extractContextPlaceInformation(SubjectNode cS, String[] contextTypes, List<String> gmlGeometries) throws Exception {
		// place

		// 0-m
		ip.setCurrent(IX_PLACENAME, contextTypes);
		extractValue(graph, cS, getURIRef(elementFactory, uri_rPlaceName), null, ip);

		ip.setCurrent(IX_CADASTRALUNIT, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCadastralUnit), ip);

		ip.setCurrent(IX_PLACETERMID, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rPlaceTermId), ip);

		ip.setCurrent(IX_PLACETERMAUTH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rPlaceTermAuth), ip);

		ip.setCurrent(IX_CONTINENTNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContinentName), ip);

		ip.setCurrent(IX_COUNTRYNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCountryName), ip);

		ip.setCurrent(IX_COUNTYNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCountyName), ip);

		ip.setCurrent(IX_MUNICIPALITYNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rMunicipalityName), ip);

		ip.setCurrent(IX_PROVINCENAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rProvinceName), ip);

		ip.setCurrent(IX_PARISHNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rParishName), ip);

		ip.setCurrent(IX_COUNTRY, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCountry), ip);

		ip.setCurrent(IX_COUNTY, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCounty), ip);

		ip.setCurrent(IX_MUNICIPALITY, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rMunicipality), ip);

		ip.setCurrent(IX_PROVINCE, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rProvince), ip);

		ip.setCurrent(IX_PARISH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rParish), ip);

		// h�mta ut gml
		String gml = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCoordinates), null);
		if (gml != null && gml.length() > 0) {
			gmlGeometries.add(gml);
			// flagga att det finns geodata
			geoDataExists = true;
		}
	}

	/**
	 * Extraherar och indexerar agentinformation ur en kontextnod.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param contextTypes kontexttypnamn
	 * @throws Exception vid fel
	 */
	protected void extractContextActorInformation(SubjectNode cS, String[] contextTypes, List<String> relations) throws Exception {
		// actor

		ip.setCurrent(IX_FIRSTNAME, contextTypes);
		String firstName = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFirstName), ip);

		ip.setCurrent(IX_SURNAME, contextTypes);
		String lastName = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rSurname), ip);

		ip.setCurrent(IX_FULLNAME, contextTypes);
		String fullName = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFullName), ip);

		// om vi inte har f�tt ett fullName men har ett f�rnamn och ett efternamn s� l�gger vi in det i IX_FULLNAME
		if (fullName == null && firstName != null && lastName != null) {
			ip.setCurrent(IX_FULLNAME, contextTypes);
			ip.addToDoc(firstName + " " + lastName);
		}

		ip.setCurrent(IX_NAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rName), ip);

		// TODO: bara vissa v�rden? http://xmlns.com/foaf/spec/#term_gender:
		// "In most cases the value will be the string 'female' or 'male' (in
		//  lowercase without surrounding quotes or spaces)."
		ip.setCurrent(IX_GENDER, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rGender), ip);

		ip.setCurrent(IX_ORGANIZATION, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rOrganization), ip);

		ip.setCurrent(IX_TITLE, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rTitle), ip);

		ip.setCurrent(IX_NAMEID, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rNameId), ip);

		ip.setCurrent(IX_NAMEAUTH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rNameAuth), ip);
	}

	/**
	 * Extraherar och indexerar tidsinformation ur en kontextnod.
	 * Hanterar de index som g�llde f�r protokollversioner till och med 1.0, se dok.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param contextTypes kontexttypnamn
	 * @throws Exception vid fel
	 */
	protected void extractContextTimeInformation(SubjectNode cS, String[] contextTypes) throws Exception {
		// time

		ip.setCurrent(IX_FROMTIME, contextTypes);
		String fromTime = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFromTime), ip);

		ip.setCurrent(IX_TOTIME, contextTypes);
		String toTime = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rToTime), ip);

		// hantera ? i tidsf�lten
		if (fromTime != null && fromTime.startsWith("?")) {
			fromTime = null;
		}
		if (toTime != null && toTime.startsWith("?")) {
			toTime = null;
		}
		// flagga om vi har tidsinfo
		if (fromTime != null || toTime != null) {
			timeInfoExists = true;
		}

		// hantera �rtionden och �rhundraden
		TimeUtil.expandDecadeAndCentury(fromTime, toTime, contextTypes, ip);

		ip.setCurrent(IX_FROMPERIODNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFromPeriodName), ip);

		ip.setCurrent(IX_TOPERIODNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rToPeriodName), ip);

		ip.setCurrent(IX_FROMPERIODID, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFromPeriodId), ip);

		ip.setCurrent(IX_TOPERIODID, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rToPeriodId), ip);

		ip.setCurrent(IX_PERIODAUTH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rPeriodAuth), ip);

		ip.setCurrent(IX_EVENTNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rEventName), ip);

		ip.setCurrent(IX_EVENTAUTH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rEventAuth), ip);
	}

	/**
	 * Ger map med giltiga toppniv�relationer nycklat p� indexnamn.
	 * 
	 * �verlagra i subklasser vid behov.
	 * @return map med toppniv�relationer
	 */
	protected abstract Map<String, URI> getTopLevelRelationsMap();

	/**
	 * Extraherar och indexerar toppniv�relationer som h�mtas via
	 * {@linkplain #getTopLevelRelationsMap()}.
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param relations lista med relationer f�r specialrelationsindexet
	 * @throws Exception vid fel
	 */
	protected void extractTopLevelRelations(List<String> relations) throws Exception {
		Map<String, URI> relationsMap = getTopLevelRelationsMap();
		extractRelationsFromNode(s, relationsMap, relations);
	}

	/**
	 * Extraherar och indexerar relationer fr�n mappen fr�n inskickad nod.  
	 * �verlagra i subklasser vid behov.
	 * 
	 * @param relations lista med relationer f�r specialrelationsindexet
	 * @throws Exception vid fel
	 */
	protected void extractRelationsFromNode(SubjectNode subjectNode,
			Map<String, URI> relationsMap, List<String> relations) throws Exception {
		// relationer, in i respektive index + i IX_RELURI
		final String[] relIx = new String[] { null, IX_RELURI };
		for (Entry<String, URI> entry: relationsMap.entrySet()) {
			relIx[0] = entry.getKey();
			ip.setCurrent(relIx, false);
			extractValue(graph, subjectNode, getURIRef(elementFactory, entry.getValue()), null, ip, relations);
		}
	}





	/**
	 * Ber�knar n�r posten f�rst lades till indexet, anv�nds f�r att f� fram listningar
	 * p� nya objekt i indexet. H�nsyn tas till n�r tj�nsten f�rst indexerades och n�r
	 * posten skapades f�r att f� fram ett n�gorlunda bra datum, se kommentar nedan.
	 * Ber�knas i praktiken som max(firstIndexed, recordCreated).
	 * 
	 * @param firstIndexed n�r tj�nsten f�rst indexerades i k-sams�k, om k�nt
	 * @param recordCreated n�r tj�nsten s�ger att posten skapades
	 * @return ber�knat datum f�r n�r posten f�rst indexerades i k-sams�k
	 */
	static Date calculateAddedToIndex(Date firstIndexed, Date recordCreated) {
		Date addedToIndex = null;
		if (firstIndexed != null) {
			// tj�nsten har redan indexerats (minst) en g�ng
			if (recordCreated != null) {
				if (recordCreated.after(firstIndexed)) {
					// nytt objekt i redan indexerad tj�nst
					// OBS:  detta datum �r inte riktigt 100% sant egentligen utan det
					//       beror ocks� p� med vilken frekvens tj�nsten sk�rdas, tex
					//       om tj�nsten sk�rdas var tredje dag s� kan det skilja p�
					//       tv� dagar n�r objektet egentligen f�rst d�k upp i indexet
					//       och vilket v�rde som anv�nds, s� fn f�r det ses som en uppskattning
					addedToIndex = recordCreated;
				} else {
					// "gammalt" objekt i redan indexerad tj�nst
					addedToIndex = firstIndexed;
				}
			} else {
				// objekt med ok�nt skapad-datum i redan indexerad tj�nst
				// TODO: s�tta "nu" ist�llet eller �r detta ok? kan vara nytt, kan vara
				//       gammalt men d� vi inte vet kanske det ska f� vara gammalt?
				addedToIndex = firstIndexed;
			}
		} else {
			// ny tj�nst -> nytt objekt
			// TODO: kanske alltid ska ha samma v�rde? kan g� �ver dygngr�ns
			//       egentligen vill man alltid ha datumet d� indexeringen lyckades (= gick
			//       klart ok) men det vet man ju aldrig h�r (varken om eller n�r)...
			addedToIndex = new Date();
		}
		return addedToIndex;
	}

	// hj�lpmetod som tar ut suffixet ur str�ngen om den startar med inskickad startstr�ng
	static String restIfStartsWith(String str, String start) {
		return restIfStartsWith(str, start, false);
	}

	// hj�lpmetod som tar ut suffixet ur str�ngen om den startar med inskickad startstr�ng
	// och f�rs�ker tolka v�rdet som ett heltal om asNumber �r sant
	static String restIfStartsWith(String str, String start, boolean asNumber) {
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

	/**
	 * NOTE: Apache 2.0 licens, s� kopiering �r ok.
	 * 
	 * Returns a Log4J logger configured as the calling class. This ensures copy-paste safe code to get a logger instance,
	 * an ensures the logger is always fetched in a consistent manner. <br>
	 * <b>usage:</b><br>
	 * 
	 * <pre>
	 * private final static Logger log = LoggerHelper.getLogger();
	 * </pre>
	 * 
	 * Since the logger is found by accessing the call stack it is important, that references are static.
	 * <p>
	 * The code is JDK1.4 compatible
	 * 
	 * @since 0.05
	 * @return log4j logger instance for the calling class
	 * @author Kasper B. Graversen
	 */
	public static Logger getClassLogger() {
		final Throwable t = new Throwable();
		t.fillInStackTrace();
		return Logger.getLogger(t.getStackTrace()[1].getClassName());
	}

}
