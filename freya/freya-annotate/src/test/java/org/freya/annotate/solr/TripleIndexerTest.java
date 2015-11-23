package org.freya.annotate.solr;

import org.freya.index.solr.TripleIndexer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:META-INF/spring/freya-applicationContext.xml"})
@Configurable
public class TripleIndexerTest {

    private static final Logger log = LoggerFactory.getLogger(SolrAnnotator.class);

    @Autowired
    private TripleIndexer indexer;

    @Test
    public void testIndexAll() throws Exception {
    	indexer.clearNotThreadSafe();
    	indexer.indexAllNotThreadSafe();
    }
    
}
