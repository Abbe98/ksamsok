package se.raa.ksamsok.api.method;

import java.io.PrintWriter;
import java.util.Map;

import org.apache.solr.common.SolrDocumentList;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.MissingParameterException;

public abstract class AbstractSearchMethod extends AbstractAPIMethod {

	/** default startplats i s�kning */
	public static final int DEFAULT_START_RECORD = 1;
	/** parameternamn d�r s�kparametrarna skall ligga n�r en s�kning g�rs */
	public static final String SEARCH_PARAMS = "query";
	/** parameternamnet som anges f�r att v�lja antalet tr�ffar per sida */
	public static final String HITS_PER_PAGE = "hitsPerPage";
	/** parameternamnet som anges f�r att v�lja startRecord */
	public static final String START_RECORD = "startRecord";
	/** max antal tr�ffar */
	public static final int MAX_HITS_PER_PAGE = 500;

	protected String queryString;
	protected int hitsPerPage;
	protected int startRecord;

	protected SolrDocumentList hitList;

	protected AbstractSearchMethod(APIServiceProvider serviceProvider, PrintWriter writer, Map<String,String> params) {
		super(serviceProvider, writer, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		this.queryString = getQueryString(params.get(SEARCH_PARAMS));
		//s�tter valfria parametrar
		int hitsPerPage = getHitsPerPage(params.get(HITS_PER_PAGE));
		int startRecord = getStartRecord(params.get(START_RECORD));
		//kontrollerar att hitsPerPage och startRecord har till�tna v�rden
		if (hitsPerPage < 1 || hitsPerPage > getMaxHitsPerPage()) {
			this.hitsPerPage = getDefaultHitsPerPage();
		} else {
			this.hitsPerPage = hitsPerPage;
		}
		if (startRecord < 1) {
			this.startRecord = DEFAULT_START_RECORD;
		} else {
			this.startRecord = startRecord;
		}


	}

	/**
	 * Ger default antal tr�ffar per sida
	 * @return default antal tr�ffar
	 */
	abstract protected int getDefaultHitsPerPage();

	/**
	 * Ger max antal tr�ffar per sida
	 * @return max antal tr�ffar
	 */
	protected int getMaxHitsPerPage() {
		return MAX_HITS_PER_PAGE;
	}

	/**
	 * returnerar en integer f�r v�rdet startRecord
	 * @param param
	 * @return
	 * @throws BadParameterException
	 */
	public int getStartRecord(String param) throws BadParameterException {
		int startRecord = 0;
		if (param != null) {
			try {
				startRecord = Integer.parseInt(param);
			} catch(NumberFormatException e) {
				throw new BadParameterException("parametern " + START_RECORD + " m�ste inneh�lla ett numeriskt v�rde", "APIMethodFactory.getSearchObject", "icke numeriskt v�rde", false);
			}
		}
		return startRecord;
	}

	/**
	 * returnerar hitsPerPage
	 * @param param
	 * @return
	 * @throws BadParameterException
	 */
	public int getHitsPerPage(String param) throws BadParameterException {
		int hitsPerPage = 0;
		if (param != null) {
			try {
				hitsPerPage = Integer.parseInt(param);
			} catch(NumberFormatException e) {
				throw new BadParameterException("parametern " + Search.HITS_PER_PAGE + " m�ste inneh�lla ett numeriskt v�rde", "APIMethodFactory.getSearchObject", "icke numeriskt v�rde", false);
			}
		}
		return hitsPerPage;
	}

}
