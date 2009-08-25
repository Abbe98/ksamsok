package se.raa.ksamsok.lucene;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;

import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.spatial.GMLInfoHolder;

/**
 * Basklass f�r inneh�llshantering av tj�nstedata och index samt diverse hj�lpmetoder.
 */
public abstract class ContentHelper {

	// diverse systemtermer

	public static final String CONTEXT_SET_SAMSOK = "samsok";
	public static final String CONTEXT_SET_SAMSOK_IDENTIFIER = "http://kulturarvsdata.se/resurser/contextSets/samsok/1.0/";
	public static final String CONTEXT_SET_REC = "rec";
	public static final String CONTEXT_SET_REC_IDENTIFIER = "info:srw/cql-context-set/2/rec-1.1";
	public static final String IX_REC_IDENTIFIER = "identifier"; // identifierare sru1.2 samma som itemId

	// systeminterna indexnamn

	public static final String I_IX_PRES = "_pres";
	public static final String I_IX_SERVICE = "_service";
	public static final String I_IX_HTML_URL = "_htmlurl";
	public static final String I_IX_MUSEUMDAT_URL = "_museumdaturl";
	public static final String I_IX_LON = "_lon";
	public static final String I_IX_LAT = "_lat";
	//public static final String I_IX_RDF = "_rdf";
	
	// generella

	public static final String IX_TEXT = "text"; // fritext f�r "alla" f�lt
	public static final String IX_SERVICENAME = "serviceName";
	public static final String IX_SERVICEORGANISATION = "serviceOrganization";
	public static final String IX_CREATEDDATE = "createdDate";
	public static final String IX_LASTCHANGEDDATE = "lastChangedDate";
	public static final String IX_ADDEDTOINDEXDATE = "addedToIndexDate"; // special

	// entitet

	public static final String IX_ITEM = "item"; // fritext f�r alla item-f�lt
	public static final String IX_ITEMID = "itemId"; // identifierare, rdf:about
	public static final String IX_SUBJECT = "subject";
	public static final String IX_COLLECTION = "collection";
	public static final String IX_MEDIATYPE = "mediaType";
	public static final String IX_DATAQUALITY = "dataQuality";
	public static final String IX_ITEMTYPE = "itemType";
	public static final String IX_ITEMCLASS = "itemClass";
	public static final String IX_ITEMCLASSNAME = "itemClassName";
	public static final String IX_ITEMNAME = "itemName";
	public static final String IX_ITEMSPECIFICATION = "itemSpecification";
	public static final String IX_ITEMTITLE = "itemTitle";
	public static final String IX_ITEMDESCRIPTION = "itemDescription";
	public static final String IX_ITEMKEYWORD = "itemKeyWord";
	public static final String IX_ITEMMOTIVEWORD = "itemMotiveWord";
	public static final String IX_ITEMMATERIAL = "itemMaterial";
	public static final String IX_ITEMTECHNIQUE = "itemTechnique";
	public static final String IX_ITEMSTYLE = "itemStyle";
	public static final String IX_ITEMCOLOR = "itemColor";
	public static final String IX_ITEMNUMBER = "itemNumber";
	public static final String IX_ITEMLICENSE = "itemLicense";
	public static final String IX_THEME = "theme";
	
	// tider, platser, personer
	//	Sammanhang enligt �ndlig lista. Sammanhanget g�ller f�r tider, platser och personer/organisationer. Listan p� sammanhang hittar du h�r: http://kulturarvsdata.se/resurser/Context
	public static final String IX_CONTEXTLABEL = "contextLabel";
	public static final String IX_CONTEXTTYPE = "contextType";
	
	// tider
	public static final String IX_TIME = "time"; // fritext i alla tidsf�lt
	public static final String IX_FROMTIME = "fromTime";
	public static final String IX_TOTIME = "toTime";
	public static final String IX_FROMPERIODNAME = "fromPeriodName";
	public static final String IX_TOPERIODNAME = "toPeriodName";
	public static final String IX_FROMPERIODID = "fromPeriodId";
	public static final String IX_TOPERIODID = "toPeriodId";
	public static final String IX_PERIODAUTH = "periodAuth";
	public static final String IX_EVENTNAME = "eventName";
	public static final String IX_EVENTAUTH = "eventAuth";
	//public static final String IX_TIMETEXT = "timeText";

	// platser
	public static final String IX_PLACE = "place"; // fritext i alla platsf�lt
	public static final String IX_CONTINENTNAME = "continentName";
	public static final String IX_COUNTRY = "country";
	public static final String IX_COUNTY = "county";
	public static final String IX_PROVINCE = "province";
	public static final String IX_MUNICIPALITY = "municipality";
	public static final String IX_PARISH = "parish";
	public static final String IX_COUNTRYNAME = "countryName";
	public static final String IX_COUNTYNAME = "countyName";
	public static final String IX_PROVINCENAME = "provinceName";
	public static final String IX_MUNICIPALITYNAME = "municipalityName";
	public static final String IX_PARISHNAME = "parishName";
	public static final String IX_PLACENAME = "placeName";
	public static final String IX_CADASTRALUNIT = "cadastralUnit";
	public static final String IX_PLACETERMID = "placeTermId";
	public static final String IX_PLACETERMAUTH = "placeTermAuth";
	//public static final String IX_COORDINATES = "coordinates";

	// personer
	public static final String IX_ACTOR = "actor"; // alla f�lt sammanslagna (ej fritext dock!)
	public static final String IX_FIRSTNAME = "firstName";
	public static final String IX_SURNAME = "surname";
	public static final String IX_NAME = "name";
	public static final String IX_FULLNAME = "fullName";
	public static final String IX_GENDER = "gender";
	public static final String IX_ORGANIZATION = "organization";
	public static final String IX_TITLE = "title";
	public static final String IX_NAMEID = "nameId";
	public static final String IX_NAMEAUTH = "nameAuth";

	// relationer
	public static final String IX_RELURI = "relUri"; // slask-indexet
	public static final String IX_CONTAINSINFORMATIONABOUT = "containsInformationAbout";
	public static final String IX_CONTAINSOBJECT = "containsObject";
	public static final String IX_HASBEENUSEDIN = "hasBeenUsedIn";
	public static final String IX_HASCHILD = "hasChild";
	public static final String IX_HASFIND = "hasFind";
	public static final String IX_HASIMAGE = "hasImage";
	public static final String IX_HASOBJECTEXAMPLE = "hasObjectExample";
	public static final String IX_HASPARENT = "hasParent";
	public static final String IX_HASPART = "hasPart";
	public static final String IX_ISDESCRIBEDBY = "isDescribedBy";
	public static final String IX_ISFOUNDIN = "isFoundIn";
	public static final String IX_ISPARTOF = "isPartOf";
	public static final String IX_ISRELATEDTO = "isRelatedTo";
	public static final String IX_ISVISUALIZEDBY = "isVisualizedBy";
	public static final String IX_SAMEAS = "sameAs"; // owl:sameAs
	public static final String IX_VISUALIZES = "visualizes";

	// spatiala specialindex
	public static final String IX_BOUNDING_BOX = "boundingBox";
	public static final String IX_POINT_DISTANCE = "pointDistance";

	// spatiala koordinatkonstanter
	public static final String SWEREF99_3006 = "SWEREF99";
	public static final String RT90_3021 = "RT90";
	public static final String WGS84_4326 = "WGS84";

	// alla index
	private static final HashMap<String,Index> indices = new LinkedHashMap<String,Index>();
	// publika index
	private static final List<Index> publicIndices = new ArrayList<Index>();

	// meddelanden om eventuella problem vid tolkning av tj�nsteinneh�llet, tex att en konstant
	// inte kunde sl�s upp etc, och antal ggr problemet f�rekom - fr�mst f�r utv/debug
	private static final ThreadLocal<Map<String,Integer>> problemMessages = new ThreadLocal<Map<String,Integer>>();

	static {
		// implementerade index

		// de visas per default i denna ordning i SRU-explain via publicIndices
		addIndex(IX_ITEMID, "Identifierare", IndexType.VERBATIM);
		addIndex(IX_SERVICENAME, "Ursprungstj�nst", IndexType.TOLOWERCASE);
		addIndex(IX_SERVICEORGANISATION, "Ursprungsorganisation", IndexType.TOLOWERCASE);
		addIndex(IX_TEXT, "Fritext, generellt", IndexType.ANALYZED);
		addIndex(IX_SUBJECT, "�mnesavgr�nsning", IndexType.TOLOWERCASE); // uri (�versatt)
		addIndex(IX_COLLECTION, "Namn p� samlingen som objektet tillh�r", IndexType.TOLOWERCASE);
		addIndex(IX_DATAQUALITY, "Beskrivningsniv�", IndexType.TOLOWERCASE); // uri (�versatt)
		addIndex(IX_MEDIATYPE, "Avgr�nsning av mediatyper", IndexType.VERBATIM); // uri // TODO: detta st�mmer ej(?)

		// objekt
		addIndex(IX_ITEM, "Fritext i entitetsf�lt", IndexType.ANALYZED);
		addIndex(IX_ITEMTYPE, "Typ av objekt", IndexType.TOLOWERCASE); // uri (�versatt)
		addIndex(IX_ITEMCLASS, "Objektets klass, kategorisering eller klassifikation", IndexType.VERBATIM); // uri
		addIndex(IX_ITEMCLASSNAME, "Klass eller kategori, om det inte finns en klassificeringsresurs", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMNAME, "Objektets huvudsakliga ben�mning eller sakord", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMSPECIFICATION, "Modellbeteckning eller liknande", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMTITLE, "Titel eller verksnamn", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMDESCRIPTION, "Fritext i beskrivningsf�lt", IndexType.ANALYZED);
		addIndex(IX_ITEMKEYWORD, "Nyckelord", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMMOTIVEWORD, "Ord som f�rekommer som beskrivning av ett motiv i ett bild- eller m�lningsobjekt", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMMATERIAL, "Material som objektet best�r av", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMTECHNIQUE, "Teknik f�r att producera objektet", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMSTYLE, "Stil som pr�glar objektet", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMCOLOR, "F�rg som pr�glar objektet", IndexType.TOLOWERCASE);
		addIndex(IX_ITEMNUMBER, "Inventarienummer eller annan identifikation av objektet", IndexType.VERBATIM);
		addIndex(IX_ITEMLICENSE, "Licens f�r anv�ndning av objektet", IndexType.VERBATIM); // typ? uri -> verbatim, kod -> lowercase
		addIndex(IX_CREATEDDATE, "Datum d� posten skapades i k�llsystemet (yyyy-mm-dd)", IndexType.VERBATIM);
		addIndex(IX_LASTCHANGEDDATE, "Datum d� posten �ndrades i k�llsystemet (yyyy-mm-dd)", IndexType.VERBATIM);
		addIndex(IX_THEME, "Tema [*]", IndexType.TOLOWERCASE);
		
		// plats
		addIndex(IX_PLACENAME, "Annat platsnamn [*]", IndexType.TOLOWERCASE);
		addIndex(IX_CADASTRALUNIT, "Fastighetsbeteckning [*]", IndexType.VERBATIM);
		addIndex(IX_PLACETERMID, "Plats-ID hos auktoritet [*]", IndexType.VERBATIM);
		addIndex(IX_PLACETERMAUTH, "Auktoritet f�r plats-ID [*]", IndexType.VERBATIM);
		addIndex(IX_CONTINENTNAME, "Kontinent [*]", IndexType.TOLOWERCASE);
		addIndex(IX_COUNTRYNAME, "Land, namn [*]", IndexType.TOLOWERCASE);
		addIndex(IX_COUNTYNAME, "L�n, namn [*]", IndexType.TOLOWERCASE);
		addIndex(IX_MUNICIPALITYNAME, "Kommun, namn [*]", IndexType.TOLOWERCASE);
		addIndex(IX_PROVINCENAME, "Landskap, namn [*]", IndexType.TOLOWERCASE);
		addIndex(IX_PARISHNAME, "Socken, namn [*]", IndexType.TOLOWERCASE);
		addIndex(IX_COUNTRY, "Land, kod [*]", IndexType.TOLOWERCASE); // uri (�versatt)
		addIndex(IX_COUNTY, "L�n, kod [*]",IndexType.TOLOWERCASE); // uri (�versatt)
		addIndex(IX_MUNICIPALITY, "Kommun, kod [*]", IndexType.TOLOWERCASE); // uri (�versatt)
		addIndex(IX_PROVINCE, "Landskap, kod [*]", IndexType.TOLOWERCASE); // uri (�versatt)
		addIndex(IX_PARISH, "Socken, kod [*]", IndexType.TOLOWERCASE); // uri (�versatt)
		addIndex(IX_PLACE, "Var - Fritext i geografiska data", IndexType.ANALYZED);

		// spatiala virtuella index
		String coordExplain = "koordinater separerade med mellanslag i (n�stan) valfritt format " +
		"((EPSG:3006 (SWEREF99 TM) �r default, OBS att x,y f�ruts�tts! Giltiga v�rden f�rutom " +
		"EPSG:XXXX �r '" + SWEREF99_3006 + "' (EPSG:3006 - SWEREF99 TM), " +
		"'" + RT90_3021 + "' (EPSG:3021 - RT90 2.5 gon V) och " +
		"'" + WGS84_4326 + "' (EPSG:4326))";
		addIndex(IX_BOUNDING_BOX, "Spatial s�kning med omslutande rektangel, " +
				coordExplain + " - ex " +
				IX_BOUNDING_BOX + "=/EPSG:3021 \"1628000.0 6585000.0 1628490.368 6585865.547\" eller " +
				IX_BOUNDING_BOX + "=/" + RT90_3021 + "\"1628000.0 6585000.0 1628490.368 6585865.547\"",
				IndexType.SPATIAL_VIRTUAL);
		addIndex(IX_POINT_DISTANCE, "Spatial n�rhetss�kning med angiven punkt och radie, " +
				coordExplain + " och radien i km - ex " +
				IX_POINT_DISTANCE + "=/EPSG:3021 \"1628000.0 6585000.0 3.5\" eller " +
				IX_POINT_DISTANCE + "=/" + RT90_3021 + " \"1628000.0 6585000.0 3.5\"",
				IndexType.SPATIAL_VIRTUAL);

		// person
		addIndex(IX_FIRSTNAME, "F�rnamn [*]", IndexType.TOLOWERCASE);
		addIndex(IX_SURNAME, "Efternamn [*]", IndexType.TOLOWERCASE);
		//addIndex(IX_NAME, "Namn", IndexType.TOLOWERCASE); // ej index, bara i med fritext
		addIndex(IX_GENDER, "K�n [*]", IndexType.TOLOWERCASE);
		addIndex(IX_FULLNAME, "Fullst�ndigt namn ([f�rnamn] [efternamn]) [*]", IndexType.TOLOWERCASE);
		addIndex(IX_ORGANIZATION, "Organisation [*]", IndexType.TOLOWERCASE);
		addIndex(IX_TITLE, "Titel (person) [*]", IndexType.TOLOWERCASE);
		addIndex(IX_NAMEID, "Auktoriserat ID [*]", IndexType.VERBATIM);
		addIndex(IX_NAMEAUTH, "Auktoritet f�r namn [*]", IndexType.VERBATIM);
		addIndex(IX_ACTOR, "Vem - Fritext i person- och organisationsdata", IndexType.ANALYZED);

		// tid
		addIndex(IX_FROMTIME, "Tidpunkt eller start p� tidsintervall (�rtal enligt ISO 8601) [*]", IndexType.ISO8601DATEYEAR);
		addIndex(IX_TOTIME, "Tidpunkt eller slut p� tidsintervall (�rtal enligt ISO 8601) [*]", IndexType.ISO8601DATEYEAR);
		addIndex(IX_FROMPERIODNAME, "Tidpunkt eller start p� tidsintervall, namn [*]", IndexType.TOLOWERCASE);
		addIndex(IX_TOPERIODNAME, "Tidpunkt eller slut p� tidsintervall, namn [*]", IndexType.TOLOWERCASE);
		addIndex(IX_FROMPERIODID, "Tidpunkt eller start p� tidsintervall, kod [*]", IndexType.VERBATIM);
		addIndex(IX_TOPERIODID, "Tidpunkt eller slut p� tidsintervall, kod [*]", IndexType.VERBATIM);
		addIndex(IX_PERIODAUTH, "Auktoritet f�r perioder [*]", IndexType.VERBATIM);
		addIndex(IX_EVENTNAME, "Namn p� en h�ndelse [*]", IndexType.TOLOWERCASE);
		addIndex(IX_EVENTAUTH, "Auktoritet f�r h�ndelser [*]", IndexType.VERBATIM);
		//addIndex(IX_TIMETEXT, "Annan tidsuppgift [*]", IndexType.ANALYZED);
		addIndex(IX_TIME, "N�r - Fritext i tidsdata", IndexType.ANALYZED);

		// context
		addIndex(IX_CONTEXTLABEL, "Sammanhang enligt �ndlig lista, beskrivning", IndexType.TOLOWERCASE);
		addIndex(IX_CONTEXTTYPE, "Sammanhang enligt �ndlig lista, nyckelv�rde", IndexType.TOLOWERCASE);

		// relationer TODO: fixa beskrivningstexterna n�r B�rje har skickat dem
		addIndex(IX_RELURI, "�r relaterat p� n�got s�tt till annat objekt (uri)", IndexType.VERBATIM);
		addIndex(IX_CONTAINSINFORMATIONABOUT, "Har information om (uri)", IndexType.VERBATIM);
		addIndex(IX_CONTAINSOBJECT, "Inneh�ller objekt (uri)", IndexType.VERBATIM);
		addIndex(IX_HASBEENUSEDIN, "Anv�nds i (uri)", IndexType.VERBATIM);
		addIndex(IX_HASCHILD, "Har underordnat objekt (uri)", IndexType.VERBATIM);
		addIndex(IX_HASFIND, "Har fynd (uri)", IndexType.VERBATIM);
		addIndex(IX_HASIMAGE, "Har bild (uri)", IndexType.VERBATIM);
		addIndex(IX_HASOBJECTEXAMPLE, "Har objektexempel (uri)", IndexType.VERBATIM);
		addIndex(IX_HASPARENT, "Har �verordnat objekt (uri)", IndexType.VERBATIM);
		addIndex(IX_HASPART, "Har del (uri)", IndexType.VERBATIM);
		addIndex(IX_ISDESCRIBEDBY, "Beskrivs av (uri)", IndexType.VERBATIM);
		addIndex(IX_ISFOUNDIN, "Finns i (uri)", IndexType.VERBATIM);
		addIndex(IX_ISPARTOF, "�r del av annat objekt (uri)", IndexType.VERBATIM);
		addIndex(IX_ISRELATEDTO, "�r relaterat till annat objekt (uri)", IndexType.VERBATIM);
		addIndex(IX_ISVISUALIZEDBY, "Visualiseras av annat objekt (uri)", IndexType.VERBATIM);
		addIndex(IX_SAMEAS, "Samma som (uri)", IndexType.VERBATIM);
		addIndex(IX_VISUALIZES, "Visualiserar objekt (uri)", IndexType.VERBATIM);
		addIndex(IX_ADDEDTOINDEXDATE, "Datum posten lades till i indexet (yyyy-mm-dd) - " + 
				"obs att detta datum �r ungef�rligt d� det beror p� sk�rdfrekvens f�r " +
				"k�lltj�nsten, ber�knas som max(k�lltj�nstens f�rsta indexeringsdatum, " +
				IX_CREATEDDATE + ")", IndexType.VERBATIM);

		// �vriga, "interna"
		addIndex(I_IX_PRES, "presentationsblocket", IndexType.VERBATIM, false);
		addIndex(I_IX_SERVICE, "tj�nst", IndexType.VERBATIM, false);
		addIndex(I_IX_HTML_URL, "html-representation, url", IndexType.VERBATIM, false);
		addIndex(I_IX_MUSEUMDAT_URL, "museumdat-representation, url", IndexType.VERBATIM, false);
		addIndex(I_IX_LON, "longitud f�r centrumpunkt", IndexType.SPATIAL_COORDINATE, false);
		addIndex(I_IX_LAT, "lattitud f�r centrumpunkt", IndexType.SPATIAL_COORDINATE, false);
		addIndex(CONTEXT_SET_REC + "." + IX_REC_IDENTIFIER, "identifierare", IndexType.VERBATIM, false);
		//addIndex(I_IX_RDF, "rdf", IndexType.VERBATIM, false);

		// publika
		for (Index index: indices.values()) {
			if (index.isPublic()) {
				publicIndices.add(index);
			}
		}
	}

	// teckenkodning i stopp-ordsfiler
	// (UTF-8 �r egentligen b�ttre, men kan vara sv�rt att hantera i vissa texteditorer)
	private static final String STOPWORD_FILE_ENCODING = "ISO-8859-1";
	// "s�kv�g" i jar-filen
	private static final String PATH = "/" + ContentHelper.class.getPackage().getName().replace('.', '/') + "/";

	// analyserar-variabler
	private static Analyzer sweAnalyzer = null;
	private static final Object sweAnalyzerSync = new Object();
	private static Analyzer engAnalyzer = null;
	private static final Object engAnalyzerSync = new Object();

	private static final Logger logger = Logger.getLogger(ContentHelper.class);

	public ContentHelper() {
	}

	// instansmetoder som m�ste implementeras i subklasser

	/**
	 * Extraherar identifierare ur xml-inneh�ll. F�r k-sams�kstj�nster ska identifieraren
	 * vara en URI och xml-inneh�llet �r en post med k-sams�ks-xml (rdf).
	 * 
	 * @param xmlContent xml-inneh�ll
	 * @param gmlInfoHolder b�na som fylls p� med funna gml-geometrier mm om ej null
	 * @return identifierare
	 * @throws Exception vid problem
	 */
	public abstract String extractIdentifierAndGML(String xmlContent, GMLInfoHolder gmlInfoHolder) throws Exception;

	/**
	 * Skapar ett lucene-dokument utifr�n det inskickade xml-inneh�llet. F�r k-sams�kstj�nster
	 * �r xml-inneh�llet en post med k-sams�ks-xml (rdf).
	 * 
	 * @param service tj�nst
	 * @param xmlContent xml-inneh�ll
	 * @return ett lucene-dokument
	 * @throws Exception vid problem
	 */
	public abstract Document createLuceneDocument(HarvestService service, String xmlContent) throws Exception;

	// statiska metoder

	/**
	 * Formaterar ett datum i svenskt standardformat med eller utan tid.
	 * @param date datum
	 * @param inclTime om tid ska med
	 * @return formaterad str�ng
	 */
	public static String formatDate(Date date, boolean inclTime) {
		final String format;
		if (inclTime) {
			format = "yyyy-MM-dd HH:mm:ss";
		} else {
			format = "yyyy-MM-dd";
		}
		SimpleDateFormat df = new SimpleDateFormat(format);
		return df.format(date);
	}

	// h�mtar ut ett konfat index, eller dess "pappa" f�r prefixade index
	// b�r bara anv�ndas f�r att fastst�lla vilken typ av index det �r
	private static IndexType getIndexType(String indexName) {
		int underScorePos = -1;
		if (indexName != null && (underScorePos = indexName.indexOf("_")) > 0) {
			indexName = indexName.substring(underScorePos + 1);
		}
		Index index = indices.get(indexName);
		return index != null ? index.getIndexType() : null;
	}

	/**
	 * Ger sant om indexnamnet �r ett analyserat index.
	 * 
	 * @param indexName indexnamn
	 * @return sant f�r analyserade indexnamn
	 */
	public static boolean isAnalyzedIndex(String indexName) {
		// analyserade index, dvs fritext
		return IndexType.ANALYZED == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet �r ett verbatim-index, dvs om v�rdet ej �ndras
	 * utan lagras exakt och m�ste s�kas efter exakt.
	 * 
	 * @param indexName indexnamn
	 * @return sant f�r index som �r verbatim
	 */
	public static boolean isVerbatimIndex(String indexName) {
		// de index som indexerats verbatim, typ identifierare, uris etc
		return IndexType.VERBATIM == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet �r ett gemener-index, dvs om v�rdet g�rs
	 * om till gemener vid indexering och s�kning.
	 * 
	 * @param indexName indexnamn
	 * @return sant f�r index vars inneh�ll g�rs om till gemener
	 */
	public static boolean isToLowerCaseIndex(String indexName) {
		// de index som indexerats med lower case
		return IndexType.TOLOWERCASE == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet �r ett spatialt virtuellt index. Denna typ �r
	 * ett specialfall som hanteras olika beroende p� indexnamn d� de kan ha
	 * olika utseende p� sina parametrar skapa fr�gor som s�ker i andra index etc.
	 * 
	 * @param indexName indexnamn
	 * @return sant f�r spatiala index
	 */
	public static boolean isSpatialVirtualIndex(String indexName) {
		// de index som indexerats med lower case
		return IndexType.SPATIAL_VIRTUAL == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet �r ett spatialt index vars v�rden �r rena koordinater.
	 * 
	 * @param indexName indexnamn
	 * @return sant f�r spatiala index
	 */
	public static boolean isSpatialCoordinateIndex(String indexName) {
		// de index som indexerats med lower case
		return IndexType.SPATIAL_COORDINATE == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet �r ett iso8601-datum-index. Denna typ �r ett specialfall
	 * d� inv�rdet i fr�n lokalnoderna kan vara hela datum men d� enbart �rtalet ska
	 * indexeras. Dvs g�rs v�rdet om till �rtal och behandlas vid indexering med algoritm
	 * fr�n solr:s NumberUtils s� att lucene kan g�ra intervalls�kningar �ven med negativa
	 * v�rden. Vid s�kning appliceras samma algoritm p� s�kv�rdet som d� f�ruts�tts
	 * vara ett �rtal.
	 * 
	 * @param indexName indexnamn
	 * @return sant f�r index vars inneh�ll ska vara �rtal och som g�rs om till str�ngv�rden
	 */
	public static boolean isISO8601DateYearIndex(String indexName) {
		// de index som indexerats verbatim, typ identifierare, uris etc
		return IndexType.ISO8601DATEYEAR == getIndexType(indexName);
	}

	// l�gger till index i index-listan, anv�nds bara vid init
	private static void addIndex(String indexName, String title, IndexType indexType) {
		addIndex(indexName, title, indexType, true);
	}
	private static void addIndex(String indexName, String title, IndexType indexType, boolean isPublic) {
		indices.put(indexName, new Index(indexName, title, indexType, isPublic));
	}

	/**
	 * H�mtar lista med alla publika index.
	 * 
	 * @return lista med publikt s�kbara index.
	 */
	public static List<Index>getPublicIndices() {
		return publicIndices;
	}

	/**
	 * Enum f�r indextyp.
	 */
	public static enum IndexType { ANALYZED, VERBATIM, TOLOWERCASE, ISO8601DATEYEAR, SPATIAL_VIRTUAL, SPATIAL_COORDINATE };

	/**
	 * Klass som representerar ett index.
	 */
	public static final class Index {
		private final String index;
		private final String title;
		private final IndexType indexType;
		private final boolean isPublic;

		Index(String index, String title, IndexType indexType) {
			this(index, title, indexType, true);
		}
		Index(String index, String title, IndexType indexType, boolean isPublic) {
			this.index = index;
			this.title = title;
			this.indexType = indexType;
			this.isPublic = isPublic;
		}

		/**
		 * H�mtar indexnamnet.
		 * 
		 * @return indexnamn
		 */
		public String getIndex() {
			return index;
		}

		/**
		 * H�mtar beskrivande text.
		 * 
		 * @return beskrivande text
		 */
		public String getTitle() {
			return title;
		}

		/**
		 * H�mtar indextyp.
		 * 
		 * @return indextyp
		 */
		public IndexType getIndexType() {
			return indexType;
		}

		/**
		 * Ger sant om indexet �r publikt.
		 * 
		 * @return sant f�r publika index
		 */
		public boolean isPublic() {
			return isPublic;
		}
	}

	/**
	 * H�mtar svensk snowball-analyserare f�r anv�ndning med lucene. Anv�nder stopp-ord
	 * fr�n swe_stop.txt.
	 * 
	 * @return svensk analyserare
	 */
	public static final Analyzer getSwedishAnalyzer() {
		synchronized (sweAnalyzerSync) {
			if (sweAnalyzer == null) {
				sweAnalyzer = new SnowballAnalyzer("Swedish", readStopWordFile("swe_stop.txt"));
			}
		}
		return sweAnalyzer;
	}

	/**
	 * H�mtar engelsk snowball-analyserare f�r anv�ndning med lucene. Anv�nder stopp-ord
	 * fr�n eng_stop.txt. Anv�nds ej fn.
	 * 
	 * @return engelsk analyserare
	 */
	public static final Analyzer getEnglishAnalyzer() {
		synchronized (engAnalyzerSync) {
			if (engAnalyzer == null) {
				engAnalyzer = new SnowballAnalyzer("English", readStopWordFile("eng_stop.txt"));
			}
		}
		return engAnalyzer;
	}

	// l�ser in fil med stopp-ord fr�n classpath
	private static String[] readStopWordFile(String fileName) {
		BufferedReader br = null;
		String[] stopWords = {};
		try {
			// l�gg p� s�kv�g (detta paket)
			fileName =  PATH + fileName;
			br = new BufferedReader(new InputStreamReader(
					ContentHelper.class.getResourceAsStream(fileName), STOPWORD_FILE_ENCODING));
			ArrayList<String> words = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				// | �r kommentarstecken i snowballs stopp-ordsfiler
				int commentIndex = line.indexOf('|');
				if (commentIndex >= 0) {
					// h�mta allt innan kommentaren
					line = line.substring(0, commentIndex);
				}
				// trimma bort resterande mellanslag
				line = StringUtils.trimToNull(line);
				// resten ska vara ett ord
				if (line != null) {
					words.add(line);
				}
			}
			if (words.size() > 0) {
				if (logger.isInfoEnabled()) {
					logger.info("L�ste in " + words.size() + " stopp-ord fr�n " + fileName);
				}
				if (logger.isDebugEnabled()) {
					logger.debug(fileName + ": stopp-ord: " + words);
				}
				stopWords = new String[words.size()];
				words.toArray(stopWords);
			} else {
				logger.warn("Inga stopp-ord funna i " + fileName);
			}
		} catch (Exception e) {
			logger.error("Fel vid l�sning av stopp-ordsfil: " + fileName + ", anv�nder inga stopp-ord", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception ignore) {
				}
				br = null;
			}
		}
		return stopWords;
	}

	/**
	 * Initierar mappen med problemmeddelanden f�r denna tr�d.
	 */
	public static void initProblemMessages() {
		// linked hashmap f�r att beh�lla ordningen
		problemMessages.set(new LinkedHashMap<String, Integer>());
	}

	/**
	 * H�mtar och rensar mappen med problemmeddelanden f�r denna tr�d.
	 * 
	 * @return problemmeddelanden eller null
	 */
	public static Map<String, Integer> getAndClearProblemMessages() {
		Map<String,Integer> map = problemMessages.get();
		problemMessages.remove();
		return map;
	}

	/**
	 * L�gger till ett problemmeddelande f�r denna tr�d/detta jobb.
	 * 
	 * @param message meddelande
	 */
	public static void addProblemMessage(String message) {
		final int maxSize = 200;
		final String xMessage = "Fler felmeddelanden finns, visar bara max " + maxSize + " olika";
		Map<String,Integer> map = problemMessages.get();
		if (map != null) {
			Integer c = map.get(message);
			if (c == null) {
				c = 0;
				if (map.size() >= maxSize && !xMessage.equals(message)) {
					addProblemMessage(xMessage);
					return;
				}
			}
			map.put(message, c + 1);
		}
	}

	/**
	 * Formaterar k�rtid/deltatid p� formatet hh:mm:ss.
	 * 
	 * @param durationMillis k�rtid i millisekunder
	 * @return str�ng med k�rtid
	 */
	public static String formatRunTime(long durationMillis) {
		return DurationFormatUtils.formatDuration(durationMillis, "HH:mm:ss");
	}

	/**
	 * Ber�knar ungef�rlig �terst�ende tid utifr�n hur l�ng tid det hittills har tagit
	 * f�r en delm�ngd. Ber�kningen antar att f�rloppet sker med konstant hastighet.
	 *  
	 * @param deltaMillis tid det tog f�r delm�ngd att bli klar
	 * @param deltaCount antal i delm�ngd
	 * @param fullCount antal totalt
	 * @return ungef�rlig �terst�ende tid i millisekunder
	 */
	public static long getRemainingRunTimeMillis(long deltaMillis, int deltaCount, int fullCount) {
        long remainingTimeMillis = -1;
		if (fullCount > 0 && deltaCount > 0) {
			long aproxRunTimeMillis = Math.round(fullCount * deltaMillis * 1.0 / deltaCount);
			remainingTimeMillis = aproxRunTimeMillis - deltaMillis;
		}
		return remainingTimeMillis; 
	}

	/**
	 * Formaterar antal per sekund.
	 * 
	 * @param count antal
	 * @param durationMillis k�rtid i millisekunder
	 * @return str�ng med antal per sekund med en nogrannhet p� en decimal
	 */
	public static String formatSpeedPerSec(long count, long durationMillis) {
		double perSec = getSpeedPerSec(count, durationMillis);
		return "ca " + perSec + "/s";
	}

	/**
	 * Ber�knar antal per sekund.
	 * 
	 * @param count antal
	 * @param durationMillis k�rtid i millisekunder
	 * @return antal per sekund med 1 decimal
	 */
	public static double getSpeedPerSec(long count, long durationMillis) {
		return Math.round(10.0 * count / Math.max(durationMillis / 1000, 1)) / 10.0;
	}

	/**
	 * G�r om inv�rdet till en str�ng som kan sorteras och anv�ndas korrekt i
	 * intervalls�kningar i lucene. Algoritm l�nad fr�n solr:s NumberUtils (apache-licens).
	 * 
	 * @param val v�rde
	 * @return str�ngrepresentation av v�rde
	 */
	public static String transformNumberToLuceneString(long val) {
		// TODO: iom att locallucene m�ste ha NumberUtils kanske den ska anv�ndas h�r, dock
		//       h�ller locallucene p� att infogas i ett spatial-block i lucene/solr och
		//       ocks� p� att byta algoritm
		int offset = 0;
		char[] out = new char[5];
		val += Long.MIN_VALUE;
	    out[offset++] = (char)(val >>>60);
	    out[offset++] = (char)(val >>>45 & 0x7fff);
	    out[offset++] = (char)(val >>>30 & 0x7fff);
	    out[offset++] = (char)(val >>>15 & 0x7fff);
	    out[offset] = (char)(val & 0x7fff);
	    return new String(out,0,5);
	}

	/**
	 * G�r om inv�rdet till en str�ng som kan sorteras och anv�ndas korrekt i
	 * intervalls�kningar i lucene. Algoritm l�nad fr�n solr:s NumberUtils (apache-licens).
	 * 
	 * @param val v�rde
	 * @return str�ngrepresentation av v�rde
	 */
	public static String transformNumberToLuceneString(double val) {
		// TODO: se todo i metod ovan
		long f = Double.doubleToRawLongBits(val);
	    if (f<0) f ^= 0x7fffffffffffffffL;
	    return transformNumberToLuceneString(f);
	}

	// 

	/**
	 * Hj�lpmetod som extraherar parametrar kodade mha utf-8 fr�n query-str�ngen, kr�vs bla
	 * f�r sru/cql.
	 * @param qs querystr�ng
	 * @return map med avkodade parametrar och v�rden
	 * @throws Exception vid fel med avkodning eller annat
	 */
	public static Map<String, String> extractUTF8Params(String qs) throws Exception {
		HashMap<String, String> params = new HashMap<String, String>();
		if (qs != null && qs.length() > 0) {
			StringTokenizer tok = new StringTokenizer(qs, "&");
			while (tok.hasMoreTokens()) {
				String[] par = tok.nextToken().split("=");
				if (par.length > 1 && par[1].length() > 0) {
					if (par.length == 2) {
						params.put(par[0], URLDecoder.decode(par[1], "UTF-8"));
					} else {
						// vi �r sn�lla och till�ter = okodat i parametrar f�r att enklare
						// kunna testa
						StringBuffer pVal = new StringBuffer();
						pVal.append(par[1]);
						for (int i = 2; i < par.length; ++i) {
							pVal.append("=").append(par[i]);
						}
						params.put(par[0], URLDecoder.decode(pVal.toString(), "UTF-8"));
					}
				}
			}
		}
		return params;
	}


}
