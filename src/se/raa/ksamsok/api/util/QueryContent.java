package se.raa.ksamsok.api.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import se.raa.ksamsok.api.exception.DiagnosticException;

/**
 * Klass som inneh�ller ett info om ett query
 * @author Henrik Hjalmarsson
 */
public class QueryContent
{
	private HashMap<String,String> terms;
	private int hits;
	
	/**
	 * skapar ett objekt av QueryContent
	 */
	public QueryContent()
	{
		terms = new HashMap<String,String>();
		hits = 0;
	}
	
	/**
	 * l�gger till en term
	 * @param index f�r term
	 * @param term v�rde f�r term
	 */
	public void addTerm(String index, String term)
	{
		terms.put(index, term);
	}
	
	/**
	 * s�tter antalet tr�ffar f�r query
	 * @param hits tr�ffar
	 */
	public void setHits(int hits)
	{
		this.hits = hits;
	}
	
	/**
	 * returnerar antalet tr�ffar f�r query
	 * @return antal tr�ffar
	 */
	public int getHits()
	{
		return hits;
	}
	
	/**
	 * returnerar mappen med termer
	 * @return Map
	 */
	public Map<String,String> getTermMap()
	{
		return terms;
	}
	
	/**
	 * skapar en query str�ng av termer
	 * @return query str�ng
	 */
	public String getQueryString()
	{
		Set<String> indexSet = terms.keySet();
		String queryString = "";

		for(String index : indexSet)
		{//TODO tror denna skall fungera f�r alla v�rden
			String term = StaticMethods.escape(terms.get(index));
			queryString += index + "=\"" + term + "\" AND ";
		}
		queryString = queryString.substring(0, queryString.length() - 5);
		return queryString;
	}
	
	public Query getQuery()
		throws DiagnosticException
	{
		BooleanQuery query = new BooleanQuery();
		for(String index : terms.keySet())
		{
			String value = terms.get(index);
			Query q = StaticMethods.analyseQuery(index, value);
			query.add(q, BooleanClause.Occur.MUST);
		}
		return query;
	}
	
	/**
	 * skapar en query str�ng av lagrade termer och given query str�ng
	 * @param query
	 * @return query str�ng
	 */
	public String getQueryString(String query)
	{
		String queryString = getQueryString();
		queryString += " AND " + query;
		return queryString;
	}
}