package se.raa.ksamsok.api.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * inneh�ller statiska metoder som anv�nds av flera klasser i systemet
 * @author Henrik Hjalmarsson
 */
public class StaticMethods
{
	private static final Logger logger =
		Logger.getLogger("se.raa.ksamsok.api.StaticMethods");
	/**
	 * anv�nds f�r att escapa special tecken i s�kningar
	 * @param s str�ng med text
	 * @return ny str�ng med escape tecken fixade
	 */
	public static String escape(String s)
	{
		String escaped = QueryParser.escape(s);
		return escaped;
	}
	
	/**
	 * formaterar special tecken som ej �r till�tna i XML
	 * @param s text
	 * @return formaterad text
	 */
	public static String xmlEscape(String s) 
	{
		String escape = StringEscapeUtils.escapeXml(s);
		escape = escape.replaceAll("<", "&lt;");
		escape = escape.replaceAll(">", "&gt;");
		return escape;
	}
	
	/**
	 * analyserar ett query
	 * @param field
	 * @param value
	 * @return
	 * @throws DiagnosticException
	 */
	public static Query analyseQuery(String field, String value)
		throws DiagnosticException
	{	
		if(value.indexOf(" ") != -1 && ContentHelper.isAnalyzedIndex(field)) {
			PhraseQuery phraseQuery = new PhraseQuery();
			StringTokenizer tokenizer = new StringTokenizer(value, " ");
			// anv�nd svensk stamning f�r dessa analyserade index, samma 
			// som f�r indexeringen
			while (tokenizer.hasMoreTokens()) {
				String curValue = tokenizer.nextToken();
				// analysera s�kv�rdet pss som vid indexering
				curValue = CQL2Lucene.analyzeIndexText(curValue);
				// ta bara med termen om det ej �r ett stopp-ord
				if (curValue != null) {
					phraseQuery.add(new Term(field, curValue));
				}
			}
			logger.info(phraseQuery);
			return phraseQuery;
		}else {
			value = CQL2Lucene.transformValueForField(field, value);
			Term term = new Term(field, value);
			TermQuery termQuery = new TermQuery(term);
			return termQuery;
		}
	}
	
	/**
	 * encodar en url
	 * @param url
	 * @return
	 */
	public static String encode(String url) 
	{
		String result = "" + url;
		result = StringUtils.replace(result, " ", "%20");
		result = StringUtils.replace(result, "&", "%26");
		return result;
	}
	
	/**
	 * H�mtar ut parametrar med r�tt teckenkodning
	 * @param param parametern som skall h�mtas ut
	 * @return parametern i r�tt teckenkodning
	 */
	public static String getParam(String param)
	{
		try { //TODO vet inte om detta �r ultimat, men det funkar ;)
			if(param != null) {
				param = URLDecoder.decode(param, "UTF-8");
				param = new String(param.getBytes("ISO-8859-1"), "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return param;
	}

	/**
	 * Tar bort ett visst tecken ur en str�ng, t ex citationstecken
	 * @param s str�ngen som ska redigeras
	 * @param c tecknet som ska tas bort
	 * @return den nya str�ngen
	 */
	public static String removeChar(String s, char c) {
		   String r = "";
		   for (int i = 0; i < s.length(); i ++) {
		      if (s.charAt(i) != c) r += s.charAt(i);
		      }
		   return r;
		}
}