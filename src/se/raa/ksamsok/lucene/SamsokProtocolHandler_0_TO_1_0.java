package se.raa.ksamsok.lucene;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jrdf.graph.Graph;
import org.jrdf.graph.SubjectNode;
import static se.raa.ksamsok.lucene.SamsokProtocol.*;
import static se.raa.ksamsok.lucene.ContentHelper.*;

public class SamsokProtocolHandler_0_TO_1_0 extends BaseSamsokProtocolHandler {

	private static final Logger classLogger = getClassLogger();

	// map med uri -> v�rde f�r indexering
	private static final Map<String,String> uriValues_0_TO_1_0;
	// bass�kv�g
	static final String PATH = "/" + SamsokContentHelper.class.getPackage().getName().replace('.', '/') + "/";
	// reationsmap
	static final Map<String, URI> relationsMap_0_TO_1_0;

	static {
		Map<String,String> values = new HashMap<String,String>();
		// l�s in uri-v�rden f�r uppslagning
		RDFUtil.readURIValueResource(PATH + "entitytype_0_TO_1.0.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "subject.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "dataquality.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "contexttype_0_TO_1.0.rdf", SamsokProtocol.uri_rContextLabel, values);

		uriValues_0_TO_1_0 = Collections.unmodifiableMap(values);

		Map<String, URI> relMap = new HashMap<String, URI>();
		relMap.put(IX_CONTAINSINFORMATIONABOUT, uri_rContainsInformationAbout);
		// h�mta ut containsObject (0n)
		relMap.put(IX_CONTAINSOBJECT, uri_rContainsObject);
		// h�mta ut hasBeenUsedIn (0n)
		relMap.put(IX_HASBEENUSEDIN, uri_rHasBeenUsedIn);
		// h�mta ut hasChild (0n)
		relMap.put(IX_HASCHILD, uri_rHasChild);
		// h�mta ut hasFind (0n)
		relMap.put(IX_HASFIND, uri_rHasFind);
		// h�mta ut hasImage (0n)
		relMap.put(IX_HASIMAGE, uri_rHasImage);
		// h�mta ut hasObjectExample (0n)
		relMap.put(IX_HASOBJECTEXAMPLE, uri_rHasObjectExample);
		// h�mta ut hasParent (0n)
		relMap.put(IX_HASPARENT, uri_rHasParent);
		// h�mta ut hasPart (0n)
		relMap.put(IX_HASPART, uri_rHasPart);
		// h�mta ut isDescribedBy (0n)
		relMap.put(IX_ISDESCRIBEDBY, uri_rIsDescribedBy);
		// h�mta ut isFoundIn (0n)
		relMap.put(IX_ISFOUNDIN, uri_rIsFoundIn);
		// h�mta ut isPartOf (0n)
		relMap.put(IX_ISPARTOF, uri_rIsPartOf);
		// h�mta ut isRelatedTo (0n)
		relMap.put(IX_ISRELATEDTO, uri_rIsRelatedTo);
		// h�mta ut isVisualizedBy (0n)
		relMap.put(IX_ISVISUALIZEDBY, uri_rIsVisualizedBy);
		// h�mta ut sameAs (0n)
		relMap.put(IX_SAMEAS, uri_rSameAs);
		// h�mta ut visualizes (0n)
		relMap.put(IX_VISUALIZES, uri_rVisualizes);

		relationsMap_0_TO_1_0 = Collections.unmodifiableMap(relMap);
	}

	protected SamsokProtocolHandler_0_TO_1_0(Graph graph, SubjectNode s) {
		super(graph, s);
	}

	@Override
	protected Map<String, String> getURIValues() {
		return uriValues_0_TO_1_0;
	}

	@Override
	public String getRelationTypeNameFromURI(String refUri) {
		// specialhantering av relationer
		String relationType;
		if (uri_rSameAs.toString().equals(refUri)) {
			relationType = ContentHelper.IX_SAMEAS;
		} else {
			relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, uriPrefixKSamsok));
		}
		return relationType;
	}

	@Override
	protected Map<String, URI> getTopLevelRelationsMap() {
		return relationsMap_0_TO_1_0;
	}

	@Override
	public Logger getLogger() {
		return classLogger;
	}

}
