package org.freya.util;

import java.util.Comparator;
import org.freya.model.ui.Annotation;

public class AnnotationScoreComparator implements Comparator<Annotation> {
    public int compare(Annotation a1, Annotation a2) {
        // compare annotation scores
        return a2.getScore().compareTo(a1.getScore());
    }
}
