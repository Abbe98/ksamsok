package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;
import org.z3950.zing.cql.CQLTermNode;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Solr;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.statistic.StatisticLoggData;
import se.raa.ksamsok.util.ShmSiteCacherHackTicket3419;

/**
 * Hanterar s�kningar efter objekt
 * @author Henrik Hjalmarsson
 */
public class Search extends AbstractSearchMethod {
	/** standardv�rdet f�r antalet tr�ffar per sida */
	public static final int DEFAULT_HITS_PER_PAGE = 50;
	/** metodnamn som anges f�r att anv�nda denna klass */
	public static final String METHOD_NAME = "search";
	/** parameternamn f�r sort */
	public static final String SORT = "sort";
	/** parameternamn f�r sort configuration */
	public static final String SORT_CONFIG = "sortConfig";
	/** parameterv�rde f�r descending sort */
	public static final String SORT_DESC = "desc";
	/** parameterv�rde f�r ascending sort */
	public static final String SORT_ASC = "asc";
	/** record shema f�r presentations data */
	public static final String NS_SAMSOK_PRES =	"http://kulturarvsdata.se/presentation#";
	/** parameternamn f�r record schema */
	public static final String RECORD_SCHEMA = "recordSchema";
	/** bas URL till record schema */
	public static final String RECORD_SCHEMA_BASE = "http://kulturarvsdata.se/";

	// index att anv�nda f�r sortering (transparent) ist�llet f�r itemName
	private static final String ITEM_NAME_SORT = "itemNameSort";
	
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.Search");

	protected String sort = null;
	protected boolean sortDesc = false;
	protected String recordSchema = null;
	protected String apiKey;
	protected String binDataField = null;

	/**
	 * skapar ett Search objekt
	 * @param params s�kparametrar
	 * @param hitsPerPage tr�ffar som skall visas per sida
	 * @param startRecord startposition i s�kningen
	 * @param writer skrivaren som skall anv�ndas f�r att skriva svaret
	 */
	public Search(APIServiceProvider serviceProvider, PrintWriter writer, Map<String,String> params) {
		super(serviceProvider, writer, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		super.extractParameters();
		this.apiKey = params.get(APIMethod.API_KEY_PARAM_NAME);
		sort = params.get(Search.SORT);
		if (sort != null) {
			if (!ContentHelper.indexExists(sort)) {
				throw new BadParameterException("Sorteringsindexet " + sort + " finns inte.", "Search.performMethod", null, false);
			}
			// TODO: generalisera, l�gga i konf-fil?
			// specialhantering f�r sortering p� itemName, ist�llet anv�nds itemNameSort
			// transparent som rensar itemName och beh�ller bara bokst�ver och siffor - fix
			// f�r att tex poster med citationstecken ("konstiga" tecken) kom f�rst
			if (ContentHelper.IX_ITEMNAME.equals(sort)) {
				sort = ITEM_NAME_SORT;
			}
		}
		sortDesc = getSortConfig(params.get(Search.SORT), params.get(Search.SORT_CONFIG));
		recordSchema = params.get(Search.RECORD_SCHEMA);
		if (recordSchema != null) {
			recordSchema = RECORD_SCHEMA_BASE + recordSchema + "#";
		}
		if (NS_SAMSOK_PRES.equals(recordSchema)) {
			binDataField = ContentHelper.I_IX_PRES;
		} else {
			binDataField = ContentHelper.I_IX_RDF;
		}

	}

	@Override
	protected int getDefaultHitsPerPage() {
		return DEFAULT_HITS_PER_PAGE;
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		try {
			SolrQuery query = createQuery();
			// start �r 0-baserad
			query.setStart(startRecord - 1);
			query.setRows(hitsPerPage);
			if (sort != null) {
				query.addSortField(sort, sortDesc ? ORDER.desc : ORDER.asc);
			}
			query.addField(ContentHelper.IX_ITEMID);
			query.addField("score"); // score �r "solr-special" f�r uhm, score...
			// ta fram r�tt data
			query.addField(binDataField);
			QueryResponse qr = serviceProvider.getSearchService().query(query);
			hitList = qr.getResults();
		} catch(SolrServerException e) {
			throw new DiagnosticException("Ov�ntat IO-fel uppstod. Var god f�rs�k igen", "Search.performMethod", e.getMessage(), true);
		} catch (BadParameterException e) {
			throw new DiagnosticException("Ov�ntat parserfel uppstod", "Search.performMethod", e.getMessage(), true);
		}
	}

	/**
	 * skriver ut nedre del av XML svar
	 */
	@Override
	protected void writeFootExtra() {	
		writer.println("<echo>");
		writer.println("<startRecord>" + startRecord + "</startRecord>");
		writer.println("<hitsPerPage>" + hitsPerPage + "</hitsPerPage>");
		writer.println("<query>" + StaticMethods.xmlEscape(queryString) + "</query>");
		writer.println("</echo>");
	}

	/**
	 * skriver ut �vre del av XML svar
	 * @param numberOfDocs
	 */
	@Override
	protected void writeHeadExtra() {
		writer.println("<totalHits>" + hitList.getNumFound() + "</totalHits>");
	}

	@Override
	protected void writeResult() {
		writer.println("<records>");
		try {
			for (SolrDocument d: hitList) {
				Float score = (Float) d.getFieldValue("score");
				String ident = (String) d.getFieldValue(ContentHelper.IX_ITEMID);
				writeContent(getContent(d, ident), score);
			}
		} finally {
			writer.println("</records>");
		}
	}

	/**
	 * skriver ut content
	 * @param content
	 * @param score
	 */
	protected void writeContent(String content, double score) {
		if (content == null) {
			return;
		}
		writer.println("<record>");
		writer.println(content);
		writer.println("<rel:score xmlns:rel=\"info:srw/extension/2/relevancy-1.0\">" + score + "</rel:score>");
		writer.println("</record>");
	}
	
	/**
	 * H�mtar xml-inneh�ll (fragment) fr�n ett lucene-dokument som en str�ng.
	 * @param doc lucenedokument
	 * @param uri postens uri (anv�nds bara f�r log)
	 * @param xmlIndex index att h�mta inneh�ll fr�n
	 * @return xml-fragment med antingen presentations-xml eller rdf; null om data saknas
	 * @throws Exception vid teckenkodningsfel (b�r ej intr�ffa) 
	 */
	protected String getContent(SolrDocument doc, String uri) {
		String content = null;
		byte[] xmlData = (byte[]) doc.getFieldValue(binDataField);
		try {
			// h�mta ev fr�n hack-cachen
			if (ShmSiteCacherHackTicket3419.useCache(params.get(ShmSiteCacherHackTicket3419.KRINGLA), uri)) {
				content = ShmSiteCacherHackTicket3419.getOrRecache(uri, xmlData);
			} else {
				if (xmlData != null) {
					content = new String(xmlData, "UTF-8");
				}
				// TODO: NEK: ta bort n�r allt �r omindexerat
				if (content == null) {
					content = serviceProvider.getHarvestRepositoryManager().getXMLData(uri);
				}
			}
			if (content == null) {
				logger.warn("Hittade inte xml-data (" + binDataField + ") f�r " + uri);
			}
		} catch (Exception e) {
			logger.error("Fel vid h�mtande av xml-data (" + binDataField + ") f�r " + uri);
		}
		return content;
	}

	/**
	 * Skapar ett query
	 * @return query
	 */
	protected SolrQuery createQuery() throws DiagnosticException, BadParameterException {
		SolrQuery query = null;
		try {
			CQLParser parser = new CQLParser();
			CQLNode rootNode = parser.parse(queryString);
			String solrQueryString = CQL2Solr.makeQuery(rootNode);
			if (solrQueryString != null) {
				query = new SolrQuery(solrQueryString);
				// logga s�kdata
				loggData(rootNode);
			}
		} catch (IOException e) {
			throw new DiagnosticException("Ov�ntat IO-fel uppstod. Var god f�rs�k igen", "Search.createQuery", e.getMessage(), true);
		} catch (CQLParseException e) {
			throw new DiagnosticException("Parserfel uppstod. Detta beror troligen p� att query-str�ngen inte f�ljer CQL syntax. Var god kontrollera s�kstr�ngen eller kontakta systemadministrat�r f�r s�ksystemet du anv�nder", "Search.createQuery", e.getMessage(), false);
		}
		return query;
	}

	/**
	 * returnerar true om sortConfig �r satt till "desc"
	 * @param sort
	 * @param sortConfig
	 * @return
	 */
	public boolean getSortConfig(String sort, String sortConfig) {
		boolean sortDesc = false;
		if (sort != null) {
			if (sortConfig != null && sortConfig.equals(Search.SORT_DESC)) {
				sortDesc = true;
			}
		}
		return sortDesc;
	}

	/**
	 * Loggar data f�r s�kningen f�r indexet "text".
	 * @param query cql
	 * @throws DiagnosticException
	 */
	private void loggData(CQLNode query) throws DiagnosticException {
		if (query == null) {
			return;
		}
		if (query instanceof CQLBooleanNode) {
			CQLBooleanNode bool = (CQLBooleanNode) query;
			loggData(bool.left);
			loggData(bool.right);
		} else if (query instanceof CQLTermNode) {
			CQLTermNode t = (CQLTermNode) query;
			// bara f�r "text"
			if (t.getIndex().equals(ContentHelper.IX_TEXT)) {
				StatisticLoggData data = new StatisticLoggData();
				data.setParam(t.getIndex());
				data.setAPIKey(apiKey);
				data.setQueryString(t.getTerm());
				serviceProvider.getStatisticsManager().addToQueue(data);
			}
		}
	}

}