package se.raa.ksamsok.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import se.raa.ksamsok.api.exception.APIException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Hanterar f�rfr�gningar till K-sams�ks API
 * @author Henrik Hjalmarsson
 */
public class APIServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	//klass specifik logger
	private static final Logger logger = 
		Logger.getLogger("se.raa.ksamsok.api.APIServlet");

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
		//s�tter contentType och character encoding
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/xml; charset=UTF-8");
		Map<String,String> reqParams = null;
		APIMethod method = null;
		PrintWriter writer = resp.getWriter();
		
		
		try 
		{
			//skriver ut XML header
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			//h�mtar parametrar i UTF-8 format
			reqParams = ContentHelper.extractUTF8Params(req.getQueryString());
			
			//skriver ut stylesheet header om stylesheet finns
			String stylesheet = reqParams.get("stylesheet");
			if (stylesheet != null && stylesheet.trim().length() > 0) 
			{
				writer.println("<?xml-stylesheet type=\"text/xsl\" href=\""
						+ stylesheet.replace("\"", "&quot;") + "\"?>");
			}
			
			//skriver ut root tag och versions nummer
			writer.println("<result>");
			writer.println("<version>" + APIMethod.API_VERSION + "</version>");
			
			//h�mtar API metod
			method = APIMethodFactory.getAPIMethod(reqParams, writer);
			
			//utf�r API metod
			method.performMethod();
		} catch (APIException e) 
		{
			Diagnostic(writer , e);
		} catch (Exception e)
		{
			logger.error(e.getMessage());
		}finally
		{
			//avslutar root tag �ven om exception f�ngas
			writer.println("</result>");
		}
	}

	/*
	 * skriver ut error tag om fel uppst�r
	 */
	private void Diagnostic(PrintWriter writer, APIException e)
	{
		logger.error(e.getClassName() + "\n" + e.getDetails());
		writer.println("<error>");
		writer.println(e.getMessage());
		writer.println("</error>");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{//TODO t�nkte ifall man vill g�ra metoder som kr�ver autensiering s� kan man 
		//skicka dessa som en post ist�llet f�r att d�lja eventuella anv�ndar
		//uppgifter
		doGet(req, resp);
	}
}