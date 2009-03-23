package se.raa.ksamsok.harvest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * Basklass f�r databasbaserad tj�nstehanterare.
 */
public class DBBasedManagerImpl {

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.DBBasedManagerImpl");

	protected DataSource ds;
	
	protected DBBasedManagerImpl(DataSource ds) {
		this.ds = ds;
	}

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
}
