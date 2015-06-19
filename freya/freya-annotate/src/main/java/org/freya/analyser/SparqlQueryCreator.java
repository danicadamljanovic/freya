package org.freya.analyser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.annotate.lucene.LuceneAnnotator;
import org.freya.model.ClassElement;
import org.freya.model.DatatypePropertyValueElement;
import org.freya.model.DatatypePropertyValueIdentifier;
import org.freya.model.InstanceElement;
import org.freya.model.InstanceListElement;
import org.freya.model.JokerElement;
import org.freya.model.OntologyElement;
import org.freya.model.PropertyElement;
import org.freya.model.QueryElement;
import org.freya.model.SerializableURI;
import org.freya.util.FreyaConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class provide methods for creating SPARQL queries from the list of OntologyElements
 * 
 * @author danica
 */
@Component
public class SparqlQueryCreator implements QueryCreator {
    protected static final Log logger = LogFactory.getLog(SparqlQueryCreator.class);

  @Autowired  LuceneAnnotator luceneAnnotator;


    /**
     * Generates sparql query from the set of triples
     * 
     * @param triples
     * @return
     */
    public QueryElement getQueryElementFromOntologyElements(List<List<OntologyElement>> elsTable)
                    throws Exception {
        QueryElement queryElement = new QueryElement();
        StringBuffer prefixPart =
                        new StringBuffer(
                                        "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        prefixPart.append("prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n");
        StringBuffer selectPart = new StringBuffer(FreyaConstants.SELECT);
        StringBuffer wherePart = new StringBuffer(" ").append("where {");
        StringBuffer orderPart = new StringBuffer(" ").append(" ORDER BY ");
        StringBuffer selectSet = new StringBuffer("");
        int flagSet=0;
        // DESC(?emp) max
        // ASC(?emp) min
        List<OntologyElement> previousColumn = null;
        // List<OntologyElement> nextColumn = null;
        int j = 0;
        for (List<OntologyElement> column : elsTable) {
            // skip the the element which is already processed (unless we
            // are in the first triple (to avoid repetition))
            if (!selectPart.toString().endsWith(FreyaConstants.SELECT)) selectPart.append(" ");
            OntologyElement sampleElement = column.get(0);
            logger.debug("This is element:" + sampleElement);
            if (sampleElement instanceof ClassElement) {
                // String typeRelation="rdf:type";
                String variable = ((ClassElement) sampleElement).getVariable();
                String typeRelation = " ?typeRelation" + variable;

                selectPart.append(" ?").append(variable);
                int numberOfElements = column.size();
                int currentIndex = 1;
                wherePart.append("{{ ");
                for (OntologyElement currentElement : column) {
                    wherePart.append(" ?").append(variable).append(" ").append(typeRelation)
                                    .append(" <")
                                    .append((((ClassElement) currentElement).getData()).toString())
                                    .append("> . ");
                    if (currentIndex < numberOfElements)
                        wherePart.append(" } UNION { ");
                    else
                        wherePart.append(" }}");
                    currentIndex++;
                }
                previousColumn = column;
            } else if (sampleElement instanceof JokerElement) {
                String type = ((JokerElement) sampleElement).getType();
                if (type.equals("class")) {
                    // for c:firstJoker,
                    // c:lastJoker
                    selectPart.append(" ?").append(((JokerElement) sampleElement).getVariable());
                } else if (type.equals("property")) {
                    JokerElement jokerElement = ((JokerElement) sampleElement);
                    selectPart.append(" ?").append(jokerElement.getVariable());
                    // add to where part previous joker next
                    // so, if it is starting with 'j' it is joker, meaning that
                    // there is not predefined property, but we will make a
                    // query
                    // with a joker, so there is no filter to be added to
                    // wherePart but we must add previous joker next
                    List<OntologyElement> nextColumn = null;
                    nextColumn = elsTable.get(j + 1);
                    String nextVariable = nextColumn.get(0).getVariable();
                    String previousVariable = previousColumn.get(0).getVariable();
                    wherePart.append(" {{ ?").append(nextVariable).append(" ").append(" ?")
                                    .append(jokerElement.getVariable()).append(" ").append("?")
                                    .append(previousVariable).append(" } UNION ");
                    wherePart.append(" { ?").append(previousVariable).append(" ").append(" ?")
                                    .append(jokerElement.getVariable()).append(" ").append("?")
                                    .append(nextVariable).append(" }} ");
                }
                previousColumn = column;
            } else if (sampleElement instanceof DatatypePropertyValueElement) {
                if (!selectPart.toString().endsWith(FreyaConstants.SELECT))
                    selectPart.append(" ");
                String variableName = ((DatatypePropertyValueElement) sampleElement).getVariable();
                selectPart.append(" ?").append(variableName);
                String propertyValue =
                                ((DatatypePropertyValueIdentifier) ((DatatypePropertyValueElement) sampleElement)
                                                .getData()).getPropertyValue().toString();
                String propertyUri =
                                ((DatatypePropertyValueIdentifier) ((DatatypePropertyValueElement) sampleElement)
                                                .getData()).getPropertyUri().toString();
                wherePart.append(" FILTER REGEX(str(?").append(variableName).append(")")
                                .append(", \"^").append(propertyValue);
                wherePart.append("$\",\"i\") . ");
                Set<String> uris = new HashSet<String>();
                if (previousColumn != null && previousColumn.size() > 0)
                    uris.add(previousColumn.get(0).getVariable());
                int index = 1;
                if (uris.size() > 0) wherePart.append(" FILTER (");
                for (String variable : uris) {
                    wherePart.append("?").append(variable).append("=<").append(propertyUri)
                                    .append(">");
                    if (index < uris.size()) wherePart.append(" || ");
                    index++;
                }
                if (uris.size() > 0) wherePart.append(")");
                // regex(?d1, '^5th president of the United States$', 'i')
                // wherePart
                // .append(" filter isLiteral(?")
                // .append(variableName)
                // .append("). filter (?")
                // .append(variableName)
                // .append("=\"")
                // .append(
                // ((DatatypePropertyValueIdentifier)((DatatypePropertyValueElement)element)
                // .getData()).getPropertyValue().toString())
                // .append("\"");
                // String propertyUri =
                // ((DatatypePropertyValueIdentifier)((DatatypePropertyValueElement)element)
                // .getData()).getPropertyUri().toString();
                // List<String> range =
                // new ArrayList(luceneAnnotator.findPropertyRange(propertyUri));
                // if(range != null && range.size() > 0)
                // wherePart.append("^^<").append(range.get(0).toString()).append(">");
                // // add ^^propertyRange
                // wherePart.append(") . ");
                previousColumn = column;
            } else if (sampleElement instanceof PropertyElement) {
                PropertyElement property = (PropertyElement) sampleElement;
                selectPart.append(" ?").append(property.getVariable());
                List<OntologyElement> nextColumn = null;
                nextColumn = elsTable.get(j + 1);
                String previousVariable = previousColumn.get(0).getVariable();
                if (previousColumn != null && previousVariable.startsWith("d")) { // this
                    // is because if we have d0 p1 i2 it is always the case that
                    // this
                    // should be reverted to read i2 p1 do
                    property.setInverseProperty(true);
                }
                // Added a module to support set operations in Freya
                if (property.isDatatypeProperty()) {
                    logger.debug("Property is datatype property and therefore it differs from the others...");
                    logger.debug("Previous is:" + previousColumn.toString());
                    logger.debug("Next is:" + nextColumn.toString());
                    PropertyElement propertySample = (PropertyElement) column.get(0);
                    if (propertySample.getFunction() != null && ( FreyaConstants.SUM_FUNCTION.equals(propertySample.getFunction()) || FreyaConstants.AVG_FUNCTION.equals(propertySample.getFunction())))
                    {
                    	selectSet=checkSetFunctionInDataProperty(column, previousColumn,nextColumn,propertySample.getFunction());
                    	flagSet=1;
                    }
                    wherePart.append(getWherePartForDatatypeProperty(column, previousColumn,
                                    nextColumn));
                    orderPart.append(getOrderPartForDatatypeProperty(column, previousColumn,
                                    nextColumn));
                } else if (nextColumn != null && previousColumn != null) {
                    // && ((next instanceof JokerElement) || (previous instanceof
                    // JokerElement))) {
                    if (property.isInverseProperty()
                                    || previousColumn.get(0) instanceof DatatypePropertyValueElement) {
                        // exchange previous and next
                        List<OntologyElement> tempColumn = nextColumn;
                        previousColumn = nextColumn;
                        nextColumn = tempColumn;
                    }
                    // create union of queries as we do not know the
                    // direction of property so we get union of both options
                    wherePart.append(getWhereStringForPropertyUsingUnion(column,
                                    previousColumn.get(0), nextColumn.get(0)));
                } else {
                    if (property.getRange() == null && property.getDomain() == null)
                        wherePart.append(getWhereStringForPropertyUsingUnion(column,
                                        previousColumn.get(0), nextColumn.get(0)));
                    else {
                        logger.info("Previous element:" + previousColumn.toString());
                        logger.info("Next element:" + nextColumn.toString());
                        String nextVariable = nextColumn.get(0).getVariable();
                        property.setInverseProperty(checkWhetherPropertyShouldBeInverted(
                                        previousColumn.get(0), property, nextColumn.get(0)));
                        logger.info("uh ovo nije bas dobro ovako jer sta ako je jedan property inverted a drugi ne...");
                        if (property.isInverseProperty()) {
                            wherePart.append(" ?").append(nextVariable).append(" ").append(" ?")
                                            .append(property.getVariable()).append(" ").append("?")
                                            .append(previousVariable);
                            wherePart.append(" . filter (");
                            int index = 1;
                            for (OntologyElement aProperty : column) {
                                wherePart.append("?").append(aProperty.getVariable()).append("=<")
                                                .append(aProperty.getData().toString())
                                                .append("> . ");
                                if (index < column.size()) wherePart.append(" || ");
                                index++;
                            }
                            wherePart.append(")");
                        } else {
                            wherePart.append(" ?").append(previousVariable);
                            wherePart.append(" ").append(" ?").append(property.getVariable())
                                            .append(" ");
                            wherePart.append("?").append(nextVariable);
                            wherePart.append(" . filter (");
                            int index = 1;
                            for (OntologyElement aProperty : column) {
                                wherePart.append("?").append(aProperty.getVariable()).append("=<")
                                                .append(aProperty.getData().toString())
                                                .append("> . ");
                                if (index < column.size()) wherePart.append(" || ");
                                index++;
                            }
                            wherePart.append(")");
                        }
                    }
                }
                previousColumn = column;
            } else if (sampleElement instanceof InstanceElement) {
                // String uri = ((InstanceElement)element).getData().toString();
                // selectPart.append(" ?")
                // .append(((OntologyElement)element).getVariable());
                // wherePart.append(" filter (?").append(
                // ((OntologyElement)element).getVariable()).append("=<").append(
                // uri).append(">) . ");
                // previous = element;
                logger.info("ovo ne bi trebalo da se desi!!!!!!!!!!!!!! svi elementi su InstanceList nema vise InstanceElement!??????");
            } else if (sampleElement instanceof InstanceListElement) {
                // String typeRelation=" rdf:type ";
                String variable = ((InstanceListElement) sampleElement).getVariable();
                String typeRelation = " ?typeRelation" + variable;
                // String typeRelation = " ?typeRelation ";
                Set<SerializableURI> urisSet = new HashSet<SerializableURI>();
                for (OntologyElement element : column) {
                    urisSet.addAll(((InstanceListElement) element).getData());
                }
                List<SerializableURI> uris = new ArrayList<SerializableURI>(urisSet);
                String classURI = null;
                if (((InstanceListElement) sampleElement).getClassURI() != null)
                    classURI = ((InstanceListElement) sampleElement).getClassURI().toString();
                selectPart.append(" ?").append(((InstanceListElement) sampleElement).getVariable());
                if (classURI != null) {
                    wherePart.append(" ?")
                                    .append(((InstanceListElement) sampleElement).getVariable())
                                    .append(typeRelation).append(" <").append(classURI)
                                    .append("> . ");
                } else {
                    wherePart.append(" ?").append(((OntologyElement) sampleElement).getVariable())
                                    .append(typeRelation).append("?instType").append(" . ");
                }
                wherePart.append(" filter (");
                for (int i = 0; i < uris.size(); i++) {
                    String uri = uris.get(i).toString();
                    wherePart.append("?")
                                    .append(((InstanceListElement) sampleElement).getVariable())
                                    .append("=<").append(uri).append(">");
                    if (i < (uris.size() - 1)) wherePart.append(" || ");
                }
                wherePart.append(") . ");
                previousColumn = column;
            }
            j = j + 1;
        }// end of iterating through triples
         // }
        StringBuffer theWholeQuery = new StringBuffer("");
        if(flagSet == 1)
        {
        	theWholeQuery.append(prefixPart).append(selectSet.toString());
            theWholeQuery.append(wherePart.toString()).append("}");
            logger.debug("theWhole query after changes:"+theWholeQuery);
            flagSet=0;
        }
        else if ((!wherePart.toString().endsWith("where ?"))
                        && (!selectPart.toString().endsWith(FreyaConstants.SELECT))) {
            theWholeQuery.append(prefixPart).append(selectPart.toString());
            // if (!wherePart.toString().endsWith("where ?"))
            theWholeQuery.append(wherePart.toString()).append("}");
        }
        if (!orderPart.toString().endsWith(" ORDER BY "))
            theWholeQuery.append(orderPart.toString());
        if (theWholeQuery != null && !theWholeQuery.toString().equals(""))
            theWholeQuery.append(" LIMIT 10000");

        if (theWholeQuery != null && !theWholeQuery.toString().equals("")) {
            queryElement.setQueryString(theWholeQuery.toString());
            // queryElement.setModifiers(modifiers);
            queryElement.setOntologyElements(elsTable);
        }
        return queryElement;
    }

    boolean checkWhetherPropertyShouldBeInverted(OntologyElement previous,
                    PropertyElement property, OntologyElement next) {
        boolean invert = false;
        String domain = property.getDomain();
        // if next=domain invert=true;
        Set<String> classUris = getClassUris(next);
        if (classUris.contains(domain)) {
            invert = true;
            return invert;
        }
        String range = property.getRange();
        // if previous=range invert=true;
        classUris = getClassUris(previous);
        if (classUris.contains(range)) {
            invert = true;
            return invert;
        }
        return invert;
    }

    Set<String> getClassUris(OntologyElement element) {
        List<String> classUris = new ArrayList<String>();
        if (element instanceof InstanceElement) {
            classUris = ((InstanceElement) element).getClassURIList();
        } else if (element instanceof InstanceListElement) {
            classUris = ((InstanceListElement) element).getClassURIList();
        } else if (element instanceof ClassElement) {
            String classUri = ((ClassElement) element).getData().toString();
            classUris.add(classUri);
        }
        return new HashSet(classUris);
    }

    /**
     * @param property
     * @param previous
     * @param next
     * @return
     */
    String getWherePartForDatatypeProperty(List<OntologyElement> propertyColumn,
                    List<OntologyElement> previousColumn, List<OntologyElement> nextColumn) {
        // List<PropertyElement> propertyColumn=(List<PropertyElement>)propColumn;
        StringBuffer wherePart = new StringBuffer("");
        OntologyElement governor = ((PropertyElement) propertyColumn.get(0)).getGovernor();
        OntologyElement sampleNext = nextColumn.get(0);
        OntologyElement samplePrevious = previousColumn.get(0);
        String propertyVariable = propertyColumn.get(0).getVariable();
        if ((governor != null && sampleNext.getData() != null && samplePrevious.getData() != null && governor
                        .getData().toString().equals(sampleNext.getData().toString()))
                        || (samplePrevious instanceof JokerElement && !(sampleNext instanceof DatatypePropertyValueElement))) {
            // obrni ih ovo znaci d p c/i/j
            wherePart.append(" ?").append(((OntologyElement) sampleNext).getVariable()).append(" ")
                            .append(" ?").append(propertyVariable).append(" ").append("?")
                            .append(((OntologyElement) samplePrevious).getVariable()).append(" . ");
            int index = 1;
            wherePart.append(" FILTER (");
            for (OntologyElement property : propertyColumn) {
                wherePart.append(" ?").append(propertyVariable).append("=<")
                                .append(((PropertyElement) property).getData().toString())
                                .append(">  ");
                if (index < propertyColumn.size()) wherePart.append(" || ");
                index++;
            }
            wherePart.append(") . ");
            logger.debug("exchanging elements...");
        } else {
            // c/i/j p d
            wherePart.append(" ?").append(((OntologyElement) samplePrevious).getVariable())
                            .append(" ").append(" ?").append(propertyVariable).append(" ")
                            .append("?").append(((OntologyElement) sampleNext).getVariable())
                            .append(" . ");
            int index = 1;
            wherePart.append(" FILTER (");
            for (OntologyElement property : propertyColumn) {
                wherePart.append(" ?").append(propertyVariable).append("=<")
                                .append(((PropertyElement) property).getData().toString())
                                .append(">  ");
                if (index < propertyColumn.size()) wherePart.append(" || ");
                index++;
            }
            wherePart.append(") . ");
        }
        return wherePart.toString();
    }
    
    /**
     * Making Room for checking SET Functionality support
     */
    StringBuffer checkSetFunctionInDataProperty(List<OntologyElement> propertyColumn,
                    List<OntologyElement> previousColumn, List<OntologyElement> nextColumn,String function)
    {
    	
    	OntologyElement governor = ((PropertyElement) propertyColumn.get(0)).getGovernor();
        String nextVariable = null;
        OntologyElement nextSample = nextColumn.get(0);
        OntologyElement previousSample = previousColumn.get(0);
        PropertyElement propertySample = (PropertyElement) propertyColumn.get(0);
        if ((governor != null && nextSample.getData() != null && previousSample.getData() != null && governor
                        .getData().toString().equals(nextSample.getData().toString()))
                        || (previousSample instanceof JokerElement && !(nextSample instanceof DatatypePropertyValueElement))) {
            // obrni ih
            nextVariable = previousSample.getVariable();
            // this means that this element holds the answer to the question
            previousSample.setAnswer(true);
        } else {
            nextVariable = nextSample.getVariable();
            // this means that this element holds the answer to the question
            nextSample.setAnswer(true);
        }
        if (FreyaConstants.SUM_FUNCTION.equals(function))
        {
        	StringBuffer selectSetTemp = new StringBuffer(FreyaConstants.SELECT_SUM);
        	selectSetTemp.append("?").append(nextVariable).append("))").append(" AS ?JokerElement").append(")");
        	return selectSetTemp;
        }
        else if (FreyaConstants.AVG_FUNCTION.equals(function))
        {
        	StringBuffer selectSetTemp = new StringBuffer(FreyaConstants.SELECT_AVG);
        	selectSetTemp.append("?").append(nextVariable).append("))").append(" AS ?JokerElement").append(")");
        	return selectSetTemp;
        }
        else
        {
        	return null;
        }
   }

    /**
     * making order part for the datatype property...
     * 
     * @param property
     * @param previous
     * @param next
     * @return
     */
    String getOrderPartForDatatypeProperty(List<OntologyElement> propertyColumn,
                    List<OntologyElement> previousColumn, List<OntologyElement> nextColumn) {
        StringBuffer orderPart = new StringBuffer("");
        OntologyElement governor = ((PropertyElement) propertyColumn.get(0)).getGovernor();
        String nextVariable = null;
        OntologyElement nextSample = nextColumn.get(0);
        OntologyElement previousSample = previousColumn.get(0);
        PropertyElement propertySample = (PropertyElement) propertyColumn.get(0);
        if ((governor != null && nextSample.getData() != null && previousSample.getData() != null && governor
                        .getData().toString().equals(nextSample.getData().toString()))
                        || (previousSample instanceof JokerElement && !(nextSample instanceof DatatypePropertyValueElement))) {
            // obrni ih
            nextVariable = previousSample.getVariable();
            // this means that this element holds the answer to the question
            previousSample.setAnswer(true);
        } else {
            nextVariable = nextSample.getVariable();
            // this means that this element holds the answer to the question
            nextSample.setAnswer(true);
        }
        if (
        // element.isMainSubject() &&
        propertySample.getFunction() != null) {
            // add type like this order by asc (xsd:float(?x))
            String longType = propertySample.getRange();
            String type = null;
            if (longType != null && longType.startsWith("http://www.w3.org/2001/XMLSchema#")) {
                logger.debug("Type BEFORE replacement:" + type);
                type = longType.replaceFirst("http://www.w3.org/2001/XMLSchema#", "xsd:");
                logger.debug("Type AFTER replacement:" + type);
                /*
                 * this is cheating, but not sure how to overcome this problem except like this...
                 */
                type = type.replaceFirst("float", "double");
                logger.debug("Type AFTER replacing float into double:" + type);
            }
            if (FreyaConstants.MAX_FUNCTION.equals(propertySample.getFunction())) {
                logger.debug("Property range used for sorting: " + type);
                orderPart.append(" DESC(");
                if (type != null && !type.equals("")) orderPart.append(type).append("(");
                orderPart.append("?").append(nextVariable).append(")");
                if (type != null && !type.equals("")) orderPart.append(") ");
            } else if (FreyaConstants.MIN_FUNCTION.equals(propertySample.getFunction())) {
                orderPart.append(" ASC(");
                if (type != null && !type.equals("")) orderPart.append(type).append("(");
                orderPart.append("?").append(nextVariable).append(")");
                if (type != null && !type.equals("")) orderPart.append(") ");
            } else if (FreyaConstants.SUM_FUNCTION.equals(propertySample.getFunction())) {
                //logger.info("sum function does not exist in sparql and "
                  //              + "therefore we need to implement it differently..."
                    //            + "not yet done...");
            }
        }
        return orderPart.toString();
    }

    /**
     * makes where part using previous, property and next element inserts union
     * 
     * @param property
     * @param previous
     * @param next
     * @return
     */
    String getWhereStringForPropertyUsingUnion(List<OntologyElement> propertyColumn,
                    OntologyElement previous, OntologyElement next) {
        logger.info("ovde sam");
        StringBuffer wherePart = new StringBuffer("");
        String propertyVariable = propertyColumn.get(0).getVariable();
        wherePart.append(" { { ?").append(next.getVariable()).append(" ?").append(propertyVariable)
                        .append(" ?").append(previous.getVariable()).append("} UNION { ?")
                        .append((previous).getVariable()).append(" ?").append(propertyVariable)
                        .append(" ?").append((next).getVariable()).append("}");
        wherePart.append(" . FILTER (");
        int index = 1;
        for (OntologyElement property : propertyColumn) {
            wherePart.append("?").append(propertyVariable).append("=<")
                            .append(property.getData().toString()).append(">");
            if (index < propertyColumn.size()) wherePart.append(" || ");
            index++;
        }
        wherePart.append(")");
        wherePart.append(" } ");
        return wherePart.toString();
    }
}
