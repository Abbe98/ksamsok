package se.raa.ksamsok.spatial;

/**
 * Enkel klass som inte g�r n�n konvertering av gml:en utan returnerar den.
 * Anv�nds fr�mst f�r debug och databaskolumnen geometry m�ste vara anpassad till
 * detta.
 */
public class VerbatimGMLDBWriter extends AbstractGMLDBWriter {

	public VerbatimGMLDBWriter() {}

	@Override
	protected Object convertToNative(String gml) throws Exception {
		return gml;
	}

}
