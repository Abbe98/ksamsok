package se.raa.ksamsok.api.util;

import java.io.PrintWriter;

import se.raa.ksamsok.api.method.APIMethod;

/**
 * Klass f�r att h�lla koll p� och skriva ut huvuddel av XML svar
 * och fot av XML fil
 * @author Henrik Hjalmarsson
 */
public class StartEndWriter
{
	private static boolean head;
	private static boolean foot;
	private static String stylesheet;
	
	/**
	 * returnerar true om huvud �r skrivet
	 * @return
	 */
	public static boolean hasHead()
	{
		return head;
	}
	
	/**
	 * returnerar true om fot �r skriven
	 * @return
	 */
	public static boolean hasFoot()
	{
		return foot;
	}
	
	/**
	 * resetar att fot och huvud �r skrivna
	 */
	public static void reset()
	{
		head = false;
		foot = false;
		stylesheet = null;
	}
	
	/**
	 * anger om huvud �r skrivet
	 * @param b
	 */
	public static void hasHead(boolean b)
	{
		head = b;
	}
	
	/**
	 * anger om fot �r skriven
	 * @param b
	 */
	public static void hasFoot(boolean b)
	{
		foot = b;
	}
	
	/**
	 * anger vilket stylesheet som skall anv�ndas
	 * @param stylesheet
	 */
	public static void setStylesheet(String stylesheet)
	{
		StartEndWriter.stylesheet = stylesheet;
	}
	
	/**
	 * skriver ut huvuddelen av XML svar
	 * @param writer
	 */
	public static void writeStart(PrintWriter writer)
	{
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		if (stylesheet != null && stylesheet.trim().length() > 0)
		{	
			writer.println("<?xml-stylesheet type=\"text/xsl\" href=\""
					+ stylesheet.replace("\"", "&quot;") + "\"?>");
		}
		writer.println("<result>");
		writer.println("<version>" + APIMethod.API_VERSION + "</version>");
	}
	
	/**
	 * Skriver ut foten av XML svar
	 * @param writer
	 */
	public static void writeEnd(PrintWriter writer)
	{
		writer.println("</result>");
	}
	
	/**
	 * skriver ut error om s�dant uppst�r
	 * @param writer
	 * @param e
	 */
	public static void writeError(PrintWriter writer, Exception e)
	{
		if(!head) {
			writeStart(writer);
		}
		writer.println("<error>");
		writer.println(e.getMessage());
		writer.println("</error>");
		if(!foot) {
			writeEnd(writer);
		}
	}
}
