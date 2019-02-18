package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.w3c.dom.Document;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class Protocol_0_TO_1_0_Test {

	private static DocumentBuilderFactory xmlFact;
	private static TransformerFactory xformerFact;
	static {
		xmlFact = DocumentBuilderFactory.newInstance();
	    xmlFact.setNamespaceAware(true);
	    xformerFact = TransformerFactory.newInstance();
	}


	@Test
	public void testURILookup() throws Exception {
		// "null"-graf
		Model m = RDFUtil.parseModel("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"></rdf:RDF>");

		SamsokProtocolHandler handler = new SamsokProtocolHandler_0_TO_1_0(m, null);
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/DataQuality#raw");
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/ContextType#exist");
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/EntityType#photo");

		String agent = lookup(handler, "http://kulturarvsdata.se/resurser/EntityType#agent");
		assertNull("Agent ska inte vara med i <= 1.0", agent);
	}

	@Test
	public void testParse1() throws Exception {
		String rdf = loadTestFileAsString("hjalm_0.99.rdf");
		Model model = RDFUtil.parseModel(rdf);
		assertNotNull("Ingen graf, fel på rdf:en?", model);
		// grund
		Property rdfType = ResourceFactory.createProperty(SamsokProtocol.uri_rdfType.toString());
		Resource samsokEntity = ResourceFactory.createResource(SamsokProtocol.uri_samsokEntity.toString());

		//SubjectNode s = null;
		SimpleSelector selector = new SimpleSelector(null, rdfType, samsokEntity);
		StmtIterator iter = model.listStatements(selector);
		Resource s = null;
		while (iter.hasNext()){
			if (s != null){
				throw new Exception("Ska bara finnas en entity i rdf-grafen");
			}
			s = iter.next().getSubject();
		}
		SamsokProtocolHandler handler = new SamsokProtocolHandler_0_TO_1_0(model, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<String>();
		List<String> gmlGeometries = new LinkedList<String>();
		SolrInputDocument doc = handler.handle(service, new Date(), relations, gmlGeometries);
		assertNotNull("Inget doc tillbaka", doc);
		assertEquals("Fel antal relationer tillbaka", 1, relations.size());
		// kontrollera specialformatet
		assertEquals("Felaktig relation", "isRelatedTo|http://kulturarvsdata.se/raa/test/2", relations.getFirst());
		// kontrollera uppslagning
		assertEquals("Felaktigt uppslaget ämne", "Kulturhistoria", doc.getFieldValue(ContentHelper.IX_SUBJECT));
		// kontrollera exists-index
		assertEquals("Felaktigt värde för geodataExists", "n", doc.getFieldValue(ContentHelper.IX_GEODATAEXISTS));
		assertEquals("Felaktigt värde för thumbnailExists", "j", doc.getFieldValue(ContentHelper.IX_THUMBNAILEXISTS));
		assertEquals("Felaktigt värde för timeInfoExists", "n", doc.getFieldValue(ContentHelper.IX_TIMEINFOEXISTS));
		// namn + namn i kontexttypspecifikt index
		assertTrue("Skaparnamn ej extraherat", doc.getFieldValues(ContentHelper.IX_NAME).contains("Kunz Lochner"));
		assertTrue("Skaparnamn (kontext-index) ej extraherat", doc.getFieldValues("create_" + ContentHelper.IX_NAME).contains("Kunz Lochner"));
	}

	private void lookupNotNull(SamsokProtocolHandler handler, String uri) {
		String value = lookup(handler, uri);
		assertNotNull("Hittade inte värde för " + uri, value);
	}

	private String lookup(SamsokProtocolHandler handler, String uri) {
		return handler.lookupURIValue(uri);
	}

	private String loadTestFileAsString(String fileName) throws Exception {
		DocumentBuilder builder = xmlFact.newDocumentBuilder();
		InputStream is = null;
		StringWriter sw = null;
		try {
			//is = getClass().getResourceAsStream(fileName);
			//Document doc = builder.parse(is);
			Document doc = builder.parse(new File("src/test/resources/" + fileName));
			final int initialSize = 4096;
			Source source = new DOMSource(doc);
			Transformer xformer = xformerFact.newTransformer();
			xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			sw = new StringWriter(initialSize);
			Result result = new StreamResult(sw);
	        xformer.transform(source, result);
		} finally {
			if (sw != null) {
				sw.close();
			}
			if (is != null) {
				is.close();
			}
		}
		return sw != null ? sw.toString() : null;
	}

}
