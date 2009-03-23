package se.raa.ksamsok.harvest;

import java.util.ArrayList;
import java.util.List;

/**
 * Klass som hanterar sk�rd av data ifr�n en fil i k-sams�ksformat.
 */
public class SamsokSimpleHarvestJob extends SimpleHarvestJob {
	public SamsokSimpleHarvestJob() {}

	@Override
	protected String getMetadataFormat() {
		return "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	}
	@Override
	protected List<ServiceFormat> performGetFormats(HarvestService service)
			throws Exception {
		final List<ServiceFormat> list = new ArrayList<ServiceFormat>(1);
		list.add(new ServiceFormat("rdf",
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
				"http://www.w3.org/2000/07/rdf.xsd")); // TODO: r�tt schemaplats
		return list;
	}


}
