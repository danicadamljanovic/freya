package org.freya.parser.stanford;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

@Component
public class StanfordSentenceSplitter {
   
    StanfordCoreNLP pipeline;

    @PostConstruct
    void init() {
        Properties props = new Properties();
        // props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.put("annotators", "tokenize, ssplit");
        pipeline = new StanfordCoreNLP(props);
    }

    public List<String> split(String input) {
        List<String> allSentences = new ArrayList<String>();
        Annotation document = new Annotation(input);
        pipeline.annotate(document);
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            allSentences.add(sentence.toString());
        }
        return allSentences;
    }
}
