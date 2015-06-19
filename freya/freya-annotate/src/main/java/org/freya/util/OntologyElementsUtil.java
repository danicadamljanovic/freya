package org.freya.util;

import java.util.ArrayList;
import java.util.List;

import org.freya.model.OntologyElement;
import org.freya.model.SemanticConcept;
public class OntologyElementsUtil {
    /**
     * @param list
     * @return
     */
    public static String beautifyListOfOntologyElements(List<OntologyElement> list) {
        StringBuffer buff = new StringBuffer();
        for (OntologyElement el : list) {
            buff.append(el.getClass().getSimpleName()).append(":").append(el.getData());

        }
        if (list != null && list.size() > 0) {
            buff.append("(").append(list.get(0).getAnnotation().getText()).append(",")
                            .append(list.get(0).getAnnotation().getStartOffset()).append(",")
                            .append(list.get(0).getAnnotation().getEndOffset()).append(")");
        }
        return buff.toString();
    }

    /**
     * utility method takes ontology elements from semantic concepts and generates a list
     * 
     * @param candidates
     * @return
     */
    public static List<OntologyElement> fromSemanticConceptsToOntologyElements(
                    List<SemanticConcept> candidates) {
        List list = new ArrayList();
        for (SemanticConcept c : candidates) {
            list.add(c.getOntologyElement());
        }
        return list;
    }
}
