package se.raa.ksamsok.harvest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * Spindeln i n�tet f�r sk�rdehantering. Bootstrappar de flesta tj�nsterna men det mesta
 * av logiken sk�ts mha jsp.
 */
public class HarvesterServlet extends HttpServlet {

	/** parameter som pekar ut var h�mtad xml ska mellanlagras, om ej satt anv�nds tempdir */
	protected static final String D_HARVEST_SPOOL_DIR = "samsok-harvest-spool-dir";

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.HarvesterServlet");

	/**
	 * Namn p� datak�llan som ska vara konfigurerad i servlet-containern, jdbc/[namn].
	 */
	static final String DATASOURCE_NAME = "harvestdb";

	private static final long serialVersionUID = 1L;

	private DataSource ds = null;
	protected HarvestServiceManagerImpl hsm;
	protected HarvestRepositoryManagerImpl hrm;
	protected StatusService ss;
	protected File spoolDir;

	private static HarvesterServlet instance = null;

	public static HarvesterServlet getInstance() {
		if (instance == null) {
			throw new RuntimeException("Systemet ej korrekt initialiserat eller " +
					"p� v�g att st�ngas ned (ingen instans)");
		}
		return instance;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if (logger.isInfoEnabled()) {
			logger.info("Startar HarvesterServlet");
		}
		try {
			Context ctx = new InitialContext();
		    Context envctx =  (Context) ctx.lookup("java:comp/env");
		    ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
		    envctx.close();
		} catch (Exception e) {
			throw new ServletException(e);
		}
		spoolDir = new File(System.getProperty(D_HARVEST_SPOOL_DIR,
				System.getProperty("java.io.tmpdir")));
		if (!spoolDir.exists() || !spoolDir.isDirectory() || !spoolDir.canWrite()) {
			throw new ServletException("Problem med spooldir " + spoolDir +
				", kontrollera att katalogen finns och �r skrivbar");
		}
		try {
			ss = new StatusServiceImpl(ds);
			hrm = new HarvestRepositoryManagerImpl(ds, ss, spoolDir);
			hsm = new HarvestServiceManagerImpl(ds, hrm, ss);
			hsm.init();
		} catch (Exception se) {
			throw new ServletException("Fel vid init av repo, schedulerare och jobs", se);
		}
		instance = this; // hack
		if (logger.isInfoEnabled()) {
			logger.info("HarvesterServlet startad");
		}
	}

	@Override
	public void destroy() {
		if (logger.isInfoEnabled()) {
			logger.info("Stoppar HarvesterServlet");
		}
		hsm.destroy();
		super.destroy();
		if (logger.isInfoEnabled()) {
			logger.info("HarvesterServlet stoppad");
		}
		instance = null;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			PrintWriter p = resp.getWriter();
			writePageHead(p, "Harvester");
			p.write("<h4>HelloWorld</h4>");
			writePageFoot(p);
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * H�mtar HarvestServiceManager som hanterar tj�nster.
	 * 
	 * @return HarvestServiceManager
	 */
	public HarvestServiceManager getHarvestServiceManager() {
		return hsm;
	}

	/**
	 * H�mtar HarvestRepositoryManager som hanterar lagring i repositoryt.
	 * 
	 * @return HarvestRepositoryManager
	 */
	public HarvestRepositoryManager getHarvestRepositoryManager() {
		return hrm;
	}

	/**
	 * H�mtar StatusService som hanterar status och jobbkontroll.
	 * 
	 * @return StatusService
	 */
	public StatusService getStatusService() {
		return ss;
	}

	/**
	 * Ger spoolkatalog, antingen satt via parametern {@link #D_HARVEST_SPOOL_DIR} eller
	 * java.io.tempdir.
	 * 
	 * @return spoolkatalog
	 */
	public File getSpoolDir() {
		return spoolDir;
	}


	private void writePageHead(PrintWriter p, String title) {
		p.write("<html><head><title>" +  title + "</title></head><body>");
	}
	private void writePageFoot(PrintWriter p) {
		p.write("</body></html>");
	}
}
