package org.freya.util;

import info.aduna.iteration.CloseableIteration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.annotate.lucene.LuceneAnnotator;
import org.freya.model.OntologyElement;
import org.freya.model.ResultGraph;
import org.freya.model.SerializableURI;
import org.freya.model.TripleElement;
import org.freya.rdf.SesameRepositoryManager;
import org.freya.rdf.format.SparqlResultPostProcessor;
import org.freya.rdf.query.CATConstants;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class QueryUtil {
    private static final Log logger = LogFactory.getLog(QueryUtil.class);

    @Autowired LuceneAnnotator luceneAnnotator;

    @Qualifier("rdfRepository")
    @Autowired SesameRepositoryManager repository;

    @Autowired SparqlResultPostProcessor sparqlResultPostProcessor;

    @Autowired GraphUtils graphUtils;

 

    /**
     * duplicate method!!!!!!!!!!!!look up json creator This method gets label randomly from the mapOfLabels;
     * 
     * @param uri
     * @param mapOfLabels
     * @return
     */
    public String getLabel(String uriString) {
        if (uriString == null) return "null";
        // Map<String, Set> mapOfLabels = ontology2Map.getMapOfLabels();
        SerializableURI uri = null;
        try {
            uri = new SerializableURI(uriString, false);
            String label = null;
            // ///////proveri da li je csv of label uris set and if yes use it
            List<String> labels = luceneAnnotator.findLabels(uriString);
            //
            // ontology2MapManager.getOntology2Map().getLuceneAnnotator()
            // .findLabels(uriString);
            for (String l : labels) {
                label = l;
                if (l.contains(" ")) return l;
            }
            if (label != null)
                return StringUtil.beautifyString(label);
            else if (label == null)
                return StringUtil.beautifyString(uri.getResourceName());
            else
                return null;
        } catch (Exception ie) {
            return StringUtil.beautifyString(uriString);
        }
    }


    /**
     * @param query
     * @param interpretation
     * @return
     * @throws GateException
     */
    public Map getResultGraph(List<List<String>> result, List<TripleElement> triples,
                    boolean removeResultsBasedOnFunction) throws Exception {
        Map map = new HashMap();
        if (result == null || (result.size() <= 0)) return new HashMap();
        Map newMap = getGraphFromListOfLists(result, triples, removeResultsBasedOnFunction);
        List<ResultGraph> graph = (List<ResultGraph>) newMap.get(FreyaConstants.RESULTS_GRAPH);
        Integer numberOfResults = (Integer) newMap.get(FreyaConstants.NUMBER_OF_RESULTS);
        // return graph;
        map.put(FreyaConstants.RESULTS_GRAPH, graph);
        map.put(FreyaConstants.NUMBER_OF_RESULTS, numberOfResults);
        return map;
    }

    /**
     * returning graph
     * 
     * @param resultIt
     * @param variables
     * @param elements
     * @return
     */
    public Map getGraphFromListOfLists(List<List<String>> table, List<TripleElement> elements,
                    boolean filterResultsAccordingToFunction) {
        Map map = new HashMap();
        Integer numberOfResults = null;
        // logger.debug("started...");
        List<ResultGraph> finalGraph = new ArrayList<ResultGraph>();
        HashMap<String, List<String>> columnData = new LinkedHashMap<String, List<String>>();
        try {
            List<String> variables = new ArrayList<String>();
            for (int row = 0; row < table.size(); row++) {
                // logger.debug("there are some results");
                List<String> tuple = table.get(row);
                // first row contains variable names
                if (row == 0) {
                    for (String var : tuple) {
                        variables.add(var);
                        if (columnData.get(var) == null)
                            columnData.put(var, new ArrayList<String>());
                    }
                    continue;
                }
                int col = 0;
                for (String value : tuple) {
                    String var = variables.get(col);
                    List<String> currentSet = columnData.get(var);
                    currentSet.add(value);
                    columnData.put(var, currentSet);
                    col = col + 1;
                }
            }
            boolean resultsFound = false;
            // stick results into elements
            for (TripleElement el : elements) {
                for (List<OntologyElement> oe : el.getElements()) {
                    List<String> currentSet = columnData.get(oe.get(0).getVariable());
                    if (currentSet != null) {
                        for (OntologyElement current : oe) {
                            current.setResults(currentSet);
                            resultsFound = true;
                        }
                        // logger.debug("current set for: " + oe.getVariable()
                        // + " is: " + currentSet.toString());
                    }
                }
            }
            // logger.debug("elements: " + elements.toString());

            if (filterResultsAccordingToFunction) {
                Map newMap = sparqlResultPostProcessor.filterResultsAccordingToFunction(elements);
                elements = (List<TripleElement>) newMap.get(FreyaConstants.TRIPLE_ELEMENTS);
                numberOfResults = (Integer) newMap.get(FreyaConstants.NUMBER_OF_RESULTS);
            }
            if (resultsFound) finalGraph = graphUtils.getResultGraph(elements);
        } catch (QueryEvaluationException e) {
            e.printStackTrace();
        } catch (MalformedQueryException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // logger.debug("exiting this method... resultGraph is of size:"
        // + finalGraph.size());
        // return finalGraph;
        map.put(FreyaConstants.RESULTS_GRAPH, finalGraph);
        map.put(FreyaConstants.NUMBER_OF_RESULTS, numberOfResults);
        return map;
    }

    /**
	 * Added an entry to handle aggregation in Freya
	 */
    public List<List<String>> get2DList(String query) throws Exception {
    	int beginIndexSum;
    	int beginIndexAvg;
		int endIndexSum;
		int endIndexAvg;
		String replaceAnswerString=null;
		List<List<String>> listOfResults;
        if (query == null || ("".equals(query))) return new ArrayList<List<String>>();
        // QueryExecuter queryExecuter = new SparqlQueryExecuter();
        CloseableIteration<BindingSet, QueryEvaluationException> result =
                        repository.executeQuery(query);
        if (result != null) logger.info("result of executed sparql is:\n" + result.toString());
        beginIndexSum=query.indexOf(FreyaConstants.SELECT_SUM);
        beginIndexAvg=query.indexOf(FreyaConstants.SELECT_AVG);
        if(beginIndexSum>-1)
        {
        	String resultsToString = getStringWithVariablesInTheFirstRow(result);
        	int split_new_line=resultsToString.indexOf(System.getProperty("line.separator"));
    		String extract=resultsToString.substring(0, split_new_line) ;
    		endIndexSum=query.indexOf(") AS");
    		replaceAnswerString=query.substring(beginIndexSum+FreyaConstants.SELECT_SUM.length()+1, endIndexSum-1);
        	StringBuffer sb = new StringBuffer(resultsToString);
    		sb.replace(0, split_new_line, replaceAnswerString);
    		resultsToString=sb.toString();
        	listOfResults = get2DListOfElementsFromTable(resultsToString);
        }
        else if(beginIndexAvg>-1)
        {
        	String resultsToString = getStringWithVariablesInTheFirstRow(result);
        	int split_new_line=resultsToString.indexOf(System.getProperty("line.separator"));
    		String extract=resultsToString.substring(0, split_new_line) ;
    		endIndexAvg=query.indexOf(") AS");
    		replaceAnswerString=query.substring(beginIndexAvg+FreyaConstants.SELECT_AVG.length()+1, endIndexAvg-1);
        	StringBuffer sb = new StringBuffer(resultsToString);
    		sb.replace(0, split_new_line, replaceAnswerString);
    		resultsToString=sb.toString();
        	listOfResults = get2DListOfElementsFromTable(resultsToString);
        }
        else
        {
        	String resultsToString = getStringWithVariablesInTheFirstRow(result);
        	//logger.info("first row text:"+resultsToString);
        	listOfResults = get2DListOfElementsFromTable(resultsToString);
        	//logger.info("first row list of results:"+listOfResults);
        }
       
        // logger
        // .debug("listOfResults (after processing returned results and generating table from them)\n"
        // + listOfResults.toString());
        return listOfResults;
    }

    /**
     * @param resultIt
     * @return
     */
    public String getStringFromCloseableIteration(CloseableIteration resultIt) {
        StringBuffer result = new StringBuffer("");
        try {
            int rows = 0;
            TupleQueryResult t = ((TupleQueryResult) resultIt);
            List<String> variables = t.getBindingNames();
            // logger.debug("variables:" + variables + "\n --------------------------");
            while (resultIt.hasNext()) {
                BindingSet tuple = (BindingSet) t.next();
                rows++;
                // iterate through variables
                long numberOfVariables = variables.size();
                long currentVariable = 0;
                for (String var : variables) {
                    currentVariable++;
                    Binding b = tuple.getBinding(var);
                    Value col = null;
                    if (b != null) {
                        col = b.getValue();
                        result.append(col.stringValue());
                    } else
                        result.append("IT WAS NULLL");
                    if (currentVariable < (numberOfVariables)) result.append("|");
                }
                result.append(CATConstants.NEW_LINE);
            }
        } catch (QueryEvaluationException e) {
            e.printStackTrace();
        } catch (MalformedQueryException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // System.out.println("executed, results:" + result);
        return result.toString();
    }

    /**
     * @param resultIt
     * @return
     */
    public String getStringWithVariablesInTheFirstRow(CloseableIteration resultIt) {
        if (resultIt == null) return null;
        StringBuffer result = new StringBuffer("");
        int counter = 0;
        List<String> variables = null;

        try {
            variables = ((TupleQueryResult) resultIt).getBindingNames();
            // if (resultIt.hasNext())
            // variables = ((BindingSet) resultIt.next()).getBindingNames();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        // logger.debug("variables:" + variables.toString());
        for (String variable : variables) {
            result.append(variable);
            if ((counter + 1) == variables.size())
                result.append(CATConstants.NEW_LINE);
            else
                result.append("|");
            counter = counter + 1;
        }
        try {
            int rows = 0;
            while (resultIt.hasNext()) {
                BindingSet tuple = (BindingSet) resultIt.next();
                // Set bNames=tuple.getBindingNames();
                rows++;
                // iterate through variables
                long numberOfVariables = variables.size();
                long currentVariable = 0;
                for (String var : variables) {
                    currentVariable++;
                    Binding b = tuple.getBinding(var);
                    Value col = null;
                    if (b != null) {
                        col = b.getValue();
                        result.append(col.stringValue());
                    } else
                        result.append("IT WAS NULLL");
                    if (currentVariable < (numberOfVariables)) result.append("|");
                }
                result.append(CATConstants.NEW_LINE);
            }
        } catch (QueryEvaluationException e) {
            e.printStackTrace();
        } catch (MalformedQueryException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // logger.debug("executed, results:" + result);
        return result.toString();
    }

    /**
     * This method reads QueryResultsTable and create a list of elements. This list is in a form of 2D table: it
     * contains a list of lists, where each list is a row which contains a list of resourceUris (Strings).
     * 
     * @param resultsTable
     * @param ontology
     * @return
     */
    public List<List<String>> get2DListOfElementsFromTable(String results) {
        List<List<String>> table = new ArrayList<List<String>>();
        int rowCount = 0;
        int columnCount = 0;
        String[] rows = null;
        String[] columns = null;
        // logger.debug(results);
        if (results != null) {
            rows = results.split(CATConstants.NEW_LINE);
            rowCount = rows.length;
            // System.out.println("this many rows..." + rowCount + "\n");
            if (rows != null && rows.length > 0) {
                columns = rows[0].split("\\|");
            }
            columnCount = columns.length;
            // System.out.println("this many columns..." + columnCount + "\n");
        }
        /* first row is empty, skip it */
        for (int row = 0; row < rowCount; row++) {
            List<String> rowList = new ArrayList<String>();
            for (int column = 0; column < columnCount; column++) {
                String[] colValues = rows[row].split("\\|");
                String value = colValues[column];
                if (value != null) {
                    String resourceUri = value.toString();
                    if (!"".equals(resourceUri.trim())) rowList.add(resourceUri);
                    // System.out.println("adding " + resourceUri +
                    // " to the list...\n");
                } else {
                    logger.debug("value is null");
                    rowList.add(null);
                }
            }
            // System.out.println("adding new line...\n");
            if (rowList != null && rowList.size() > 0) table.add(rowList);
        }
        return table;
    }

    /**
     * Returns list created from the table of results.
     */
    public List query(CloseableIteration<BindingSet, QueryEvaluationException> result, String query)
                    throws Exception {
        try {
            if (result == null || (!result.hasNext())) return new ArrayList<List<String>>();
        } catch (QueryEvaluationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List listOfResults = getFormattedListFromTable(getStringFromCloseableIteration(result));
        return listOfResults;
    }

    /**
     * This method reads QueryResultsTable and create a list of elements. This list contains resources listed in triples
     * (OResources) separated by (String) elements: CloneQlConstants.NEW_LINE is btw. triples. Elements inside each
     * triple are separated by CloneQlConstants.TRIPLES_SEPARATOR.
     * 
     * @param resultsTable
     * @param ontology
     * @return
     */
    List<String> getFormattedListFromTable(String results) {
        List<String> listToReturn = new ArrayList<String>();
        int rowCount = 0;
        int columnCount = 0;
        String[] rows = null;
        String[] columns = null;
        if (results != null) {
            rows = results.split(CATConstants.NEW_LINE);
            rowCount = rows.length;
            columns = rows[0].split("\\|");
            columnCount = columns.length;
        }
        for (int row = 2; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                String[] colValues = rows[row].split("\\|");
                String value = colValues[column];
                if (value != null) {
                    String resourceUri = value.toString();
                    listToReturn.add(resourceUri);
                } else {
                    logger.debug("value is null");
                    listToReturn.add(null);
                }
                if (column < (columnCount - 1)) {
                    int size = listToReturn.size();
                    if (FreyaConstants.INVERSE_PROPERTY.equals(listToReturn.get(size - 1)))
                        listToReturn.add(" ");
                    else
                        listToReturn.add(FreyaConstants.TRIPLES_SEPARATOR);
                }
            }
            listToReturn.add(CATConstants.NEW_LINE);
        }
        return listToReturn;
    }

    public String readQueryContent(String queryPath) {
        InputStream data;
        data = this.getClass().getResourceAsStream(queryPath);
        java.util.Scanner s = new java.util.Scanner(data).useDelimiter("\\A");
        String queryContent = s.hasNext() ? s.next() : "";
        s.close();
        return queryContent;
    }
}
