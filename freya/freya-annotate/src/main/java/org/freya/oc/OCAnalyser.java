package org.freya.oc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.analyser.BacktrackEngine;
import org.freya.analyser.FormalQueryMaker;
import org.freya.analyser.QueryResultsMaker;
import org.freya.analyser.TripleCreator;
import org.freya.model.FormalQueryResult;
import org.freya.model.NoneElement;
import org.freya.model.OntologyElement;
import org.freya.model.QueryElement;
import org.freya.model.Question;
import org.freya.model.ResultGraph;
import org.freya.model.SemanticConcept;
import org.freya.model.TripleElement;
import org.freya.rdf.SesameRepositoryManager;
import org.freya.util.FreyaConstants;
import org.freya.util.OntologyElementListComparator;
import org.freya.util.QueryUtil;
import org.freya.util.SemanticConceptListComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * this class is used to combine OCs into triples and generating sparql from triples
 * 
 * @todo investigate KeyPhraseElements: OR and AND, now ignored; probably easy to do with stanford
 * @author danica
 */
@Component
public class OCAnalyser {

    protected final Log logger = LogFactory.getLog(getClass());
    /*
     * whether the sparql results should be postprocessed or not if yes then we will show the max(R(i)), where i=1..n, n
     * is the number of returned results (rows)
     */
    boolean filterResultsBasedOnTheFunction = true;

    @Autowired QueryUtil queryUtil;
    @Autowired OCUtil ocUtil;
    @Autowired TripleCreator tripleCreator;
    @Autowired FormalQueryMaker queryMaker;
    @Autowired QueryResultsMaker queryResultsMaker;
    @Qualifier("rdfRepository") @Autowired SesameRepositoryManager repository;

    /**
     * @param elements
     * @return
     */
    private List<TripleElement> getTriples(List<List<OntologyElement>> elements) {
        List<TripleElement> triples = null;
        // run tripleCreator here
        try {
            triples = tripleCreator.getTriples(elements);
        } catch (Exception e) {
            logger.error(e);
        }
        // interpretation = QueryUtil
        // .attachNiceDescriptionsToInterpretations(interpretation);
        // triples = interpretation.getTriples();
        return triples;
    }

    /**
     * @TODO ovde input parameters treba da budu konkretniji
     * @param map
     * @return
     */
    public Map getResultGraph(HashMap map) {

        HashMap toReturn = new HashMap();

        List<List<OntologyElement>> preparedOntologyElements =
                        (List<List<OntologyElement>>) map.get("elements");

        List<List<String>> list2DOfResults = (List<List<String>>) map.get("table");
        String preciseSparql = (String) map.get("preciseSparql");
        // ovde ga promeni

        List<ResultGraph> resultGraph = null;
        try {
            List<TripleElement> triples = getTriples(preparedOntologyElements);
            logger.info("Passing now these elements to query maker:\n" + triples);
            // resultGraph =
            // queryResultsMaker.getQueryUtil().getResultGraph(list2DOfResults,
            // triples, filterResultsBasedOnTheFunction);

            Map newMap;

            newMap =
                            queryUtil.getResultGraph(list2DOfResults,
                                            triples, filterResultsBasedOnTheFunction);
            resultGraph = (List<ResultGraph>) newMap.get(FreyaConstants.RESULTS_GRAPH);
            toReturn.put(FreyaConstants.RESULTS_GRAPH, resultGraph);
            toReturn.put(FreyaConstants.NUMBER_OF_RESULTS,
                            newMap.get(FreyaConstants.NUMBER_OF_RESULTS));

            // logger.debug("Generated graph:" + resultGraph.toString());
            if (resultGraph != null)
                logger.info("Generated graph has " + resultGraph.size() + " nodes.");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return toReturn;
    }

    /**
     * if all overlapped elements are of the same type returns true otherwise false
     * 
     * @param table
     * @return
     */
    public boolean elementsConsistent(List<List<SemanticConcept>> table) {
        boolean consistent = true;
        for (List<SemanticConcept> column : table) {
            boolean colConsistent = false;
            if (column.size() == 1)
                colConsistent = true;
            else {
                String previous = null;
                for (SemanticConcept cell : column) {
                    OntologyElement current = cell.getOntologyElement();
                    String currentClass = current.getClass().toString();
                    // logger.info("CurrentClass:"+currentClass);
                    if (previous == null) {
                        previous = currentClass;
                        continue;
                    } else if (previous != null && currentClass.equals(previous)) {
                        previous = currentClass;
                        colConsistent = true;
                    } else {
                        colConsistent = false;
                        return colConsistent;
                    }
                }
            }
        }
        return consistent;
    }

    /**
     * input is a list of columns; output is list of raws generated using backtracking
     * 
     * @param table
     * @return
     */
    List<List<SemanticConcept>> generateAllCombinations(List<List<SemanticConcept>> table) {
        List<List<SemanticConcept>> allCombinations = new ArrayList<List<SemanticConcept>>();
        int numberOfElements = table.size();
        BacktrackEngine bcte = new BacktrackEngine(new int[numberOfElements]);
        int[] newArray = bcte.getSizes();
        int[] clock = new int[numberOfElements];
        for (int i = 0; i < numberOfElements; i++) {
            List<SemanticConcept> object = table.get(i);
            Integer size = object.size();
            newArray[i] = size;
            clock[i] = 0;
        }
        bcte.setSizes(newArray);
        long start = System.currentTimeMillis();
        while (clock != null) {
            List listOfIndexes = new ArrayList();
            for (int n : clock) {
                // logger.debug("" + n + ":");
                listOfIndexes.add(n);
            }
            List<SemanticConcept> oneCombination = new ArrayList<SemanticConcept>();
            for (int l = 0; l < listOfIndexes.size(); l++) {
                for (List<SemanticConcept> column : table) {
                    SemanticConcept oneElement = column.get(l);
                    oneCombination.add(oneElement);
                }
                allCombinations.add(oneCombination);
            }
            clock = bcte.nextIndexes(clock);
        }
        long duration = System.currentTimeMillis() - start;
        logger.debug("Exec time : " + duration);
        return allCombinations;
    }

    /**
     * returns map where one key is table with results the other one is list of elements
     * 
     * @param table
     * @return
     */
    public HashMap getResultMap(List<List<SemanticConcept>> table, Question question) {
        List<List<OntologyElement>> preparedOntologyElements = null;
        HashMap map = new HashMap();
        List<List<String>> list2DOfResults = null;
        SemanticConceptListComparator semanticConceptComparator =
                        new SemanticConceptListComparator();
        Collections.sort(table, semanticConceptComparator);
        StringBuffer elms = new StringBuffer("");
        String sparqlQuery = null;
        String preciseSparql = null;
        // added to fix Bug
        ArrayList<Integer> toBeRemoved = new ArrayList();
        String uriTracker =null;
        Set<String> tempSet = new HashSet();
        for (List<SemanticConcept> column : table) {
            elms.append("Start new column:");
            for (SemanticConcept cell : column) {
                elms.append(cell.getOntologyElement().getData().toString());
                elms.append("(");
                uriTracker=cell.getOntologyElement().getData().toString();
                if (cell.getOntologyElement().getFunction() != null)
                    elms.append(cell.getFunction().toString());
                elms.append(") ");
                elms.append("|");
                // added to remove duplicate uri entry
                if (tempSet.add(uriTracker)) {
                    logger.debug("Added The following URI entry:"+ uriTracker);
                    logger.debug("Function of the corresponding URI is:"+cell.getOntologyElement().getFunction());
                }
                else
                {
                	logger.debug(" Not Added the duplciate URI entry:"+ uriTracker);
                	toBeRemoved.add(table.indexOf(column));
                	logger.debug("Function of the duplicate URI is:"+cell.getOntologyElement().getFunction());
                }
            }
        }
        logger.info("Duplicate Entries To Be removed:"+toBeRemoved);
        for (int i: toBeRemoved)
        {
        	table.remove(i);
        }
        logger.info("Now generating SPARQL from " + table.size() + " columns: " + elms);
        logger.info("Elements are:\n " + table.toString());
        try {
            List<List<OntologyElement>> elementsTable = new ArrayList<List<OntologyElement>>();
            for (List<SemanticConcept> column : table) {
                List<OntologyElement> columnOfOE = new ArrayList<OntologyElement>();
                for (SemanticConcept sc : column) {
                    if (sc.getOntologyElement() instanceof NoneElement) {
                        logger.info("Ignoring NONE element.");
                        continue;
                    }
                    if (!sc.getOntologyElement().isAlreadyAdded()) {
                        // TODO sc should not have function ...only oe..or the
                        // other
                        // way around
                        sc.getOntologyElement().setFunction(sc.getFunction());
                        columnOfOE.add(sc.getOntologyElement());
                        logger.debug("Adding function from sc to oe..." + sc.getFunction());
                    } else {
                        logger.debug("Found element which is already added:"
                                        + sc.getOntologyElement().getData().toString()
                                        + " and with function: " + sc.getFunction());
                        // change function
                        SemanticConcept changedSc = sc;
                        List<SemanticConcept> sElementsNoDuplicates =
                                        new ArrayList<SemanticConcept>();
                        sElementsNoDuplicates.addAll(column);
                        // remove the one flagged 'already added' but update the
                        // function
                        for (SemanticConcept concept : sElementsNoDuplicates) {
                            if (concept.getOntologyElement().getAnnotation()
                                            .equals(changedSc.getOntologyElement().getAnnotation())
                            // function is equal?
                            ) {
                                concept.getOntologyElement().setFunction(changedSc.getFunction());
                                logger.debug("Updating function of already existing ontology element: "
                                                + changedSc.getOntologyElement().getAnnotation()
                                                                .getText()
                                                + " from "
                                                + concept.getFunction()
                                                + " to "
                                                + changedSc.getFunction());
                            }
                        }
                    }
                }
                if (columnOfOE.size() > 0) elementsTable.add(columnOfOE);
                columnOfOE = new ArrayList();
            }
            int col = 0;
            for (List<OntologyElement> column : elementsTable) {
                logger.debug("These are elements in col " + col + ":" + column.toString());
                col++;
            }
            OntologyElementListComparator c = new OntologyElementListComparator();
            Collections.sort(elementsTable, c);
            logger.debug("Calling triple creator...");
            preparedOntologyElements = tripleCreator.prepareOntologyElements(elementsTable);
            // now check all property elements: if they are datatypeProperty
            // elements and not surrounded by datatype property value element or
            // Joker, then shift them to the right or to the left
            // query maker will also add setAnswer(true) for the non-governors
            // of the datatype properties
            QueryElement queryElement = queryMaker.transform(preparedOntologyElements);
            logger.debug("after query maker:\n" + queryElement.toString());
            FormalQueryResult formalQueryResult =
                            queryResultsMaker.transform(queryElement, question);
            logger.debug("Results:\n" + formalQueryResult.toString());
            list2DOfResults = formalQueryResult.getData();
            sparqlQuery = formalQueryResult.getQuery();
            preciseSparql = formalQueryResult.getPreciseSparqlQuery();
            logger.debug("list2DOfResults:\n" + list2DOfResults.toString());
            logger.info("SPARQL result has " + list2DOfResults.size() + " rows.");
        } catch (Exception e) {
            logger.error(e);
        }
        // return list2DOfResults;
        map.put("elements", preparedOntologyElements);
        map.put("table", list2DOfResults);
        map.put("sparql", sparqlQuery);
        map.put(FreyaConstants.PRECISE_SPARQL, preciseSparql);
        // map.put(FreyaConstants.REPOSITORY_URL, ((SparqlQueryExecuter) getQueryResultsMaker()
        // .getQueryUtil().getQueryExecuter()).getSparqlUtils().getRepositoryURL());
        // map.put(FreyaConstants.REPOSITORY_ID, ((SparqlQueryExecuter) getQueryResultsMaker()
        // .getQueryUtil().getQueryExecuter()).getSparqlUtils().getRepositoryId());
        map.put(FreyaConstants.REPOSITORY_URL, repository.getRepositoryURL());
        map.put(FreyaConstants.REPOSITORY_ID, repository.getRepositoryId());
        return map;
    }


}
