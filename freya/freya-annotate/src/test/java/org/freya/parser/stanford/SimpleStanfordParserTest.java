package org.freya.parser.stanford;
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
public class SimpleStanfordParserTest {
    
    private static final Log logger = LogFactory.getLog(SimpleStanfordParserTest.class);
    
    @Autowired SimpleStanfordParser simpleStanfordParser;

    @Test
    public void testSimpleStanfordParser() throws Exception {

        Question q = simpleStanfordParser.parseQuestion("List cities in california.");
        assertEquals(2, q.getPocs().size());
    }
}