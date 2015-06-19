package org.freya.rdf.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.OntologyElement;
import org.freya.model.TripleElement;
import org.freya.util.FreyaConstants;
import org.springframework.stereotype.Component;

/**
 * @author danica
 */
@Component
public class SparqlResultPostProcessor {
  Log logger = LogFactory.getLog(SparqlResultPostProcessor.class);
/**
 * 
 * 
 * @param tElements
 * @return map with 'numberOfResults' key and value integer; and 'tripleElements' and List<TripleElement> in value
 */
  public Map filterResultsAccordingToFunction(
          List<TripleElement> tElements) {
    Map map=new HashMap();
    int numberOfResults = -1;
    int numberOfOriginalResults = tElements.get(0).getElements().get(0).get(0).getResults().size();
    boolean filterResults = false;
    for(TripleElement triple : tElements) {
      // we expect property in the middle of the triple
      OntologyElement property = null;
      if(triple.getElements().size() > 2)
        property = triple.getElements().get(1).get(0);
      for(int i = 0; i < triple.getElements().size(); i = i + 2) {
        OntologyElement el = triple.getElements().get(i).get(0);
        List<String> results = el.getResults();
        String previous = null;
        if(property != null
                && property.getFunction() != null
                && ( (FreyaConstants.MAX_FUNCTION.equals(property.getFunction()) ) || ( FreyaConstants.MIN_FUNCTION
                        .equals(property.getFunction()) ) || ( FreyaConstants.SUM_FUNCTION.equals(property.getFunction() ) || ( FreyaConstants.AVG_FUNCTION.equals(property.getFunction() ) ) )) && el.isAnswer() ) {
          filterResults = true;
          List<String> newList = new ArrayList<String>();
          for(int j = 0; j < results.size(); j++) {
            // skiping the first row as those are variables
            String result = results.get(j);
            if(previous != null && !result.equals(previous)) {
              int tempNumberOfResults = newList.size();
              if(numberOfResults < tempNumberOfResults) {
                numberOfResults = tempNumberOfResults;
                logger.info("Current numberOfResults..." + numberOfResults);
              }
              break;
            } else {
              newList.add(result);
              previous = new String(result);
              numberOfResults = newList.size();
            }
          }
          if(numberOfResults > -1) filterResults = true;
        }
      }
    }
    if(filterResults) {
      logger.info("Number of rows to be included in the result:"
              + numberOfResults);
      // now cut the element results up to the numberOfResults
      // Added an entry for aggregation check
      for(TripleElement triple : tElements) {
        for(List<OntologyElement> el : triple.getElements()) {
          List<String> results = el.get(0).getResults();
          List<String> cutResults = new ArrayList<String>();
          if( results == null) continue;
          for(int index = 0; index < numberOfResults; index++) {
            cutResults.add(results.get(index));
            logger.debug("adding element..." + index);
          }
          logger.debug("Results after removing:" + cutResults.size()
                  + " details:\n" + cutResults);
          for (OntologyElement currEl:el){
            currEl.setResults(cutResults);
            
          }
        }
      }
    } else {
      logger
              .info("Results are not being changed from the original ones returned by SPARQL...");
    }
   if (numberOfResults==-1)
     numberOfResults=numberOfOriginalResults;
   map.put(FreyaConstants.NUMBER_OF_RESULTS, new Integer(numberOfResults));
   map.put(FreyaConstants.TRIPLE_ELEMENTS, tElements);
    return map;
  }
}
