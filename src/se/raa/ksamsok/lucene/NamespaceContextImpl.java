package se.raa.ksamsok.lucene;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.PrefixResolverDefault;
import org.w3c.dom.Document;

/**
 * Hj�lpklass f�r uri-prefixhantering. Anv�nds bara f�r DC-data fn.
 */
public class NamespaceContextImpl implements NamespaceContext {

	private PrefixResolver resolver;

	NamespaceContextImpl(Document doc) {
		this.resolver = new PrefixResolverDefault(doc.getDocumentElement());
	}

	public String getNamespaceURI(String prefix) {
		return resolver.getNamespaceForPrefix(prefix);
	}

	public String getPrefix(String namespaceURI) {
		// EJ IMPLEMENTERAD (anv�nds dock ej i v�rt fall)
		return null;
	}

	@SuppressWarnings("unchecked")
	public Iterator getPrefixes(String namespaceURI) {
		// EJ IMPLEMENTERAD (anv�nds dock ej i v�rt fall)
		return null;
	}

}
