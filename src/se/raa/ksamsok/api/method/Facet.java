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
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * Klass gjort f�r att enkelt implementera facet s�kningar i TA
 * @author Henrik Hjalmarsson
 */
public class Facet extends StatisticSearch 
{	
	/** metodens namn */
	public static final String METHOD_NAME = "facet";
	
	private static Logger logger;

	/**
	 * skapar ett objekt av Facet
	 * @param indexMap de index som skall ing� i facetten
	 * @param writer f�r att skriva resultatet
	 * @param queryString filtrerar resultatet efter query
	 */
	public Facet(Map<String, String> indexMap, PrintWriter writer, String queryString) 
	{
		super(writer, queryString, indexMap);
		logger = Logger.getLogger("se.raa.ksamsok.api.method.Facet");
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
			throw new DiagnosticException("Ov�ntat parser fel uppstod detta beror troligen p� att query str�ngen inte f�ljer CQL syntax. Var god kontrollera query str�ngen eller kontakta system administrat�r f�r systemet du anv�nder dig av.", "Facet.performMethod", null, false);
		} catch (IOException e) {
			throw new DiagnosticException("Ov�ntat IO fel. var god f�rs�k igen", "Facet.performMethod", e.getMessage(), true); 
		}finally {
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
			for(int i = 0; i < queryContentList.size(); i++) {	
				QueryContent queryContent = queryContentList.get(i);
				Query query = queryContent.getQuery();
				if(logger.isDebugEnabled()) {
					logger.debug("about to make " + i + " queries");
					logger.debug(query);
				}
				TopDocs topDocs = searcher.search(query, qwf, 1);
				if(topDocs.totalHits < removeBelow) {
					queryContentList.remove(i);
					i--;
				}else {
					queryContent.setHits(topDocs.totalHits);
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