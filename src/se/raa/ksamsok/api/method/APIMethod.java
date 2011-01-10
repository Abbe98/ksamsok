package se.raa.ksamsok.api.method;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;

/**
 * Interface f�r API metoder
 * @author Henrik Hjalmarsson
 */
public interface APIMethod 
{
	/** versionen f�r detta API */
	public static final String API_VERSION = "1.0";
	/** namnet p� metod parametern */
	public static final String METHOD = "method";
	/** API nyckel parameter namn */
	public static final String API_KEY_PARAM_NAME = "x-api";
	/** delare f�r att dela query str�ngar */
	public static final String DELIMITER = "|";
	
	/**
	 * utf�r API metod
	 * @throws MissingParameterException om obligatorisk parameter saknas
	 * @throws BadParameterException om parameter �r felformaterad
	 * @throws DiagnosticException vid ov�ntat fel
	 */
	public void performMethod()
		throws MissingParameterException, BadParameterException,
			DiagnosticException;

	/**
	 * Ger om huvud har skrivits.
	 * @return sant om huvud har skrivits
	 */
	public boolean isHeadWritten();

	/**
	 * Ger om fot har skrivits.
	 * @return sant om fot har skrivits
	 */
	public boolean isFootWritten();
}