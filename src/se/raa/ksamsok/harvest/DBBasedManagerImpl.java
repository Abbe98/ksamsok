package se.raa.ksamsok.harvest;

import javax.sql.DataSource;

/**
 * Basklass f�r databasbaserad tj�nstehanterare.
 */
public class DBBasedManagerImpl {

	protected DataSource ds;
	
	protected DBBasedManagerImpl(DataSource ds) {
		this.ds = ds;
	}
}
