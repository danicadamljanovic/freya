package org.freya.exe;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Trees;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.POC;
import org.freya.model.Question;
import org.freya.parser.stanford.SimpleStanfordParser;
import org.freya.parser.stanford.TreeUtils;
import org.freya.util.FreyaConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StanfordExecutor {
    private static final Log logger = LogFactory.getLog(StanfordExecutor.class);
    @Autowired private SimpleStanfordParser stanfordParser;
    @Autowired TreeUtils treeUtils;

    public SimpleStanfordParser getStanfordParser() {
        return stanfordParser;
    }

    public void setStanfordParser(SimpleStanfordParser stanfordParser) {
        this.stanfordParser = stanfordParser;
    }

    /**
     * finding POCs independently from GATE
     * 
     * @param question
     * @return
     * @throws Exception
     */
    public Question findPocs(String text) {
        Question question = null;
        try {
            question = stanfordParser.parseQuestion(text);
        } catch (ExecutionException e) {
            logger.error(e);
        }
        return question;
    }

    /**
     * this method is removing pocs such as 'how' 'which' 'me' etc. given that they have certain tags attached there
     * might be better (more robust way) to handle this, but for now...
     * 
     * @param question
     * @return
     */
    public Question removePocsToIgnore(Question question) {
        logger
                        .info("Removing POCs at the beginning  of the sentence (who,give,which,what..)...");
        List<POC> pocsToRemove = new ArrayList<POC>();
        List<POC> pocs = question.getPocs();
        for (int i = 0; i < pocs.size(); i++) {
            POC poc = pocs.get(i);
            for (int k = 0; k < poc.getAnnotation().getSyntaxTree().size(); k++) {
                Tree tree = poc.getAnnotation().getSyntaxTree().get(k);
                logger.debug("Checking poc:" + poc + "\n  is prepreterminal:"
                                + tree.isPrePreTerminal() + " and is preterminal:"
                                + tree.isPreTerminal());
                if (tree.isPreTerminal()) {
                    String annText = poc.getAnnotation().getText().toLowerCase();
                    logger.debug("Poc text:" + annText);
                    if (annText.startsWith("how")
                                    || annText.startsWith("many")
                                    || annText.startsWith("what")
                                    || annText.startsWith("which")
                                    || annText.startsWith("list")
                                    || annText.startsWith("show")
                                    || annText.startsWith("give")
                                    || (tree != null && tree.label().value().startsWith(
                                                    FreyaConstants.PRP_TAG_TREEBANK))) {
                        logger.debug("annText text:" + annText);
                        logger.debug("tree.label().value() text:" + tree.label().value());
                        if (poc.getAnnotation().getSyntaxTree().size() > 1) {
                            // upate the tree of this poc, do not remove it
                            List<Tree> newTree = new ArrayList<Tree>();
                            for (int j = 1; i < poc.getAnnotation().getSyntaxTree().size(); i++) {
                                newTree.add(poc.getAnnotation().getSyntaxTree().get(j));
                                // newTree.remove(tree);
                                long startNode =
                                                Trees.leftEdge(poc.getAnnotation().getSyntaxTree().get(
                                                                k + 1), question.getSyntaxTree());
                                poc.getAnnotation().setStartOffset(startNode);
                                poc.getAnnotation().setSyntaxTree(newTree);
                                String newText =
                                                treeUtils.getNiceString(
                                                                newTree);
                                poc.getAnnotation().setText(newText);
                                logger.debug("The new poc is now:" + poc.toString());
                            }
                        } else {
                            logger.info("POC marked for removal: "
                                            + poc.getAnnotation().getSyntaxTree());
                            pocsToRemove.add(poc);
                        }
                    }
                } else if (tree.isPrePreTerminal()) {
                    // this does not happen often but if it does then split it before
                    // running this method again
                    List<Tree> children = tree.getChildrenAsList();
                    poc.getAnnotation().setSyntaxTree(children);
                    removePocsToIgnore(question);
                }
            }
        }
        logger.info("Marked " + pocsToRemove.size() + " POCs to remove.");
        question.getPocs().removeAll(pocsToRemove);
        return question;
    }

    /**
     * this method is removing pocs such as 'how' 'which' 'me' etc. given that they have certain tags attached there
     * might be better (more robust way) to handle this, but for now...
     * 
     * @param question
     * @return
     */
    public Question removePocsToIgnoreNew(Question question) {
        logger.info("Removing POCs which are 'perhaps' useless...");
        List<POC> pocsToRemove = new ArrayList<POC>();
        List<POC> pocsToAdd = new ArrayList<POC>();
        List<POC> pocs = question.getPocs();
        for (int i = 0; i < pocs.size(); i++) {
            POC poc = pocs.get(i);
            Tree tree = poc.getAnnotation().getSyntaxTree().get(0);
            String annText = poc.getAnnotation().getText().toLowerCase();
            if (annText.startsWith("how")
                            || annText.startsWith("many")
                            || annText.startsWith("what")
                            || annText.startsWith("which")
                            || annText.startsWith("list")
                            || annText.startsWith("show")
                            || (tree != null && tree.label().value().startsWith(
                                            FreyaConstants.PRP_TAG_TREEBANK))) {
                logger.info("POC marked for removal: "
                                + poc.getAnnotation().getSyntaxTree());
                pocsToRemove.add(poc);
            }
        }
        logger.info("Marked " + pocsToRemove.size() + " POCs to remove.");
        question.getPocs().removeAll(pocsToRemove);
        return question;
    }

    /**
     * Closes the GATE application (and by extension all the PRs it contains) and the corpus using
     * {@link Factory#deleteResource(gate.Resource) deleteResource}.
     */
    public void shutdown() {
        logger.debug("SHUTDOWN stanfordExecutor called");
        stanfordParser = null;
    }
}
