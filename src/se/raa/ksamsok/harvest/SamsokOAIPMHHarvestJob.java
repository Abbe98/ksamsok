package se.raa.ksamsok.harvest;

/**
 * Klass som hanterar sk�rd av OAI-PMH-data i k-sams�ksformat.
 */
public class SamsokOAIPMHHarvestJob extends OAIPMHHarvestJob {
	public SamsokOAIPMHHarvestJob() {}

	@Override
	protected String getMetadataFormat() {
		// TODO: ska det vara vanilj-rdf eller kulturarvsdata-rdf?
		//       Nils har anv�nt detta i sin nod s� det f�r vara s� h�r tills vidare i alla fall
		return "http://kulturarvsdata.se/schema/ksamsok-rdf#";
		//return "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	}
}
