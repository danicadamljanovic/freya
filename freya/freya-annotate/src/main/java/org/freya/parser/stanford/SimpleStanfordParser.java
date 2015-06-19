package org.freya.parser.stanford;

import java.io.CharArrayReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.freya.model.Annotation;
import org.freya.model.MainSubject;
import org.freya.model.POC;
import org.freya.model.Question;
import org.freya.util.FreyaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.sun.org.apache.bcel.internal.generic.Type;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.Trees;

/**
 * This USED TO BE PR is a very simple wrapper for SP: as input it takes documents, and as output it generates
 * edu.stanford.nlp.trees.Tree object and stores it in the default Annotation Set as 'StanfordTree' annotation type with
 * feature 'tree'
 * 
 * @author danica
 */
@Component
public class SimpleStanfordParser {
    private static final long serialVersionUID = -3062171258011850283L;
    static Logger logger = LoggerFactory.getLogger(SimpleStanfordParser.class);
//    static org.apache.commons.logging.Log logger =
//                    org.apache.commons.logging.LogFactory.getLog(SimpleStanfordParser.class);

    @Value("${org.freya.parser.stanford.model}") Resource parserFile;
    protected edu.stanford.nlp.parser.lexparser.LexicalizedParser lp;
    @Autowired TreeUtils treeUtils;



    /**
     * tries to predice whether it is boolean how many/count or any other question type
     * 
     * @param question
     * @return
     */
    public Question findQuestionType(Question question) {
        Tree root = question.getSyntaxTree();
        Tree firstTree = Trees.getPreTerminal(root, 0);
        System.out.println("firstTree:" + firstTree.label().value());
        System.out.println("firstTree children 0:"
                        + firstTree.getChildrenAsList().get(0).label().value());
        if (firstTree.label().value().startsWith("VB")
                        && (!firstTree.getChildrenAsList().get(0).label().value()
                                        .toLowerCase().equals("show")
                                        && !firstTree.children()[0].label().value().toLowerCase()
                                                        .equals("give")
                                        && !firstTree.children()[0].label().value().toLowerCase()
                                                        .equals("list") && !firstTree.children()[0].label()
                                        .value().toLowerCase().equals("count"))) {
            question.setType(Type.BOOLEAN);
        } else if (firstTree.getChild(0).label().value().toLowerCase().startsWith(
                        "count")) {
            question.setType(Type.LONG);
        } else if (firstTree.getChild(0).label().value().toLowerCase().equals("how")) {
            Tree secondTree = Trees.getPreTerminal(root, 1);
            if (secondTree.getChild(0).label().value().toLowerCase().equals("much")
                            || secondTree.getChild(0).label().value().toLowerCase().equals(
                                            "many"))
                question.setType(Type.LONG);
            else
                question.setType(Type.UNKNOWN);
        } else {
            question.setType(Type.UNKNOWN);
        }
        return question;
    }

    /**
     * Parse the current document without making links to GATE annotaitons.
     */
    public Question parseQuestion(String text) throws ExecutionException {
        Question question = new Question();
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        List<? extends HasWord> wordList = null;
        if (lp != null && text.length() > 0) {
            Tokenizer<? extends HasWord> toke =
                            tlp.getTokenizerFactory().getTokenizer(
                                            new CharArrayReader(text.toCharArray()));
            // wordList = stanfordSentence.getWordList();
            // use this for stanford tokeniser
            // System.out.println("*******************Parsing:" + text);
            wordList = toke.tokenize();
            // System.out.println("wordList:" + wordList.size());
            List<HasWord> tokens = new ArrayList<HasWord>();
            int i = 0;
            for (HasWord word : wordList) {
                // System.out.println("Words:" + word.word());
                // logger.info("Words:"+word.word());
                if (word.word() != null && !word.word().trim().equals("")
                                && !word.word().trim().equals(".")
                                && !word.word().trim().equals("?")
                                && !word.word().trim().equals("!")
                                && !word.word().trim().equals(","))
                    // if(((word.word().toLowerCase().equals("show")
                    // || word.word().toLowerCase().equals("list") || word.word()
                    // .toLowerCase().equals("give")
                    // && i == 0) || (word.word().toLowerCase().equals("me"))
                    // && i == 1)) {
                    // do not add these tokens
                    // } else {
                    tokens.add(word);
                // }
                i++;
            }
            question.setTokens(tokens);
        }
        // boolean successful;
        Tree tree = null;
        try {
            tree = lp.apply(wordList);
            // logger.info("Parsing was successful?" + successful);
            // tree = lp.getBestParse();
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Could not parse selected sentence\n(sentence probably too long)");
            logger.info("Error parsing");
        }
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
        Collection tdl = gs.typedDependenciesCollapsed();
        TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
        tp.printTree(tree);
        // these all would be prepreterminals?
        List<Tree> trees = treeUtils.findPocs(tree);
        question = findFocus(question);
        // list of final pocs
        List<POC> pocs = new ArrayList<POC>();
        for (Tree aPocTree : trees) {
            // for each prepreterminal, make one poc
            POC poc = new POC();
            List<Tree> aPocTrees = new ArrayList<Tree>();
            aPocTrees.add(aPocTree);
            poc.getAnnotation().setSyntaxTree(aPocTrees);
            long parserStartOffset = Trees.leftEdge(aPocTree, tree);
            long parserEndOffset = Trees.rightEdge(aPocTree, tree);
            poc.getAnnotation().setStartOffset(parserStartOffset);
            poc.getAnnotation().setEndOffset(parserEndOffset);
            String pocToText = treeUtils.getNiceString(aPocTrees);
            poc.getAnnotation().setText(pocToText);
            // now find the head of the poc and also modifiers
            Tree headTree = treeUtils.findTheHeadOfTheNounPhrase(aPocTree);
            List<TreeGraphNode> modfs =
                            treeUtils.findModifiersOfTheNounPhrase(aPocTree, headTree);
            List<String> modifiers = new ArrayList<String>();
            for (TreeGraphNode mod : modfs) {
                modifiers.add(mod.label().value());
            }
            POC head = new POC();
            head.getAnnotation().setText(headTree.label().value());
            parserStartOffset = Trees.leftEdge(headTree, tree);
            parserEndOffset = Trees.rightEdge(headTree, tree);
            head.getAnnotation().setStartOffset(parserStartOffset);
            head.getAnnotation().setEndOffset(parserEndOffset);
            List<Tree> headTreeList = new ArrayList<Tree>();
            headTreeList.add(headTree);
            head.getAnnotation().setSyntaxTree(headTreeList);
            poc.setHead(head);
            poc.setModifiers(modifiers);
            // //////////////////////////////////////////////
            pocs.add(poc);
        }
        // main subjec is generated so that it can carry some other things such
        // as priority
        // focus used to be here
        /*
         * remove DT from the POCs; separate ADJP into separate pocs and remove them from the rest
         */
        pocs = treeUtils.removeDTFromPOCs(pocs, tree);
        // logger.info(pocs.size() + " POCS before removing DTs.");
        question.setPocs(pocs);
        question.setSyntaxTree(tree);
        return question;
    }

    public Question findFocus(Question question) {

        // TODO consider an alternative solution
        MainSubject mainSubject = new MainSubject();
        List<POC> pocs = question.getPocs();
        logger.info("There are " + pocs.size() + " pocs");
        if (pocs != null && pocs.size() > 0) {
            POC firstPoc = pocs.get(0);
            String mainSubjectString =
                            firstPoc.getAnnotation().getText().toLowerCase();
            if (mainSubjectString.startsWith("how")
                            || mainSubjectString.startsWith("where")
                            || mainSubjectString.startsWith("when")
                            || mainSubjectString.startsWith("since")
                            || mainSubjectString.startsWith("who")
                            || mainSubjectString.startsWith("list")
                            || mainSubjectString.startsWith("show")) {
                logger
                                .info("SETTING UP THE PRIORITY FOR THE MAIN SUBJECT AS IT STARTS WITH HOW...");
                mainSubject.setPriority(FreyaConstants.MAIN_SUBJECT_PRIORITY_MAX);
            } else
                mainSubject
                                .setPriority(FreyaConstants.MAIN_SUBJECT_PRIORITY_MIN);


            firstPoc.setMainSubject(mainSubject);
            question.setFocus(firstPoc);

            POC preservedFocus = preserveFocus(question.getFocus(), question.getSyntaxTree());
            question.setFocus(preservedFocus);
        }

        return question;
    }

    /**
     * just copy POC into another POC so that focus can be saved for later even when all pocs are resolved through the
     * dialog
     * 
     * @param focus
     * @return
     */
    public POC preserveFocus(POC focus, Tree root) {
        POC preservedFocus = new POC();
        Annotation annotation =
                        new Annotation();
        if (focus != null) {
            annotation.setText(focus.getAnnotation().getText());
            annotation.setEndOffset(focus.getAnnotation().getEndOffset());
            annotation.setStartOffset(focus.getAnnotation().getStartOffset());

            List<Tree> trees = focus.getAnnotation().getSyntaxTree();
            Tree firstTree = trees.get(0);
            boolean toRemove = false;
            if (firstTree.isPrePreTerminal()) {
                logger.info("OK:");
                // check whether it starts with which or what and if yes remove it
                Tree toTest = firstTree.getChildrenAsList().get(0).getLeaves().get(0);
                logger.info("totest:" + toTest.label().value());
                if (toTest.label().value().toLowerCase().startsWith("which")
                                || toTest.label().value().toLowerCase().startsWith("what"))
                    toRemove = true;
            } else {

                logger.info("Not OK:, firstTree:" + firstTree.toString());
            }
            if (toRemove) {
                List<Tree> all = firstTree.getChildrenAsList();
                all.remove(0);
                focus.getAnnotation().setSyntaxTree(all);
                focus.getAnnotation().setStartOffset(focus.getAnnotation().getStartOffset() + 1);
                focus.getAnnotation().setText(treeUtils.getNiceString(all));
            }
            logger.info("Focus now looks like this:" + focus.toString());
            annotation.setSyntaxTree(focus.getAnnotation().getSyntaxTree());

            preservedFocus.setMainSubject(focus.getMainSubject());
            preservedFocus.setHead(focus.getHead());
            preservedFocus.setModifiers(focus.getModifiers());
        }
        preservedFocus.setAnnotation(annotation);
        return preservedFocus;
    }

    /**
     * method which 'cleans' pocs and separate jjs into separate ones, deletes wrb if they are followed by something
     * else, leaves them otherwise e.g. where is...is WHADVP-WRB-where..here we do not want to delete WRB how big
     * is...is WHADJP-(WRB-JJ), so here we want to get rid of WRB
     * 
     * @param cleanedPocs
     * @param root
     * @param stanfordSentence
     * @return
     */
    public Question cleanPocsLucene(Question question) {
        List<POC> cleanedPocs = question.getPocs();
        Tree root = question.getSyntaxTree();
        // remove wrb elements
        // cleanedPocs = treeUtils.removeWRB(cleanedPocs);
        logger.info("Before separating POCs (if they contain WRB) there are "
                        + cleanedPocs.size() + " POCs." + cleanedPocs.toString());
        cleanedPocs = treeUtils.generateSeparatePOCForAdjectives(cleanedPocs, root);
        logger
                        .info("After separating POCs if they contain ajdectives JJ* there are "
                                        + cleanedPocs.size() + " POCs." + cleanedPocs.toString());
        return question;
    }

    /**
     * initialisation of stanford parser, loading training data from file
     * 
     * @throws ResourceInstantiationException
     */
    @PostConstruct
    public void instantiateStanfordParser() throws Exception {
        try {
            logger.info("parserFile:" + parserFile.getFile().getAbsolutePath());

            lp = LexicalizedParser.loadModel(parserFile.getFile().getAbsolutePath());
            lp.setOptionFlags(new String[] {"-maxLength", "80",
                            "-retainTmpSubcategories"});
        } catch (Exception e) {
            throw e;
        }
    }



    public static void main(String[] args) throws Exception {
        SimpleStanfordParser p = new SimpleStanfordParser();
        p.instantiateStanfordParser();
        String text = "How may albums did Michael Jackson record?.";
        try {
            p.parseQuestion(text);
        } catch (ExecutionException e) {
            logger.info(e.getMessage());
        }
    }
}
