/**
 * 
 */
package org.freya.analyser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.ClassElement;
import org.freya.model.DatatypePropertyValueElement;
import org.freya.model.GroupElement;
import org.freya.model.InstanceElement;
import org.freya.model.InstanceListElement;
import org.freya.model.JokerElement;
import org.freya.model.OntologyElement;
import org.freya.model.PropertyElement;
import org.freya.model.TripleElement;
import org.freya.util.StringUtil;
import org.springframework.stereotype.Component;

/**
 * @author danica
 */
@Component
public class TripleCreator {
  private static final Log logger = LogFactory.getLog(TripleCreator.class);

  /**
   * add jokers etc
   * 
   * @param intElements
   * @return
   */
  public List<List<OntologyElement>> prepareOntologyElements(
          List<List<OntologyElement>> intElementsTable) {
    intElementsTable =
            positionTheDatatypePropertyElementsTheOptimalWay(intElementsTable);
    intElementsTable = positionTheLastPropertyTheOptimalWay(intElementsTable);
    if(intElementsTable == null)
      intElementsTable = new ArrayList<List<OntologyElement>>();
    List<List<OntologyElement>> oldElements =
            new ArrayList<List<OntologyElement>>();
    /*
     * for example: ClassElement followed by an InstanceElement would be
     * represented as c0-i1, so first char representing a type of element
     * followed by index
     */
    StringBuffer interpretationOfResources = new StringBuffer("");
    for(int i = 0; i < intElementsTable.size(); i++) {
      // although this is table; all elements are of the same type so we get the
      // first one
      OntologyElement el = (OntologyElement)intElementsTable.get(i).get(0);
      oldElements.add(intElementsTable.get(i));
      if(el instanceof ClassElement) {
        interpretationOfResources.append("c").append(i).append("-");
      } else if(el instanceof InstanceElement
              || el instanceof InstanceListElement) {
        interpretationOfResources.append("i").append(i).append("-");
      } else if(el instanceof PropertyElement) {
        interpretationOfResources.append("p").append(i).append("-");
      } else if(el instanceof DatatypePropertyValueElement) {
        interpretationOfResources.append("d").append(i).append("-");
      } else if(el instanceof GroupElement) {
        interpretationOfResources.append("g").append(i).append("-");
      } 
    }
    /*
     * this metod will 'complete' the string in a way that it will clean it from
     * any dashes, spaces, and the like and also it will check for the potential
     * properties and add them into the string, for example, if two instances
     * are not separated by a property it will add the property element between
     * them; additionally, it will add c:firstJoker or c:lastJoker if the first
     * or the last elements are properties
     */
    logger.debug("Before adding jokers etc.."
            + interpretationOfResources.toString());
    String[] interpretationOfResource =
            StringUtil.createCompleteString(
                    interpretationOfResources.toString()).toString().split("-");
    logger
            .debug("This is string with all variables and properties(before processing):");
    for(String s : interpretationOfResource) {
      logger.debug(s);
    }
    logger.debug("and after processing (" + interpretationOfResource.length
            + ") " );
    for(String s : interpretationOfResource) {
      logger.debug(s);
    }
    // generate Joker elements for all these j and c:firsJoker and
    // c:lastJoker etc
    /*
     * index will be null: if starts with j it means that will be a potential
     * property; if starts with c: it means that this is the first or the last
     * element that has been added to complete the string; neither of the two
     * has index specified
     */
    int index = 0;
    List<List<OntologyElement>> elementsTable =
            new ArrayList<List<OntologyElement>>();
    List<String> elms = new ArrayList<String>();
    for(int i = 0; i < interpretationOfResource.length; i++) {
      String key = interpretationOfResource[i];
      elms.add(key);
     // logger.debug("key:" + key);
      if("".equals(key)) continue;
      if(key.trim().startsWith("joker:p") || key.trim().endsWith("Joker")) {// firstJoker
        // or
        // lastJoker or property joker: joker:p0:p1:joker which is in between
        // two properties
        JokerElement element = new JokerElement();
        element.setType("class");
        if(key.trim().startsWith("joker:p")) {
          ((OntologyElement)element).setVariable("classJoker" + i);
        } else {
          ((OntologyElement)element).setVariable("firstJoker" + i);
        }
        List<OntologyElement> newElements = new ArrayList<OntologyElement>();
        newElements.add(element);
        elementsTable.add(newElements);
        // logger.debug("adding element" +
        // element.getVariable());
      } else if(key.trim().startsWith("joker:")) {// joker01
        JokerElement propertyE = new JokerElement();
        propertyE.setType("property");
        ((OntologyElement)propertyE).setVariable("joker" + i);
        List<OntologyElement> newElements = new ArrayList<OntologyElement>();
        newElements.add(propertyE);
        elementsTable.add(newElements);
      } else {
        // these are non joker elements
        List<OntologyElement> inter =
                ((List<OntologyElement>)oldElements.get(index));
        index++;
        for(OntologyElement el : inter) {
          el.setVariable(key);
        }
        elementsTable.add(inter);
      }
    }
    return elementsTable;
    // List ontElements = new ArrayList();
    // for(OntologyElement el : elements) {
    // ontElements.add((OntologyElement)el);
    // }
    // return ontElements;
  }

  /**
   * check if element is c/i/d
   * 
   * @param el
   * @return
   */
  boolean isConcept(OntologyElement el) {
    boolean isConcept = false;
    if(el instanceof InstanceListElement || el instanceof ClassElement
            || el instanceof DatatypePropertyValueElement
            || el instanceof InstanceElement) isConcept = true;
    return isConcept;
  }

  /**
   * if the elements are: c/i/d c/i/d p then it will move p in between
   * 
   * @param elementsTable
   * @return
   */
  List<List<OntologyElement>> positionTheLastPropertyTheOptimalWay(
          List<List<OntologyElement>> elementsTable) {
    
    int lastElementIndex = elementsTable.size() - 1;
    int oneButLastElementIndex = elementsTable.size() - 2;
    int twoButLastElementIndex = elementsTable.size() - 3;
    
    List<OntologyElement> lastColumn = null;
    if(lastElementIndex > -1) lastColumn = elementsTable.get(lastElementIndex);
    OntologyElement sampleLastColumn=null;
    if (lastColumn!=null) sampleLastColumn=lastColumn.get(0);
    
    List<OntologyElement> oneButLastColumn = null;
    if(oneButLastElementIndex > -1) oneButLastColumn = elementsTable.get(oneButLastElementIndex);
    OntologyElement sampleOneButLastColumn=null;
    if (oneButLastColumn!=null) sampleOneButLastColumn=oneButLastColumn.get(0);
    
    List<OntologyElement> twoButLastColumn = null;
    if(twoButLastElementIndex > -1) twoButLastColumn = elementsTable.get(twoButLastElementIndex);
    OntologyElement sampleTwoButLastColumn=null;
    if (twoButLastColumn!=null) sampleTwoButLastColumn=twoButLastColumn.get(0);
    
    if (sampleLastColumn !=null && sampleLastColumn instanceof PropertyElement 
         & sampleOneButLastColumn!=null&   isConcept(sampleOneButLastColumn) & 
         sampleTwoButLastColumn!=null &  isConcept(sampleTwoButLastColumn)){
      
      //swap the last and oneButLast
      elementsTable.add(lastElementIndex+1, oneButLastColumn);
      elementsTable.remove(oneButLastElementIndex);
      logger
      .debug("Swapped the property and the class/instance/datatypevalue and the list looks like this:\n"
              + elementsTable.toString());
    }
   
    return elementsTable;
  }

  /**
   * this method repositions the datatype property elements to the right or to
   * the left if they are not surrounded with either datatype property value
   * elements or with jokers; so if there is J-P-C-P2-C order where P is
   * datatype property we need to move it to the right or to the left
   * 
   * @param ontElements
   * @return
   */
  List<List<OntologyElement>> positionTheDatatypePropertyElementsTheOptimalWay(
          List<List<OntologyElement>> elementsTable) {
    for(int i = 0; i < elementsTable.size(); i++) {
      // it does not matter as one column is of the same type
      List<OntologyElement> elColumn = elementsTable.get(i);
      OntologyElement sampleEl = elColumn.get(0);
      if(sampleEl instanceof PropertyElement
              && ((PropertyElement)sampleEl).isDatatypeProperty()) {
        OntologyElement previous = null;
        OntologyElement next = null;
        int previousIndex = i - 1;
        int nextIndex = i + 1;
        if(previousIndex > -1) {
          previous = elementsTable.get(previousIndex).get(0);
        }
        if(nextIndex < elementsTable.size()) {
          next = elementsTable.get(nextIndex).get(0);
        }
        if(previous == null || next == null) {
          // this means that the datatype property element is
          // positioned
          // at the end or at the beginning so no need to move it
          // anywhere
        } else {
          if(previous instanceof DatatypePropertyValueElement
                  || next instanceof DatatypePropertyValueElement) {
            // again do nothing as this means that the property is
            // on the right place
          } else {
            // move it around the governor
            OntologyElement governor =
                    ((PropertyElement)sampleEl).getGovernor();
            int elementIndex = elementsTable.indexOf(elColumn);
            if(governor != null && governor.equals(next)) {
              // move it around next
              elementsTable.add(nextIndex + 1, elColumn);
              elementsTable.remove(elementIndex);
              logger.debug("Rearranging around next...");
            } else if(governor != null && governor.equals(previous)) {
              // move it around previous
              elementsTable.add(previousIndex, elColumn);
              elementsTable.remove(elementIndex);
              logger.debug("Rearranging around previous...");
            }
          }
        }
      }
    }
    logger
            .debug("After rearranging the datatype property elements the list looks like this:\n"
                    + elementsTable.toString());
    return elementsTable;
  }

  /**
   * generates triples from set of ontology elements
   * 
   * @param interpretation
   * @return
   * @throws GateException
   */
  public List<TripleElement> getTriples(
          List<List<OntologyElement>> ontElementsTable) throws Exception {
    List<TripleElement> trs = new ArrayList<TripleElement>();
    // QueryInterpretation currentFilteredInterpretation=null;
    try {
      // after jokers are created create triples
      // create list of list i.e. a 2d matrix/table) out of elements
      // so that each row contains 3 elements so that the last element
      // in
      // a
      // row is the first element in the next row etc. for example if
      // this
      // is
      // the column header: a-b-c-d-e-f-g-h-i then the newly created
      // 'table'
      // will look like
      // a-b-c
      // c-d-e
      // e-f-g
      // g-h-i
      List<List<List<OntologyElement>>> newColumnData =
              StringUtil.getListOfTriplesFromLinearForm(ontElementsTable);
      logger.debug("After splitting up in triples this manny triples:"
              + newColumnData.size());
      // check if each second element in the row is the property with
      // inverseProperty=true, in that case revert first and third
      // column
      // for
      // example: a-b-c row has b as a property with
      // inverseProperty=true,
      // so
      // this row becomes c-b-a, if d f and h are not
      // invertedProperties
      // than
      // the table becomes:
      // c-b-a
      // c-d-e
      // e-f-g
      // g-h-i
      List<List<List<OntologyElement>>> invertedColumnData =
              StringUtil.getInvertedTriplesIfPropertiesAreInverted(
                      newColumnData, ontElementsTable);
      logger.debug("After inverting els in triples this manny triples:"
              + invertedColumnData.size());
      for(List<List<OntologyElement>> els : invertedColumnData) {
        TripleElement triple = new TripleElement();
        triple.setElements(els);
        logger.debug("Number of elements per triple:" + els.size());
        trs.add(triple);
      }
      // remove those interpretations which do not have 1 element or 3*n+2
      // elements where n=1,...,m
      // }// stop iterating through interpretations
      // currentFilteredInterpretation = QueryUtil
      // .filterNotCompleteTripleInterpretationsWithUncompleteTriples(interpretation);
      // logger.debug("interpretation AFTER filtering:"
      // + currentFilteredInterpretation);
    } catch(Throwable e) {
      e.printStackTrace();
    }
    return trs;
    // return currentFilteredInterpretation;
  }
}
