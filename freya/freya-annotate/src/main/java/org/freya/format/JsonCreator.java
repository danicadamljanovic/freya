package org.freya.format;


import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.freya.annotate.lucene.LuceneAnnotator;
import org.freya.model.ClassElement;
import org.freya.model.DatatypePropertyValueElement;
import org.freya.model.InstanceElement;
import org.freya.model.InstanceListElement;
import org.freya.model.JsonSuggestion;
import org.freya.model.JsonVote;
import org.freya.model.OntologyElement;
import org.freya.model.PropertyElement;
import org.freya.model.ResultGraph;
import org.freya.model.SemanticConcept;
import org.freya.model.SerializableURI;
import org.freya.model.SuggestionPair;
import org.freya.model.Vote;
import org.freya.model.service.FreyaResponse;
import org.freya.rdf.query.CATConstants;
import org.freya.util.FreyaConstants;
import org.freya.util.GraphUtils;
import org.freya.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JsonCreator {
    private static final Log logger = LogFactory.getLog(JsonCreator.class);
    @Autowired GraphUtils graphUtils;
    @Autowired LuceneAnnotator luceneAnnotator;


    public JsonCreator() {}

    /**
     * generates json string from list of json suggestions
     * 
     * @param question
     * @return
     */
    public ObjectNode fromSuggestionsToJson(SuggestionPair pair) {
        List<JsonSuggestion> jsonSuggestions = generateJsonSuggestions(pair);
        ObjectNode topNode = JsonNodeFactory.instance.objectNode();
        ArrayNode clarificationNodes = JsonNodeFactory.instance.arrayNode();
        for (JsonSuggestion suggestion : jsonSuggestions) {
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.put("stringToClarify", suggestion.getText());
            // here beautify the key and add label not uri
            ArrayNode optionsNode = JsonNodeFactory.instance.arrayNode();
            List<JsonVote> list = new ArrayList<JsonVote>(suggestion.getVotes());
            for (JsonVote jsonVote : list) {
                ObjectNode userVoteNode = JsonNodeFactory.instance.objectNode();
                userVoteNode.put("id", jsonVote.getId());
                userVoteNode.put("option", jsonVote.getCandidate());
                userVoteNode.put("vote", jsonVote.getVote());
                String function = "-";
                if (jsonVote.getFunction() != null) {
                    function = jsonVote.getFunction();
                }
                userVoteNode.put("function", function);
                optionsNode.add(userVoteNode);
            }
            node.put("clarificationOptions", optionsNode);
            clarificationNodes.add(node);
        }
        topNode.put("clarifications", clarificationNodes);
        return topNode;// topNode.toString();
    }

    @Value("${org.freya.use.labels}") String useLabels;
    @Value("${org.freya.max.suggestions}") String maxSuggestions;

    /**
     * generates json suggestions from suggestions
     * 
     * @param suggestions
     * @return
     */
    public List<JsonSuggestion> generateJsonSuggestions(SuggestionPair pair) {

        List<JsonSuggestion> list = new ArrayList<JsonSuggestion>();
        JsonSuggestion jsonSuggestion = new JsonSuggestion();
        jsonSuggestion.setText(pair.getKey().getText());
        jsonSuggestion.setVotes(new ArrayList<JsonVote>());
        // here beautify the key and add label not uri
        List<Vote> votes = pair.getVote();
        for (Vote vote : votes) {
            // to ensure that java script does not go crazy with 200 000 suggestions
            if (jsonSuggestion.getVotes().size() > new Long(maxSuggestions)) {
                // ovde ne bi trebalo samo ovakoo nego i da se nadje none i da se doda
                break;
            }


            SemanticConcept sc = vote.getCandidate();
            OntologyElement ontologyElement = null;
            if (sc != null) {
                ontologyElement = sc.getOntologyElement();
            } else {
                String v = null;
                if (vote != null) v = vote.toString();
                logger.error("Candidate is null!! How this happened? Vote:" + v);
            }
            String keyLabel = null;// getLabel(uri);
            // change keyLabel based on what is oc as when its instance we want
            // dialog with classes
            keyLabel = findKeyLabel(ontologyElement, new Boolean(useLabels));
            String function = sc.getFunction();
            JsonVote jsonVote =
                            new JsonVote(keyLabel, new Double(vote.getVote()).toString(),
                                            new Long(vote.getId()).toString(), function);
            jsonSuggestion.getVotes().add(jsonVote);
            logger.info("keylabel (shouldn't be null):" + keyLabel);
        }
        list.add(jsonSuggestion);
        // logger.info("jsonSuggestions:" + jsonSuggestion.toString());
        return list;
    }

    /**
     * when there are no interpretations found create relevant json (empty nodes)
     * 
     * @return
     */
    String getNoInterpretationsFoundNode() {
        ObjectNode topNode = JsonNodeFactory.instance.objectNode();
        // ArrayNode interpretations = JsonNodeFactory.instance.arrayNode();
        ArrayNode clarifications = JsonNodeFactory.instance.arrayNode();
        /* this is faking just so that js does not complain */
        // topNode.put("interpretations", interpretations);
        topNode.put("clarifications", clarifications);
        return topNode.toString();
    }

    /**
     * returns suggestion string based on the type of ontology element
     * 
     * @param ontologyElement
     * @param useLabels
     * @return
     */
    String findKeyLabel(OntologyElement ontologyElement, boolean useLabels) {
        String keyLabel = "";
        String uri = (ontologyElement.getData()).toString();
        Set<String> instanceUriLabels = new HashSet<String>();
        if (!useLabels) {
            if (ontologyElement instanceof InstanceElement) {
                List<String> classUris =
                                ((InstanceElement) ontologyElement).getClassURIList();
                StringBuffer classUrisString = new StringBuffer("");
                for (String cUri : classUris) {
                    classUrisString.append(cUri).append("|");
                }
                keyLabel = uri + " (" + classUrisString + ")";
            } else if (ontologyElement instanceof InstanceListElement) {
                List<SerializableURI> instanceUris =
                                ((InstanceListElement) ontologyElement).getData();
                String instanceLabels = "";
                for (SerializableURI instUri : instanceUris) {
                    instanceLabels = instanceLabels + "," + instUri.toString();
                }
                String classUri =
                                ((InstanceListElement) ontologyElement).getClassURI().toString();
                // keyLabel = getLabel(classUri);
                keyLabel = instanceLabels + " (" + classUri + ")";
            } else {
                keyLabel = uri;
            }
        } else if (useLabels) {
            if (ontologyElement instanceof InstanceElement) {
                List<String> classUris =
                                ((InstanceElement) ontologyElement).getClassURIList();
                if (classUris == null) classUris = new ArrayList<String>();
                logger.info("class  LIST for" + uri + " is:\n" + classUris.toString());
                String instanceLabel = getLabels(uri);
                String classesString = "";
                for (String classUri : classUris) {
                    classesString = classesString + getLabels(classUri) + " |OR| ";
                }
                keyLabel = instanceLabel + " (" + getLabels(classesString) + ")";;
            } else if (ontologyElement instanceof InstanceListElement) {
                List<SerializableURI> instanceUris =
                                ((InstanceListElement) ontologyElement).getData();
                String instanceLabels = "";
                for (SerializableURI instUri : instanceUris) {
                    instanceUriLabels.add(getLabels(instUri.toString()));
                }
                List<String> classUris =
                                ((InstanceListElement) ontologyElement).getClassURIList();
                for (String s : instanceUriLabels) {
                    if (s != null && !"".equals(s.trim()))
                        instanceLabels = instanceLabels + s + ",";
                }
                instanceLabels =
                                instanceLabels.substring(0, instanceLabels.lastIndexOf(","));
                // keyLabel = instanceLabels + " (" + getLabels(classUri) + ")";
                String classesString = "";
                for (String classUri : classUris) {
                    classesString = classesString + getLabels(classUri) + " |OR| ";
                }
                keyLabel = instanceLabels + " (" + classesString + ")";
            } else if (ontologyElement instanceof ClassElement
                            || ontologyElement instanceof PropertyElement) {
                keyLabel = uri;
            } else if (ontologyElement instanceof DatatypePropertyValueElement) {
                DatatypePropertyValueElement el =
                                ((DatatypePropertyValueElement) ontologyElement);
                keyLabel =
                                uri;
                // + " ( "
                // + ((DatatypePropertyValueIdentifier)el.getData())
                // .getInstanceURI()
                // + " "
                // + ((DatatypePropertyValueIdentifier)el.getData())
                // .getPropertyUri() + ")";
            } else {
                keyLabel = getLabels(uri);
            }
        }
        return keyLabel;
    }



    /**
     * @param graph
     * @return
     */
    @SuppressWarnings("finally")
    public FreyaResponse createJsonNodeFromResultGraph(List<ResultGraph> aGraph,
                    List<List<String>> table, FreyaResponse response) {
        // ObjectNode topNode = JsonNodeFactory.instance.objectNode();
        //FreyaResponse topNode = new FreyaResponse();
        if (aGraph == null || (aGraph != null && aGraph.size() == 0)) {// result
            // is
            // empty
            // and
            // call
            // some refinement
            return getRefinementNode();
        }
        // ArrayNode nodes = JsonNodeFactory.instance.arrayNode();
        ArrayList<Object> nodes = new ArrayList();
        try {
            // get all labels for the nodes for which we know URIs at the moment
            HashMap nodeNames = getNodeNames(aGraph);
            // then, find out the names for edges
            HashMap propertyNames = getPropertyNames(aGraph, nodeNames);
            // iterate through all nodes in order to find the main subject: this
            // might be stupid for the moment, but cannot figure out the
            // alternative way to find the name of the property which is a main
            // subject
            String propertyHeader = null;
            Integer id = null;
            ResultGraph propertyNode = null;
            List<ResultGraph> bGraph = new ArrayList<ResultGraph>();
            bGraph.addAll(aGraph);
            for (ResultGraph aNode : aGraph) {
                boolean isMSubject = false;
                if (aNode.isMainSubject() != null)
                    isMSubject = aNode.isMainSubject().booleanValue();
                if (isMSubject) {
                    for (ResultGraph bNode : bGraph) {
                        List<Integer> adjs = bNode.getAdjacencies();
                        if (adjs != null && adjs.size() > 0) {
                            if (adjs.contains(aNode.getId())) {
                                id = bNode.getId();
                                logger.debug("main subject is: " + aNode.getURI());
                                propertyNode = aNode;
                                // logger.info("found id for the property node is:"
                                // + id.toString());
                                break;
                            }
                        }
                    }
                }
            }
            // now find the graph with the id above: this will be the node which
            // represents the property which needs to be shown in the answer
            // for (ResultGraph cNode : aGraph) {
            // Integer nodeId = cNode.getId();
            // if (id != null && nodeId.intValue() == id.intValue()) {
            // propertyNode = cNode;
            // logger
            // .info("found node which is a property; that value of this property is a main subject:"
            // + propertyNode.getURI());
            // break;
            // }
            //
            // }
            if (propertyNode != null) {
                propertyHeader = getLabel(propertyNode.getURI());
                // logger
                // .info("now finding the label of the above property...the label is:"
                // + propertyHeader);
            }
            // System.out.print("nodeNames:\n" + nodeNames.toString());
            // update the graph
            List<ResultGraph> newGraph =
                            graphUtils.removePropertiesAsNodesAndOptimizeGraph(aGraph);
            logger.debug("optimized graph (NO properties):" + newGraph.toString());
            logger.debug("optimized graph (NO properties) consistent:"
                            + graphUtils.isGraphConsistent(newGraph));
            for (ResultGraph gNode : newGraph) {
                ObjectNode node = JsonNodeFactory.instance.objectNode();
                String label = getLabel(gNode.getURI());
                node.put("id", gNode.getId());
                String nodeName = label;
                if (CATConstants.FEATURE_TYPE_CLASS.equals(gNode.getType())) {
                    if (gNode.getData().size() > 1)
                        nodeName = nodeName + " (" + gNode.getData().size() + ")";
                }
                String propertyNodeName = label;
                node.put("name", nodeName);
                ObjectNode dataNode = JsonNodeFactory.instance.objectNode();
                ArrayNode answer = JsonNodeFactory.instance.arrayNode();
                ArrayNode blankAnswer = JsonNodeFactory.instance.arrayNode();
                // getData returns list of String (URIs)
                List<String> listOfUris = gNode.getData();
                for (String ch : listOfUris) {
                    answer.add(getLabel(ch));
                }
                dataNode.put("answer", answer);
                Font font = new Font("Arial", Font.PLAIN, 11);
                FontMetrics metrics = new FontMetrics(font) {};
                // Rectangle2D bounds = metrics.getStringBounds(nodeName, null);
                // int widthInPixels = (int)bounds.getWidth();
                // dataNode.put("$width", widthInPixels);
                // dataNode.put("$dim", widthInPixels);
                String type = gNode.getType();
                if (type != null && CATConstants.TYPE_PROPERTY.equals(type)) {
                    dataNode.put("$color", "#9F9595");
                    dataNode.put("$type", "custom-line");
                    // dataNode.put("$type", "square");
                    // just for now comment it out
                    // dataNode.put("$type", "none");
                    node.put("name", propertyNodeName);
                    dataNode.put("answer", blankAnswer);
                    // bounds = metrics.getStringBounds(propertyNodeName, null);
                    // widthInPixels = (int)bounds.getWidth();
                    // dataNode.put("$width", widthInPixels);
                    // dataNode.put("$dim", widthInPixels);
                    // dataNode.put("$color","#FFFFFF");
                } else if (type != null && CATConstants.FEATURE_TYPE_CLASS.equals(type)) {
                    // dataNode.put("$type", "circle");
                    dataNode.put("$color", "#BF7777");
                    // dataNode.put("$dim", widthInPixels);
                } else if (type != null && CATConstants.TYPE_INSTANCE.equals(type)) {
                    // dataNode.put("$type", "star");
                    // dataNode.put("$dim", widthInPixels);
                } else {
                    // dataNode.put("$type", "triangle");// datatype
                    // propertyvalues
                    // if (propertyHeader!=null)
                    // node.put("name", propertyHeader);
                    dataNode.put("$color", "#E0FF5F");
                    // dataNode.put("$dim", widthInPixels);
                }
                boolean isMSubject = false;
                if (gNode.isMainSubject() != null)
                    isMSubject = gNode.isMainSubject().booleanValue();
                dataNode.put("mainSubject", isMSubject);
                node.put("data", dataNode);
                // if (isMSubject && propertyHeader != null) {
                if (propertyHeader != null
                                && CATConstants.TYPE_DATATYPE_PROPERTY_VALUE.equals(gNode
                                                .getType())) {
                    dataNode.put("header", propertyHeader);
                    logger.debug("setting property header: " + propertyHeader);
                    dataNode.put("mainSubject", true);
                }
                ArrayNode newAdjacencies = JsonNodeFactory.instance.arrayNode();
                List<Integer> adjacencies = gNode.getAdjacencies();
                for (Integer adjacency : adjacencies) {
                    // System.out.println("now calling for adjacency:"
                    // + adjacency.toString());
                    ObjectNode adjacencyNode = JsonNodeFactory.instance.objectNode();
                    adjacencyNode.put("nodeTo", adjacency);
                    ObjectNode relationDataNode = JsonNodeFactory.instance.objectNode();
                    ArrayNode fromAndToId = JsonNodeFactory.instance.arrayNode();
                    fromAndToId.add(gNode.getId());
                    fromAndToId.add(adjacency);
                    String labelId =
                                    new Integer(gNode.getId()).toString() + "-"
                                                    + adjacency.toString();
                    // System.out.print("labelId " + labelId);
                    relationDataNode.put("$direction", fromAndToId);
                    String labelText = (String) propertyNames.get(labelId);
                    // System.out.print("\nlabelText " + labelText);
                    if (labelText != null) {
                        relationDataNode.put("labeltext", labelText);
                        if (labelId != null) relationDataNode.put("labelid", labelId);
                        relationDataNode.put("$type", "custom-line");
                    } else {
                        relationDataNode.put("$type", "arrow");
                    }
                    adjacencyNode.put("data", relationDataNode);
                    newAdjacencies.add(adjacencyNode);
                }
                node.put("adjacencies", newAdjacencies);
                nodes.add(node);
            }// for each node in a graph
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            List<List<HashMap>> tableResult = generateTable(table);
            response.setTable(tableResult);
            response.setGraph(nodes);
            // topNode.put(FreyaConstants.TABLE, tableResult);
            // topNode.put(FreyaConstants.GRAPH, nodes);
            // String rootnodestring = topNode.toString();
            // return rootnodestring;
            return response;
        }
    }

    /**
     * @param table
     * @return
     */
    List<List<HashMap>> generateTable(List<List<String>> table) {
        // ArrayNode tableResult = JsonNodeFactory.instance.arrayNode();
        List<List<HashMap>> tableResult = new ArrayList<List<HashMap>>();
        for (List<String> row : table) {
            // ArrayNode rowNode = JsonNodeFactory.instance.arrayNode();

            List<HashMap> rowNode = new ArrayList<HashMap>();
            for (String cell : row) {
                // ObjectNode cellNode = JsonNodeFactory.instance.objectNode();
                HashMap map = new HashMap();
                map.put("uri", cell);
                map.put("label", getLabel(cell));
                // cellNode.put("uri", cell);
                // cellNode.put("label", getLabel(cell));
                // rowNode.add(cellNode);
                rowNode.add(map);

            }
            tableResult.add(rowNode);
        }
        return tableResult;
    }

    /**
     * create empty node or add refinements
     * 
     * @return
     */
    public FreyaResponse getRefinementNode() {
        // method
        FreyaResponse response = new FreyaResponse();
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        // ArrayNode rootNodes = JsonNodeFactory.instance.arrayNode();
        List<Object> refinementNodes = new ArrayList<Object>();
        rootNode.put("id", 1);
        String message = "No results found.";// , how
        // about
        // refining your
        // query?";
        rootNode.put("name", message);
        String someRefinementSuggestion = "no refinement suggestions";// "this is refinement suggestion";
        ObjectNode dataNode = JsonNodeFactory.instance.objectNode();
        ArrayNode someRefinementSuggestionArray =
                        JsonNodeFactory.instance.arrayNode();
        someRefinementSuggestionArray.add(someRefinementSuggestion);
        dataNode.put("answer", someRefinementSuggestionArray);
        // dataNode.put("$type", "circle");
        // dataNode.put("$dim", "15");
        // Font font = new Font("Arial", Font.PLAIN, 10);
        /*
         * FontMetrics metrics = new FontMetrics(font) { }; Rectangle2D bounds = metrics.getStringBounds(message, null);
         * int widthInPixels = (int)bounds.getWidth(); dataNode.put("$width", widthInPixels);
         */
        rootNode.put("data", dataNode);
        ArrayNode adjacencies = JsonNodeFactory.instance.arrayNode();
        rootNode.put("adjacencies", adjacencies);
        refinementNodes.add(rootNode);
        
        response.setRefinementNodes(refinementNodes);
        return response;
    }

    /**
     * @param nodeId
     * @param aGraph
     * @return
     */
    private Integer findPrevious(Integer nodeId, List<ResultGraph> aGraph) {
        Integer result = null;
        for (ResultGraph node : aGraph) {
            Integer currentNodeId = node.getId();
            List<Integer> adjs = node.getAdjacencies();
            if (adjs.contains(nodeId)) {
                result = currentNodeId;
                break;
            }
        }
        return result;
    }

    /**
     * returns property names but updated so that when they are removed from the graph label id makes sense so, key is
     * 'node1-node2' and value is the name of the property
     * 
     * @param aGraph
     * @param nodeNames
     * @return
     */
    public HashMap getPropertyNames(List<ResultGraph> aGraph, HashMap nodeNames) {
        HashMap propertyNames = new HashMap();// key='node1-node2'
        for (ResultGraph node : aGraph) {
            Integer nodeId = node.getId();
            if (CATConstants.TYPE_PROPERTY.equals(node.getType())) {
                List<Integer> adjs = node.getAdjacencies();
                for (Integer adj : adjs) {
                    int IdOfPreviousElement = findPrevious(nodeId, aGraph);
                    String labelIds = IdOfPreviousElement + "-" + adj;
                    propertyNames.put(labelIds, nodeNames.get(nodeId));
                }
                // System.out.print("propertyNames:\n" +
                // propertyNames.toString());
            }
            // for node id - adj id pair: store the property name
        }
        return propertyNames;
    }

    /**
     * @param aGraph
     * @param mapOfLabels
     * @return
     */
    public HashMap getNodeNames(List<ResultGraph> aGraph) {
        if (aGraph == null) return null;
        HashMap nodeNames = new HashMap();
        for (ResultGraph node : aGraph) {
            Integer nodeId = node.getId();
            nodeNames.put(nodeId, getLabel(node.getURI()));
        }
        return nodeNames;
    }

    /**
     * This method gets label randomly from the mapOfLabels; this method exist in queryUtils??????????
     * 
     * @param uri
     * @param mapOfLabels
     * @return
     */
    public String getLabel(String uriString) {
        if (uriString == null) return "null";
        SerializableURI uri = null;
        try {
            uri = new SerializableURI(uriString, false);
            String label = null;
            // logger
            // .info("to do: proveri da li je csv of label uris set and if yes use it!!!!!!!!");
            List<String> labels =
                            luceneAnnotator.findLabels(uriString);
            if (labels == null || labels.size() == 0) {
                String propertyURI = "http://purl.org/dc/elements/1.1/title";
                labels =
                                luceneAnnotator.findLiteral(uriString,
                                                propertyURI);
            }
            for (String l : labels) {
                label = l;
                if (l == null)
                    continue;
                else if (l.contains(" ")) return l;
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
     * returns all existing labels
     * 
     * @param uriString
     * @return
     */
    public String getLabels(String uriString) {
        if (uriString == null) return "null";
        SerializableURI uri = null;
        try {
            uri = new SerializableURI(uriString, false);
            logger
                            .info("to do: proveri da li je csv of label uris set and if yes use it!!!!!!!!");
            List<String> labels =
                            luceneAnnotator.findLabels(uriString);
            if (labels == null || labels.size() == 0) {
                String propertyURI = "http://purl.org/dc/elements/1.1/title";
                labels =
                                luceneAnnotator.findLiteral(uriString,
                                                propertyURI);
            }
            if (labels == null || labels.size() == 0)
                return StringUtil.beautifyString(uri.getResourceName());
            else {
                StringBuffer labelsString = new StringBuffer("");
                for (String thisLabel : labels) {
                    labelsString.append(thisLabel).append(" OR ");
                }
                int endIndex = labelsString.toString().lastIndexOf(" OR ");
                logger.info("Before cutting:" + labelsString.toString());
                return labelsString.toString().substring(0, endIndex);
            }
        } catch (Exception ie) {
            return StringUtil.beautifyString(uriString);
        }
    }
}
