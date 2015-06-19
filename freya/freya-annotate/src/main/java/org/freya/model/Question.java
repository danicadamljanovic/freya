package org.freya.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.sun.org.apache.bcel.internal.generic.Type;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.Tree;

@XmlRootElement
public class Question {

    /**
     * question type such as boolean, number, classic (all the others);
     */

    public String sentenceText;
    public Type type;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    List<HasWord> tokens;

    Tree syntaxTree;

    /* this is focus */
    POC focus;

    List<OntologyElement> answerType;

    List<POC> pocs = new ArrayList<POC>();


    public List<HasWord> getTokens() {
        return tokens;
    }

    public void setTokens(List<HasWord> tokens) {
        this.tokens = tokens;
    }


    List<List<SemanticConcept>> semanticConcepts =
                    new ArrayList<List<SemanticConcept>>();

    public List<OntologyElement> getAnswerType() {
        return answerType;
    }

    public void setAnswerType(List<OntologyElement> answerType) {
        this.answerType = answerType;
    }

    public POC getFocus() {
        return focus;
    }

    public void setFocus(POC focus) {
        this.focus = focus;
    }

    public Tree getSyntaxTree() {
        return syntaxTree;
    }

    public void setSyntaxTree(Tree syntaxTree) {
        this.syntaxTree = syntaxTree;
    }

    public List<List<SemanticConcept>> getSemanticConcepts() {
        return semanticConcepts;
    }

    public void setSemanticConcepts(List<List<SemanticConcept>> semanticConcepts) {
        this.semanticConcepts = semanticConcepts;
    }

    public List<POC> getPocs() {
        return pocs;
    }

    public void setPocs(List<POC> pocs) {
        this.pocs = pocs;
    }

    public String toString() {
        StringBuffer b = new StringBuffer("");
        b.append("sem concepts:");
        if (this.semanticConcepts != null)
            b.append(this.semanticConcepts.toString());
        b.append("pocs:");
        if (this.pocs != null) b.append(pocs.toString());
        b.append("Tree:");
        if (this.syntaxTree != null) b.append(this.syntaxTree.toString());
        return b.toString();
    }
}
