/**
 * 
 */
package org.freya.model;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author danica
 */
public class InstanceListElement implements OntologyElement {
  private static final Log logger = LogFactory.getLog(InstanceListElement.class);

  boolean isAnswer;

  public boolean isAnswer() {
    return isAnswer;
  }

  public void setAnswer(boolean isAnswer) {
    this.isAnswer = isAnswer;
  }

  /** max min sum */
  String function;

  public String getFunction() {
    return function;
  }

  public void setFunction(String function) {
    this.function = function;
  }

  /*
   * this flag indicates whether the element is already in the list of elements
   * and whether it should be ignored except for the function; this happens when
   * some existing elements are modified with for example max min etc
   */
  boolean alreadyAdded;

  public boolean isAlreadyAdded() {
    return alreadyAdded;
  }

  public void setAlreadyAdded(boolean alreadyAdded) {
    this.alreadyAdded = alreadyAdded;
  }

  public boolean mainSubject = false;

  public List<String> results;

  Annotation annotation = new Annotation();

  public Annotation getAnnotation() {
    return annotation;
  }

  public void setAnnotation(Annotation annotation) {
    this.annotation = annotation;
  }


  public List<String> getResults() {
    return results;
  }

  public void setResults(List<String> results) {
    this.results = results;
  }

  public boolean isMainSubject() {
    return mainSubject;
  }

  public void setMainSubject(boolean mainSubject) {
    this.mainSubject = mainSubject;
  }

  private List<SerializableURI> data;

  private String variable = null;

  public String getVariable() {
    return variable;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  private SerializableURI classURI;

  private List<String> classURIList;

  public SerializableURI getClassURI() {
    return classURI;
  }

  public void setClassURI(SerializableURI classURI) {
    this.classURI = classURI;
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer s =
            new StringBuffer(this.getClass().getSimpleName().toString());
    s.append(" Is it the main subject? ").append(isMainSubject());
    if(annotation != null) s.append(annotation.toString());
    s.append(" Instance URI: ").append(this.data).append(" Class URI: ")
            .append(this.classURI).append(" \n Variable:")
            .append(this.variable);
    s.append("\nResults:" + results);
    s.append("Class URIs");
    if(this.classURIList != null) s.append(this.classURIList.toString());
    return s.toString();
  }

  public List<String> getClassURIList() {
    return classURIList;
  }

  public void setClassURIList(List<String> classURIList) {
    this.classURIList = classURIList;
  }

  Score score;

  public Score getScore() {
    return this.score;
  }

  public void setScore(Score score) {
    this.score = score;
  }

  @Override
  public boolean equals(Object anotherObject) {
    if(this == anotherObject) return true;
    if(!(anotherObject instanceof InstanceListElement)) return false;
    //logger.debug("calling instance list element equals method...for:"
    //        + this.toString() + " and " + anotherObject.toString());
    boolean annotationsEqual =
            this.annotation != null
                    && (((InstanceListElement)anotherObject).getAnnotation() != null)
                    && (this.getAnnotation()
                            .equals(((InstanceListElement)anotherObject)
                                    .getAnnotation()));
    boolean isMainSubjectEqual =
            this.isMainSubject() == ((InstanceListElement)anotherObject)
                    .isMainSubject();
    boolean isUriEqual =
            ((List<SerializableURI>)this.getData()).toString().equals(
                    ((List<SerializableURI>)((InstanceListElement)anotherObject)
                            .getData()).toString());
    //logger.debug("annotationsEqual:" + annotationsEqual + "isMainSubjectEqual:"
    //        + isMainSubjectEqual + "isUriEqual:" + isUriEqual);
    if(annotationsEqual && isMainSubjectEqual && isUriEqual)
      return true;
    else return false;
  }

  public List<SerializableURI> getData() {
    return data;
  }

  public void setData(Object data) {
    this.data=(List<SerializableURI>)data;
    
  }
}
