package se.raa.ksamsok.lucene;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

/**
 * LuceneServlet, hanterar gr�nssnitt mot lucene
 *
 */
public class LuceneServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(LuceneServlet.class);

	// synkobjekt f�r att begr�nsa �tkomst map IndexWriter
	public static final Object IW_SYNC = new Object();
	private static final int WRITER_CLOSE_WAIT = 15;

	// konstanter som kan p�verka lucene-prestanda, se lucene-dokumentation
	private static final int MERGE_FACTOR = 20; // default �r 10
	private static final double RAM_BUFFER_SIZE_MB = 48; // default �r 16
	private static final int TERM_INDEX_INTERVAL = 256; // default �r 128

	/**
	 * Systemparameter f�r katalog f�r lucene-index, om ej satt anv�nds /var/lucene-index/ksamsok.
	 */
	public static final String D_LUCENE_INDEX_DIR = "samsok-lucene-index-dir";

	/**
	 * Namn p� datak�llan som ska vara konfigurerad i servlet-containern, jdbc/[namn].
	 */
	static final String DATASOURCE_NAME = "harvestdb";

	private static final String LUCENE_DEFAULT = "/var/lucene-index/ksamsok";

	protected Directory indexDir;
	protected IndexSearcher is = null;
	protected IndexWriter iw = null;
	protected Map<IndexSearcher, Long> searchers = new HashMap<IndexSearcher, Long>();
	protected boolean iwBorrowed = false;
	protected boolean isDestroying = false;
	private static LuceneServlet instance;

	/**
	 * H�mtar k�rande instans.
	 * @return aktuell instans
	 */
	public static LuceneServlet getInstance() {
		if (instance == null) {
			throw new RuntimeException("LuceneServlet har inte initialiserats korrekt");
		}
		return instance;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		if (logger.isInfoEnabled()) {
			logger.info("Startar LuceneServlet");
		}
		// �sterst�ll statusvariabler utifall att denna instans �teranv�nds av
		// servletcontainern
		isDestroying = false;
		iwBorrowed = false;
		try {
			// f�rsk� h�mta katalogv�rde fr�n systemproperties, ta LUCENE_DEFAULT annars
			String dir = System.getProperty(D_LUCENE_INDEX_DIR, LUCENE_DEFAULT);
			File fDir = new File(dir);
			if (!fDir.exists() || !fDir.isDirectory() || !fDir.canWrite()) {
				throw new ServletException("Problem med tilldelad katalog f�r lucene-index: " + dir +
						", kontrollera skrivr�ttigheter och att katalogen finns");
			}
			// TODO: NIOFSDirectory tydligen l�ngsam/trasig p� win pga en sun-bug
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6265734 
			// https://issues.apache.org/jira/browse/LUCENE-753
			indexDir = NIOFSDirectory.getDirectory(fDir);

			boolean createIndex = false;
			if (!IndexReader.indexExists(indexDir)) {
				if (logger.isInfoEnabled()) {
					logger.info("Inget index hittat i " + fDir + ", skapar nytt");
				}
				createIndex = true;
			} else if (IndexWriter.isLocked(indexDir)) {
				logger.warn("lucene-indexet var l�st vilket det inte borde varit, l�ser upp det f�r skrivning");
				IndexWriter.unlock(indexDir);
			}
			iw = createIndexWriter(indexDir, createIndex);
			is = new IndexSearcher(IndexReader.open(indexDir, true));
			synchronized (searchers) {
				// stoppa in is s� att den inte beh�ver specialbehandlas i destroy
				searchers.put(is, Long.valueOf(0));
			}
			instance = this;
		} catch (Throwable t) {
			logger.error("Fel vid init av lucene-index", t);
			throw new UnavailableException("Fel vid init av lucene-index");
		}
		if (logger.isInfoEnabled()) {
			logger.info("LuceneServlet startad");
		}
	}

	@Override
	public void destroy() {
		isDestroying = true;
		if (logger.isInfoEnabled()) {
			logger.info("Stoppar LuceneServlet");
		}
		IndexReader r = null;
		synchronized (searchers) {
			for (IndexSearcher searcher: searchers.keySet()) {
				// m�ste spara en referens d� is.close() inte st�nger en inskickad reader
				// vilket vi g�r i init() f�r att kunna ange readOnly p� den
				r = searcher.getIndexReader();
				try {
					searcher.close();
				} catch (IOException e) {
					log("Fel vid nedst�ngning av lucene-index-searcher", e);
				}
				try {
					r.close();
				} catch (IOException e) {
					log("Fel vid nedst�ngning av lucene-index-reader", e);
				}
			}
		}
		if (iw != null) {
			if (iwBorrowed) {
				if (logger.isInfoEnabled()) {
					logger.info("En writer �r utl�nad, v�ntar " + WRITER_CLOSE_WAIT + " sek p� att den ska l�mnas tillbaka");
				}
				try {
					Thread.sleep(WRITER_CLOSE_WAIT * 1000);
				} catch (Exception ignore) {
				}
				if (iwBorrowed) {
					logger.warn("Writer fortfarande utl�nad efter v�ntan, g�r rollback och st�nger den �nd�");
				}
				try {
					iw.rollback();
				} catch (Exception e) {
					logger.error("Fel vid rollback f�r iw vid destroy", e);
				}
			}
			try {
				iw.close();
			} catch (Exception e) {
				logger.error("Fel vid st�ngning av iw vid destroy", e);
			}
		}
		if (indexDir != null) {
			try {
				indexDir.close();
			} catch (IOException e) {
				log("Fel vid nedst�ngning av lucene-index-dir", e);
			}
		}
		super.destroy();
		if (logger.isInfoEnabled()) {
			logger.info("LuceneServlet stoppad");
		}
	}

	/**
	 * L�nar en IndexSearcher f�r s�kning i indexet, m�ste l�mnas tillbaka.
	 * 
	 * @return en IndexSearcher
	 */
	public IndexSearcher borrowIndexSearcher() {
		if (isDestroying) {
			throw new RuntimeException("Applikationen h�ller p� att st�nga ner");
		}
		IndexSearcher ret = null;
		synchronized(searchers) {
			ret = is;
			Long c = searchers.get(ret);
			if (c == null) {
				c = Long.valueOf(1);
			} else {
				c = Long.valueOf(c.longValue() + 1);
			}
			searchers.put(ret, c);
		}
		return ret;
	}
	
	/**
	 * L�mnar tillbaka en IndexSearcher.
	 * 
	 * @param ret IndexSearcher
	 */
	public void returnIndexSearcher(IndexSearcher ret) {
		// TODO: hantera gamla indexers som kanske inte l�mnas tillbaka b�ttre
		//       s� att m�ngden indexers inte bara v�xer, l�gg till timestamp?
		if (isDestroying) {
			// allt st�ngs i destroy
			return;
		}
		if (ret == null) {
			return;
		}
		synchronized (searchers) {
			Long c = searchers.get(ret);
			if (c == null) {
				logger.error("Fick tillbaka en is som inte anv�nts?: " + ret);
			} else {
				c = Long.valueOf(c.longValue() - 1);
				if (ret != is && c.longValue() == 0) {
					// om det �r en gammal instans som kommer tillbaka s� st�nger vi den
					searchers.remove(ret);
					IndexReader ir = ret.getIndexReader();
					try {
						ret.close();
						ir.close();
					} catch (IOException e) {
						logger.error("Fel vid st�ngning av is och ir vid livscykelavslut", e);
					}
					logger.debug("St�ngde en gammal is (och ir): " + ret);
				} else {
					if (c.longValue() < 0) {
						// varna och f�rs�k "fixa"
						logger.warn("IndexSearcher har l�mnats tillbaka mer g�nger �n den l�nats ut");
						c = Long.valueOf(0);
					}
					searchers.put(ret, c);
				}

			}
		}
	}

	/**
	 * L�nar en IndexWriter f�r skrivning i lucene-indexet, m�ste l�mnas tillbaka.
	 * 
	 * @return en IndexWriter
	 */
	public IndexWriter borrowIndexWriter() {
		if (isDestroying) {
			throw new RuntimeException("Applikationen h�ller p� att st�nga ner");
		}
		synchronized (IW_SYNC) {
			if (iwBorrowed) {
				throw new RuntimeException("Bara en iw i taget");
			}
			iwBorrowed = true;
		}
		return iw;
	}

	/**
	 * L�mnar tillbaka en utl�nad IndexWriter.
	 * 
	 * @param ret IndexWriter
	 * @param refresh sant om data har skrivits och indexl�sarna ska uppdateras
	 */
	public void returnIndexWriter(IndexWriter ret, boolean refresh) {
		if (ret == null) {
			return;
		}
		synchronized (IW_SYNC) {
			if (!iwBorrowed) {
				logger.warn("Fel iw tillbaka: " + ret + ", ingen var utl�nad...");
			}
			if (iw != ret) {
				logger.error("Fel iw tillbaka: " + ret + ", borde varit: " + iw);
			} else {
				iwBorrowed = false;
			}
			// f�rs�k avg�ra om iw:n har blivit st�ngd pga rollback
			if (!isDestroying) {
				try {
					iw.getTermIndexInterval();
				} catch (Exception e) {
					if (logger.isInfoEnabled()) {
						logger.info("iw st�ngd (pga rollback(?)), skapar ny");
					}
					try {
						iw = createIndexWriter(indexDir, false);
					} catch (Exception e2) {
						logger.error("Fel vid skapande av ny iw", e2);
					}
				}
			}
		}
		if (!isDestroying && refresh) {
			synchronized (searchers) {
				try {
					IndexReader old = is.getIndexReader();
					IndexReader reopened = old.reopen();
					if (reopened != old) {
						// indexet har uppdaterats och en ny searcher/reader m�ste skapas
						Long c = searchers.get(is);
						if (c == null || c.longValue() == 0) {
							// om ej utl�nad, st�ng och ta bort
							searchers.remove(is);
							is.close();
							old.close();
						}
						is = new IndexSearcher(reopened);
						if (logger.isInfoEnabled()) {
							logger.info("lucene-index har uppdaterats");
						}
					}
				} catch (Exception e) {
					logger.error("Fel vid refresh av ir och is", e);
				}
			}
		}
	}

	/**
	 * Optimerar lucene-indexet.
	 * 
	 * @throws Exception
	 */
	public void optimizeLuceneIndex() throws Exception {
		IndexWriter iw = null;
		boolean refreshIndex = false;
		synchronized (IW_SYNC) { // en i taget som f�r k�ra index-write
			try {
				iw = borrowIndexWriter();
				if (logger.isInfoEnabled()) {
					logger.info("Optimize av lucene-index, start");
				}
				iw.optimize();
				if (logger.isInfoEnabled()) {
					logger.info("Optimize av lucene-index, klart");
				}
				iw.commit();
				refreshIndex = true;
			} catch (Throwable e) {
				if (iw != null) {
					try {
						iw.rollback();
					} catch (Exception e2) {
						logger.warn("Fel vid rollback f�r lucene-index", e2);
					}
				}
				logger.error("Fel vid optimize av lucene-index", e);
				throw new Exception(e);
			} finally {
				returnIndexWriter(iw, refreshIndex);
			}
		}
	}

	/**
	 * Rensar lucene-indexet - OBS mycket b�ttre att stoppa tomcat och rensa indexkatalogen.
	 * 
	 * @throws Exception
	 */
	public void clearLuceneIndex() throws Exception {
		IndexWriter iw = null;
		boolean refreshIndex = false;
		synchronized (IW_SYNC) { // en i taget som f�r k�ra index-write
			try {
				iw = borrowIndexWriter();
				if (logger.isInfoEnabled()) {
					logger.info("Rensning av lucene-index, start");
				}
				// alla dokument borde ha ett serviceId-index
				iw.deleteDocuments(new WildcardQuery(new Term(ContentHelper.I_IX_SERVICE, "*")));
				if (logger.isInfoEnabled()) {
					logger.info("Rensning av lucene-index, klart");
				}
				iw.commit();
				refreshIndex = true;
			} catch (Throwable e) {
				if (iw != null) {
					try {
						iw.rollback();
					} catch (Exception e2) {
						logger.warn("Fel vid rollback f�r lucene-index", e2);
					}
				}
				logger.error("Fel vid optimize av lucene-index", e);
				throw new Exception(e);
			} finally {
				returnIndexWriter(iw, refreshIndex);
			}
		}
	}

	/**
	 * Ger totala antalet indexerade poster i lucene-indexet.
	 * 
	 * @return antal indexerade poster
	 */
	public int getTotalCount() {
		IndexSearcher s = null;
		int poster = 0;
		try {
			s = borrowIndexSearcher();
			poster = s.getIndexReader().numDocs();
		} finally {
			returnIndexSearcher(s);
		}
		return poster;
	}

	/**
	 * Ger antalet indexerade poster f�r angiven tj�nst.
	 * 
	 * @param serviceId id f�r tj�nst
	 * @return antal indexerade poster
	 */
	public int getCount(String serviceId) {
		IndexSearcher s = null;
		int poster = -1;
		try {
			s = borrowIndexSearcher();
			poster = s.search(new TermQuery(new Term(ContentHelper.I_IX_SERVICE, serviceId)), 1).totalHits;
		} catch (Exception e) {
			logger.error("Fel vid h�mtning av antal indexerade poster f�r tj�nst " + serviceId);
		} finally {
			returnIndexSearcher(s);
		}
		return poster;
	}

	private static IndexWriter createIndexWriter(Directory indexDir, boolean create) throws Exception {
		IndexWriter iw = new IndexWriter(indexDir, ContentHelper.getSwedishAnalyzer(), create, IndexWriter.MaxFieldLength.UNLIMITED);
		iw.setRAMBufferSizeMB(RAM_BUFFER_SIZE_MB);
		iw.setMergeFactor(MERGE_FACTOR);
		iw.setTermIndexInterval(TERM_INDEX_INTERVAL);
		return iw;
	}
}
