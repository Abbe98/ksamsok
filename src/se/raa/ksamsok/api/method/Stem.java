package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.StartEndWriter;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Uf�r ordstammning av inskickad str�ng och ger tillbaka en lista med unika ordstammar.
 */
public class Stem implements APIMethod {

	public static final String METHOD_NAME = "stem";
	public static final String PARAM_WORDS = "words";
	private static final Logger logg = Logger.getLogger(Stem.class);

	private PrintWriter writer;
	private String words;

	public Stem(PrintWriter writer, Map<String, String> params) {
		this.writer = writer;
		this.words = StringUtils.trimToNull(params.get(PARAM_WORDS));
	}

	@Override
	public void performMethod() throws MissingParameterException,
			BadParameterException, DiagnosticException {
		if (words == null) {
			throw new MissingParameterException("Missing or empty parameter (" + PARAM_WORDS + ")",
					getClass().getName(), "Parameter " + PARAM_WORDS + " is required", false);
		}
		Analyzer a = ContentHelper.getSwedishAnalyzer();
		TokenStream ts = null;
		Set<String> stems = new HashSet<String>();
		try {
			ts = a.tokenStream(null, new StringReader(words));
			Token t = new Token();
			while((t = ts.next(t)) != null) {
				stems.add(t.term());
			}
			writeHead(stems);
			writeResult(stems);
			writeFot();
		} catch (IOException e) {
			throw new DiagnosticException("Ov�ntat IO-fel uppstod", "Stem.performMethod", e.getMessage(), true);
		} catch (Exception e) {
			throw new DiagnosticException("Ov�ntat fel uppstod vid ordstammning", "Stem.performMethod", e.getMessage(), true);
		} finally {
			if (ts != null) {
				try {
					ts.close();
				} catch (IOException e) {
					logg.warn("Fel vid st�ngning av tokenstr�m vid analys av index-text", e);
				}
			}
		}
	}

	/**
	 * Skriver ut b�rjan av svaret
	 * @param stemList
	 */
	protected void writeHead(Set<String> stemList){
		StartEndWriter.writeStart(writer);
		// TODO: detta �r inte tr�ds�kert! g�r dock likadant som �verallt annars tills vidare
		StartEndWriter.hasHead(true);
		writer.println("<numberOfStems>" + stemList.size() + "</numberOfStems>");
		writer.println("<stems>");
	}
	
	/**
	 * skriver ut resultatet av svaret
	 * @param termList
	 */
	protected void writeResult(Set<String> stemList) {
		for (String stem: stemList) {
			writer.print("<stem>");
			writer.println(StaticMethods.xmlEscape(stem));
			writer.print("</stem>");
		}
	}
	
	/**
	 * Skriver ut foten av svaret
	 */
	protected void writeFot() {
		writer.println("</stems>");
		writer.println("<echo>");
		writer.println("<method>" + METHOD_NAME + "</method>");
		writer.println("<words>" + StaticMethods.xmlEscape(words) + "</words>");
		writer.println("</echo>");
		StartEndWriter.writeEnd(writer);
		// TODO: detta �r inte tr�ds�kert! g�r dock likadant som �verallt annars tills vidare
		StartEndWriter.hasFoot(true);
	}

}
