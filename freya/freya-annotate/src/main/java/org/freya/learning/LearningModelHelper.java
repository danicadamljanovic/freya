package org.freya.learning;

import org.freya.annotate.lucene.LuceneAnnotator;
import org.freya.model.DatatypePropertyValueElement;
import org.freya.model.DatatypePropertyValueIdentifier;
import org.freya.model.InstanceElement;
import org.freya.model.InstanceListElement;
import org.freya.model.OntologyElement;
import org.freya.model.SemanticConcept;
import org.freya.model.SerializableURI;
import org.freya.model.SuggestionPair;
import org.freya.model.Vote;
import org.freya.model.learning.Key;
import org.freya.util.FreyaConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
// import gate.freya.util.FreyaConstants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Component
public class LearningModelHelper {
    protected final Log logger = LogFactory.getLog(getClass());
    
    @Autowired LearningModel learningModel;

    @Autowired LuceneAnnotator luceneAnnotator;


    /**
     * @param pair
     */
    public void updateTheLearningModel(SuggestionPair pair) {
        List<Vote> votes = pair.getVote();
        for (SemanticConcept sc : pair.getKey().getNearestNeighbours()) {
            Key learningKey = new Key();
            learningKey.setText(pair.getKey().getText());
            Object genericElement =
                            findGenericElement(sc.getOntologyElement());
            learningKey.setOntologyElementIdentifier(genericElement);
            List<org.freya.model.learning.Vote> lightVotes =
                            getLearningVotesFromVotes(votes);
            // logger.debug("Updating the learning model (in memory). ");
            learningModel.getVotesMap().put(
                            learningKey.toString(), lightVotes);
        }
        if (pair.getKey().getNearestNeighbours().size() == 0) {
            // still need to generate key and votes
            Key key = new Key();
            key.setText(pair.getKey().getText());
            key
                            .setOntologyElementIdentifier(FreyaConstants.NONE_NEIGHBOURING_ONTOLOGY_ELEMENT);
            List<org.freya.model.learning.Vote> lightVotes =
                            getLearningVotesFromVotes(votes);
            learningModel.getVotesMap().put(key.toString(),
                            lightVotes);
        }
    }

    /**
     * @param element
     * @return
     */
    public Object findGenericElement(OntologyElement element) {
        Object genericElement = null;
        // save class if it is instance
        String classURI = null;
        String uri = null;
        if (element instanceof InstanceElement) {
            classURI = ((InstanceElement) element).getClassURI().toString();
            uri = ((InstanceElement) element).getData().toString();
        } else if (element instanceof InstanceListElement) {
            classURI = ((InstanceListElement) element).getClassURI().toString();
            uri = ((InstanceListElement) element).getData().toString();
        } else if (element instanceof DatatypePropertyValueElement) {
            DatatypePropertyValueIdentifier dtpv =
                            (DatatypePropertyValueIdentifier) element.getData();
            List<SerializableURI> uris = dtpv.getInstanceURIs();
            String sampleUri = uris.get(0).toString();
            classURI = luceneAnnotator.findOneDirectType(sampleUri);
            uri = dtpv.getInstanceURIs().get(0).toString();
        }
        if (element instanceof InstanceElement
                        || element instanceof InstanceListElement
                        || element instanceof DatatypePropertyValueElement) {
            if (classURI != null)
                genericElement = classURI;
            else
                genericElement = uri;
            // else genericElement=FreyaConstants.INSTANCE_WITHOUT_DIRECT_CLASS;
            // TODO find the top classes
        } else
            genericElement = element.getData().toString();
        return genericElement;
    }

    /**
     * @param votes
     * @return
     */
    public List<org.freya.model.learning.Vote> getLearningVotesFromVotes(
                    List<Vote> votes) {
        List<org.freya.model.learning.Vote> lightVotes =
                        new ArrayList<org.freya.model.learning.Vote>();
        for (Vote voteAgain : votes) {
            org.freya.model.learning.Vote newVote =
                            new org.freya.model.learning.Vote();
            newVote.setFunction(voteAgain.getCandidate().getFunction());
            newVote.setId(voteAgain.getId());
            newVote.setIdentifier(voteAgain.getCandidate().getOntologyElement()
                            .getData());
            newVote.setScore(voteAgain.getVote());
            lightVotes.add(newVote);
        }
        return lightVotes;
    }

    /**
     * updates the heavy votes with the scores from the learning ones
     * 
     * @param allVotes
     * @param list
     * @return
     */
    public List<Vote> getVotesFromLearningVotes(
                    List<org.freya.model.learning.Vote> learningVotes,
                    List<Vote> oldVotes) {
        for (org.freya.model.learning.Vote v : learningVotes) {
            // find vote in oldVotes and update the score
            for (Vote oldVote : oldVotes) {
                if (v.getIdentifier().equals(
                                oldVote.getCandidate().getOntologyElement().getData())) {
                    if ((v.getFunction() != null && v.getFunction().equals(
                                    oldVote.getCandidate().getFunction()))
                                    || (v.getFunction() == null || "null".equals(v.getFunction()))) {
                        oldVote.setVote(v.getScore());
                        // logger.debug("Updating the vote's score from:" +
                        // oldVote.getVote()
                        // + " to:" + v.getScore());
                    }
                }
            }
        }
        return oldVotes;
    }

    /**
     * generates 'heavy' votes from the learning onesdddd
     * 
     * @param learningVotes
     * @param sConcepts
     * @return
     */
    List<Vote> getOCVotesFromLearningVotes(
                    List<org.freya.model.learning.Vote> learningVotes,
                    List<SemanticConcept> sConcepts) {
        Iterator iterator = sConcepts.iterator();
        List<Vote> votes = new ArrayList<Vote>();
        for (org.freya.model.learning.Vote v : learningVotes) {
            // find vote in oldVotes and update the score
            Vote vote = new Vote();
            if (iterator.hasNext())
                vote.setCandidate((SemanticConcept) iterator.next());
            vote.setId(v.getId());
            vote.setVote(v.getScore());
            votes.add(vote);
        }
        return votes;
    }
}
