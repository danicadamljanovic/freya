package org.freya.model.learning;

import org.freya.model.DatatypePropertyValueIdentifier;

//import gate.clone.ql.model.ui.DatatypePropertyValueIdentifier;

public class Key {

	String text;
	Object ontologyElementIdentifier;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Object getOntologyElementIdentifier() {
		return ontologyElementIdentifier;
	}

	public void setOntologyElementIdentifier(Object ontologyElementIdentifier) {
		this.ontologyElementIdentifier = ontologyElementIdentifier;
	}

	public String toString() {
		StringBuffer s = new StringBuffer("Key:");
		s.append(text);
		if (ontologyElementIdentifier instanceof DatatypePropertyValueIdentifier) {
			s
					.append(((DatatypePropertyValueIdentifier) ontologyElementIdentifier)
							.getInstanceURIs());
			s
					.append(((DatatypePropertyValueIdentifier) ontologyElementIdentifier)
							.getPropertyUri());
			s
					.append(((DatatypePropertyValueIdentifier) ontologyElementIdentifier)
							.getPropertyValue());
		} else if (ontologyElementIdentifier!=null){
			s.append((ontologyElementIdentifier).toString());
		}
		return s.toString();
	}

}
