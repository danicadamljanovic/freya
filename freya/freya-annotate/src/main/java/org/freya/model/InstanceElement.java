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
public class InstanceElement implements OntologyElement {
  private static final Log logger = LogFactory.getLog(InstanceElement.class);

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

  public void setData(SerializableURI data) {
    this.data = data;
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

  private SerializableURI data;

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

  public SerializableURI getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = (SerializableURI)data;
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer s =
            new StringBuffer(this.getClass().getSimpleName().toString());
    s.append(" Is it the main subject? ").append(isMainSubject());
    if(annotation != null) s.append(" ").append(annotation.toString());
    s.append(" Instance URI: ").append(this.data).append(" Class URI: ")
            .append(this.classURI).append(" Variable:")
            .append(this.variable);
    s.append(" Results:" + results);
    s.append(" Class URIs");
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
    if(!(anotherObject instanceof InstanceElement)) return false;
    //logger.debug("calling instance element equals method...for:"
    //        + this.toString() + " and " + anotherObject.toString());
    boolean annotationsEqual =
            this.annotation != null
                    && (((InstanceElement)anotherObject).getAnnotation() != null)
                    && (this.getAnnotation()
                            .equals(((InstanceElement)anotherObject)
                                    .getAnnotation()));
    boolean isMainSubjectEqual =
            this.isMainSubject() == ((InstanceElement)anotherObject)
                    .isMainSubject();
    boolean isUriEqual =
            ((SerializableURI)this.getData()).toString().equals(
                    ((SerializableURI)((InstanceElement)anotherObject)
                            .getData()).toString());
   // logger.debug("annotationsEqual:" + annotationsEqual + "isMainSubjectEqual:"
    //        + isMainSubjectEqual + "isUriEqual:" + isUriEqual);
    if(annotationsEqual && isMainSubjectEqual && isUriEqual)
      return true;
    else return false;
  }
}
