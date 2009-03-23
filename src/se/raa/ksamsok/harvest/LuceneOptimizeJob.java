package se.raa.ksamsok.harvest;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import se.raa.ksamsok.harvest.StatusService.Step;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * Klass som k�r en optimering av lucene-index i form av en tj�nst/cron-jobb.
 */
public class LuceneOptimizeJob extends HarvestJob {

	@Override
	protected List<ServiceFormat> performGetFormats(HarvestService service)
			throws Exception {
		return Collections.emptyList();
	}

	@Override
	protected int performGetRecords(HarvestService service, ServiceMetadata sm,
			ServiceFormat f, File storeTo, StatusService ss) throws Exception {
		return 0;
	}

	@Override
	protected ServiceMetadata performIdentify(HarvestService service)
			throws Exception {
		return null;
	}

	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		interrupted = false;
		StatusService ss = null;
		HarvestService service = null;
		try {
			JobDetail jd = ctx.getJobDetail();
			HarvestServiceManager hsm = getHarvestServiceManager(ctx);
			ss = getStatusService(ctx);
			String serviceId = jd.getName();
			if (logger.isInfoEnabled()) {
				logger.info("K�r jobb f�r att optimera lucene-index (" + serviceId + ")");
			}
			service = hsm.getService(serviceId);
			boolean hasService = (service != null);
			if (!hasService) {
				service = new HarvestServiceImpl();
				service.setId(serviceId);
				service.setName("Temp-jobb f�r Lucene-optimering");
			}
			ss.initStatus(service, "Init");
			ss.setStep(service, Step.INDEX);
			ss.setStatusTextAndLog(service, "Startar index-optimering");
			long start = System.currentTimeMillis();
			LuceneServlet.getInstance().optimizeLuceneIndex();
			long durationMillis = System.currentTimeMillis() - start;
			ss.setStatusTextAndLog(service, "Index-optimering genomf�rd p� " +
					ContentHelper.formatRunTime(durationMillis));
			ss.setStep(service, Step.IDLE);
			// uppdatera bara om vi har en tj�nst med inskickat id, annars �r det en eng�ngsk�rning
			if (hasService) {
				hsm.updateServiceDate(service, new Date());
			}
			if (logger.isDebugEnabled()) {
				List<String> log = ss.getStatusLog(service);
				logger.debug(serviceId + ": ----- logsammanfattning -----");
				for (String logMsg: log) {
					logger.debug(serviceId + ": " + logMsg);
				}
			}

		} catch (Exception e) {
			String errMsg = e.getMessage();
			if (errMsg == null || errMsg.length() == 0) {
				errMsg = e.toString();
			}
			if (ss != null) {
				reportError(service, "Fel vid jobbk�rning i steg " + ss.getStep(service), e);
				ss.setErrorTextAndLog(service, errMsg);
				ss.setStep(service, Step.IDLE);
			} else {
				logger.error("Ingen statusservice att rapportera fel till!");
				reportError(service, "Fel vid jobbk�rning", e);
			}
		}
	}

	
}
