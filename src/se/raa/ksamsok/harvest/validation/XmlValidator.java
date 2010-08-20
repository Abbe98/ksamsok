package se.raa.ksamsok.harvest.validation;

import java.io.IOException;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Validating a xml and checks if it matches ksams�ks standard harvest-format
 * 
 * @author Martin Duveborg
 *
 * To test you can use this xml:
 * http://mis.historiska.se/OAICat/SHM/art?verb=ListRecords&metadataPrefix=ksamsok-rdf
 * http://api.dimu.se/oaipmh/?verb=ListRecords&metadataPrefix=ksamsok-rdf
 * 
 * Jars added to make the validation work:
 * resolver.jar (new)
 * serializer.jar (new)
 * xercesImpl.jar (updated)
 * xml-apis.jar (new)
 * 
 */
public class XmlValidator {
	private static final String SCHEMA_LOCATION = "/" + XmlValidator.class.getPackage().getName().replace(".", "/") + "/oai.xsd";
	
	public List<Message> validate(String xml) 
	{
		ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false); 
			factory.setNamespaceAware(true);
	      
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			SAXParser parser = null;
			try {
				factory.setSchema(schemaFactory.newSchema(getClass().getResource(SCHEMA_LOCATION)));
				parser = factory.newSAXParser();
			}
			catch (SAXException se) {
				errorHandler.addMessage("Fel vid XSD parsing: ", se);
				return errorHandler.getReport();
			}
	      
			XMLReader reader = parser.getXMLReader();
			reader.setErrorHandler(errorHandler);
			reader.parse(new InputSource(xml));	
		 }    
		 catch (ParserConfigurationException pce) {
			 errorHandler.addMessage("ParserConfigurationException: ", pce);
		 } 
		 catch (IOException io) {
	    	 errorHandler.addMessage("IO-Problem: ", io);
		 }
		 catch (SAXException se){
	    	 errorHandler.addMessage("SAXException: ", se);
		 }
	    
		 if(errorHandler.getReport().isEmpty()){
			 errorHandler.addMessage("Inga fel uppstod!");
		 }
		 
		 return errorHandler.getReport();
	 } 
}
