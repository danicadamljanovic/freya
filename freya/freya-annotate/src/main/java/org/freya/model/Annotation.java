package org.freya.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.stanford.nlp.trees.Tree;

@XmlRootElement
public class Annotation implements Serializable {

    String text;
    HashMap features;
    Long startOffset;
    Long endOffset;

    @JsonIgnore
    List<Tree> syntaxTree;
    
    public HashMap getFeatures() {
        return features;
    }

    public void setFeatures(HashMap features) {
        this.features = features;
    }

    public List<Tree> getSyntaxTree() {
        return syntaxTree;
    }

    public void setSyntaxTree(List<Tree> syntaxTree) {
        this.syntaxTree = syntaxTree;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Long startOffset) {
        this.startOffset = startOffset;
    }

    public Long getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(Long endOffSet) {
        this.endOffset = endOffSet;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer("");
        buffer.append(" Annotation: StartOffset:" + startOffset);
        buffer.append(" End offset:" + endOffset);
        buffer.append(" String:" + text);
        buffer.append(" Syntax tree:" + syntaxTree);
        if (features != null)
            buffer.append(" Features:" + features.toString());
        return buffer.toString();
    }

    public boolean equals(Object anotherObject) {
        if (this == anotherObject)
            return true;
        if (!(anotherObject instanceof Annotation))
            return false;
        if (this.startOffset == null && this.endOffset == null
                        && ((Annotation) anotherObject).startOffset != null
                        && ((Annotation) anotherObject).endOffset != null)
            return true;
        if ((anotherObject != null)
                        && (this.startOffset.longValue() == ((Annotation) anotherObject)
                                        .getStartOffset().longValue())
                        && (this.endOffset.longValue() == ((Annotation) anotherObject)
                                        .getEndOffset().longValue())
                        && (this.features.equals(((Annotation) anotherObject).features)
                        ))

            return true;
        else
            return false;
    }

    /**
     * 
     * @param aAnnot
     * @return
     */
    public boolean overlaps(Annotation aAnnot) {
        if (aAnnot == null) return false;
        if (aAnnot.getStartOffset() == null ||
                        aAnnot.getEndOffset() == null) return false;

        if (aAnnot.getEndOffset().longValue() <= this.getStartOffset().longValue())
            return false;

        if (aAnnot.getStartOffset().longValue() >= this.getEndOffset().longValue())
            return false;

        return true;
    }// overlaps


    public Object clone() {
        Object deepCopy = null;
        ByteArrayOutputStream byteArrOs = new ByteArrayOutputStream();
        ObjectOutputStream objOs;
        try {
            objOs = new ObjectOutputStream(byteArrOs);
            objOs.writeObject(this);
            ByteArrayInputStream byteArrIs =
                            new ByteArrayInputStream(byteArrOs.toByteArray());
            ObjectInputStream objIs = new ObjectInputStream(byteArrIs);
            deepCopy = objIs.readObject();
            return deepCopy;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return deepCopy;
    }

}
