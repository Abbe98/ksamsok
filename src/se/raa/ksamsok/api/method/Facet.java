package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TopDocs;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.QueryContent;
import se.raa.ksamsok.api.util.StartEndWriter;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * Klass gjort f�r att enkelt implementera facet s�kningar i TA
 * @author Henrik Hjalmarsson
 */
public class Facet extends StatisticSearch 
{	
	/** metodens namn */
	public static final String METHOD_NAME = "facet";
	
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.method.Facet");

	/**
	 * skapar ett objekt av Facet
	 * @param indexMap de index som skall ing� i facetten
	 * @param writer f�r att skriva resultatet
	 * @param queryString filtrerar resultatet efter query
	 */
	public Facet(Map<String, String> indexMap, PrintWriter writer, String queryString) 
	{
		super(writer, queryString, indexMap); 
	}

	@Override
	public void performMethod() 
		throws BadParameterException, DiagnosticException, 
			MissingParameterException 
	{
		IndexSearcher searcher = LuceneServlet.getInstance().borrowIndexSearcher();
		try {	
			Map<String,Set<Term>> termMap = buildTermMap(searcher);
			List<QueryContent> queryContentList = 
				convertTermMapToQueryContentList(termMap);
			CQLParser parser = new CQLParser();
			CQLNode node = parser.parse(queryString);
			Query filterQuery = CQL2Lucene.makeQuery(node);
			doFacet(searcher, queryContentList, filterQuery);
			writeHead(queryContentList);
			writeResult(queryContentList);
			writeFot();
		} catch (CQLParseException e) {
			throw new DiagnosticException("Ov�ntat parserfel uppstod - detta beror troligen p� att query str�ngen inte f�ljer CQL syntax. Var god kontrollera query-str�ngen eller kontakta systemadministrat�ren f�r systemet du anv�nder dig av.", "Facet.performMethod", null, false);
		} catch (IOException e) {
			throw new DiagnosticException("Ov�ntat IO-fel, var god f�rs�k igen", "Facet.performMethod", e.getMessage(), true); 
		} finally {
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}

	@Override
	protected void writeFot()
	{
		writer.println("<echo>");
		writer.println("<method>" + Facet.METHOD_NAME + "</method>");
		for(String index : indexMap.keySet()) {	
			writer.println("<index>" + index + "</index>");
		}
		writer.println("<query>" + StaticMethods.xmlEscape(queryString) + "</query>");
		writer.println("</echo>");
		StartEndWriter.writeEnd(writer);
		StartEndWriter.hasFoot(true);
	}

	/**
	 * Tar ut facetter utifr�n den inskickade fr�gan. Anv�nd bara om removeBelow
	 * �r > 0 d� denna metod anv�nder sig av termfrekvensvektorer f�r de dokument
	 * som tr�ffas och d�rf�r aldrig kan lista alla v�rden (f�rekomst 0 ggr).
	 * 
	 * @param searcher lucene-searcher
	 * @param filterQuery s�kfr�ga
	 * @return lista med {@linkplain QueryContent}-instanser
	 * @throws DiagnosticException vid alla fel
	 */
	/*
	 * F�rs�k med termvektorer som inte blev helt bra. F�rsta s�kningen (med samma parametrar) tar
	 * l�ngre tid �n alternativet men sen g�r det oftast snabbare, tyv�rr s� s�ker folk inte p�
	 * exakt samma sak flera g�nger och eftersom det cachas internt i lucene vet man inte hur
	 * l�nge fr�gecachen "lever". F�r att detta ska funka alls m�ste f�lt till skapas med flaggan
	 * Field.TermVector.YES, dvs med new Field(... Field.Store.NO, Field.Index.NOT_ANALYZED, Field.TermVector.YES);
	 * vid indexering, annars lagras ingen termvektor.
	private List<QueryContent> doFacet(IndexSearcher searcher, DocIdSet idSet) throws DiagnosticException {
		List<QueryContent> qcList = null;
		//Filter qwf = new CachingWrapperFilter(new QueryWrapperFilter(filterQuery));
		try {
			// h�mta doc-id:n
			//DocIdSet idSet = qwf.getDocIdSet(searcher.getIndexReader());
			DocIdSetIterator iter = idSet.iterator();
			qcList = new LinkedList<QueryContent>();
			// map f�r att spara undan tr�ffar
			Map<String, Map<String, Integer>> counts = new HashMap<String, Map<String,Integer>>();
			Map<String, Integer> count;
			while (iter.next()) {
				// f�r varje dokument och index h�mta termfreq-vektor och uppdatera
				// totala antalet f�rekomster av termer i dokumentm�ngden
				for (Entry<String, String> indices: indexMap.entrySet()) {
					String index = CQL2Lucene.translateIndexName(indices.getKey());
					TermFreqVector v = searcher.getIndexReader().getTermFreqVector(iter.doc(), index);
					if (v == null) {
						continue;
					}
					for (String term: v.getTerms()) {
						count = counts.get(index);
						if (count == null) {
							// f�rsta g�ngen indexet anv�nds
							count = new HashMap<String, Integer>();
							counts.put(index, count);
						}
						Integer c = count.get(term);
						if (c == null) {
							// f�rsta f�rekomsten av termen
							c = 1;
						} else {
							++c;
						}
						count.put(term, c);
					}
				}
			}
			for (Entry<String, Map<String, Integer>> indexNames: counts.entrySet()) {
				String indexName = indexNames.getKey();
				// m�ste indexets v�rden konverteras?
				final boolean isIsoIndex = ContentHelper.isISO8601DateYearIndex(indexName);
				String term;
				int termCount;
				for (Entry<String, Integer> termCounts: indexNames.getValue().entrySet()) {
					QueryContent qc = new QueryContent();
					term = termCounts.getKey();
					termCount = termCounts.getValue();
					// filtrera bort de som har f�r f� tr�ffar
					if (termCount >= removeBelow) {
						// konvertera ev v�rdet till n�t l�sbart
						if (isIsoIndex) {
							term = Long.toString(ContentHelper.transformLuceneStringToLong(term));
						}
						qc.addTerm(indexName, term);
						qc.setHits(termCount);
						qcList.add(qc);
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Problem att ta fram facetter (removeBelow: " + removeBelow + ")", t);
			throw new DiagnosticException("Problem att ta fram facetter", "Facet.doFacet", t.getMessage(), false);
		}
		return qcList;
	}
	*/

	/**
	 * utf�r facet s�kningen
	 * @param searcher
	 * @param queryContentList
	 * @throws DiagnosticException
	 */
	private void doFacet(IndexSearcher searcher, List<QueryContent> queryContentList,
			Query filterQuery) 
		throws DiagnosticException
	{
		try {
			// anv�nd fr�gan som ett filter och cacha upp filterresultatet
			Filter qwf = new CachingWrapperFilter(new QueryWrapperFilter(filterQuery));
			if (logger.isDebugEnabled()) {
				logger.debug("about to make " + queryContentList.size() +
						" queries filtered by " + filterQuery);
			}
			// TODO: datastrukturen/algoritmen b�r kanske �ndras h�r d� det borde bli
			//       en del on�digt kopierande inne i ArrayList n�r element tas bort
			for (int i = 0; i < queryContentList.size(); i++) {	
				QueryContent queryContent = queryContentList.get(i);
				Query query = queryContent.getQuery();
				TopDocs topDocs = searcher.search(query, qwf, 1);
				if (topDocs.totalHits < removeBelow) {
					queryContentList.remove(i);
					i--;
				} else {
					queryContent.setHits(topDocs.totalHits);
					Map<String, String> termMap = queryContent.getTermMap();
					// g� igenom indexen f�r att se om v�rdena beh�ver �vers�ttas f�r att kunna visas
					for (Map.Entry<String, String> indexTerm: termMap.entrySet()) {
						if (ContentHelper.isISO8601DateYearIndex(indexTerm.getKey())) {
							long year = ContentHelper.transformLuceneStringToLong(indexTerm.getValue());
							indexTerm.setValue(Long.toString(year));
						}
					}
					queryContentList.set(i, queryContent);
				}
			}
		} catch (IOException e) {
			throw new DiagnosticException("Ov�ntat IO fel uppstod. Var god f�rs�k igen.", "Facet.doFacet", e.getMessage(), true);
		}
	}
	
	/**
	 * converterar term mappen till en lista med QueryContent Objekt.
	 * detta f�r att denna metod ej skall g�ra kartesisk produkt p� alla v�rden.
	 * @param termMap
	 * @return List<QueryContent>
	 */
	protected List<QueryContent> convertTermMapToQueryContentList(
			Map<String,Set<Term>> termMap)
	{
		List<QueryContent> queryContentList =  new ArrayList<QueryContent>();
		for(String index : termMap.keySet()) {
			for(Term term: termMap.get(index)) {
				QueryContent queryContent = new QueryContent();
				queryContent.addTerm(index, term.text());
				queryContentList.add(queryContent);
			}
		}
		return queryContentList;
	}
}