package org.freya.util;

import java.util.ArrayList;
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
import org.freya.model.ClassElement;
import org.freya.model.InstanceListElement;
import org.freya.model.JokerElement;
import org.freya.model.OntologyElement;
import org.freya.model.PropertyElement;
import org.freya.model.ResultGraph;
import org.freya.model.SerializableURI;
import org.freya.model.TripleElement;
import org.freya.rdf.query.CATConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GraphUtils {
    public static AtomicInteger idCounter = new AtomicInteger();

    private static final Log logger = LogFactory.getLog(GraphUtils.class);

    @Autowired LuceneAnnotator luceneAnnotator;

    /**
     * this method creates the root ResultGraph node
     * 
     * @param columnData
     * @return
     */
    public List<ResultGraph> getResultGraph(List<TripleElement> triples)
                    throws Exception {
        List<ResultGraph> nonOptimizedGraph = new ArrayList<ResultGraph>();

        nonOptimizedGraph = getGraphFrom(triples);
        logger.info("\n Graph consistent:" + isGraphConsistent(nonOptimizedGraph));
        logger.info("before optimization:\n" + nonOptimizedGraph.toString());
        // here optimise graph
        logger.info("\n Now calling optimizeGraph method...");
        List<ResultGraph> optimizedGraph = optimizeGraph(nonOptimizedGraph);
        logger.info("after optimization:\n" + optimizedGraph.toString());
        logger.info("\n Optimized graph consistent:"
                        + isGraphConsistent(optimizedGraph));
        logger.info("\n Is data lost?"
                        + isDataLost(nonOptimizedGraph, optimizedGraph));
        return optimizedGraph;
    }

    public static boolean isDataLost(List<ResultGraph> nonOptimizedFinalGraph,
                    List<ResultGraph> optimizedGraph) {
        List<String> afterOptimizationUris = new ArrayList<String>();
        boolean result = false;
        for (ResultGraph node : optimizedGraph) {
            String uri = node.getURI();
            afterOptimizationUris.add(uri);
        }
        for (ResultGraph node : nonOptimizedFinalGraph) {
            String uri = node.getURI();
            if (!afterOptimizationUris.contains(uri)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Consistent graph: 1) each node must have EITHER at least one incoming, OR at least one outcoming; 2) all
     * outcoming ids must match with ids of existing nodes
     * 
     * @param graph
     * @return
     */
    public static boolean isGraphConsistent(List<ResultGraph> graph) {
        boolean consistent = true;
        // check graph consistency
        List<Integer> ids = new ArrayList<Integer>();
        List<Integer> outcomings = new ArrayList<Integer>();
        for (ResultGraph node : graph) {
            ids.add(node.getId());
            if (node.getAdjacencies() != null)
                outcomings.addAll(node.getAdjacencies());
        }
        for (ResultGraph node : graph) {
            List<Integer> adjs = node.getAdjacencies();
            for (Integer num : adjs) {
                if (!ids.contains(num)) {
                    logger.info("Node with id:" + num
                                    + " does not exist, and is referenced by another node!");
                    consistent = false;
                }
            }
            // check point 1: whether there exist at least one incoming or
            // outcoming node
            if (!outcomings.contains(node.getId()) && (adjs.size() <= 0))
                consistent = false;
        }
        return consistent;
    }

    /**
     * updates adjacencies by reading elements from the newGraph and updating the elements with the same URI in the
     * oldGraph; this methods basically 'adds' new adjacencies from newGraph to the elements in the old graph
     * 
     * @param finalGraph
     * @param rootNodesForLastElement
     */
    public static List<ResultGraph> updateGraphWithNewAdjacencies(
                    List<ResultGraph> oldGraph, List<ResultGraph> newGraph) {
        // find rootNodesForLastElement in finalGraph and updateAdjacencies
        HashMap<Integer, ResultGraph> oldGraphMap =
                        new HashMap<Integer, ResultGraph>();
        for (ResultGraph node : oldGraph) {
            oldGraphMap.put(node.getId(), node);
        }
        for (ResultGraph node : newGraph) {
            ResultGraph old = (ResultGraph) oldGraphMap.get(node.getId());
            if (node != null && old != null) {
                logger.info("old graph has adjacencies:" + old.getAdjacencies());
                logger.info("updated node HAS these adjacencies:"
                                + node.getAdjacencies());
                // if (old.getAdjacencies() == null)
                // old.setAdjacencies(new ArrayList());
                // if (node.getAdjacencies() != null)
                old.getAdjacencies().addAll(node.getAdjacencies());
                // if (old != null)
                oldGraphMap.put(node.getId(), old);
            } else {
                logger
                                .info("WARNING::::The node with this id does not exist in the new graph which should have info about updated nodes!!!!:"
                                                + node.getId());
                logger.debug("old element is:" + old);
                logger.debug("new element is:" + node);
                old.getAdjacencies().addAll(node.getAdjacencies());
                oldGraphMap.put(node.getId(), old);
                // oldGraphMap.put(node.getId(), node);
                // logger.debug("Adding it to the map...");
            }
        }
        List<ResultGraph> updatedGraph = new ArrayList<ResultGraph>();
        for (Integer key : oldGraphMap.keySet()) {
            updatedGraph.add(oldGraphMap.get(key));
        }
        return updatedGraph;
    }

    /**
     * building the graph: iterate through triples now and for the first row create one resultGraph per element, so
     * rt(c), rt(b), rt(a) the first resultGraph rt(c) becomes root, then rt(b) is his adjacent, whose adjacent is
     * rt(a), so we now have structure rt(c)-hasAdjacent- rt(b)-hasAdjacent- rt(a) so the first element becomes root
     * namely rt(c), and the last element becomes LASTCHILD now iterate through the rest of the table and check (case 1)
     * if the FIRST ELEMENT in a row = ROOT then do this: create ResultGraph for d and e and add d as a child of root,
     * and add e as a child of d, so now we have rt(c)-hasChild- rt(b)-hasChild- rt(a) rt(c)-hasChild- rt(d)-hasChild-
     * rt(e) (case 2) else if the FIRST ELEMENT in a row == LASTCHILD then do this: add d to be a child of the
     * lastChild, and e to be the child of d; for example if our table was a-b-c c-d-e so now we have rt(a)-hasChild-
     * rt(b)-hasChild- rt(c)-hasChild- rt(d)-hasChild- rt(e) where rt(a)=ROOT and rt(e)=LASTCHILD (case 3) else if the
     * LAST ELEMENT in a row ==ROOT so for example c-b-a e-d-c create ResultGraph e, and d, e becomes ROOT, than d is a
     * child of e, and ROOT is added as a child of e, so now we have rt(e)-hasChild- rt(d)-hasChild- rt(c)-hasChild-
     * rt(b)-hasChild- rt(a) (case 4) else if the LAST ELEMENT in a row==LASTCHILD for example: a-b-c e-d-c this becomes
     * the case 1, but need to add property to ResultGraph (orientationOpposite=true) so, first change the table to
     * c-b-a c-d-e and then apply the same algorithm as for the case 1, and set the property for orientation to be true
     * 
     * @param triples
     * @return
     */
    public List<ResultGraph> getGraphFrom(List<TripleElement> triples) {
        List<ResultGraph> finalGraph = new ArrayList<ResultGraph>();
        logger.info("generating graph from these triples: " + triples.toString());
        TripleElement firstTriple = triples.get(0);
        logger.info("taking first triple: " + firstTriple.toString());
        int numberOfElements;
        List<ResultGraph> rootNodes = null;
        List<ResultGraph> otherNodes = null;
        List<ResultGraph> rootNodesForLastElement = null;
        List<ResultGraph> otherNodesForLastElement = null;
        List<String> setOfValues=null;
        HashMap<String, List<ResultGraph>> graphFromTheFirstTripleElement;
        try {
            // the FIRST element in the first triple
            graphFromTheFirstTripleElement =
                            getResultGraphFromOntologyElement(firstTriple.getElements()
                                            .get(0));
            logger.debug("Taking first element in the triple: "
                            + firstTriple.getElements().get(0).toString());
            logger.debug("generated graph:" + graphFromTheFirstTripleElement);
            // logger.debug("Graph from the first triple element:"
            // + graphFromTheFirstTripleElement.toString());
            rootNodes = (List<ResultGraph>) graphFromTheFirstTripleElement.get("root");
            otherNodes =
                            (List<ResultGraph>) graphFromTheFirstTripleElement.get("other");
            finalGraph.addAll(rootNodes);
            finalGraph.addAll(otherNodes);
            logger.info("Graph from the first node:" + finalGraph.toString());
            numberOfElements=firstTriple.getElements().size();
            // Check if the second element in triple contains result
            setOfValues = firstTriple.getElements().get(1).get(0).getResults();
            if (firstTriple.getElements().size() > 2 && setOfValues!=null) {
                // the SECOND element in the first triple
                List<ResultGraph> rootNodesForTheSecondElement = null;
                List<ResultGraph> otherNodesForTheSecondElement = null;
                logger
                                .debug("generating graph for the second element in the first triple which is:::::::::::::::::::"
                                                + firstTriple.getElements().get(1).toString());
                HashMap<String, List<ResultGraph>> secondElInTheFirstTriple =
                                getResultGraphFromOntologyElement(firstTriple.getElements()
                                                .get(1));
                rootNodesForTheSecondElement =
                                (List<ResultGraph>) secondElInTheFirstTriple.get("root");
                otherNodesForTheSecondElement =
                                (List<ResultGraph>) secondElInTheFirstTriple.get("other");
                // finalGraph.addAll(rootNodesForTheSecondElement);
                // finalGraph.addAll(otherNodesForTheSecondElement);
                // the THIRD element in the first triple
                HashMap<String, List<ResultGraph>> lastGraphElementFromTheTriple =
                                getResultGraphFromOntologyElement(firstTriple.getElements()
                                                .get(2));
                rootNodesForLastElement =
                                (List<ResultGraph>) lastGraphElementFromTheTriple.get("root");
                otherNodesForLastElement =
                                (List<ResultGraph>) lastGraphElementFromTheTriple.get("other");
                //
                // finalGraph.addAll(rootNodesForLastElement);
                // finalGraph.addAll(otherNodesForLastElement);
                // now connect elements from the first triple
                if (rootNodesForTheSecondElement != null
                                && rootNodesForTheSecondElement.size() > 0) {
                    Iterator<ResultGraph> iMiddle =
                                    rootNodesForTheSecondElement.iterator();
                    if (iMiddle.hasNext()) {
                        for (ResultGraph node : rootNodes) {
                            logger.debug("for node:" + node.toString());
                            // for (ResultGraph
                            // propertyNode:rootNodesForTheSecondElement){
                            // probaj ovo pa vidi je l ima veze ovde je negde
                            // problem
                            ResultGraph propertyNode = (ResultGraph) iMiddle.next();
                            node.getAdjacencies().add(propertyNode.getId());
                            logger.debug("adding adjs propeprty node:"
                                            + propertyNode.toString());
                        }
                    }
                    // logger.info("e ovo je sada pojebano*****************************\n"+finalGraph.toString());
                    if (rootNodesForTheSecondElement != null)
                        finalGraph.addAll(rootNodesForTheSecondElement);
                    if (otherNodesForTheSecondElement != null)
                        finalGraph.addAll(otherNodesForTheSecondElement);
                }
                if (rootNodesForLastElement != null
                                && rootNodesForTheSecondElement != null) {
                    Iterator<ResultGraph> iLast = rootNodesForLastElement.iterator();
                    // for (ResultGraph lastEl : rootNodesForLastElement) {
                    for (ResultGraph middleElement : rootNodesForTheSecondElement) {
                        if (iLast.hasNext()) {
                            ResultGraph lastElement = (ResultGraph) iLast.next();
                            middleElement.getAdjacencies().add(lastElement.getId());
                        }
                    }
                    if (rootNodesForLastElement != null)
                        finalGraph.addAll(rootNodesForLastElement);
                    if (otherNodesForLastElement != null)
                        finalGraph.addAll(otherNodesForLastElement);
                }
                logger.debug("Graph from the first TRIPLE looks like this:"
                                + finalGraph.toString());
                // now process other triples
                int numberOfRows = triples.size();
                // we compare classes which are in 'other' part of
                // the map as they are used to 'connect' graph; but we must
                // also compare instance uris :(
                ResultGraph first = null;
                List<String> firstInstanceRoots = new ArrayList<String>();
                ResultGraph third = null;
                List<String> thirdInstanceRoots = new ArrayList<String>();
                List<String> rootUris = new ArrayList<String>();
                List<String> rootInstanceUris = new ArrayList<String>();
                List<String> lastChildrenUris = new ArrayList<String>();
                List<String> lastChildrenInstanceUris = new ArrayList<String>();
                // we skip the first row as that's already processed to create
                // root and last child and initial tree
                for (int i = 1; i < numberOfRows; i++) {
                    TripleElement triple = triples.get(i);
                    // logger.debug("this row has this many elements:::::::::::::::"
                    // + row.size());
                    // we add i here to maintain the id inside the elements and
                    // align it with the rows
                    Map<String, List<ResultGraph>> firstMap =
                                    getResultGraphFromOntologyElement(triple.getElements().get(0));
                    Map<String, List<ResultGraph>> secondMap =
                                    getResultGraphFromOntologyElement(triple.getElements().get(1));
                    Map<String, List<ResultGraph>> thirdMap = new HashMap<String, List<ResultGraph>>();
                    if (triple.getElements().size() > 2)
                        thirdMap = getResultGraphFromOntologyElement(triple.getElements().get(2));
                    List<ResultGraph> firstListRoot = firstMap.get("root");
                    List<ResultGraph> secondListRoot = secondMap.get("root");
                    List<ResultGraph> thirdListRoot = thirdMap.get("root");
                    logger.info("firstListRoot: " + firstListRoot.toString());
                    logger.info("secondListRoot: " + secondListRoot.toString());
                    if (thirdListRoot != null)
                        logger.info("thirdListRoot: " + thirdListRoot.toString());
                    else
                        logger.info("thirdListRoot: " + thirdListRoot);
                    List<ResultGraph> firstListOther = firstMap.get("other");
                    List<ResultGraph> secondListOther = secondMap.get("other");
                    List<ResultGraph> thirdListOther = thirdMap.get("other");
                    logger.info("firstListOther: " + firstListOther.toString());
                    logger.info("secondListOther: " + secondListOther.toString());
                    logger.info("thirdListOther: " + thirdListOther);
                    // this is very bad practice but cannot think of anything
                    // better now..so when i==1 this means that the first, third
                    // etc need be initialized, however for every future step
                    // this need not to be done
                    if (i == 1) {
                        // these must be initialised
                        // only classes/instances have other nodes as these hold
                        // classURI
                        if (otherNodes != null && otherNodes.size() > 0) {
                            for (ResultGraph node : otherNodes) {
                                rootUris.add(node.getURI());
                            }
                            for (ResultGraph node : rootNodes) {
                                rootInstanceUris.add(node.getURI());
                            }
                        } else {// this happens for the rest
                            for (ResultGraph node : rootNodes) {
                                rootUris.add(node.getURI());
                            }
                        }
                        // logger.debug("*****************************roots:"
                        // + rootUris.toString());
                        if (otherNodesForLastElement != null
                                        && otherNodesForLastElement.size() > 0) {
                            lastChildrenUris.add(otherNodesForLastElement.get(0).getURI());// todo:
                            // what if there are more?
                            for (ResultGraph graph : rootNodesForLastElement) {
                                lastChildrenInstanceUris.add(graph.getURI());
                            }
                        } else if (rootNodesForLastElement != null
                                        && rootNodesForLastElement.size() > 0) {
                            lastChildrenUris.add(rootNodesForLastElement.get(0).getURI()
                                            .trim());
                        }
                    }
                    if (firstListOther != null && firstListOther.size() > 0) {
                        first = firstListOther.get(0);
                        firstInstanceRoots = new ArrayList<String>();
                        for (ResultGraph graph : firstListRoot) {
                            firstInstanceRoots.add(graph.getURI());
                        }
                    } else if (firstListRoot != null && firstListRoot.size() > 0) {
                        // this might happen in the case of datatype
                        // properties
                        // etc/
                        first = firstListRoot.get(0);
                    }
                    if (thirdListOther != null && thirdListOther.size() > 0) {
                        third = thirdListOther.get(0);
                        thirdInstanceRoots = new ArrayList<String>();
                        for (ResultGraph graph : thirdListRoot) {
                            thirdInstanceRoots.add(graph.getURI());
                        }
                    } else if (thirdListRoot != null && thirdListRoot.size() > 0) {
                        // this might happen in the case of datatype
                        // properties/
                        third = thirdListRoot.get(0);
                    }
                    // logger.debug("*****************************rootUris:"
                    // + rootUris.toString());
                    // logger.debug("*****************************lastChildren:"
                    // + lastChildrenUris.toString());
                    if (first != null && first.getURI() != null && third != null
                                    && third.getURI() != null) {
                        logger.info("first is:\n" + first.toString());
                        logger.info("third is:\n" + third.toString());
                        logger.info("rootUris:\n" + rootUris.toString());
                        logger.info("rootInstanceUris:\n" + rootInstanceUris.toString());
                        logger.info("lastChildrenURIs:\n" + lastChildrenUris.toString());
                        logger.info("lastChildrenInstanceURIs\n:"
                                        + lastChildrenInstanceUris.toString());
                        // *******************************************
                        // case 1 starts
                        // *******************************************
                        if (rootUris.contains(first.getURI().trim())
                                        && rootInstanceUris.containsAll(firstInstanceRoots)) {
                            Iterator<ResultGraph> secondI = secondListRoot.iterator();
                            for (ResultGraph node : rootNodes) {
                                ResultGraph property = null;
                                if (secondI.hasNext()) {
                                    property = (ResultGraph) secondI.next();
                                    node.getAdjacencies().add(property.getId());
                                }
                            }
                            if (secondListRoot != null) finalGraph.addAll(secondListRoot);
                            if (secondListOther != null) finalGraph.addAll(secondListOther);
                            Iterator<ResultGraph> thirdI = thirdListRoot.iterator();
                            for (ResultGraph secondNode : secondListRoot) {
                                if (thirdI.hasNext()) {
                                    ResultGraph thirdNode = (ResultGraph) thirdI.next();
                                    secondNode.getAdjacencies().add(thirdNode.getId());
                                }
                            }
                            // }
                            if (thirdListRoot != null) finalGraph.addAll(thirdListRoot);
                            if (thirdListOther != null) finalGraph.addAll(thirdListOther);
                            lastChildrenUris = new ArrayList<String>();
                            lastChildrenUris.add(third.getURI());
                            lastChildrenUris.addAll(rootUris);
                            // NEW!
                            lastChildrenInstanceUris = new ArrayList<String>();
                            lastChildrenInstanceUris.addAll(rootInstanceUris);
                            lastChildrenInstanceUris.addAll(thirdInstanceRoots);
                            // rootNodesForLastElement needs to be updated?
                            logger.info("that was CASE 1 and the final graph now looks like:"
                                            + finalGraph.toString());
                            // root remains the same
                        }// end of case 1
                         // *******************************************************************
                         // case 2:
                         // ********************************************************************
                        else if (lastChildrenUris.contains(first.getURI().trim())
                                        && lastChildrenInstanceUris.containsAll(firstInstanceRoots)) {
                            Iterator<ResultGraph> secondI = secondListRoot.iterator();
                            // logger
                            // .debug("this is secondListRoot which needs to be connected to last element:"
                            // + secondListRoot.toString());
                            for (ResultGraph node : rootNodesForLastElement) {
                                ResultGraph property = null;
                                if (secondI.hasNext()) {
                                    property = (ResultGraph) secondI.next();
                                    node.getAdjacencies().add(property.getId());
                                    // List propertyIds = new ArrayList();
                                    // propertyIds.add(property.getId());
                                    // node.setAdjacencies(propertyIds);
                                    // logger.debug("adding:" + property.getId()
                                    // + "as adjacent to node with id:"
                                    // + node.getId());
                                }
                            }
                            finalGraph =
                                            updateGraphWithNewAdjacencies(finalGraph,
                                                            rootNodesForLastElement);
                            if (secondListRoot != null) finalGraph.addAll(secondListRoot);
                            if (secondListOther != null) finalGraph.addAll(secondListOther);
                            secondI = secondListRoot.iterator();
                            // second.getAdjacencies().add(third.getId());
                            // for (ResultGraph node : secondListRoot) {
                            for (ResultGraph sNode : thirdListRoot) {
                                ResultGraph property = null;
                                if (secondI.hasNext()) {
                                    property = (ResultGraph) secondI.next();
                                    property.getAdjacencies().add(sNode.getId());
                                }
                            }
                            // }
                            if (thirdListRoot != null) finalGraph.addAll(thirdListRoot);
                            if (thirdListOther != null) finalGraph.addAll(thirdListOther);
                            lastChildrenUris = new ArrayList<String>();
                            lastChildrenUris.add(third.getURI().trim());
                            // NEW!
                            lastChildrenInstanceUris = new ArrayList<String>();
                            lastChildrenInstanceUris.addAll(thirdInstanceRoots);
                            // rootNodesForLastElement=new
                            // ArrayList<ResultGraph>();
                            rootNodesForLastElement = thirdListRoot;
                            otherNodesForLastElement = thirdListOther;
                            // root remains the same
                            logger.info("that was CASE 2 and the final graph now looks like:"
                                            + finalGraph.toString());
                            logger.debug("graph consistent?" + isGraphConsistent(finalGraph));
                        }// end of case 2
                         // **************************************
                         // case 3:
                         // **************************************
                        else if (rootUris.contains(third.getURI().trim())
                                        && rootInstanceUris.containsAll(thirdInstanceRoots)) {
                            Iterator<ResultGraph> secondI = secondListRoot.iterator();
                            for (ResultGraph node : firstListRoot) {
                                ResultGraph property = null;
                                if (secondI.hasNext()) {
                                    property = (ResultGraph) secondI.next();
                                    node.getAdjacencies().add(property.getId());
                                }
                            }
                            if (firstListRoot != null) finalGraph.addAll(firstListRoot);
                            if (firstListOther != null) finalGraph.addAll(firstListOther);
                            Iterator<ResultGraph> rootI = rootNodes.iterator();
                            for (ResultGraph node : secondListRoot) {
                                ResultGraph element = null;
                                if (rootI.hasNext()) {
                                    element = (ResultGraph) rootI.next();
                                    node.getAdjacencies().add(element.getId());
                                }
                            }
                            if (secondListRoot != null) finalGraph.addAll(secondListRoot);
                            if (secondListOther != null) finalGraph.addAll(secondListOther);
                            // finalGraph = updateGraphWithNewAdjacencies(
                            // finalGraph, secondListOther);
                            rootUris = new ArrayList<String>();
                            rootUris.add(first.getURI());
                            // new!
                            rootInstanceUris = new ArrayList<String>();
                            rootInstanceUris.addAll(firstInstanceRoots);
                            // lastChild remains the same
                            logger.info("that was CASE 3 and the final graph now looks like:"
                                            + finalGraph.toString());
                            logger.debug("graph consistent?" + isGraphConsistent(finalGraph));
                        }// end of case 3
                         // ******************************************************
                         // case 4:
                         // ******************************************************
                        else if (lastChildrenUris.contains(third.getURI().trim())
                                        && lastChildrenInstanceUris.containsAll(thirdInstanceRoots)) {
                            Iterator<ResultGraph> secondI = secondListRoot.iterator();
                            for (ResultGraph node : firstListRoot) {
                                ResultGraph property = null;
                                if (secondI.hasNext()) {
                                    property = (ResultGraph) secondI.next();
                                    node.getAdjacencies().add(property.getId());
                                }
                            }
                            if (firstListRoot != null) finalGraph.addAll(firstListRoot);
                            if (firstListOther != null) finalGraph.addAll(firstListOther);
                            secondI = secondListRoot.iterator();
                            for (ResultGraph lastElement : rootNodesForLastElement) {
                                ResultGraph property = null;
                                if (secondI.hasNext()) {
                                    property = (ResultGraph) secondI.next();
                                    property.getAdjacencies().add(lastElement.getId());
                                }
                            }
                            if (secondListRoot != null) finalGraph.addAll(secondListRoot);
                            if (secondListOther != null) finalGraph.addAll(secondListOther);
                            // last children remains the same
                            // what happens to the root?
                            // rootUris = new ArrayList<String>();
                            rootUris.add(first.getURI());
                            // new!
                            // rootInstanceUris = new ArrayList<String>();
                            rootInstanceUris.addAll(firstInstanceRoots);
                            // rootNodesForLastElement=new
                            // ArrayList<ResultGraph>();
                            // rootNodesForLastElement=thirdListRoot;
                            logger.info("that was CASE 4 and the final graph now looks like:"
                                            + finalGraph.toString());
                            logger.debug("graph consistent?" + isGraphConsistent(finalGraph));
                        }// end of case 4
                        else {
                            logger
                                            .info("This is CASE 5, this case does not exist and you have done something wrong!!!");
                        }
                    }// if first and third are not null
                }
            }// if elements size>2
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.debug("graph from ALL triples is consistent?"
                        + isGraphConsistent(finalGraph));
        return finalGraph;
    }

    /**
     * removes properties from the graph and adds their 'data' to the previous node
     * 
     * @param graph
     * @return
     */
    public static List<ResultGraph> removePropertiesAsNodesAndOptimizeGraph(
                    List<ResultGraph> graph) {
        List<ResultGraph> list = new ArrayList<ResultGraph>();
        // ResultGraph previous = null;
        List<HashMap<String, Integer>> oldAndNewId =
                        new ArrayList<HashMap<String, Integer>>();
        for (ResultGraph propertyNode : graph) {
            if (CATConstants.TYPE_PROPERTY.equals(propertyNode.getType())) {
                // skip it and read its id which becomes oldId, then find the
                // newId which is the id of the node which is incoming
                // need something smarter here
                // if (previous != null)
                // previous.getData().addAll(node.getData());
                int idToBeUpdated = propertyNode.getId();
                // now delete all adjacencies to this node and convert them
                // to the id of the node which is already in 'result' list
                List<Integer> newIds = propertyNode.getAdjacencies();
                for (Integer newId : newIds) {
                    HashMap<String, Integer> map = new HashMap<String, Integer>();
                    map.put("old", idToBeUpdated);
                    map.put("new", newId);
                    // logger.debug("for uri:\n" + map.toString());
                    oldAndNewId.add(map);
                }
            } else {
                list.add(propertyNode);
                // previous = propertyNode;
            }
        }
        // now iterate through the whole result and update ids
        for (HashMap<String, Integer> m : oldAndNewId) {
            logger
                            .debug("list to be updated (should match with above listed)::::::::::::::::::::::;"
                                            + m.toString());
            int oldId = (Integer) m.get("old");
            int newId = (Integer) m.get("new");
            for (ResultGraph element : list) {
                List<Integer> adjs = element.getAdjacencies();
                // int currentId = element.getId();
                if (adjs.contains(oldId)) {
                    adjs.remove(adjs.indexOf(oldId));
                    adjs.add(newId);
                }
                Set<Integer> ajdsNoDuplicates = new HashSet<Integer>();
                ajdsNoDuplicates.addAll(adjs);
                element.setAdjacencies(new ArrayList<Integer>(ajdsNoDuplicates));
            }
        }
        return list;
    }

    /**
     * optimizes graph check if any node has the same uri (skip properties) if yes: - delete duplicates - update
     * adjaciencies in other nodes (find node which have // theNodeToBeDeleted as adjacency and update it with the only
     * // preserivng node)
     * 
     * @param ignoreProperties when true then ignore properties when analysing graph, otherwise, consider them as any
     *        other type
     * @param finalGraph
     * @return
     */
    public static List<ResultGraph> optimizeGraph(List<ResultGraph> finalGraph) {
        List<ResultGraph> result = new ArrayList<ResultGraph>();
        List<String> uris = new ArrayList<String>();
        List<HashMap> oldAndNewId = new ArrayList<HashMap>();
        for (ResultGraph node : finalGraph) {
            if ((!CATConstants.TYPE_PROPERTY.equals(node.getType()))) {
                logger.debug("checking node:\n " + node.toString());
                String uri = node.getURI();
                if (uris.contains(uri)) {
                    // eliminate duplicate
                    logger
                                    .debug("node with this URI has already been added:\n "
                                                    + uri
                                                    + "...now finding its id and finding the id with which to update...");
                    Integer idToBeUpdated = node.getId();
                    // now delete all adjacencies to this node and convert them
                    // to the id of the node which is already in 'result' list
                    Integer newId = 0;
                    logger.debug("old id is:" + idToBeUpdated);
                    for (ResultGraph g : result) {
                        String currentUri = g.getURI();
                        if (uri.equals(currentUri)) {
                            newId = g.getId();
                            logger.info(" new id is:" + newId);
                        }
                    }
                    HashMap map = new HashMap();
                    map.put("old", idToBeUpdated);
                    map.put("new", newId);
                    map.put("outcomings", node.getAdjacencies());
                    logger
                                    .debug("old adjacencies of the node which needs to be updated is:"
                                                    + node.getAdjacencies().toString());
                    // what about the nodes which have as adjacencies this node?
                    // those need be removed/updated???
                    map.put("data", node.getData());
                    oldAndNewId.add(map);
                } else {
                    logger.debug("adding this node to the list of final nodes:"
                                    + node.toString());
                    result.add(node);
                    uris.add(uri);
                }
            } else {
                result.add(node);
            }
        }
        logger
                        .debug("Here is the list of old and new elements (BEFORE updated outcomings): "
                                        + oldAndNewId.toString());
        // update all outcomings before updating adjacencies for elements
        List<HashMap> updatedoldAndNewId = updateOutComings(oldAndNewId);
        logger
                        .debug("Here is the list of old and new elements (AFTER updated outcomings): "
                                        + updatedoldAndNewId.toString());
        // now iterate through the whole result and update ids
        logger
                        .debug("now FINAL update and removing references to old nodes.........");
        for (ResultGraph graph : result) {
            logger.debug("checking node:" + graph.toString());
            List<Integer> adjs = graph.getAdjacencies();
            Set<Integer> adjsNoDuplicates = new HashSet();
            adjsNoDuplicates.addAll(adjs);
            adjs = new ArrayList(adjsNoDuplicates);
            int currentId = graph.getId();
            for (HashMap m : updatedoldAndNewId) {
                // logger.debug("old::::::::::::::::::::::;" + m.toString());
                int oldId = (Integer) m.get("old");
                int newId = (Integer) m.get("new");
                List data = (List) m.get("data");
                List<Integer> outcomings = (List) m.get("outcomings");
                if (adjs.contains(oldId)) {
                    adjs.remove(adjs.indexOf(oldId));
                    adjs.add(newId);
                    graph.getData().addAll(data);
                }
                if (currentId == newId) {
                    adjs.addAll(outcomings);
                }
                Set<Integer> ajdsNoDuplicates = new HashSet<Integer>();
                ajdsNoDuplicates.addAll(adjs);
                graph.setAdjacencies(adjs);
            }
        }
        return result;
    }

    /**
     * updates adjacencies of the nodes which are removed from the graph
     * 
     * @param oldAndNewId
     * @return
     */
    public static List updateOutComings(List<HashMap> oldAndNewId) {
        List<HashMap> result = new ArrayList<HashMap>();
        List<Integer> oldIds = new ArrayList<Integer>();
        HashMap<Integer, Integer> updatedIds = new HashMap<Integer, Integer>();
        for (HashMap<Integer, Integer> m : oldAndNewId) {
            Integer oldId = (Integer) m.get("old");
            Integer newId = (Integer) m.get("new");
            logger.info("now reading old:" + oldId + " and new: " + newId);
            updatedIds.put(oldId, newId);
        }
        Set<Integer> idsToBeUpdated = updatedIds.keySet();
        logger.info("ids to be updated:" + idsToBeUpdated.toString());
        for (HashMap m : oldAndNewId) {
            List<Integer> oldOutcomings = (List) m.get("outcomings");
            logger.info("old outcomings are:" + oldOutcomings.toString());
            Set oldOutcomingsNoDuplicates = new HashSet();
            oldOutcomingsNoDuplicates.addAll(oldOutcomings);
            for (Integer id : idsToBeUpdated) {
                if (oldOutcomingsNoDuplicates.contains(id)) {
                    logger.info("now removing: " + id);
                    logger.info("replacing it with: " + updatedIds.get(id));
                    oldOutcomingsNoDuplicates.remove(id);
                    oldOutcomingsNoDuplicates.add((updatedIds.get(id)));
                }
            }
            logger.info("now updated outcomings:" + oldOutcomingsNoDuplicates);
            m.put("outcomings", new ArrayList(oldOutcomingsNoDuplicates));
            result.add(m);
        }
        return result;
    }

    /**
     * This method creates HashMap with key 'root' where the value is a list of ResultGraphs which are in the root ready
     * to be connected to the rest of the graph in the next step and key 'others' with list of ResultGraphs which are
     * connected to the root only and not important for connection in the next steps for example instances connect with
     * other instances so their uris are stored in other but they are connected to the root element class based on the
     * InterpretationElement type and using data from the cell object InstanceElement: other: classNode root:
     * instanceNode ClassElement: other: classNode root: instanceNode DatatypePropertyValueElement: PropertyElement:
     * JokerElement (type="class"): at this point there should not be a Joker with type "property" so 2 options: 1.
     * joker represents a class: create both root and others 2. joker represents a datatype property: - create root only
     * 
     * @param cell
     * @return
     * @throws Exception
     */
    public HashMap<String, List<ResultGraph>> getResultGraphFromOntologyElement(
                    List<OntologyElement> cellColumn) throws Exception {
        OntologyElement cell = cellColumn.get(0);
        HashMap<String, List<ResultGraph>> result =
                        new HashMap<String, List<ResultGraph>>();
        List<ResultGraph> rootGraph = new ArrayList<ResultGraph>();
        List<ResultGraph> otherGraph = new ArrayList<ResultGraph>();
        int i = 0;
        if (cell instanceof ClassElement) {
            Map map = processClass(cell);
            rootGraph.addAll((List) map.get("root"));
            otherGraph.addAll((List) map.get("other"));
        } else if (cell instanceof JokerElement) {
            JokerElement jokerElement = (JokerElement) cell;
            try {
                SerializableURI uri = null;
                List<String> setOfInstanceUris = cell.getResults();
                if (setOfInstanceUris != null && setOfInstanceUris.size() > 0)
                    uri = new SerializableURI(setOfInstanceUris.get(0), false);
                if ("class".equals(jokerElement.getType())) {
                    Map<String, List<ResultGraph>> map = processClass(cell);
                    rootGraph.addAll((List<ResultGraph>) map.get("root"));
                    otherGraph.addAll((List<ResultGraph>) map.get("other"));
                } else {// joker is property
                    rootGraph.addAll(processProperty(cell));
                }
                // also create othergraph and put classtype there in
                // case that it exist
            } catch (Exception e) {
                // here create one node as this means that joker represents
                // datatypepropertyvalue
                rootGraph.addAll(processDatatypePropertyValue(cell));
            }
        }// datatypepropertyvalue jokers sorted
        else if (cell instanceof PropertyElement) {
            rootGraph.addAll(processProperty(cell));
        } else if (cell instanceof InstanceListElement) {
            Map map = processInstance(cell);
            rootGraph.addAll((List) map.get("root"));
            otherGraph.addAll((List) map.get("other"));
        } else {// if this is DatatypePropertyValueElement
            rootGraph.addAll(processDatatypePropertyValue(cell));
        }
        i = i + 1;
        result.put("root", rootGraph);
        result.put("other", otherGraph);
        logger.debug("Created nodes:" + result.toString());
        return result;
    }

    /**
     * generates rootGraph with instances and otherGraph with classes
     * 
     * @param cell
     * @return
     */
    Map<String, List<ResultGraph>> processClass(OntologyElement cell) {
        Map<String, List<ResultGraph>> result =
                        new HashMap<String, List<ResultGraph>>();
        List<ResultGraph> rootGraph = new ArrayList<ResultGraph>();
        List<ResultGraph> otherGraph = new ArrayList<ResultGraph>();
        List<String> setOfInstanceUris = cell.getResults();
        if (setOfInstanceUris.size() > 0) {
            ResultGraph classNode = new ResultGraph();
            classNode.setMainSubject(cell.isMainSubject());
            String classUri = null;
            if (cell instanceof JokerElement)
                classUri = getDirectType(setOfInstanceUris);
            else
                classUri = ((ClassElement) cell).getData().toString();
            classNode.setURI(classUri);
            List<String> removeDuplicates = new ArrayList<String>();
            for (String uri : setOfInstanceUris) {
                if (!removeDuplicates.contains(uri)) removeDuplicates.add(uri);
            }
            classNode.setData(removeDuplicates);
            // classNode.setData(setOfInstanceUris);
            classNode.setType(CATConstants.FEATURE_TYPE_CLASS);
            int classId = idCounter.incrementAndGet();
            classNode.setId(classId);
            otherGraph.add(classNode);
            // here generate instances
            for (String instanceUri : setOfInstanceUris) {
                ResultGraph instanceNode = new ResultGraph();
                instanceNode.setURI(instanceUri);
                int instanceId = idCounter.incrementAndGet();
                instanceNode.setId(instanceId);
                List<String> listWithClassUri = new ArrayList<String>();
                listWithClassUri.add(classUri);
                instanceNode.setData(listWithClassUri);
                List adj = new ArrayList();
                adj.add(classId);
                instanceNode.setAdjacencies(adj);
                instanceNode.setType(CATConstants.TYPE_INSTANCE);
                // instance node is not a main subject
                // instanceNode.setMainSubject(cell.isMainSubject());
                rootGraph.add(instanceNode);
            }
        }
        result.put("root", rootGraph);
        result.put("other", otherGraph);
        return result;
    }

    /**
     * generates rootgraph from the cell which represents datatypepropertyvalue
     * 
     * @param cell
     * @return
     */
    static List<ResultGraph> processDatatypePropertyValue(OntologyElement cell) {
        List<ResultGraph> rootGraph = new ArrayList<ResultGraph>();
        List<String> setOfValues = cell.getResults();
        /*
         * TODO this should be something more clever than just default sorting, as this method will sort datatype
         * property values as string even if they are numbers
         * 
         * kakvo crno sortiranje ovde???? pa zato i bugovi...uh
         */
        // Collections.sort(setOfValues);
        for (String uri : setOfValues) {
            ResultGraph datatypePropertyValueNode = new ResultGraph();
            try {
                SerializableURI sampleUriValidation = new SerializableURI(uri, false);
                datatypePropertyValueNode.setURI(uri);
                List setOfValuesNoDuplicates = new ArrayList();
                // setOfValuesNoDuplicates.addAll(setOfValues);
                for (String value : setOfValues) {
                    if (!setOfValuesNoDuplicates.contains(value))
                        setOfValuesNoDuplicates.add(value);
                }
                datatypePropertyValueNode.setData(setOfValuesNoDuplicates);
                datatypePropertyValueNode
                                .setType(CATConstants.TYPE_DATATYPE_PROPERTY_VALUE);
            } catch (Exception ie) {
                datatypePropertyValueNode.setURI(uri);
                // Set setOfValuesNoDuplicates = new HashSet();
                // setOfValuesNoDuplicates.addAll(setOfValues);
                List setOfValuesNoDuplicates = new ArrayList();
                // setOfValuesNoDuplicates.addAll(setOfValues);
                for (String value : setOfValues) {
                    if (!setOfValuesNoDuplicates.contains(value))
                        setOfValuesNoDuplicates.add(value);
                }
                datatypePropertyValueNode.setData(setOfValuesNoDuplicates);
                // datatypePropertyValueNode.setData(setOfValues);
                datatypePropertyValueNode
                                .setType(CATConstants.TYPE_DATATYPE_PROPERTY_VALUE);
            }
            // if not already added
            int id = idCounter.incrementAndGet();
            datatypePropertyValueNode.setId(id);
            datatypePropertyValueNode.setMainSubject(cell.isMainSubject());
            rootGraph.add(datatypePropertyValueNode);
        }// iterate through list
        return rootGraph;
    }

    /**
     * process property and create root graph
     * 
     * @param cell
     * @return
     */
    static List<ResultGraph> processProperty(OntologyElement cell) {
        List<ResultGraph> rootGraph = new ArrayList<ResultGraph>();
        List<String> setOfPropertyUris = cell.getResults();
        List<String> list = new ArrayList<String>();
        if ( cell.getResults() == null) return rootGraph;
        for (String uri : setOfPropertyUris) {
            ResultGraph propertyNode = new ResultGraph();
            propertyNode.setURI(uri);
            list.add(uri);
            propertyNode.setData(list);
            propertyNode.setType(CATConstants.TYPE_PROPERTY);
            int id = idCounter.incrementAndGet();
            propertyNode.setId(id);
            propertyNode.setMainSubject(cell.isMainSubject());
            rootGraph.add(propertyNode);
            // System.out.print("property:"+uri+"\n");
        }
        return rootGraph;
    }

    /**
     * generates map with key 'root':instanceElement and 'other': classElement and value Resultgraphs
     * 
     * @param cell
     * @return
     */
    static Map<String, List<ResultGraph>> processInstance(OntologyElement cell) {
        Map<String, List<ResultGraph>> result =
                        new HashMap<String, List<ResultGraph>>();
        List<ResultGraph> rootGraph = new ArrayList<ResultGraph>();
        List<ResultGraph> otherGraph = new ArrayList<ResultGraph>();
        List<String> setOfInstanceUris = cell.getResults();
        ResultGraph instanceNode = null;
        String directType = null;
        int classId = idCounter.incrementAndGet();
        int instanceId = idCounter.incrementAndGet();
        if (((InstanceListElement) cell).getClassURI() != null)
            directType = ((InstanceListElement) cell).getClassURI().toString();
        List<String> list = new ArrayList<String>();
        if (directType != null) list.add(directType);
        if (setOfInstanceUris.size() > 0) {
            for (String instanceUri : setOfInstanceUris) {
                instanceNode = new ResultGraph();
                instanceNode.setURI(instanceUri);
                instanceNode.setData(list);
                instanceId = idCounter.incrementAndGet();
                instanceNode.setId(instanceId);
                // adding instance node and then adding class node as its
                // adjacency
                List<Integer> adjacencies = new ArrayList<Integer>();
                adjacencies.add(classId);
                instanceNode.setAdjacencies(adjacencies);
                instanceNode.setType(CATConstants.TYPE_INSTANCE);
                // instanceNode.setMainSubject(cell.isMainSubject());
                rootGraph.add(instanceNode);
            }
            ResultGraph classNode = new ResultGraph();
            classNode.setType(CATConstants.FEATURE_TYPE_CLASS);
            List<String> dataList = new ArrayList<String>();
            for (String uri : setOfInstanceUris) {
                if (!dataList.contains(uri)) dataList.add(uri);
            }
            classNode.setData(dataList);
            classNode.setURI(directType);
            classNode.setId(classId);
            classNode.setMainSubject(cell.isMainSubject());
            otherGraph.add(classNode);
        }
        result.put("root", rootGraph);
        result.put("other", otherGraph);
        return result;
    }

    /**
     * this method tries to detect if the first uri in the list is valid URI if yes then it calls getClassType to detect
     * the class type for instances otherwise it returns that URI as that is the literal value then probalby...
     * 
     * @param instanceUris
     * @return
     */
    public String getClassTypeOrLiteralValue(List<String> instanceUris) {
        String result = null;
        if (instanceUris != null && instanceUris.size() > 0) {
            String sampleUri = instanceUris.get(0);
            try {
                SerializableURI sampleUriUri = new SerializableURI(sampleUri, false);
                result = getClassType(instanceUris);
            } catch (Exception iue) {
                // result = sampleUri;
                // result = "value";
                result = sampleUri;
            }
        }
        return result;
    }

    /**
     * Returns the class type which is the most specific out of all direct types for the set of given instances
     * 
     * @param instanceUris
     * @return
     */
    public String getClassType(List<String> instanceUris) {
        String theMostSpecific = "";
        // dynamically create sparql query and execute it
//
//         HashMap<String, Double> scores = ontology2Map.getSpecificityScores();//OntoResAnaluseUtils...
//
//        
//        Double maxScore = -0.1;
//        for (String uri : instanceUris) {
//
//            List<String> directTypes = luceneAnnotator.findDirectTypes(uri);// ontology2Map.getLuceneAnnotator().findDirectTypes(uri);
//
//            String directType = null;
//            if (directTypes != null && directTypes.size() > 0)
//                directType = directTypes.get(0);
//            Double currentScore = scores.get(directType);
//            if (currentScore > maxScore) {
//                maxScore = currentScore;
//                theMostSpecific = directType;
//            }
//            // }
//            // }
//        }
        logger.info("Calculating specificity scores not implemented in this new version .....");
        return theMostSpecific;
    }

    /**
     * creates list of graphs from the first triple
     * 
     * @param firstRow
     * @return
     */
    public List<ResultGraph> getGraphFromTheFirstTriple(TripleElement firstTriple) {
        List<ResultGraph> finalGraph = new ArrayList<ResultGraph>();
        // number of elements is usually 3 except in the case when there is only
        // one concept
        int numberOfElements = firstTriple.getElements().size();
        logger.info("host triple element:"+firstTriple);
        logger.info("numberofelement:"+numberOfElements);
        try {
            HashMap graphFromTheFirstTripleElement =
                            getResultGraphFromOntologyElement(firstTriple.getElements()
                                            .get(0));
            List<ResultGraph> rootNodes =
                            (List<ResultGraph>) graphFromTheFirstTripleElement.get("root");
            List<ResultGraph> otherNodes =
                            (List<ResultGraph>) graphFromTheFirstTripleElement.get("other");
            // the first in the list is always root
            if (rootNodes != null) {
                finalGraph.addAll(rootNodes);
            }
            if (otherNodes != null) {
                finalGraph.addAll(otherNodes);
            }
            List<ResultGraph> rootNodesForMiddleElement = null;
            List<ResultGraph> otherNodesForMiddleElement = null;
            List<ResultGraph> rootNodesForLastElement = null;
            List<ResultGraph> otherNodesForLastElement = null;
            // rootNodesForLastElement = rootNodes;
            if (numberOfElements > 1) {
                if (firstTriple.getElements().size() > 1) {
                    HashMap graphFromTheTripleElement =
                                    getResultGraphFromOntologyElement(firstTriple.getElements()
                                                    .get(1));
                    rootNodesForMiddleElement =
                                    (List<ResultGraph>) graphFromTheTripleElement.get("root");
                    otherNodesForMiddleElement =
                                    (List<ResultGraph>) graphFromTheTripleElement.get("other");
                    // rootNodesForLastElement = rootNodesForMiddleElement;
                }
                if (firstTriple.getElements().size() > 2) {
                    HashMap graphFromTheRow =
                                    getResultGraphFromOntologyElement(firstTriple.getElements()
                                                    .get(2));
                    rootNodesForLastElement =
                                    (List<ResultGraph>) graphFromTheRow.get("root");
                    otherNodesForLastElement =
                                    (List<ResultGraph>) graphFromTheRow.get("other");
                }
                if (rootNodesForMiddleElement != null
                                && rootNodesForMiddleElement.size() > 0) {
                    Iterator<ResultGraph> iMiddle = rootNodesForMiddleElement.iterator();
                    for (ResultGraph node : rootNodes) {
                        if (iMiddle.hasNext()) {
                            ResultGraph propertyNode = (ResultGraph) iMiddle.next();
                            node.getAdjacencies().add(propertyNode.getId());
                        }
                    }
                    if (rootNodesForMiddleElement != null)
                        finalGraph.addAll(rootNodesForMiddleElement);
                    if (otherNodesForMiddleElement != null)
                        finalGraph.addAll(otherNodesForMiddleElement);
                }
                if (rootNodesForLastElement != null && rootNodesForMiddleElement != null) {
                    Iterator<ResultGraph> iLast = rootNodesForLastElement.iterator();
                    // for (ResultGraph lastEl : rootNodesForLastElement) {
                    for (ResultGraph middleElement : rootNodesForMiddleElement) {
                        if (iLast.hasNext()) {
                            ResultGraph lastElement = (ResultGraph) iLast.next();
                            middleElement.getAdjacencies().add(lastElement.getId());
                        }
                    }
                    if (rootNodesForLastElement != null)
                        finalGraph.addAll(rootNodesForLastElement);
                    if (otherNodesForLastElement != null)
                        finalGraph.addAll(otherNodesForLastElement);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalGraph;
    }

    /**
     * this method is cheating, needs to be fixed to really retrieve what it supposed to
     * 
     * @param uris
     * @return
     */
    public String getDirectType(List<String> uris) {
        String theMostSpecific = "no direct type";
        for (String uri : uris) {
            theMostSpecific = uri;
            List<String> dirTypes = luceneAnnotator.findDirectTypes(uri);
            if (dirTypes != null && dirTypes.size() > 0)
                theMostSpecific = (String) dirTypes.iterator().next();
        }
        return theMostSpecific;
    }
}
