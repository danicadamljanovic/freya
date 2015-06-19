package org.freya.oc;

import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.Annotation;
import org.freya.model.DatatypePropertyValueElement;
import org.freya.model.DatatypePropertyValueIdentifier;
import org.freya.model.NoneElement;
import org.freya.model.OntologyElement;
import org.freya.model.POC;
import org.freya.model.Question;
import org.freya.model.SemanticConcept;
import org.freya.parser.stanford.TreeUtils;
import org.freya.rdf.query.CATConstants;
import org.freya.util.AnnotationOffsetComparator;
import org.freya.util.SemanticConceptListComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OCUtil {
    private static final Log logger = LogFactory.getLog(OCUtil.class);

    AtomicInteger id = new AtomicInteger();
    @Autowired TreeUtils treeUtils;



    /**
     * 
     * @param anns
     * @return
     */
    public Set<Annotation> filterInstanceClass(List<Annotation> anns) {
        HashMap<Long, List<Annotation>> map = new HashMap<Long, List<Annotation>>();

        for (Annotation ann : anns) {
            List<Annotation> tmp = map.get(ann.getStartOffset());
            if (tmp == null) tmp = new ArrayList<Annotation>();
            tmp.add(ann);
            map.put(ann.getStartOffset(), tmp);
        }
        // now we have all start offsets
        logger.info("Now finished puting all anns into the map where key is offsets, these are the keys:"
                        + map.keySet().toString());
        List<Long> startOffsets = new ArrayList<Long>();
        startOffsets.addAll(map.keySet());
        Collections.sort(startOffsets);
        List toRemove = new ArrayList();

        for (int i = 0; i < startOffsets.size() - 1; i++) {

            Long current = startOffsets.get(i);
            Long next = startOffsets.get(i + 1);
            logger.info("Current ofsset" + current.toString() + " next offset:" + next.toString());
            List<Annotation> currentAnnotations = map.get(current);
            List<Annotation> nextAnnotations = map.get(next);
            toRemove.addAll(whichToRemove(currentAnnotations, nextAnnotations));
            toRemove.addAll(whichToRemove(nextAnnotations, currentAnnotations));
        }

        // if (toRemove!=null)
        // anns.removeAll(toRemove);
        logger.info("Marked to remove due to classInstance:" + toRemove.size());
        // return anns;
        return new HashSet(toRemove);

    }

    // }
    /**
     * if first is class and second is instance then it checks the class of the instance and if the two match marks the
     * class to remove
     */
    List<Annotation> whichToRemove(List<Annotation> first, List<Annotation> second) {
        List<Annotation> toRemove = new ArrayList<Annotation>();
        // save in this list members from 'first' only if they are classes
        List<Annotation> firstClassesOnly = new ArrayList<Annotation>();
        // save in this list members from 'second' only if they are instances only
        List<Annotation> secondInstancesOnly = new ArrayList<Annotation>();
        for (Annotation ann1 : first) {
            String type = (String) ann1.getFeatures().get(CATConstants.TYPE_FEATURE);
            if (type.equals(CATConstants.FEATURE_TYPE_CLASS)) {
                firstClassesOnly.add(ann1);
            }
        }
        logger.info("There are " + firstClassesOnly.size() + " classes in the first column.");
        // second column
        for (Annotation ann1 : second) {
            String type = (String) ann1.getFeatures().get(CATConstants.TYPE_FEATURE);
            if (type.equals(CATConstants.TYPE_INSTANCE)) {
                secondInstancesOnly.add(ann1);
            }
        }
        logger.info("There are " + secondInstancesOnly.size() + " instances in the second column.");
        // collect classUris from first

        // now we know for sure that the first column is class and second is instance
        // so we take the second only, find all classUris of instances, and if we do find
        // that instance X is of the existing type of class then we do this:
        // - remove all instances from first
        // - remove all but X from the second column
        Set<String> allClassUrisFirst = new HashSet<String>();
        for (Annotation ann1 : firstClassesOnly) {
            String classUri = (String) ann1.getFeatures().get(CATConstants.FEATURE_URI);
            logger.info("class uri:" + classUri);
            allClassUrisFirst.add(classUri);
        }
        Set<Annotation> secondToKeep = new HashSet<Annotation>();
        for (Annotation ann1 : secondInstancesOnly) {
            logger.info("inst annotation:" + ann1.toString());

            Set<String> classUris = new HashSet((List) (ann1.getFeatures().get(CATConstants.CLASS_URI_LIST)));
            String classUri = (String) ann1.getFeatures().get(CATConstants.CLASS_URI);
            classUris.add(classUri);
            logger.info("classUris:" + classUris);
            // if classUris contain any of the classes in allClassUrisFirst then
            if (classInstanceFound(classUris, allClassUrisFirst))
            {
                secondToKeep.add(ann1);
            }
        }
        if (secondToKeep.size() > 0) {
            toRemove.addAll(first);
            toRemove.addAll(second);
            toRemove.removeAll(secondToKeep);
        }
        logger.info("marked to remove:" + toRemove.size());
        return toRemove;
    }

    /**
     * 
     * @param instanceClassUris
     * @param allClassUris
     * @return
     */
    boolean classInstanceFound(Set<String> instanceClassUris, Set<String> allClassUris) {
        boolean toReturn = false;
        for (String instClassUri : instanceClassUris) {
            logger.info("Checking:" + instClassUri);
            for (String classUri : allClassUris) {
                logger.info(" and:" + classUri);
                if (instClassUri.equals(classUri)) {
                    logger.info("Annotation should be removed?");
                    toReturn = true;
                    break;
                }
            }
        }
        return toReturn;
    }

    /**
     * finding nearest neighbour for poc
     * 
     * @param question
     * @param poc
     * @return
     */
    public List<SemanticConcept> findNearestNeighbours(Question question, POC poc) {
        List<SemanticConcept> nearestNeighbours = new ArrayList<SemanticConcept>();
        Tree root = question.getSyntaxTree();
        List<SemanticConcept> scList = null;
        try {
            scList = getLinearisedSemanticConcepts(question.getSemanticConcepts());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int min = 1000;
        logger.info("Finding nearest neighbour for POC:"
                        + poc.getAnnotation().getText());
        logger.info("There are:" + scList.size()
                        + " candidates (semantic concepts)");
        for (SemanticConcept sConcept : scList) {
            if (sConcept.getOntologyElement() instanceof NoneElement) {
                continue;
            }
            int distance =
                            treeUtils.getDistance(poc.getAnnotation().getSyntaxTree(),
                                            sConcept.getOntologyElement().getAnnotation()
                                                            .getSyntaxTree(), root);
            // logger.debug("Distance is:"
            // + distance
            // + " between "
            // + poc.getAnnotation().getSyntaxTree().toString()
            // + " \n and \n"
            // + sConcept.getOntologyElement().getAnnotation()
            // .getSyntaxTree().toString());
            if (min > distance) {
                min = distance;
                nearestNeighbours = new ArrayList<SemanticConcept>();
                nearestNeighbours.add(sConcept);
            } else if (min == distance) {
                nearestNeighbours.add(sConcept);
            }
        }
        return nearestNeighbours;
    }

    /**
     * @param question
     * @param keySemConcept
     * @return
     */
    public List<SemanticConcept> findNearestNeighbours(Question question,
                    List<SemanticConcept> list) {
        SemanticConcept keySemConcept = list.get(0);
        List<Tree> treeOfTheKeySemanticConcept =
                        keySemConcept.getOntologyElement().getAnnotation().getSyntaxTree();
        List<SemanticConcept> nearestNeighbours = new ArrayList<SemanticConcept>();
        Tree root = question.getSyntaxTree();
        List<SemanticConcept> newList = null;
        // this currentList is introduced so that the element is not found to
        // be the nearest neighbour of itself
        List<List<SemanticConcept>> currentList =
                        new ArrayList<List<SemanticConcept>>();
        currentList.addAll(question.getSemanticConcepts());
        currentList.remove(list);
        try {
            newList = getAllSemanticConceptsLinearised(currentList);
            logger.debug("currently:" + newList.size()
                            + " in the semantic elements list");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List noneToRemove = new ArrayList();
        // if any of te elements in the neighbourhood is None then ignore them
        for (SemanticConcept theOne : newList) {
            if (theOne.getOntologyElement() instanceof NoneElement) {
                noneToRemove.add(theOne);
            }
        }
        newList.removeAll(noneToRemove);
        logger.info("after removing none elements:" + newList.size()
                        + " in the semantic elements list");
        logger.debug("Removed:" + noneToRemove.size()
                        + " none elements as they are not counted when finding NN.");
        int min = 1000;
        logger.debug("Found " + newList.size() + " neighbours for: "
                        + keySemConcept.getOntologyElement().getData().toString());
        for (SemanticConcept sConcept : newList) {
            int distance =
                            treeUtils.getDistance(treeOfTheKeySemanticConcept, sConcept
                                            .getOntologyElement().getAnnotation().getSyntaxTree(),
                                            root);
            // logger.debug("distance is:" + distance + " between "
            // // + treeOfTheKeySemanticConcept + " \n and \n"
            // + sConcept.getOntologyElement().getAnnotation().getSyntaxTree());
            if (min > distance) {
                min = distance;
                nearestNeighbours = new ArrayList<SemanticConcept>();
                nearestNeighbours.add(sConcept);
                // logger.debug("Adding:"
                // + sConcept.getOntologyElement().getData().toString()
                // + " to the list of NNs");
            } else if (min == distance) {
                nearestNeighbours.add(sConcept);
                // logger.debug("Adding:"
                // // + sConcept.getOntologyElement().getData().toString()
                // + " to the list of NNs");
            }
        }
        return nearestNeighbours;
    }

    /**
     * @param question
     * @param poc
     * @return
     */
    public POC whichPOC(Question question) {
        List<POC> pocs = question.getPocs();
        POC theOne = null;
        Tree root = question.getSyntaxTree();
        List<SemanticConcept> scList = null;
        try {
            scList = getLinearisedSemanticConcepts(question.getSemanticConcepts());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.debug("This many sConcepts:" + scList.size());
        logger
                        .debug("Trying to decide which poc to use for the dialog; this many pocs:"
                                        + pocs.size());
        int min = 1000;
        for (POC poc : pocs) {
            List<SemanticConcept> nearestNeighbours =
                            findNearestNeighbours(question, poc);
            SemanticConcept sc = null;
            // take one, it does not matter which
            if (nearestNeighbours != null && nearestNeighbours.size() > 0)
                sc = nearestNeighbours.get(0);
            if (sc != null) {
                int distance =
                                treeUtils.getDistance(poc.getAnnotation().getSyntaxTree(), sc
                                                .getOntologyElement().getAnnotation().getSyntaxTree(),
                                                root);
                if (min > distance) {
                    min = distance;
                    theOne = poc;
                } else if (min == distance && theOne != null) {
                    // take the one which is on the right-hand side
                    // this means that theOne and poc are on the same level:
                    // take the one on the right
                    if (poc.getAnnotation().getStartOffset().longValue() > theOne
                                    .getAnnotation().getStartOffset().longValue()) theOne = poc;
                }
            }
        }
        // this means if there are not ocs, use the first one, which should also
        // be mainsubject TODO: this is more a workaround than a solution but I
        // live it like this for the time being
        if (pocs.size() > 0 && theOne == null) {
            theOne = pocs.get(0);
        }
        return theOne;
    }

    public boolean ocsVerified(List<SemanticConcept> list) {
        boolean allVerified = false;
        for (SemanticConcept c : list) {
            if (c.isVerified().booleanValue() == true)
                allVerified = true;
        }
        return allVerified;
    }

    /**
     * finds the OC to be put into dialog: looks which one is closest to focus
     * 
     * @param question
     * @return
     */
    public List<SemanticConcept> whichOC(Question question) {

        if (question.getFocus() == null) {
            return question.getSemanticConcepts().get(0);
        }

        SemanticConceptListComparator c = new SemanticConceptListComparator();
        long start = System.currentTimeMillis();
        if (question.getSemanticConcepts().size() > 1)
            Collections.sort(question.getSemanticConcepts(), c);
        long end = System.currentTimeMillis();
        logger.info("Sorting sem elements took:" + (end - start) + "ms");
        List<SemanticConcept> theOne = null;
        Tree root = question.getSyntaxTree();
        int min = 1000;
        start = System.currentTimeMillis();
        for (List<SemanticConcept> list : question.getSemanticConcepts()) {
            if (!ocsVerified(list)) {
                SemanticConcept sampleElement = list.get(0);
                int distance =
                                treeUtils.getDistance(question.getFocus().getAnnotation()
                                                .getSyntaxTree(), sampleElement.getOntologyElement()
                                                .getAnnotation().getSyntaxTree(), root);
                logger.debug("Distance:" + distance);
                if (min > distance && list.size() > 1) {
                    min = distance;
                    theOne = list;
                }
            }
        }
        end = System.currentTimeMillis();
        logger.info("Calculating distance for which oc took:" + (end - start) + "ms");
        // this means if there are no ocs, use the first one, which should also
        // be mainsubject TODO: this is more a workaround than a solution but I
        // live it like this for the time being
        logger.debug("Focus:" + question.getFocus());
        logger.debug("Sem elements to use for the OC dialog:" + theOne);
        return theOne;

    }

    public List<SemanticConcept> getLinearisedSemanticConcepts(
                    List<List<SemanticConcept>> table) throws Exception {
        List<SemanticConcept> linearisedList = new ArrayList<SemanticConcept>();
        for (List<SemanticConcept> row : table) {
            if (row.size() > 1) {
                logger
                                .debug("List of OCs is ambiguious and needs to be disambiguated using dialog");
                linearisedList.add(row.get(0));
            } else if (row.size() > 0) linearisedList.add(row.get(0));
        }
        return linearisedList;
    }

    /**
     * utility method to transform list of lists into one list
     * 
     * @param table
     * @return
     * @throws Exception
     */
    public List<SemanticConcept> getAllSemanticConceptsLinearised(
                    List<List<SemanticConcept>> table) throws Exception {
        List<SemanticConcept> linearisedList = new ArrayList<SemanticConcept>();
        for (List<SemanticConcept> row : table) {
            // logger
            // .debug("List of OCs is ambiguious and needs to be disambiguated using dialog!");
            linearisedList.addAll(row);
        }
        return linearisedList;
    }

    /**
     * if two semantic concepts are in the same column and refer to the same ontology concept (same instance URI or
     * class URI or propertyURI) then remove it
     * 
     * @param Question
     * @return Question
     */
    public Question removeDuplicateOcs(Question question) {
        List<List<SemanticConcept>> table = question.getSemanticConcepts();
        // logger.debug("input table with semantic concepts:" + table.toString());
        // key: column index within table; value: list of indexes in the column to
        // be removed
        Map<Integer, List<Integer>> mapToRemove =
                        new HashMap<Integer, List<Integer>>();
        for (List<SemanticConcept> sList : table) {
            List<String> noDuplicates = new ArrayList<String>();
            List<DatatypePropertyValueIdentifier> noDuplicatesDTVI =
                            new ArrayList<DatatypePropertyValueIdentifier>();
            for (SemanticConcept sConcept : sList) {
                OntologyElement oe = sConcept.getOntologyElement();
                // uri for classes, instances or properties
                String uri = null;
                DatatypePropertyValueIdentifier dpvIdentifier = null;
                if (oe instanceof DatatypePropertyValueElement) {
                    dpvIdentifier =
                                    (DatatypePropertyValueIdentifier) ((DatatypePropertyValueElement) oe)
                                                    .getData();
                } else {
                    Object data = oe.getData();
                    if (data != null) uri = data.toString();
                }
                logger.debug("dpvIdentifier:" + dpvIdentifier);
                logger.debug("URI:" + uri);
                // different treatment for classes,properties and instances
                // versus datatypepropertyvalues
                if (uri != null && !noDuplicates.contains(uri)) {
                    noDuplicates.add(uri);
                } else if (dpvIdentifier != null
                                && !noDuplicatesDTVI.contains(dpvIdentifier)) {
                    noDuplicatesDTVI.add(dpvIdentifier);
                } else {
                    List<Integer> list =
                                    mapToRemove.get(new Integer(table.indexOf(sList)));
                    if (list == null) list = new ArrayList<Integer>();
                    list.add(sList.indexOf(sConcept));
                    mapToRemove.put(new Integer(table.indexOf(sList)), list);
                    logger.debug("List with elements to be removed:" + list.toString());
                }
            }
        }
        if (mapToRemove.keySet().size() > 0)
            logger.info("Removing " + mapToRemove.keySet().size()
                            + " OCs as they are duplicated.");
        logger.debug("map with semantic concepts to be removed:"
                        + mapToRemove.toString());
        logger
                        .debug("before removing:"
                                        + question.getSemanticConcepts().toString());
        question = removeMarkedOcsByIndex(question, mapToRemove);
        logger.debug("after removing:" + question.getSemanticConcepts().toString());
        return question;
    }

    /**
     * removes marked ocs from the table: the map contains key=index of the column; and value=list of elements to be
     * removed
     * 
     * @param question
     * @param toRemove
     * @return
     */
    public Question removeMarkedOcsByIndex(Question question,
                    Map<Integer, List<Integer>> toRemove) {
        List<Integer> keys = new ArrayList<Integer>(toRemove.keySet());
        Integer index = null;
        for (int i = 0; i < keys.size(); i++) {
            index = keys.get(i);
            if (index != null) {
                List<SemanticConcept> column =
                                question.getSemanticConcepts().get(index.intValue());
                logger.debug("the columns has " + column.size() + " elements.");
                List<Integer> elementsToRemove = toRemove.get(index);
                logger.debug("now removing " + elementsToRemove.size()
                                + " element(s) from the column.");
                for (Integer rowIndex : elementsToRemove) {
                    column.remove(rowIndex.intValue());
                }
                logger.debug("after removing this many left: " + column.size()
                                + " elements.");
            }
        }
        return question;
    }

    /**
     * removes marked ocs from the table: the map contains key=index of the column; and value=list of elements to be
     * removed
     * 
     * @param question
     * @param toRemove
     * @return
     */
    public Question removeMarkedOcs(Question question,
                    Map<Integer, List<SemanticConcept>> toRemove) {
        List<Integer> keys = new ArrayList<Integer>(toRemove.keySet());
        for (Integer index : keys) {
            if (index != null) {
                List<SemanticConcept> column =
                                question.getSemanticConcepts().get(index.intValue());
                List<SemanticConcept> onlySelectedElements = toRemove.get(index);
                logger.info("About to remove elements from column:" + index + " there are:"
                                + (column.size()));
                column.clear();
                // verified means the user selected it
                for (SemanticConcept c : onlySelectedElements) {
                    c.setVerified(true);
                }

                column.addAll(onlySelectedElements);

            }
        }
        return question;
    }

    /**
     * @param question
     * @return
     */
    public List<List<SemanticConcept>> addNoneToAll(
                    List<List<SemanticConcept>> elements) {
        for (List<SemanticConcept> column : elements) {
            for (SemanticConcept sampleElement : column) {
                if (sampleElement.getOntologyElement().getAnnotation() != null) {
                    SemanticConcept scEl = new SemanticConcept();
                    NoneElement noneElement = new NoneElement();
                    Annotation newAnn =
                                    (Annotation) sampleElement
                                                    .getOntologyElement().getAnnotation().clone();
                    noneElement.setAnnotation(newAnn);
                    noneElement.setId(new Integer(id.getAndIncrement()).toString());
                    logger.debug("Setting annotation for none element:"
                                    + noneElement.toString());
                    scEl.setOntologyElement(noneElement);
                    column.add(scEl);
                    break;
                }
            }
        }
        return elements;
    }

    /**
     * checkes whether there are any overlapped annotations, if there are leaves only the longest (of type Lookup! only)
     * 
     * @param allAnnotations
     * @return
     */
    public List<Annotation> disambiguateBasedOnLength(
                    List<Annotation> allAnnotations) {
        AnnotationOffsetComparator c = new AnnotationOffsetComparator();
        Collections.sort(allAnnotations, c);
        ArrayList<Annotation> aCopyOfLookupAnnotations =
                        null;
        ArrayList<Annotation> checkedAnnotations =
                        new ArrayList<Annotation>();
        ArrayList<Annotation> toRemove =
                        new ArrayList<Annotation>();
        // startofsset, endoffset
        Map<Long, Long> removed = new HashMap();
        for (Annotation ann : allAnnotations) {
            if (!checkedAnnotations.contains(ann)) {
                checkedAnnotations.add(ann);
                aCopyOfLookupAnnotations =
                                new ArrayList<Annotation>();
                aCopyOfLookupAnnotations.addAll(allAnnotations);
                aCopyOfLookupAnnotations.remove(ann);
                for (Annotation newAnn : aCopyOfLookupAnnotations) {
                    if (ann.overlaps(newAnn)) {
                        // logger.info(ann.toString()
                        // + "\n***************overlaps with***********\n"
                        // + newAnn.toString());
                        // which one is longer
                        // add it to toRemove
                        long diffAnn =
                                        ann.getEndOffset().longValue()
                                                        - ann.getStartOffset().longValue();
                        long diffNewAnn =
                                        newAnn.getEndOffset().longValue()
                                                        - newAnn.getStartOffset().longValue();
                        if (diffAnn > diffNewAnn) {
                            if (!toRemove.contains(newAnn)) {
                                toRemove.add(newAnn);
                                removed.put(newAnn.getStartOffset(), newAnn.getEndOffset());
                                if (!checkedAnnotations.contains(newAnn))
                                    checkedAnnotations.add(newAnn);
                                // logger.info("\nadding to remove:\n" + newAnn.toString());
                            }
                        } else if (diffAnn < diffNewAnn) {
                            if (!toRemove.contains(ann)) {
                                toRemove.add(ann);
                                removed.put(ann.getStartOffset(), ann.getEndOffset());
                                if (!checkedAnnotations.contains(ann))
                                    checkedAnnotations.add(ann);
                                // logger.info("\nadding to remove:\n" + ann.toString());
                            }
                        }
                    } else {
                        // they do not overlap, continue to the next one
                        // logger.debug(ann.toString()
                        // + "***************DO NOT overlap with***********"
                        // + newAnn.toString());
                    }
                }// newAnn
            }
        }
        logger
                        .debug("********************************disambiguation******************\n");
        // logger.info("before:" + allAnnotations.toString());
        if (toRemove.size() > 0)
            logger.info("Removing:" + toRemove.size() + " columns (short annotations).");
        allAnnotations.removeAll(toRemove);
        // logger.info("after:" + allAnnotations.toString());
        toRemove = new ArrayList();
        for (Annotation remove : allAnnotations) {
            Set<Long> startKeys = removed.keySet();
            // logger.info("startOfsets:"+startKeys);
            if (startKeys.contains(remove.getStartOffset())) {
                Long endOffset = removed.get(remove.getStartOffset());
                // logger.info("endOffset:"+endOffset);
                if (endOffset.longValue() == remove.getEndOffset().longValue()) {
                    toRemove.add(remove);
                }
            }
        }
        // logger.info("and again before:" + allAnnotations.toString());
        if (toRemove.size() > 0)
            logger.info("Removing again:" + toRemove.size() + " annotations.");
        allAnnotations.removeAll(toRemove);
        // logger.info("after:" + allAnnotations.toString());
        return allAnnotations;
    }

    public static void main(String[] args) {
        OCUtil u = new OCUtil();
        List<Annotation> set =
                        new ArrayList<Annotation>();
        AnnotationOffsetComparator c = new AnnotationOffsetComparator();
        Annotation ann =
                        new Annotation();
        ann.setText("Michael");
        ann.setStartOffset(new Long(0));
        ann.setEndOffset(new Long(1));
        set.add(ann);
        ann = new Annotation();
        ann.setText("Michael");
        ann.setStartOffset(new Long(0));
        ann.setEndOffset(new Long(1));
        set.add(ann);
        ann = new Annotation();
        ann.setText("Michael Jackson");
        ann.setStartOffset(new Long(0));
        ann.setEndOffset(new Long(2));
        set.add(ann);
        ann = new Annotation();
        ann.setText("Michael Jackson");
        ann.setStartOffset(new Long(0));
        ann.setEndOffset(new Long(2));
        set.add(ann);
        ann = new Annotation();
        ann.setText("Jackson");
        ann.setStartOffset(new Long(1));
        ann.setEndOffset(new Long(2));
        set.add(ann);
        logger.info("Before:" + set.size());
        List sortedList = new ArrayList(set);
        Collections.sort(sortedList, c);
        set = u.disambiguateBasedOnLength(sortedList);
        logger.info("After:" + set.size());
    }
}
