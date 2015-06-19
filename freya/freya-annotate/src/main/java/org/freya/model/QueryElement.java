/**
 * 
 */
package org.freya.model;

//import gate.clone.ql.model.InterpretationElement;

import java.util.ArrayList;
import java.util.List;

/**
 * this class carries formal query related data...
 * 
 * @author danica
 * 
 */
public class QueryElement {

List<List<OntologyElement>> ontologyElements;

	

	public List<List<OntologyElement>> getOntologyElements() {
  return ontologyElements;
}

public void setOntologyElements(List<List<OntologyElement>> ontologyElements) {
  this.ontologyElements = ontologyElements;
}

  List<String> uris = new ArrayList<String>();

	/* query in formal language */
	String queryString;
	/*
	 * modifiers that could not be included into the queryString itself, as not
	 * supported by formal languages or there is need to perform some
	 * calculations or both; most common modifiers are instances of
	 * KeyPhraseElement class
	 */
	List<InterpretationElement> modifiers;

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new StringBuffer(this.getClass().getSimpleName().toString())
				.append("\nQuery: ").append(this.queryString)
				// .append(" Modifiers: ")
				// .append(this.modifiers.toString())
				.append("\n").toString();
	}

	/**
	 * @return the modifiers
	 */
	public List<InterpretationElement> getModifiers() {
		return modifiers;
	}

	/**
	 * @param modifiers
	 *            the modifiers to set
	 */
	public void setModifiers(List<InterpretationElement> modifiers) {
		this.modifiers = modifiers;
	}

	/**
	 * @return the queryString
	 */
	public String getQueryString() {
		return queryString;
	}

	/**
	 * @param queryString
	 *            the queryString to set
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * @return the uris
	 */
	public List<String> getUris() {
		return uris;
	}

	/**
	 * @param uris
	 *            the uris to set
	 */
	public void setUris(List<String> uris) {
		this.uris = uris;
	}
}