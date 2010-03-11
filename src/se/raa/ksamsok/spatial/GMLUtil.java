package se.raa.ksamsok.spatial;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;

import javax.vecmath.Point2d;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml.producer.GeometryTransformer;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.geotools.xml.transform.Translator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Klass med metoder f�r att jobba med GML, koordinater och koordinatsystem.
 * TODO: se �ver allt som har med GML-versioner och koordinatsystem/ordning att g�ra - nu
 *       k�nns det som om det �r en salig blandning
 */
public class GMLUtil {

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.spatial.GMLUtil");

	// klassnamn f�r databasspecifik GML-hanterare
	private static String gmlDBWriterClassName;
	// flagga som anger om man har f�rs�kt s�tta klassnamnsvariabeln
	private static boolean gmlDBWriterClassNameSet = false;

	static {
		// OBS! denna �r v�ldigt viktig - det blir fel annars - i alla fall om srsName �r p� formen EPSG:4326
		// TODO: kolla upp vad som h�nder om man f�r in en uri p� formen
		//       http://www.opengis.net/gml/srs/epsg.xml#4326 (lon f�rst?)
		//       eller p� formen urn:x-ogc:def:crs:EPSG:4326 (lat f�rst(default)?)
		//       som geotools kan producera
		System.setProperty("org.geotools.referencing.forceXY", "true");
	}

	/** EPSG:4326, WGS 84 */
	public static final String CRS_WGS84_4326 = "EPSG:4326";
	/** EPSG:3006, SWEREF 99 TM */
	public static final String CRS_SWEREF99_TM_3006 = "EPSG:3006";
	/** EPSG:3021, RT90 2.5 gon V */
	public static final String CRS_RT90_3021 = "EPSG:3021";

	// vi anv�nder en enda parser och synkroniserar parsning pga f�ljande bug i geotools 2.5.5
	// http://jira.codehaus.org/browse/GEOT-2615
	// TODO: n�r den �r fixat och geotools uppgraderas kan man ta bort synkroniseringen
	private static final Configuration configuration = new GMLConfiguration();
	private static final Parser parser = new Parser(configuration);
	private static final GeometryFactory geometryFactory = new GeometryFactory();

	/**
	 * Konverterar gml-geometri till annat koordinatsystem.
	 * @param gml gml
	 * @param crsName identifierare f�r koordinatsystem
	 * @return konverterad gml-str�ng
	 * @throws Exception vid fel
	 */
	public static String convertTo(String gml, String crsName) throws Exception {
		Geometry g = parseGeometry(gml);
		CoordinateReferenceSystem parsedCRS = getCRS(g);
		if (parsedCRS.getIdentifiers().iterator().next().toString().equalsIgnoreCase(crsName)) {
			return gml;
		}
		CoordinateReferenceSystem toCRS = CRS.decode(crsName);
		g = transformCRS(g, parsedCRS, toCRS, false);
		gml = toXML(g, toCRS);
		return gml;
	}

	/**
	 * H�mtar ut centrumpunkt f�r gml-geometrin som lon/lat (x/y).
	 * @param gml gml-geometri
	 * @return centrumpunkt i WGS 84 (EPSG:4326)
	 * @throws Exception vid fel
	 */
	public static Point2d getLonLatCentroid(String gml) throws Exception {
		Geometry g = parseGeometry(gml);
		// ta ut centrumpunkt
		Point centroid = g.getCentroid();
		CoordinateReferenceSystem parsedCRS = getCRS(g);
		CoordinateReferenceSystem wsg84 = CRS.decode(CRS_WGS84_4326); // true); // lonfirst?
		boolean nameEquals = parsedCRS.getName().equals(wsg84.getName());
		boolean equals = parsedCRS.equals(wsg84);
		if (nameEquals) {
			// TODO: se �ver
			if (!equals) {
				// samma namn, men ej samma referenssystem - beror s� gott som alltid p�
				// att lat/lon tros vara omkastade mot vad vi vill, men vi "kr�ver" att
				// det ska vara p� xy s� vi antar att det �r det
				logger.warn("getLonLatCentroid: gml �r (WGS84) men koordinatsystemen �r !equals");
			}
		} else {
			// TODO: kanske �r samma problem med andra koordinatreferenssystem med omkastade
			//       koordinater och inte bara wsg 84?
			centroid = (Point) transformCRS(centroid, wsg84, parsedCRS, false);
		}
		return new Point2d(centroid.getX(), centroid.getY());
	}

	/**
	 * Transformerar array med koordinater fr�n ett namngivet koordinatsystem till ett annat.
	 * Ordningen f�ruts�tts vara x1, y1, x2, y2 etc och antalet inskickade koordinatv�rden
	 * m�ste vara j�mnt.
	 * @param coords koordinater
	 * @param fromCRS koordinaternas nuvarande koordinatsystem
	 * @param toCRS koordinatsystem att konvertera till
	 * @return konverterade kooordinater
	 * @throws Exception vid fel
	 */
	public static double[] transformCRS(double[] coords, String fromCRS, String toCRS) throws Exception {
		if (fromCRS.equals(toCRS)) {
			return coords;
		}
		if (coords.length % 2 != 0) {
			throw new Exception("Felaktig koordinatlista - oj�mnt antal koordinater");
		}
		double result[] = new double[coords.length];
		CoordinateReferenceSystem sourceCRS = CRS.decode(fromCRS);
		CoordinateReferenceSystem targetCRS = CRS.decode(toCRS);
		for (int i = 0; i < coords.length; i+=2) {
			Point p = geometryFactory.createPoint(new Coordinate(coords[i], coords[i + 1]));
			p = (Point) transformCRS(p, sourceCRS, targetCRS, false);
			result[i] = p.getX();
			result[i + 1] = p.getY();
		}
		return result;
	}

	/**
	 * H�mtar (ev) en ny instans av en databas-specifik klass f�r att hantera geometrier.
	 * Databasuppkopplingen anv�nds f�r att f�rs�ka h�rleda fram en klass.
	 * F�r n�rvarande st�ds bara Oracle, se {@linkplain OracleGMLDBWriter},
	 * och f�r �vriga kommer anropet ge null.<br/>
	 * Beteendet ovan kan �sidos�ttas genom att explicit s�tta ett klassnamn
	 * via -Dsamsok.spatial.class=x.y.Z (klassen m�ste implementera GMLDBWriter och ha
	 * en publik default-konstruktor).<br/>
	 * Om man ej vill anv�nda det spatiala st�det alls kan man st�nga av det genom att
	 * s�tta flaggan -Dsamsok.spatial=false.
	 * 
	 * @param serviceId tj�nst
	 * @param c databasuppkoppling
	 * @return en f�r aktuell databas (eller konf) l�mplig hanterare av geometrier, eller null 
	 */
	public static GMLDBWriter getGMLDBWriter(String serviceId, Connection c) {
		GMLDBWriter gmlDbWriter = null;
		// kolla om vi ska skriva i spatialtabeller, default �r sant
		if (Boolean.parseBoolean(System.getProperty("samsok.spatial", "true"))) {
			// h�mta klassnamnet och instantiera och initera en writer
			String className = getGMLDBWriterClassName(c);
			if (className != null) {
				try {
					gmlDbWriter = (GMLDBWriter) Class.forName(className).newInstance();
				} catch (Throwable t) {
					logger.error("Misslyckades att skapa ny instans av GMLDBWriter (" +
							className + ")", t);
				}
				if (gmlDbWriter != null) {
					gmlDbWriter.init(serviceId, c);
				}
			}
		}
		return gmlDbWriter;
	}

	// h�mtar cachat klassnamn, eller f�rs�ker ta reda p� ett bra klassnamn och cacha upp det
	private static String getGMLDBWriterClassName(Connection c) {
		// hit ska vi bara komma om vi ska spara data i spatialtabeller
		// TODO: synkronisera kanske?
		if (!gmlDBWriterClassNameSet) {
			gmlDBWriterClassName = System.getProperty("samsok.spatial.class");
			// om det inte �r satt som en systemproperty f�rs�k lista ut fr�n uppkopplingen
			if (gmlDBWriterClassName == null) {
				String extractedClassName = c.getClass().getName();
				// om det �r en dbcp delegate, testa att ta fram den aktuella klassen
				if (extractedClassName.indexOf("dbcp") > 0) {
					try {
						Class<?> classToAnalyze = c.getClass();
						while (classToAnalyze != null && !Modifier.isPublic(classToAnalyze.getModifiers())) {
							classToAnalyze = classToAnalyze.getSuperclass();
						}
						if (classToAnalyze != null) {
							Method getInnermostDelegate = classToAnalyze.getMethod("getInnermostDelegate", (Class[]) null);
							Object id = getInnermostDelegate.invoke(c, (Object[]) null);
							if (id == null) {
								// fallback genom ett litet trick om metodanrop ger null
								// TODO: detta �r egentligen allt som beh�vs f�r att f� ett
								//       bra klassnamn att kolla
								id = c.getMetaData().getConnection();
							}
							extractedClassName = id.getClass().getName();
						}
					} catch (Exception e) {
						logger.error("Fel vid kontroll av innermost delegate f�r en dbcp connection", e);
					}
				}
				if (extractedClassName.toLowerCase().indexOf("oracle") >= 0) {
					gmlDBWriterClassName = "se.raa.ksamsok.spatial.OracleGMLDBWriter";
				} else {
					logger.info("Ingen spatial-kapabel (och k�nd) databas anv�nds(?), " +
							"connection-klass tolkades som " + extractedClassName);
				}
			}
			gmlDBWriterClassNameSet = true;
		}
		return gmlDBWriterClassName;
	}

	// transformera geometri fr�n ett koordinatsystem till ett annat
	private static Geometry transformCRS(Geometry geometry, CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS, boolean lenient) throws Exception {
		//if (logger.isDebugEnabled()) {
		//	logger.debug("xform (lenient: " + lenient + ") from " + sourceCRS.getName() + " (" +
		//		CRS.getGeographicBoundingBox(sourceCRS) + ") --> " +
		//		targetCRS.getName() + " (" + CRS.getGeographicBoundingBox(targetCRS) + ")");
		//}
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, lenient);
		Geometry xformed = JTS.transform(geometry, transform);
		// s�tt om crs
		xformed.setUserData(targetCRS);
		return xformed;
	}

	// f�rs�ker skapa en geometri-instans fr�n gml
	static Geometry parseGeometry(String gml) throws Exception {
		Object o;
		if (gml == null) {
			throw new Exception("gml �r null");
		}
		// fulfix f�r oracle-genererade srsName med SDO som authority vilket geotools inte
		// k�nner till n�t om
		gml = gml.replace("SDO:", "EPSG:");
		try {
			// vi m�ste synkronisera parsningen pga en bug i geotools (eller eclipse-emf/xsd)
			// se http://jira.codehaus.org/browse/GEOT-2615
			// obs att det inte spelar n�n roll om man anv�nder nya parserinstanser istf en
			// enda utan problemet uppst�r i alla fall
			// ett alternativ skulle kunna vara att anv�nda "xdo"-parser ist�llet d� den
			// inte ber�rs av buggen och dessutom �r snabbare  - den g�r dock lite annorlunda
			// med koordinatsystemen och l�gger till skillnad fr�n gt-xml bara namnet p�
			// utparsat srsName i userData ist�llet f�r sj�lva CoordinateReferenceSystem-
			// instansen vilket man i s� fall m�ste ta h�nsyn till
			// typ:
			// XMLReader reader = XMLReaderFactory.createXMLReader();
			// XMLSAXHandler xmlHandler = new XMLSAXHandler(new HashMap());
			// reader.setContentHandler(xmlHandler);
			// reader.parse(new InputSource(new StringReader(gml)));
			// Object o = xmlHandler.getDocument(); // b�r ge en Geometry-instans
			synchronized (parser) {
				o = parser.parse(new StringReader(gml));
			}
		} catch (Throwable t) {
			throw new Exception("Fel vid parsning av gml: " + t.getMessage(), t);
		}
		if (o == null) {
			throw new Exception("Kunde inte tolka inskickad xml som gml");
		}
		if (!(o instanceof Geometry)) {
			throw new Exception("XML verkar vara gml, men kunde inte f� fram en geometri");
		}
		return (Geometry) o;
	}

	// h�mtar ut koordinatsystem fr�n geometri
	// TODO: �r detta samma som GML2EncodingUtils.getCRS()? om ja, anv�nd den ist�llet?
	private static CoordinateReferenceSystem getCRS(Geometry g) throws Exception {
		// i userdata l�gger geotools-parsern fn info om vilket koordinatsystem gml:en var i
		Object o = g.getUserData();
		if (o == null || "".equals(o)) {
			// TODO: f�ruts�tta att det �r n�t speciellt crs och ge tillbaka en s�n instans?
			throw new Exception("GML/XML-parsern fr�n Geotools la ingen koordinatsystem-info " +
					"i userdata, saknas den i gml:en?");
		}
		if (!(o instanceof CoordinateReferenceSystem || o instanceof String)) {
			throw new Exception("GML/XML-parsern har lagt n�t annat �n koordinatsystem-info " +
					"i userdata, en instans av " + o.getClass().getName());
		}
		if (o instanceof String) {
			CoordinateReferenceSystem crs = CRS.decode((String) o);
			if (crs == null) {
				throw new Exception("Fick ingen koordinatsystem-info vid avkodning av uttolkat " +
						"srsName: " + o);
			}
			o = crs;
		}

		return (CoordinateReferenceSystem) o;
	}

	// ger geometrin som ett gml-fragment med angivet koordinatreferenssystem som srsName.
	private static String toXML(Geometry g, CoordinateReferenceSystem crs) throws Exception {
		if (crs == null) {
			crs = getCRS(g);
		}
		// skulle kunna anv�nda xdo ist�llet men det �r sv�rt att styra saker, typ:
		// GMLConfiguration conf = new GMLConfiguration(); // gml2 eller 3
		// org.geotools.xml.Encoder e = new org.geotools.xml.Encoder(conf);
		// e.setOmitXMLDeclaration(true);
		// e.setIndenting(true);
		// QName qname = new QName(GMLSchema.NAMESPACE.toString(), gType, "gml");
		// // g m�ste ha en crs-instans i userdata f�r att f� med srsName i gml:en, namnet r�cker ej
		// e.encode(g, qname, System.out);

		// TODO: detta �r lite hackigt d� det var sv�rt att f� ut ett ok gml-fragment
		//       s� som vi vill ha det med angivet srsName och inga extra namespaces
		//       fr�n geotools
		final String toSrsName = crs.getIdentifiers().iterator().next().toString();
		GeometryTransformer gt = new GeometryTransformer() {
			@Override
			public Translator createTranslator(
					ContentHandler handler) {
				// TODO: 8 decimaler ok?
				GeometryTranslator gt = new GeometryTranslator(handler, 8) {

					@Override
					public void encode(Geometry geometry,
							String srsName) {
						if (srsName == null) {
							srsName = toSrsName;
						}
						super.encode(geometry, srsName);
					}
					@Override
					public String getDefaultNamespace() {
						// TODO: flytta till constructor etc
						// bort med extra namespaces
						coordWriter.setNamespaceUri(null);
						return null;
					}
				};
				return gt;
			}
		};
		gt.setIndentation(4); // TODO: detta kan skippas men �r trevligt vid debug
		gt.setOmitXMLDeclaration(true);
		gt.setNamespaceDeclarationEnabled(true);
		return gt.transform(g);
	}

}
