package se.raa.ksamsok.api.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;

/**
 * inneh�ller statiska metoder som anv�nds av flera klasser i systemet
 * @author Henrik Hjalmarsson
 */
public class StaticMethods
{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.StaticMethods");
	/**
	 * anv�nds f�r att escapa special tecken i s�kningar
	 * @param s str�ng med text
	 * @return ny str�ng med escape tecken fixade
	 */
	public static String escape(String s)
	{
		String escaped = ClientUtils.escapeQueryChars(s);
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
		try {//Vet inte om detta �r ultimat. Men det tycks funka
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