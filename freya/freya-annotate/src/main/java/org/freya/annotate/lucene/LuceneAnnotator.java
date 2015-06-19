package org.freya.annotate.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.freya.model.Annotation;
import org.freya.util.FreyaConstants;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * @deprecated Using {@link org.freya.annotate.solr.SolrAnnotator} instead
 */
@Component
public class LuceneAnnotator {

    @Value("${org.freya.lucene.index.dir.search}") Resource luceneIndexDir;

    public LuceneAnnotator() {}

    public void close() {
        if (this.reader != null) try {
            reader.close();

            // if(this.searcher != null) searcher.close();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private static final Log logger = LogFactory.getLog(LuceneAnnotator.class);

    File index;

    public File getIndex() {
        return index;
    }

    public void setIndex(File index) {
        this.index = index;
    }

    FileInputStream synonymFile;

    //public SynonymMap synonymMap;

    IndexReader reader;

    IndexSearcher searcher;

    @PostConstruct
    public void init() {
        try {
        if (index == null)
            index = luceneIndexDir.getFile();
      
            // if (!index.exists()) IndexTriplesExec.main(new String[0]);

            if (reader == null && index.exists())
                reader = DirectoryReader.open(FSDirectory.open(index));// IndexReader.open(FSDirectory.open(index),
                                                                       // true);
            if (searcher == null && index.exists()) {
                try {
                    // lazily instantiate searcher
                    searcher = new IndexSearcher(reader);
                } catch (Exception e) {
                    throw e;
                }
            }
        } catch (CorruptIndexException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (Throwable e) {
            logger.error(e);
        }
    }

    public Map<String, Double> getSpecificityScores() {
        Map map = new HashMap<String, Double>();
        logger.info("Need to implement....");
        return map;
    }

    /**
     * find lucene annotations for this poc specialTreatment is for common nouns so that they are searched with stem not
     * exact match
     * 
     * @param annotation
     * @return
     */
    public List<Annotation> searchIndex(Annotation annotation,
                    boolean specialTreatment) {
        if (specialTreatment) return searchStemFirst(annotation);
        List<Annotation> annotations = new ArrayList<Annotation>();
        try {
            int maxSynonyms = 0;
            EnglishAnalyzer stemAnalyser = new EnglishAnalyzer(Version.LUCENE_45);
            // Analyzer stemmedAnalyser = AnalyzerUtil.getSynonymAnalyzer(AnalyzerUtil
            // .getPorterStemmerAnalyzer(new KeywordAnalyzer()),
            // synonymMap, maxSynonyms);
            KeywordAnalyzer analyser = new KeywordAnalyzer();
            QueryParser parser =
                            new QueryParser(Version.LUCENE_45,
                                            FreyaConstants.FIELD_EXACT_CONTENT, analyser);
            String pocString = annotation.getText();
            String preparePocString = "\"" + pocString + "\"";
            String preparePocStringLowercase = "\"" + pocString.toLowerCase() + "\"";

            Query query = parser.parse(preparePocString);
            TopDocs result = searcher.search(query, 1);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            int freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            logger.debug("For " + pocString + " : " + result.totalHits);
            if (freq <= 0) {
                // search lowercased exact
                QueryParser lowerCasedParser =
                                new QueryParser(Version.LUCENE_45,
                                                FreyaConstants.FIELD_EXACT_LOWERCASED_CONTENT, analyser);
                query = lowerCasedParser.parse(preparePocStringLowercase);
                // logger.info("Searching for: " + query.toString());
                result = searcher.search(query, 1);
                freq = result.totalHits;
                if (freq > 0) {
                    result = searcher.search(query, freq);
                }
                hits = result.scoreDocs;
                logger.debug("For " + query.toString() + " : " + result.totalHits);
            }
            if (hits.length == 0 && preparePocStringLowercase.indexOf(" ") < 0) {
                // search stemmed
                QueryParser stemParser =
                                new QueryParser(Version.LUCENE_45,
                                                FreyaConstants.FIELD_STEMMED_CONTENT, stemAnalyser);
                query = stemParser.parse(preparePocStringLowercase);
                // logger.info("Searching for: " + query.toString());
                result = searcher.search(query, 1);
                freq = result.totalHits;
                if (freq > 0) {
                    result = searcher.search(query, freq);
                }
                hits = result.scoreDocs;
                logger.debug("For " + query.toString() + " : " + result.totalHits);
            }
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                searcher.explain(query, hit.doc);

                Annotation ann = new Annotation();
                HashMap features = new HashMap();
                features.put(FreyaConstants.CLASS_FEATURE_LKB, doc
                                .get(FreyaConstants.CLASS_FEATURE_LKB));
                features.put(FreyaConstants.INST_FEATURE_LKB, doc
                                .get(FreyaConstants.INST_FEATURE_LKB));
                features.put(FreyaConstants.PROPERTY_FEATURE_LKB, doc
                                .get(FreyaConstants.PROPERTY_FEATURE_LKB));
                features.put("string", doc.get(FreyaConstants.FIELD_EXACT_CONTENT));
                features.put(FreyaConstants.SCORE, hit.score);
                ann.setFeatures(features);
                ann.setEndOffset(annotation.getEndOffset());
                ann.setStartOffset(annotation.getStartOffset());
                ann.setSyntaxTree(annotation.getSyntaxTree());
                ann.setText(annotation.getText());
                annotations.add(ann);
            }
        } catch (CorruptIndexException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (ParseException e) {
            logger.error(e);
        } catch (Exception e) {
            logger.error(e);
        }
        return annotations;
    }

    /**
     * this method now search both stem and lowercase
     * 
     * @param annotation
     * @return
     */
    public List<Annotation> searchStemFirst(Annotation annotation) {
        List<Annotation> annotations = new ArrayList<Annotation>();
        String pocString = annotation.getText();
        String preparePocStringOriginal = "\"" + pocString + "\"";
        String preparePocStringLowercase = "\"" + pocString.toLowerCase() + "\"";
        try {
            int maxSynonyms = 0;
            // Analyzer stemmedAnalyser =
            // AnalyzerUtil.getSynonymAnalyzer(AnalyzerUtil
            // .getPorterStemmerAnalyzer(new KeywordAnalyzer()),
            // synonymMap, maxSynonyms);
            Analyzer stemmedAnalyser = new EnglishAnalyzer(Version.LUCENE_45);
            KeywordAnalyzer analyser = new KeywordAnalyzer();
            QueryParser stemParser =
                            new QueryParser(Version.LUCENE_CURRENT,
                                            FreyaConstants.FIELD_STEMMED_CONTENT, stemmedAnalyser);
            Query query = stemParser.parse(preparePocStringLowercase);
            TopDocs result = searcher.search(query, 1);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            int freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] stemHits = result.scoreDocs;
            ScoreDoc[] allHits = stemHits;
            // if(stemHits.length == 0) {
            // search lowercased exact
            QueryParser parser =
                            new QueryParser(Version.LUCENE_CURRENT,
                                            FreyaConstants.FIELD_EXACT_LOWERCASED_CONTENT, analyser);
            query = parser.parse(preparePocStringLowercase);
            result = searcher.search(query, 1);
            freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] lowHits = result.scoreDocs;
            allHits = (ScoreDoc[]) ArrayUtils.addAll(allHits, lowHits);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            // }
            // if(allHits.length == 0) {
            // search exact
            QueryParser exactParser =
                            new QueryParser(Version.LUCENE_CURRENT,
                                            FreyaConstants.FIELD_EXACT_CONTENT, analyser);
            query = exactParser.parse(preparePocStringLowercase);
            result = searcher.search(query, 1);
            freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            allHits = (ScoreDoc[]) ArrayUtils.addAll(allHits, result.scoreDocs);;
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            // }
            for (ScoreDoc hit : allHits) {
                Document doc = searcher.doc(hit.doc);
                searcher.explain(query, hit.doc);
                Annotation ann = new Annotation();
                HashMap features = new HashMap();
                features.put(FreyaConstants.CLASS_FEATURE_LKB, doc
                                .get(FreyaConstants.CLASS_FEATURE_LKB));
                features.put(FreyaConstants.INST_FEATURE_LKB, doc
                                .get(FreyaConstants.INST_FEATURE_LKB));
                features.put(FreyaConstants.PROPERTY_FEATURE_LKB, doc
                                .get(FreyaConstants.PROPERTY_FEATURE_LKB));
                features.put("string", doc.get(FreyaConstants.FIELD_EXACT_CONTENT));
                features.put("score", hit.score);
                ann.setFeatures(features);
                ann.setEndOffset(annotation.getEndOffset());
                ann.setStartOffset(annotation.getStartOffset());
                ann.setSyntaxTree(annotation.getSyntaxTree());
                ann.setText(annotation.getText());
                annotations.add(ann);
            }
        } catch (CorruptIndexException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (ParseException e) {
            logger.error(e);
        } catch (Exception e) {
            logger.error(e);
        }
        return annotations;
    }

    /**
     * @return
     */
    public Set<String> findPropertyURIs() {
        Set<String> uris = new HashSet<String>();
        uris.addAll(findPropertyURIs(OWL.DATATYPEPROPERTY.stringValue(), null));
        uris.addAll(findPropertyURIs(OWL.OBJECTPROPERTY.stringValue(), null));
        uris.addAll(findRDFPropertyURIs(null));
        return uris;
    }

    /**
     * @param max
     * @return
     */
    public Set<String> findPropertyURIs(Integer max) {
        Set<String> uris = new HashSet<String>();
        uris.addAll(findPropertyURIs(OWL.DATATYPEPROPERTY.stringValue(), max));
        uris.addAll(findPropertyURIs(OWL.OBJECTPROPERTY.stringValue(), max));
        uris.addAll(findRDFPropertyURIs(max));
        return uris;
    }

    /**
     * @return
     */
    public Set<String> findDatatypePropertyURIs() {
        Set<String> uris = new HashSet<String>();
        uris.addAll(findPropertyURIs(OWL.DATATYPEPROPERTY.stringValue(), null));
        return uris;
    }

    /**
     * @return
     */
    public Set<String> findObjectPropertyURIs() {
        Set<String> uris = new HashSet<String>();
        uris.addAll(findPropertyURIs(OWL.OBJECTPROPERTY.stringValue(), null));
        return uris;
    }

    /**
     * @param max
     * @return
     */
    public Set<String> findRDFPropertyURIs(Integer max) {
        Set<String> uris = new HashSet<String>();
        String owl = "http://www.w3.org/2002/07/owl";
        Set<String> rdfProps = findPropertyURIs(RDF.PROPERTY.stringValue(), max);
        for (String prop : rdfProps) {
            if (prop != null && !prop.startsWith(owl)) uris.add(prop);
        }
        return uris;
    }

    /**
     * @return
     */
    public Set<String> findClassURIs() {
        Set<String> uris = new HashSet<String>();
        uris.addAll(findPropertyURIs(OWL.CLASS.stringValue(), null));
        uris.addAll(findPropertyURIs(RDFS.CLASS.stringValue(), null));
        return uris;
    }

    /**
     * find lucene annotations for this poc
     * 
     * @param annotation
     * @return
     */
    public Set<String> findPropertyURIs(String propertyType, Integer max) {
        Set<String> uris = new HashSet<String>();
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            QueryParser parser =
                            new QueryParser(Version.LUCENE_CURRENT,
                                            FreyaConstants.CLASS_FEATURE_LKB, analyzer);
            Query query = parser.parse("\"" + propertyType + "\"");
            TopDocs result = searcher.search(query, 1);
            int freq = result.totalHits;
            if (max != null) freq = max.intValue();
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            logger.debug("For " + query.toString() + " : " + result.totalHits
                            + " max:" + max);
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                searcher.explain(query, hit.doc);

                uris.add(doc.get(FreyaConstants.INST_FEATURE_LKB));
            }
        } catch (CorruptIndexException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (ParseException e) {
            logger.error(e);
        }
        return uris;
    }

    /**
     * @param propertyUri
     * @return
     */
    public Set<String> findPropertyRange(String propertyUri) {
        String rangeUri = "http://www.w3.org/2000/01/rdf-schema#range";
        return searchForClass(propertyUri, rangeUri);
    }

    /**
     * @param propertyUri
     * @return
     */
    public Set<String> findPropertyDomain(String propertyUri) {
        String rangeUri = "http://www.w3.org/2000/01/rdf-schema#domain";
        return searchForClass(propertyUri, rangeUri);
    }

    /**
     * given classUri search for field class so that pred=subClassOf
     * 
     * @param classUri
     * @return
     */
    public Set<String> findSubClasses(String classUri) {
        String propertyURI = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
        Set<String> subClasses = new HashSet<String>();
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            String[] fields =
                            new String[] {FreyaConstants.CLASS_FEATURE_LKB,
                                            FreyaConstants.PROPERTY_FEATURE_LKB};
            BooleanClause.Occur[] flags =
            {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST};
            String subClassUri = "\"" + propertyURI + "\"";
            String[] queries = new String[] {"\"" + classUri + "\"", subClassUri};
            Query query =
                            MultiFieldQueryParser.parse(Version.LUCENE_30, queries, fields,
                                            flags, analyzer);
            TopDocs result = searcher.search(query, 1);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            int freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);

                subClasses.add(doc.get(FreyaConstants.INST_FEATURE_LKB));
            }
        } catch (CorruptIndexException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (ParseException e) {
            logger.error(e);
        }
        return subClasses;
    }

    /**
     * check whether this is datatype property or not
     * 
     * @param propertyUri
     * @return
     */
    public boolean isItDatatypeProperty(String propertyUri) {
        Set<String> result =
                        checkIfItIsDatatypeProperty(propertyUri);
        boolean exists = false;
        if (result != null && result.size() > 0) exists = true;
        // logger.info("isItDatatypeProperty for " + propertyUri + " is " + exists);
        return exists;
    }

    /**
     * @param classUri
     * @return
     */
    private Set<String> getDefinedPropertiesWhereClassIsADomain(String classUri) {
        Set<String> properties =
                        searchForInstance(classUri, RDFS.DOMAIN.toString());
        return properties;
    }

    public Set<String> getDefinedPropertiesWhereClassIsADomain(String classUri,
                    boolean forceSuperClasses) {
        Set<String> properties = new HashSet<String>();
        if (forceSuperClasses) {
            Set<String> superClasses = findSuperClasses(classUri, new HashSet());
            superClasses.add(classUri);
            for (String uri : superClasses) {
                properties.addAll(getDefinedPropertiesWhereClassIsADomain(uri));
            }
        }
        return properties;
    }

    public Set<String> getDefinedPropertiesWhereClassIsARange(String classUri,
                    boolean forceSuperClasses) {
        Set<String> properties = new HashSet<String>();
        if (forceSuperClasses) {
            Set<String> superClasses = findSuperClasses(classUri, new HashSet());
            superClasses.add(classUri);
            for (String uri : superClasses) {
                properties.addAll(getDefinedPropertiesWhereClassIsARange(uri));
            }
        }
        return properties;
    }

    /**
     * @param classUri
     * @return
     */
    private Set<String> getDefinedPropertiesWhereClassIsARange(String classUri) {
        Set<String> properties = searchForInstance(classUri, RDFS.RANGE.toString());
        return properties;
    }

    /**
     * @param classUri
     * @return
     */
    public Set<String> getNeighbouringClassesWhereGivenClassIsADomain(
                    String classUri, boolean forceSuperClasses) {
        Set<String> classes = new HashSet<String>();
        if (forceSuperClasses) {
            // here recursively go and first find all super classes
            Set<String> feedClasses = findSuperClasses(classUri, new HashSet());
            feedClasses.add(classUri);
            // then for each superclass do the same as above
            for (String uri : feedClasses) {
                classes.addAll(getNeighbouringClassesWhereGivenClassIsADomain(uri));
            }
        }
        return classes;
    }

    public Set<String> getNeighbouringClassesWhereGivenClassIsARange(
                    String classUri, boolean forceSuperClasses) {
        Set<String> classes = new HashSet<String>();
        if (forceSuperClasses) {
            // here recursively go and first find all super classes
            Set<String> feedClasses = findSuperClasses(classUri, new HashSet());
            feedClasses.add(classUri);
            logger.info("found " + feedClasses.size() + " super classes for "
                            + classUri);
            // then for each superclass do the same as above
            for (String uri : feedClasses) {
                classes.addAll(getNeighbouringClassesWhereGivenClassIsARange(uri));
                logger.info("found " + classes.size() + " elements for " + uri);
            }
        }
        return classes;
    }

    /**
     * @param classUri
     * @return
     */
    public Set<String> findSuperClasses(String classUri,
                    Set<String> superClassesToSave) {
        boolean searchFinished = false;
        while (!searchFinished) {
            Set<String> directSuperClasses =
                            searchForClass(classUri, RDFS.SUBCLASSOF.toString());
            // System.out.println("SuperClasses for:" + classUri + " are: "
            // + directSuperClasses.toString());
            if (directSuperClasses == null || directSuperClasses.size() == 0
                            || superClassesToSave.containsAll(directSuperClasses)) {
                searchFinished = true;
                // logger.info("searchFinished for SuperClasses");
            } else {
                // System.out.println("size:"+directSuperClasses.size());
                superClassesToSave.addAll(directSuperClasses);
                for (String cUri : directSuperClasses) {
                    // System.out.println("Curi:"+cUri);
                    superClassesToSave.addAll(findSuperClasses(cUri, superClassesToSave));
                }
                searchFinished = true;
            }
        }
        logger.info("For " + classUri + " found " + superClassesToSave.size()
                        + " super-classes.");
        return superClassesToSave;
    }

    /**
     * @param classUri
     * @return
     */
    public Set<String> getNeighbouringClassesWhereGivenClassIsADomain(
                    String classUri) {
        Set<String> classes = new HashSet<String>();
        Set<String> properties =
                        searchForInstance(classUri, RDFS.DOMAIN.toString());
        for (String property : properties) {
            classes.addAll(searchForClass(property, RDFS.RANGE.toString()));
        }
        return classes;
    }

    /**
     * @param classUri
     * @return
     */
    public Set<String> getNeighbouringClassesWhereGivenClassIsARange(
                    String classUri) {
        Set classes = new HashSet();
        Set<String> properties = searchForInstance(classUri, RDFS.RANGE.toString());
        for (String property : properties) {
            classes.addAll(searchForClass(property, RDFS.DOMAIN.toString()));
        }
        return classes;
    }

    public Set<String> searchForInstance(String classUri, String pred) {
        Set<String> uris = new HashSet<String>();
        String[] fields =
                        new String[] {FreyaConstants.CLASS_FEATURE_LKB,
                                        FreyaConstants.PROPERTY_FEATURE_LKB};
        BooleanClause.Occur[] flags =
        {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST};
        String[] queries = new String[] {"\"" + classUri + "\"", "\"" + pred + "\""};
        Query query;
        try {
            query =
                            MultiFieldQueryParser.parse(Version.LUCENE_30, queries, fields,
                                            flags, new KeywordAnalyzer());
            TopDocs result = searcher.search(query, 1);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            int freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                uris.add(doc.get(FreyaConstants.INST_FEATURE_LKB));
            }
        } catch (ParseException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
        return uris;
    }

    /**
     * 
     * @param inst
     * @param className
     * @return
     */
    Set<String> checkIfItIsDatatypeProperty(String inst) {
        Set<String> classUris = new HashSet<String>();
        String[] fields =
                        new String[] {FreyaConstants.INST_FEATURE_LKB,
                                        FreyaConstants.CLASS_FEATURE_LKB};
        BooleanClause.Occur[] flags =
        {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST};
        String[] queries = new String[] {"\"" + inst + "\"", "\"" + OWL.DATATYPEPROPERTY.toString() + "\""};
        Query query;
        try {
            query =
                            MultiFieldQueryParser.parse(Version.LUCENE_30, queries, fields,
                                            flags, new KeywordAnalyzer());
            TopDocs result = searcher.search(query, 1);
            logger.info("For " + query.toString() + " : " + result.totalHits);
            int freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                classUris.add(doc.get(FreyaConstants.INST_FEATURE_LKB));
            }
        } catch (ParseException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
        return classUris;
    }

    /**
     * @param inst
     * @param pred
     * @return
     */
    Set<String> searchForClass(String inst, String pred) {
        Set<String> classUris = new HashSet<String>();
        String[] fields =
                        new String[] {FreyaConstants.INST_FEATURE_LKB,
                                        FreyaConstants.PROPERTY_FEATURE_LKB};
        BooleanClause.Occur[] flags =
        {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST};
        String[] queries = new String[] {"\"" + inst + "\"", "\"" + pred + "\""};
        Query query;
        try {
            query =
                            MultiFieldQueryParser.parse(Version.LUCENE_30, queries, fields,
                                            flags, new KeywordAnalyzer());
            TopDocs result = searcher.search(query, 1);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            int freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                classUris.add(doc.get(FreyaConstants.CLASS_FEATURE_LKB));
            }
        } catch (ParseException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
        return classUris;
    }

    /**
     * @return
     */
    public Set<String> findTopClasses() {
        String propertyURI = RDFS.SUBCLASSOF.toString();
        Set<String> allClasses = new HashSet<String>();
        Set<String> topClasses = new HashSet<String>();
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            QueryParser parser =
                            new QueryParser(Version.LUCENE_CURRENT,
                                            FreyaConstants.PROPERTY_FEATURE_LKB, analyzer);
            Query query = parser.parse("\"" + propertyURI + "\"");
            TopDocs result = searcher.search(query, 1);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            int freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                allClasses.add(doc.get(FreyaConstants.CLASS_FEATURE_LKB));
            }
            for (String classUri : allClasses) {
                logger.debug("Checking whether " + classUri + " is a top class.");
                // search inst and pred retrieve class
                // if class exists that means it is not top class otherwise add to
                // topClasses
                Set<String> classes = searchForClass(classUri, propertyURI);
                logger.debug("top classes:" + classes.size());
                if (classes != null || classes.size() > 0) {
                    logger.debug("This is not a top class...");
                } else {
                    topClasses.add(classUri);
                    logger.debug("Adding " + classUri + " to top classes.");
                }
            }
        } catch (CorruptIndexException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (ParseException e) {
            logger.error(e);
        }
        return topClasses;
    }

    /**
     * randomly gets the direct type
     * 
     * @param instanceUri
     * @return
     */
    public String findOneDirectType(String instanceUri) {
        return findDirectTypes(instanceUri, 1).get(0);
    }

    public List<String> findDirectTypes(String instanceUri) {
        return findDirectTypes(instanceUri, null);
    }

    /**
     * find direct types
     * 
     * @param annotation
     * @return
     */
    public List<String> findDirectTypes(String instanceUri, Integer max) {
        Set<String> dTypes = new HashSet<String>();
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            QueryParser parser =
                            new QueryParser(Version.LUCENE_CURRENT, "inst", analyzer);
            Query query = parser.parse("\"" + instanceUri + "\"");
            TopDocs result = searcher.search(query, 1);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            int freq = 0;
            if (max != null)
                freq = max;
            else
                freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                searcher.explain(query, hit.doc);

                dTypes.add(doc.get(FreyaConstants.CLASS_FEATURE_LKB));
            }
        } catch (CorruptIndexException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (ParseException e) {
            logger.error(e);
        }
        logger.debug("there are " + dTypes.size() + " unique direct types");
        return new ArrayList(dTypes);
    }

    /**
     * find lucene annotations for this poc
     * 
     * @param annotation
     * @return
     */
    public List<String> findLabels(String instanceUri) {
        Set<String> labels = new HashSet<String>();
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            String[] fields =
                            new String[] {FreyaConstants.INST_FEATURE_LKB,
                                            FreyaConstants.PROPERTY_FEATURE_LKB};
            BooleanClause.Occur[] flags =
            {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST};
            String labelOrTitleUris =
                            "\"http://www.w3.org/2000/01/rdf-schema#label\"";// +
            // " OR http://purl.org/dc/elements/1.1/title";
            String[] queries =
                            new String[] {"\"" + instanceUri + "\"", labelOrTitleUris};
            Query query =
                            MultiFieldQueryParser.parse(Version.LUCENE_30, queries, fields,
                                            flags, analyzer);
            TopDocs result = searcher.search(query, 1);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            int freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                labels.add(doc.get(FreyaConstants.FIELD_EXACT_CONTENT));
            }
        } catch (CorruptIndexException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (ParseException e) {
            logger.error(e);
        }
        return new ArrayList(labels);
    }

    public List<String> findLiteral(String instanceUri, String propertyURI) {
        Set<String> labels = new HashSet<String>();
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            String[] fields =
                            new String[] {FreyaConstants.INST_FEATURE_LKB,
                                            FreyaConstants.PROPERTY_FEATURE_LKB};
            BooleanClause.Occur[] flags =
            {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST};
            String labelOrTitleUris = "\"" + propertyURI + "\"";
            String[] queries =
                            new String[] {"\"" + instanceUri + "\"", labelOrTitleUris};
            Query query =
                            MultiFieldQueryParser.parse(Version.LUCENE_30, queries, fields,
                                            flags, analyzer);
            TopDocs result = searcher.search(query, 1);
            logger.debug("For " + query.toString() + " : " + result.totalHits);
            int freq = result.totalHits;
            if (freq > 0) {
                result = searcher.search(query, freq);
            }
            ScoreDoc[] hits = result.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                labels.add(doc.get(FreyaConstants.FIELD_EXACT_CONTENT));
            }
        } catch (CorruptIndexException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (ParseException e) {
            logger.error(e);
        }
        return new ArrayList(labels);
    }

    public static void main(String[] args) {
        LuceneAnnotator an = new LuceneAnnotator();
        an.init();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream("WordNet-3.0/dict/prolog/wn_s.pl");
           // an.synonymMap = new SynonymMap(inputStream);
            an.setIndex(new File("/home/danica/freya/mooney/index"));
            String classUri = "http://dbpedia.org/ontology/Mountain";
            Set<String> result = an.findSuperClasses(classUri, new HashSet());
            System.out.println("Finished 1: " + result.toString());
            result = an.getDefinedPropertiesWhereClassIsADomain(classUri, true);
            System.out.println("Finished 2: " + result.toString());
            String thisPropertyUri = "http://www.mooney.net/geo#cityPopulation";
            boolean isIt = an.isItDatatypeProperty(thisPropertyUri);
            System.out.println("Finished 3: " + isIt);
            // Annotation annotation = new Annotation();
            // annotation.setText("album");
            // System.out.println("started ");
            // Set<Annotation> result = an.searchIndex(annotation);
            // System.out.println("finished "+result.size());
            inputStream.close();
        } catch (FileNotFoundException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
