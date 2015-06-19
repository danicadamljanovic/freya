package org.freya.oc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.annotate.lucene.LuceneAnnotator;
import org.freya.format.JsonCreator;
import org.freya.model.Annotation;
import org.freya.model.ClassElement;
import org.freya.model.DatatypePropertyValueElement;
import org.freya.model.DatatypePropertyValueIdentifier;
import org.freya.model.InstanceElement;
import org.freya.model.InstanceListElement;
import org.freya.model.OntologyElement;
import org.freya.model.PropertyElement;
import org.freya.model.Question;
import org.freya.model.Score;
import org.freya.model.SemanticConcept;
import org.freya.model.SerializableURI;
import org.freya.rdf.query.CATConstants;
import org.freya.util.AnnotationOffsetComparator;
import org.freya.util.FreyaConstants;
import org.freya.util.OntologyElementComparator;
import org.freya.util.SemanticConceptListComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OCCreator {
    private static final Log logger = LogFactory.getLog(OCCreator.class);

    private AtomicInteger idCounter = new AtomicInteger();
    @Autowired private JsonCreator jsonCreator;
    @Autowired LuceneAnnotator luceneAnnotator;


    /**
     * takes a list of overlapped elements
     * 
     * @param lookups
     * @return
     */
    private List<List<OntologyElement>> getOntologyElementsLucene(
                    Set<Annotation> lookups) {
        logger.info("Received:" + lookups.size()
                        + " annotations, now generating Ontology Elements.");
        List<List<OntologyElement>> ontologyElements =
                        new ArrayList<List<OntologyElement>>();
        AnnotationOffsetComparator oc = new AnnotationOffsetComparator();
        List<Annotation> lookupsList =
                        new ArrayList<Annotation>();
        if (lookups != null) lookupsList.addAll(lookups);
        Collections.sort(lookupsList, oc);
        List<List<Annotation>> overlappedGroups =
                        getOverlappedGroupsOfLuceneAnnotations(lookupsList);
        logger.debug("from above annotations generated:" + overlappedGroups.size()
                        + " columns with annoations, now generating ontology elements...");
        ontologyElements =
                        getOverlappedGroupsOfOntologyElementsLucene(overlappedGroups);
        int col = 0;
        List<List<OntologyElement>> toRemove =
                        new ArrayList<List<OntologyElement>>();
        List<List<OntologyElement>> toAdd = new ArrayList<List<OntologyElement>>();
        for (List<OntologyElement> column : ontologyElements) {
            // logger.debug("column:" + col + " size:" + column.size());
            HashMap<String, List<OntologyElement>> map =
                            new HashMap<String, List<OntologyElement>>();
            for (OntologyElement el : column) {
                List<OntologyElement> list = map.get(el.getAnnotation().getText());
                if (list == null) list = new ArrayList<OntologyElement>();
                list.add(el);
                map.put(el.getAnnotation().getText(), list);
            }
            if (map.keySet().size() > 1) {
                logger
                                .debug("Now making separate column for overlapped OCs which have different text...");
                // this means that overlapped annotations have different text so
                // separate this one column into text.size
                toRemove.add(column);
                List keys = new ArrayList(map.keySet());
                for (int i = 0; i < map.keySet().size(); i++) {
                    String key = (String) keys.get(i);
                    toAdd.add(map.get(key));
                }
            }
            col++;
        }
        logger.info("Generated " + ontologyElements.size() + " columns.");
        ontologyElements.removeAll(toRemove);
        logger
                        .info("Removed those that will be added as split in a moment, this many left: "
                                        + ontologyElements.size() + " columns.");
        ontologyElements.addAll(toAdd);
        logger.info("Added the above split column, there are  "
                        + ontologyElements.size()
                        + " columns (because overlapped OCs have different text).");
        return ontologyElements;
    }

    /**
     * lucene generates semanticElements from ontologyElements found by Lucene
     * 
     * @param oElements
     * @return
     */
    public List<List<SemanticConcept>> getSemanticConcepts(
                    Set<Annotation> lookups, Question question) {
        List<List<OntologyElement>> oElements = getOntologyElementsLucene(lookups);
        logger.info("from " + lookups.size() + " generated ontogy elements:"
                        + oElements.size() + " columns");
        int colN = 0;
        List<List<SemanticConcept>> semanticElements =
                        new ArrayList<List<SemanticConcept>>();
        for (List<OntologyElement> ontologyElements : oElements) {
            // logger.info("col " + colN + ontologyElements.toString());
            List<SemanticConcept> scColumn = new ArrayList<SemanticConcept>();
            // so this is now a list of lists with grouped ontology elements, this is
            // not only overlapped elements but this grouping is within one column
            List<OntologyElement> listOfGroupedElements =
                            groupOntologyElements(ontologyElements);
            logger.info("After grouping elements these shrink into:"
                            + listOfGroupedElements.size());
            // + " elements and these are:" + listOfGroupedElements.toString());
            // listOfGroupedElements=OCUtil.filterInstanceClass(listOfGroupedElements);
            // sada ovde uradi filter class instance: ako su jedno do drugog izbrisi class i takodje sve ostale instance
            // daj im neki los score

            for (OntologyElement ontologyElement : listOfGroupedElements) {
                // here we want to check whether any of ontologyElements needs to go
                // together into SemanticConcept
                // for example if they share classURI and are InstanceElements than they
                // are grouped together
                SemanticConcept sc = new SemanticConcept();
                sc.setOntologyElement(ontologyElement);
                scColumn.add(sc);
            }
            semanticElements.add(scColumn);
            colN++;
        }
        SemanticConceptListComparator c = new SemanticConceptListComparator();
        Collections.sort(semanticElements, c);
        return semanticElements;
    }

    /**
     * grouping by type: group all elements with different instanceURI and same classURI so that they will be
     * represented by one SemanticConcept and InstanceListElement input is a list of overlapped ontology elements: those
     * that cover the same span
     * 
     * @return
     */
    List<OntologyElement> groupOntologyElements(
                    List<OntologyElement> ontologyElements) {
        List<OntologyElement> finalList = new ArrayList<OntologyElement>();
        Map<String, List<OntologyElement>> map =
                        new HashMap<String, List<OntologyElement>>();
        logger.info("There are " + ontologyElements.size() + " elements.");
        long counter = 0;
        for (OntologyElement el : ontologyElements) {
            if (el instanceof InstanceElement) {
                String classURI = ((InstanceElement) el).getClassURI().toString();
                Collections.sort(((InstanceElement) el).getClassURIList());// sortiraj
                                                                           // kako bi
                                                                           // iste
                                                                           // instance
                                                                           // bile na
                                                                           // istom
                String classURIList =
                                ((InstanceElement) el).getClassURIList().toString();
                String text = ((InstanceElement) el).getAnnotation().getText();
                // logger.debug("Text is:" + text);
                // String key = "I" + classURI + text;
                String key = "I" + classURIList + text;
                List<OntologyElement> lista = map.get(key);
                if (lista == null) lista = new ArrayList<OntologyElement>();
                lista.add(el);
                map.put(key, lista);
            } else if (el instanceof DatatypePropertyValueElement) {
                DatatypePropertyValueIdentifier dpve =
                                (DatatypePropertyValueIdentifier) ((DatatypePropertyValueElement) el)
                                                .getData();
                String propertyURI = dpve.getPropertyUri();
                String propertyValue = dpve.getPropertyValue();
                String text =
                                ((DatatypePropertyValueElement) el).getAnnotation().getText();
                // logger.debug("Text is:" + text);
                String key = "D" + propertyURI + propertyValue + text;
                List<OntologyElement> lista = map.get(key);
                if (lista == null) lista = new ArrayList<OntologyElement>();
                lista.add(el);
                map.put(key, lista);
            } else {
                // for any other element just generate a list and return it with one
                // element
                finalList.add(el);
                // ?
            }
            // logger.debug("Now finished no:"+counter);
            counter++;
        }
        logger.debug("Finished puting elements in the map, now rearanging t"
                        + "hem i.e. making InstanceList or making a set of URIs for DTPV:");
        for (String classURIAndText : map.keySet()) {
            List<OntologyElement> thoseGrouped = map.get(classURIAndText);
            logger.info("Key:" + classURIAndText + ": " + thoseGrouped.size()
                            + " elements.");
            // if(thoseGrouped != null && thoseGrouped.size() == 1) {
            // // do not group as this is only one instance element then just add it
            // finalList.add(thoseGrouped.get(0));
            // continue;
            // }
            // is it instance or dtpv
            OntologyElement sample = thoseGrouped.get(0);
            if (sample instanceof InstanceElement) {
                InstanceListElement instanceListElement = new InstanceListElement();
                // this means we now need to generate a new OntologyElement for now
                // the only supported is InstanceListElement which contains list of
                // instanceURIs and classURI
                List<SerializableURI> uris = new ArrayList<SerializableURI>();
                for (OntologyElement el : thoseGrouped) {
                    uris.add((SerializableURI) el.getData());
                }
                instanceListElement.setData(uris);
                instanceListElement.setAlreadyAdded(sample.isAlreadyAdded());
                instanceListElement.setVariable(sample.getVariable());
                instanceListElement.setAnnotation(sample.getAnnotation());
                instanceListElement.setAnswer(sample.isAnswer());
                instanceListElement.setFunction(sample.getFunction());
                instanceListElement.setMainSubject(sample.isMainSubject());
                Set<String> classUriList = new HashSet<String>();
                for (OntologyElement el : thoseGrouped) {
                    List<String> tempList = ((InstanceElement) el).getClassURIList();
                    String classUri = ((InstanceElement) el).getClassURI().toString();
                    if (tempList == null || tempList.size() == 0) {
                        classUriList.add(classUri);
                    } else {
                        classUriList.addAll(tempList);
                    }
                }
                instanceListElement.setClassURIList(new ArrayList(classUriList));
                instanceListElement.setClassURI(((InstanceElement) thoseGrouped.get(0))
                                .getClassURI());
                finalList.add(instanceListElement);
            } else if (sample instanceof DatatypePropertyValueElement) {
                Set<SerializableURI> instanceUriList = new HashSet<SerializableURI>();
                DatatypePropertyValueIdentifier dpve =
                                (DatatypePropertyValueIdentifier) ((DatatypePropertyValueElement) sample)
                                                .getData();
                for (OntologyElement el : thoseGrouped) {
                    DatatypePropertyValueIdentifier tmp =
                                    (DatatypePropertyValueIdentifier) ((DatatypePropertyValueElement) el)
                                                    .getData();
                    // at this stage each dpv has only one element in instancelist that's
                    // why we can do get(0)
                    instanceUriList.add(tmp.getInstanceURIs().get(0));
                }
                dpve.setInstanceURIs(new ArrayList(instanceUriList));
                // dodaj klasu? nee...
                finalList.add(sample);
            }
        }
        logger
                        .debug("Finished rearranging the elements and grouping those with same classURI/predURi+value");
        OntologyElementComparator comparator = new OntologyElementComparator();
        Collections.sort(finalList, comparator);
        logger.debug("Finished sorting elements.");
        return finalList;
    }

    /**
     * translates annotations into OntologyElements
     * 
     * @return
     */
    public List<List<OntologyElement>> getOverlappedGroupsOfOntologyElementsLucene(
                    List<List<Annotation>> overlappedGroups) {
        List<List<OntologyElement>> tableOfOntologyElements =
                        new ArrayList<List<OntologyElement>>();
        // each column represents overlapped annotations
        int col = 0;
        for (List<Annotation> column : overlappedGroups) {
            List<OntologyElement> newColumn = getOntologyElementsLucene(column);
            tableOfOntologyElements.add(newColumn);
            logger
                            .info("Generating " + newColumn.size() + " elements for col:"
                                            + col);
            // + " these are: " + newColumn.toString());
            col++;
        }
        return tableOfOntologyElements;
    }

    /**
     * translates annotations into ontology elements of various types: class, instance, class, property and
     * datatypepropertyValue this method does whatever old OntoResTransformer used to do
     * 
     * @param row
     * @return
     */
    List<OntologyElement> getOntologyElementsLucene(
                    List<Annotation> column) {
        List<OntologyElement> newRow = new ArrayList<OntologyElement>();
        for (Annotation cell : column) {
            OntologyElement element = null;
            String type = (String) cell.getFeatures().get(CATConstants.TYPE_FEATURE);
            Float solrScore = (Float) cell.getFeatures().get(FreyaConstants.SCORE);
            
            if (CATConstants.FEATURE_TYPE_CLASS.equals(type)) {
                element = new ClassElement();
                SerializableURI uri;
                try {
                    uri = new SerializableURI((String) cell.getFeatures().get(
                                    CATConstants.FEATURE_URI), false);

                    // transform element to be ClassElement
                    element.setData(uri);
                    Double specificityScore = 0.0;
                    if (luceneAnnotator.getSpecificityScores() != null) {
                        specificityScore =
                                        (Double) luceneAnnotator.getSpecificityScores()
                                                        .get(uri);
                    }


                    Score score = new Score();
                    if (specificityScore!=null)
                    score.setSpecificityScore(specificityScore);

                    if (cell != null && cell.getFeatures() != null
                                    && cell.getFeatures().get("score") != null)
                        score.setSimilarityScore((Float) cell.getFeatures().get("score"));
                    element.setScore(score);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (CATConstants.TYPE_INSTANCE.equals(type)) {
                element = new InstanceElement();
                String classUri =
                                (String) cell.getFeatures().get(CATConstants.CLASS_URI);
                String uriString =
                                (String) cell.getFeatures().get(CATConstants.FEATURE_URI);
                SerializableURI uri;
                try {
                    uri = new SerializableURI(uriString, false);

                    element.setData(uri);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                List<String> classURIList =
                                (List<String>) cell.getFeatures().get(
                                                CATConstants.CLASS_URI_LIST);
                if (classURIList == null || classURIList.size() == 0)
                    classURIList.add(classUri);
                ((InstanceElement) element).setClassURIList(classURIList);
                // logger.info("Setting class uri list of size "+classURIList.size());
                if (classUri != null) {
                    // }
                    // Map specificityScoreSet = jsonCreator.getOntology2Map()
                    // .getSpecificityScores();
                    //
                    // Double specificityScore = (Double) jsonCreator
                    // .getOntology2Map().getSpecificityScores().get(classUri);
                    SerializableURI classUriUri;
                    try {
                        classUriUri = new SerializableURI(classUri, false);

                        ((InstanceElement) element).setClassURI(classUriUri);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    logger.info("Class URI for the instance:" + uriString
                                    + " is NULLLLLLLLLLLLLLLLLLLLLlll; this should not happen");
                }
                Double specificityScore = 0.0;
                Score score = new Score();
                score.setSpecificityScore(specificityScore * 1.2);
                if (cell != null && cell.getFeatures() != null
                                && cell.getFeatures().get("score") != null)
                    score.setSimilarityScore((Float) cell.getFeatures().get("score"));
                element.setScore(score);// this times 1.2 is
                // because we want to assign higher score to instances in comparison to
                // classes
            } else if (CATConstants.TYPE_PROPERTY.equals(type)) {
                String propertyUri = (String) cell.getFeatures().get("URI");
                element = new PropertyElement();
                SerializableURI uri;
                try {
                    uri = new SerializableURI(propertyUri, false);

                    element.setData(uri);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                boolean isDatatypeProperty =
                                luceneAnnotator
                                                .isItDatatypeProperty(propertyUri);
                // if(datatypePropertiesList.contains(propertyUri))
                ((PropertyElement) element).setDatatypeProperty(isDatatypeProperty);
                Set<String> rangeSet =
                                luceneAnnotator
                                                .findPropertyRange(propertyUri);
                String range = null;
                Iterator<String> it = null;
                if (rangeSet != null) it = rangeSet.iterator();
                if (it != null && it.hasNext()) {
                    range = it.next();
                    ((PropertyElement) element).setRange(range);
                }
                Set<String> domainSet =
                                luceneAnnotator
                                                .findPropertyDomain(propertyUri);
                String domain = null;
                Iterator<String> itDomain = null;
                if (domainSet != null) itDomain = domainSet.iterator();
                if (itDomain != null && itDomain.hasNext()) {
                    domain = itDomain.next();
                    ((PropertyElement) element).setDomain(domain);
                }
            } else if (CATConstants.TYPE_DATATYPE_PROPERTY_VALUE.equals(type)) {
                element = new DatatypePropertyValueElement();
                // String propertyValue = (String) cell.getFeatures().get(
                // "propertyValue");
                SerializableURI instanceURI;
                try {
                    instanceURI = new SerializableURI((String) cell.getFeatures().get(
                                    CATConstants.FEATURE_INSTANCE_URI), false);

                    String propertyUri =
                                    (String) cell.getFeatures().get(
                                                    CATConstants.FEATURE_PROPERTY_URI);
                    DatatypePropertyValueIdentifier dtvIdentifier =
                                    new DatatypePropertyValueIdentifier();
                    dtvIdentifier.setPropertyUri(propertyUri);
                    dtvIdentifier.setPropertyValue(cell.getText());
                    List<SerializableURI> instanceUriList = new ArrayList();
                    instanceUriList.add(instanceURI);

                    dtvIdentifier.setInstanceURIs(instanceUriList);
                    element.setData(dtvIdentifier);
                } catch (Exception e) {
                  logger.info(e.getMessage());
                }
            }
            Score newScore = new Score();
            newScore.setSolrScore(solrScore);
            element.setScore(newScore);
            element.setAnnotation(cell);
            if (element != null) newRow.add(element);
        }
        return newRow;
    }

    /**
     * returns a list of lists of annotations in case they overlap; if there is no overlapped annotations then this will
     * be a list of lists with 1 element only
     * 
     * @param lookups
     * @return
     */
    public List<List<Annotation>> getOverlappedGroupsOfLuceneAnnotations(
                    List<Annotation> lookups) {
        List<Annotation> processedAnnotations =
                        new ArrayList<Annotation>();
        List<List<Annotation>> overlappedGroups =
                        new ArrayList<List<Annotation>>();
        // generate a list of lists of annotations where these second list is
        // overlapped annotations
        for (Annotation annotation : lookups) {
            // System.out
            // .println("****************************processing started...for...\n"
            // + annotation.toString());
            if (processedAnnotations.contains(annotation)) {
                // System.out.println("****************************skipping\n");
                continue;
            }
            processedAnnotations.add(annotation);
            List<Annotation> aCopyOfAllAnnotations =
                            new ArrayList<Annotation>();
            aCopyOfAllAnnotations.addAll(lookups);
            // System.out
            // .println("****************************aCopyOfAllAnnotations.....\n"
            // + aCopyOfAllAnnotations.toString());
            aCopyOfAllAnnotations.remove(annotation);
            List<Annotation> overlapped =
                            new ArrayList<Annotation>();
            overlapped.add(annotation);
            // System.out
            // .println("***************again....*************aCopyOfAllAnnotations.....\n"
            // + aCopyOfAllAnnotations.toString());
            for (Annotation anotherAnnotation : aCopyOfAllAnnotations) {
                // System.out
                // .println("****************************checking now.....\n"
                // + anotherAnnotation);
                if (annotation.overlaps(anotherAnnotation)) {
                    // System.out
                    // .println("****************************overlap found");
                    overlapped.add(anotherAnnotation);
                    processedAnnotations.add(anotherAnnotation);
                }
            }
            overlappedGroups.add(overlapped);
            // System.out.println("****************************overlaped:"
            // + overlapped.toString());
        }
        return overlappedGroups;
    }
}
