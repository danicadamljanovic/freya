/**
 * 
 */

package org.freya.analyser;

import java.util.List;

import org.freya.model.OntologyElement;
import org.freya.model.QueryElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * This transformer creates queries.
 * 
 * @author danica
 * 
 */
@Component
public class FormalQueryMaker {
    static org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory
                    .getLog(FormalQueryMaker.class);


    @Autowired private QueryCreator queryCreator;

    /**
     * generates sparql query
     * 
     * @param List<OntologyElement>
     * @return
     * @throws GateException
     */
    public QueryElement transform(List<List<OntologyElement>> ontologyElementsTable)
                    throws Exception {
        QueryElement formalQueryElement;
        try {
            formalQueryElement = queryCreator
                            .getQueryElementFromOntologyElements(ontologyElementsTable);
            formalQueryElement.setOntologyElements(ontologyElementsTable);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return formalQueryElement;
    }
}
