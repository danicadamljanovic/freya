package org.freya.dialogue;

import info.aduna.iteration.CloseableIteration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.rdf.SesameRepositoryManager;
import org.freya.util.QueryUtil;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SuggestionsHelper {
    private static final Log logger = LogFactory.getLog(SuggestionsHelper.class);

    @Qualifier("rdfRepository") @Autowired SesameRepositoryManager repository;

    @Autowired QueryUtil queryUtil;

    public String getDatatypePropertiesNoDomain() throws Exception {

        String query = queryUtil.readQueryContent("/queries/getDatatypePropertiesNoDomain.sparql");
        // String query = getDatatypePropertiesNoDomain;
        logger.debug("calling getDatatypePropertiesNoDomain");
        long startTime = System.currentTimeMillis();
        CloseableIteration<BindingSet, QueryEvaluationException> sparqlResult = repository.executeQuery(query);
        long endTime = System.currentTimeMillis();

        startTime = System.currentTimeMillis();
        String result = queryUtil.getStringFromCloseableIteration(sparqlResult);
        endTime = System.currentTimeMillis();
        logger.debug("RESULT IS___________________\n" + result);
        return result;
    }
}
