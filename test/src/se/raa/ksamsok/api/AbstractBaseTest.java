package se.raa.ksamsok.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.HashMap;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import se.raa.ksamsok.solr.SearchServiceImpl;

import com.github.jsonldjava.jena.JenaJSONLD;

public class AbstractBaseTest {

	APIMethodFactory apiMethodFactory;
	HashMap<String, String> reqParams;
	
	public void setUp() throws MalformedURLException{
		SolrServer solr = new CommonsHttpSolrServer("http://lx-ra-ksamtest1:8080/solr");
		SearchServiceImpl searchService = new SearchServiceImpl();
		// The solr is @Autowired in the project. It is necessary to set up it by hand in the test cases
		ReflectionTestUtils.setField(searchService,"solr", solr);
		apiMethodFactory = new APIMethodFactory();
		// The searchService is @Autowired in the project. It is necessary to set up it by hand in the test cases
		ReflectionTestUtils.setField(apiMethodFactory,"searchService", searchService);
		JenaJSONLD.init();
		
	}

	/**
	 * This method assert the base properties of the xml document like verions, encoding and stylesheet
	 * @param doc - The document to assert
	 */
	protected void assertBaseDocProp(Document doc) {
		// Check encoding
		assertTrue(doc.getXmlEncoding().equalsIgnoreCase("UTF-8"));
		// Check version
		assertTrue(doc.getXmlVersion().equalsIgnoreCase("1.0"));
		// Check stylesheet
		if (reqParams.containsKey("stylesheet")){
			ProcessingInstruction styleElement = (ProcessingInstruction) doc.getDocumentElement().getPreviousSibling();
			assertNotNull(styleElement);
			assertTrue(styleElement.getData().contains(reqParams.get("stylesheet")));
		} else {
			assertNull(doc.getDocumentElement().getPreviousSibling());
		}
	}

	/**
	 * This method asserts a child node, i.e. a node without any childs, and returns the node's value
	 * @param node - The node to assert
	 * @return - A string with the node's value
	 */
	protected String assertChild(Node node) {
		assertTrue(node.getNodeType()==Node.TEXT_NODE);
		assertNull(node.getFirstChild());		
		return node.getNodeValue();
	}

	/**
	 * This method asserts a parent node, i.e. a node without value but with at least one child.
	 * @param node - The node to assert
	 * @param nodeName - The name it should have
	 */
	protected void assertParent(Node node, String nodeName) {
		assertTrue(node.getNodeName().equals(nodeName));
		assertEquals(0,node.getAttributes().getLength());
		assertNull(node.getNodeValue());
		assertTrue(node.getNodeType()==Element.ELEMENT_NODE);
		assertTrue(node.getChildNodes().getLength()>0);
	}

}