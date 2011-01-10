package se.raa.ksamsok.spatial;

import java.sql.PreparedStatement;

/**
 * Postgres/postgis-specifik variant av GMLDBWriter.
 */
public class PostgresGMLDBWriter extends AbstractGMLDBWriter {

	// TODO: m�jligt att denna �veragring inte beh�vs om setObject hanteras ok med det �ndrade pst:t
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
				String g = (String) convertToNative(gml);
				insertPst.setString(4, g);
				inserted += insertPst.executeUpdate();
			}
		}
		return inserted;
	}

	@Override
	protected PreparedStatement prepareInsert() throws Exception {
		return c.prepareStatement("insert into geometries " +
				"(uri, serviceId, name, geometry) values (?, ? , ?, ST_GeomFromGML(?))");
	}
	@Override
	protected Object convertToNative(String gml) throws Exception {
		// hack d� postgres/postgis inte gillar GeometryCollection utan f�redrar MultiGeometry
		// GeometryCollection finns inte i gml 2+ utan bara i gml 1 men f�rekommer ev i ra�:s data fn
		return gml.replace("GeometryCollection", "MultiGeometry");
	}

}
