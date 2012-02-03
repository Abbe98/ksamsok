package se.raa.ksamsok.lucene;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jrdf.JRDFFactory;
import org.jrdf.SortedMemoryJRDFFactory;
import org.jrdf.collection.MemMapFactory;
import org.jrdf.graph.AnyObjectNode;
import org.jrdf.graph.AnySubjectNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.Literal;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;
import org.jrdf.parser.rdfxml.GraphRdfXmlParser;

public class RDFUtil {

	private static final Logger logger = Logger.getLogger(RDFUtil.class);

    // har en close() men den g�r inget s� vi skapar bara en instans
	private static final JRDFFactory jrdfFactory = SortedMemoryJRDFFactory.getFactory();

	public static Graph parseGraph(String rdfXml) throws Exception {
		Graph graph;
		StringReader r = null;
		try {
			r = new StringReader(rdfXml);
			graph = parseGraph(r);
		} finally {
			if (r != null) {
				r.close();
			}
		}
		return graph;
	}

	public static Graph parseGraph(Reader r) throws Exception {
		Graph graph = jrdfFactory.getNewGraph();
		GraphRdfXmlParser parser = new GraphRdfXmlParser(graph, new MemMapFactory());
		parser.parse(r, "");
		return graph;
	}

	// l�ser ut ett v�rde ur subjektnoden eller subjektnodens objektnod om denna �r en subjektnod
	// och l�gger till v�rdet mha indexprocessorn
	static String extractValue(Graph graph, SubjectNode s, URIReference ref, URIReference refRef, IndexProcessor ip) throws Exception {
		return extractValue(graph, s, ref, refRef, ip, null);
	}

	// l�ser ut ett v�rde ur subjektnoden eller subjektnodens objektnod om denna �r en subjektnod
	// och l�gger till v�rdet mha indexprocessorn och ev specialhanterar relationer
	static String extractValue(Graph graph, SubjectNode s, URIReference ref, URIReference refRef, IndexProcessor ip, List<String> relations) throws Exception {
		final String sep = " ";
		StringBuffer buf = new StringBuffer();
		String value = null;
		for (Triple t: graph.find(s, ref, AnyObjectNode.ANY_OBJECT_NODE)) {
			if (t.getObject() instanceof Literal) {
				Literal l = (Literal) t.getObject();
				if (buf.length() > 0) {
					buf.append(sep);
				}
				value = l.getValue().toString();
				buf.append(value);
				if (ip != null) {
					ip.addToDoc(value);
					// specialfall f�r att hantera relationer som nog egentligen felaktigt(?) �r vanliga v�rden, de borde vara rdf-resurser och d� URIReferences
					// jmfr <isPartOf>http...</isPartOf> och <isPartOf rdf:resource="http..." />
					if (relations != null) {
						ip.lookupAndHandleURIValue(value, relations, ref != null ? ref.getURI().toString() : null);
					}
				}
			} else if (t.getObject() instanceof URIReference) {
				value = getReferenceValue((URIReference) t.getObject(), ip, relations, ref);
				// l�gg till i buffer bara om detta �r en uri vi ska sl� upp v�rde f�r
				if (value != null && ip != null && ip.translateURI()) {
					if (buf.length() > 0) {
						buf.append(sep);
					}
					buf.append(value);
				}
				if (ip != null) {
					ip.addToDoc(value);
				}
			} else if (refRef != null && t.getObject() instanceof SubjectNode) {
				SubjectNode resSub = (SubjectNode) t.getObject();
				value = extractSingleValue(graph, resSub, refRef, ip);
				if (value != null) {
					if (buf.length() > 0) {
						buf.append(sep);
					}
					buf.append(value);
				}
			}
//			if (value != null && ip != null && relations != null) {
//				ip.handleRelation(ref, value, relations);
//			}
//			if (value != null && relations != null) {
//				// specialhantering av relationer
//				String relationType;
//				if (SamsokProtocol.uri_rSameAs.equals(ref.getURI())) {
//					relationType = "sameAs";
//				} else {
//					relationType = StringUtils.trimToNull(StringUtils.substringAfter(ref.getURI().toString(), uriPrefixKSamsok));
//					// TODO: fixa b�ttre
//					if (relationType == null) {
//						// testa cidoc
//						relationType = StringUtils.trimToNull(StringUtils.substringAfter(ref.getURI().toString(), uriPrefix_cidoc_crm));
//						if (relationType != null) {
//							// strippa sifferdelen
//							relationType = StringUtils.trimToNull(StringUtils.substringAfter(relationType, "."));
//						} else {
//							// bios
//							relationType = StringUtils.trimToNull(StringUtils.substringAfter(ref.getURI().toString(), uriPrefix_bio));
//						}
//					}
//					if (relationType == null) {
//						throw new Exception("Ok�nd relation? B�rjar ej med k�nt prefix: " + ref.getURI().toString());
//					}
//				}
//				relations.add(relationType + "|" + value);
//			}
			value = null;
		}
		return buf.length() > 0 ? StringUtils.trimToNull(buf.toString()) : null;
	}

	// l�ser ut ett enkelt v�rde ur subjektoden d�r objektnoden m�ste vara en literal eller en uri-referens
	// och l�gger till det mha indexprocessorn
	static String extractSingleValue(Graph graph, SubjectNode s, PredicateNode p, IndexProcessor ip) throws Exception {
		String value = null;
		for (Triple t: graph.find(s, p, AnyObjectNode.ANY_OBJECT_NODE)) {
			if (value != null) {
				throw new Exception("Fler v�rden �n ett f�r s: " + s + " p: " + p);
			}
			if (t.getObject() instanceof Literal) {
				value = StringUtils.trimToNull(((Literal) t.getObject()).getValue().toString());
			} else if (t.getObject() instanceof URIReference) {
				value = getReferenceValue((URIReference) t.getObject(), ip, null, null); // TODO: handle relations?
			} else {
				throw new Exception("M�ste vara literal/urireference o.class: " + t.getObject().getClass().getSimpleName() + " f�r s: " + s + " p: " + p);
			}
			if (ip != null) {
				ip.addToDoc(value);
			}
		}
		return value;
	}

	// f�rs�ker �vers�tta ett uri-v�rde till ett f�rinl�st v�rde
	private static String getReferenceValue(URIReference object, IndexProcessor ip, List<String> relations, URIReference ref) throws Exception {
		String value = null;
		String uri = object.getURI().toString();
		String refUri = ref != null ? ref.getURI().toString() : null;
		// se om vi ska f�rs�ka ers�tta uri:n med en uppslagen text
		// och l�gga in den i relations
		if (ip != null) { // && ip.translateURI()) {
			value = ip.lookupAndHandleURIValue(uri, relations, refUri);
		} else {
			value = StringUtils.trimToNull(uri);
		}
		return value;
/*
private String getReferenceValue(URIReference ref, IndexProcessor ip) {
String value = null;
		String uri = ref.getURI().toString();
		String lookedUpValue;
		// se om vi ska f�rs�ka ers�tta uri:n med en uppslagen text
		if (ip != null && ip.translateURI() && (lookedUpValue = lookupURIValue(uri)) != null) {
			value = lookedUpValue;
		} else {
			value = StringUtils.trimToNull(uri);
		}
		return value;
 */
	}

	// l�ser in en rdf-resurs och lagrar uri-v�rden och �vers�ttningsv�rden f�r uppslagning
	// alla resurser f�ruts�tts vara kodade i utf-8 och att v�rdena �r Literals
	static void readURIValueResource(String fileName, URI predicateURI, Map<String, String> uriValues) {
		Reader r = null;
		Graph graph = null;
		try {
			r = new InputStreamReader(RDFUtil.class.getResourceAsStream(fileName), "UTF-8");
			graph = parseGraph(r);
			GraphElementFactory elementFactory = graph.getElementFactory();
			URIReference rName = elementFactory.createURIReference(predicateURI);
			int c = 0;
			for (Triple t: graph.find(AnySubjectNode.ANY_SUBJECT_NODE, rName, AnyObjectNode.ANY_OBJECT_NODE)) {
				uriValues.put(t.getSubject().toString(), StringUtils.trimToNull(((Literal) t.getObject()).getValue().toString()));
				++c;
			}
			if (logger.isInfoEnabled()) {
				logger.info("L�ste in " + c + " uris/v�rden fr�n " + fileName);
			}
		} catch (Exception e) {
			throw new RuntimeException("Problem att l�sa in uri-�vers�ttningsfil " +
					fileName, e);
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (Exception ignore) {
				}
			}
			if (graph != null) {
				graph.close();
			}
		}
	}

}
