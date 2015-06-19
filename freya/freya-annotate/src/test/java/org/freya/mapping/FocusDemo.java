package org.freya.mapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;

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
public class FocusDemo {
    
    private static final Log logger = LogFactory.getLog(FocusDemo.class);
    
    @Autowired Mapper mapper;

    @Test
    public void testMapper() throws Exception {

    	File inFile = new File("src/test/resources/FactualQuestions.txt");
    	File outFile = new File("src/test/resources/FactualQuestions-Focus.csv");
    	//load all questions
    	 PrintStream writer = null;
    	 writer = new PrintStream(outFile);
    	try (BufferedReader br = new BufferedReader(new FileReader(inFile))) {
    	    String line;
    	    while ((line = br.readLine()) != null) {
    	       // process the line.
    	    	Question q = mapper.processQuestionLucene(line, false, null, true);

    	    	String focus = null;
    	    	String focusHead = null;
    	    	if (q.getFocus()!=null){
    	    		focus = q.getFocus().getAnnotation().getText();
    	    		focusHead = q.getFocus().getHead().getAnnotation().getText();
    	    	}
    	    	StringBuffer questionWithFocus = new StringBuffer(line).append(",").append(focus).append(",").append(focusHead);
    	    	writer.println(questionWithFocus.toString());
    	        System.out.println("question:"+line+" focus:"+focus+ " fhead:"+focusHead);
    	    }
    	}
        
        
        
    }
}
