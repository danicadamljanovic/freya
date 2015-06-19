package org.freya.index.lucene;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
// import org.apache.lucene.wordnet.AnalyzerUtil;
// import org.apache.lucene.wordnet.SynonymMap;
import org.freya.util.FreyaConstants;
import org.openrdf.repository.RepositoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class IndexTriplesExec {

    @Autowired IndexTriples indexTriples;
    private static final Log logger = LogFactory.getLog(IndexTriplesExec.class);

    public static void main(String[] args) {

        long mainStart = System.currentTimeMillis();

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                        new String[] {
                                        "classpath*:META-INF/spring/freya-applicationContext.xml"
                        });


        IndexTriples indexTriples = context.getBean(IndexTriples.class);

        boolean includeInferred = false;

        String usage =
                        "org.freya.index.lucene.IndexTriplesExec <repURL> <repID> "
                                        + "<directory_where_index_will_be_stored> <optional:create index=true or false, default=true>";
        if (args.length < 3) {

            System.out.println("System indexing using defaults.");
            System.err.println("To overide provide arguments: " + usage);
            // System.exit(1);
        }
        String theRepositoryIDString = null;
        String theRepositoryURLString = null;

        if (args.length > 0)
            theRepositoryURLString = args[0];
        else
            theRepositoryURLString = indexTriples.repositoryURL;

        if (args.length > 1)
            theRepositoryIDString = args[1];
        else
            theRepositoryIDString = indexTriples.repositoryId;

        if (args.length > 2)
            indexTriples.INDEX_DIR = new File(args[2]);

        else {
            try {
                indexTriples.INDEX_DIR = indexTriples.luceneIndexDir.getFile();
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }

        if (args.length > 3) indexTriples.create = new Boolean(args[3]);
        if (args.length > 4) indexTriples.indexInstancesOnly = new Boolean(args[4]);
        // indexing class and property uris
        Set<String> filterClassAndPropertyUris = indexTriples.getFilterClassAndPropertyUris();
        // for indexing instances
        Set<String> filterUris = new HashSet<String>();
        try {
            indexTriples.init(theRepositoryURLString, theRepositoryIDString);

            long start = System.currentTimeMillis();
            filterUris = indexTriples.findFilterUris();
            long end = System.currentTimeMillis();
            logger.info("Filter classes (" + filterUris.size() + ") for " + (end - start) + " ms.");


            IndexWriterConfig iwc = setUpLucene(indexTriples);

            IndexWriter writer =
                            new IndexWriter(FSDirectory.open(indexTriples.INDEX_DIR), iwc);
            logger.info("Indexing to directory '" + indexTriples.INDEX_DIR
                            + "'...create new index=" + indexTriples.create);

            start = System.currentTimeMillis();
            if (!indexTriples.indexInstancesOnly)
                indexTriples.makeIndex(writer, indexTriples.baseQueryClassesAndProperties,
                                filterClassAndPropertyUris, true, indexTriples, true, "{", false, includeInferred);

            end = System.currentTimeMillis();
            logger.info("Indexed classes and properties for " + (end - start) + " ms.");

            start = System.currentTimeMillis();
            indexTriples.makeIndex(writer, indexTriples.baseQueryInstances, filterUris, false, indexTriples,
                            true, "WHERE {", true, includeInferred);
                           // true, "} FILTER isLiteral(?L).}", true, includeInferred);

            end = System.currentTimeMillis();
            logger.info("Index instances for " + (end - start) + " ms.");

            indexTriples.addSubClassesToIndex(writer);// with reasoning?

            indexTriples.addPropertyRangeAndDomainToIndex(writer);// with reasoning?
            writer.close();
            end = System.currentTimeMillis();
            logger.info("Indexing finished in " + (end - mainStart)
                            + " total milliseconds");
            
        } catch (CorruptIndexException e) {
            logger.info(e.getMessage());
        } catch (LockObtainFailedException e) {
            logger.info(e.getMessage());
        } catch (IOException e) {
            logger.info(e.getMessage());
        } catch (Exception e) {
            logger.info(e.getMessage());
        } finally {
            try {
                logger.info("About to close the connection to the repository...");
                indexTriples.rep.getConnection().close();
            } catch (RepositoryException e) {
                logger.info(e.getMessage());
            }
            System.exit(0);
        }
    }

    /**
     * set up analyzers etc
     * 
     * @param indexTriples
     * @return
     */
    static IndexWriterConfig setUpLucene(IndexTriples indexTriples) {
        int maxSynonyms = 0;
        // SynonymMap synonymMap = null;
        KeywordAnalyzer kAnalyser = new KeywordAnalyzer();
        EnglishAnalyzer stemAnalyser = new EnglishAnalyzer(Version.LUCENE_45);

        // try {
        // synonymMap = new SynonymMap(indexTriples.synMapStream);
        // } catch (IOException e) {
        // logger.info(e.getMessage());
        // }
        // Analyzer stemAnalyser = AnalyzerUtil.getSynonymAnalyzer(AnalyzerUtil
        // .getPorterStemmerAnalyzer(new KeywordAnalyzer()), synonymMap, maxSynonyms);


        Map<String, Analyzer> mapOfAnalyzers = new HashMap<String, Analyzer>();
        mapOfAnalyzers.put(FreyaConstants.FIELD_EXACT_CONTENT, kAnalyser);
        mapOfAnalyzers.put(FreyaConstants.FIELD_EXACT_LOWERCASED_CONTENT, kAnalyser);
        mapOfAnalyzers.put(FreyaConstants.INST_FEATURE_LKB, kAnalyser);
        mapOfAnalyzers.put(FreyaConstants.PROPERTY_FEATURE_LKB, kAnalyser);
        mapOfAnalyzers.put(FreyaConstants.CLASS_FEATURE_LKB, kAnalyser);

        // danica to do:uncommet this and make it work with stemmer and synonyms!
        mapOfAnalyzers.put(FreyaConstants.FIELD_STEMMED_CONTENT, stemAnalyser);
        PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(kAnalyser, mapOfAnalyzers);
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_45, wrapper);
        iwc.setRAMBufferSizeMB(1024);
        if (indexTriples.create) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            iwc.setOpenMode(OpenMode.CREATE);
        } else {
            // Add new documents to an existing index:
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }
        return iwc;
    }

}
