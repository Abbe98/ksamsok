package se.raa.ksamsok.harvest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Hj�lpmetoder och konstanter f�r databasoperationer.
 */
public class DBUtil {

	private static final Logger logger = Logger.getLogger(DBUtil.class);
	// st�dda databastyper (n�dv�ndigt d� det �r olika syntax f�r rownum/limit/offet etc)
	private static enum DBType  { DERBY, ORACLE, POSTGRES };
	// instans f�r att komma ih�g vilken databastyp det var
	private static volatile DBType dbType = null;

	/** Konstant f�r normalt v�rde f�r status p� poster */
	public static final int STATUS_NORMAL = 0;
	/** Konstant f�r att flagga att en post h�ller p� att behandlas */
	public static final int STATUS_PENDING = 1;

	/** Fetchsize att anv�nda p� statements om man inte anv�nder begr�nsande sql (limit/rownum etc)
	 * Se �ven {@linkplain #fetchFirst(Connection, String, int)} f�r annat alternativ.
	 * Oracle verkar ha ett default-v�rde (via row prefetching) p� 10, f�r diskusion om detta se
	 * http://download.oracle.com/docs/cd/B10501_01/java.920/a96654/oraperf.htm#1002425
	 */
	public static final int FETCH_SIZE = 100;

	/**
	 * Hj�lpmetod som st�nger databasresurser.
	 * 
	 * @param rs resultset eller null
	 * @param st statement eller null
	 * @param c connection eller null
	 */
	public static void closeDBResources(ResultSet rs, Statement st, Connection c) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception ignore) {}
		}
		if (st != null) {
			try {
				st.close();
			} catch (Exception ignore) {}
		}
		if (c != null) {
			try {
				c.close();
			} catch (Exception ignore) {}
		}
	}

	/**
	 * Hj�lpmetod som g�r commit om c != null och ej i autocommit-l�ge.
	 * 
	 * @param c connection
	 * @throws SQLException
	 */
	public static void commit(Connection c) throws SQLException {
		if (c != null && !c.getAutoCommit()) {
			c.commit();
		}
	}

	/**
	 * Hj�lpmetod som g�r rollback om c != null och ej i autocommit-l�ge.
	 * 
	 * @param c connection
	 */
	public static void rollback(Connection c) {
		try {
			if (c != null && !c.getAutoCommit()) {
				c.rollback();
			}
		} catch (SQLException e) {
			logger.error("Fel vid rollback", e);
		}
	}

	/**
	 * Ger sql f�r att h�mta de f�rsta fetchNum raderna av inskickad sql f�r den
	 * databastyp som uppkopplingen st�djer.
	 * @param c databasuppkoppling
	 * @param sql sql
	 * @param fetchNum antal rader att h�mta
	 * @return sql anpassad till aktuell databas
	 */
	public static String fetchFirst(Connection c, String sql, int fetchNum) {
		switch (determineDBType(c)) {
		case ORACLE:
			return "select * from (" + sql + ") where rownum <= " + fetchNum;
		case DERBY:
			return sql + " FETCH FIRST " + fetchNum + " ROWS ONLY";
		case POSTGRES:
			return sql + " LIMIT "+ fetchNum;
			default:
				logger.error("Unsupported database");
				throw new RuntimeException("unsupported database");
		}
	}

	/**
	 * Ger en str�ng som kan anv�ndas f�r att f� en geometrikolumn �versatt till gml (v2).
	 * @param c connection
	 * @param columnName geometrins kolumnnamn
	 * @return str�ng med funktionsanrop anpassat till aktuell databas
	 */
	public static String toGMLCall(Connection c, String columnName) {
		switch (determineDBType(c)) {
		case ORACLE:
			return "sdo_util.to_gmlgeometry(" + columnName + ")";
		case POSTGRES:
			// TODO: kan ha fler parametrar i anropet
			return "ST_AsGML(" + columnName + ")";
		case DERBY:
			logger.error("Derby does not handle gml/spatial data");
			throw new RuntimeException("Derby does not handle gml/spatial data");
		default:
			logger.error("Unsupported database");
			throw new RuntimeException("unsupported database");
		}
	}

	// avg�r och cachar upp databastyp f�r uppkopplingen, kastar runtime exception
	// om databastypen inte gick att avg�ra eller om den inte st�ds
	private static DBType determineDBType(Connection c) {
		if (dbType == null) {
			try {
				String dbName = c.getMetaData().getDatabaseProductName();
				if (dbName != null) {
					if (dbName.toLowerCase().contains("derby")) {
						dbType = DBType.DERBY;
					} else if (dbName.toLowerCase().contains("oracle")) {
						dbType = DBType.ORACLE;
					} else if (dbName.toLowerCase().contains("postgres")) {
						dbType = DBType.POSTGRES;
					}
				}
				if (dbType == null) {
					throw new Exception("could not determine database from product name: " +
							dbName);
				}
			} catch (Exception e) {
				logger.error("There was a problem determining database type", e);
				throw new RuntimeException("Unsupported database");
			} 
		}
		return dbType;
	}
}
