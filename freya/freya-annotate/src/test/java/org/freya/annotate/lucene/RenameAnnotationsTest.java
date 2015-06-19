package org.freya.annotate.lucene;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.Annotation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:META-INF/spring/freya-applicationContext.xml"})
@Configurable
public class RenameAnnotationsTest {

    private static final Log logger = LogFactory.getLog(RenameAnnotationsTest.class);
    @Autowired RenameAnnotations rename;

    @Test
    public void testRenameAnnotations() throws Exception {
        List<Annotation> annotations = new ArrayList<Annotation>();
        Annotation annotation = new Annotation();
        annotation.setText("city");
        HashMap<String, Object> features = new HashMap<String, Object>();
        features.put("inst", "http://www.mooney.net/geo#City");
        features.put("pred", null);
        features.put("class", "http://www.w3.org/2002/07/owl#Class");
        features.put("string", "City");
        features.put("score", new Float(7.74));
        annotation.setFeatures(features);
        annotations.add(annotation);
        List<Annotation> result = rename.rename(annotations);
        assertEquals(1, result.size());
    }
    
    @Test
    public void testRenameAnnotationFeatures() throws Exception {
        List<Annotation> annotations = new ArrayList<Annotation>();
        Annotation annotation = new Annotation();
        annotation.setText("city");
        HashMap<String, Object> features = new HashMap<String, Object>();
        features.put("inst", "http://www.mooney.net/geo#City");
        features.put("pred", null);
        features.put("class", "http://www.w3.org/2002/07/owl#Class");
        features.put("string", "City");
        features.put("score", new Float(7.74));
        annotation.setFeatures(features);
        annotations.add(annotation);
        List<Annotation> result = rename.rename(annotations);
        
        Annotation renamedOne = result.get(0);
        String type = (String)renamedOne.getFeatures().get("type");
        String uri = (String)renamedOne.getFeatures().get("URI");
        
        assertEquals("class", type);
        assertEquals("http://www.mooney.net/geo#City", uri);
    }
}
