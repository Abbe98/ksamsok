package se.raa.ksamsok.harvest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.quartz.InterruptableJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.StatefulJob;
import org.quartz.UnableToInterruptJobException;

import se.raa.ksamsok.harvest.StatusService.Step;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Basklass f�r sk�rdejobb.
 */
public abstract class HarvestJob implements StatefulJob, InterruptableJob {

	protected final Logger logger;
	boolean interrupted;

	protected HarvestJob() {
		logger = Logger.getLogger(this.getClass().getName());
	}

	/**
	 * H�mtar HarvestServiceManager fr�n cron-context.
	 * 
	 * @param ctx context
	 * @return HarvestServiceManager
	 * @throws SchedulerException
	 */
	protected HarvestServiceManager getHarvestServiceManager(JobExecutionContext ctx) throws SchedulerException {
		return (HarvestServiceManager) ctx.getScheduler().getContext().get(HarvestServiceManager.HSM_KEY);
	}

	/**
	 * H�mtar HarvestRepositoryManager fr�n cron-context.
	 * 
	 * @param ctx context
	 * @return HarvestRepositoryManager
	 * @throws SchedulerException
	 */
	protected HarvestRepositoryManager getHarvestRepositoryManager(JobExecutionContext ctx) throws SchedulerException {
		return (HarvestRepositoryManager) ctx.getScheduler().getContext().get(HarvestServiceManager.HRM_KEY);
	}

	/**
	 * H�mtar StatusService fr�n cron-context.
	 * 
	 * @param ctx context
	 * @return StatusService
	 * @throws SchedulerException
	 */
	protected StatusService getStatusService(JobExecutionContext ctx) throws SchedulerException {
		return (StatusService) ctx.getScheduler().getContext().get(HarvestServiceManager.SS_KEY);
	}

	/**
	 * G�r (OAI-PMH) identify.
	 * 
	 * @param service tj�nst
	 * @return ett v�rdeobjekt med metadata om sk�rdenoden
	 * @throws Exception
	 */
	protected abstract ServiceMetadata performIdentify(HarvestService service) throws Exception;

	/**
	 * G�r (OAI-PMH) getFormats.
	 * 
	 * @param service tj�nst
	 * @return lista med av sk�rdenoden st�dda format
	 * @throws Exception
	 */
	protected abstract List<ServiceFormat> performGetFormats(HarvestService service) throws Exception;

	/**
	 * G�r (OAI-PMH) getSets.
	 * 
	 * @param service tj�nst
	 * @return lista med av sk�rdenoden st�dda sets.
	 * @throws Exception
	 */
	protected List<String> performGetSets(HarvestService service) throws Exception {
		return Collections.emptyList();
	}

	/**
	 * G�r (OAI-PMH) getRecords.
	 * 
	 * @param service tj�nst
	 * @param sm service-metadata (fr�n identify)
	 * @param f service-format (�nskat format)
	 * @param storeTo katalog att mellanlagra i
	 * @param ss statusservice
	 * @return antal records, eller -1 om det inte kunde best�mmas
	 * @throws Exception
	 */
	protected abstract int performGetRecords(HarvestService service, ServiceMetadata sm, ServiceFormat f, File storeTo, StatusService ss) throws Exception;

	/**
	 * Ger uri f�r �nskat metadataformat.
	 * 
	 * @return uri
	 */
	protected String getMetadataFormat() {
		return "http://www.openarchives.org/OAI/2.0/oai_dc/";
	}

	/* (non-Javadoc)
	 * @see org.quartz.InterruptableJob#interrupt()
	 */
	public void interrupt() throws UnableToInterruptJobException {
		interrupted = true;
	}

	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		// 1. init
		// 2. kontrollera startsteg
		// 3. om start �r index k�r det och sluta, annars k�r identify
		// 4. om vi har en h�mtad fil sen tidigare i spool ta den och g� till steg 8
		// 5. h�mta och kontrollera metadataformat (getMetadataFormats)
		// 6. om vi ska h�mta ett visst set, h�mta st�dda sets och kontrollera (getSets)
		// 7. h�mta data till temp och flytta sen till spool-fil (getRecords)
		// 8. g� igenom och lagra sk�rd i repo (lagra undan full sk�rd)
		// 9. uppdatera lucene-index fr�n repo
		// 10. klar
		interrupted = false;
		Date now = new Date();
		Timestamp nowTs = new Timestamp(now.getTime());
		Date lastSuccessfulHarvestDate; // TODO: ts eller date?
		File temp = null;
		File spoolFile = null;
		HarvestService service = null;
		StatusService ss = null;
		long start = System.currentTimeMillis();
		try {
			JobDetail jd = ctx.getJobDetail();
			HarvestServiceManager hsm = getHarvestServiceManager(ctx);
			HarvestRepositoryManager hrm = getHarvestRepositoryManager(ctx);
			ss = getStatusService(ctx);
			String serviceId = jd.getName();
			if (logger.isInfoEnabled()) {
				logger.info("Running job for " + serviceId);
			}
			service = hsm.getService(serviceId);
			ss.containsErrors(service, false);
			if (service == null) {
				throw new JobExecutionException("Could not find service with ID: " + serviceId);
			}
			// specialfall f�r indexering fr�n repo
			if (ss.getStartStep(service) == Step.INDEX) {
				ss.initStatus(service, "Init");
				ss.setStatusTextAndLog(service, "Updating lucene index from repository");
				ss.setStep(service, Step.INDEX);
				hrm.updateLuceneIndex(service, null);
				hsm.storeFirstIndexDateIfNotSet(service);
				long durationMillis = System.currentTimeMillis() - start;
				ss.setStatusTextAndLog(service, "Ok, job time: " + ContentHelper.formatRunTime(durationMillis));
				ss.setStep(service, Step.IDLE);
				return;
			}
			if (ss.getStartStep(service) == Step.EMPTYINDEX) {
				ss.initStatus(service, "Init");
				ss.setStatusTextAndLog(service, "Removing lucene index for service with ID: " + serviceId);
				ss.setStep(service, Step.EMPTYINDEX);
				hrm.removeLuceneIndex(service);
				//hsm.storeFirstIndexDateIfNotSet(service);
				long durationMillis = System.currentTimeMillis() - start;
				ss.setStatusTextAndLog(service, "Ok, job time: " + ContentHelper.formatRunTime(durationMillis));
				ss.setStep(service, Step.IDLE);
				return;
			}
			ss.initStatus(service, "Init");
			ss.setStep(service, Step.FETCH);
			ss.setStatusTextAndLog(service, "Performing Identify");

			ServiceMetadata sm = performIdentify(service);

			int numRecords = -1; // -1 �r ok�nt antal poster
			spoolFile = hrm.getSpoolFile(service);
			// kolla om vi har en h�mtad fil som vi kan anv�nda
			if (!spoolFile.exists()) {
				// ingen tidigare h�mtning att anv�nda
				ss.setStatusTextAndLog(service, "Fetching metadata format");
				List<ServiceFormat> formats = performGetFormats(service);
				String f = getMetadataFormat();
				ServiceFormat format = null;
				for (ServiceFormat sf: formats) {
					if (sf.getNamespace().equals(f)) {
						format = sf;
						break;
					}
				}
				if (format == null) {
					throw new Exception("Requested format (" + f + ") not supported");
				}

				// kolla om vi ska avbryta
				checkInterrupt(ss, service);

				// kontrollera om ev �nskat set st�ds av tj�nsten
				String setSpec = service.getHarvestSetSpec();
				if (setSpec != null) {
					boolean setSpecSupported = false;
					ss.setStatusTextAndLog(service, "Checking specified set: " + setSpec);
					List<String> setSpecs = performGetSets(service);
					for (String fetchedSetSpec: setSpecs) {
						if (setSpec.equals(fetchedSetSpec)) {
							setSpecSupported = true;
							break;
						}
					}
					if (!setSpecSupported) {
						throw new Exception("Specified set not supported: (" + setSpec + "), " + setSpecs);
					}
				}

				// kolla om vi ska avbryta
				checkInterrupt(ss, service);

				// skapa tempfil
				temp = File.createTempFile(jd.getName().substring(0, Math.min(4, serviceId.length())), null);
				// h�mta data till tempfilen
				ss.setStatusTextAndLog(service, "Fetching data to temp file");
				numRecords = performGetRecords(service, sm, format, temp, ss);
				if (numRecords != 0) {
					if (logger.isDebugEnabled()) {
						logger.debug(serviceId + ", Fetched " + numRecords + " records");
					}
					ss.setStatusTextAndLog(service, "Moving temp file to spool");
					if (!temp.renameTo(spoolFile)) {
						throw new Exception("Could not move temp file to spool file, " +
								temp + " -> " + spoolFile);
					}
				}
			} else {
				ss.setStatusTextAndLog(service, "Using existing file from spool");
				if (logger.isDebugEnabled()) {
					logger.debug(serviceId + ", using existing file from spool: " + spoolFile.getName());
				}
			}
			// om vi har records och en spool-fil ska vi bearbeta den
			if (numRecords != 0 && spoolFile.exists()) {
				// kolla om vi ska avbryta
				checkInterrupt(ss, service);

				long fsizeMb = spoolFile.length() / (1024 * 1024);
				// lagra sk�rd i repot
				ss.setStatusTextAndLog(service, "Storing data in repo (" + numRecords + " records, appr " +
						fsizeMb + "MB)");
				if (logger.isDebugEnabled()) {
					logger.debug(serviceId + ", storing data in repo (" + numRecords + " records, appr " +
						fsizeMb + "MB)");
				}
				ss.setStep(service, Step.STORE);
				boolean changed = hrm.storeHarvest(service, sm, spoolFile, nowTs);
				if (logger.isDebugEnabled()) {
					logger.debug(serviceId + ", stored records");
				}

				// arkivera fulla sk�rdar
				// TODO: arkiveringskatalog? tr�d? delta-sk�rdar?
				if (!sm.handlesPersistentDeletes() || service.getLastHarvestDate() == null) {
					// "full sk�rd", arkivera
					ss.setStatusTextAndLog(service, "Archiving full harvest");
					OutputStream os = null;
					InputStream is = null;
					File of = new File(spoolFile.getAbsolutePath() + ".gz");
					byte[] buf = new byte[8192];
					int c;
					try {
						is = new BufferedInputStream(new FileInputStream(spoolFile));
						os = new GZIPOutputStream(new BufferedOutputStream(
								new FileOutputStream(of)));
						while ((c = is.read(buf)) > 0) {
							os.write(buf, 0, c);
						}
						os.flush();
					} finally {
						closeStream(is);
						closeStream(os);
					}
					ss.setStatusTextAndLog(service, "Archived full harvest to gzip (" + (of.length() / (1024*1024)) + " MB)");
				}
				// ta bort spool-filen d� vi �r klara med inneh�llet
				if (!spoolFile.delete()) {
					logger.error(serviceId + ", could not remove spool file");
					ss.setStatusTextAndLog(service, "Note: Could not remove spool file");
				}

				// TODO: �r detta r�tt datum/tid att s�tta �ven om vi har �terupptagit
				//       ett jobb som inte gick bra? kanske ska ta datum fr�n spoolFile?

				// h�mta senaste lyckade k�rning, bara om tj�nsten st�djer persistent deletes
				// m�jligg�r omindexering av mindre m�ngd
				Timestamp lastSuccessfulHarvestTs = null;
				if (sm.handlesPersistentDeletes()) {
					lastSuccessfulHarvestDate = service.getLastHarvestDate();
					if (lastSuccessfulHarvestDate != null) {
						lastSuccessfulHarvestTs = new Timestamp(lastSuccessfulHarvestDate.getTime());
					}
				}
				// uppdatera senaste sk�rde datum/tid f�r servicen
				if(!ss.containsErrors(service)) {
					hsm.updateServiceDate(service, nowTs);
				}

				// kolla om vi ska avbryta
				checkInterrupt(ss, service);

				// uppdatera lucene-index f�r servicen
				if (changed) {
					ss.setStatusTextAndLog(service, "Updating lucene index" +
							(lastSuccessfulHarvestTs != null ?
									" > " + lastSuccessfulHarvestTs : ""));
					ss.setStep(service, Step.INDEX);
					hrm.updateLuceneIndex(service, lastSuccessfulHarvestTs);
					hsm.storeFirstIndexDateIfNotSet(service);
				} else {
					if (logger.isInfoEnabled()) {
						logger.info(serviceId + ", no index update needed");
					}
				}
				if (logger.isInfoEnabled()) {
					logger.info(serviceId + ", harvested");
				}
			} else {
				if (logger.isInfoEnabled()) {
					logger.info(serviceId + ", harvest resulted in no records");
				}
				if(!ss.containsErrors(service)) {
					// uppdatera med senaste sk�rdetid
					hsm.updateServiceDate(service, now);
				}
			}
			long durationMillis = System.currentTimeMillis() - start;
			ss.setStatusTextAndLog(service, "Ok, job time: " +
					ContentHelper.formatRunTime(durationMillis) + ", " + numRecords + " records");
			ss.setStep(service, Step.IDLE);

			if (logger.isDebugEnabled()) {
				List<String> log = ss.getStatusLog(service);
				logger.debug(serviceId + ": ----- log summary -----");
				for (String logMsg: log) {
					logger.debug(serviceId + ": " + logMsg);
				}
			}
		} catch (Throwable e) {
			// s�tt felmeddelande i statusservicen
			String errMsg = e.getMessage();
			if (errMsg == null || errMsg.length() == 0) {
				errMsg = e.toString();
			}
			if (ss != null) {
				reportError(service, "Error in job step: " + ss.getStep(service), e);
				ss.setErrorTextAndLog(service, errMsg);
				ss.setStep(service, Step.IDLE);
			} else {
				logger.error("No status service to report error to!");
				reportError(service, "Error in job execution", e);
			}
		} finally {
			if (temp != null && temp.exists()) {
				if (!temp.delete()) {
					logger.warn("Could not remove temp file: " + temp.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Kontrollerar om jobbet ska avbrytas och kastar i s� fall ett exception.
	 * 
	 * @param ss statusservice
	 * @param service tj�nst
	 * @throws Exception om jobb ska avbrytas
	 */
	protected void checkInterrupt(StatusService ss, HarvestService service) throws Exception {
		if (interrupted) {
			throw new Exception("Job disrupted on request");
		}
		if (ss != null) {
			ss.checkInterrupt(service);
		}
	}

	/**
	 * Rapporterar fel i loggen.
	 * 
	 * @param service tj�nst
	 * @param message meddelande
	 * @param e fel eller null
	 */
	protected void reportError(HarvestService service, String message, Throwable e) {
		if (service != null) {
			logger.error(service.getId() + " - " + message, e);
		} else {
			logger.error(message, e);
		}
	}

	/**
	 * Hj�lpmetod som st�nger en ut-str�m.
	 * 
	 * @param os str�m
	 */
	protected void closeStream(OutputStream os) {
		if (os != null) {
			try {
				os.close();
			} catch (Exception ignore) {}
		}
	}

	/**
	 * Hj�lpmetod som st�nger en in-str�m.
	 * 
	 * @param is str�m
	 */
	protected void closeStream(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (Exception ignore) {}
		}
	}
}
