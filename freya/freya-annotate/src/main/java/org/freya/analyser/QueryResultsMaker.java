/**
 * 
 */
package org.freya.analyser;

import java.util.ArrayList;
import java.util.List;

import org.freya.model.FormalQueryResult;
import org.freya.model.OntologyElement;
import org.freya.model.PropertyElement;
import org.freya.model.QueryElement;
import org.freya.model.Question;
import org.freya.util.FreyaConstants;
import org.freya.util.QueryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.org.apache.bcel.internal.generic.Type;

/**
 * @author danica
 */
@Component
public class QueryResultsMaker {
    static org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory
                    .getLog(QueryResultsMaker.class);

    @Autowired QueryUtil queryUtil;


    /**
     * @param queryElement
     * @return
     * @throws GateException
     */
    public FormalQueryResult transform(QueryElement queryElement, Question question)
                    throws Exception {
        List<String> uris = queryElement.getUris();
        String queryString = queryElement.getQueryString();
        logger.info("SPARQL:\n" + queryString);
        List<List<String>> list2DOfResults = null;
        try {
            list2DOfResults = queryUtil.get2DList(queryString);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        FormalQueryResult qrElement = new FormalQueryResult();
        qrElement.setQuery(queryString);
        qrElement.setData(list2DOfResults);
        qrElement.setUris(uris);

        String preciseSparqlQuery = null;
        boolean preciseAnswerKnown = false;
        String answerVariable = null;
        // answerVariable=queryElement.getOntologyElements().get(0).get(0).getVariable();
        // answerVariable=question.getAnswerType().get(0).getVariable();

        String answerVariableToOveride = null;
        List<List<OntologyElement>> elements = queryElement.getOntologyElements();
        if (question.getAnswerType() != null && question.getAnswerType().size() > 0)
            answerVariable = question.getAnswerType().get(0).getVariable();
        logger.info("Answer variable is:" + answerVariable);
        // for(List<OntologyElement> elColumn : elements) {
        // this will override initially assigned answer type in case of properties
        // if(elColumn.get(0).isAnswer()) {
        // answerVariableToOveride = elColumn.get(0).getVariable();
        // logger.info("Answer type to be modified to variable:"
        // + answerVariableToOveride);
        // } else
        // if {
        // ako je answer type property
        // if answer type is property then take its domain or range
        List<OntologyElement> aType = question.getAnswerType();
        if (aType != null && aType.size() > 0 && aType.get(0) instanceof PropertyElement) {
            // take the element before it as atype
            for (int i = 0; i < elements.size(); i++) {
                List<OntologyElement> col = elements.get(i);

                if (col.get(0).getAnnotation().getStartOffset() != null
                                && (col.get(0)).getAnnotation().equals(
                                                question.getAnswerType().get(0).getAnnotation())) {
                    logger.info("This is property and we now change the answer type to its precendent.");
                    // find the index of this column
                    int propIndex = elements.indexOf(col);
                    logger.info("propIndex " + propIndex + " col:" + col);
                    logger.info("elements: " + elements.toString());
                    // find the index of this column
                    int aTypeIndex = propIndex - 1;
                    if (aTypeIndex > -1) {
                        List<OntologyElement> newAt = new ArrayList<OntologyElement>();
                        List<OntologyElement> thoseToBeAnswerType = elements.get(aTypeIndex);
                        for (OntologyElement c : thoseToBeAnswerType) {
                            c.setMainSubject(true);
                            newAt.add(c);
                            answerVariableToOveride = c.getVariable();
                            logger.info("BINGOOOOOOOOOOOOOOOOOOOOOOOOOO! "
                                            + answerVariableToOveride);
                        }
                        for (OntologyElement c : col) {
                            c.setMainSubject(false);

                        }

                        question.setAnswerType(newAt);
                    }
                    // reduce it for 1
                    break;
                    // that becomes at
                }
            }
        }
        // }
        // }



        if (answerVariableToOveride != null) {
            answerVariable = answerVariableToOveride;
        }

        int endIndex = 0;
        if (queryString != null) endIndex = queryString.indexOf("where");
        if (answerVariable != null) preciseAnswerKnown = true;
        Type type = question.getType();
        if (type.equals(Type.BOOLEAN)) {
            // precise sparql will start with ASK
            //logger.debug("Before replacing:" + queryString);
            int beginIndex = queryString.indexOf(FreyaConstants.SELECT);
            String oldChar = queryString.substring(beginIndex, endIndex);
            preciseSparqlQuery = queryString.replace(oldChar, " ASK ");
            preciseSparqlQuery =
                            preciseSparqlQuery.substring(0, preciseSparqlQuery.indexOf("LIMIT"));
            //logger.debug("After replacing:" + preciseSparqlQuery);
        } else if (preciseAnswerKnown && type.equals(Type.UNKNOWN)) {
            // find Select
            //logger.debug("Before replacing:" + queryString);
            int beginIndexSum=queryString.indexOf(FreyaConstants.SELECT_SUM);
            if(beginIndexSum>-1) preciseSparqlQuery = null;
            else
            {
	            int beginIndex =
	                            queryString.indexOf(FreyaConstants.SELECT)
	                                            + FreyaConstants.SELECT.length();
	            String oldChar = queryString.substring(beginIndex, endIndex);
	            preciseSparqlQuery = queryString.replace(oldChar, " ?" + answerVariable + " ");
	            //logger.debug("After replacing:" + preciseSparqlQuery);
            }
        } else if (preciseAnswerKnown && type.equals(Type.LONG)) {
            // find Select
            //logger.debug("Before replacing:" + queryString);
            int beginIndex = queryString.indexOf(FreyaConstants.SELECT);
            String oldChar = queryString.substring(beginIndex, endIndex);
            preciseSparqlQuery =
                            queryString.replace(oldChar, " SELECT COUNT(?" + answerVariable + ") ");

            preciseSparqlQuery =
                            preciseSparqlQuery.substring(0, preciseSparqlQuery.indexOf("LIMIT"));

            //logger.debug("After replacing:" + preciseSparqlQuery);
        }
        if (preciseSparqlQuery == null) preciseSparqlQuery = queryString;
        qrElement.setPreciseSparqlQuery(preciseSparqlQuery);
        return qrElement;
    }
}
