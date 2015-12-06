package org.freya.mapping;

import static org.junit.Assert.assertEquals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.Question;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:META-INF/spring/freya-applicationContext.xml"})
@Configurable
public class MapperTest {
    
    private static final Log logger = LogFactory.getLog(MapperTest.class);
    
    @Autowired Mapper mapper;

    @Test
    public void testMapper() throws Exception {

        Question q = mapper.processQuestion("List cities in california.", false, null, true);

        assertEquals(2, q.getSemanticConcepts().size());
        
        assertEquals("cities", q.getFocus().getAnnotation().getText());
        
    }
}
