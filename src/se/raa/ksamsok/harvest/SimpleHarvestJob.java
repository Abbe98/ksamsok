package se.raa.ksamsok.harvest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Klass som hanterar h�mtning/sk�rd av data fr�n fil.
 */
public class SimpleHarvestJob extends HarvestJob {

	public SimpleHarvestJob() {
		super();
	}

	@Override
	protected ServiceMetadata performIdentify(HarvestService service)
			throws Exception {
		return new ServiceMetadata(ServiceMetadata.D_TRANSIENT, ServiceMetadata.G_DAY);
	}

	@Override
	protected List<ServiceFormat> performGetFormats(HarvestService service)
			throws Exception {
		final List<ServiceFormat> list = new ArrayList<ServiceFormat>(1);
		list.add(new ServiceFormat("oai_dc",
				"http://www.openarchives.org/OAI/2.0/oai_dc/",
				"http://www.openarchives.org/OAI/2.0/oai_dc.xsd"));
		return list;
	}

	@Override
	protected int performGetRecords(HarvestService service,
			ServiceMetadata sm, ServiceFormat f, File storeTo, StatusService ss) throws Exception {
		int result = 0;
		if (logger.isDebugEnabled()) {
			logger.debug(service.getId() + " - H�mtar " + service.getHarvestURL() + ", senaste h�mtning: " + service.getLastHarvestDate());
		}
		byte[] buf = new byte[16384];
		int read;
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			URL u = new URL(service.getHarvestURL());
			is = u.openStream();
			fos = new FileOutputStream(storeTo);
			while ((read = is.read(buf)) > 0) {
				fos.write(buf, 0, read);
			}
			fos.flush();
			// anv�nd -1 d� vi inte vet hur m�nga poster det �r
			result = -1;
		} finally {
			closeStream(is);
			closeStream(fos);
		}
		return result;
	}
}
