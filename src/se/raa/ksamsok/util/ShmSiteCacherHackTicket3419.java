package se.raa.ksamsok.util;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.HarvestService;

/**
 * Hack f�r att gringg� problem med stora rdf:er som har m�nga relationer, specifikt
 * shm/site/56171 som har �ver 200k st.
 * Kopplad till #3419 och hela denna klass kan/ska tas bort n�r den ticketen �r �verspelad.
 * Cachen rensas n�r sk�rdedatum f�r tj�nsten f�r shm site uppdateras, alternativt
 * kan det g�ras manuellt genom att anropa resolverservlet med parametern clear_cache=true,
 * exvis http://kulturarvsdata.se/ksamsok/?clear_cache=true, eller tex
 * http://kulturarvsdata.se/ksamsok/raa/fmi/123?clear_cache=true
 * Kringla identifierar sig fn f�r detta med parametern kringla=true vid s�kningar etc s� f�r
 * att simulera ett kringla-anrop och se det kringla ser l�gg p� den parametern, exvis
 * http://kulturarvsdata.se/ksamsok/shm/site/56171?kringla=true
 *
 */
public class ShmSiteCacherHackTicket3419 {

	private static final Logger logger = Logger.getLogger(ShmSiteCacherHackTicket3419.class);
	private static final Map<String,String> cache = new HashMap<String, String>();

	public static final String KRINGLA = "kringla";
	public static final String CLEAR_CACHE = "clear_cache";

	/**
	 * Om cachen ska anv�ndas. Kringla-parametern m�ste vara sann och uri:n m�ste
	 * matcha objekt man vill cacha.
	 * @param kringlaParam sant om det �r kringla som fr�gar
	 * @param uri uri
	 * @return sant om cachen ska anv�ndas
	 */
	public static boolean useCache(String kringlaParam, String uri) {
		return "true".equals(kringlaParam) && uri != null &&
				uri.endsWith("shm/site/56171");
	}

	/**
	 * Rensar cache f�r om tj�nsten shm/site skickas in. Sk�rde-URL:en anv�nds f�r att
	 * avg�ra om det �r r�tt tj�nst.
	 * @param service tj�nst
	 */
	public static void clearCache(HarvestService service) {
		if (service != null && service.getHarvestURL() != null &&
				service.getHarvestURL().trim().toLowerCase().endsWith("shm/site")) {
			clearCache();
		}
	}

	/**
	 * Rensar cache om argumentet �r "true", anv�nds f�r att manuellt kunna trigga
	 * en cacherensning.
	 * @param clearCache
	 */
	public static void clearCache(String clearCache) {
		if ("true".equals(clearCache)) {
			clearCache();
		}
	}

	// rensar ovillkorligen cachen
	private static void clearCache() {
		logger.warn("Clearing shm site cache");
		synchronized (cache) {
			cache.clear();
		}
	}

	/**
	 * H�mtar data fr�n cachen, eller strippar (utvalda) relationer fr�n
	 * rdf och pres och cachar sen resultatet.
	 * @param uri uri
	 * @param xmlContent xml-data
	 * @return strippad rdf
	 * @throws UnsupportedEncodingException
	 */
	public static String getOrRecache(String uri, byte[] xmlContent) throws UnsupportedEncodingException {
		synchronized (cache) {
			String content = cache.get(uri);
			if (content != null) {
				if (logger.isInfoEnabled()) {
					logger.info("Returning cached content for " + uri);
				}
				return content;
			}
			content = new String(xmlContent, "UTF-8");
			logger.warn("Recaching and stripping stuff for " + uri + ", size=" + content.length());
			content = content.replaceAll("<pres:reference>.*</pres:reference>", "");
			logger.warn("after pres:reference removal, size=" + content.length());
			content = content.replaceAll("<hasPart>.*</hasPart>", "");
			logger.warn("after hasPart removal, size=" + content.length());
			content = content.replaceAll("<hasFind>.*</hasFind>", "");
			logger.warn("after hasFind removal, size=" + content.length());
			content = content.replaceAll("<isPartOf>.*</isPartOf>", "");
			logger.warn("after isPartOf removal, size=" + content.length());
			content = content.replaceAll("<isDescribedBy>.*</isDescribedBy>", "");
			logger.warn("after isDescribedBy removal, size=" + content.length());

			cache.put(uri,  content);
			return content;
		}
	}

}
