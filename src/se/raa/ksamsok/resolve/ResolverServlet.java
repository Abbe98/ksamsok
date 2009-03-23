package se.raa.ksamsok.resolve;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import se.raa.ksamsok.harvest.HarvesterServlet;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * Enkel servlet som s�ker i lucene mha pathInfo som en identifierare och g�r redirect 
 * till respektive tj�nst beroende p� format eller levererar lagrad rdf eller xml. 
 */
public class ResolverServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ResolverServlet.class);
	// urlar att redirecta till f�r inte starta med detta (gemener)
	private static final String badURLPrefix = "http://kulturarvsdata.se/";

	/**
	 * Enum f�r de olika formaten som st�ds.
	 */
	private enum Format {
		RDF, HTML, MUSEUMDAT, XML;

		/**
		 * Parsar en str�ng med formatet och ger motsvarande konstant.
		 * 
		 * @param formatString formatstr�ng
		 * @return formatkonstant eller null
		 */
		static Format parseFormat(String formatString) {
			Format format = null;
			if ("rdf".equals(formatString)) {
				format = RDF;
			} else if ("html".equals(formatString)) {
				format = HTML;
			} else if ("museumdat".equals(formatString)) {
				format = MUSEUMDAT;
			} else if ("xml".equals(formatString)) {
				format = XML;
			}
			return format;
		}
	};

	/**
	 * F�rs�ker kontrollera om requesten �r n�got resolverservleten ska hantera. Om det inte
	 * �r det skickas requesten vidare mha request dispatchers.
	 * 
	 * @param req request
	 * @param resp response
	 * @return de olika delarna i pathInfo, eller null om requesten inte ska hanteras av resolvern
	 * @throws ServletException
	 * @throws IOException
	 */
	protected String[] checkAndForwardRequests(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		String path = req.getPathInfo();
		// special d� resolverservlet verkar "k�ka" upp default-sidehanteringen
		if ("/admin/".equals(path)) {
			resp.sendRedirect("index.jsp");
			return null;
		}
		// hantera "specialfallet" med /resurser/a/b/c[/d]
		if (path != null && path.startsWith("/resurser")) {
			path = path.substring(9);
		}
		String[] pathComponents = null;

		if (path != null && !path.contains(".") && (pathComponents = path.substring(1).split("/")) != null) {
			if (pathComponents.length < 3 || pathComponents.length > 4) {
				pathComponents = null;
			}
		}
		if (pathComponents == null) {
			forwardRequest(req, resp);
		}
		return pathComponents;
	}

	/**
	 * F�rs�ker g�ra forward p� ej hanterad request genom att hitta en passande request
	 * dispatcher.
	 * 
	 * @param req request
	 * @param resp response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void forwardRequest(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		RequestDispatcher rd;
		// TODO: dessa dispatchers fungerar f�r tomcat, fungerar de f�r �vriga?
		String name = "default";
		if (req.getRequestURI().endsWith(".jsp")) {
			name = "jsp";
		}
		rd = getServletContext().getNamedDispatcher(name);
		if (rd != null) {
			rd.forward(req, resp);
		} else {
			logger.error("Hittade inte dispatcher f�r \"" + name + "\"" +
					", andra namn f�r dessa eller ej tomcat?");
			resp.sendError(500, "Kunde inte skicka request vidare, ingen dispatcher f�r \"" +
					name + "\"");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String[] pathComponents = checkAndForwardRequests(req, resp);
		if (pathComponents == null) {
			return;
		}
		String path;
		Format format;
		// hantera olika format
		if (pathComponents.length == 4) {
			format = Format.parseFormat(pathComponents[2].toLowerCase());
			if (format == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ogiltigt format: " + pathComponents[2]);
				}
				resp.sendError(404, "Ogiltigt format " + pathComponents[2]);
				return;
			}
			path = pathComponents[0] + "/" + pathComponents[1] + "/" + pathComponents[3];
		} else {
			format = Format.RDF;
			path = pathComponents[0] + "/" + pathComponents[1] + "/" + pathComponents[2];
		}
		IndexSearcher is = LuceneServlet.getInstance().borrowIndexSearcher();
		try {
			PrintWriter writer;
			String urli, urle;
			String content = null;
			urli = "http://kulturarvsdata.se/" + path;
			Query q = new TermQuery(new Term(ContentHelper.IX_ITEMID, urli));
			if (logger.isDebugEnabled()) {
				logger.debug("resolve av (" + format + ") uri: " + urli);
			}
			TopDocs hits = is.search(q, 1);
			if (hits.totalHits != 1) {
				if (logger.isDebugEnabled()) {
					logger.debug("Hittade inte post f�r q: " + q);
				}
				resp.sendError(404, "Hittade inte post f�r s�kv�g");
				return;
			}
			Document d = is.doc(hits.scoreDocs[0].doc);
			switch (format) {
			case RDF:
				resp.setContentType("application/rdf+xml; charset=UTF-8");
				writer = resp.getWriter();
				urli = d.get(ContentHelper.IX_ITEMID);
				// xml-header
				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				content = HarvesterServlet.getInstance().getHarvestRepositoryManager().getXMLData(urli);
				if (content == null) {
					logger.warn("Hittade inte rdf f�r post med uri: " + urli);
					resp.sendError(404, "Ingen rdf f�r post");
					return;
				}
				writer.println(content);
				writer.flush();
				break;

			case XML:
				resp.setContentType("text/xml; charset=UTF-8");
				writer = resp.getWriter();
				// xml-header
				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				byte[] pres = d.getBinaryValue(ContentHelper.I_IX_PRES);
				if (pres != null) {
					content = new String(pres, "UTF-8");
				}
				if (content == null) {
					logger.warn("Hittade inte xml f�r post med uri: " + urli);
					resp.sendError(404, "Ingen presentations-xml f�r post");
					return;
				}
				writer.println(content);
				writer.flush();
				break;

			case HTML:
				urle = d.get(ContentHelper.I_IX_HTML_URL);
				if (urle != null) {
					if (urle.toLowerCase().startsWith(badURLPrefix)) {
						logger.warn("HTML-l�nk g�r felaktigt till " + badURLPrefix +
								" f�r " + urli + ": " + urle);
						resp.sendError(404, "Felaktig html-url att skicka vidare till");
					} else {
						resp.sendRedirect(urle);
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Hittade inte html-url f�r post med uri: " + urli);
					}
					resp.sendError(404, "Hittade inte html-url att skicka vidare till");
				}
				break;

			case MUSEUMDAT:
				urle = d.get(ContentHelper.I_IX_MUSEUMDAT_URL);
				if (urle != null) {
					if (urle.toLowerCase().startsWith(badURLPrefix)) {
						logger.warn("Museumdat-l�nk g�r felaktigt till " + badURLPrefix +
								" f�r " + urli + ": " + urle);
						resp.sendError(404, "Felaktigt museumdat-url att skicka vidare till");
					} else {
						resp.sendRedirect(urle);
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Hittade inte museumdat-url f�r post med uri: " + urli);
					}
					resp.sendError(404, "Hittade inte museumdat-url att skicka vidare till");
				}
				break;

				default:
					logger.warn("Ogiltigt format: " + format);
					resp.sendError(404, "Ogiltigt format");
			}

		} catch (Exception e) {
			logger.error("Fel vid uppslagning av url, path:" + path + ", format: " + format, e);
			throw new ServletException("Fel vid uppslagning av url", e);
		} finally {
			LuceneServlet.getInstance().returnIndexSearcher(is);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		forwardRequest(req, resp);
	}
}
