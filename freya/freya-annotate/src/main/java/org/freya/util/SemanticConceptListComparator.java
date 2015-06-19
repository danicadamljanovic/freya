package org.freya.util;

import java.util.Comparator;
import java.util.List;

import org.freya.model.Annotation;
import org.freya.model.OntologyElement;
import org.freya.model.SemanticConcept;

public class SemanticConceptListComparator implements Comparator<List<SemanticConcept>> {

	public int compare(List<SemanticConcept> s1, List<SemanticConcept> s2) {
		OntologyElement o1=s1.get(0).getOntologyElement();
		OntologyElement o2=s2.get(0).getOntologyElement();
		
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


