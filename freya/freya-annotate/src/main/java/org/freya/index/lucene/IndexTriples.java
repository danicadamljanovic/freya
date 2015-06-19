package org.freya.index.lucene;

/**
 * This is the demo class from Lucene currently used with stop analysers which accepts a list of stop words as
 * constructor and does not do stemming; however it does extends the featuers of simpleAnalyser so it works on
 * lowercased text.
 * 
 * @danica
 */
import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.freya.util.FreyaConstants;
import org.freya.util.QueryUtil;
import org.freya.util.StringUtil;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

@Component
public class IndexTriples {

    
    @Autowired QueryUtil queryUtil;
    public @Value("${org.freya.lucene.index.dir}") org.springframework.core.io.Resource luceneIndexDir;
    public @Value("${org.freya.rdf.repository.url}") String repositoryURL;
    public @Value("${org.freya.rdf.repository.id}") String repositoryId;

    protected static String stopWordsFilePath;

    private static final Log logger = LogFactory.getLog(IndexTriples.class);

    public static AtomicInteger docCounter = new AtomicInteger(1);

    protected void init(String theRepositoryURLString,
                    String theRepositoryIDString) {
        initRepository(theRepositoryURLString, theRepositoryIDString);
    }

    static File INDEX_DIR = null;

    RepositoryConnection conn;

    Repository rep;

    static boolean create = true;

    static String baseQueryInstances;

    static String baseQueryClassesAndProperties;

    static String filterClassesQuery;

    static String getSubClasses;

    static String getPropertyDomainAndRange;

    static boolean indexInstancesOnly = false;

   static InputStream synMapStream;

    /**
     * @param baseQuery
     * @param filterUris
     * @param stem
     */
    public static void makeIndex(IndexWriter writer, String baseQuery,
                    Set<String> filterUris, boolean stem, IndexTriples indexTriples,
                    boolean camelCase, String stringToBeSearchedInTheBaseQuery, boolean firstIndex,
                    boolean includeInferred) {
        try {
            long total = filterUris.size();;
            long countFilter = 0;
            for (String filterUri : filterUris) {
                // if (countFilter==1000) break;
                logger.info("Processing:"+countFilter+"/"+total);
                long startAll = System.currentTimeMillis();
                String query =
                                indexTriples.extendQuery(baseQuery, filterUri,
                                                stringToBeSearchedInTheBaseQuery, firstIndex);
                // System.out.println("Query:\n" + query);
                logger.debug("Processing query with filterURI(" + countFilter
                                + "/" + filterUris.size() + "):" + filterUri);
                long startQ = System.currentTimeMillis();
                logger.info("Query:\n" + query);
                CloseableIteration<BindingSet, QueryEvaluationException> result =
                                executeSingleQuery(query, indexTriples.rep.getConnection(), includeInferred);

                long endQ = System.currentTimeMillis();
                logger.debug("Finished SPARQL query evaluation for:"
                                + (endQ - startQ) + "ms");
                long start = System.currentTimeMillis();
                indexTriples(writer, result, stem, filterUri, camelCase);
                long end = System.currentTimeMillis();
                logger.debug("Finished reading from SPARQL and adding to lucene for:"
                                                + (end - start) + "ms");

                if (countFilter % 10000 == 0)
                    logger.info(" Processing " + countFilter + " filter URI");
                countFilter++;
                logger.debug("Finished " + filterUri + " for:"
                                + (System.currentTimeMillis() - startAll) + "ms");
            }
        } catch (Exception e) {
          logger.error(e);
        }
    }

    /**
     * @param writer
     * @param file
     * @throws IOException
     */
    static void indexTriples(IndexWriter writer,
                    CloseableIteration<BindingSet, QueryEvaluationException> result,
                    boolean saveStemmed, String classURIString,
                    boolean additionalProcessing) {
        long countDocuments = 0;
        long overallStart = System.currentTimeMillis();
        try {
            int rowCounter = 0;
            while (result.hasNext()) {
                rowCounter++;
                long start = System.currentTimeMillis();
                BindingSet row = result.next();
                URI instanceURI = null;
                if (row.getBinding(FreyaConstants.VARIABLE_E).getValue() instanceof URIImpl) {
                    instanceURI =
                                    ((URIImpl) row.getBinding(FreyaConstants.VARIABLE_E)
                                                    .getValue());
                } else if (row.getBinding(FreyaConstants.VARIABLE_E).getValue() instanceof BNodeImpl) {
                    // blank node, skip it
                    continue;
                } else
                    instanceURI =
                                    ((URIImpl) row.getBinding(FreyaConstants.VARIABLE_E).getValue());
                String instanceURIString = instanceURI.stringValue();
                String predURI = null;
                if (row.getBinding(FreyaConstants.VARIABLE_P) != null)
                    predURI =
                                    row.getBinding(FreyaConstants.VARIABLE_P).getValue()
                                                    .toString();
                String label = null;
                Set<String> contentWords = new HashSet<String>();
                if (row.getBinding(FreyaConstants.VARIABLE_L) != null) {
                    label =
                                    row.getBinding(FreyaConstants.VARIABLE_L).getValue()
                                                    .stringValue().toString();
                    contentWords.add(label);
                }
                long end = System.currentTimeMillis();
                if ((end - start) > 100)
                    logger.debug(rowCounter + "r for "
                                    + (end - start) + "ms " + classURIString);
                // add local names only if there are no labels/literals
                if (contentWords.size() == 0) {
                    contentWords.add(instanceURI.getLocalName());
                    logger.debug("Adding " + instanceURI.getLocalName() + " because label is null");
                }
                Set<String> variations = new HashSet<String>();
                Set<String> filteredContentWords = new HashSet();
                boolean dbpediaCheck = true;

                for (String word : contentWords) {
                    // comment out this line if you are not working with dbpedia
                    if (word != null && word.length() > 60) dbpediaCheck = false;

                    if (word != null && dbpediaCheck) { // {
                        // && word.length() < 40// && word.length() > 2
                        // && !(word.contains("."))
                        // && !(word.contains(",")) && !(word.contains("["))
                        // && !(word.contains("'"))) {
                        if (additionalProcessing) {
                            variations.addAll(StringUtil.findVariations(word, true));
                            // variations.addAll(StringUtil.findVariations(instanceURI
                            // .getLocalName(), true));
                            // variations.add(instanceURI.getLocalName());
                        }
                        filteredContentWords.add(word);
                    }
                }
                // this will add only strings shorter than 40 chars
                variations.addAll(filteredContentWords);
                start = end;
                end = System.currentTimeMillis();
                // if((end - start) > 50)
                // System.out.println("Finished deciding which words to index for "
                // + (end - start) + "ms");
                start = end;
                Field instField =
                                new Field(FreyaConstants.INST_FEATURE_LKB, instanceURIString,
                                                Field.Store.YES, Field.Index.ANALYZED);
                Field classField =
                                new Field(FreyaConstants.CLASS_FEATURE_LKB, classURIString,
                                                Field.Store.YES, Field.Index.ANALYZED);
                for (String variation : variations) {
                    Document doc = new Document();
                    doc.add(instField);
                    doc.add(classField);
                    if (predURI != null) {
                        Field predField =
                                        new Field(FreyaConstants.PROPERTY_FEATURE_LKB, predURI,
                                                        Field.Store.YES, Field.Index.ANALYZED);
                        doc.add(predField);
                    }
                    Field exactContentField =
                                    new Field(FreyaConstants.FIELD_EXACT_CONTENT, variation
                                                    .trim(), Field.Store.YES, Field.Index.ANALYZED);
                    doc.add(exactContentField);
                    // if(!variation.trim().equals(variation.trim().toLowerCase())) {
                    Field exactLowercasedContentField =
                                    new Field(FreyaConstants.FIELD_EXACT_LOWERCASED_CONTENT,
                                                    variation.trim().toLowerCase(), Field.Store.NO,
                                                    Field.Index.ANALYZED);
                    doc.add(exactLowercasedContentField);
                    // }
                    if (saveStemmed) {
                        Field stemmedContentField =
                                        new Field(FreyaConstants.FIELD_STEMMED_CONTENT, variation
                                                        .trim().toLowerCase(), Field.Store.NO,
                                                        Field.Index.ANALYZED);
                        doc.add(stemmedContentField);
                    }
                    writer.addDocument(doc);
                    if (countDocuments % 10000 == 0 && countDocuments!=0)
                        logger.info(" " + countDocuments + " docs ");
                    countDocuments++;
                }
                end = System.currentTimeMillis();
                if ((end - start) > 100)
                    logger.info(variations.size()
                                    + " docs from 1 row added to lucene for:" + (end - start)
                                    + "ms");
            }
            result.close();
            long end = System.currentTimeMillis();
            if (end - overallStart > 100)
                logger.info("Total results of sparql processed for "
                                + (end - overallStart) + "ms");
        } catch (QueryEvaluationException e) {
            logger.error(e);
        } catch (Exception e) {
            logger.error(e);
        } catch (Throwable e) {
            logger.error(e);
        }
        logger.info("Added:" + countDocuments + " documents to index.");
    }


    /**
     * @param baseQuery
     * @param filterUris
     * @param stem
     */
    public void addSubClassesToIndex(IndexWriter writer) {

        long countDocuments = 0;
        try {
            TupleQueryResult result =
                            rep.getConnection().prepareTupleQuery(
                                            org.openrdf.query.QueryLanguage.SPARQL,
                                            getSubClasses).evaluate();
            while (result.hasNext()) {
                BindingSet row = result.next();
                Resource aClass =
                                (Resource) row.getValue(
                                                "x");
                Resource aSubClass =
                                (Resource) row.getValue(
                                                "y");
                // add to index: aClass - inst aSubClass -class, subClassOf - pred
                if (!aClass.stringValue().equals(aSubClass.stringValue())) {
                    Document doc = new Document();
                    Field instField =
                                    new Field(FreyaConstants.INST_FEATURE_LKB, aSubClass.stringValue(),
                                                    Field.Store.YES, Field.Index.ANALYZED);
                    // System.out.println("Adding class:"+aClass.stringValue());
                    Field classField =
                                    new Field(FreyaConstants.CLASS_FEATURE_LKB, aClass.stringValue(),
                                                    Field.Store.YES, Field.Index.ANALYZED);
                    Field predField =
                                    new Field(FreyaConstants.PROPERTY_FEATURE_LKB,
                                                    "http://www.w3.org/2000/01/rdf-schema#subClassOf",
                                                    Field.Store.YES, Field.Index.ANALYZED);
                    // System.out.println("Adding inst:"+aSubClass.stringValue());
                    // System.out.println("Adding pred:"+"http://www.w3.org/2000/01/rdf-schema#subClassOf");
                    doc.add(instField);
                    doc.add(classField);
                    doc.add(predField);

                    writer.addDocument(doc);
                    if (countDocuments % 10000 == 0 && countDocuments!=0)
                        logger.info(" " + countDocuments + " docs ");
                    countDocuments++;
                }
            }
            result.close();

        } catch (Exception e) {
            logger.error(e);
        } catch (Throwable e) {
            logger.error(e);
        }
        logger.info("Added:" + countDocuments + " documents (subClassOf predicate) to index.");
    }

    /**
     * @param baseQuery
     * @param filterUris
     * @param stem
     */
    public void addPropertyRangeAndDomainToIndex(IndexWriter writer) {
        long countDocuments = 0;
        try {

            TupleQueryResult result =
                            rep.getConnection().prepareTupleQuery(
                                            org.openrdf.query.QueryLanguage.SPARQL,
                                            getPropertyDomainAndRange).evaluate();

            while (result.hasNext()) {
                BindingSet row = result.next();
                Resource property = (Resource) row.getValue("p");
                Resource range = (Resource) row.getValue("rr");
                Resource domain = (Resource) row.getValue("dd");
                // add to index: aClass - inst aSubClass -class, subClassOf - pred
                String rangeUri = "http://www.w3.org/2000/01/rdf-schema#range";
                String domainUri = "http://www.w3.org/2000/01/rdf-schema#domain";

                if (range != null) {

                    Document doc = new Document();
                    Field instField =
                                    new Field(FreyaConstants.INST_FEATURE_LKB, property
                                                    .stringValue(), Field.Store.YES, Field.Index.ANALYZED);
                   logger.debug("Adding inst:" + property.stringValue());
                    Field predField =
                                    new Field(FreyaConstants.PROPERTY_FEATURE_LKB, rangeUri,
                                                    Field.Store.YES, Field.Index.ANALYZED);
                    logger.debug("Adding pred:" + rangeUri);
                    Field classField =
                                    new Field(FreyaConstants.CLASS_FEATURE_LKB,
                                                    range.stringValue(), Field.Store.YES,
                                                    Field.Index.ANALYZED);
                    logger.debug("Adding class:" + range.stringValue());
                    doc.add(instField);
                    doc.add(classField);
                    doc.add(predField);
                    writer.addDocument(doc);
                    if (countDocuments % 10000 == 0 && countDocuments!=0)
                        logger.info(" " + countDocuments + " docs ");
                }
                if (domain != null) {
                    Document doc = new Document();
                    Field instField =
                                    new Field(FreyaConstants.INST_FEATURE_LKB, property
                                                    .stringValue(), Field.Store.YES, Field.Index.ANALYZED);
                    logger.debug("Adding inst:" + property.stringValue());
                    Field classField =
                                    new Field(FreyaConstants.CLASS_FEATURE_LKB, domain
                                                    .stringValue(), Field.Store.YES, Field.Index.ANALYZED);
                    logger.debug("Adding class:" + domain.stringValue());
                    Field predField =
                                    new Field(FreyaConstants.PROPERTY_FEATURE_LKB, domainUri,
                                                    Field.Store.YES, Field.Index.ANALYZED);
                    logger.debug("Adding pred:" + domainUri);
                    doc.add(instField);
                    doc.add(classField);
                    doc.add(predField);
                    writer.addDocument(doc);
                    if (countDocuments % 10000 == 0 && countDocuments!=0)
                        logger.info(" " + countDocuments + " docs ");
                }
                countDocuments++;
            }
            result.close();
        } catch (Exception e) {
            logger.error(e);
        } catch (Throwable e) {
            logger.error(e);
        }
        logger.info("Added:" + countDocuments
                        + " documents (domain and range for properties) to index.");
    }

    /**
     * adds additional statement to the base query
     * 
     * @param baseQuery
     * @param filterURI
     * @return
     */
    String extendQuery(String baseQuery, String filterURI,
                    String stringToBeSearchedInTheBaseQuery, boolean firstIndex) {
        String additionalStatement = "  ?E rdf:type <" + filterURI + "> .";
        // int whereEnd = baseQuery.lastIndexOf("}");
        int whereStart = -1;
        // if (firstIndex){
        whereStart = baseQuery.indexOf(stringToBeSearchedInTheBaseQuery);
        // }
        // else{
        // whereEnd=baseQuery.lastIndexOf(stringToBeSearchedInTheBaseQuery);
        // }
        if (whereStart == -1) {
            throw new RuntimeException("Character: "
                            + stringToBeSearchedInTheBaseQuery + " NOT found in SPARQL:\n"
                            + baseQuery);
        }
        //else
        //    whereStart = whereStart + 1;
        return (baseQuery.substring(0, whereStart)).trim() + "\n" + stringToBeSearchedInTheBaseQuery + "\n"
                        + additionalStatement + baseQuery.substring(whereStart+stringToBeSearchedInTheBaseQuery.length());
    }

    /**
     * @param theRepositoryURLString
     * @param theRepositoryIDString
     */
    public void initRepository(String theRepositoryURLString,
                    String theRepositoryIDString) {
        try {
            //SparqlUtils utils = new SparqlUtils();
            if (theRepositoryURLString.startsWith("http")) {
                if (theRepositoryIDString.equals("useSparqlEndPoint"))
                    theRepositoryIDString = "";
                rep = new HTTPRepository(theRepositoryURLString, theRepositoryIDString);
                rep.initialize();

                logger.info("Finished repository intiialisation.");
            } else {
                // utils.setRepositoryDir(theRepositoryURLString);
                // utils.setRepositoryId(theRepositoryIDString);
                // utils.setPreload(preload);
                // utils.init();
                // loader = utils.getLoader();
                // rep = utils.getLoader().getRepository();
                logger.info("Loading files locally into repository currently not supported.....");
            }
            baseQueryInstances = queryUtil.readQueryContent("/queries/obg/getBaseQueryInstances.sparql");
            baseQueryClassesAndProperties = queryUtil.readQueryContent("/queries/obg/getBaseQueryClassesAndProperties.sparql");
            logger.info("getBaseQueryInstances:\n" + baseQueryInstances);
            logger.info("baseQueryClassesAndProperties:\n"
                            + baseQueryClassesAndProperties);
            filterClassesQuery = queryUtil.readQueryContent("/queries/getAllClasses.sparql");
            getSubClasses = queryUtil.readQueryContent("/queries/getSubClasses.sparql");
            getPropertyDomainAndRange = queryUtil.readQueryContent("/queries/getPropertyDomainAndRange.sparql");
            logger.info("filterClassesQuery:" + filterClassesQuery);


            synMapStream = this.getClass().getResourceAsStream("/WordNet-3.0/dict/prolog/wn_s.pl");
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public Set<String> findFilterUris() {
        Set<String> filterUris = new HashSet<String>();
        TupleQueryResult subjectTupleQueryResult;
        try {
            subjectTupleQueryResult = rep.getConnection().prepareTupleQuery(
                            org.openrdf.query.QueryLanguage.SPARQL,
                            filterClassesQuery).evaluate();

            while (subjectTupleQueryResult.hasNext()) {
                Resource subj =
                                (Resource) subjectTupleQueryResult.next().getValue(
                                                FreyaConstants.VARIABLE_T);
                // System.out.println("Found subject: "+subj);
                String filterClass = subj.toString();
                filterUris.add(filterClass);
            }
            subjectTupleQueryResult.close();
        } catch (QueryEvaluationException e) {
            logger.error(e);
        } catch (RepositoryException e) {
            logger.error(e);
        } catch (MalformedQueryException e) {
            logger.error(e);
        }
        return filterUris;

    }

    /**
     * @return
     */
    static Set<String> getFilterClassAndPropertyUris() {
        Set<String> filterClassAndPropertyUris = new HashSet<String>();
        filterClassAndPropertyUris.add("http://www.w3.org/2002/07/owl#Class");
        filterClassAndPropertyUris
                        .add("http://www.w3.org/2000/01/rdf-schema#Class");
        filterClassAndPropertyUris
                        .add("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property");
        filterClassAndPropertyUris
                        .add("http://www.w3.org/2002/07/owl#DatatypeProperty");
        filterClassAndPropertyUris
                        .add("http://www.w3.org/2002/07/owl#ObjectProperty");
        return filterClassAndPropertyUris;
    }

    /**
     * @param query
     * @return
     */
    private static CloseableIteration executeSingleQuery(String query,
                    RepositoryConnection conn, boolean includeInferred) {
        CloseableIteration<? extends BindingSet, QueryEvaluationException> result =
                        null;
        try {
            long start = System.nanoTime();
            Query preparedQuery = conn.prepareQuery(QueryLanguage.SPARQL, query);

            preparedQuery.setIncludeInferred(includeInferred);
            if (preparedQuery == null) {
                logger.info("Unable to parse query: " + query);
            }

            // ParsedQuery pq = ((SailQuery)preparedQuery).getParsedQuery();
            if (preparedQuery instanceof TupleQuery) {
                TupleQuery q = (TupleQuery) preparedQuery;
                long queryBegin = System.nanoTime();

                result = q.evaluate();
            }

            // SailRepositoryConnection sailRepositoryConnecion =
            // (SailRepositoryConnection)conn;
            // SailConnection sailConnection =
            // sailRepositoryConnecion.getSailConnection();
            //
            //
            // result =
            // sailConnection.evaluate(new SelectQuery(), pq.getDataset(),
            // preparedQuery.getBindings(), true);
//            logger.info("query prepared in " + (System.nanoTime() - start)
//                            / 1000000 + "ms");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }
}
