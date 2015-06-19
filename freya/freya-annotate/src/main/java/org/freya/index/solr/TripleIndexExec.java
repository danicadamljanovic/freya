package org.freya.index.solr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TripleIndexExec {
    static Logger logger = LoggerFactory.getLogger(TripleIndexExec.class);
    public static void main(String[] args) {
        String appContextXml = "classpath:META-INF/spring/freya-applicationContext.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(appContextXml);
        TripleIndexer indexer = context.getBean(TripleIndexer.class);

        logger.info("Started indexing");
        indexer.indexAll();
        logger.info("Finished indexing");
    }
}
