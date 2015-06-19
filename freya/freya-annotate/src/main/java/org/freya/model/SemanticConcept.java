package org.freya.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SemanticConcept implements Serializable {

    OntologyElement ontologyElement;
    String function;
    Boolean verified = false;
    Double score;

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Boolean isVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public OntologyElement getOntologyElement() {
        return ontologyElement;
    }

    public void setOntologyElement(OntologyElement ontologyElement) {
        this.ontologyElement = ontologyElement;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String toString() {
        StringBuffer b = new StringBuffer("");
        b.append("Ontology Elements:\n");
        if (ontologyElement != null) b.append(ontologyElement.toString());
        b.append(" Verified:").append(verified).append("\n");
        b.append(" Function:").append(function).append("\n");
        if (ontologyElement != null)
            b.append("Is ontology element already added:"
                            + ontologyElement.isAlreadyAdded() + "\n");
        return b.toString();
    }

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

    public boolean equals(Object theOtherConcept) {
        boolean isEqual = false;
        String function1 = this.getFunction();
        String function2 = ((SemanticConcept) theOtherConcept).getFunction();
        OntologyElement element1 = this.getOntologyElement();
        OntologyElement element2 =
                        ((SemanticConcept) theOtherConcept).getOntologyElement();
        boolean functionEquals = false;
        if (function1 != null && function2 != null)
            functionEquals = function1.equals(function2);
        else if (function1 == null && function2 == null) functionEquals = true;
        boolean verifiedEquals = false;
        if (this.isVerified().booleanValue() == ((SemanticConcept) theOtherConcept)
                        .isVerified().booleanValue()) verifiedEquals = true;
        if (functionEquals && element1.equals(element2))
            isEqual = true;
        // System.out
        // .print("************************************************************calling equals method..result is:"
        // + isEqual);
        return isEqual;
    }
}
