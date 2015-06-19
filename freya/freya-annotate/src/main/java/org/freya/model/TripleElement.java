package org.freya.model;


import java.io.Serializable;
import java.util.List;

/**
 * This class is holding tree elements of a triple in a natural order
 * 
 * @author danica
 * 
 */
public class TripleElement implements Serializable{

	private List<List<OntologyElement>> elements;

	public List<List<OntologyElement>> getElements() {
		return elements;
	}

	public void setElements(List<List<OntologyElement>> elements) {
		this.elements = elements;
	}

	public String toString() {
		StringBuffer buff = new StringBuffer("");
		buff.append("Triple: ");
		for (List<OntologyElement> col : getElements()) {
		  for (OntologyElement el:col){
			buff.append(el.getData());
			buff.append(" AND ");
		  }
			buff.append("-");
		}
		buff.append("\n");
		return buff.toString();
	}
}
