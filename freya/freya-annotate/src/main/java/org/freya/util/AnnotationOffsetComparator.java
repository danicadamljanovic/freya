package org.freya.util;

import java.util.Comparator;

import org.freya.model.Annotation;

public class AnnotationOffsetComparator implements Comparator<Annotation> {

    public int compare(Annotation a1, Annotation a2){
      int result;

      // compare start offsets
      result = a1.getStartOffset().compareTo(
          a2.getStartOffset());

      // if start offsets are equal compare end offsets
      if(result == 0) {
        result = a1.getEndOffset().compareTo(
            a2.getEndOffset());
      } // if

      return result;
    }
  
  
}
