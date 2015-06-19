package org.freya.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.NoneElement;
import org.freya.model.OntologyElement;
import org.freya.model.POC;
import org.freya.model.PropertyElement;
import org.freya.model.Question;
import org.freya.model.SemanticConcept;
import org.freya.oc.OCUtil;
import org.freya.util.FreyaConstants;
import org.freya.util.SemanticConceptListComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Consolidator {
    @Autowired OCUtil ocUtil;


    Log logger = LogFactory.getLog(Consolidator.class);

    /**
     * find the answer type
     * 
     * @param question
     * @return
     */
    public Question consolidateTheAnswerType(Question question) {
        POC focus = question.getFocus();
        POC headOfTheFocus = null;
        if (focus != null) headOfTheFocus = focus.getHead();
        List<OntologyElement> foc = new ArrayList<OntologyElement>();
        try {
            List<List<SemanticConcept>> sConceptsTable = question.getSemanticConcepts();
            SemanticConceptListComparator scc = new SemanticConceptListComparator();
            Collections.sort(sConceptsTable, scc);
            for (List<SemanticConcept> columnConcept : sConceptsTable) {
                if (columnConcept.get(0).getOntologyElement() instanceof NoneElement) {
                    continue;
                } else {
                    for (SemanticConcept concept : columnConcept) {
                        foc.add(concept.getOntologyElement());
                    }
                    break;
                }
            }
            if (focus != null)
                logger.info("Head of the focus:" + headOfTheFocus.getAnnotation().toString());
            OntologyElement focSample = null;
            if (foc != null && foc.size() > 0) {
                focSample = foc.get(0);
                logger.info("FOC:" + focSample.getAnnotation().toString());
            } else
                logger.info("No FOC found!!!!!!!!!!!!!!!!!!!!");
            List<OntologyElement> answerType = new ArrayList<OntologyElement>();
            // perfect match: any oc (not foc!) and head of the focus overlap
            for (List<SemanticConcept> columnConcept : sConceptsTable) {
                if (!(columnConcept.get(0).getOntologyElement() instanceof NoneElement)
                                && focus != null
                                && headOfTheFocus.getAnnotation().equals(
                                                columnConcept.get(0).getOntologyElement()
                                                                .getAnnotation())) {
                    for (SemanticConcept concept : columnConcept) {
                        answerType.add(concept.getOntologyElement());
                        concept.getOntologyElement().setMainSubject(true);
                    }
                    question.setAnswerType(answerType);
                    logger.info("Perfect match: OC and head of the focus!");
                    break;
                }
            }
            if (answerType.size() == 0) {
                // if(headOfTheFocus != null
                // && headOfTheFocus.getAnnotation().equals(
                // focSample.getAnnotation())) {
                // answerType = foc;
                // for(OntologyElement focElement : foc) {
                // focElement.setMainSubject(true);
                // }
                // question.setAnswerType(answerType);
                // } else
                if (focus != null
                                && focus.getAnnotation() != null
                                && focSample != null
                                && focSample.getAnnotation() != null
                                && focus.getHead() != null
                                && focSample.getAnnotation().getStartOffset() >= focus.getHead()
                                                .getAnnotation().getEndOffset()) {
                    answerType = foc;
                    for (OntologyElement focElement : foc) {
                        focElement.setMainSubject(true);
                    }
                    question.setAnswerType(answerType);
                    logger.info("FOC is after the focus...needs dialog to identify the answer type...");
                } else if (focus != null
                                && focus.getAnnotation() != null
                                && focSample != null
                                && focus.getHead() != null
                                && focus.getHead().getAnnotation() != null
                                && focSample.getAnnotation().getEndOffset() <= focus.getHead()
                                                .getAnnotation().getStartOffset()) {
                    // if the foc is modifier of the existing element (is of type
                    // property and hasGovernor!=null then the answer type is not
                    // changed!!!)
                    if (foc instanceof PropertyElement
                                    && ((PropertyElement) foc).getGovernor() != null) {
                        // do not change the answer type as this is property.....!!!
                        logger.info("FOC is before the head of the focus HOWEVER it is a property with governor!=null and therefore it cannot become the answer type!");
                    } else {
                        answerType = foc;
                        for (OntologyElement focElement : foc) {
                            focElement.setMainSubject(true);
                        }
                        question.setAnswerType(answerType);
                        logger.info("FOC is before the head of the focus (and it is not property with governor!=null) and thefefore it becomes the answer type!");
                    }
                }
            }

            if (question.getAnswerType() == null) question.setAnswerType(foc);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // question = removeOntologyConceptsBasedOnMainSubjectPriority(question);
        return question;
    }

    /**
     * This method checks if mainsubject's priority is max, if yes, remove ontology concepts which are overlapped
     * 
     * @param query
     * @param mainSubject
     * @return
     */
    public Question removeOntologyConceptsBasedOnMainSubjectPriority(Question question) {
        POC focus = question.getFocus();
        String focusString = null;
        if (focus != null && focus.getAnnotation().getSyntaxTree() != null)
            focusString = focus.getAnnotation().getSyntaxTree().toString();
        logger.info("Question focus:" + focusString);
        List<List<SemanticConcept>> sConceptsTable = null;
        try {
            sConceptsTable = question.getSemanticConcepts();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // this map holds key: column and value the row of the element in the
        // selected column
        Map<Integer, Integer> toRemove = new HashMap<Integer, Integer>();
        for (List<SemanticConcept> sConceptList : sConceptsTable) {
            for (SemanticConcept sConcept : sConceptList) {
                if (focus != null
                                && focus.getMainSubject() != null
                                && focus.getMainSubject().getPriority() != null
                                && focus.getMainSubject().getPriority().intValue() == FreyaConstants.MAIN_SUBJECT_PRIORITY_MAX) {
                    logger.debug("Main subject has max priority, removing ontology element (probably question of type how long, where, when, who)...");
                    // remove ontology concept which is overlapped with the main
                    // Subject
                    OntologyElement el = sConcept.getOntologyElement();
                    if (overlaps(focus, el)) {
                        logger.debug("bingo bingo: main subject overlaps with oc");
                        int col = sConceptsTable.indexOf(sConceptList);
                        int row = sConceptList.indexOf(sConcept);
                        toRemove.put(new Integer(col), new Integer(row));
                        // logger.info("col:" + col + " row: " + row);
                    }
                } else {
                    // logger.info("focus was null...");
                }
            }
        }
        Integer col = null;
        Iterator it = toRemove.keySet().iterator();
        if (it.hasNext()) col = (Integer) it.next();
        Integer row = null;
        if (col != null) row = toRemove.get(col);
        if (row != null) {
            // here we need to remove all raws within the col because it
            // overalps with the main subject we delete all!
            sConceptsTable.remove(col.intValue());
            logger.debug("After removing OCs due to priritized main subject:"
                            + sConceptsTable.toString());
        } else {
            logger.debug("No OCs removed after consolidation of prioritised main subject...");
        }
        question.setSemanticConcepts(sConceptsTable);
        return question;
    }

    /**
     * checks whether the main subject overlaps with the ontology concept
     * 
     * @param question
     * @param mainSubject
     * @param el
     * @return
     */
    public boolean overlaps(POC focus, OntologyElement el) {
        if (focus == null || el == null) return false;
        if (focus != null && focus.getAnnotation().getText() == null) return false;
        Long mainSubjectStartOffset = focus.getAnnotation().getStartOffset();
        Long mainSubjectEndOffset = focus.getAnnotation().getEndOffset();
        Long startOffset = el.getAnnotation().getStartOffset();
        Long endOffset = el.getAnnotation().getEndOffset();
        if (mainSubjectStartOffset == null || mainSubjectEndOffset == null || startOffset == null
                        || endOffset == null) {
            // logger.info("an offset is null...which one?");
            return false;
        }
        if (endOffset.longValue() == mainSubjectEndOffset.longValue()
                        && startOffset.longValue() == mainSubjectStartOffset.longValue()) {
            // logger
            // .info("endOffset.longValue() < mainSubjectStartOffset.longValue()");
            return true;
        } else {
            //
            // logger
            // .info("startOffset.longValue() > mainSubjectEndOffset.longValue()");
            return false;
        }
        //
        // logger
        // .info("main subject overlaps with oc***************************************************bingo!!!!");
    }// overlaps
}
