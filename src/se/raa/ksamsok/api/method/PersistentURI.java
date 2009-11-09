package se.raa.ksamsok.api.method;

import java.io.Writer;

/**
 * h�mtar persistent URI fr�n given URL
 * @author Henrik Hjalmarsson
 */
public class PersistentURI implements APIMethod 
{
	/** namn p� metoden */
	public static String METHOD_NAME = "persistentURI";
	/** parameter f�r url */
	public static String URI_PARAMETER = "url";
	
	private Writer writer;
	private String url;
	
	/**
	 * skapar ett objekt av PersistentURI
	 * @param writer
	 * @param url
	 */
	public PersistentURI(Writer writer, String url)
	{
		this.writer = writer;
		this.url = url;
	}
	
	@Override
	public void performMethod() 
	{
	}
}