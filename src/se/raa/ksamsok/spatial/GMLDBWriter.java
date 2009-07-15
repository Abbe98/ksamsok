package se.raa.ksamsok.spatial;

import java.sql.Connection;

/**
 * Interface f�r att hantera skrivning av spatial-data i form av gml till databas.
 * Instanser skapas med default-konstruktorn och init() anropas innan n�gon av de andra
 * metoderna k�rs. Se ocks� {@linkplain GMLUtil#getGMLDBWriter(String, Connection)}.
 * Alla gml-geometrier �r konverterade till SWEREF 99 TM (EPSG:3006) innan de kommer
 * in till instanser av GMLDBWriter.
 */
public interface GMLDBWriter {

	/**
	 * Initierar instansen med v�rden f�r aktuell tj�nst och databas.
	 * @param serviceId id f�r tj�nsten
	 * @param c en uppkoppling mot databasen
	 */
	void init(String serviceId, Connection c);

	/**
	 * Anropas vid insert av ny post i repositoriet (nytt lucenedokument).
	 * @param gmlInfoHolder gmldatah�llare
	 * @throws Exception vid fel
	 */
	void insert(GMLInfoHolder gmlInfoHolder) throws Exception;

	/**
	 * Anropas vid update av ny post i repositoriet (nytt lucenedokument).
	 * @param gmlInfoHolder gmldatah�llare
	 * @throws Exception vid fel
	 */
	void update(GMLInfoHolder gmlInfoHolder) throws Exception;

	/**
	 * Anropas n�r en post tas bort ur repositoriet.
	 * @param oaiURI oaiURI
	 * @throws Exception vid fel
	 */
	void delete(String oaiURI) throws Exception;

	/**
	 * Anropas vid rensning av alla poster f�r denna tj�nst.
	 * @throws Exception vid fel
	 */
	void deleteAllForService() throws Exception;

}
