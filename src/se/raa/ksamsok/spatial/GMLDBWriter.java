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
	 * @throws Exception vid problem
	 */
	void init(String serviceId, Connection c) throws Exception;

	/**
	 * Frig�r eventuella resurser instansen h�ller.
	 */
	void destroy();

	/**
	 * Anropas vid insert av post.
	 * @param gmlInfoHolder gmldatah�llare
	 * @return antal inlagda geometrier
	 * @throws Exception vid fel
	 */
	int insert(GMLInfoHolder gmlInfoHolder) throws Exception;

	/**
	 * Anropas vid update av post.
	 * @param gmlInfoHolder gmldatah�llare
	 * @return antal �ndringar (inlagda + borttagna)
	 * @throws Exception vid fel
	 */
	int update(GMLInfoHolder gmlInfoHolder) throws Exception;

	/**
	 * Anropas n�r en post tas bort.
	 * @param identifier uri
	 * @return antal borttagna
	 * @throws Exception vid fel
	 */
	int delete(String identifier) throws Exception;

	/**
	 * Anropas f�r att ta bort alla geometrier i databasen f�r tj�nsten.
	 * Anv�nds enbart i samband med rensning av repository f�r en tj�nst.
	 * @throws Exception
	 */
	int deleteAllForService() throws Exception;

}
