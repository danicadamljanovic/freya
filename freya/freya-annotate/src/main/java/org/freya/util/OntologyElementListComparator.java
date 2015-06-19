package org.freya.util;

import java.util.Comparator;
import java.util.List;

import org.freya.model.Annotation;
import org.freya.model.OntologyElement;

public class OntologyElementListComparator implements Comparator<List<OntologyElement>> {

    public int compare(List<OntologyElement> o1, List<OntologyElement> o2) {

        Annotation a1 = o1.get(0).getAnnotation();
        Annotation a2 = o2.get(0).getAnnotation();
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
