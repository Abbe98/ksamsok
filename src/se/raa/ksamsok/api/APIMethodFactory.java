package se.raa.ksamsok.api;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.AllIndexUniqueValueCount;
import se.raa.ksamsok.api.method.Facet;
import se.raa.ksamsok.api.method.Search;
import se.raa.ksamsok.api.method.SearchHelp;
import se.raa.ksamsok.api.method.Statistic;
import se.raa.ksamsok.api.method.StatisticSearch;

/**
 * Factory klass som bygger APIMethod objekt
 * @author Henrik Hjalmarsson
 */
public class APIMethodFactory 
{
	/** delare f�r att dela query str�ngar */
	private static final String DELIMITER = "|";
	//logger som anv�nds
	
	/**
	 * returnerar en instans av APIMethod beroende p� vilka parametrar som
	 * kommer in
	 * @param params mottagna parametrar
	 * @param writer f�r att skriva svaret
	 * @return APIMethod en istans av n�gon subklass till APIMethod
	 */
	public static APIMethod getAPIMethod(Map<String, String> params,
				PrintWriter writer)
			throws MissingParameterException, BadParameterException
	{
		//h�mtar ut metod namnet fr�n parameter mappen
		String method = params.get(APIMethod.METHOD);
		if(method == null)//m�ste alltid finnas en metod
		{
			throw new MissingParameterException("obligatorisk parameter " + 
					APIMethod.METHOD + " saknas",
					"APIMethodFactory.getAPIMethod", "metod saknas", false);
		}
		
		APIMethod m = null;
		
		//en ny if sats l�ggs till f�r varje ny metod
		if(method.equals(Search.METHOD_NAME))
		{
			m = getSearchObject(params, writer);
		}else if(method.equals(Statistic.METHOD_NAME))
		{
			m = getStatisticObject(params, writer);
		}else if(method.equals(StatisticSearch.METHOD_NAME))
		{
			m = getStatisticSearchObject(params, writer);
		}else if(method.equals(AllIndexUniqueValueCount.METHOD_NAME))
		{
			m = getAllIndexUniqueValueCountObject(params, writer);
		}else if(method.equals(Facet.METHOD_NAME))
		{
			m = getFacetObject(params, writer);
		}else if(method.equals(SearchHelp.METHOD_NAME))
		{
			m = getSearchHelpObject(params, writer);
		}
		else
		{
			throw new MissingParameterException("metoden " + method + 
					" finns inte", "APIMethodFactory.getAPIMethod",
					"felaktig metod", false);
		}
		
		return m;
	}

	/**
	 * skapar ett objekt av SearchHelp
	 * @param params
	 * @param writer
	 * @return SearchHelp objekt
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static APIMethod getSearchHelpObject(Map<String, String> params,
			PrintWriter writer) throws MissingParameterException, BadParameterException
	{
		SearchHelp m = null;
		List<String> indexList = getIndexList(params.get(SearchHelp.INDEX_PARAMETER));
		String prefix = getPrefix(params.get(SearchHelp.PREFIX_PARAMETER));
		int maxValueCount = 
			getMaxValueCount(params.get(SearchHelp.MAX_VALUE_COUNT_PARAMETER));
		m = new SearchHelp(writer, indexList, prefix, maxValueCount);
		return m;
	}

	

	/**
	 * skapar ett objekt av typen Facet
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static APIMethod getFacetObject(Map<String, String> params,
			PrintWriter writer) 
		throws MissingParameterException, BadParameterException
	{
		Facet m = null;
		String queryString = getQueryString(params.get(Facet.QUERY_PARAMS));
		Map<String,String> indexMap = 
			getIndexMapSingleValue(params.get(Facet.INDEX_PARAMETER), "*");
		m = new Facet(indexMap, writer, queryString);
		int removeBelow = getRemoveBelow(params.get(Facet.REMOVE_BELOW));
		m.setRemoveBelow(removeBelow);
		return m;
	}

	/**
	 * skapar ett ojekt av typen AllIndexUniqueValueCount
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 */
	private static APIMethod getAllIndexUniqueValueCountObject(Map<String,
			String> params, PrintWriter writer)
		throws MissingParameterException
	{
		AllIndexUniqueValueCount m = null;
		String queryString =
			getQueryString(params.get(AllIndexUniqueValueCount.QUERY_PARAMS));
		String indexString = params.get(AllIndexUniqueValueCount.INDEX_PARAMETER);
		Map<String,String> indexMap = null;
		if(indexString != null)
		{
			indexMap = getIndexMapSingleValue(indexString, "*");
		}
		
		m = new AllIndexUniqueValueCount(queryString, writer, indexMap);
		return m;
	}

	/*
	 * skapar ett objekt av StatisticSearch
	 */
	private static APIMethod getStatisticSearchObject(
			Map<String, String> params, PrintWriter writer)
		throws MissingParameterException, BadParameterException
	{
		StatisticSearch m = null;
		Map<String,String> indexMap = getIndexMapDoubleValue(params.get(
				StatisticSearch.INDEX_PARAMETER));
		String queryString = params.get(StatisticSearch.QUERY_PARAMS);
		queryString = getQueryString(queryString);
		
		m = new StatisticSearch(writer, queryString, indexMap);
		int removeBelow = getRemoveBelow(params.get(StatisticSearch.REMOVE_BELOW));
		m.setRemoveBelow(removeBelow);
		return m;
	}

	/*
	 * skapar ett Statistic objekt
	 */
	private static APIMethod getStatisticObject(Map<String, String> params,
			PrintWriter writer)
		throws MissingParameterException, BadParameterException
	{
		Statistic m;
		Map<String,String> indexMap = getIndexMapDoubleValue(params.get(
				Statistic.INDEX_PARAMETER));
		
		m = new Statistic(indexMap, writer);
		int removeBelow = getRemoveBelow(params.get(Statistic.REMOVE_BELOW));
		m.setRemoveBelow(removeBelow);
		return m;
	}

	/*
	 * skapar Search objektet
	 */
	private static APIMethod getSearchObject(Map<String, String> params,
			PrintWriter writer)
		throws MissingParameterException, BadParameterException
	{
		Search m;
		String query = params.get(Search.SEARCH_PARAMS);
		query = getQueryString(query);
		
		//s�tter valfria parametrar
		int hitsPerPage = Search.DEFAULT_HITS_PER_PAGE;
		String temp = params.get(Search.HITS_PER_PAGE);
		if(temp != null)
		{
			try
			{
				hitsPerPage = Integer.parseInt(temp);
			}catch(NumberFormatException e)
			{
				throw new BadParameterException("parametern " + 
						Search.HITS_PER_PAGE + " m�ste inneh�lla ett " +
								"numeriskt v�rde",
								"APIMethodFactory.getSearchObject", "icke " +
										"numeriskt v�rde", false);
			}
		}
		int startRecord = Search.DEFAULT_START_RECORD;
		temp = params.get(Search.START_RECORD);
		if(temp != null)
		{
			try
			{
				startRecord = Integer.parseInt(temp);
			}catch(NumberFormatException e)
			{
				throw new BadParameterException("parametern " + 
						Search.START_RECORD + " m�ste inneh�lla ett " +
								"numeriskt v�rde",
								"APIMethodFactory.getSearchObject", "icke " +
										"numeriskt v�rde", false);
			}
		}
		m = new Search(query, hitsPerPage, startRecord, writer);
		String sort = params.get(Search.SORT);
		if(sort != null)
		{	
			m.sortBy(sort);
			String sortConfig = params.get(Search.SORT_CONFIG);
			if(sortConfig != null)
			{
				if(sortConfig.equalsIgnoreCase(Search.SORT_DESC))
				{
					m.sortDesc(true);
				}
			}
		}
		
		String recordSchema = params.get(Search.RECORD_SCHEMA);
		if(recordSchema != null)
		{
			m.setRecordSchema(recordSchema);
		}
		return m;
	}
	
	private static String getQueryString(String queryString) 
		throws MissingParameterException
	{
		if(queryString == null || queryString.trim().length() < 1)
		{
			throw new MissingParameterException("parametern query saknas eller �r tom",
					"APIMethodFactory.getQueryString", null, false);
		}
		return queryString;
	}
	
	private static List<String> getIndexList(String indexString) 
		throws MissingParameterException
	{
		List<String> indexList = new ArrayList<String>();
		if(indexString == null || indexString.trim().length() < 1)
		{
			throw new MissingParameterException("parametern index saknas eller �r tom",
					"APIMethodFactory.getIndexList", null, false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		while(indexTokenizer.hasMoreTokens())
		{
			indexList.add(indexTokenizer.nextToken());
		}
		return indexList;
	}
	
	private static String getPrefix(String prefix)
	{
		if(prefix == null)
		{
			prefix = "*";
		}else if(!prefix.endsWith("*"))
		{
			prefix += "*";
		}
		return prefix;
	}
	
	private static int getMaxValueCount(String maxValueCountString) 
	throws BadParameterException
	{
		int maxValueCount = 0;
		if(maxValueCountString == null)
		{
			maxValueCount = SearchHelp.DEFAULT_MAX_VALUE_COUNT;
		}else
		{
			try
			{
				maxValueCount = Integer.parseInt(maxValueCountString);
			}catch(NumberFormatException e)
			{
				throw new BadParameterException("parametern maxValueCount m�ste vara " +
						"ett numeriskt v�rde", "APIMethodFactory.getMaxValueCount",
						null, false);
			}
		}
		return maxValueCount;
	}
	
	private static Map<String,String> getIndexMapSingleValue(String indexString,
			String value) throws MissingParameterException
	{
		Map<String,String> indexMap = new HashMap<String,String>();
		if(indexString == null || indexString.trim().length() < 1)
		{
			throw new MissingParameterException("parametern index saknas eller �r tom",
					"APIMethodFactory.getIndexMapSingleValue", null, false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		while(indexTokenizer.hasMoreTokens())
		{
			indexMap.put(indexTokenizer.nextToken(), value);
		}
		return indexMap;
	}
	
	private static int getRemoveBelow(String removeBelowString) 
		throws BadParameterException
	{
		int removeBelow = 0;
		if(removeBelowString != null)
		{
			try
			{
				removeBelow = Integer.parseInt(removeBelowString);
			}catch(NumberFormatException e)
			{
				throw new BadParameterException("Parametern removeBelow m�ste " +
						"inneh�lla ett numeriskt v�rde",
						"APIMethodFactory.getRemoveBelow", null, false);
			}
		}
		return removeBelow;
	}
	
	private static Map<String,String> getIndexMapDoubleValue(String indexString) 
		throws MissingParameterException, BadParameterException
	{	
		if(indexString == null || indexString.trim().length() < 1)
		{
			throw new MissingParameterException("parametern " + 
					StatisticSearch.INDEX_PARAMETER + " saknas eller �r tom",
					"APIMethodFactory.getStatisticSearchObject", "index " +
							"parametern saknas", false);
		}
		StringTokenizer indexTokenizer = 
			new StringTokenizer(indexString, DELIMITER);
		HashMap<String,String> indexMap = new HashMap<String,String>();
		while(indexTokenizer.hasMoreTokens())
		{
			String[] tokens = indexTokenizer.nextToken().split("=");
			String index = null;
			String value = null;
			if(tokens.length < 2)
			{
				throw new BadParameterException("parametern " + 
						StatisticSearch.INDEX_PARAMETER + " �r felskriven",
						"APIMethodFactory.getStatisticSearchObject",
						"syntax error i index parametern", false);
			}
			for(int i = 0; i < 2; i++)
			{
				if(i == 0)
				{
					index = tokens[i];
				}
				if(i == 1)
				{
					value = tokens[i];
				}
			}
			indexMap.put(index, value);
		}
		return indexMap;
	}
}