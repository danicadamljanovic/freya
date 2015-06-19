package org.freya.annotate.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.freya.index.solr.TripleIndexer;
import org.freya.model.Annotation;
import org.freya.util.AnnotationSolrScoreComparator;
import org.freya.util.FreyaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@Component
public class SolrAnnotator {

    private static final Logger log = LoggerFactory.getLogger(SolrAnnotator.class);

    @Autowired
    private TripleIndexer indexer;

    public SolrDocumentList query(String k, String v) {
        SolrQuery query = new SolrQuery();
        query.setParam("fl", "*,score");
        query.setQuery(k + ":\"" + v + "\"");
        return query(query);
    }

    public SolrDocumentList query(SolrParams params) {
        SolrDocumentList documentList = null;
        try {
            QueryResponse response = indexer.getSolrServer().query(params);
            documentList = response.getResults();
        } catch (SolrServerException e) {
            log.error("Error while communicating with SolrServer", e);
            throw new RuntimeException(e);
        }
        return documentList;
    }

    public List<String> findDirectTypes(String instanceUri) {
        Set<String> dTypes = new HashSet<String>();
        SolrDocumentList docs = query(FreyaConstants.SOLR_FIELD_INSTANCE, instanceUri);
        for (SolrDocument doc : docs) {
            dTypes.add(doc.get(FreyaConstants.SOLR_FIELD_CLASS).toString());
        }

        log.debug("there are " + dTypes.size() + " unique direct types");

        return new ArrayList<String>(dTypes);
    }

    public List<String> findLabels(String instanceUri) {
        Set<String> labels = new HashSet<String>();
        SolrQuery params = new SolrQuery();

        params.setQuery(FreyaConstants.SOLR_FIELD_INSTANCE + ":\"" + instanceUri + "\" AND " +
                FreyaConstants.SOLR_FIELD_PROPERTY + ":\"http://www.w3.org/2000/01/rdf-schema#label\"");
        SolrDocumentList docs = query(params);
        for (SolrDocument doc: docs) {
            labels.add(doc.get(FreyaConstants.SOLR_FIELD_EXACT_CONTENT).toString());
        }

        log.debug("there are " + labels.size() + " unique labels");

        return new ArrayList<String>(labels);
    }

    public List<String> findLiteral(String instanceUri, String propertyURI) {
        Set<String> labels = new HashSet<String>();
        SolrQuery params = new SolrQuery(
                FreyaConstants.SOLR_FIELD_INSTANCE + ":\"" + instanceUri + "\" AND " +
                        FreyaConstants.SOLR_FIELD_PROPERTY + ":\"" + propertyURI + "\""
        );
        SolrDocumentList docs = query(params);
        for (SolrDocument doc: docs) {
            labels.add(doc.get(FreyaConstants.SOLR_FIELD_EXACT_CONTENT).toString());
        }

        log.debug("there are " + labels.size() + " unique literals");

        return new ArrayList<String>(labels);
    }

    public List<Annotation> searchExactContentFirst(Annotation annotation) {
        return searchIndexByThreeSteps(annotation, FreyaConstants.SOLR_FIELD_EXACT_CONTENT,
                FreyaConstants.SOLR_FIELD_LOWERCASE_CONTENT, FreyaConstants.SOLR_FIELD_STEMMED_CONTENT);
    }

    public List<Annotation> searchStemmedContentFirst(Annotation annotation) {
        return searchIndexByThreeSteps(annotation, FreyaConstants.SOLR_FIELD_STEMMED_CONTENT,
                FreyaConstants.SOLR_FIELD_LOWERCASE_CONTENT, FreyaConstants.SOLR_FIELD_EXACT_CONTENT);
    }

    public List<Annotation> searchIndexByThreeSteps(Annotation annotation, String first, String second, String third) {
        SolrDocumentList docs = query(first, annotation.getText());
        if (docs.size() == 0) {
            docs = query(second, annotation.getText());
        }
        if (docs.size() == 0) {
            docs = query(third, annotation.getText());
        }

        return populateAnnotationList(docs, annotation);
    }

    private List<Annotation> populateAnnotationList(SolrDocumentList docs, Annotation annotation) {
        List<Annotation> annotations = new ArrayList<Annotation>();
        for (SolrDocument doc : docs) {
            Annotation ann = new Annotation();
            HashMap<String, Object> features = new HashMap<String, Object>();
            features.put(FreyaConstants.CLASS_FEATURE_LKB, doc.get(FreyaConstants.SOLR_FIELD_CLASS));
            features.put(FreyaConstants.INST_FEATURE_LKB, doc.get(FreyaConstants.SOLR_FIELD_INSTANCE));
            features.put(FreyaConstants.PROPERTY_FEATURE_LKB, doc.get(FreyaConstants.SOLR_FIELD_PROPERTY));
            features.put("string", doc.get(FreyaConstants.SOLR_FIELD_EXACT_CONTENT));
            features.put(FreyaConstants.SCORE, doc.get(FreyaConstants.SCORE));
            ann.setFeatures(features);
            ann.setEndOffset(annotation.getEndOffset());
            ann.setStartOffset(annotation.getStartOffset());
            ann.setSyntaxTree(annotation.getSyntaxTree());
            ann.setText(annotation.getText());
            annotations.add(ann);
        }
        //sort annotations here
        System.out.println("Before sorting:"+annotations.toString());
        Comparator c = new AnnotationSolrScoreComparator();
        Collections.sort(annotations, c);
        System.out.println("After sorting:"+annotations.toString());
        return annotations;
    }

}



