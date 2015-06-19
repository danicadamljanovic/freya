package org.freya.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.Annotation;
import org.freya.model.ClassElement;
import org.freya.model.Question;
import org.freya.model.SemanticConcept;
import org.freya.model.service.FreyaResponse;
import org.freya.rdf.query.CATConstants;
import org.freya.util.FreyaConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:META-INF/spring/freya-applicationContext.xml"})
@Configurable
public class FreyaServiceHelperTest {

    private static final Log logger = LogFactory.getLog(FreyaServiceHelperTest.class);
    @Autowired FreyaServiceHelper freyaServiceHelper;

    @Test
    public void extractAnnotationsFromQuestionTest() {
        List<Annotation> annotations = new ArrayList<Annotation>();
        Annotation annotation = new Annotation();
        annotation.setText("city");
        HashMap<String, Object> features = new HashMap<String, Object>();
        features.put(CATConstants.FEATURE_URI, "http://www.mooney.net/geo#City");
        features.put(CATConstants.TYPE_FEATURE, "class");
        features.put(FreyaConstants.SCORE, new Float(7.00));
        features.put(FreyaConstants.RESULT_TYPE_STRING, "city");
        annotation.setFeatures(features);
        annotations.add(annotation);
        Question question = new Question();
        List<List<SemanticConcept>> table = question.getSemanticConcepts();
        List<SemanticConcept> list = new ArrayList<SemanticConcept>();
        SemanticConcept concept = new SemanticConcept();
        list.add(concept);
        table.add(list);
        org.freya.model.ui.Annotation ann = new org.freya.model.ui.Annotation();
        ann.setStartOffset(new Long(0));
        ann.setEndOffset(new Long(1));
        ann.setScore(new Double(2.0));
        ClassElement element = new ClassElement();
        element.setAnnotation(annotation);
        concept.setOntologyElement(element);
        FreyaResponse result = freyaServiceHelper.extractAnnotationsFromQuestion(question);
        assertEquals("class", result.getAnnotations().get(0).getType());
        assertEquals("http://www.mooney.net/geo#City", result.getAnnotations().get(0).getUri());
    }
}
