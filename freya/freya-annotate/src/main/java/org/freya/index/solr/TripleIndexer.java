package org.freya.index.solr;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.freya.rdf.SesameRepositoryManager;
import org.freya.util.FreyaConstants;
import org.freya.util.QueryUtil;
import org.freya.util.StringUtil;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class TripleIndexer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(TripleIndexer.class);

    private final static List<String> FILTER_CLASS_AND_PROPERTY_URIS = Arrays.asList(
            "http://www.w3.org/2002/07/owl#Class",
            "http://www.w3.org/2000/01/rdf-schema#Class",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property",
            "http://www.w3.org/2002/07/owl#DatatypeProperty",
            "http://www.w3.org/2002/07/owl#ObjectProperty"
    );

    private final List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    @Qualifier("rdfRepository")
    private SesameRepositoryManager sesame;

    @Autowired
    private QueryUtil queryUtil;

    private SolrServer solrServer;

    public SolrServer getSolrServer() {
        return solrServer;
    }

    public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    public void indexAll() {
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    //clear();
                    indexbaseQueryClassesAndProperties();
                    indexBaseQueryInstances();
                    indexSubClasses();
                    indexPropertyRangeAndDomain();
                    solrServer.add(docs);
                    solrServer.commit();
                } catch (SolrServerException e) {
                    log.error("Error while communicating with SolrServer", e);
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    log.error("Error while adding document to Solr", e);
                    throw new RuntimeException(e);
                } finally {
                    docs.clear();
                }
            }
        });
    }

    public void clear() {
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    solrServer.deleteByQuery("*:*");
                    solrServer.commit();
                    log.info("## Deleted all indexes.");
                } catch (SolrServerException e) {
                    log.error("Error while communicating with SolrServer", e);
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    log.error("Error while adding document to Solr", e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void indexbaseQueryClassesAndProperties() {
        log.info("## Reindexing classes and properties ...");
        String baseQuery = queryUtil.readQueryContent("/queries/obg/getBaseQueryClassesAndProperties.sparql");
        indexBaseQuery(FILTER_CLASS_AND_PROPERTY_URIS, baseQuery, "{", true, true);
    }

    public void indexBaseQueryInstances() {
        log.info("## Reindexing instances ...");
        String baseQuery = queryUtil.readQueryContent("/queries/obg/getBaseQueryInstances.sparql");
        indexBaseQuery(findFilterUris(), baseQuery, "WHERE {", true, false);
    }

    public void indexSubClasses() {
        log.info("## Reindexing sub classes ...");
        String querySubClasses = queryUtil.readQueryContent("/queries/getSubClasses.sparql");
        TupleQueryResult result = sesame.executeQuery(querySubClasses);
        String uriSubClassOf = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
        try {
            while (result.hasNext()) {
                BindingSet row = result.next();
                Resource aClass = (Resource) row.getValue("x");
                Resource aSubClass = (Resource) row.getValue("y");
                // add to index: aClass - inst aSubClass -class, subClassOf - pred
                if (!aClass.stringValue().equals(aSubClass.stringValue())) {
                    addDocument(aSubClass.stringValue(), uriSubClassOf, aClass.stringValue());
                }
            }
        } catch (QueryEvaluationException e) {
            log.error("Error while indexing sub classes", e);
            throw new RuntimeException(e.getMessage());
        } finally {
            try {
                result.close();
            } catch (QueryEvaluationException e) {
                log.error("Error while closing result", e);
            }
        }
    }

    public void indexPropertyRangeAndDomain() {
        log.info("## Reindexing property range and domain");
        String queryPropertyDomainAndRange = queryUtil.readQueryContent("/queries/getPropertyDomainAndRange.sparql");
        TupleQueryResult result = sesame.executeQuery(queryPropertyDomainAndRange);
        String rangeUri = "http://www.w3.org/2000/01/rdf-schema#range";
        String domainUri = "http://www.w3.org/2000/01/rdf-schema#domain";
        try {
            while (result.hasNext()) {
                BindingSet row = result.next();
                Resource property = (Resource) row.getValue("p");
                Resource range = (Resource) row.getValue("rr");
                Resource domain = (Resource) row.getValue("dd");
                if (range != null) {
                    addDocument(property.stringValue(), rangeUri, range.stringValue());
                }
                if (domain != null) {
                    addDocument(property.stringValue(), domainUri, domain.stringValue());
                }
            }
        } catch (QueryEvaluationException e) {
            log.error("Error while indexing property range and domain", e);
            throw new RuntimeException(e.getMessage());
        } finally {
            try {
                result.close();
            } catch (QueryEvaluationException e) {
                log.error("Error while closing result", e);
            }
        }
    }

    private void indexBaseQuery(Collection<String> filterUris, String baseQuery,
                                String position, boolean camelCase, boolean saveStemmed) {
        for (String uri : filterUris) {
            String query = addExtraStatement(baseQuery, uri, position);
            TupleQueryResult result = sesame.executeQuery(query, false);
            try {
                addIndex(result, uri, camelCase, saveStemmed);
            } catch (QueryEvaluationException e) {
                log.error("Error while indexing result for uri <" + uri + ">", e);
                throw new RuntimeException(e.getMessage());
            } finally {
                try {
                    result.close();
                } catch (QueryEvaluationException e) {
                    log.error("Error while closing result", e);
                }
            }
        }
    }

    private Set<String> findFilterUris() {
        TupleQueryResult subjectResults;
        String filterClassesQuery = queryUtil.readQueryContent("/queries/getAllClasses.sparql");
        subjectResults = sesame.executeQuery(filterClassesQuery);
        Set<String> filterUris = new HashSet<String>();

        try {
            while (subjectResults.hasNext()) {
                Resource subject = (Resource) subjectResults.next().getValue(FreyaConstants.VARIABLE_T);
                filterUris.add(subject.stringValue());
            }
        } catch (QueryEvaluationException e) {
            log.error("ERROR", e);
            throw new RuntimeException(e.getMessage());
        } finally {
            try {
                subjectResults.close();
            } catch (QueryEvaluationException e) {
                log.error("ERROR on closing TupleQueryResult", e);
            }
        }

        return filterUris;
    }

    private void addIndex(TupleQueryResult result, String filterUri, boolean camelCase,
                          boolean saveStemmed) throws QueryEvaluationException {
        log.info("indexing for filter URI <" + filterUri + ">");
        while (result.hasNext()) {
            BindingSet row = result.next();
            Value e = row.getBinding(FreyaConstants.VARIABLE_E).getValue();

            URIImpl instanceURI;
            if (e instanceof BNodeImpl) {
                continue;
            } else {
                instanceURI = (URIImpl) e;
            }

            Binding bindingP = row.getBinding(FreyaConstants.VARIABLE_P);
            String predURI = bindingP == null ? null : bindingP.getValue().stringValue();

            Binding bindingL = row.getBinding(FreyaConstants.VARIABLE_L);
            String label = bindingL == null ? null : bindingL.getValue().stringValue();
            label = label == null ? instanceURI.getLocalName() : label;

            Set<String> variations = new HashSet<String>();
            findVariations(variations, label, camelCase);
            addDocument(variations, instanceURI.stringValue(), filterUri, predURI, saveStemmed);
        }
    }

    private void findVariations(Set<String> variations, String contentWord, boolean camelCase) {
        Set<String> filteredContentWords = new HashSet<String>();
        boolean dbpediaCheck = true;

        if (contentWord != null && contentWord.length() > 60) {
            dbpediaCheck = false;
        }
        if (contentWord != null && dbpediaCheck) {
            if (camelCase) {
                variations.addAll(StringUtil.findVariations(contentWord, true));
            }
            filteredContentWords.add(contentWord);
        }

        // this will add only strings shorter than 40 chars
        variations.addAll(filteredContentWords);
    }

    private void addDocument(Set<String> variations, String instanceURI,
                             String classURI, String predURI, boolean saveStemmed) {
        log.debug("addDocument: " + instanceURI + ", " + classURI + ", " + predURI + ", " + variations);
        for (String variation : variations) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField(FreyaConstants.SOLR_FIELD_ID, UUID.randomUUID());
            doc.addField(FreyaConstants.SOLR_FIELD_INSTANCE, instanceURI);
            doc.addField(FreyaConstants.SOLR_FIELD_CLASS, classURI);
            doc.addField(FreyaConstants.SOLR_FIELD_EXACT_CONTENT, variation.trim());

            if (predURI != null) {
                doc.addField(FreyaConstants.SOLR_FIELD_PROPERTY, predURI);
            }

            if (saveStemmed) {
                doc.addField(FreyaConstants.SOLR_FIELD_STEMMED_CONTENT, variation.trim().toLowerCase());
            }

            docs.add(doc);
        }
    }

    private void addDocument(String strInstance, String strProperty, String strClass) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(FreyaConstants.SOLR_FIELD_ID, UUID.randomUUID());
        doc.addField(FreyaConstants.SOLR_FIELD_INSTANCE, strInstance);
        doc.addField(FreyaConstants.SOLR_FIELD_PROPERTY, strProperty);
        doc.addField(FreyaConstants.SOLR_FIELD_CLASS, strClass);
        docs.add(doc);
        log.debug("addDocument: " + strInstance + ", " + strProperty + ", " + strClass);
    }

    private String addExtraStatement(String baseQuery, String filterURI, String where) {
        String additionalStatement = "  ?E rdf:type <" + filterURI + "> .";
        int whereStart = baseQuery.indexOf(where);

        if (whereStart == -1) {
            throw new RuntimeException("Character: " + where + " NOT found in SPARQL:\n" + baseQuery);
        }

        String beginPart = baseQuery.substring(0, whereStart).trim();
        String endPart = baseQuery.substring(whereStart + where.length());

        return beginPart + "\n" + where + "\n" + additionalStatement + endPart;

    }

    @Override
    public void destroy() throws Exception {
        this.executor.shutdownNow();
    }
}
