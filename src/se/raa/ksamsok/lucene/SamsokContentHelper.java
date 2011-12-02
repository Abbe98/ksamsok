package se.raa.ksamsok.lucene;

import static se.raa.ksamsok.lucene.RDFUtil.extractSingleValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.uriPrefix;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import javax.vecmath.Point2d;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.Base64;
import org.jrdf.graph.AnyObjectNode;
import org.jrdf.graph.AnySubjectNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import se.raa.ksamsok.harvest.ExtractedInfo;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.spatial.GMLInfoHolder;
import se.raa.ksamsok.spatial.GMLUtil;

public class SamsokContentHelper extends ContentHelper {

	private static final Logger logger = Logger.getLogger(SamsokContentHelper.class);

	private static DocumentBuilderFactory xmlFact;
	private static TransformerFactory xformerFact;

	static {
		xmlFact = DocumentBuilderFactory.newInstance();
	    xmlFact.setNamespaceAware(true);
	    xformerFact = TransformerFactory.newInstance();
	}

	@Override
	public SolrInputDocument createSolrDocument(HarvestService service,
			String xmlContent, Date added) throws Exception {
		Graph graph = null;
		String identifier = null;
		SolrInputDocument luceneDoc = null;
		try {
			graph = RDFUtil.parseGraph(xmlContent);

			GraphElementFactory elementFactory = graph.getElementFactory();
			// grund
			URIReference rdfType = elementFactory.createURIReference(SamsokProtocol.uri_rdfType);
			URIReference samsokEntity = elementFactory.createURIReference(SamsokProtocol.uri_samsokEntity);
			URIReference rPres = elementFactory.createURIReference(SamsokProtocol.uri_rPres);
			// url
			URIReference rURL = elementFactory.createURIReference(SamsokProtocol.uri_rURL);
			URIReference rMuseumdatURL = elementFactory.createURIReference(SamsokProtocol.uri_rMuseumdatURL);
			// special
			URIReference rItemForIndexing = elementFactory.createURIReference(SamsokProtocol.uri_rItemForIndexing);

			SubjectNode s = null;
			for (Triple triple: graph.find(AnySubjectNode.ANY_SUBJECT_NODE, rdfType, samsokEntity)) {
				if (s != null) {
					throw new Exception("Ska bara finnas en entity i rdf-grafen");
				}
				s = triple.getSubject();
			}
			if (s == null) {
				logger.error("Hittade ingen entity i rdf-grafen:\n" + graph);
				throw new Exception("Hittade ingen entity i rdf-grafen");
			}
			identifier = s.toString();
			// kolla om denna post inte ska indexeras och returnera i s� fall null
			// notera att detta g�r att inte posten indexeras alls vilket kr�ver ett
			// specialfall i resolverservleten d� den fr�mst jobbar mot lucene-indexet
			String itemForIndexing = RDFUtil.extractSingleValue(graph, s, rItemForIndexing, null);
			if ("n".equals(itemForIndexing)) {
				return null;
			}
			LinkedList<String> gmlGeometries = new LinkedList<String>();
			LinkedList<String> relations = new LinkedList<String>();
			SamsokProtocolHandler sph = getProtocolHandlerForVersion(graph, s);
			luceneDoc = sph.handle(service, added, relations, gmlGeometries);

			// den unika identifieraren
			luceneDoc.addField(IX_ITEMID, identifier);

			// interna system-index

			// tj�nst-id
			luceneDoc.addField(I_IX_SERVICE, service.getId());

			// html-url
			String url = extractSingleValue(graph, s, rURL, null);
			if (url != null) {
				if (url.toLowerCase().startsWith(uriPrefix)) {
					addProblemMessage("HTML URL starts with " + uriPrefix);
				}
				luceneDoc.addField(I_IX_HTML_URL, url);
			}
			// museumdat-url
			url = extractSingleValue(graph, s, rMuseumdatURL, null);
			if (url != null) {
				if (url.toLowerCase().startsWith(uriPrefix)) {
					addProblemMessage("Museumdat URL starts with " + uriPrefix);
				}
				luceneDoc.addField(I_IX_MUSEUMDAT_URL, url);
			}

			// lagra den f�rsta geometrins centroid
			if (gmlGeometries.size() > 0) {
				String gml = gmlGeometries.getFirst();
				if (gmlGeometries.size() > 1 && logger.isDebugEnabled()) {
					logger.debug("H�mtade " + gmlGeometries.size() +
							" geometrier f�r " + identifier + ", kommer bara " +
							"att anv�nda den f�rsta");
				}
				try {
					Point2d p = GMLUtil.getLonLatCentroid(gml);
					luceneDoc.addField(I_IX_LON, p.x);
					luceneDoc.addField(I_IX_LAT, p.y);
				} catch (Exception e) {
					addProblemMessage("Error when indexing geometries for " + identifier +
							": " + e.getMessage());
				}
			}

			// l�gg in relationer i specialstruktur/index (typ|uri)
			if (relations.size() > 0) {
				for (String value: relations) {
					luceneDoc.addField(I_IX_RELATIONS, value);
				}
			}

			// h�mta ut presentationsblocket
			String pres = extractSingleValue(graph, s, rPres, null);
			if (pres != null && pres.length() > 0) {
				// verifiera att det �r xml
				// TODO: kontrollera korrekt schema ocks�
				Document doc = parseDocument(pres);
				// serialisera som ett xml-fragment, dvs utan xml-deklaration
				pres = serializeDocumentAsFragment(doc);
				// lagra bin�rt, kodat i UTF-8
				byte[] presBytes = pres.getBytes("UTF-8");
				luceneDoc.addField(I_IX_PRES, Base64.byteArrayToBase64(presBytes, 0, presBytes.length));
			}

			// lagra rdf:en 
			byte[] rdfBytes = xmlContent.getBytes("UTF-8");
			luceneDoc.addField(I_IX_RDF, Base64.byteArrayToBase64(rdfBytes, 0, rdfBytes.length));

		} catch (Exception e) {
			// TODO: kasta exception/r�kna felen/annat?
			logger.error("Fel vid skapande av lucenedokument f�r " + identifier + ": " + e.getMessage());
			throw e;
		} finally {
			if (graph != null) {
				graph.close();
			}
		}
		return luceneDoc;
	}

	private SamsokProtocolHandler getProtocolHandlerForVersion(Graph graph, SubjectNode s)  throws Exception {
		URIReference ksamsokVersion = graph.getElementFactory().createURIReference(SamsokProtocol.uri_rKsamsokVersion);
		String protocolVersion = RDFUtil.extractSingleValue(graph, s, ksamsokVersion, null);
		if (protocolVersion == null) {
			logger.error("Hittade ingen protokollversion i rdf-grafen:\n" + graph);
			throw new Exception("Hittade ingen protokollversion i rdf-grafen");
		}

		Double protocol;
		SamsokProtocolHandler handler = null;
		try {
			protocol = Double.parseDouble(protocolVersion);
		} catch (Exception e) {
			logger.error("Ej numeriskt protokollversionsnummer: " + protocolVersion);
			throw new Exception("Ej numeriskt protokollversionsnummer: " + protocolVersion);
		}
		final double latest = 1.1;
		if (protocol == latest) {
			handler = new SamsokProtocolHandler_1_1(graph, s);
		} else if (protocol > 0 && protocol <= 1.0) {
			handler = new SamsokProtocolHandler_0_TO_1_0(graph, s);
		} else {
			logger.error("Ej hanterat versionsnummer: " + protocolVersion);
			throw new Exception("Ej hanterat versionsnummer: " + protocolVersion);
		}
		return handler;
	}

	@Override
	public ExtractedInfo extractInfo(String xmlContent,
			GMLInfoHolder gmlInfoHolder) throws Exception {
		String identifier = null;
		String htmlURL = null;
		Graph graph = null;
		ExtractedInfo info = new ExtractedInfo();
		try {
			graph = RDFUtil.parseGraph(xmlContent);
	
			GraphElementFactory elementFactory = graph.getElementFactory();
			URIReference rdfType = elementFactory.createURIReference(SamsokProtocol.uri_rdfType);
			URIReference samsokEntity = elementFactory.createURIReference(SamsokProtocol.uri_samsokEntity);
			URIReference rURL = elementFactory.createURIReference(SamsokProtocol.uri_rURL);
			SubjectNode s = null;
			for (Triple triple: graph.find(AnySubjectNode.ANY_SUBJECT_NODE, rdfType, samsokEntity)) {
				if (identifier != null) {
					throw new Exception("Ska bara finnas en entity");
				}
				s = triple.getSubject();
				identifier = s.toString();
				htmlURL = RDFUtil.extractSingleValue(graph, s, rURL, null);
			}
			if (identifier == null) {
				logger.error("Kunde inte extrahera identifierare ur rdf-grafen:\n" + xmlContent);
				throw new Exception("Kunde inte extrahera identifierare ur rdf-grafen");
			}
			info.setIdentifier(identifier);
			info.setNativeURL(htmlURL);
			if (gmlInfoHolder != null) {
				try {
					// s�tt identifier f�rst d� den anv�nds f�r deletes etc �ven om det g�r
					// fel nedan
					gmlInfoHolder.setIdentifier(identifier);
					URIReference rCoordinates = elementFactory.createURIReference(SamsokProtocol.uri_rCoordinates);
					URIReference rContext = elementFactory.createURIReference(SamsokProtocol.uri_rContext);
					// h�mta ev gml fr�n kontext-noder
					LinkedList<String> gmlGeometries = new LinkedList<String>();
					for (Triple triple: graph.find(s, rContext, AnyObjectNode.ANY_OBJECT_NODE)) {
						if (triple.getObject() instanceof SubjectNode) {
							SubjectNode cS = (SubjectNode) triple.getObject();
							String gml = RDFUtil.extractSingleValue(graph, cS, rCoordinates, null);
							if (gml != null && gml.length() > 0) {
								// vi konverterar till SWEREF 99 TM d� det �r v�rt standardformat
								// dessutom fungerar konverteringen som en kontroll av om gml:en �r ok
								gml = GMLUtil.convertTo(gml, GMLUtil.CRS_SWEREF99_TM_3006);
								gmlGeometries.add(gml);
							}
						}
					}
					gmlInfoHolder.setGmlGeometries(gmlGeometries);
					// itemTitle kan vara 0M s� om den saknas f�rs�ker vi ta itemName och
					// om den saknas, itemType
					URIReference rItemTitle = elementFactory.createURIReference(SamsokProtocol.uri_rItemTitle);
					String name = StringUtils.trimToNull(RDFUtil.extractValue(graph, s, rItemTitle, null, null));
					if (name == null) {
						URIReference rItemName = elementFactory.createURIReference(SamsokProtocol.uri_rItemName);
						name = StringUtils.trimToNull(RDFUtil.extractValue(graph, s, rItemName, null, null));
						if (name == null) {
							URIReference rItemType = elementFactory.createURIReference(SamsokProtocol.uri_rItemType);
							String typeUri = RDFUtil.extractSingleValue(graph, s, rItemType, null);
							if (typeUri != null) {
								SamsokProtocolHandler handler = getProtocolHandlerForVersion(graph, s);
								name = handler.lookupURIValue(typeUri);
								//name = uriValues.get(typeUri);
							}
						}
					}
					if (name == null) {
						name = "Ok�nt objekt";
					}
					gmlInfoHolder.setName(name);
				} catch (Exception e) {
					//logger.error("Fel vid gmlhantering f�r " + identifier, e);
					// rensa m�ngd med geometrier
					gmlInfoHolder.setGmlGeometries(null);
					addProblemMessage("Problem with GML for " + identifier + ": " + e.getMessage());
				}
			}
		} finally {
			if (graph != null) {
				graph.close();
			}
		}
		return info;
	}

	// hj�lpmetod som parsar ett xml-dokument och ger en dom tillbaka
	static Document parseDocument(String xmlContent) throws Exception {
        // records m�ste matcha schema
        //xmlFact.setSchema(schema);
        DocumentBuilder builder = xmlFact.newDocumentBuilder();
        StringReader sr = null;
        Document doc;
        try {
        	sr = new StringReader(xmlContent);
        	doc = builder.parse(new InputSource(sr));
        } finally {
        	if (sr != null) {
        		try {
        			sr.close();
        		} catch (Exception ignore) {
				}
        	}
        }
        return doc;
	}

	// hj�lpmetod som serialiserar ett dom-tr�d som xml utan xml-deklaration
	static String serializeDocumentAsFragment(Document doc) throws Exception {
		// TODO: anv�nd samma Transformer f�r en hel serie, kr�ver refaktorering
		//       av hur ContentHelpers anv�nds map deras livscykel
		final int initialSize = 4096;
		Source source = new DOMSource(doc);
		Transformer xformer = xformerFact.newTransformer();
		// ingen xml-deklaration d� vi vill anv�nda den som ett xml-fragment
		xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		StringWriter sw = new StringWriter(initialSize);
		Result result = new StreamResult(sw);
        xformer.transform(source, result);
        sw.close();
		return sw.toString();
	}

}
