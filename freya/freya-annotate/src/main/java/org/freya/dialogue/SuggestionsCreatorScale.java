package org.freya.dialogue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.annotate.lucene.LuceneAnnotator;
import org.freya.model.ClassElement;
import org.freya.model.DatatypePropertyValueElement;
import org.freya.model.DatatypePropertyValueIdentifier;
import org.freya.model.InstanceElement;
import org.freya.model.InstanceListElement;
import org.freya.model.OntologyElement;
import org.freya.model.POC;
import org.freya.model.PropertyElement;
import org.freya.model.SemanticConcept;
import org.freya.model.SerializableURI;
import org.freya.model.SuggestionKey;
import org.freya.model.Vote;
import org.freya.parser.stanford.TreeUtils;
import org.freya.similarity.SimilarityCalculator;
import org.freya.util.FreyaConstants;
import org.freya.util.StringUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.trees.Tree;

/**
 * on the cotrary to basic, this one finds suggestions in its own repository
 * 
 * @author danica
 */
@Component
public class SuggestionsCreatorScale implements SuggestionsCreator {
    static Log logger = LogFactory.getLog(SuggestionsCreatorScale.class);
    @Autowired SuggestionsHelper suggestionsHelper;
    // Boolean forceSuggestions = false;

    boolean forceSuperClasses = true;

    AtomicInteger incrementer = new AtomicInteger();

    String[] namespacesToIgnore = {"http://sw.opencyc.org/concept/"};
    @Value("${org.freya.dialogue.force.suggestions}") String forceSuggestions;



    public SuggestionsCreatorScale() {
//        InputStream is = this.getClass().getResourceAsStream("/Service.properties");
//        Properties ps = new Properties();
//        try {
//            ps.load(is);
//        } catch (IOException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }

    }

    @Autowired SimilarityCalculator similarityCalculator;
    @Autowired LuceneAnnotator luceneAnnotator;
    @Autowired TreeUtils treeUtils;

    /**
     * this is to enable access to the small repository used only for generating suggestions
     */
    // SparqlUtils sparqlUtils;
    //
    // public SparqlUtils getSparqlUtils() {
    // return sparqlUtils;
    // }
    //
    // public void setSparqlUtils(SparqlUtils sparqlUtils) {
    // this.sparqlUtils = sparqlUtils;
    // }

    /**
     * this is to enable access to the major repository
     */
    // Ontology2Map ontology2Map;
    //
    // public Ontology2Map getOntology2Map() {
    // return ontology2Map;
    // }
    //
    // public void setOntology2Map(Ontology2Map ontology2Map) {
    // this.ontology2Map = ontology2Map;
    // }

    // public TreeUtils getTreeUtils() {
    // return treeUtils;
    // }
    //
    // public void setTreeUtils(TreeUtils treeUtils) {
    // this.treeUtils = treeUtils;
    // }

    private List<OntologyElement> findCandidatesForADTPV(DatatypePropertyValueElement el,
                    String text) {
        // Set<String> allSuggestions = new HashSet<String>();
        long start = System.currentTimeMillis();
        DatatypePropertyValueIdentifier dtpv = (DatatypePropertyValueIdentifier) el.getData();
        List<SerializableURI> instanceUris = dtpv.getInstanceURIs();
        Set<String> allClassUris = new HashSet<String>();
        for (SerializableURI uri : instanceUris) {
            List<String> dTypes = luceneAnnotator.findDirectTypes(uri.toString());
            allClassUris.addAll(dTypes);
        }
        long end = System.currentTimeMillis();
        logger.info("Lucene finished findDirectTypes for " + (end - start) + "ms and found:"
                        + allClassUris.size() + " dTypes");
        List<OntologyElement> elements = new ArrayList();
        Set<String> properties = new HashSet();
        start = System.currentTimeMillis();
        for (String classUri : allClassUris) {
            properties.addAll(findCandidatesForClass(classUri));
        }
        end = System.currentTimeMillis();
        logger.info("findCandidatesForClass for " + (end - start) + "ms and found:"
                        + properties.size() + " prop uris");
        start = System.currentTimeMillis();
        try {
            elements.addAll(returnPropertyElements(properties));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        end = System.currentTimeMillis();
        logger.info("returnPropertyElements for " + (end - start) + "ms and found:"
                        + properties.size() + " prop uris");
        return elements;
    }

    /**
     * @param property
     * @param text
     * @return
     */
    private List<OntologyElement> findCandidatesForAProperty(PropertyElement property, String text)
                    throws Exception {
        // Set<String> allSuggestions = new HashSet<String>();
        String uri = ((SerializableURI) property.getData()).toString();
        logger.debug("Finding candidates for property:" + property.getData().toString());
        // all elements
        List<OntologyElement> elements = new ArrayList<OntologyElement>();
        // feed classes: these are used to find more suggestions but they are also
        // added to the list!
        Set<String> feedClasses = new HashSet<String>();
        Set<String> properties = new HashSet<String>();
        Set<String> allClasses = new HashSet<String>();
        Set<String> rangeClasses = luceneAnnotator.findPropertyRange(uri);
        feedClasses.addAll(rangeClasses);
        logger.debug("getRangeClassesForProperty:" + uri + " found " + rangeClasses.size());
        Set<String> domainClasses = luceneAnnotator.findPropertyDomain(uri);
        feedClasses.addAll(domainClasses);
        logger.debug("getDomainClassesForProperty:" + uri + " found " + domainClasses.size());
        if (feedClasses == null || feedClasses.size() <= 1) {
            // find top classes
            logger.debug("Number of feed classes is 0....forceSuggestions=" + forceSuggestions);
            if (new Boolean(forceSuggestions).booleanValue() == true) {
                feedClasses.addAll(luceneAnnotator.findClassURIs());
            }
        }
        // a onda za te classes nadji properties where class is a domain/range
        for (String classUri : feedClasses) {
            properties.addAll(luceneAnnotator
                            .getDefinedPropertiesWhereClassIsADomain(classUri, forceSuperClasses));
            properties.addAll(luceneAnnotator
                            .getDefinedPropertiesWhereClassIsARange(classUri, forceSuperClasses));
        }
        //
        if (new Boolean(forceSuggestions).booleanValue() == true) {
            logger.info("cheating!!!!!!!!!!!!!!!!!!!!!!!!!!! here you should not get ALL properties but only relevant ones");
            Set<String> datatypePropertiesList =
                            luceneAnnotator.findDatatypePropertyURIs();
            Set<String> objectPropertiesList =
                            luceneAnnotator.findObjectPropertyURIs();
            Set<String> rdfPropertiesList =
                            luceneAnnotator.findRDFPropertyURIs(null);
            properties.addAll(datatypePropertiesList);
            properties.addAll(objectPropertiesList);
            properties.addAll(rdfPropertiesList);
        }
        Set<String> filteredProperties = new HashSet<String>();
        for (String t : properties) {
            if (!isInIgnoreNameSpaceList(t)) {
                filteredProperties.add(t);
            }
        }
        // add datatype properties which do not have domain defined: mb endDate
        // beginDate
        // String table = getSparqlUtils().getDatatypePropertiesNoDomain();
        // properties.addAll(gate.freya.util.StringUtil.fromStringToSet(table));
        elements.addAll(returnPropertyElements(filteredProperties));
        for (String classUri : feedClasses) {
            allClasses.addAll(luceneAnnotator
                            .getNeighbouringClassesWhereGivenClassIsADomain(classUri,
                                            forceSuperClasses));
            allClasses.addAll(luceneAnnotator
                            .getNeighbouringClassesWhereGivenClassIsARange(classUri,
                                            forceSuperClasses));
        }
        Set<String> filteredFeedClasses = new HashSet<String>();
        for (String t : feedClasses) {
            if (!isInIgnoreNameSpaceList(t)) {
                filteredFeedClasses.add(t);
            }
        }
        allClasses.addAll(filteredFeedClasses);
        elements.addAll(returnClassElements(allClasses));
        return elements;
    }

    /**
     * @param dTypes
     * @return
     */
    List<String> filterDirectTypes(List<String> dTypes) {
        List<String> filteredList = new ArrayList();
        List<String> toRemove = new ArrayList();
        if (dTypes != null && dTypes.size() > 1) {
            // Map<String, Set<String>> subClasses = ontology2Map.getSubClasses();
            List<String> copyOfDtypes = new ArrayList();
            copyOfDtypes.addAll(dTypes);
            for (String classUri : dTypes) {
                Set<String> subClassesOfThis =
                                luceneAnnotator.findSubClasses(classUri);// subClasses.get(classUri);
                for (String uri : copyOfDtypes) {
                    if (subClassesOfThis.contains(uri) && !classUri.equals(uri)) {
                        // remove this
                        toRemove.add(uri);
                    }
                }
                // classUri subclass DTypes[i]; ostaje
                // classUri superclass DTypes[i]; remove classUri
                // OVO NE TREBA DA BUDE OVDE; PROBAJ DA TO UBACIS TAMO KAD TRAZIS
                // SUGGESTIONS ILI RAZMISLI GDE JE VEC NAJBOLJE
            }
        }
        filteredList.addAll(dTypes);
        filteredList.removeAll(toRemove);
        return filteredList;
    }

    /**
     * checks whether this prop should be ignored or not
     * 
     * @param uri
     * @return
     */
    boolean isInIgnoreNameSpaceList(String uri) {
        boolean ignore = false;
        for (String toIgnore : namespacesToIgnore) {
            if (uri.startsWith(toIgnore)) {
                ignore = true;
                break;
            }
        }
        return ignore;
    }

    public Set<String> findCandidatesForClass(String classUri) {
        Set<String> listOfPotentialCandidates = new HashSet<String>();
        listOfPotentialCandidates.addAll(luceneAnnotator
                        .getDefinedPropertiesWhereClassIsADomain(classUri, forceSuperClasses));
        listOfPotentialCandidates.addAll(luceneAnnotator
                        .getDefinedPropertiesWhereClassIsARange(classUri, forceSuperClasses));
        Set<String> filteredList = new HashSet<String>();
        for (String t : listOfPotentialCandidates) {
            if (!isInIgnoreNameSpaceList(t)) {
                filteredList.add(t);
            }
        }
        return filteredList;
    }

    /**
     * find candidate ontology elements
     * 
     * @return
     */
    private List<OntologyElement> findCandidates(SemanticConcept sc, String text) throws Exception {
        // Set<String> allSuggestions = new HashSet<String>();
        OntologyElement el = sc.getOntologyElement();
        // what is the population of california?
        // what is the population of cities in california?
        // what is the highest point of california?
        // String uri = null;
        Set<String> classUris = new HashSet<String>();
        if (el instanceof InstanceElement) {
            List<String> allClassUris = ((InstanceElement) el).getClassURIList();
            logger.info("There were:" + allClassUris.size() + " direct types");
            // classUris = filterDirectTypes(allClassUris);
            classUris = new HashSet(allClassUris);
            logger.info("There is NO FILTERING of direct types...");
        } else if (el instanceof InstanceListElement) {
            List allClassUris = ((InstanceListElement) el).getClassURIList();
            logger.info("There were:" + allClassUris.size() + " direct types");
            // classUris = filterDirectTypes(allClassUris);
            classUris = new HashSet(allClassUris);
            logger.info("There is NO FILTERING of direct types...");
        } else if (el instanceof ClassElement) {
            String uri = ((SerializableURI) el.getData()).toString();
            classUris.add(uri);
            logger.info("NN is class" + uri);
        } else if (el instanceof DatatypePropertyValueElement) {
            logger.info("NN is DPVE " + el.getData());
            return findCandidatesForADTPV(((DatatypePropertyValueElement) el), text);
        } else if (el instanceof PropertyElement) {
            logger.info("NN is property " + el.getData());
            return findCandidatesForAProperty(((PropertyElement) el), text);
        }
        Set<OntologyElement> elements = new HashSet<OntologyElement>();
        Set<String> listOfPotentialCandidates = new HashSet<String>();
        Set<String> classes = new HashSet<String>();
        long start = System.currentTimeMillis();
        for (String uri : classUris) {
            // //////////properties first//////////////////////////
            listOfPotentialCandidates = findCandidatesForClass(uri);
            elements.addAll(returnPropertyElements(listOfPotentialCandidates));
            // allSuggestions.addAll(listOfPotentialCandidates);
        }
        logger.info("Found " + elements.size() + " elements so far.");
        // ////////////////////////////////////
        for (String uri : classUris) {
            classes.addAll(luceneAnnotator
                            .getNeighbouringClassesWhereGivenClassIsADomain(uri, forceSuperClasses));
            logger.info("Found " + classes.size() + " class candidates from IsADomain method.");
            classes.addAll(luceneAnnotator
                            .getNeighbouringClassesWhereGivenClassIsARange(uri, forceSuperClasses));
            logger.info("Found more, now total: " + classes.size()
                            + " class candidates from IsADomain and isARange method.");
        }
        Set<String> filteredClasses = new HashSet<String>();
        for (String t : classes) {
            if (!isInIgnoreNameSpaceList(t)) {
                filteredClasses.add(t);
            }
        }
        elements.addAll(returnClassElements(filteredClasses));
        long end = System.currentTimeMillis();
        logger.info("Found " + elements.size() + " cadidates for " + classUris.size()
                        + " classes for " + (end - start) + "ms.");
        return new ArrayList(elements);
    }

    /**
     * generate class elements from the set of class uris
     * 
     * @param classes
     * @return
     */
    List<OntologyElement> returnClassElements(Set<String> classes) throws Exception {
        List<OntologyElement> elements = new ArrayList<OntologyElement>();
        for (String euri : classes) {
            SerializableURI elementUri = null;
            try {
                elementUri = new SerializableURI(euri, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            OntologyElement e = new ClassElement();
            e.setData(elementUri);
            elements.add(e);
        }
        return elements;
    }

    /**
     * generate property elements from the set of property uris, and check whether these properties are datatype so that
     * additional flag can be added to the property elements
     * 
     * @param classes
     * @return
     */
    List<OntologyElement> returnPropertyElements(Set<String> properties) throws Exception {
        if (properties == null) return new ArrayList();
        List<OntologyElement> elements = new ArrayList<OntologyElement>();
        for (String euri : properties) {
            // logger.info("Checking if datatypeproperty=true for: " + euri);
            SerializableURI elementUri = null;
            try {
                elementUri = new SerializableURI(euri, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            OntologyElement e = new PropertyElement();
            // so that later we can distinguish property elements if they refer
            // to datatypes
            if (luceneAnnotator.isItDatatypeProperty(euri)) {
                ((PropertyElement) e).setDatatypeProperty(true);
            }
            e.setData(elementUri);
            elements.add(((PropertyElement) e));
        }
        return elements;
    }

    public List<Vote> generateVotes(SuggestionKey key, POC poc) {
        return generateVotes(key, poc, true, new ArrayList());
    }

    /**
     * 
     * @param candidateElements
     * @param avoidThese
     * @param poc
     * @param neighbour
     * @param text
     * @return
     */
    List<Vote> generateVotesFromOntologyElements(List<OntologyElement> candidateElements,
                    Set<String> avoidThese, POC poc, SemanticConcept neighbour, String text)
                    throws Exception {
        List<Vote> votes = new ArrayList<Vote>();
        List<String> listOfSuggestionUris = new ArrayList<String>();
        List<DatatypePropertyValueIdentifier> listOfDTPVIdentifiers =
                        new ArrayList<DatatypePropertyValueIdentifier>();
        for (OntologyElement element : candidateElements) {
            // skip adding suggestions which are already in ocs
            if (avoidThese.contains(element.getData().toString())) continue;
            // run some similarity metric here and use some threshold...
            // if similarityScore>threshold then add this to the
            // clarificationOptions
            String pPropertyShortNameOrUri = null;
            String suggestion = ((SerializableURI) element.getData()).toString();
            try {
                SerializableURI elementUri = new SerializableURI(suggestion, false);
                pPropertyShortNameOrUri = elementUri.getResourceName();
            } catch (Exception e) {
                pPropertyShortNameOrUri = suggestion;
            }
            if (!listOfSuggestionUris.contains(suggestion)) {
                listOfSuggestionUris.add(suggestion);
                if (poc.getAnnotation() != null) element.setAnnotation(poc.getAnnotation());
                // check whether the element is datatype property and add
                // the governor
                if (neighbour != null)
                    element = addGovernor(element, neighbour.getOntologyElement());
                /* cloning elements */
                Vote vote = generateVote(text, pPropertyShortNameOrUri, element, null);
                votes.add(vote);
                votes.addAll(addAdditionalVotes(element, poc, text, pPropertyShortNameOrUri, false));
            }
        }
        return votes;
    }

    /**
     * Finds clarification options for given interpretation by applying rules such as: match head with some existing
     * (datatype)property of the OntoRes which is first in the query (if there is such OntoRes) what is city population
     * of california? head: population modf: city unified two methods on 27.02.2010. what is the largest city in
     * california? (city is governor, largest is dependent) s: is 'largest' related to: <list od datatype properties
     * which are related to cities (in california)>
     * 
     * @param key
     * @return
     */
    public List<Vote> generateVotes(SuggestionKey key, POC poc, boolean addNone,
                    List<SemanticConcept> toSkip) {
        logger.debug("Before generating suggestions, this many needs to be skipped: "
                        + toSkip.size());
        long start = System.currentTimeMillis();
        Set<String> avoidThese = new HashSet<String>();
        if (toSkip != null) for (SemanticConcept concept : toSkip) {
            String uri = concept.getOntologyElement().getData().toString();
            avoidThese.add(uri);
        }
        long end = System.currentTimeMillis();
        List<Vote> votes = new ArrayList<Vote>();
        logger.info("Moved OCs to the list to avoid for " + (end - start) + "ms.");
        List<SemanticConcept> neighbours = key.getNearestNeighbours();
        String text = key.getText();
        Integer num = null;
        if (neighbours != null) num = neighbours.size();
        logger.info("Found " + num + " neighbours for " + key.getText());
        /*
         * this list ensures that the suggestions with an URI is added only once as if there are more than one
         * NNeighbours it can happen that the same suggestion is generated for each neighbour
         */
        Set<OntologyElement> allCandidateElements = new HashSet<OntologyElement>();
        for (SemanticConcept neighbour : neighbours) {
            start = System.currentTimeMillis();
            Set<OntologyElement> candidateElements = new HashSet<OntologyElement>();
            List<OntologyElement> tmpCandidates;
            try {
                tmpCandidates = findCandidates(neighbour, text);

                end = System.currentTimeMillis();
                logger.info("Finding candidates for:" + (end - start) + "ms");
                start = System.currentTimeMillis();
                for (OntologyElement el : tmpCandidates) {
                    boolean elementAlreadyInSuggestions =
                                    elementAlreadyInSuggestions(
                                                    new ArrayList(allCandidateElements), el);
                    if (!elementAlreadyInSuggestions) {
                        candidateElements.add(el);
                        allCandidateElements.add(el);
                    }
                    {
                        logger.debug("Skipping, element already added:" + el.getData().toString());
                    }
                }

            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            end = System.currentTimeMillis();
            logger.info("Checking whether candidates already added for:" + (end - start) + "ms");
            logger.info("Found " + candidateElements.size()
                            + " suggestions (function elements not counted) for NN:"
                            + neighbour.getOntologyElement().getData().toString() + " and text:"
                            + text + ", total up to now:" + allCandidateElements.size()
                            + ", total to remove: " + avoidThese.size());
            start = System.currentTimeMillis();
            try {
                votes.addAll(generateVotesFromOntologyElements(new ArrayList(candidateElements),
                                avoidThese, poc, neighbour, text));
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            end = System.currentTimeMillis();
            logger.info("Generating Ontology Elements for:" + (end - start) + "ms, " + votes.size()
                            + " suggestions to be shown.");
            // ////////////////////////////////////////////////////
            // ////////add the closest element if the poc to be clarified is
            // adjective
            // //////////////////////////////////////////////////
            OntologyElement closestOntologyConcept = neighbour.getOntologyElement();
            // if (!candidateElements.contains(closestOntologyConcept)) {
            boolean elementAlreadyInSuggestions =
                            elementAlreadyInSuggestions(new ArrayList(candidateElements),
                                            closestOntologyConcept);
            if (!elementAlreadyInSuggestions) {
                SerializableURI uriUri = null;
                String cocSuggestion = closestOntologyConcept.getData().toString();
                String cocUriOrLiteral = null;
                try {
                    uriUri = new SerializableURI(cocSuggestion, false);
                    cocUriOrLiteral = uriUri.getResourceName();
                } catch (Exception e) {
                    cocUriOrLiteral = cocSuggestion;
                }
                votes.addAll(addAdditionalVotes(closestOntologyConcept, poc, text, cocUriOrLiteral,
                                true));
            }
        }
        start = System.currentTimeMillis();
        // here we add datatypeProperties with no domain etc.
        List<OntologyElement> hangingElements = new ArrayList<OntologyElement>();
        try {
            hangingElements.addAll(findHangingElements());
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        List filteredHangingElements = new ArrayList();

        for (OntologyElement el : hangingElements) {
            boolean elementAlreadyInSuggestions =
                            elementAlreadyInSuggestions(new ArrayList(allCandidateElements), el);
            if (!elementAlreadyInSuggestions) filteredHangingElements.add(el);
        }
        try {
            votes.addAll(generateVotesFromOntologyElements(filteredHangingElements, avoidThese,
                            poc, null, text));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        end = System.currentTimeMillis();
        logger.info("Added hanging Elements for:" + (end - start) + "ms");
        // add None element
        if (addNone) {
            Vote vote = VoteGenerator.generateNoneVote(incrementer.incrementAndGet());
            vote.getCandidate().getOntologyElement().setAnnotation(poc.getAnnotation());
            votes.add(vote);
        }
        // now call wordnet to recalculate scores
        return votes;
    }

    /**
     * find properties with no domain or range defined and also add some generic elements like label or type
     * 
     * @return
     */
    List<OntologyElement> findHangingElements() throws Exception {
        List<OntologyElement> elements = new ArrayList<OntologyElement>();
        Set<String> properties = new HashSet<String>();
        properties.add(RDFS.LABEL.toString());
        properties.add(RDF.TYPE.toString());
        String table = suggestionsHelper.getDatatypePropertiesNoDomain();
        properties.addAll(StringUtil.fromStringToSet(table));

        String thingUri = "http://www.w3.org/2002/07/owl#Thing";
        properties.addAll(luceneAnnotator
                        .getDefinedPropertiesWhereClassIsADomain(thingUri, forceSuperClasses));
        properties.addAll(luceneAnnotator.getDefinedPropertiesWhereClassIsARange(
                        thingUri, forceSuperClasses));
        elements.addAll(returnPropertyElements(properties));

        logger.info("Adding:" + properties.size() + " hanging properties");

        Set<String> classes = new HashSet<String>();
        classes.add(thingUri);
        elements.addAll(returnClassElements(classes));
        return elements;
    }

    /**
     * checking whether the element is already added
     * 
     * @param candidateElements
     * @param closestOntologyConcept
     * @return
     */
    boolean elementAlreadyInSuggestions(List<OntologyElement> candidateElements,
                    OntologyElement closestOntologyConcept) {
        boolean elementAlreadyInSuggestions = false;
        for (OntologyElement el : candidateElements) {
            if (el.getData() != null
                            && el.getData().toString()
                                            .equals(closestOntologyConcept.getData().toString()))
                elementAlreadyInSuggestions = true;
            else if (el instanceof DatatypePropertyValueElement
                            && closestOntologyConcept instanceof DatatypePropertyValueElement) {
                DatatypePropertyValueIdentifier identif1 =
                                (DatatypePropertyValueIdentifier) ((DatatypePropertyValueElement) el)
                                                .getData();
                DatatypePropertyValueIdentifier identif2 =
                                (DatatypePropertyValueIdentifier) ((DatatypePropertyValueElement) closestOntologyConcept)
                                                .getData();
                if (identif1.equals(identif2)) elementAlreadyInSuggestions = true;
            }
        }
        return elementAlreadyInSuggestions;
    }

    /**
     * checks whether the element is property and adds governor if yes
     * 
     * @param pElement
     * @param governor
     * @return
     */
    OntologyElement addGovernor(OntologyElement pElement, OntologyElement governor) {
        PropertyElement property = null;
        if (pElement instanceof PropertyElement) {
            property = (PropertyElement) pElement;
            // logger.debug("Property found:" + property.getData().toString()
            // + "...is it datatype?" + property.isDatatypeProperty());
            if (property.isDatatypeProperty()) {
                // set governor for that element to be the
                // neighbour.getOntologyElement()
                property.setGovernor(governor);
                // logger.debug("Setting governor:" + governor.toString()
                // + " for element: " + property.getData().toString());
                String uri = ((SerializableURI) property.getData()).toString();
                Set<String> range = luceneAnnotator.findPropertyRange(uri);
                String firstOne = null;
                if (range != null && range.size() > 0) {
                    firstOne = (String) (new ArrayList(range)).get(0);
                    property.setRange(firstOne);
                }
            }
        }
        if (property == null)
            return pElement;
        else
            return property;
    }

    /**
     * if element is datatype property then add min, max and sum as options
     * 
     * @param element
     * @param poc
     * @return
     */
    List<Vote> addAdditionalVotes(OntologyElement element, POC poc, String text,
                    String propertyUri, boolean alreadyAdded) {
        // if element is datatype property add suggestions with min,
        // max, sum
        List<Vote> votes = new ArrayList<Vote>();
        // logger.info("Adding additional votes(min,max,etc), poc is:"+poc.toString());
        if (element instanceof PropertyElement) {
            PropertyElement clonedElement = new PropertyElement();
            String thisPropertyUri = ((PropertyElement) element).getData().toString();
            // logger.info("thisPropertyUri "+thisPropertyUri);
            if (luceneAnnotator.isItDatatypeProperty(thisPropertyUri)) {
                ((PropertyElement) clonedElement).setDatatypeProperty(true);
                boolean containsJJ = false;
                containsJJ = treeUtils.pocContainsJJs(poc);
                // logger.info("Checking whether it contains JJ*..." + containsJJ);
                if (containsJJ) {
                    if (alreadyAdded)
                        clonedElement.setAlreadyAdded(true);
                    else
                        clonedElement.setAlreadyAdded(false);
                    clonedElement.setAnnotation(element.getAnnotation());
                    clonedElement.setData(element.getData());
                    if (((PropertyElement) element).getRange() != null)
                        clonedElement.setRange(((PropertyElement) element).getRange());
                    if (((PropertyElement) element).getDomain() != null)
                        clonedElement.setDomain(((PropertyElement) element).getDomain());
                    if (((PropertyElement) element).getGovernor() != null)
                        clonedElement.setGovernor(((PropertyElement) element).getGovernor());
                    Vote vote =
                                    generateVote(text, propertyUri, clonedElement,
                                                    FreyaConstants.MAX_FUNCTION);
                    votes.add(vote);
                    vote =
                                    generateVote(text, propertyUri, clonedElement,
                                                    FreyaConstants.MIN_FUNCTION);
                    votes.add(vote);
                    vote =
                                    generateVote(text, propertyUri, clonedElement,
                                                    FreyaConstants.SUM_FUNCTION);
                    votes.add(vote);
                    vote =
                            generateVote(text, propertyUri, clonedElement,
                                            FreyaConstants.AVG_FUNCTION);
                    votes.add(vote);
                    
                }
            }
        }
        return votes;
    }

    /**
     * @param text
     * @param pPropertyShortNameOrUri
     * @param element
     * @param function
     * @return
     */
    public Vote generateVote(String text, String pPropertyShortNameOrUri, OntologyElement element,
                    String function) {
        String niceLabel = StringUtil.beautifyString(pPropertyShortNameOrUri);
        Vote vote = new Vote();
        long id = incrementer.incrementAndGet();
        vote.setId(id);
        SemanticConcept candidateSemanticConcept = new SemanticConcept();
        candidateSemanticConcept.setOntologyElement(element);
        // element.setFunction(function);
        candidateSemanticConcept.setFunction(function);
        // SemanticConcept clonedElement =
        // (SemanticConcept)candidateSemanticConcept.clone();
        vote.setCandidate(candidateSemanticConcept);
        // vote.setCandidate(clonedElement);
        // give more weight to monge
        try {
            double totalSimilarity = similarityCalculator.findSimilarity(text, niceLabel);
            vote.setVote(totalSimilarity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vote;
    }

    /**
     * add none by default
     * 
     * @param key
     * @param poc
     * @return
     */
    public List<Vote> generateGenericVotes(SuggestionKey key, POC poc) {
        return generateGenericVotes(key, poc, true, 1000000, null);
    }

    /**
     * this method finds potential options based on the detected main subject; it is usually used when none of the
     * ontology-related annotations exist
     * 
     * @param key
     * @return
     */
    public List<Vote> generateGenericVotes(SuggestionKey key, POC poc, boolean addNone,
                    Integer max, List<SemanticConcept> toSkip) {
        Set<String> avoidThese = new HashSet<String>();
        if (toSkip != null) for (SemanticConcept concept : toSkip) {
            String uri = concept.getOntologyElement().getData().toString();
            avoidThese.add(uri);
        }
        String text = key.getText();
        List<Vote> votes = new ArrayList<Vote>();
        List<OntologyElement> elements;
        try {
            elements = findGenericOntologyElements(max);

            for (OntologyElement element : elements) {
                if (avoidThese.contains(element.getData().toString())) continue;
                element.setAnnotation(poc.getAnnotation());
                // run some similarity metric here and use some threshold...
                // if similarityScore>threshold then add this to the
                // clarificationOptions
                String pPropertyShortName = "";
                String suggestion = ((SerializableURI) element.getData()).toString();
                try {
                    SerializableURI elementUri = new SerializableURI(suggestion, false);
                    pPropertyShortName = elementUri.getResourceName();
                } catch (Exception e) {
                    pPropertyShortName = suggestion;
                }
                String niceLabel = StringUtil.beautifyString(pPropertyShortName);
                Vote vote = new Vote();
                long id = incrementer.incrementAndGet();
                vote.setId(id);
                SemanticConcept candidateSemanticConcept = new SemanticConcept();
                candidateSemanticConcept.setOntologyElement(element);
                SemanticConcept clonedConcept = (SemanticConcept) candidateSemanticConcept.clone();
                vote.setCandidate(clonedConcept);
                double totalSimilarity = similarityCalculator.findSimilarity(text, niceLabel);
                vote.setVote(totalSimilarity);
                votes.add(vote);
                votes.addAll(addAdditionalVotes(element, poc, text, pPropertyShortName, false));
            }

            // here we add datatypeProperties with no domain etc.
            votes.addAll(generateVotesFromOntologyElements(findHangingElements(), avoidThese, poc,
                            null, text));
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        // add None element
        if (addNone) {
            Vote vote = VoteGenerator.generateNoneVote(incrementer.incrementAndGet());
            vote.getCandidate().getOntologyElement().setAnnotation(poc.getAnnotation());
            votes.add(vote);
        }
        return votes;
    }

    /**
     * finds elements which are the top most in the ontology...
     * 
     * @return
     */
    private List<OntologyElement> findGenericOntologyElements(Integer max) throws Exception {
        List<OntologyElement> elements = new ArrayList<OntologyElement>();
        Set<String> suggestions = luceneAnnotator.findPropertyURIs(max);
        // now remove all that are after max
        List<String> list = new ArrayList<String>(suggestions);
        if (suggestions != null && suggestions.size() > max) {
            for (int i = max; i < suggestions.size(); i++) {
                String prop = list.get(i);
                suggestions.remove(prop);
            }
        }
        elements.addAll(returnPropertyElements(suggestions));
        long start = System.currentTimeMillis();
        Set<String> classUris = luceneAnnotator.findTopClasses();
        long end = System.currentTimeMillis();
        logger.info("Finished searching lucene for top classes for:" + (end - start) + "ms.");
        if (classUris == null) classUris = new HashSet();
        logger.info("Found " + classUris.size() + " top classes.");
        // now remove all that are after max
        List<String> newList = new ArrayList<String>(classUris);
        if (newList.size() > max) {
            for (int i = max; i < newList.size(); i++) {
                String classUri = newList.get(i);
                classUris.remove(classUri);
            }
        }
        for (String euri : classUris) {
            SerializableURI elementUri = null;
            try {
                elementUri = new SerializableURI(euri, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            OntologyElement e = new ClassElement();
            e.setData(elementUri);
            elements.add(e);
        }
        return elements;
    }

    /**
     * Finds clarification options for given head, by calling: Wordnet/Watson/Cyc something else and trying to come up
     * with a list of options which are related to 'something' in the ontology world)
     */
    public List<Vote> findClarificationOptions(Tree head) {
        List<Vote> votes = new ArrayList<Vote>();
        return votes;
    }
}
