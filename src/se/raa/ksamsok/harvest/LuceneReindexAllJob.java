package se.raa.ksamsok.harvest;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import se.raa.ksamsok.harvest.StatusService.Step;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Klass som k�r omindexering av alla tj�nster i form av en tj�nst/cron-jobb. Obs att
 * denna inte kan/ska kunna scheduleras.
 */
public class LuceneReindexAllJob extends HarvestJob {

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
			HarvestRepositoryManager hrm = getHarvestRepositoryManager(ctx);
			ss = getStatusService(ctx);
			String serviceId = jd.getName();
			if (logger.isInfoEnabled()) {
				logger.info("K�r jobb f�r att indexera om lucene-index fr�n repo(" + serviceId + ")");
			}
			service = new HarvestServiceImpl();
			service.setId(serviceId);
			service.setName("Temp-jobb f�r Lucene-indexering");

			ss.initStatus(service, "Init");
			ss.setStep(service, Step.INDEX);
			List<HarvestService> services = hsm.getServices();
			ss.setStatusTextAndLog(service, "Startar omindexering av " + services.size() + " tj�nster");
			long start = System.currentTimeMillis();
			for (HarvestService reindexMe: services) {
				if (ss.getStep(reindexMe) != Step.IDLE || hsm.isRunning(reindexMe)) {
					ss.setStatusTextAndLog(service, "Tj�nsten " + reindexMe.getId() + " k�r, hoppar �ver den");
					continue;
				}
				ss.setStatusTextAndLog(service, "Startar indexering av tj�nst " + reindexMe.getId());
				long serviceStart = System.currentTimeMillis();
				// "initiera jobb" och k�r ungef�r som i HarvestJob f�r reindex
				ss.initStatus(reindexMe, "Init");
				ss.setStatusTextAndLog(reindexMe, "Uppdaterar lucene-index fr�n repository (k�rs av " + service.getId() + ")");
				try {
					ss.setStep(reindexMe, Step.INDEX);
					hrm.updateLuceneIndex(reindexMe, null, service);
				} catch (Exception e) {
					// s�tta felet p� aktuell tj�nst och kasta vidare s� att det ocks� s�tts p� reindexall
					String errMsg = e.getMessage();
					if (errMsg == null || errMsg.length() == 0) {
						errMsg = e.toString();
					}
					ss.setErrorTextAndLog(reindexMe, errMsg);
					throw e;
				} finally {
					ss.setStep(reindexMe, Step.IDLE);
				}
				long durationMillis = System.currentTimeMillis() - serviceStart;
				ss.setStatusTextAndLog(reindexMe, "Ok, k�rtid " + ContentHelper.formatRunTime(durationMillis));

				// kontrollera om reindexall-jobbet ska avbrytas
				ss.checkInterrupt(service);
				ss.setStatusTextAndLog(service, "Indexerade tj�nst " + reindexMe.getId() +
						", tid: " + ContentHelper.formatRunTime(durationMillis));
			}
			long durationMillis = System.currentTimeMillis() - start;
			ss.setStatusTextAndLog(service, "Omindexering genomf�rd, tid: " +
					ContentHelper.formatRunTime(durationMillis));
			ss.setStep(service, Step.IDLE);
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
