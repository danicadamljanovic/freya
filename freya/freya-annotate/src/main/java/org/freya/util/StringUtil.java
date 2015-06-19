package org.freya.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.freya.model.OntologyElement;
import org.freya.model.PropertyElement;
import org.freya.model.SerializableURI;
import org.freya.rdf.query.CATConstants;
import org.freya.regex.ExpressionFinder;
import org.freya.regex.JokerFinder;

/**
 * @author Danica Damljanovic
 */
public class StringUtil {
    static org.apache.commons.logging.Log logger =
                    org.apache.commons.logging.LogFactory.getLog(StringUtil.class);

    /**
     * This method assume that given string is in the form of QueryResultsTable.toString() with 2 columns and converts
     * it to HashMap where key is the first entry from the table and the value is the set of second entries.
     * 
     * @param resultsTable
     * @return a Map with a key taken from the first column of the table and a value being a set of values taken from
     *         the second column.
     */
    public static Map<String, Set<String>> fromStringToMap(String resultsTable) {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        String[] rows = resultsTable.split(CATConstants.NEW_LINE);
        for (String eachRow : rows) {
            String[] columns = eachRow.split("\\|");
            if (columns.length == 2) {
                String propertyUri = columns[0].trim();
                String classUri = columns[1].trim();
                Set<String> classes = map.get(propertyUri);
                if (classes == null) {
                    classes = new HashSet<String>();
                }
                classes.add(classUri);
                try {
                    SerializableURI uri = new SerializableURI(propertyUri, false);
                    map.put(propertyUri, classes);
                } catch (Exception e) {
                    // logger.debug("URI:" + propertyUri + " is not valid.\n");
                }
            }
        }
        return map;
    }

    /**
     * This method assume that given string is in the form of QueryResultsTable.toString() with 1 column and converts it
     * to the Set of strings i.e. values from the each row of the table
     * 
     * @param resultsTable
     * @return a Set of Strings from the given table (string separated by new lines)
     */
    public static Set<String> fromStringToSet(String resultsTable) {
        Set<String> set = new HashSet<String>();
        String[] rows = resultsTable.split(CATConstants.NEW_LINE);
        for (String eachRow : rows) {
            String uri = eachRow.trim();
            try {
                SerializableURI uriUri = new SerializableURI(uri, false);
                set.add(uri);
            } catch (Exception e) {
                // logger.debug("URI:" + uri + " is not valid.\n");
            }
        }
        return set;
    }

    /**
     * This method takes a string and beautifies it by inserting spaces instead of dash and underline and also by
     * splitting camelCased words
     * 
     * @param inputString
     * @return
     */
    public static String beautifyString(String inputString) {
        String resultString =
                        ExpressionFinder.findAndSeparateCamelCases(inputString,
                                        CATConstants.REGEX_CAMEL_CASE, " ");
        // also replace dashes and underscores with space
        if (resultString.contains("_"))
            resultString = resultString.replace('_', ' ');
        if (resultString.contains("-"))
            resultString = resultString.replace('-', ' ');
        return resultString;
    }

    /**
     * This method is actually cleaning the string from any spaces, dashes, etc. Additionally, it executes the method
     * for finding two ontology resources that are not separated by a property and adds a so called 'joker' for example
     * joker:i0:i1:joker if there were no p elements between i0 and i1 Also, it checks the first and the last element in
     * the string and in case it reveals a property it adds c:firstJoker before the first element or c:lastJoker after
     * the last element
     * 
     * @param interpretationOfResources
     * @return
     */
    public static String createCompleteString(String interpretationOfResources) {
        /*
         * this method will find any places in the string where potential properties can be added
         */
        String interpretationOfResourcesWithFoundJokers =
                        JokerFinder.findAndReplaceGroupOccurencies(
                                        interpretationOfResources.toString(),
                                        FreyaConstants.REGEX_FIND_JOKER, FreyaConstants.JOKER);
        logger.info("After injecting property jokers:"
                        + interpretationOfResourcesWithFoundJokers);
        interpretationOfResourcesWithFoundJokers =
                        JokerFinder.findAndReplaceGroupOccurencies(
                                        interpretationOfResourcesWithFoundJokers,
                                        FreyaConstants.REGEX_FIND_CLASS_JOKER,
                                        FreyaConstants.JOKER);
        logger.info("After injecting class jokers:"
                        + interpretationOfResourcesWithFoundJokers);
        /*
         * additionally, if the string starts or ends with 'p' we need to add jokerClasses before or after them
         */
        String[] interpretationOfResource =
                        interpretationOfResourcesWithFoundJokers.toString().split("-");
        String firstElement = interpretationOfResource[0];
        String lastElement =
                        interpretationOfResource[interpretationOfResource.length - 1];
        StringBuffer finalString = new StringBuffer("");
        // System.out.println("before checking p:" + finalString.toString());
        if (firstElement.startsWith("p") && lastElement.startsWith("p")
                        && firstElement.equals(lastElement)) {
            // this means that they are exactly same elements such as po-
            finalString.append("c:firstJoker-");
            finalString.append(firstElement);
            finalString.append("-c:lastJoker");
            return finalString.toString();
        }
        if (firstElement.startsWith("p")) {
            finalString.append("c:firstJoker");
            if (!interpretationOfResourcesWithFoundJokers.startsWith("-"))
                finalString.append("-");
            if (firstElement != lastElement) // this check is to ensure that if
                // we have
                // one element only, it will be added twice, both after the
                // first
                // element and before
                // the second element, although they are the same one
                finalString.append(interpretationOfResourcesWithFoundJokers);
            // System.out.println("before checking p:" +
            // finalString.toString());
        }
        logger.info("firstElement:" + firstElement);
        logger.info("lastElement:" + lastElement);
        if (lastElement.startsWith("p")) {
            // check whether the firstElement was starting with p as if it was
            // this does not have to be added again
            if (!firstElement.startsWith("p"))
                finalString.append(interpretationOfResourcesWithFoundJokers);
            if (!interpretationOfResourcesWithFoundJokers.endsWith("-"))
                finalString.append("-");
            finalString.append("c:lastJoker");
        }
        if ("".equals(finalString.toString()))
            finalString.append(interpretationOfResourcesWithFoundJokers);
        // remove any "-" on the start or the beginning
        String returnString = null;
        if (finalString.toString().trim().endsWith("-"))
            returnString =
                            finalString.toString().substring(0,
                                            finalString.toString().length() - 1);
        else if (finalString.toString().trim().startsWith("-"))
            returnString =
                            finalString.toString().substring(1,
                                            finalString.toString().length());
        else
            returnString = finalString.toString();
        logger.info("Final string before generating sparql:" + returnString);
        return returnString;
    }

    /**
     * // check if each second element in the row is the property with // inverseProperty=true, in that case revert
     * first and third column for // example: a-b-c row has b as a property with inverseProperty=true, so // this row
     * becomes c-b-a, if d f and h are not invertedProperties than // the table becomes: // c-b-a // c-d-e // e-f-g //
     * g-h-i
     * 
     * @param newColumnData
     * @param elements
     * @return
     */
    public static List<List<List<OntologyElement>>> getInvertedTriplesIfPropertiesAreInverted(
                    List<List<List<OntologyElement>>> newColumnData,
                    List<List<OntologyElement>> elementsTable) {
        int positionOfPropertyElementWithinElements = 0;
        List<List<List<OntologyElement>>> invertedColumnData =
                        new ArrayList<List<List<OntologyElement>>>();
        for (List<List<OntologyElement>> row : newColumnData) {
            if ((elementsTable.size() > (positionOfPropertyElementWithinElements + 1))
                            && elementsTable.get(positionOfPropertyElementWithinElements + 1).get(0) instanceof PropertyElement) {
                // System.out
                // .println("checking if it's property................................................................");
                PropertyElement potentialProperty =
                                (PropertyElement) elementsTable
                                                .get(positionOfPropertyElementWithinElements + 1).get(0);
                if (potentialProperty.isInverseProperty()) {
                    // System.out.println("is inverted property.............");
                    // revert first and third element in this row
                    // save the first element in the temp element
                    List<OntologyElement> firstColumn = row.get(0);
                    List<OntologyElement> secondColumn = row.get(1);
                    List<OntologyElement> thirdColumn = row.get(2);
                    OntologyElement first = firstColumn.get(0);// the second 0 is because we dont care whichone of the
                                                               // overlapped we will take; they are all the same type
                    OntologyElement second = secondColumn.get(0);
                    // overide first element with third element
                    OntologyElement third = thirdColumn.get(0);
                    // row.add(0, thirdElement);
                    // third element takes values from temp elements
                    // row.add(2, tempElement);
                    List<List<OntologyElement>> newRow = new ArrayList<List<OntologyElement>>();
                    newRow.add(thirdColumn);
                    newRow.add(secondColumn);
                    newRow.add(firstColumn);
                    invertedColumnData.add(newRow);
                } else {
                    // add this row without changing anything
                    invertedColumnData.add(row);
                }
            } else {
                // this happens only when there is one element in the row
                invertedColumnData.add(row);
            }
            // }
            // now move to another triple
            positionOfPropertyElementWithinElements =
                            positionOfPropertyElementWithinElements + 2;
        }
        return invertedColumnData;
    }

    /**
     * // create list of list i.e. a 2d matrix/table) out of elements // so that each row contains 3 elements so that
     * the last element in a // row is the first element in the next row etc. for example if this is // the column
     * header: a-b-c-d-e-f-g-h-i then the newly created 'table' // will look like // a-b-c // c-d-e // e-f-g // g-h-i
     * 
     * @param elements
     * @return
     */
    public static List<List<List<OntologyElement>>> getListOfTriplesFromLinearForm(
                    List<List<OntologyElement>> elementsTable) {
        List<List<OntologyElement>> rowData = new ArrayList<List<OntologyElement>>();
        List<List<List<OntologyElement>>> newColumnData =
                        new ArrayList<List<List<OntologyElement>>>();
        List<OntologyElement> connectingElement = null;
        int numOfElements = elementsTable.size();
        for (int i = 0; i < numOfElements; i = i + 2) {
            // check if there is more than 1 element in the interpretation
            if (numOfElements > 1 && (i == (numOfElements - 1))) break;
            List<OntologyElement> currentElement = null;
            if (connectingElement != null) {
                currentElement = connectingElement;
            } else {
                currentElement = (List<OntologyElement>) elementsTable.get(i);
            }
            rowData.add(currentElement);
            if (numOfElements > (i + 1)) {
                List<OntologyElement> middleElement = (List<OntologyElement>) elementsTable.get(i + 1);
                rowData.add(middleElement);
            }
            if (numOfElements > (i + 2)) {
                connectingElement = (List<OntologyElement>) elementsTable.get(i + 2);
                rowData.add(connectingElement);
            }
            newColumnData.add(rowData);
            rowData = new ArrayList<List<OntologyElement>>();
        }
        return newColumnData;
    }

    public static Set<String> findVariations(String aliasLabel,
                    boolean separateCamelCasedWords) {
        long start = System.currentTimeMillis();
        Set<String> newAliasLabels = new HashSet<String>();
        // aliasLabel = aliasLabel.toLowerCase();
        if (aliasLabel != null && aliasLabel.trim().length() > 0) {
            if (aliasLabel.contains("_")) {
                String newText = aliasLabel.trim().replace("_", " ");
                if (newText != null && !"".equals(newText)) newAliasLabels.add(newText);
            }
            // if text is camel cased add space between words
            if (separateCamelCasedWords && aliasLabel.indexOf(" ") < 0) {
                String separatedCamelCase =
                                ExpressionFinder.findAndSeparateCamelCases(aliasLabel,
                                                CATConstants.REGEX_CAMEL_CASE, " ");
                if (aliasLabel != null && (!aliasLabel.equals(separatedCamelCase))) {
                    newAliasLabels.add(separatedCamelCase);
                }
            }
        }
        long end = System.currentTimeMillis();
        if ((end - start) > 100)
            System.out.println("Alias:" + aliasLabel + ",Finding variations for:"
                            + (end - start) + "ms");
        return newAliasLabels;
    }

    public static void main(String[] args) {
        StringUtil u = new StringUtil();
        String query = "p0-p1-i2-";
        // String query = "p0-";
        String answer = u.createCompleteString(query);
        String query1 = "c0-c1-i2-";
        // String query = "p0-";
        String answer1 = u.createCompleteString(query1);
    }
}
