package org.freya.util;

import java.util.Comparator;

import org.freya.model.Annotation;

public class AnnotationSolrScoreComparator implements Comparator<Annotation> {
    public int compare(Annotation a1, Annotation a2) {
        // compare annotation scores
        return ((Float) a2.getFeatures().get(FreyaConstants.SCORE))
        		.compareTo((Float)(a1.getFeatures()).get(FreyaConstants.SCORE));
    }
}
