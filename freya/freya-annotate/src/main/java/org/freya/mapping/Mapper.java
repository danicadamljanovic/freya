package org.freya.mapping;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Trees;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.annotate.lucene.RenameAnnotations;
import org.freya.annotate.solr.SolrAnnotator;
import org.freya.exe.StanfordExecutor;
import org.freya.model.Annotation;
import org.freya.model.POC;
import org.freya.model.Question;
import org.freya.model.SemanticConcept;
import org.freya.oc.OCCreator;
import org.freya.oc.OCUtil;
import org.freya.parser.stanford.TreeUtils;
import org.freya.util.FreyaConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * this class communicates with stanford executor and query (gate) executor and mappes results of one to the other (pocs
 * to ocs)
 * 
 * @author danica
 */
@Component
public class Mapper {
    private static final Log logger = LogFactory.getLog(Mapper.class);

    @Autowired
    private SolrAnnotator solrAnnotator;

    //@Autowired
    //private LuceneAnnotator luceneAnnotator;

    @Autowired
    private RenameAnnotations renameAnnotations;

    @Autowired
    private StanfordExecutor stanfordExecutor;

    @Autowired
    private OCCreator ocCreator;

    @Autowired
    private Consolidator consolidator;

    @Autowired
    private OCUtil ocUtil;

    @Autowired
    private TreeUtils treeUtils;

    /**
     * @param text
     * @param forceDialog
     * @param forceDialogThreshold
     * @param preferLonger
     * @return
     */
    public Question processQuestionLucene(String text, boolean forceDialog,
                    Long forceDialogThreshold, boolean preferLonger) {
        return processQuestionLucene(text, forceDialog, forceDialogThreshold, preferLonger,
                        forceDialog);
    }

    /**
     * this is the initial query processing
     * 
     * @param text
     * @param forceDialog
     * @param forceDialogThreshold
     * @param preferLonger
     * @return
     */
    public Question processQuestionLucene(String text, boolean forceDialog,
                    Long forceDialogThreshold, boolean preferLonger, boolean addNoneToOCDialog) {
        Question question = new Question();
        // get POCs start
        long startTime = System.currentTimeMillis();
        question = stanfordExecutor.findPocs(text);
        question = stanfordExecutor.getStanfordParser().findQuestionType(question);
        long endTime = System.currentTimeMillis();
        logger.info("Found " + question.getPocs().size() + " POCs:" + question.getPocs().toString()
                        + " for " + (endTime - startTime) + " ms.");
        // get POCs end
        // get OCs start
        startTime = System.currentTimeMillis();
        List<Annotation> lookupsList =
                        fromTokensToLuceneLookupAnnotations(question.getTokens(),
                                        question.getSyntaxTree(), preferLonger);


        // /filter instance class or class instance
        long start = System.currentTimeMillis();
        Set<Annotation> toRemove = ocUtil.filterInstanceClass(lookupsList);
        logger.info("To remove due to automatic disambiguation:" + toRemove.size());
        // ///////////////////////////////////////////////////
        // //////////move this somewhere else start ////////////////
        Set<POC> pocsToRemove = new HashSet<POC>();
        if (toRemove.size() > 0) {
            lookupsList.removeAll(toRemove);
            // also remove POCs with the same startofset, endoffset and string
            List<POC> pocs = question.getPocs();
            for (POC poc : pocs) {
                for (Annotation ocAnnotation : toRemove) {
                    if (poc.getAnnotation().equals(ocAnnotation)) {
                        // if (poc.getAnnotation().overlaps(ocAnnotation)){
                        pocsToRemove.add(poc);
                    } else if (poc.getAnnotation().getStartOffset().longValue() == ocAnnotation
                                    .getStartOffset().longValue()) {
                        pocsToRemove.add(poc);
                    } else if (poc.getAnnotation().getEndOffset().longValue() == ocAnnotation
                                    .getEndOffset().longValue()) {
                        pocsToRemove.add(poc);
                    }
                }
            }

        }
        // ///////////////////////////////////////////////////
        // //////////move this somewhere else end ////////////////

        question.getPocs().removeAll(pocsToRemove);

        long end = System.currentTimeMillis();
        long runtime = end - start;
        logger.info("filterInstanceClass for " + runtime + " ms. There are " + lookupsList.size()
                        + " annotations.");

        // Set<Annotation> lookups = null;
        endTime = System.currentTimeMillis();
        if (lookupsList != null)
            logger.info("Generated " + lookupsList.size() + " lucene lookup annotatioons for "
                            + (endTime - startTime) + " ms.  " + " These are:\n"
                            + lookupsList.toString());
        // get OCs END
        // consolidate POC and OC into Semantic Concepts START
        startTime = System.currentTimeMillis();
        List<List<SemanticConcept>> semanticConcepts =
                        ocCreator.getSemanticConcepts(new HashSet(lookupsList), question);
        endTime = System.currentTimeMillis();
        logger.info("Generated " + semanticConcepts.size()
                        + " column with semantic concepts from Lookup annotatioons for "
                        + (endTime - startTime) + " ms.");
        // consolidate POC and OC into Semantic Concepts END
        // adding now None concept to all
        if (addNoneToOCDialog) {
            semanticConcepts = ocUtil.addNoneToAll(semanticConcepts);
        }
        //logger.info("Finshed adding none.");
        question.setSemanticConcepts(semanticConcepts);
        // remove now wrb, make separate pocs for jjs etc...
        startTime = System.currentTimeMillis();
        question = stanfordExecutor.getStanfordParser().findFocus(question);
        question = stanfordExecutor.getStanfordParser().cleanPocsLucene(question);
        endTime = System.currentTimeMillis();
        logger.info("Cleaned POCs (removed WRBs etc) for " + (endTime - startTime) + " ms.");
        // remove WRBs in how many, which state etc
        question = stanfordExecutor.removePocsToIgnore(question);
        logger.info("Removed POCs to ignore.");
        // check priority of the main subject and remove ocs if they overlap
        // with priritized main subject such as how long;
        startTime = System.currentTimeMillis();
        question = consolidator.removeOntologyConceptsBasedOnMainSubjectPriority(question);
        endTime = System.currentTimeMillis();
        logger.debug("removeOntologyConceptsBasedOnMainSubjectPriority for "
                        + (endTime - startTime) + " ms.");
        // remove POCs if OCs exist over the same span
        startTime = System.currentTimeMillis();
        question = resolvePocsWhichOverlapWithOcsLucene(question);
        logger.info("Current pocs:" + question.getPocs());
        endTime = System.currentTimeMillis();
        logger.info("Resolved POC which overlap with OCs for " + (endTime - startTime) + " ms.");
        return question;
    }

 
    /**
     * checks whether poc spans over oc
     * 
     * @param poc
     * @param oc
     * @return
     */
    private boolean pocSpansOverOC(Annotation poc, Annotation oc) {
        boolean pocSpansOverOC = false;
        if ((oc.getEndOffset().longValue() <= poc.getEndOffset().longValue())
                        && oc.getStartOffset().longValue() >= poc.getStartOffset().longValue()) {
            pocSpansOverOC = true;
        }
        return pocSpansOverOC;
    }

    /**
     * oc spans over poc
     * 
     * @param poc
     * @param oc
     * @return
     */
    private boolean ocSpansOverPoc(Annotation poc, Annotation oc) {
        boolean ocSpansOverPOC = false;
        if ((poc.getEndOffset().longValue() <= oc.getEndOffset().longValue())
                        && poc.getStartOffset().longValue() >= oc.getStartOffset().longValue()) {
            ocSpansOverPOC = true;
        }
        return ocSpansOverPOC;
    }

    /**
     * this method checks whether the poc overlaps over any ocs and returns the list of list of ocs over which the poc
     * overlaps
     * 
     * @param poc
     * @param table
     * @return
     */
    List<List<SemanticConcept>> pocAndOcOverlap(POC poc, List<List<SemanticConcept>> table) {
        List<List<SemanticConcept>> newTable = new ArrayList<List<SemanticConcept>>();
        long realStartOfThePoc = poc.getAnnotation().getStartOffset().longValue();
        long realEndOfThePoc = poc.getAnnotation().getEndOffset().longValue();
        boolean pocMatched = false;
        for (int i = 0; i < table.size(); i++) {
            List<SemanticConcept> column = table.get(i);
            List<SemanticConcept> newColumn = new ArrayList<SemanticConcept>();
            for (int j = 0; j < column.size(); j++) {
                SemanticConcept sConcept = column.get(j);
                // here get the first one, it does not really matter as start offset and
                // endoffset should be equal
                long realStartOfTheOC =
                                sConcept.getOntologyElement().getAnnotation().getStartOffset()
                                                .longValue();
                long realEndOfTheOC =
                                sConcept.getOntologyElement().getAnnotation().getEndOffset()
                                                .longValue();
                // logger.debug("Started checking OC:"
                // + sConcept.getOntologyElement().getAnnotation());
                if (realStartOfThePoc == realStartOfTheOC) {
                    // logger.debug("The start offset of OC matches with that of POC.");
                    if (!newColumn.contains(sConcept)) newColumn.add(sConcept);
                    if (realEndOfThePoc == realEndOfTheOC) {
                        // logger.debug(" The end is also matched...(OC within the POC)...");
                        pocMatched = true;
                        break;
                    } else {
                        // take all remaining elements and iterate through them
                        // in order to check whether the end of one of them
                        // matches with that of poc
                        // logger
                        // .debug("The end was NOT matched; iterating through the remaining elements in the table...");
                        List<List<SemanticConcept>> remainingTable =
                                        new ArrayList<List<SemanticConcept>>();
                        for (int k = i + 1; k < table.size(); k++) {
                            remainingTable.add(table.get(k));
                        }
                        boolean matched = false;
                        for (List<SemanticConcept> col : remainingTable) {
                            List<SemanticConcept> nextColumn = new ArrayList<SemanticConcept>();
                            for (SemanticConcept sc : col) {
                                if (!nextColumn.contains(sc)) nextColumn.add(sc);
                                long endOfTheOC =
                                                sc.getOntologyElement().getAnnotation()
                                                                .getEndOffset().longValue();
                                if (realEndOfThePoc == endOfTheOC) {
                                    // logger.debug("Found OC within the POC.");
                                    matched = true;
                                    pocMatched = true;
                                    break;
                                }
                            }
                            if (nextColumn.size() > 0) newTable.add(nextColumn);
                            // logger
                            // .debug("Adding next column to the list of verified OC elements:"
                            // + nextColumn.toString());
                            if (matched) break;
                        }
                        break;
                    }
                } else {
                    continue;
                }
            }
            if (newColumn.size() > 0) newTable.add(newColumn);
            // logger.debug("Adding this column with verified ocs:"
            // + newColumn.toString());
            if (pocMatched) break;
        }
        return newTable;
    }

    /**
     * lucene checks whether poc overlaps with oc if yes, sConcept.verified=true and poc is removed from the list this
     * method also adds tree to the ontology concepts!!! TODO: find trees for ontology concepts!! it also assigns a main
     * subject from the poc to the semantic concept
     * 
     * @param ocs
     * @return
     */
    public Question resolvePocsWhichOverlapWithOcsLucene(Question question) {
        logger.debug("Checking whether any POC is overlaped with OC so that we can remove it...");
        List<List<SemanticConcept>> table = question.getSemanticConcepts();
        // check here which pocs overlap with ocs
        Set<POC> pocsToRemove = new HashSet<POC>();
        Set<POC> pocsToAdd = new HashSet<POC>();
        List<POC> pocs = question.getPocs();
        // logger.debug(pocs.size() + " pocs...starting verification...");
        for (POC poc : pocs) {
            // logger.debug("Checking poc:" + poc.toString());
            // logger
            // .debug("Above poc needs to be matched against the following ocs in the table:"
            // + table.toString());
            for (int i = 0; i < table.size(); i++) {
                List<SemanticConcept> sList = table.get(i);
                for (int j = 0; j < sList.size(); j++) {
                    SemanticConcept sConcept = sList.get(j);
                    boolean pocSpansOverOC =
                                    pocSpansOverOC(poc.getAnnotation(), sConcept
                                                    .getOntologyElement().getAnnotation());
                    // logger.debug("pocSpansOverOC:" + pocSpansOverOC);
                    boolean ocSpansOverPOC =
                                    ocSpansOverPoc(poc.getAnnotation(), sConcept
                                                    .getOntologyElement().getAnnotation());
                    // logger.debug("ocSpansOverPOC:" + ocSpansOverPOC);
                    List<List<SemanticConcept>> sConceptsToBeVerifiedTable =
                                    pocAndOcOverlap(poc, table);
                    // logger.debug("sConceptsToBeVerifiedTable:"
                    // + sConceptsToBeVerifiedTable.toString());
                    if ((sConceptsToBeVerifiedTable != null && sConceptsToBeVerifiedTable.size() > 0)
                                    || ocSpansOverPOC) {
                        for (List<SemanticConcept> sConceptsToBeVerified : sConceptsToBeVerifiedTable) {
                            for (SemanticConcept vConcept : sConceptsToBeVerified) {
                                // logger.debug("Verified OC "
                                // + vConcept.getOntologyElement().getData().toString());
                                // vConcept.setVerified(true);
                                // TODO:check if this is ok!!!
                                vConcept.getOntologyElement().getAnnotation()
                                                .setSyntaxTree(poc.getAnnotation().getSyntaxTree());
                                if (poc.getMainSubject() != null) {
                                    vConcept.getOntologyElement().setMainSubject(true);
                                }
                            }
                        }
                        logger.debug("POC marked for removal: "
                                        + poc.getAnnotation().getSyntaxTree());
                        pocsToRemove.add(poc);
                    } else if (pocSpansOverOC) {
                        // change the start and end annotation sets of the
                        // poc
                        // and verify the contained oc
                        // delete old poc
                        if (j > 0) {
                            // dont generate new poc...
                        } else {
                            // add new one
                            POC newPOC = new POC();
                            // TODO: make this more generic
                            // at the moment, we assume that the oc to be
                            // verified
                            // is always on the right hand side
                            newPOC.getAnnotation().setStartOffset(
                                            poc.getAnnotation().getStartOffset());
                            Long newEnd =
                                            sConcept.getOntologyElement().getAnnotation()
                                                            .getStartOffset() - 1;
                            newPOC.getAnnotation().setEndOffset(newEnd);
                            int ocSize =
                                            sConcept.getOntologyElement().getAnnotation().getText()
                                                            .length();
                            logger.debug("ocSize:" + ocSize);
                            int endPoc = poc.getAnnotation().getText().length() - ocSize;
                            logger.debug("endPoc:" + endPoc);
                            String newText = poc.getAnnotation().getText().substring(0, endPoc - 1);
                            newPOC.getAnnotation().setText(newText);
                            // TODO the tree should be
                            // checked!!!!!!!!!!!!!!!!!!!!!!!!!
                            List<Tree> ontologyConceptTrees =
                                            sConcept.getOntologyElement().getAnnotation()
                                                            .getSyntaxTree();
                            List<Tree> syntaxTree = poc.getAnnotation().getSyntaxTree();
                            List<Tree> newTree = new ArrayList<Tree>();
                            for (Tree tr : syntaxTree) {
                                if (ontologyConceptTrees != null
                                                && ontologyConceptTrees.contains(tr)) {
                                    // do nothing
                                } else {
                                    newTree.add(tr);
                                }
                            }
                            newPOC.getAnnotation().setSyntaxTree(newTree);
                            pocsToAdd.add(newPOC);
                            pocsToRemove.add(poc);
                        }
                        // sConcept.setVerified(true);
                        sConcept.getOntologyElement().getAnnotation()
                                        .setSyntaxTree(poc.getAnnotation().getSyntaxTree());
                    }
                }
            }
            // logger.debug("Finished checking POC: " +
            // poc.getAnnotation().getText());
        }
        logger.info("Marked " + pocsToRemove.size()
                        + " POCs to remove (i.e.they overlap with OCs).");
        question.getPocs().removeAll(pocsToRemove);
        question.getPocs().addAll(pocsToAdd);
        return question;
    }

    /**
     * check whether word is in the specified list this should be moved somewhere to the configuration file
     * 
     * @param preTerminal
     * @return
     */
    boolean isInTheIgnoreList(Tree preTerminal) {
        String word = preTerminal.getChildrenAsList().get(0).label().value().toLowerCase();
        logger.debug("Checking:" + word);
        String[] ignoreArray =
        {"do", "does", "did", "are", "were", "was", "is", "has", "have", "give",
                        "show", "list"};
        List<String> ignoreList = Arrays.asList(ignoreArray);
        if (ignoreList.contains(word)) {
            logger.debug(word + " found in the ignore list.");
            return true;
        } else {
            logger.debug(word + " NOT in the ignore list.");
            return false;
        }
    }

    /**
     * @deprecated
     * @param root
     * @param start
     * @param word
     * @return
     */
    Tree locateTheTreeWithThisWord(Tree root, int start, String word) {
        boolean locatedWord = false;
        Tree thisTree = null;
        while (!locatedWord) {
            thisTree = Trees.getTerminal(root, start);
            String treeString = thisTree.label().value();
            logger.info("Tree string:" + treeString + " searching for word:" + word);
            if (treeString.equals(word)) {
                thisTree = Trees.getPreTerminal(root, start);
                logger.info("Found:" + thisTree.toString());
                locatedWord = true;
            } else {
                start++;
            }
        }
        return thisTree;
    }

    /**
     * for phrases such as give me and show me
     * 
     * @param root
     * @param start
     * @param end
     * @return
     */
    boolean checkWhetherThisIsATwoWordToIgnore(Tree root, int start, int end) {
        boolean foundWordToIgnore = false;
        List<Tree> list = new ArrayList();
        for (int i = start; i < end; i++) {
            Tree tree = Trees.getTerminal(root, i);
            list.add(tree);
        }
        String niceString =
                        treeUtils
                                        .getNiceString(list).toLowerCase();
        String[] ignoreTwoWordList = {"give me", "show me"};
        List ignoreList = Arrays.asList(ignoreTwoWordList);
        if (ignoreList.contains(niceString)) foundWordToIgnore = true;
        return foundWordToIgnore;
    }

    /**
     * check whether word should be skipped by lucene annotator
     * 
     * @param root
     * @param start
     * @return
     */
    boolean checkWhetherItShouldBeIgnored(Tree root, int start, Tree thisTree) {
        boolean foundWordToIgnore = false;
        logger.debug("Checking " + thisTree.toString());
        if (FreyaConstants.DT_TAG_TREEBANK.equals(thisTree.label().value()))
            foundWordToIgnore = true;
        else if (thisTree.label().value().startsWith("VB") && isInTheIgnoreList(thisTree))
            foundWordToIgnore = true;
        else if (thisTree.label().value().startsWith("PRP"))
            foundWordToIgnore = true;
        // check for IN used to be here but now is in the phrase part because
        else if (thisTree.getChildrenAsList().get(0).label().value().equals("a"))
            foundWordToIgnore = true;// this is to correct SP when it identifies that
        // 'a' is a foreign word?!
        return foundWordToIgnore;
    }

    /**
     * checks whether word starts with WH or IN and then does not do lookup for these
     * 
     * @param root
     * @param start
     * @return
     */
    boolean checkWhetherThisIsAWHPhrase(Tree root, int start, Tree thisTree) {
        boolean foundWordToIgnore = false;
        // Tree thisTree = locateTheTreeWithThisWord(root,start,word);
        // thisTree.parent() is null; we must supply the root tree in order to find
        // parent
        if (thisTree.parent(root).label().value().startsWith("WH")
                        && !thisTree.label().value().startsWith(FreyaConstants.NN_TAG_TREEBANK)
                        && !thisTree.label().value().startsWith(FreyaConstants.JJ_TAG_TREEBANK))
            foundWordToIgnore = true;
        // also if it starts with IN than this phrase should be skipped
        if (thisTree.label().value().startsWith("IN")) foundWordToIgnore = true;
        return foundWordToIgnore;
    }

    /**
     * this method takes question with tokens and
     *
     * @param tokens
     * @param root
     * @param preferLonger
     * @return a list of Annotations.
     */
    public List<Annotation> fromTokensToLuceneLookupAnnotations(List<HasWord> tokens, Tree root, boolean preferLonger) {
        List<Annotation> lookups = new ArrayList<Annotation>();
        logger.debug("There are this many tokens in the query:" + tokens.size());

        for (int start = 0; start < tokens.size(); start++) {
            Tree thisTree = Trees.getPreTerminal(root, start);
            for (int end = start + 1; end <= tokens.size(); end++) {
                boolean foundWordToIgnore = checkWhetherItShouldBeIgnored(root, start, thisTree);
                boolean foundPhraseToIgnore = checkWhetherThisIsAWHPhrase(root, start, thisTree);
                boolean foundTwoWordsToIgnore = checkWhetherThisIsATwoWordToIgnore(root, start, end);
                if (start == end - 2 && foundTwoWordsToIgnore) {
                    logger.debug("NOT searching solr for this (found in ignore list):" + start + " end:" + end);
                    break;
                } else if (start == end - 1 && foundWordToIgnore) {
                    logger.debug("NOT searching solr for this (found in ignore list):" + thisTree.toString());
                    // do nothing, skip this DT
                } else if (foundPhraseToIgnore) {
                    // do nothing skip this is for words starting with some special words
                    // such as WH phrase eg how many
                    logger.debug("NOT searching solr for this (found in ignore list):" + thisTree.toString());
                } else {
                    // String to be looked up
                    String span = "";
                    Annotation ann = new Annotation();
                    Long startNode = null;
                    Long endNode = null;
                    List<Tree> trees = new ArrayList<Tree>();
                    for (int k = start; k < end; k++) {
                        span = span + " " + tokens.get(k).word();
                        Tree tree = Trees.getPreTerminal(root, k);
                        trees.add(tree);
                    }
                    startNode = new Long(start);
                    endNode = new Long(end);
                    ann.setStartOffset(startNode);
                    ann.setEndOffset(endNode);
                    ann.setText(span.trim());
                    ann.setSyntaxTree(trees);

                    // check here whether this is common noun plural (NNS);
                    // if yes, and if start=end-1 then this should have special treatment:
                    // i.e. sent it to stemmer immediately; if nothing found lowercase;
                    // then exact...
                    boolean specialTreatment = false;
                    if (start == end - 1 && requiresSpecialTreatment(trees)) {
                        specialTreatment = true;
                    }

                    // this is to support all live albums so that we can search for live
                    // albums not all live albums and also use stemming for phrases
                    if (phraseRequiresSpecialTreatment(trees, root)) {
                        specialTreatment = true;
                        // remove DT from the phrase if any
                        // if(trees.get(0).label().value().startsWith("DT")) {
                        // ann.getSyntaxTree().remove(0);
                        // logger.info("After Removing DT:"+trees.toString());
                        // }
                        // remove everything but not JJ or NN
                        // How many live albums did The Rolling Stones release?
                        // Are there any live albums by the Beatles?
                        List<Tree> newTrees = new ArrayList<Tree>();
                        for (Tree t : trees) {
                            if (t.label().value().contains("JJ") && !t.parent(root).label().value().startsWith("WH")) {
                                newTrees.add(t);
                            }
                            if (t.label().value().contains("NN")) {
                                newTrees.add(t);
                            }
                        }
                        ann.setSyntaxTree(newTrees);
                    }

                    //List<Annotation> tmplookups = luceneAnnotator.searchIndex(ann, specialTreatment);
                    List<Annotation> tmplookups;
//                    if (specialTreatment) {
//                        tmplookups = solrAnnotator.searchStemmedContentFirst(ann);
//                    } else {
                        tmplookups = solrAnnotator.searchExactContentFirst(ann);
//                    }
                    lookups.addAll(tmplookups);
                    if (tmplookups.size() > 0) {
                        logger.info("Found " + tmplookups.size() + " entries in the solr index for " + ann.getText());
                    }
                }
            }
        }
        logger.debug("before filtering:" + lookups.size());
        if (preferLonger) {
            lookups = ocUtil.disambiguateBasedOnLength(lookups);
        }
        logger.debug("after filtering:" + lookups.size());
        // rename here
        int before = lookups.size();
        List<Annotation> toReturn = renameAnnotations.rename(lookups);
        int after = lookups.size();
        if (before != after)
            logger.debug("After renaming the number changed from " + before + " to " + after);
        return toReturn;
    }

    /**
     * @param trees
     * @return
     */
    boolean requiresSpecialTreatment(List<Tree> trees) {
        boolean yesorno = false;
        Tree aTree = trees.get(0);
        if (aTree.label().value().equals("NNS")) yesorno = true;
        // some mass nouns can be NN
        if (aTree.label().value().equals("NN")) yesorno = true;
        return yesorno;
    }

    /**
     * if the parent is NP or WHNP
     * 
     * @param trees
     * @return
     */
    boolean phraseRequiresSpecialTreatment(List<Tree> trees, Tree root) {
        boolean yesorno = false;
        Tree aTree = trees.get(0).parent(root);
        logger.info("Parent is:" + aTree.toString());
        if (aTree.label().value().contains("NP"))
            yesorno = true;
        // not always there are prepreterminals see:
        // How many live albums did The Rolling Stones release?
        else if (aTree.parent(root).label().value().contains("NP")) yesorno = true;
        return yesorno;
    }

    /**
     * finds annotations with annotation.getText()
     * 
     * @param annotation
     * @return
     */
    public List<Annotation> findLiteral(Annotation annotation) {
        List<Annotation> anns = solrAnnotator.searchExactContentFirst(annotation);
        logger.info("For " + annotation.getText() + " found:" + anns.toString());
        List toReturn = renameAnnotations.rename(anns);
        logger.info("After renaming these are " + toReturn.toString());
        return toReturn;
    }
}
