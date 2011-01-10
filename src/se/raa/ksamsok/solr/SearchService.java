package se.raa.ksamsok.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import se.raa.ksamsok.api.util.Term;

public interface SearchService {

	/**
	 * St�ller en fr�ga och ger svaret.
	 * @param query fr�ga
	 * @return fr�gesvar
	 * @throws SolrServerException vid kommuikationsproblem
	 */
	QueryResponse query(SolrQuery query) throws SolrServerException;

	/**
	 * H�mtar antal dokument i indexet f�r angiven tj�nst, eller totalt om tj�nstenamnet �r null.
	 * @param serviceName tj�nstenamn
	 * @return antal tr�ffar f�r tj�nsten eller totalt
	 * @throws SolrServerException vid fel
	 */
	long getIndexCount(String serviceName) throws SolrServerException;

	/**
	 * H�mtar antal dokument i indexet f�r alla tj�nster nycklat p� tj�nste-id.
	 * @return antal tr�ffar f�r alla tj�nster
	 * @throws SolrServerException vid fel
	 */
	Map<String, Long> getIndexCounts() throws SolrServerException;

	/**
	 * Analyserar (stammar) ett eller flera ord.
	 * @param words ord
	 * @return m�ngd med ordstammar
	 * @throws SolrServerException vid s�kfel
	 * @throws IOException vid kommunikationsfel
	 */
	Set<String> analyze(String words) throws SolrServerException, IOException;

	/**
	 * H�mtar termer f�r angivet index med angivet prefix sorterade i fallande f�rekomstordning.
	 * @param index indexnamn
	 * @param prefix prefix
	 * @param removeBelow minsta tr�ff-frekvens
	 * @param maxCount max antal termer
	 * @return m�ngd med {@linkplain Term}er
	 * @throws SolrServerException
	 */
	List<Term> terms(String index, String prefix, int removeBelow, int maxCount) throws SolrServerException;

	/**
	 * Ger url till den solr-instans som anv�nds.
	 * @return url eller null
	 */
	String getSolrURL();

}
