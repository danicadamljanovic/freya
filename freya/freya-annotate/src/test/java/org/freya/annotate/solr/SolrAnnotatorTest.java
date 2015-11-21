package org.freya.annotate.solr;

import static org.junit.Assert.assertEquals;

import org.freya.model.Annotation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:META-INF/spring/freya-applicationContext.xml"})
@Configurable
public class SolrAnnotatorTest {

    private static final Logger log = LoggerFactory.getLogger(SolrAnnotator.class);

    @Autowired
    private SolrAnnotator solrAnnotator;

    @Test
    public void testExactMatch() throws Exception {
        Annotation annotation = new Annotation();
        annotation.setText("city");
        List<Annotation> result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(1, result.size());
    }

    @Test
    public void testStemmer() throws Exception {
        Annotation annotation = new Annotation();
        annotation.setText("cities");
        List<Annotation> result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(1, result.size());
    }

    @Test
    public void testInstance() throws Exception {
        Annotation annotation = new Annotation();
        annotation.setText("california");
        List<Annotation> result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(1, result.size());
    }

    @Test
    public void testSolrIndex() throws Exception {
        Annotation annotation = new Annotation();
        annotation.setText("cit");
        List<Annotation> result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(0, result.size());

        annotation.setText("city");
        result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(1, result.size());

        annotation.setText("cities");
        result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(1, result.size());

        annotation.setText("River");
        result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(1, result.size());

        annotation.setText("Rivers");
        result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(2, result.size());

        annotation.setText("mississippi river");
        result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(4, result.size());

        annotation.setText("washington");
        result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(2, result.size());

        annotation.setText("new york");
        result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(2, result.size());

        annotation.setText("border");
        result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(1, result.size());

        annotation.setText("bordering");
        result = solrAnnotator.searchExactContentFirst(annotation);
        assertEquals(1, result.size());

        String uri1 = "http://www.mooney.net/geo#california";
        String uri2 = "http://www.mooney.net/geo#sacramentoCa";
        List<String> results = solrAnnotator.findDirectTypes(uri1);
        assertEquals(1, results.size());

        results = solrAnnotator.findDirectTypes(uri2);
        assertEquals(1, results.size());
        results = solrAnnotator.findLabels(uri1);
        assertEquals(1, results.size());
        results = solrAnnotator.findLabels(uri2);
        assertEquals(1, results.size());

        String instanceUri = "http://www.mooney.net/geo#sacramentoCa";
        String propertyURI = "http://purl.org/dc/elements/1.1/title";
        results = solrAnnotator.findLiteral(instanceUri, propertyURI);
        assertEquals(0, results.size());
    }
}
