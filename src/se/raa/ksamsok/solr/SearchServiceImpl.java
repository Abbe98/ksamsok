package se.raa.ksamsok.solr;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.FieldAnalysisRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.AnalysisPhase;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse.Analysis;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.TermsParams;
import org.apache.solr.common.util.NamedList;
import org.springframework.beans.factory.annotation.Autowired;

import se.raa.ksamsok.api.util.Term;
import se.raa.ksamsok.lucene.ContentHelper;

public class SearchServiceImpl implements SearchService {

	// max antal termer att h�mta om inget angivits (-1)
	private static final int DEFAULT_TERM_COUNT = 1000;

	private static final Logger logger = Logger.getLogger(SearchService.class);

	@Autowired
	private SolrServer solr;

	public void setSolr(SolrServer solr) {
		this.solr = solr;
	}
	@Override
	public QueryResponse query(SolrQuery query) throws SolrServerException {
		if (logger.isInfoEnabled()) {
			logger.info("S�ker med " + query.getQuery());
		}
		return solr.query(query, METHOD.POST);
	}

	@Override
	public Set<String> analyze(String words) throws SolrServerException, IOException {
		if (logger.isInfoEnabled()) {
			logger.info("Analyserar " + words);
		}
		Set<String> stems = new HashSet<String>();
		FieldAnalysisRequest far = new FieldAnalysisRequest();
		// vi k�r analys mot text-indexet d� vi vet att den stammar
		far.addFieldName(ContentHelper.IX_TEXT);
		List<String> noFieldTypes = Collections.emptyList();
		far.setFieldTypes(noFieldTypes); // m�ste s�tta pga en bugg i solrj(?!)
		// det �r query-delen vi fr�mst �r intresserade av (ger a.getQueryPhases() != null nedan)
		far.setQuery(words);
		// men field value m�ste s�ttas
		far.setFieldValue(words);
		far.setMethod(METHOD.POST);

		FieldAnalysisResponse fares = far.process(solr);
		Analysis a = fares.getFieldNameAnalysis(ContentHelper.IX_TEXT);
		List<TokenInfo> lastTokenInfoList = null;
		for (AnalysisPhase ap: a.getQueryPhases()) {
			lastTokenInfoList = ap.getTokens();
		}
		if (lastTokenInfoList != null) {
			for (TokenInfo ti: lastTokenInfoList) {
				stems.add(ti.getText());
			}
		}
		return stems;
	}

	@Override
	public List<Term> terms(String index, String prefix, int removeBelow, int maxCount) throws SolrServerException {
		// * tolkas som del av termen s� s�dana kan vi inte ha med
		prefix = prefix.replace("*", "");
		if (logger.isInfoEnabled()) {
			logger.info("H�mtar termer f�r index: " + index + ", prefix: " + prefix);
		}
		// TODO: kommer finnas b�ttre s�tt att g�ra detta i senare solr/solrj-versioner
		List<Term> terms = new LinkedList<Term>();
		SolrQuery query = new SolrQuery();
		query.setQueryType("/terms");
		query.set(TermsParams.TERMS_FIELD, index);
		query.set(TermsParams.TERMS, true);
		query.set(TermsParams.TERMS_PREFIX_STR, prefix);
		query.set(TermsParams.TERMS_MINCOUNT, removeBelow);
		if (maxCount > 0) {
			query.set(TermsParams.TERMS_LIMIT, maxCount);
		} else {
			// solr har default 10 vilket �r lite s� vi s�tter alltid mer h�r
			query.set(TermsParams.TERMS_LIMIT, DEFAULT_TERM_COUNT);
		}
		QueryRequest qreq = new QueryRequest(query, METHOD.POST);
		@SuppressWarnings("unchecked")
		NamedList<Object> termList = (NamedList<Object>) qreq.process(solr).getResponse().get("terms");
		for (int i = 0; i < termList.size(); ++i) {
			String term = termList.getName(i);
			@SuppressWarnings("unchecked")
			NamedList<Object> items = (NamedList<Object>) termList.getVal(i);
			for (int j = 0; j < items.size(); ++j) {
				terms.add(new Term(term, items.getName(j), ((Number) items.getVal(j)).longValue()));
			}
		}
		return terms;
	}

	@Override
	public long getIndexCount(String serviceName) throws SolrServerException {
		if (logger.isInfoEnabled()) {
			logger.info("H�mtar antal f�r tj�nsten " + (serviceName != null ? serviceName : "*"));
		}

		SolrQuery query = new SolrQuery();
		query.setQuery(ContentHelper.I_IX_SERVICE + ":" + (serviceName != null ? serviceName : "*"));
		query.setFields(ContentHelper.I_IX_SERVICE);
		query.setRows(0);
		return query(query).getResults().getNumFound();
	}

	@Override
	public Map<String, Long> getIndexCounts() throws SolrServerException {
		if (logger.isInfoEnabled()) {
			logger.info("H�mtar antal f�r alla tj�nster");
		}
		Map<String, Long> countMap = new HashMap<String, Long>();
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.setFields(ContentHelper.I_IX_SERVICE);
		query.setRows(0);
		query.setFacet(true);
		query.addFacetField(ContentHelper.I_IX_SERVICE);
		QueryResponse qr = query(query);
		for (FacetField ff: qr.getFacetFields()) {
			for (Count value: ff.getValues()) {
				countMap.put(value.getName(), value.getCount());
			}
		}
		return countMap;
	}

	@Override
	public String getSolrURL() {
		return (solr instanceof CommonsHttpSolrServer ? ((CommonsHttpSolrServer) solr).getBaseURL() : null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public NamedList<Object> getIndexInfo() throws SolrServerException {
		final String qpath = "/admin/indexdiskinfo";
		SolrQuery query = new SolrQuery();
		query.setQueryType(qpath);
		QueryRequest qreq = new QueryRequest(query, METHOD.POST);
		QueryResponse qres = qreq.process(solr);
		return (NamedList<Object>) qres.getResponse().get("index");
	}
}
