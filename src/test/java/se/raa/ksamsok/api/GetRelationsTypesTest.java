package se.raa.ksamsok.api;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GetRelationsTypesTest extends AbstractBaseTest {

	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "getRelationTypes");
		reqParams.put("relation","all");
	}
	
	@Test
	public void testGetRelationsTypesXMLResponse(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod getRelationTypes;
		try {
			getRelationTypes = apiMethodFactory.getAPIMethod(reqParams, out);
			getRelationTypes.setFormat(Format.XML);
			getRelationTypes.performMethod();
//			System.out.println(out.toString("UTF-8"));
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			Node relationTypes = assertBaseDocProp(resultDoc);
			assertTrue(relationTypes.getNodeName().equals("relationTypes"));
			int numberOfRelations = Integer.parseInt(relationTypes.getAttributes().getNamedItem("count").getTextContent());
			NodeList relationTypeList = relationTypes.getChildNodes();
			assertEquals(numberOfRelations, relationTypeList.getLength());
			for (int i = 0; i < numberOfRelations; i++){
				assertRelationType(relationTypeList.item(i));
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	private void assertRelationType(Node relationType) throws URISyntaxException{
		assertTrue(relationType.getNodeName().equals("relationType"));
		NamedNodeMap relAttrList = relationType.getAttributes();
		assertEquals(3, relAttrList.getLength());
		for(int i = 0; i < relAttrList.getLength(); i++){
			Node relAttr = relAttrList.item(i);
			if (relAttr.getNodeName().equals("name")){
				assertChild(relAttr.getFirstChild());
			} else if (relAttr.getNodeName().equals("title")){
				assertChild(relAttr.getFirstChild());
			} else if (relAttr.getNodeName().equals("reverse")){
				assertChild(relAttr.getFirstChild());
			} else {
				fail("Unknown attribute was found in relation tag: "+ relAttr.getNodeName());
			}
		}
		assertTrue(relationType.getFirstChild() == null);
	}

	@Test
	public void testGetRelationTypesJSONResponse(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod getRelationTypes;
		try {
			getRelationTypes = apiMethodFactory.getAPIMethod(reqParams, out);
			getRelationTypes.setFormat(Format.JSON_LD);
			getRelationTypes.performMethod();
			//System.out.println(out.toString("UTF-8"));
			JSONObject response = new JSONObject(out.toString("UTF-8"));
			assertBaseJSONProp(response);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetRelationTypesMissingReqParam(){
		reqParams.remove("relation");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod getRelationTypes;
		try {
			getRelationTypes = apiMethodFactory.getAPIMethod(reqParams, out);
			getRelationTypes.setFormat(Format.JSON_LD);
			getRelationTypes.performMethod();
			//System.out.println(out.toString("UTF-8"));
			JSONObject response = new JSONObject(out.toString("UTF-8"));
			assertBaseJSONProp(response);
			fail("No exception was thrown, expected MissingParameterException");
		} catch (MissingParameterException e) {
			// Correct exception was thrown
		} catch (Exception e) {
			fail("Wrong exception was thrown, expected MissingParameterException: "+e.getCause().toString());
		}
	}

	@Test
	public void testGetRelationTypesInvalidReqParam(){
		reqParams.remove("relation");
		reqParams.put("relation","allan");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod getRelationTypes;
		try {
			getRelationTypes = apiMethodFactory.getAPIMethod(reqParams, out);
			getRelationTypes.setFormat(Format.JSON_LD);
			getRelationTypes.performMethod();
			//System.out.println(out.toString("UTF-8"));
			JSONObject response = new JSONObject(out.toString("UTF-8"));
			assertBaseJSONProp(response);
			fail("No exception was thrown, expected BadParameterException");
		} catch (BadParameterException e) {
			// Correct exception was thrown
		} catch (Exception e) {
			fail("Wrong exception was thrown, expected BadParameterException: "+e.getCause().toString());
		}
	}

}

