package se.raa.ksamsok.harvest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.spi.IIORegistry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Anv�nds bara fn f�r att kolla att systemet verkar korrekt instantierat map spring-tj�nster
 * och all interaktion sker via jsp.
 */
public class HarvesterServlet extends HttpServlet {


	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.HarvesterServlet");

	private static final long serialVersionUID = 1L;

	@Autowired
	private DataSource ds;
	@Autowired
	protected HarvestServiceManagerImpl hsm;
	@Autowired
	protected HarvestRepositoryManagerImpl hrm;
	@Autowired
	protected StatusService ss; 

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if (logger.isInfoEnabled()) {
			logger.info("Startar HarvesterServlet");
		}
		ServletContext servletContext = config.getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		// lite "sanity-checks" p� hela systemet, inget av detta anv�nds i denna servlet
		Validate.notNull(ds, "DataSource �r null, spring har inte initialiserats ok");
		Validate.notNull(ss, "StatusService �r null, spring har inte initialiserats ok");
		Validate.notNull(hsm, "HarvestServiceManager �r null, spring har inte initialiserats ok");
		Validate.notNull(hrm, "HarvestRepositoryManager �r null, spring har inte initialiserats ok");
		if (logger.isInfoEnabled()) {
			logger.info("HarvesterServlet startad");
		}
	}

	@Override
	public void destroy() {
		if (logger.isInfoEnabled()) {
			logger.info("Stoppar HarvesterServlet");
		}
		super.destroy();
		// finalizer-hack f�r att g�ra det m�jligt att redeploya webappen, annars
		// h�lls referencing-jar bla l�st, kanske inte sn�llt om fler webappar k�r
		// i samma tomcat och imageio ligger utanf�r webappen i common/lib men...
		IIORegistry.getDefaultInstance().deregisterAll();
		System.runFinalization();
		if (logger.isInfoEnabled()) {
			logger.info("HarvesterServlet stoppad");
		}
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

	private void writePageHead(PrintWriter p, String title) {
		p.write("<html><head><title>" +  title + "</title></head><body>");
	}
	private void writePageFoot(PrintWriter p) {
		p.write("</body></html>");
	}
}
