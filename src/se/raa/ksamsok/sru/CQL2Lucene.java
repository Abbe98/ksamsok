package se.raa.ksamsok.sru;

import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLSortNode;
import org.z3950.zing.cql.CQLTermNode;

import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Kod mer eller mindre kopierad fr�n LuceneTranslator fr�n 
 * srwlucene 1.0 (http://wiki.osuosl.org/display/OCKPub/SRWLucene)
 * vilken �r under apache 2.0-licens. Projektet ej uppdaterat p�
 * 2 �r och har beroenden p� ett projekt (SRW/U 2.0,
 * http://www.oclc.org/research/software/srw/default.htm)
 * som ej heller �r uppdaterat p� 2 �r och till vilket det dessutom
 * inte g�r att n� k�llkoden till pga trasig webbplats.
 * 
 * Omskrivet till stor del f�r att hantera uppdaterad lucene, andra s�tt att g�ra
 * queries och indexmanipulering samt v�rdenormalisering mm.
 * 
 */
public class CQL2Lucene {

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.sru.CQL2Lucene");

	private static final String INDEX_CQL_SERVERCHOICE = "cql.serverChoice";
	private static final String INDEX_CQL_RESULTSETID = "cql.resultSetId";

	/**
	 * Skapar en lucene-query utifr�n en CQL-nod.
	 * 
	 * @param node cql-nod
	 * @return lucene-query eller null
	 * @throws Exception
	 */
	static Query makeQuery(CQLNode node) throws Exception {
		return makeQuery(node, null);
	}

	/**
	 * Skapar en lucene-query utifr�n en CQL-nod och ett v�nsterled.
	 * 
	 * @param node cql-nod
	 * @param leftQuery v�nsterled i form av en lucene-query eller null
	 * @return en lucene-query eller null
	 * @throws Exception
	 */
	static Query makeQuery(CQLNode node, Query leftQuery) throws Exception {
		Query query = null;

		if(node instanceof CQLBooleanNode) {
			CQLBooleanNode cbn=(CQLBooleanNode)node;

			Query left = makeQuery(cbn.left);
			Query right = makeQuery(cbn.right, left);

			if(node instanceof CQLAndNode) {
				if (left instanceof BooleanQuery) {
					query = left;
					if (logger.isDebugEnabled()) {
						logger.debug("  Anding left and right");
					}
					AndQuery((BooleanQuery) left, right);
				} else {
					query = new BooleanQuery();
					if (logger.isDebugEnabled()) {
						logger.debug("  Anding left and right in new query");
					}
					AndQuery((BooleanQuery) query, left);
					AndQuery((BooleanQuery) query, right);
				}

			} else if(node instanceof CQLNotNode) {

				if (left instanceof BooleanQuery) {
					if (logger.isDebugEnabled()) {
						logger.debug("  Notting left and right");
					}
					query = left;
					NotQuery((BooleanQuery) left, right);
				} else {
					query = new BooleanQuery();
					if (logger.isDebugEnabled()) {
						logger.debug("  Notting left and right in new query");
					}
					AndQuery((BooleanQuery) query, left);
					NotQuery((BooleanQuery) query, right);
				}

			} else if(node instanceof CQLOrNode) {
				if (left instanceof BooleanQuery) {
					if (logger.isDebugEnabled()) {
						logger.debug("  Or'ing left and right");
					}
					query = left;
					OrQuery((BooleanQuery) left, right);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("  Or'ing left and right in new query");
					}
					query = new BooleanQuery();
					OrQuery((BooleanQuery) query, left);
					OrQuery((BooleanQuery) query, right);
				}
			} else {
				throw new RuntimeException("Unknown boolean");
			}

		} else if(node instanceof CQLTermNode) {
			CQLTermNode ctn=(CQLTermNode)node;

			String relation = ctn.getRelation().getBase();
			String index = translateIndexName(ctn.getIndex()); //getQualifier();

			if (!index.equals("")) {
				String term = ctn.getTerm();
				if(relation.equals("=") || relation.equals("scr")) {
					query = createTermQuery(index,term, relation);
				} else if (relation.equals("<")) {
					//term is upperbound, exclusive
					term = transformValueForField(index, term);
					// csrq ist�llet f�r en range query d� den inte ger TooManyClauses
					// TODO: beh�ver vi anv�nda cache-wrapper (CachingWrapperFilter) f�r dessa?
					query = new ConstantScoreRangeQuery(index, null, term, false, false);
				} else if (relation.equals(">")) {
					//term is lowerbound, exclusive
					term = transformValueForField(index, term);
					query = new ConstantScoreRangeQuery(index, term, null, false, false);
				} else if (relation.equals("<=")) {
					//term is upperbound, inclusive
					term = transformValueForField(index, term);
					query = new ConstantScoreRangeQuery(index, null, term, false, true);
				} else if (relation.equals(">=")) {
					//term is lowebound, inclusive
					term = transformValueForField(index, term);
					query = new ConstantScoreRangeQuery(index, term, null, true, false);
				} else if (relation.equals("<>")) {
					/**
					 * <> is an implicit NOT.
					 *
					 * For example the following statements are identical results:
					 *   foo=bar and zoo<>xar
					 *   foo=bar not zoo=xar
					 */

					if (leftQuery == null) {
						// first term in query create an empty Boolean query to NOT
						query = new BooleanQuery();
					} else {
						if (leftQuery instanceof BooleanQuery) {
							// left query is already a BooleanQuery use it
							query = leftQuery;
						} else {
							// left query was not a boolean, create a boolean query
							// and AND the left query to it
							query = new BooleanQuery();
							AndQuery((BooleanQuery)query, leftQuery);
						}
					}
					//create a term query for the term then NOT it to the boolean query
					Query termQuery = createTermQuery(index,term, relation);
					NotQuery((BooleanQuery) query, termQuery);

				} else if (relation.equals("any")) {
					//implicit or
					query = createTermQuery(index,term, relation);

				} else if (relation.equals("all")) {
					//implicit and
					query = createTermQuery(index,term, relation);
				} else if (relation.equals("exact")) {
					/**
					 * implicit and.  this query will only return accurate
					 * results for indexes that have been indexed using
					 * a non-tokenizing analyzer
					 */
					query = createTermQuery(index,term, relation);
				} else {
					//anything else is unsupported
					throw new DiagnosticsException(19, "Unsupported relation", ctn.getRelation().getBase());
				}

			}
		} else if(node instanceof CQLSortNode) {
			/* TODO: sortering, kr�ver att man kan l�mna ifr�n sig en lucene-Sort-instans
					vilket g�r att denna metod skulle beh�va tv� returv�rden -> refaktorering
			CQLSortNode csn = (CQLSortNode) node;
			for (ModifierSet mf: csn.getSortIndexes()) {
				log("sort: " + mf.getBase() + " " + mf.getModifiers());
			}
			*/
			throw new DiagnosticsException(80, "Sort not supported");
		} else {
			throw new Exception("code: " + 47 + " - UnknownCQLNode: "+node+")");
		}
		if (query != null && logger.isDebugEnabled()) {
			// f�r att klara de specialstr�ngar som kr�vs f�r ISO-datum
			// utan att skriva ut l�ga kontrolltecken men �nd� n�n info per tecken
			// kontrolltecknen kan tex ge h�gtalarpip p� windows
			String q = query.toString();
			StringBuffer b = new StringBuffer(q.length());
			for (char c: q.toCharArray()) {
				if (c < 32) {
					b.append('#');
				} else {
					b.append(c);
				}
			}
			logger.debug("Query : " + b);
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
	public static Query createTermQuery(String field, String value, String relation) throws Exception {

		Query termQuery = null;

		// result sets st�ds ej
		if (INDEX_CQL_RESULTSETID.equals(field)) {
			throw new DiagnosticsException(50, "Result sets not supported");
		}

		/**
		 * check to see if there are any spaces.  If there are spaces each
		 * word must be broken into a single term search and then all queries
		 * must be combined using an and.
		 */
		// f�r ej analyserade f�lt vill vi till�ta mellanslag i termerna, typ "Stockholm 1:1"
		if (value.indexOf(" ") == -1 || !ContentHelper.isAnalyzedIndex(field)) {
			// no space found, just create a single term search
			//todo case insensitivity?
			Term term;
			if (value.indexOf("?") != -1 || value.indexOf("*")!=-1 ){
				if (ContentHelper.isToLowerCaseIndex(field)) {
					// g�r till gemener
					value = value.toLowerCase();
				} else if (ContentHelper.isISO8601DateYearIndex(field)) {
					// inget st�d f�r wildcards f�r dessa f�lt
					throw new DiagnosticsException(28, "Masking character not supported for index", field);
				}
				term = new Term(field, value);
				termQuery = new WildcardQuery(term);
			} else {
				// fixa ev till v�rdet beroende p� index
				value = transformValueForField(field, value);
				term = new Term(field, value);
				termQuery = new TermQuery(term);
			}
		} else {
			// space found, iterate through the terms to create a multiterm search

			if (relation == null || relation.equals("=") || relation.equals("<>") || relation.equals("exact")) {
				/**
				 * default is =, all terms must be next to eachother.
				 * <> uses = as its term query.
				 * exact is a phrase query
				 */
				PhraseQuery phraseQuery = new PhraseQuery();
				StringTokenizer tokenizer = new StringTokenizer(value, " ");
				// anv�nd svensk stamning, samma som f�r indexeringen
				// beroende p� f�lt/index om vi ska stamma eller ej
				boolean isAnalyzedField = ContentHelper.isAnalyzedIndex(field);
				boolean isLowerCaseField = ContentHelper.isToLowerCaseIndex(field);
				boolean isISO8601DateYearField = ContentHelper.isISO8601DateYearIndex(field);
				while (tokenizer.hasMoreTokens()) {
					String curValue = tokenizer.nextToken();
					if (isAnalyzedField) {
						// analysera s�kv�rdet pss som vid indexering
						if (curValue.indexOf("?") < 0 && curValue.indexOf("*") < 0 ) {
							// TODO: hur hantera wildcards i en s�n h�r query?
							curValue = analyzeIndexText(curValue);
						}
					} else if (isLowerCaseField) {
						// g�r till gemener
						curValue = curValue.toLowerCase();
					} else if (isISO8601DateYearField) {
						// g�r om �r till f�r lucene tillfixad str�ng s� att interval mm st�ds
						try {
							curValue = ContentHelper.transformNumberToLuceneString(parseYear(curValue));
						} catch (Exception e) {
							throw new DiagnosticsException(36,
									"Term in invalid format for index or relation",
									field + ": " + curValue);
						}
					}
					// ta bara med termen om det ej �r ett stopp-ord
					if (curValue != null) {
						phraseQuery.add(new Term(field, curValue));
					}
				}
				Term[] t = phraseQuery.getTerms();
				if (t == null || t.length == 0) {
					throw new DiagnosticsException(35, "Term contains only stopwords", value);
				}
				termQuery = phraseQuery;

			} else if(relation.equals("any")) {
				/**
				 * any is an implicit OR
				 */
				termQuery = new BooleanQuery();
				StringTokenizer tokenizer = new StringTokenizer(value, " ");
				while (tokenizer.hasMoreTokens()) {
					String curValue = tokenizer.nextToken();
					Query subSubQuery = createTermQuery(field, curValue, relation);
					OrQuery((BooleanQuery) termQuery, subSubQuery);
				}

			} else if (relation.equals("all")) {
				/**
				 * any is an implicit AND
				 */
				termQuery = new BooleanQuery();
				StringTokenizer tokenizer = new StringTokenizer(value, " ");
				while (tokenizer.hasMoreTokens()) {
					String curValue = tokenizer.nextToken();
					Query subSubQuery = createTermQuery(field, curValue, relation);
					AndQuery((BooleanQuery) termQuery, subSubQuery);
				}
			} // TODO: else?

		}

		return termQuery;
	}

	/**
	 * Join the two queries together with boolean AND
	 * @param query
	 * @param query2
	 */
	public static void AndQuery(BooleanQuery query, Query query2) {
		/**
		 * required = true (must match sub query)
		 * prohibited = false (does not need to NOT match sub query)
		 */
		query.add(query2, BooleanClause.Occur.MUST);
	}

	public static void OrQuery(BooleanQuery query, Query query2) {
		/**
		 * required = false (does not need to match sub query)
		 * prohibited = false (does not need to NOT match sub query)
		 */
		query.add(query2, BooleanClause.Occur.SHOULD);
	}

	public static void NotQuery(BooleanQuery query, Query query2) {
		/**
		 * required = false (does not need to match sub query)
		 * prohibited = true (must not match sub query)
		 */
		query.add(query2, BooleanClause.Occur.MUST_NOT);
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
		} else if (index.indexOf('.') > 0 && index.startsWith(ContentHelper.CONTEXT_SET_SAMSOK)) {
			// strippar samsok-contextet eftersom det �r default
			translatedIndex = index.substring(ContentHelper.CONTEXT_SET_SAMSOK.length() + 1);
		}
		return translatedIndex;
	}

	// analyserar (stammar) inskickad text med svensk analyzer
	private static String analyzeIndexText(String text) {
		Analyzer a = ContentHelper.getSwedishAnalyzer();
		TokenStream ts = null;
		String retText = text;
		try {
			ts = a.tokenStream(null, new StringReader(text));
			Token t = new Token();
			t = ts.next(t);
			if (t != null) {
				retText = t.term();
				if ((t = ts.next(t)) != null) {
					logger.warn("Fick (minst) 2 tokens i tokenstr�mmen '" + text + "' vid analys");
					// s�tt tillbaka
					retText = text;
				}
			} else {
				retText = null;
			}
		} catch (Exception e) {
			logger.warn("Fel vid analys av index-text", e);
		} finally {
			if (ts != null) {
				try {
					ts.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.warn("Fel vid st�ngning av tokenstr�m vid analys av index-text", e);
				}
			}
		}
		return retText;
	}

	// "�vers�tter" ev v�rde beroende p� indextyp
	private static String transformValueForField(String field, String value) throws DiagnosticsException {
		// anv�nd svensk stamning, samma som f�r indexeringen
		// beroende p� f�lt/index om vi ska stamma eller ej
		if (ContentHelper.isAnalyzedIndex(field)) {
			String analyzedValue = analyzeIndexText(value);
			if (analyzedValue == null) {
				throw new DiagnosticsException(35, "Term contains only stopwords", value);
			}
			value = analyzedValue;
		} else if (ContentHelper.isToLowerCaseIndex(field)) {
			value = value.toLowerCase();
		} else if (ContentHelper.isISO8601DateYearIndex(field)) {
			// g�r om �r till f�r lucene tillfixad str�ng s� att interval mm st�ds
			try {
				value = ContentHelper.transformNumberToLuceneString(parseYear(value));
			} catch (Exception e) {
				throw new DiagnosticsException(36,
						"Term in invalid format for index or relation",
						field + ": " + value);
			}
		}
		return value;
	}

	private static int parseYear(String value) {
		return Integer.parseInt(value);
	}

	// bara f�r debug, dumpar cql-tr�det
	static void dumpQueryTree(CQLNode node) {
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
			logger.debug("term(qualifier=\""+ctn.getIndex() /*getQualifier()*/+"\" relation=\""+
					ctn.getRelation().getBase()+"\" term=\""+ctn.getTerm()+"\")");
		} else {
			logger.debug("UnknownCQLNode("+node+")");
		}
	}

}
