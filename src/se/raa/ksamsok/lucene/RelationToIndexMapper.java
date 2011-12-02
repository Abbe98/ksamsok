package se.raa.ksamsok.lucene;


/**
 * Interface f�r klasser f�r specialhantering av relationer i form av �vers�ttning av
 * URI:er till indexnamn/relationstypnamn.
 */
public interface RelationToIndexMapper {

	/**
	 * Ger ett relationstypnamn (typiskt ett indexnamn) f�r en relations-URI.
	 * @param refUri URI
	 * @return relationstypnamn eller null om URI:n inte kunde tolkas/�vers�ttas. 
	 */
	String getRelationTypeNameFromURI(String refUri);
}
