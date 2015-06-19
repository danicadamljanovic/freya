package org.freya.util;

import java.util.Comparator;

import org.freya.model.Annotation;
import org.freya.model.OntologyElement;
import org.freya.model.SemanticConcept;

public class SemanticConceptComparator implements Comparator<SemanticConcept> {

	public int compare(SemanticConcept s1, SemanticConcept s2) {
		OntologyElement o1=s1.getOntologyElement();
		OntologyElement o2=s2.getOntologyElement();
		
		Annotation a1 = o1.getAnnotation();
		Annotation a2 = o2.getAnnotation();
		int result;

		// compare start offsets
		result = a1.getStartOffset().compareTo(a2.getStartOffset());

		// if start offsets are equal compare end offsets
		if (result == 0) {
			result = a1.getEndOffset().compareTo(a2.getEndOffset());
		} // if

		return result;
	}

}
