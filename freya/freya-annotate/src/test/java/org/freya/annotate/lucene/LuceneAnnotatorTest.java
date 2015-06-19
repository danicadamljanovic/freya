package org.freya.annotate.lucene;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.runner.RunWith;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.Annotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:META-INF/spring/freya-applicationContext.xml"})
@Configurable
public class LuceneAnnotatorTest {
    private static final Log logger = LogFactory.getLog(LuceneAnnotatorTest.class);
    @Autowired LuceneAnnotator luceneAnnotator;

    @Test
    public void testExactMatch() throws Exception {
        Annotation annotation = new Annotation();
        annotation.setText("city");
        List<Annotation> result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(1, result.size());
    }

    @Test
    public void testStemmer() throws Exception {
        Annotation annotation = new Annotation();
        annotation.setText("cities");
        List<Annotation> result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(4, result.size());
    }

    @Test
    public void testInstance() throws Exception {
        Annotation annotation = new Annotation();
        annotation.setText("california");
        List<Annotation> result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(1, result.size());
    }
    
    @Test
    public void testLuceneIndex() throws Exception {

        Annotation annotation = new Annotation();
        annotation.setText("cit");
        List<Annotation> result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(0, result.size());
        System.out.println();
        annotation.setText("city");
        result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(1, result.size());
        System.out.println();
        annotation.setText("cities");
        result = luceneAnnotator.searchIndex(annotation, false);
        // assertEquals(1, result.size());
        assertEquals(4, result.size());
        System.out.println();
        annotation.setText("River");
        result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(1, result.size());
        System.out.println();
        annotation.setText("Rivers");
        result = luceneAnnotator.searchIndex(annotation, false);
        // assertEquals(1, result.size());
        assertEquals(2, result.size());
        System.out.println();
        annotation.setText("mississippi river");
        result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(4, result.size());
        System.out.println();
        annotation.setText("washington");
        result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(2, result.size());
        System.out.println();
        annotation.setText("new york");
        result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(2, result.size());
        System.out.println();
        annotation.setText("border");
        result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(1, result.size());
        System.out.println();
        annotation.setText("bordering");
        result = luceneAnnotator.searchIndex(annotation, false);
        assertEquals(1, result.size());
        String uri1 = "http://www.mooney.net/geo#california";
        String uri2 = "http://www.mooney.net/geo#sacramentoCa";
        List<String> results = luceneAnnotator.findDirectTypes(uri1);
        assertEquals(1, results.size());
        results = luceneAnnotator.findDirectTypes(uri2);
        assertEquals(1, results.size());
        results = luceneAnnotator.findLabels(uri1);
        assertEquals(1, results.size());
        results = luceneAnnotator.findLabels(uri2);
        assertEquals(1, results.size());
        String instanceUri = "http://www.mooney.net/geo#sacramentoCa";
        String propertyURI = "http://purl.org/dc/elements/1.1/title";
        results = luceneAnnotator.findLiteral(instanceUri, propertyURI);
        assertEquals(0, results.size());
    }

    public void onTearDown() {
        // mapper.getLuceneAnnotator().close();
    }
}
