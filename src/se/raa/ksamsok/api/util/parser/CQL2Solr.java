package se.raa.ksamsok.api.util.parser;

import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLSortNode;
import org.z3950.zing.cql.CQLTermNode;
import org.z3950.zing.cql.Modifier;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.spatial.GMLUtil;

/**
 * Kod inspirerad av LuceneTranslator fr�n 
 * srwlucene 1.0 (http://wiki.osuosl.org/display/OCKPub/SRWLucene)
 * vilken �r under apache 2.0-licens.
 * 
 * TODO: hanterar bara enkla fr�gor fn
 */
public class CQL2Solr {

	private static final Logger logger = Logger.getLogger(CQL2Solr.class);

	private static final String INDEX_CQL_SERVERCHOICE = "cql.serverChoice";
	private static final String INDEX_CQL_RESULTSETID = "cql.resultSetId";

	//private static final Query NO_MATCH_DUMMY_QUERY = new TermQuery(new Term("dumdummy", "dummy"));

	/**
	 * Skapar en query utifr�n en CQL-nod.
	 * 
	 * @param node cql-nod
	 * @return query eller null
	 */
	public static String makeQuery(CQLNode node)
		throws DiagnosticException, BadParameterException {
		return makeQuery(node, null);
	}

	/**
	 * Skapar en lucene-query utifr�n en CQL-nod och ett v�nsterled.
	 * 
	 * @param node cql-nod
	 * @param leftQuery v�nsterled i form av en lucene-query eller null
	 * @return en lucene-query eller null
	 */
	static String makeQuery(CQLNode node, String leftQuery)
		throws DiagnosticException, BadParameterException {
		String query = null;

		if (node instanceof CQLBooleanNode)  {
			CQLBooleanNode cbn = (CQLBooleanNode) node;

			String left = makeQuery(cbn.left);
			String right = makeQuery(cbn.right, left);

			if (node instanceof CQLAndNode) {
				// TODO: blir detta r�tt i alla fall som kan uppst�?
				// om h�ger tr�d �r en or l�gger vi en parentes runt
				boolean rIsOr = (cbn.right instanceof CQLOrNode);
				query = left + " AND " + (rIsOr ? "(" + right + ")" : right);
			} else if (node instanceof CQLNotNode) {
				query = left + " NOT " + right;
			} else if (node instanceof CQLOrNode) {
				query = left + " OR " + right;
			} else {
				throw new BadParameterException("ok�nd boolesk operation",
						"CQL2Solr.makeQuery", node.toString(), true);
			}
		} else if (node instanceof CQLTermNode) {
			CQLTermNode ctn = (CQLTermNode) node;

			String relation = ctn.getRelation().getBase();
			String index = translateIndexName(ctn.getIndex());

			if (!index.equals("")) {
				String term = ctn.getTerm();
				// result sets st�ds ej
				if (INDEX_CQL_RESULTSETID.equals(index)) {
					throw new DiagnosticException("Resultat sett st�ds ej",
							"CQL2Solr.makeQuery", "unsupported", false);
				}
				// anchoring (position av v�rde i indexerat f�lt) st�ds ej
				if (term.indexOf('^') != -1) {
					throw new DiagnosticException("ankartecken st�ds ej",
							"CQL2Solr.makeQuery", "unsupported", false);
				}
				// hantera virtuellt index
				if (ContentHelper.isSpatialVirtualIndex(index)) {
					query = createSpatialQuery(index, ctn);
				} else if (relation.equals("=") || relation.equals("scr")) {
					query = createTermQuery(index,term, relation);
				} else if (relation.equals("<")) {
					//term is upperbound, exclusive
					term = transformValueForField(index, term);
					query = index + ":{* TO " + term + "}";
				} else if (relation.equals(">")) {
					//term is lowerbound, exclusive
					term = transformValueForField(index, term);
					query = index + ":{" + term + " TO *}";
				} else if (relation.equals("<=")) {
					//term is upperbound, inclusive
					term = transformValueForField(index, term);
					query = index + ":[* TO " + term + "]";
				} else if (relation.equals(">=")) {
					//term is lowebound, inclusive
					term = transformValueForField(index, term);
					query = index + ":[" + term + " TO *]";
				} else if (relation.equals("<>")) {
					/**
					 * <> is an implicit NOT.
					 *
					 * For example the following statements are identical 
					 * results:
					 *   foo=bar and zoo<>xar
					 *   foo=bar not zoo=xar
					 */
/*
					if (leftQuery == null) {
						// first term in query create an empty Boolean query
						// to NOT
						query = new BooleanQuery();
					} else {
							query = new BooleanQuery();
							AndQuery((BooleanQuery)query, leftQuery);
					}
*/
					//create a term query for the term then NOT it to the
					// boolean query
					String termQuery = createTermQuery(index, term, relation);
					query = "NOT " + termQuery;
				} else if (relation.equals("any")) {
					//implicit or
					query = createTermQuery(index, term, relation);
				} else if (relation.equals("all")) {
					//implicit and
					query = createTermQuery(index, term, relation);
				} else if (relation.equals("exact")) {
					/**
					 * implicit and.  this query will only return accurate
					 * results for indexes that have been indexed using
					 * a non-tokenizing analyzer
					 */
					query = createTermQuery(index, term, relation);
				} else {
					//anything else is unsupported
					throw new DiagnosticException("relationen " + 
							ctn.getRelation().getBase() + " st�ds ej",
							"CQL2Solr.makeQuery",
							ctn.getRelation().getBase() + " st�ds ej", true);
				}
			}
		} else if (node instanceof CQLSortNode) {
			throw new DiagnosticException("sortering st�ds ej",
					"CQL2Solr.makeQuery", null, false);
		} else {
			throw new DiagnosticException("ok�nt fel uppstod",
					"CQL2Solr.makeQuery", "error: " + 47 + " - ok�nd CQL "
					+ "nod: " + ""+node+")", true);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Query : " + query);
		}
		return query;
	}

	/**
	 * Skapar en term-query utifr�n inskickade v�rden.
	 * 
	 * @param field index
	 * @param value v�rde
	 * @param relation relation
	 * @return lucene-query eller null
	 * @throws Exception
	 */
	public static String createTermQuery(String field, String value, String relation) throws DiagnosticException {
		String termQuery = null;

		// TODO: se �ver denna logik (och kommentarer mm) lite, k�nns som om den kan f�renklas
		//       en hel del nu n�r ingen analys av v�rden mm sker p� klientsidan

		/**
		 * check to see if there are any spaces.  If there are spaces each
		 * word must be broken into a single term search and then all queries
		 * must be combined using an and.
		 */
		// f�r ej analyserade f�lt vill vi till�ta mellanslag i termerna, 
		// typ "Stockholm 1:1"
		if (value.indexOf(" ") == -1 ||	!ContentHelper.isAnalyzedIndex(field)) {
			// inga mellanslag, skapa en term query eller wildcard query
			//Term term;
			if (value.indexOf("?") != -1 || value.indexOf("*")!= -1) {
				if (ContentHelper.isISO8601DateYearIndex(field) ||
						ContentHelper.isSpatialCoordinateIndex(field)) {
					// inget st�d f�r wildcards f�r dessa f�lt
					throw new DiagnosticException("Wildcardtecken st�ds ej" +
							" f�r index: " + field,
							"CQL2Solr.createTermQuery", null, true);
				}
				// g�r till gemener
				value = value.toLowerCase();
				//term = new Term(field, value);
				termQuery = createEscapedTermQuery(field, value, "exact".equals(relation));
			} else {
				// fixa ev till v�rdet beroende p� index
				value = transformValueForField(field, value);
				termQuery = createEscapedTermQuery(field, value, "exact".equals(relation));
			}
		} else {
			// space found, iterate through the terms to create a multiterm 
			//search
			if (relation == null || relation.equals("=") ||
					relation.equals("<>") || relation.equals("exact")) {
				termQuery = createEscapedTermQuery(field, value, "exact".equals(relation));
				if (relation != null && relation.equals("<>")) {
					termQuery = "NOT " + termQuery;
				}
				/**
				 * default is =, all terms must be next to eachother.
				 * <> uses = as its term query.
				 * exact is a phrase query
				 */
				/*
				if (value.indexOf("?") != -1 || value.indexOf("*")!=-1 ) {
					throw new DiagnosticException("Wildcard tecken st�ds ej" +
							" f�r detta query", "CQL2Solr.createTermQuery",
							"wildcard tecken st�ds ej:" + field, true);
				}
				PhraseQuery phraseQuery =
					(PhraseQuery) StaticMethods.analyseQuery(field,
						value);
				Term[] t = phraseQuery.getTerms();
				if (t == null || t.length == 0) 
				{
					if (!lenientOnStopwords) {
						throw new DiagnosticException("fel i query str�ng",
								"CQL2Solr.createTermQuery", "endast stopp " +
										"ord: " + field, true);
					} else {
						termQuery = NO_MATCH_DUMMY_QUERY;
					}
				} else {
					termQuery = phraseQuery;
				}
				*/
			} else if (relation.equals("any")) {
				/**
				 * any is an implicit OR
				 */
				
				StringTokenizer tokenizer = new StringTokenizer(value, " ");
				if (tokenizer.hasMoreTokens()) {
					String curValue = tokenizer.nextToken();
					termQuery = "(" + createTermQuery(field, curValue, relation);
					while (tokenizer.hasMoreTokens()) {
						curValue = tokenizer.nextToken();
						termQuery += " OR " + createTermQuery(field, curValue, relation);
					}
					termQuery += ")";
				}
			} else if (relation.equals("all")) {
				/**
				 * any is an implicit AND
				 */
				StringTokenizer tokenizer = new StringTokenizer(value, " ");
				if (tokenizer.hasMoreTokens()) {
					String curValue = tokenizer.nextToken();
					termQuery = "(" + createTermQuery(field, curValue, relation);
					while (tokenizer.hasMoreTokens()) {
						curValue = tokenizer.nextToken();
						termQuery += " AND " + createTermQuery(field, curValue, relation);
					}
					termQuery += ")";
				}
			} else {
				throw new DiagnosticException("relationen " + relation +
						" st�ds ej", "CQL2Solr.createTermQuery",
						"relationen " + relation + " st�ds ej f�r phrase" +
								" query", true);
			}

		}

		return termQuery;
	}

	/**
	 * Skapar en spatial-query f�r lucene f�r ett virtuellt spatialt index.
	 * Queryn blir olika beroende p� index.
	 * @param index indexnamn
	 * @param ctn termnod
	 * @return query
	 * @throws DiagnosticException vid problem
	 */
	private static String createSpatialQuery(String index, CQLTermNode ctn) 
		throws DiagnosticException {
		String query;
		String relation = ctn.getRelation().getBase();
		if (!"=".equals(relation)) {
			throw new DiagnosticException("kombinationen av relation och " +
					"index st�ds ej: " + index + " " + relation,
					"CQL2Solr.createSpatialQuery", null, true);
		}
		String term = ctn.getTerm();
		if (ContentHelper.IX_BOUNDING_BOX.equals(index)) {
			double[] coords = parseDoubleValues(term, 4);
			if (coords == null) {
				throw new DiagnosticException("Termen har ogiltligt format" +
						" f�r index eller relation: " + index + ": " + term,
						"CQL2Solr.createSpatialQuery", null, true);
			}
			String epsgIdent = getSingleModifier(ctn);
			epsgIdent = translateEPSGModifier(epsgIdent);
			coords = transformCoordsToWGS84(epsgIdent, coords);
			// g�r pss som locallucene, men utan p�tvingad fyrkant
			// (dvs vi till�ter rektanglar)
			query = ContentHelper.I_IX_LAT + ":[" + coords[1] + " TO " + coords[3] + "] AND " +
				ContentHelper.I_IX_LON + ":[" + coords[0] + " TO " + coords[2] + "]";
		} else if (ContentHelper.IX_POINT_DISTANCE.equals(index)) {
			throw new DiagnosticException("Funktionen/indexet st�ds ej fn",
					"CQL2Solr.createSpatialQueryssName", null, true);
			/*
			double[] coordsAndDist = parseDoubleValues(term, 3);
			if (coordsAndDist == null) 
			{
				throw new DiagnosticException("Termen har ogiltligt format" +
						" f�r index eller relation: " + index + ": " + term,
						"CQL2Solr.createSpatialQuery", null, true);
			}
			String epsgIdent = getSingleModifier(ctn);
			epsgIdent = translateEPSGModifier(epsgIdent);
			// h�mta ut punkten
			double[] point = { coordsAndDist[0], coordsAndDist[1] };
			// och h�mta distansen och konvertera den till miles f�r
			// locallucenes DistanceQuery
			double distInKm = coordsAndDist[2];
			double distInMiles =  distInKm * 0.621371;
			if (distInMiles < 1) 
			{
				// locallucene ger fel om distansv�rdet �r f�r litet s� vi
				// kontrollerar h�r f�rst
				throw new DiagnosticException("Termen har ogiltligt format" +
						" f�r index eller relation: avst�nd f�r litet",
						"CQL2Solr.createSpatialQuery", "distans " +
						distInMiles + " f�r liten", true);
			} else if (distInKm > 30) 
			{
				// locallucene cachar upp en massa data f�r punkt + distans
				// s� det h�r v�rdet
				// f�r definitivt inte vara f�r stort heller, d� riskerar vi
				// oom - 30km kanske �r ok?
				throw new DiagnosticException("Term har ogiltigt format f�r" +
						" index eller relation: avst�nd f�r stort",
						"CQL2Solr.createSpatialQuery", "distans " +
						distInKm + " f�r stor", true);
			}
			point = transformCoordsToWGS84(epsgIdent, point);
			query = new DistanceQuery(point[1], point[0], distInMiles,
					ContentHelper.I_IX_LAT,	ContentHelper.I_IX_LON,
					true).getQuery();
			*/
		} else {
			// Hmm, det h�r borde inte h�nda
			throw new DiagnosticException("index " + index + " st�ds ej",
					"CQL2Solr.createSpatialQuery", null, false);
		}
		return query;
	}


	/**
	 * �vers�tter indexnamn fr�n cql till de som lucene anv�nder internt.
	 * Ger default-indexnamn och strippar sams�kskontext. 
	 * 
	 * @param index index
	 * @return �versatt indexnamn
	 */
	public static String translateIndexName(String index) {
		// TODO: toLowerCase() ?
		String translatedIndex = index;
		// �vers�tt default-index till fritext
		if (INDEX_CQL_SERVERCHOICE.equals(index)) {
			translatedIndex = ContentHelper.IX_TEXT;
		} else if (index.indexOf('.') > 0 &&
				index.startsWith(ContentHelper.CONTEXT_SET_SAMSOK)) {
			// strippar samsok-contextet eftersom det �r default
			translatedIndex = index.substring(
					ContentHelper.CONTEXT_SET_SAMSOK.length() + 1);
		}
		return translatedIndex;
	}

	// "�vers�tter" ev v�rde beroende p� indextyp
	public static String transformValueForField(String field, String value)
		throws DiagnosticException {
		if (ContentHelper.isToLowerCaseIndex(field)) {
			value = value.toLowerCase();
		} else if (ContentHelper.isISO8601DateYearIndex(field)) {
			// TODO: beh�vs detta?
			// g�r om �r till f�r lucene tillfixad str�ng s� att intervall
			// mm st�ds
			try {
				value =	String.valueOf(Integer.parseInt(value));
			} catch (Exception e) {
				throw new DiagnosticException("Term har ogiltigt format f�r" +
						" index eller relation",
						"CQL2Solr.transformValueForField", "ogiltigt " +
								"format: " + field + ": " + value, true);
			}
		}
		return value;
	}

	// skapar en s�kterm av indexnamn, v�rde och info om det �r en exakt matchning som avses
	private static String createEscapedTermQuery(String indexName, String value, boolean isExact) {
		// TODO: verkar r�tt f�r de flesta fall, men st�mmer det f�r alla? flytta in term�vers�ttning
		//       och indexkontroll map wildcard mm hit ocks�?
		String termQuery;
		if (!isExact) {
			// OBS �r det ett wildcard med m�ste det alltid g�ras till gemener pga hur de hanteras i solr
			if (value.indexOf("?") != -1 || value.indexOf("*")!= -1 || ContentHelper.isToLowerCaseIndex(indexName)) {
				value = value.toLowerCase();
			}
			// g�r escape av hela v�rdet och sen "unescape" p� ev wildcards
			value = ClientUtils.escapeQueryChars(value).replace("\\*", "*").replace("\\?", "?");
			// s�tt ihop
			termQuery = indexName + ":" + value;
		} else {
			// s�tt ihop och anv�nd quotes f�r en exakt matchning
			termQuery = indexName + ":\"" + value + "\"";
		}
		return termQuery;
	}

	private static double[] parseDoubleValues(String value, int expected) {
		double[] values = null;
		StringTokenizer tok = new StringTokenizer(value, " ");
		if (tok.countTokens() == expected) {
			values = new double[expected];
			int i = 0;
			while (tok.hasMoreTokens()) {
				try {
					values[i] = Double.parseDouble(tok.nextToken());
					++i;
				} catch (Exception e) {
					values = null;
					break;
				}
			}
		}
		return values;
	}

	private static String getSingleModifier(CQLTermNode ctn)
		throws DiagnosticException {
		String singleModifier = null;
		List<Modifier> modifiers = ctn.getRelation().getModifiers();
		if (modifiers.size() > 1) {
			String diagData = modifiers.get(0).getType();
			for (int j = 1; j < modifiers.size(); ++j) {
				diagData += "/" + modifiers.get(j).getType();
			}
			throw new DiagnosticException("Kombinationen av relations " +
					"modifierare st�ds ej", "CQL2Solr.getSingleModifier",
					"kombinationen st�ds ej: " + diagData, true);
		} else if (modifiers.size() == 1) {
			singleModifier = modifiers.get(0).getType().toUpperCase();
		}
		return singleModifier;
	}

	private static double[] transformCoordsToWGS84(String fromCRS, double[] coords)
		throws DiagnosticException {
		double[] xformedCoords = coords;
		if (fromCRS != null && !GMLUtil.CRS_WGS84_4326.equals(fromCRS)) {
			if (coords == null || coords.length == 0) {
				throw new DiagnosticException("Term har ogiltigt format f�r" +
						" index eller relation: Inga kordinater",
						"CQL2Solr.transformCoordsToWGS84", null, false);
			}
			try {
				xformedCoords = GMLUtil.transformCRS(coords, fromCRS,
						GMLUtil.CRS_WGS84_4326);
			} catch (Exception e) {
				throw new DiagnosticException("Relations modifierare st�ds" +
						" ej", "CQL2Solr.transformCoordsToWGS84",
						"relationsmodifierare st�ds ej: " + fromCRS, true);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Transformerade koordinater fr�n " + fromCRS +
						" till WGS 84 (" + ArrayUtils.toString(coords) +
						" -> " + ArrayUtils.toString(xformedCoords) + ")");
			}
		}
		return xformedCoords;
	}

	private static String translateEPSGModifier(String epsgIdent) {
		if (epsgIdent != null) {
			// �vers�tt k�nda konstanter till epsg-koder
			if (ContentHelper.WGS84_4326.equalsIgnoreCase(epsgIdent)) {
				epsgIdent = GMLUtil.CRS_WGS84_4326;
			} else if (ContentHelper.SWEREF99_3006.equalsIgnoreCase(epsgIdent)) {
				epsgIdent = GMLUtil.CRS_SWEREF99_TM_3006;
			} else if (ContentHelper.RT90_3021.equalsIgnoreCase(epsgIdent)) {
				epsgIdent = GMLUtil.CRS_RT90_3021;
			}
		} else {
			// default �r SWEREF99 s� vi m�ste alltid transformera d�
			// koordinater lagras i wgs84
			epsgIdent = GMLUtil.CRS_SWEREF99_TM_3006;
		}
		return epsgIdent;
	}

	// bara f�r debug, dumpar cql-tr�det
	public static void dumpQueryTree(CQLNode node) {
		if (!logger.isDebugEnabled()) {
			return;
		}
		if (node instanceof CQLBooleanNode) {
			CQLBooleanNode cbn=(CQLBooleanNode)node;
			dumpQueryTree(cbn.left);
			if (node instanceof CQLAndNode) {
				logger.debug(" AND ");
			} else if (node instanceof CQLNotNode) {
				logger.debug(" NOT ");
			} else if (node instanceof CQLOrNode) {
				logger.debug(" OR ");
			} else {
				logger.debug(" UnknownBoolean("+cbn+") ");
			}
			dumpQueryTree(cbn.right);
		} else if (node instanceof CQLTermNode) {
			CQLTermNode ctn=(CQLTermNode)node;
			logger.debug("term(qualifier=\""+ctn.getIndex()+"\" relation=\""+
					ctn.getRelation().getBase()+"\" term=\""+ctn.getTerm()+
					"\")");
		} else {
			logger.debug("UnknownCQLNode("+node+")");
		}
	}
}