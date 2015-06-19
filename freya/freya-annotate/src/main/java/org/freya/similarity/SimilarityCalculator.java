package org.freya.similarity;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.util.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;

@Component
public class SimilarityCalculator {
    static Log logger = LogFactory.getLog(SimilarityCalculator.class);

    AbstractStringMetric metrics = new MongeElkan();

    AbstractStringMetric soundex =
                    new uk.ac.shef.wit.simmetrics.similaritymetrics.Soundex();

    /* shall the wordnet be used or not */
    boolean useWordnet = true;
    @Autowired WordnetInvoker wordnetInvoker;


    /**
     * compare the two strings
     * 
     * @param text
     * @param niceLabel
     * @return
     */
    public double findSimilarity(String text, String niceLabel) {
        float simBtwStrings = 0;
        float soundexSim = 0;
        int maxNumberOfSynonyms = 3;
        // first find synonym of poc and compare to suggestion
        Set<String> synonyms =
                        wordnetInvoker.getSynonyms(text, maxNumberOfSynonyms);
        int numberOfSynonyms = 0;
        float synonymSim = 0;
        for (String syn : synonyms) {
            float tmpSim = metrics.getSimilarity(syn, niceLabel);
            synonymSim = synonymSim + tmpSim;
            logger.debug("sim(" + niceLabel + ",(synonym)" + syn + ")= " + tmpSim);
            numberOfSynonyms++;
        }
        if (numberOfSynonyms > 0) synonymSim = synonymSim / numberOfSynonyms;
        // then find synonym of suggestion and compare to poc
        Set<String> synonyms2 =
                        wordnetInvoker.getSynonyms(niceLabel, numberOfSynonyms);
        int numberOfSynonyms2 = 0;
        float synonymSim2 = 0;
        for (String syn : synonyms2) {
            float tmpSim = metrics.getSimilarity(syn, text);
            synonymSim2 = synonymSim2 + tmpSim;
            logger.debug("sim(" + text + ",(synonym)" + syn + ")= " + tmpSim);
            numberOfSynonyms2++;
        }
        if (numberOfSynonyms2 > 0) synonymSim2 = synonymSim2 / numberOfSynonyms2;
        simBtwStrings = metrics.getSimilarity(text, niceLabel);
        soundexSim = soundex.getSimilarity(text, niceLabel);
        double totalSimilarity =
                        NumberUtils.roundTwoDecimals(0.45 * simBtwStrings + 0.15
                                        * (soundexSim) + 0.2 * synonymSim + 0.2 * synonymSim2);
        logger.debug("TotalSim(" + niceLabel + "," + text + ")=" + totalSimilarity
                        + "(" + " MongeElkan:" + simBtwStrings + " Soundex:" + soundexSim
                        + " Wordnet:" + synonymSim + "+" + synonymSim2 + ")");
        return totalSimilarity;
    }

    /**
     * compare the two strings using MongeElkan and Soundex
     * 
     * @param text
     * @param niceLabel
     * @return
     */
    public double findStringSimilarity(String text, String niceLabel, String similarityTypeName) {
        float simBtwStrings = 0;
        float soundexSim = 0;

        simBtwStrings = metrics.getSimilarity(text, niceLabel);
        soundexSim = soundex.getSimilarity(text, niceLabel);
        double totalSimilarity =
                        NumberUtils.roundTwoDecimals(0.45 * simBtwStrings + 0.15
                                        * (soundexSim));
        logger.debug("TotalSim(" + niceLabel + "," + text + ")=" + totalSimilarity
                        + "(" + " MongeElkan:" + simBtwStrings + " Soundex:" + soundexSim
                        );
        return totalSimilarity;
    }
}
