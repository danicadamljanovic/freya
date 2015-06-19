package org.freya.dialogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.learning.LearningModel;
import org.freya.learning.LearningModelHelper;
import org.freya.model.Annotation;
import org.freya.model.NoneElement;
import org.freya.model.POC;
import org.freya.model.Question;
import org.freya.model.SemanticConcept;
import org.freya.model.SuggestionKey;
import org.freya.model.SuggestionPair;
import org.freya.model.Vote;
import org.freya.model.learning.Key;
import org.freya.oc.OCUtil;
import org.freya.util.FreyaConstants;
import org.freya.util.VoteComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DialogManager {
    protected final static Log logger = LogFactory.getLog(DialogManager.class);

    @Autowired OCUtil ocUtil;

    @Autowired SuggestionsCreator suggestionsCreator;

    @Autowired private LearningModelHelper learningModelHelper;
    @Autowired LearningModel learningModel;

    AtomicInteger incrementer = new AtomicInteger();

    /**
     * check whether dialog needs to be modeled or not for any disambiguations (ocs)
     * 
     * @param question
     * @return
     */
    public boolean requiresOCDialog(Question question, boolean forceDialog) {
        boolean result = false;
        List<List<SemanticConcept>> ocs = question.getSemanticConcepts();
        for (List<SemanticConcept> list : ocs) {
            if (list.size() > 1) {
                if (!ocUtil.ocsVerified(list)) {
                    result = true;
                    logger.info("Overlapped OCs found?" + result);
                    return result;
                }
            }
        }
        logger.info("Overlapped OCs found?" + result);
        return result;
    }

    public boolean requiresOCDialog(Question question) {
        return requiresOCDialog(question, false);
    }


    /**
     * checkes whether there are any pocs so that the dialog is generated
     * 
     * @param question
     * @return
     */
    public boolean requiresPOCDialog(Question question) {
        boolean result = false;
        List<POC> pocs = question.getPocs();
        if (pocs.size() > 0) result = true;
        logger.info("Unresolved POCs found?" + result);
        return result;
    }

    /**
     * generate dialog when ocs are overlapped before starting resolvement of pocs
     * 
     * @param question
     */
    public SuggestionPair generateDisambiguationDialog(Question question,
                    boolean forceDialog) {
        SuggestionPair pair = null;
        // which OC here
        long start = System.currentTimeMillis();
        List<SemanticConcept> list = whichOC(question);
        long end = System.currentTimeMillis();
        logger.info("Which OC took:" + (end - start) + "ms.");
        if (list.size() > 1) {
            start = System.currentTimeMillis();
            pair = getOCVotesFromTheLearningModel(list, question, forceDialog);
            end = System.currentTimeMillis();
            logger.info("getOCVotesFromTheLearningModel took:" + (end - start)
                            + "ms.");
        }
        VoteComparator c = new VoteComparator();
        Collections.sort(pair.getVote(), c);
        return pair;
    }

    /**
     * default is that dialog is not forced for oc
     * 
     * @param question
     * @return
     */
    public SuggestionPair generateDisambiguationDialog(Question question) {
        return generateDisambiguationDialog(question, false);
    }

    /**
     * this method generates the sample key from the sample element in the list and then checks whether the learning
     * model contains the pair with that key; if yes, return it, if no, generate the pair with initialized votes and add
     * it to the learning model
     * 
     * @param list
     * @return
     */
    SuggestionPair getOCVotesFromTheLearningModel(List<SemanticConcept> list,
                    Question question, boolean forceDialog) {
        SuggestionKey key = new SuggestionKey();
        SuggestionPair pair = new SuggestionPair();
        SemanticConcept sampleElement = null;
        // the assumption here is that all elements will have the same text and
        // NNeighbours
        if (list != null && list.size() > 0) sampleElement = list.get(0);
        String text;
        if (sampleElement.getOntologyElement().getAnnotation().getText() == null)
            text =
                            "Annotation for an ontology element is null?!!!this shouldnot happen dfkdjf !!";
        else
            text = sampleElement.getOntologyElement().getAnnotation().getText();
        key.setText(text);
        // finding NN for any sample element as these ocs overlap so they
        // are at the same position
        List<SemanticConcept> nearestNeighbours = null;
        long start = System.currentTimeMillis();
        if (sampleElement != null) {
            nearestNeighbours = ocUtil.findNearestNeighbours(question, list);
        }
        long end = System.currentTimeMillis();
        logger.info("Found " + nearestNeighbours.size() + " NNs for "
                        + (end - start) + "ms.");
        key.setNearestNeighbours(nearestNeighbours);
        List<Key> generatedKeys =
                        generateLearningKey(key);
        List<org.freya.model.learning.Vote> learningVotes =
                        findLearningVotes(generatedKeys);
        pair.setKey(key);
        start = System.currentTimeMillis();
        List<Vote> suggestionPairVotes = getInitialVotes(list);
        end = System.currentTimeMillis();
        logger.info("getInitialVotes for " + (end - start) + "ms.");
        // ///////////////////////////////////////////////////////////////////////
        // ovde sada call find suggestions isto kao za poc
        if (forceDialog) {
            start = System.currentTimeMillis();
            suggestionPairVotes.addAll(findAdditionalVotesAsIfThisOCwereAPOC(
                            question, sampleElement, key, list));
            end = System.currentTimeMillis();
            logger.info("findAdditionalVotesAsIfThisOCwereAPOC for " + (end - start)
                            + "ms.");
        }
        // /////////////////////////////////////////////////////////////////////////
        start = System.currentTimeMillis();
        if (learningVotes != null && learningVotes.size() > 0) {
            // take it from the learning model
            suggestionPairVotes =
                            learningModelHelper.getVotesFromLearningVotes(learningVotes, suggestionPairVotes);
            pair.setVote(suggestionPairVotes);
        } else {
            // nothing is found in the learning model
            // use initialized vote and add it to the learning model
            pair.setVote(suggestionPairVotes);
            // save for each element which needs to be disambiguated one
            // learningKey
            for (org.freya.model.learning.Key learningKey : generatedKeys) {
                List<org.freya.model.learning.Vote> lightVotes =
                                learningModelHelper.getLearningVotesFromVotes(suggestionPairVotes);
                learningModel.getVotesMap().put(learningKey.toString(), lightVotes);
            }
        }
        end = System.currentTimeMillis();
        logger.info("gedting votes from the learning model for " + (end - start)
                        + "ms.");
        return pair;
    }

    /**
     * generates additional votes and updates question.getSemanticConcepts so that addiiotnal elemenst are treated as
     * OCs if they are selected
     * 
     * @param question
     * @param sampleElement
     * @param key
     * @return
     */
    List<Vote> findAdditionalVotesAsIfThisOCwereAPOC(Question question,
                    SemanticConcept sampleElement, SuggestionKey key,
                    List<SemanticConcept> existingList) {
        List<Vote> suggestionPairVotes = new ArrayList<Vote>();
        POC poc = new POC();
        Annotation newAnn = new Annotation();
        newAnn
                        .setText(sampleElement.getOntologyElement().getAnnotation()
                                        .getText());
        newAnn.setStartOffset(sampleElement.getOntologyElement().getAnnotation()
                        .getStartOffset());
        newAnn.setEndOffset(sampleElement.getOntologyElement().getAnnotation()
                        .getEndOffset());
        newAnn.setSyntaxTree(sampleElement.getOntologyElement().getAnnotation()
                        .getSyntaxTree());
        poc.setAnnotation(newAnn);
        List<Vote> pocVotes = new ArrayList<Vote>();
        boolean addNone = false;
        int max = 100000;
        if (key.getNearestNeighbours().size() > 0) {
            long start = System.currentTimeMillis();
            pocVotes.addAll(suggestionsCreator.generateVotes(key, poc, addNone,
                            existingList));
            long end = System.currentTimeMillis();
            logger.info("Generate Votes: " + (end - start) + "ms.");
        } else {
            long start = System.currentTimeMillis();
            pocVotes.addAll(suggestionsCreator.generateGenericVotes(key, poc,
                            addNone, max, existingList));
            long end = System.currentTimeMillis();
            logger.info("generateGenericVotes for " + (end - start) + "ms.");
        }
        logger.debug("Set max property size to " + max);
        long start = System.currentTimeMillis();
        List<SemanticConcept> candidates = new ArrayList<SemanticConcept>();
        for (Vote vote : pocVotes) {
            if (!existingList.contains(vote.getCandidate())
                            || !(vote.getCandidate().getOntologyElement() instanceof NoneElement)) {
                // transfer the score
                vote.getCandidate().setScore(vote.getVote());
                // logger
                // .info("Setting vote:"
                // + vote.getVote()
                // + " for "
                // + vote.getCandidate().getOntologyElement().getData()
                // .toString());
                candidates.add(vote.getCandidate());
            }
        }
        suggestionPairVotes.addAll(getInitialVotes(candidates));
        long end = System.currentTimeMillis();
        logger.debug("Generating candidate elements from pocVotes for "
                        + (end - start) + "ms.");
        logger.info("There were:" + existingList.size()
                        + " elements, and we now add " + candidates.size() + " more.");
        existingList.addAll(candidates);
        logger.info("After adding total suggestions for oc dialog: "
                        + existingList.size());
        return suggestionPairVotes;
    }



    /**
     * finds candidate based on the identifier which is either URI or DatatypePropertyValueIdentifier
     * 
     * @param vote
     * @param list
     * @return
     */
    SemanticConcept findCandidate(org.freya.model.learning.Vote vote,
                    List<SemanticConcept> list) {
        SemanticConcept sc = null;
        for (SemanticConcept sConcept : list) {
            if (vote.getIdentifier().equals(sConcept.getOntologyElement().getData())) {
                // bingo
                return sConcept;
            }
        }
        return sc;
    }

    /**
     * generate initial votes for the semantic elements by setting scores to some initial value
     * 
     * @param elements
     * @return
     */
    List<Vote> getInitialVotes(List<SemanticConcept> elements) {
        List<Vote> votes = new ArrayList<Vote>();
        // 1.0 because we want it before any poc suggestion
        double initialVoteScore = 1.0;
        for (SemanticConcept element : elements) {
            Vote vote = new Vote();
            long id = incrementer.incrementAndGet();
            vote.setId(id);
            SemanticConcept clonedElement = (SemanticConcept) element.clone();
            vote.setCandidate(clonedElement);
            if (element.getScore() != null)
                initialVoteScore = element.getScore().doubleValue();
            vote.setVote(initialVoteScore);
            votes.add(vote);
        }
        return votes;
    }

    /**
     * generate dialog for mapping poc to oc
     * 
     * @param question
     * @return
     */
    public SuggestionPair generateMappingPocToOcDialog(Question question) {
        SuggestionPair pair = new SuggestionPair();
        SuggestionKey key = new SuggestionKey();
        logger.info("Generating dialog with POCs (" + question.getPocs().size()
                        + " POCs)");
        // which poc? start with the one which is closest to the
        // existing semantic concept
        POC poc = whichPoc(question);
        logger.info("Decided to use " + poc.getAnnotation().getText()
                        + " for the dialog");
        key.setText(poc.getAnnotation().getText());
        List<SemanticConcept> nearestNeighbours =
                        ocUtil.findNearestNeighbours(question, poc);
        logger.info("Found " + nearestNeighbours.size() + " neighbours.");
        key.setNearestNeighbours(nearestNeighbours);
        pair.setCurrentDialogSubject(poc);
        List<org.freya.model.learning.Key> generatedKeys =
                        generateLearningKey(key);
        List<org.freya.model.learning.Vote> learningVotes =
                        findLearningVotes(generatedKeys);
        List<Vote> votes = null;
        if (nearestNeighbours.size() > 0)
            votes = suggestionsCreator.generateVotes(key, poc);
        else
            votes = suggestionsCreator.generateGenericVotes(key, poc);
        pair.setKey(key);
        if (learningVotes != null) {
            List<Vote> suggestionPairVotes =
                            learningModelHelper.getVotesFromLearningVotes(learningVotes, votes);
            pair.setVote(suggestionPairVotes);
        } else {
            // generate it and update the learning model
            pair.setVote(votes);
            for (org.freya.model.learning.Key learningKey : generatedKeys) {
                List<org.freya.model.learning.Vote> lightVotes =
                                learningModelHelper.getLearningVotesFromVotes(votes);
                learningModel.getVotesMap().put(learningKey.toString(), lightVotes);
            }
        }
        VoteComparator c = new VoteComparator();
        Collections.sort(pair.getVote(), c);
        return pair;
    }

    /**
     * this method identifies which poc should be used first for the dialog: it iterates through all ocs and then finds
     * the poc which is closest to its nearestNeighbours
     * 
     * @param question
     * @return
     */
    public POC whichPoc(Question question) {
        POC poc = ocUtil.whichPOC(question);
        return poc;
    }

    /**
     * @param question
     */
    public List<SemanticConcept> whichOC(Question question) {
        return ocUtil.whichOC(question);
    }

    /**
     * finds the semanticConcept which is equal to dialogSubject and makes the map with key=index of the column in the
     * table and the value=list with only dialog Subject which will not be removed
     * 
     * @param table
     * @param dialogSubject
     * @return
     */
    public HashMap<Integer, List<SemanticConcept>> findSemanticConceptsToRemove(
                    List<List<SemanticConcept>> table, List<SemanticConcept> candidates) {
        HashMap<Integer, List<SemanticConcept>> toRemove =
                        new HashMap<Integer, List<SemanticConcept>>();
        for (SemanticConcept candidate : candidates) {
            for (List<SemanticConcept> column : table) {
                for (SemanticConcept cell : column) {
                    if ((candidate.getOntologyElement() instanceof NoneElement && candidate
                                    .getOntologyElement().equals(cell.getOntologyElement()))
                                    || (!(candidate.getOntologyElement() instanceof NoneElement) && candidate
                                                    .getOntologyElement().getData().toString().equals(
                                                                    cell.getOntologyElement().getData().toString()))) {
                        // mark only the one that will not be removed
                        List<SemanticConcept> list = toRemove.get(new Integer(table.indexOf(column)));
                        if (list == null)
                            list = new ArrayList<SemanticConcept>();
                        // list.addAll(column);
                        // list.remove(cell);
                        list.add(cell);
                        logger.info("Marked " + (column.size() - 1)
                                        + " elements for removal. Thread:"
                                        + Thread.currentThread().getName());
                        toRemove.put(new Integer(table.indexOf(column)), list);
                    }
                }
            }
        }
        return toRemove;
    }


    /**
     * this method generates learning key based on which it is possible to search the learning model
     * 
     * @param key
     * @return
     */
    List<org.freya.model.learning.Key> generateLearningKey(SuggestionKey key) {
        List<org.freya.model.learning.Key> lk =
                        new ArrayList<org.freya.model.learning.Key>();
        for (SemanticConcept sc : key.getNearestNeighbours()) {
            org.freya.model.learning.Key learningKey =
                            new org.freya.model.learning.Key();
            learningKey.setText(key.getText());
            Object genericElement = learningModelHelper.findGenericElement(sc.getOntologyElement());
            learningKey.setOntologyElementIdentifier(genericElement);
            lk.add(learningKey);
        }
        if (key.getNearestNeighbours() == null
                        || key.getNearestNeighbours().size() == 0) {
            org.freya.model.learning.Key learningKey =
                            new org.freya.model.learning.Key();
            learningKey.setText(key.getText());
            learningKey
                            .setOntologyElementIdentifier(FreyaConstants.NONE_NEIGHBOURING_ONTOLOGY_ELEMENT);
            lk.add(learningKey);
        }
        return lk;
    }

    /**
     * searches the learning model to find existing key and votes if they exist
     * 
     * @return
     */
    List<org.freya.model.learning.Vote> findLearningVotes(
                    List<org.freya.model.learning.Key> generatedKeys) {
        List<org.freya.model.learning.Vote> learningVotes =
                        new ArrayList<org.freya.model.learning.Vote>();
        for (org.freya.model.learning.Key k : generatedKeys) {
            List<org.freya.model.learning.Vote> theVotes =
                            ((Map<String, List<org.freya.model.learning.Vote>>) learningModel
                                            .getVotesMap()).get(k.toString());
            Integer num = null;
            if (theVotes != null) num = theVotes.size();
            logger.debug("Found " + num + " votes for " + k.toString());
            if (theVotes != null) learningVotes.addAll(theVotes);
        }
        return learningVotes;
    }
}
