package org.freya.model;

//import gate.clone.ql.model.ui.Annotation;
//import gate.clone.ql.model.ui.JokerElement;
//import gate.clone.ql.model.ui.OntologyElement;
//import gate.clone.ql.model.ui.Score;

import java.util.List;

import org.freya.util.FreyaConstants;

public class NoneElement implements OntologyElement {
  Annotation annotation = new Annotation();

  String id = "";

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  boolean isAnswer;

  public boolean isAnswer() {
    return isAnswer;
  }

  public void setAnswer(boolean isAnswer) {
    this.isAnswer = isAnswer;
  }

  /** max min sum */
  String function;

  Score score;

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

  String data = FreyaConstants.CLARIFICATION_OPTIONS_NONE;

  public Annotation getAnnotation() {
    return annotation;
  }

  public void setAnnotation(Annotation ann) {
    this.annotation = ann;
  }

  public Object getData() {
    // TODO Auto-generated method stub
    return this.data;
  }

  public List<String> getResults() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getVariable() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isMainSubject() {
    // TODO Auto-generated method stub
    return false;
  }

  public void setData(Object data) {
    // TODO Auto-generated method stub
  }

  public void setMainSubject(boolean value) {
    // TODO Auto-generated method stub
  }

  public void setResults(List<String> results) {
    // TODO Auto-generated method stub
  }

  public void setVariable(String key) {
    // TODO Auto-generated method stub
  }

  public Score getScore() {
    return score;
  }

  public void setScore(Score score) {
    this.score = score;
  }

  public boolean equals(Object anotherObject) {
    if(this == anotherObject) return true;
    if(!(anotherObject instanceof NoneElement)) return false;
    boolean annEqual = false;
    boolean idEqual = false;
    if((this.annotation != null)
            && (((NoneElement)anotherObject).getAnnotation() != null)
            && (this.getAnnotation().equals(((NoneElement)anotherObject)
                    .getAnnotation()))) annEqual = true;
    if(this.getId().toString().equals(
            ((NoneElement)anotherObject).getId().toString())) idEqual = true;
    if(idEqual && annEqual)
      return true;
    else return false;
  }

  public String toString() {
    StringBuffer buff =
            new StringBuffer((this.getClass().getSimpleName().toString()));
    if(annotation != null) buff.append(annotation.toString());
    buff.append(" Id:").append(id);
    buff.append("\n");
    return buff.toString();
  }
}
