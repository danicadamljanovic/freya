package org.freya.parser.stanford;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.freya.model.POC;
import org.freya.util.FreyaConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.ModCollinsHeadFinder;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.Trees;
import edu.stanford.nlp.trees.TypedDependency;

@Component
public class TreeUtils {
    static org.apache.commons.logging.Log logger =
                    org.apache.commons.logging.LogFactory.getLog(TreeUtils.class);

    @Value("${org.freya.parser.preterminal.pocs}") String preterminalPOCs;
    //TODO WRAP THIS UP IN A PROPERTY LIKE preterminalPOCs!!!!!!!
    //String toExclude = "DT,PRP,RB";
    @Value("${org.freya.parser.preterminal.exclude}") String preterminalsToExclude;

    public Tree getAnswerType(Tree tree) throws Exception {
        Tree mainSubjectTree = findTheHeadOfTheNounPhrase(tree);
        return mainSubjectTree;
    }

    /**
     * @param tree
     * @return
     */
    public Tree getMainSubjectTree(Tree tree) {
        List<Tree> pocs = findPocs(tree);
        if (pocs != null && pocs.size() > 0)
            return pocs.get(0);
        else
            return null;
    }

    /**
     * finds the first NP/NN* (main subject/answer type/focus);
     * 
     * @param tree
     * @return
     */
    public Tree findTheFirstNounPhraseOld(Tree tree) {
        Tree value = null;
        // treeLabel is the pos tag: NP, VP, etc.
        String treeLabel = tree.label().value();
        if (treeLabel != null
                        && (treeLabel.startsWith("NP") || treeLabel.startsWith("NN"))
                        && !(treeLabel.startsWith("PRP")))
        // || treeLabel.startsWith("WHNP")
        {
            return tree;
        } else if (treeLabel != null
                        && (treeLabel.startsWith("WHADVP") || treeLabel.startsWith("WHNP") || treeLabel
                                        .startsWith("WHADJP"))// this is to support how long,
        // how
        // large, how big, etc.
        ) {
            List children = tree.getChildrenAsList();
            if (children != null && children.size() <= 2) {// we always expect
                // one or two
                Tree firstChild = (Tree) children.get(0);
                Tree secondChild = null;
                if (children.size() > 1) secondChild = (Tree) children.get(1);
                // this is to support where what etc
                if (secondChild == null
                                && (firstChild.label().value().startsWith("WRB"))) {
                    // || firstChild.label().value().startsWith("WP"))) {
                    return tree;
                } else if (secondChild != null) {
                    if (firstChild.label().value().startsWith("WRB")
                                    && ((secondChild.label().value().startsWith("JJ")) || secondChild
                                                    .label().value().startsWith("ADJP"))
                                    || secondChild.label().value().startsWith("NN")
                                    || secondChild.label().value().startsWith("NP")
                                    || firstChild.label().value().startsWith("NN")
                                    || firstChild.label().value().startsWith("NP"))
                        // return secondChild;// well?
                        return tree;
                }
            } else if (children != null) {
                Iterator it = children.iterator();
                while (it.hasNext()) {
                    Tree something = (Tree) it.next();
                    if (something.label().value().startsWith("NP")
                                    || (something.label().value().startsWith("NN")))
                        return something;
                }
            }
        } else {
            List<Tree> listOfChildren = tree.getChildrenAsList();
            // put them in reverse order
            // Collections.reverse(listOfChildren);
            for (Tree currentTree : listOfChildren) {
                Tree mainSubjectTree = getMainSubjectTree(currentTree);
                if (mainSubjectTree != null) {
                    value = mainSubjectTree;
                    break;
                }
            }
        }
        return value;
    }

    /**
     * fins preterminals
     * 
     * @param tree
     * @return
     */
    public List<Tree> findPrePreTerminals(Tree tree) {
        List<Tree> pocs = new ArrayList<Tree>();
        Tree value = null;
        // treeLabel is the pos tag: NP, VP, etc.
        String treeLabel = tree.label().value();
        // logger.info("label is:"+treeLabel);
        if (tree.isPrePreTerminal()) {
            pocs.add(tree);
            logger.debug(treeLabel
                            + " is prepreterminal so adding it to the list of POCs.");
            return pocs;
        } else {
            List<Tree> listOfChildren = tree.getChildrenAsList();
            // put them in reverse order
            // Collections.reverse(listOfChildren);
            // logger.info("now listing children...");
            for (Tree currentTree : listOfChildren) {
                // logger.info("child..."+currentTree.label().value());
                if (currentTree.isPrePreTerminal())
                    pocs.add(currentTree);
                else
                    pocs.addAll(findPrePreTerminals(currentTree));
                // logger.info("currently total pocs are:"+pocs.size());
            }
        }
        return pocs;
    }


    /**
     * lucene split poc which contains adjective into two pocs: adjective and the rest
     * 
     * @param pocs
     * @param root
     * @param stanfordSentence
     * @return
     */
    public List<POC> generateSeparatePOCForAdjectives(List<POC> pocs, Tree root) {
        // if there is no JJ* generate a new poc and update the current
        List<Tree> adjectives = new ArrayList<Tree>();
        List<POC> toRemove = new ArrayList<POC>();
        logger.debug("before checking jjs...pocs:" + pocs.toString());
        for (POC pocElement : pocs) {
            List<Tree> trees = pocElement.getAnnotation().getSyntaxTree();
            List<Tree> splitTrees = new ArrayList<Tree>();
            for (Tree eachTree : trees) {
                if (eachTree.isPrePreTerminal()) {
                    // split into preterminals
                    splitTrees.addAll(eachTree.getChildrenAsList());
                } else {
                    splitTrees.add(eachTree);
                }
            }
            List<Tree> remainingList = new ArrayList<Tree>();
            for (Tree pocTree : splitTrees) {
                if (isDescriptive(pocTree) && splitTrees.size() > 1) {
                    // so that if JJs are already split we do not generate
                    // duplicates
                    adjectives.add(pocTree);
                } else {
                    if (!remainingList.contains(pocTree)) remainingList.add(pocTree);
                }
            }
            // //////////////////////////////////////update
            if (remainingList != null && remainingList.size() > 0) {
                pocElement = updatePOCAnnotation(remainingList, root, pocElement);
            } else {
                // mark the poc for removal
                toRemove.add(pocElement);
            }
        }
        logger.debug("before splitting jjs...pocs:" + pocs.toString());
        // //////////////////////////////////////////
        // generate poc from the adjective
        // ////////////////////////////////////////
        if (adjectives.size() > 0) {
            for (Tree adjective : adjectives) {
                POC newPoc = new POC();
                List<Tree> adjectiveTree = new ArrayList<Tree>();
                adjectiveTree.add(adjective);
                newPoc = updatePOCAnnotation(adjectiveTree, root, newPoc);
                pocs.add(newPoc);
            }
        }
        logger.debug("after adding jjs...:" + pocs.toString());
        if (toRemove.size() > 0) pocs.removeAll(toRemove);
        return pocs;
    }

    /**
     * checks whether the tree is ajdective, adverb superlative or verb, past participle (as this verb is always part of
     * the NP we assume it has the function of adjective such as 'populated state')
     * 
     * @param tree
     * @return
     */
    boolean isDescriptive(Tree tree) {
        boolean isDescriptive = false;
        if (tree.label().value().startsWith(FreyaConstants.JJ_TAG_TREEBANK)
                        || tree.label().value().startsWith(
                                        FreyaConstants.VBN_TAG_TREEBANK)
                        || tree.label().value().startsWith(
                                        FreyaConstants.RBS_TAG_TREEBANK)
                        || tree.label().value().startsWith("ADJP")) {
            isDescriptive = true;
        }
        return isDescriptive;
    }

    /**
     * checks whether there are any JJ* or ADJP inside poc
     * 
     * @param poc
     * @return
     */
    public boolean pocContainsJJs(POC poc) {
        boolean containsJJs = false;
        for (Tree tree : poc.getAnnotation().getSyntaxTree()) {
            String treeLabel = tree.label().value();
            // logger.info("Checking whether "+treeLabel+ " is JJ");
            if (isDescriptive(tree)) {
                containsJJs = true;
                break;
            } else {
                List<Tree> children = tree.getChildrenAsList();
                for (Tree child : children) {
                    // String childLabel = child.label().value();
                    if (isDescriptive(child)) {
                        containsJJs = true;
                        break;
                    }
                }
            }
        }
        return containsJJs;
    }

    /**
     * if pocs are prepreterminals then split their tree into children
     * 
     * @param pocs
     * @return
     */
    public List<POC> splitPocs(List<POC> pocs) {
        List<POC> toRemove = new ArrayList<POC>();
        for (POC poc : pocs) {
            List<Tree> trees = poc.getAnnotation().getSyntaxTree();
            if (trees != null && trees.size() == 1) {
                // check if they are prepreterminals
                Tree tree = trees.get(0);
                if (tree.isPrePreTerminal()) {
                    // split into preterminals
                    List<Tree> newTree = new ArrayList<Tree>();
                    for (Tree child : tree.getChildrenAsList()) {
                        newTree.add(child);
                    }
                    poc.getAnnotation().setSyntaxTree(newTree);
                }
            }
        }
        return pocs;
    }


    /**
     * update the poc with new syntax tree and also update it's annotation start and end offsets and the text
     * 
     * @param treeList
     * @param root
     * @param stanfordSentence
     * @param poc
     * @return poc updated with the syntax tree; null if the tree is empty
     */
    POC updatePOCAnnotation(List<Tree> treeList, Tree root, POC poc) {
        if (treeList == null || treeList.size() == 0) return null;
        Tree firstTree = treeList.get(0);
        long parserStartOffset = Trees.leftEdge(firstTree, root);
        Tree lastTree = treeList.get(treeList.size() - 1);
        long parserEndOffset = Trees.rightEdge(lastTree, root);
        poc.getAnnotation().setStartOffset(parserStartOffset);
        poc.getAnnotation().setEndOffset(parserEndOffset);
        poc.getAnnotation().setText(getNiceString(treeList));
        poc.getAnnotation().setSyntaxTree(treeList);
        return poc;
    }


    /**
     * independent of stanfordSentence remove DT from pocs: if poc has DT then transform it to the list of its children
     * but remove DT now poc can be a list of trees which are on the same level, not only one tree
     * 
     * @param pocs
     * @return
     */
    public List<POC> removeDTFromPOCs(List<POC> pocs, Tree root) {
        // if there is no DT leave pocs as they are (one Tree in the list)
        for (POC pocElement : pocs) {
            // before executing this method all pocs are stored in list each
            // list having only one element
            Tree pocTree = pocElement.getAnnotation().getSyntaxTree().get(0);
            List<Tree> list = new ArrayList<Tree>();
            List<Tree> children = pocTree.getChildrenAsList();
            for (Tree child : children) {
                if (FreyaConstants.DT_TAG_TREEBANK.equals(child.label().value())) {
                    List<Tree> withoutDT = new ArrayList<Tree>();
                    withoutDT.addAll(children);
                    withoutDT.remove(child);
                    logger.debug("Removing DT from POC.");
                    list = withoutDT;
                    break;
                } else {
                    if (!list.contains(pocTree)) list.add(pocTree);
                }
            }
            pocElement = updatePOCAnnotation(list, root, pocElement);
        }
        return pocs;
    }

    boolean pocValid(Tree node) {
      boolean isValid = true;
      
      int numberOfChildren = node.getChildrenAsList().size();
      if (numberOfChildren == 1) {
        String childLabel = node.getChildrenAsList().get(0).label().value();
        if (preterminalsToExclude.contains(childLabel)) isValid = false;
      }
      
      return isValid;
    }
    
    /**
     * finds pocs: CURRENTLY these are NN* and NP*
     * 
     * @param tree
     * @return
     */
    public List<Tree> findPocs(Tree tree) {
        List<Tree> pocs = new ArrayList<Tree>();
        List<Tree> prePreTerminals = findPrePreTerminals(tree);
        for (Tree node : prePreTerminals) {
            String nodeLabel = node.label().value();
            String nodeChildLabel = node.getChildrenAsList().get(0).label().value();
            // NPs and NNs
            if (nodeLabel != null
                            && (nodeLabel.startsWith(FreyaConstants.NP_TAG_TREEBANK)
                                            || nodeLabel.startsWith(FreyaConstants.NN_TAG_TREEBANK)
                                            || nodeLabel.startsWith(FreyaConstants.NX_TAG_TREEBANK) || (nodeLabel
                                                .startsWith(FreyaConstants.ADJP_TAG_TREEBANK)))
                            && !(nodeChildLabel.startsWith(FreyaConstants.PRP_TAG_TREEBANK))
                            && !(nodeChildLabel.startsWith(FreyaConstants.EX_TAG_TREEBANK))) {
                if (pocValid(node))
                  pocs.add(node);// if they are single nodes with DT or PRP then ignore them
            }
            // //////////////////////////////////////////////////////////////////
            // this is to support how long, //////
            // how //////
            // large, how big, etc. //////
            // //////////////////////////////////////////////////////////////////
            else if (nodeLabel != null
                            && (nodeLabel.startsWith(FreyaConstants.WHADVP_TAG_TREEBANK)
                                            || nodeLabel
                                                            .startsWith(FreyaConstants.WHNP_TAG_TREEBANK) || nodeLabel
                                                // added because of high high: high is prepreterminal but
                                                // the whole how high is not
                                                .startsWith(FreyaConstants.WHADJP_TAG_TREEBANK))) {
                List children = node.getChildrenAsList();
                if (children != null && children.size() <= 2) {// we always
                    // expect
                    // one or two
                    Tree firstChild = (Tree) children.get(0);
                    Tree secondChild = null;
                    if (children.size() > 1) secondChild = (Tree) children.get(1);
                    // this is to support where what etc
                    if (secondChild == null
                                    && (firstChild.label().value().startsWith(
                                                    FreyaConstants.WRB_TAG_TREEBANK) || firstChild
                                                    .label().value().startsWith(
                                                                    FreyaConstants.WP_TAG_TREEBANK))) {
                        // || firstChild.label().value().startsWith("WP"))) {
                        List<Tree> leaves = firstChild.getLeaves();
                        for (Tree leaf : leaves) {
                            if (leaf.label().value().toLowerCase().equals("where")
                                            || leaf.label().value().toLowerCase().equals("when")
                                            || leaf.label().value().toLowerCase().equals("who")) {
                                logger
                                                .info("Found WH-");
                                // logger.info("leaf.label().value():" + leaf.label().value());
                                pocs.add(node);
                            }
                        }
                    } else if (secondChild != null) {
                        if (firstChild.label().value().startsWith(
                                        FreyaConstants.WRB_TAG_TREEBANK)
                                        && ((secondChild.label().value()
                                                        .startsWith(FreyaConstants.JJ_TAG_TREEBANK)) || secondChild
                                                        .label().value().startsWith(
                                                                        FreyaConstants.ADJP_TAG_TREEBANK))
                                        || secondChild.label().value().startsWith(
                                                        FreyaConstants.NN_TAG_TREEBANK)
                                        || secondChild.label().value().startsWith(
                                                        FreyaConstants.NP_TAG_TREEBANK)
                                        || firstChild.label().value().startsWith(
                                                        FreyaConstants.NN_TAG_TREEBANK)
                                        || firstChild.label().value().startsWith(
                                                        FreyaConstants.NP_TAG_TREEBANK)
                                        || firstChild.label().value().startsWith(
                                                        FreyaConstants.RB_TAG_TREEBANK))
                            // return secondChild;// well?
                            // this is to remove WRB in 'how long' how many etc
                            pocs.add(node);
                    }
                } else if (children != null) {
                    Iterator it = children.iterator();
                    while (it.hasNext()) {
                        Tree something = (Tree) it.next();
                        if (something.label().value().startsWith("NP")
                                        || (something.label().value().startsWith("NN")))
                            pocs.add(something);
                    }
                }
            }// if prepreterminal is WHNP, WHADJP or WHADVP
        }
        // now also add those that are preterminals and NNs but were missed in
        // findPocs method
        logger.info("Number of pocs as prepreterminals:" + pocs.size());
        pocs.addAll(findPreTerminalsThatAreNNOrINs(tree, pocs));
        logger
                        .info("Number of pocs as preterminals and prepreterminals (final num of pocs):"
                                        + pocs.size());
        return pocs;
    }

    /**
     * find those preterminals that are NNs as they were missed due to the way sp parses tree
     * 
     * @param root
     * @param pocs
     * @return
     */
    List<Tree> findPreTerminalsThatAreNNOrINs(Tree root, List<Tree> pocs) {
        long numberOfTokens = root.getLeaves().size();
        List<Tree> newList = new ArrayList<Tree>();
        Set<Long> skipList = new HashSet();

        for (Tree poc : pocs) {
            logger.debug("Checking poc:" + poc.toString());
            long start = Trees.leftEdge(poc, root);
            long end = Trees.rightEdge(poc, root);
            logger.debug("Start poc:" + start);
            logger.debug("End poc:" + end);
            for (long j = start; j < end; j++) {
                skipList.add(new Long(j));
                logger.debug("Adding element to skip list:" + j);
            }
        }
        Set<Long> listToConsider = new HashSet<Long>();
        for (long i = 0; i < numberOfTokens; i++) {
            boolean iFound = false;
            for (Long toSkip : skipList) {
                if (toSkip.longValue() == i) {
                    iFound = true;
                    break;
                } else {}
            }
            if (!iFound) listToConsider.add(i);
        }
        for (Long element : listToConsider) {
            Tree preTerminal = null;
            preTerminal = Trees.getPreTerminal(root, element.intValue());
            logger.debug("checking now:" + element.intValue()
                            + " and its preterminal:" + preTerminal);
            if (preTerminal != null
                            && startsWithTagsToConsider(preTerminal.label().value())) {
                newList.add(preTerminal);
                logger.info("Found preTerminal:" + preTerminal.toString());
            }
        }
        return newList;
    }

    /**
     * reads pocs to consider form properties file and checks whether it should be considered or not; this applies to
     * preterminal pocs only
     * 
     * @param preterminalLabelValue
     * @return
     */
    boolean startsWithTagsToConsider(String preterminalLabelValue) {
        boolean consider = false;

        // InputStream is = this.getClass().getResourceAsStream("/Service.properties");
        // Properties ps = new Properties();
        // try {
        // ps.load(is);
        // } catch(IOException e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }
        // String preterminalPOCs = new String(ps.getProperty("preterminalPOCs"));
        String[] tagsToConsider = preterminalPOCs.split(",");
        for (String tag : tagsToConsider) {
            if (preterminalLabelValue.startsWith(tag)) {
                consider = true;
                break;
            }
        }
        return consider;
    }

    /**
     * this method finds the head of the phrase
     * 
     * @param tree
     * @return
     */
    public Tree findTheHeadOfTheNounPhrase(Tree tree) {
        Tree toReturn = null;
        if (tree != null) {
            HeadFinder hf = new ModCollinsHeadFinder();
            Tree head = tree.headTerminal(hf);
            if (head.label().value() != null
                            && !(head.label().value().startsWith("PRP"))) {
                toReturn = head;
            }
        }
        return toReturn;
        // System.out.println("the head is:" + head.toString());
    }

    /**
     * this method finds the head of the phrase
     * 
     * @param tree
     * @return
     */
    public List<TreeGraphNode> findModifiersOfTheNounPhrase(Tree tree, Tree head) {
        if (tree == null || head == null) return null;
        HeadFinder hf = new ModCollinsHeadFinder();
        // Tree head = tree.headTerminal(hf);
        List<TreeGraphNode> toReturn = new ArrayList<TreeGraphNode>();
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
        //Collection tdl = gs.typedDependenciesCollapsed();
        List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
        Iterator it = tdl.iterator();
        while (it.hasNext()) {
            TypedDependency td = (TypedDependency) it.next();
            String depName = td.reln().getShortName();
            if ((depName.equals("amod") || depName.equals("dep") || depName
                            .equals("advmod"))
                            && td.gov().value() == head.value()) {
                toReturn.add(td.dep());
            }
        }
        TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
        //tp.printTree(head);
        return toReturn;
        // System.out.println("the head is:" + head.toString());
    }

    /**
     * generates a nice string from the tree
     * 
     * @param firstNPorNN
     * @return
     */
    public String getNiceString(List<Tree> trees) {
        String niceString = "";
        for (Tree tree : trees) {
            for (Tree leaf : tree.getLeaves()) {
                if (leaf.isLeaf()) {
                    niceString = niceString + " " + leaf.value();
                } else {
                    List<Tree> leafs = new ArrayList<Tree>();
                    leafs.add(leaf);
                    niceString = niceString + " " + getNiceString(leafs);
                }
            }
        }
        return niceString.trim();
    }

    /**
     * returns the distance from one node to the other.
     * 
     * @param tree1
     * @param tree2
     * @return
     */
    public int getDistance(List<Tree> tree1List, List<Tree> tree2List, Tree root) {
        Tree tree1 = null;
        Tree tree2 = null;
        if (tree1List != null && tree1List.size() > 0) tree1 = tree1List.get(0);
        if (tree2List != null && tree2List.size() > 0) tree2 = tree2List.get(0);
        List nodes = root.pathNodeToNode(tree1, tree2);
        if (tree1 != null && tree2 != null && nodes != null) {
            logger.debug("Distance btw" + tree1.toString() + "and "
                            + tree2.toString() + " is:" + nodes.size());
        } else {
            logger.debug("Distance btw tree1:" + tree1 + "and tree2 " + tree2
                            + " is:" + nodes);
        }
        int nodeSize = 0;
        if (nodes != null) nodeSize = nodes.size();
        return nodeSize;
    }

    /**
     * get closest oc to the given poc
     * 
     * @param poc
     * @param ocs
     * @param root
     * @return
     */
    int getClosestOc(List<Tree> poc, List<List<Tree>> ocs, Tree root) {
        int min = 0;
        for (List<Tree> oc : ocs) {
            int dist = getDistance(poc, oc, root);
            if (dist < min) min = dist;
        }
        return min;
    }
}
