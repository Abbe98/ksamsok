package se.raa.ksamsok.spatial;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.sql.Connection;

import oracle.jdbc.OracleConnection;
import oracle.spatial.geometry.JGeometry;
import oracle.spatial.util.GML;
import oracle.xml.parser.v2.DOMParser;

import org.w3c.dom.Node;

/**
 * Oracle-specifik variant av GMLDBWriter.
 */
public class OracleGMLDBWriter extends AbstractGMLDBWriter {

	private DOMParser parser;

	public OracleGMLDBWriter() {
		parser = new DOMParser();
	}

	@Override
	public void init(String serviceId, Connection c) {
		// m�ste vara en oracle-connection f�r att det ska funka med GML-klassen nedan
		// h�mta ut underliggande oracle-uppkopplingen med ett hack
		if (c instanceof OracleConnection == false) {
			try {
				c = c.getMetaData().getConnection();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (c instanceof OracleConnection == false) {
				c = find(c);
			}
		}
		super.init(serviceId, c);
	}

	private OracleConnection find(Connection c) {
		if (c != null) {
			if (c instanceof OracleConnection) {
	            return (OracleConnection) c;
			}
	        // try to find the Oracleconnection recursively
			for (Method method : c.getClass().getMethods()) {
				if (method.getReturnType().isAssignableFrom(java.sql.Connection.class) &&
						method.getParameterTypes().length == 0) {
		                    try {
		                    	return find((java.sql.Connection) (method.invoke(c, new Object[] {})));
		                    } catch (Exception e) {
		                            // Shouldn't ever happen.
		                    	e.printStackTrace();
		                    }
				}
			}
		}
		throw new RuntimeException("Fick ingen spatial-kompatibel oracleuppkoppling, " +
			"oracle spatial-jarfilerna m�ste kanske l�ggas i tomcat/lib och tas bort fr�n WEB-INF lib?");
	}

	@Override
	protected Object convertToNative(String gml) throws Exception {
		// hack d� oracles GML-klasser inte gillar GeometryCollection utan f�redrar MultiGeometry
		// GeometryCollection finns inte i gml 2+ utan bara i gml 1 men f�rekommer i ra�:s data fn
		gml = gml.replace("GeometryCollection", "MultiGeometry");
		parser.parse(new StringReader(gml));
		Node geomNode = parser.getDocument().getFirstChild();
		JGeometry jGeometry = GML.fromNodeToGeometry(geomNode);
		Object g = JGeometry.store(jGeometry, c);
		return g;
	}

}
