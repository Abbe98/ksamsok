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
	private static enum DBType  { DERBY, ORACLE };
	// instans f�r att komma ih�g vilken databastyp det var
	private static DBType dbType = null;

	/** Konstant f�r normalt v�rde f�r status p� poster */
	public static final int STATUS_NORMAL = 0;
	/** Konstant f�r att flagga att en post h�ller p� att behandlas */
	public static final int STATUS_PENDING = 1;

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
				if (dbName != null && dbName.toLowerCase().contains("derby")) {
					dbType = DBType.DERBY;
				} else if (dbName != null && dbName.toLowerCase().contains("oracle")) {
					dbType = DBType.ORACLE;
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
