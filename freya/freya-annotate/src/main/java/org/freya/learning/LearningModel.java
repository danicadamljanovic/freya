package org.freya.learning;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.freya.model.learning.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * this class is initialising the learning model from file; on start up it feeds it into the memory and then it saves it
 * at certain periods of time
 * 
 * @author danica
 * 
 */
@Component
public class LearningModel {
    private static final Logger logger = LoggerFactory.getLogger(LearningModel.class);


    Map<String, List<Vote>> votesMap = new ConcurrentHashMap<String, List<Vote>>();

    public Map<String, List<Vote>> getVotesMap() {
        return votesMap;
    }

    public void setVotesMap(Map<String, List<Vote>> votesMap) {
        this.votesMap = votesMap;
    }

    boolean learningEnabled = true;

    public boolean isLearningEnabled() {
        return learningEnabled;
    }

    public void setLearningEnabled(boolean learningEnabled) {
        this.learningEnabled = learningEnabled;
    }

    // String learningModelJsonFilePath;
    ObjectMapper mapper = new ObjectMapper();

    @Value("${org.freya.learning.model}") Resource learningModelFile;

    /**
     * this method is called on startup to load learning model from file if loadModelOnInitialization flat is set to
     * true
     */
    @PostConstruct
    public void loadLearningModelFromFile() {

        if (learningEnabled) {
            // read the path to the file
            try {
                if (learningModelFile.exists()) {
                    // and here's something different: instead of TwitterEntry,
                    // let's claim content is a Map!
                    TypeReference<ConcurrentHashMap<String, List<Vote>>> typeRef =
                                    new TypeReference<ConcurrentHashMap<String, List<Vote>>>() {};

                    Map<String, List<Vote>> votesMap = mapper.readValue(
                                    learningModelFile.getFile(), typeRef);
                    this.setVotesMap(votesMap);
                    logger.info("Learning model loaded from: "
                                    + learningModelFile.getURL().toString());
                } else {
                    logger
                                    .info("Learning model does not exist on the specified location and cannot be loaded: "
                                                    + learningModelFile);
                }
            } catch (JsonParseException e) {
                logger.info(e.getMessage());
            } catch (JsonMappingException e) {
                logger.info(e.getMessage());
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }
    }

    public boolean clear() {
        boolean successful = false;
        setVotesMap(new ConcurrentHashMap<String, List<Vote>>());
        return successful;
    }

    /**
     * saves learning model into the file; if specified file does not exist, generate a new one
     * 
     * @param pairsMap
     */
    public void saveLearningModel() {
        try {
            if (!learningModelFile.exists()) {
                FileUtils.touch(learningModelFile.getFile());
            }
            if (votesMap != null) {
                mapper.writeValue(learningModelFile.getFile(), votesMap);
                logger.debug("Finished writing to the model...");
            }
        } catch (JsonGenerationException e) {
            logger.info(e.getMessage());
        } catch (JsonMappingException e) {
            logger.info(e.getMessage());
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // // TODO Auto-generated method stub
        // SuggestionPair pair = new SuggestionPair();
        //
        // SuggestionKey key = new SuggestionKey();
        // key.setText("cipa");
        // OntologyElement e = new ClassElement();
        // e
        // .setData(new SerializableURI("http://www.mooney.net/geo#State",
        // false));
        // SemanticConcept sc = new SemanticConcept();
        // sc.setOntologyElement(e);
        // sc.setFunction("max");
        // List<SemanticConcept> nearestNeighbours = new
        // ArrayList<SemanticConcept>();
        // nearestNeighbours.add(sc);
        // key.setNearestNeighbours(nearestNeighbours);
        //
        // Vote vote = new Vote();
        // vote.setId(1);
        // vote.setVote(0.2);
        //
        // OntologyElement property = new PropertyElement();
        // e.setData(new SerializableURI(
        // "http://www.mooney.net/geo#statePopulation", false));
        // SemanticConcept candidate = new SemanticConcept();
        // sc.setOntologyElement(property);
        // sc.setFunction("max");
        // vote.setCandidate(candidate);
        // List<Vote> votes = new ArrayList<Vote>();
        // votes.add(vote);
        //
        // pair.setKey(key);
        // pair.setVote(votes);
        // ObjectMapper mapper = new ObjectMapper();
        // try {
        // System.out.println("pair:\n" + pair.toString());
        //
        // List<SuggestionPair> listOfPairs = new ArrayList<SuggestionPair>();
        // listOfPairs.add(pair);
        // listOfPairs.add(pair);
        // listOfPairs.add(pair);
        //
        // String learningModelJsonFilePath;
        //
        // LearningModel lm = new LearningModel();
        // InputStream is = lm.getClass().getResourceAsStream(
        // "/Service.properties");
        // Properties ps = new Properties();
        //
        // ps.load(is);
        //
        // learningModelJsonFilePath = ps.getProperty("learningModelJsonFile");
        // File learningModelFile = new File(learningModelJsonFilePath);
        // mapper.writeValue(learningModelFile, listOfPairs);
        // } catch (JsonGenerationException e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // } catch (JsonMappingException e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // } catch (IOException e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }
    }

}
