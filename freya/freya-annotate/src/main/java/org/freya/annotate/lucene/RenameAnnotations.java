package org.freya.annotate.lucene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.Annotation;
import org.freya.model.DatatypePropertyValueIdentifier;
import org.freya.model.SerializableURI;
import org.freya.rdf.query.CATConstants;
import org.freya.util.FreyaConstants;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This class changes the feature of annotations
 */
@Component
public class RenameAnnotations {
    private static final Log logger = LogFactory.getLog(RenameAnnotations.class);
    @Autowired LuceneAnnotator luceneAnnotator;
    /*
     * this list contains those properties which are used to identify the entity; for example label property or
     * 'resourceHasName' type properties; the idea is to make this configurable as various ontologies use different
     * naming conventions and we need distinction btw. labels and 'those-serving-as-labels' properties as opposed to
     * others such as date-of-birth for example, which is not identifying the entity but the specific feature of it
     */
    @Value("${org.freya.annotate.rename.annotations.properties.list}") String propertiesListString;

    List<String> propertiesList;

    /*
     * list of properties for which the values will be ignored...such as abbreviation in the case of mooney...or
     * comment..etc
     */
    @Value("${org.freya.annotate.rename.annotations.ignore.properties.list}") String ignorePropertiesListString;
    /**
     * "The list of property URIs which are used to identify an entity. For example label property or 'resourceHasName' properties. Properties which are not in this list will generate 'datatypePropertyValue' annotation feature type"
     * )
     * 
     * @param propertiesList
     */
    List<String> ignorePropertiesList;

    @PostConstruct
    void init() {
        propertiesList = new ArrayList<String>();
        ignorePropertiesList = new ArrayList<String>();
        if (propertiesListString != null && ignorePropertiesListString != null) {
            String[] list = propertiesListString.split(",");
            for (String s : list) {
                if (s != null && !"".equals(s))
                    propertiesList.add(s);
            }

            list = ignorePropertiesListString.split(",");
            for (String s : list) {
                if (s != null && !"".equals(s))
                    ignorePropertiesList.add(s);
            }
        }
    }

    /**
     * @param annotations
     * @param lookup
     * @return
     */
    boolean anyOfAlreadyAddedAnnotationsOverlapWithLookup(
                    List<Annotation> annotations, Annotation lookup) {
        boolean result = false;
        for (Annotation ann : annotations) {
            if (lookup.overlaps(ann)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
	 * 
	 */
    private static final long serialVersionUID = -2085562223086784633L;


    /**
     * 
     * @param lookupAnnotations
     * @return
     */
    public List<Annotation> rename(List<Annotation> lookupAnnotations) {

        // map of uris with the value list of annotations
        Map<String, List<Annotation>> uris =
                        new HashMap<String, List<Annotation>>();
        Map<DatatypePropertyValueIdentifier, List<Annotation>> dtpvi =
                        new HashMap<DatatypePropertyValueIdentifier, List<Annotation>>();
        // now read Lookup annotations
        List<Annotation> toRemove = new ArrayList<Annotation>();
        List<Annotation> newLookupAnnotations = new ArrayList<Annotation>();
        for (Annotation lookup : lookupAnnotations) {
            // now read feature class
            String classFeature =
                            (String) lookup.getFeatures()
                                            .get(FreyaConstants.CLASS_FEATURE_LKB);
            String instFeature =
                            (String) lookup.getFeatures().get(FreyaConstants.INST_FEATURE_LKB);
            String predFeature =
                            (String) lookup.getFeatures().get(
                                            FreyaConstants.PROPERTY_FEATURE_LKB);
            String annText = (String) lookup.getFeatures().get(
                            FreyaConstants.RESULT_TYPE_STRING);
            Float score = (Float) lookup.getFeatures().get(
                            FreyaConstants.SCORE);
            // //////////////////////////////
            // //////////////hack: this is due to the bug in the jiant sparql
            // / where duplicates are retrieved for each class/property/inst ///////
            // //////////////////////////////
            if ("http://www.w3.org/2000/01/rdf-schema#Resource".equals(classFeature))
                // remove lookup
                toRemove.add(lookup);
            if (instFeature == null && classFeature == null) {
                toRemove.add(lookup);
            }
            // //////////////////////////////
            // //////////////instances///////
            // //////////////////////////////
            else if (OWL.CLASS.toString().equals(classFeature)
                            || RDFS.CLASS.toString().equals(classFeature)) {
                if (uris.keySet().contains(instFeature)
                                && anyOfAlreadyAddedAnnotationsOverlapWithLookup(uris
                                                .get(instFeature), lookup)) {
                    // and the annotations overlap
                    // && ((uris.get(instFeature)).contains(lookup))) {
                    // delete the annotation
                    toRemove.add(lookup);
                } else {
                    List<Annotation> annotations = uris.get(instFeature);
                    if (annotations == null) annotations = new ArrayList<Annotation>();
                    annotations.add(lookup);
                    uris.put(instFeature, annotations);
                    // generate class type
                    HashMap features = new HashMap();
                    features.put(CATConstants.TYPE_FEATURE, CATConstants.FEATURE_TYPE_CLASS);
                    features.put(CATConstants.FEATURE_URI, instFeature);
                    features.put(FreyaConstants.SCORE, score);
                    features.put(FreyaConstants.RESULT_TYPE_STRING, annText);
                    lookup.setFeatures(features);
                }
                // features.put(
                // CATConstants.FEATURE_PROPERTY_URI, propUri);
            } else if (OWL.OBJECTPROPERTY.toString().equals(classFeature)
                            || RDF.PROPERTY.toString().equals(classFeature)
                            || OWL.DATATYPEPROPERTY.toString().equals(classFeature)) {
                if (uris.keySet().contains(instFeature)
                                && anyOfAlreadyAddedAnnotationsOverlapWithLookup(uris
                                                .get(instFeature), lookup)) {
                    // delete the annotation
                    toRemove.add(lookup);
                } else {
                    List<Annotation> annotations = uris.get(instFeature);
                    if (annotations == null) annotations = new ArrayList<Annotation>();
                    annotations.add(lookup);
                    uris.put(instFeature, annotations);
                    // generate property
                    HashMap features = new HashMap();
                    features.put(CATConstants.TYPE_FEATURE, CATConstants.TYPE_PROPERTY);
                    features.put(CATConstants.FEATURE_URI, instFeature);
                    features.put(FreyaConstants.SCORE, score);
                    features.put(FreyaConstants.RESULT_TYPE_STRING, annText);
                    lookup.setFeatures(features);
                }
            } else if (ignorePropertiesList.contains(predFeature)) {
                // do nothing, ignore the property value...
                logger
                                .debug("Removing annotation, as ignoring the value for property: "
                                                + predFeature);
                toRemove.add(lookup);
            } else {
                // first check whether there is feature 'pred' if yes:
                // check whether it is in the propertiesList, if yes:
                // generate instance
                // if not: generate datatypeproperty value
                // if pred==null generate instance

                if (predFeature != null && propertiesList != null
                                && !propertiesList.contains(predFeature)) {
                    DatatypePropertyValueIdentifier datatypePropertyValueIdentifier =
                                    new DatatypePropertyValueIdentifier();
                    List<SerializableURI> instanceUriList = new ArrayList();
                    try {
                        instanceUriList.add(new SerializableURI(
                                        instFeature, false));
                    } catch (Exception e) {
                        logger.info(e.getMessage());
                    }
                    datatypePropertyValueIdentifier.setInstanceURIs(instanceUriList);
                    datatypePropertyValueIdentifier.setPropertyUri(predFeature);
                    datatypePropertyValueIdentifier.setPropertyValue(lookup.getText());
                    // if (dtpvi.contains(datatypePropertyValueIdentifier)) {
                    if (dtpvi.keySet().contains(datatypePropertyValueIdentifier)
                                    && anyOfAlreadyAddedAnnotationsOverlapWithLookup(dtpvi
                                                    .get(datatypePropertyValueIdentifier), lookup)) {
                        // delete the annotation
                        toRemove.add(lookup);
                    } else {
                        List<Annotation> annotations =
                                        dtpvi.get(datatypePropertyValueIdentifier);
                        if (annotations == null) annotations = new ArrayList<Annotation>();
                        annotations.add(lookup);
                        dtpvi.put(datatypePropertyValueIdentifier, annotations);
                        // generate datatype property value
                        HashMap features = new HashMap();
                        features.put(CATConstants.TYPE_FEATURE,
                                        CATConstants.TYPE_DATATYPE_PROPERTY_VALUE);
                        features.put(CATConstants.FEATURE_PROPERTY_URI, predFeature);
                        features.put(CATConstants.FEATURE_INSTANCE_URI, instFeature);
                        features.put(CATConstants.FEATURE_PROPERTY_VALUE, lookup.getText());
                        features.put(FreyaConstants.SCORE, score);
                        features.put(FreyaConstants.RESULT_TYPE_STRING, annText);
                        lookup.setFeatures(features);
                    }
                } else {
                    if (uris.keySet().contains(instFeature)
                                    && anyOfAlreadyAddedAnnotationsOverlapWithLookup(uris
                                                    .get(instFeature), lookup)) {
                        // delete the annotation
                        logger.info("Removing:" + lookup.toString());
                        toRemove.add(lookup);
                    } else {
                        List<Annotation> annotations = uris.get(instFeature);
                        if (annotations == null) annotations = new ArrayList<Annotation>();
                        annotations.add(lookup);
                        uris.put(instFeature, annotations);
                        // instances
                        HashMap features = new HashMap();
                        features.put(CATConstants.TYPE_FEATURE, CATConstants.TYPE_INSTANCE);
                        features.put(CATConstants.FEATURE_URI, instFeature);
                        features.put(CATConstants.CLASS_URI, classFeature);

                        features.put(FreyaConstants.SCORE, score);
                        features.put(FreyaConstants.RESULT_TYPE_STRING, annText);

                        // List<String> classUris=(List<String> )features.get(CATConstants.CLASS_URI_LIST);

                        List<String> dTypes = luceneAnnotator.findDirectTypes(instFeature);
                        // logger.info("Found "+dTypes.size()+" direct types for "+instFeature);
                        // List<String> dTypesFiltered =filterDirectTypes(dTypes);

                        features.put(CATConstants.CLASS_URI_LIST, dTypes);
                        lookup.setFeatures(features);
                    }
                }// generating type instance
            }
            // if (features != null) lookup.setFeatures(features);
        }
        // ////////////////////////////////////////////////////////////
        // ////////////////////test to see whether it will be faster than jape
        // /////////////////////////////////////////////////////////////////////
        // this is now done through jape
        // filterBasedOnPriority();
        for (Annotation annToRemove : toRemove) {
            int index = lookupAnnotations.indexOf(annToRemove);
            lookupAnnotations.remove(index);
        }

        // lookupAnnotations.removeAll(toRemove);

        // lookupAnnotations=filterInstanceClass(lookupAnnotations);
        return lookupAnnotations;
    }


    /**
     * this method is checking whether there are any annotations with feature priority=high this means that other
     * annotations which are overlapped should be removed such as in mississippi river where mississippi refers to state
     * and river but because of the following river the annotation referring to mississippi river will have feature
     * priority=high meaning that the annotation referring to state should be removed this method does exactly the same
     * as the FilterInstanceClass.jape
     */
    Set<Annotation> filterBasedOnPriority(Set<Annotation> allAnnotations) {
        List toRemove = new ArrayList();
        logger
                        .debug("===========================================BEGIN filterBasedOnPriority ================================");
        logger.debug("This many annotations:" + allAnnotations.size());
        if (allAnnotations.size() > 5000) {
            // skip this filtering
        } else {
            int i = 0;
            java.util.List<Annotation> allAnnotationsCopy =
                            new java.util.ArrayList<Annotation>();
            allAnnotationsCopy.addAll(allAnnotations);
            // System.out.println("=======allannotations=====\n"+allAnnotationsCopy.toString());
            for (Annotation lookup : allAnnotations) {
                allAnnotationsCopy.remove(lookup);
                logger.debug("=======checking lookup=====  numer\n" + i + ":"
                                + lookup.toString());
                for (Annotation ann : allAnnotationsCopy) {
                    logger.debug("=======checking annotation=====\n" + ann.toString());
                    // remove annotations which are 'within' the annotations
                    // with
                    // feature priority=high
                    // so that mississippi river always refers to either river
                    // or
                    // low point with name
                    // mississippi river and never to the state
                    if ((ann.getStartOffset().longValue() > lookup.getStartOffset()
                                    .longValue() && ann.getEndOffset().longValue() <= lookup
                                    .getEndOffset().longValue())
                                    || (ann.getStartOffset().longValue() >= lookup
                                                    .getStartOffset().longValue() && ann.getEndOffset()
                                                    .longValue() < lookup.getEndOffset().longValue())) {
                        String priority = (String) lookup.getFeatures().get("priority");
                        if (priority != null && priority.equals("high")) {
                            logger.debug("REMOVING:" + ann.toString()
                                            + "because lookup is prioritized:" + lookup.toString());
                            if (allAnnotations.contains(ann)) toRemove.add(ann);
                        }
                    }
                    // now what if we need to actually delete the lookup
                    // annotation?
                    if ((ann.getStartOffset().longValue() < lookup.getStartOffset()
                                    .longValue() && ann.getEndOffset().longValue() >= lookup
                                    .getEndOffset().longValue())
                                    || (ann.getStartOffset().longValue() <= lookup
                                                    .getStartOffset().longValue() && ann.getEndOffset()
                                                    .longValue() > lookup.getEndOffset().longValue())) {
                        String priority = (String) ann.getFeatures().get("priority");
                        if (priority != null && priority.equals("high")) {
                            logger.debug("REMOVING lookup:" + lookup.toString()
                                            + "because ann is prioritized:" + ann.toString());
                            if (allAnnotations.contains(lookup)) toRemove.add(lookup);
                        }
                    }
                }
                logger
                                .debug("=====================END filtering based on priority ================================");
            }
        }
        allAnnotations.removeAll(toRemove);
        return allAnnotations;
    }
}
