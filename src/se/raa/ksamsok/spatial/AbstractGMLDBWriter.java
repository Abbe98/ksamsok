package se.raa.ksamsok.spatial;

import java.sql.Connection;
import java.sql.PreparedStatement;

import se.raa.ksamsok.harvest.DBBasedManagerImpl;

/**
 * Basklass som kan anv�ndas f�r att implementera databasspecifika instanser av
 * GMLDBWriter.
 */
public abstract class AbstractGMLDBWriter implements GMLDBWriter {

	protected String serviceId;
	protected Connection c;

	protected AbstractGMLDBWriter() {
	}

	@Override
	public void init(String serviceId, Connection c) {
		this.serviceId = serviceId;
		this.c = c;
	}

	@Override
	public void insert(GMLInfoHolder gmlInfoHolder) throws Exception {
		if (gmlInfoHolder.hasGeometries()) {
			PreparedStatement pst = null; 
			try {
				pst = c.prepareStatement("insert into geometries " +
				"(uri, serviceId, name, geometry) values (?, ? , ?, ?)");
				pst.setString(1, gmlInfoHolder.getIdentifier());
				pst.setString(2, serviceId);
				pst.setString(3, gmlInfoHolder.getName());
				for (String gml: gmlInfoHolder.getGmlGeometries()) {
					Object g = convertToNative(gml);
					pst.setObject(4, g);
					pst.executeUpdate();
				}
			} catch (Exception t) {
				t.printStackTrace();
				throw t;
			} finally {
				DBBasedManagerImpl.closeDBResources(null, pst, null);
			}
		}
	}

	@Override
	public void update(GMLInfoHolder gmlInfoHolder) throws Exception {
		// ta bort alla och sen stoppa in nya - det kan vara fler/f�rre �n innan s�
		// det �r enklare att rensa och stoppa in p� nytt �n att f�rs�ka uppdatera
		// befintliga tupler
		delete(gmlInfoHolder.getIdentifier());
		insert(gmlInfoHolder);
	}

	@Override
	public void delete(String identifier) throws Exception {
		if (identifier == null) {
			return;
		}
		PreparedStatement pst = null; 
		try {
			// TODO: l�gga p� serviceId som villkor ocks�?
			pst = c.prepareStatement("delete from geometries where uri = ?");
			pst.setString(1, identifier);
			pst.executeUpdate();
		} finally {
			DBBasedManagerImpl.closeDBResources(null, pst, null);
		}
	}

	@Override
	public void deleteAllForService() throws Exception {
		PreparedStatement pst = null; 
		try {
			pst = c.prepareStatement("delete from geometries where serviceId = ?");
			pst.setString(1, serviceId);
			pst.executeUpdate();
		} finally {
			DBBasedManagerImpl.closeDBResources(null, pst, null);
		}
	}

	/**
	 * Metod att �verlagra i subklasser f�r att konvertera gml till en databasspecifik
	 * struct/objekt. Returv�rdet anv�nds med setObject() p� ett prepared statement.
	 * @param gml gml-geometri
	 * @return ett databasspecifikt objekt
	 * @throws Exception vid fel
	 */
	protected abstract Object convertToNative(String gml) throws Exception;
}
