package se.raa.ksamsok.spatial;

import java.sql.Connection;
import java.sql.PreparedStatement;

import se.raa.ksamsok.harvest.DBUtil;

/**
 * Basklass som kan anv�ndas f�r att implementera databasspecifika instanser av
 * GMLDBWriter.
 */
public abstract class AbstractGMLDBWriter implements GMLDBWriter {

	protected String serviceId;
	protected Connection c;
	protected PreparedStatement deleteByUriPst;
	protected PreparedStatement insertPst;

	protected AbstractGMLDBWriter() {
	}

	@Override
	public void init(String serviceId, Connection c) throws Exception {
		this.serviceId = serviceId;
		this.c = c;
		// f�rbered n�gra frekvent anv�nda databas-statements
		this.insertPst = c.prepareStatement("insert into geometries " +
				"(uri, serviceId, name, geometry) values (?, ? , ?, ?)");
		this.deleteByUriPst = c.prepareStatement("delete from geometries where uri = ?");
	}

	@Override
	public void destroy() {
		DBUtil.closeDBResources(null, deleteByUriPst, null);
		DBUtil.closeDBResources(null, insertPst, null);
		deleteByUriPst = null;
		insertPst = null;
		c = null;
	}

	@Override
	public int insert(GMLInfoHolder gmlInfoHolder) throws Exception {
		int inserted = 0;
		if (gmlInfoHolder.hasGeometries()) {
			//pst = c.prepareStatement("insert into geometries " +
			//"(uri, serviceId, name, geometry) values (?, ? , ?, ?)");
			insertPst.setString(1, gmlInfoHolder.getIdentifier());
			insertPst.setString(2, serviceId);
			insertPst.setString(3, gmlInfoHolder.getName());
			for (String gml: gmlInfoHolder.getGmlGeometries()) {
				Object g = convertToNative(gml);
				insertPst.setObject(4, g);
				inserted += insertPst.executeUpdate();
			}
		}
		return inserted;
	}

	@Override
	public int update(GMLInfoHolder gmlInfoHolder) throws Exception {
		// ta bort alla och sen stoppa in nya - det kan vara fler/f�rre �n innan s�
		// det �r enklare att rensa och stoppa in p� nytt �n att f�rs�ka uppdatera
		// befintliga tupler
		int updated = 0;
		updated += delete(gmlInfoHolder.getIdentifier());
		updated += insert(gmlInfoHolder);
		return updated;
	}

	@Override
	public int delete(String identifier) throws Exception {
		int deleted = 0;
		if (identifier == null) {
			return deleted;
		}
		// TODO: l�gga p� serviceId som villkor ocks�?
		//pst = c.prepareStatement("delete from geometries where uri = ?");
		deleteByUriPst.setString(1, identifier);
		deleted = deleteByUriPst.executeUpdate();
		return deleted;
	}

	@Override
	public int deleteAllForService() throws Exception {
		int deleted = 0;
		PreparedStatement pst = null;
		try {
			pst = c.prepareStatement("delete from geometries where serviceId = ?");
			pst.setString(1, serviceId);
			deleted = pst.executeUpdate();
		} finally {
			DBUtil.closeDBResources(null, pst, null);
		}
		return deleted;
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
