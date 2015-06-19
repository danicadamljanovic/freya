package org.freya.model.learning;

/**
 * this bean holds one 'row' used by learning model; it is more primitive than
 * using SuggestionPair and is used for testing
 * 
 * @author danica
 */
public class LearningItem {
  
  Integer baselineCorrectnessPerQuestion;

  String query;
  /* this is poc or oc */
  String unknownTerm;
  
  String coc;
  
  String function;
  
  String candidateURI;

  public Integer getBaselineCorrectnessPerQuestion() {
    return baselineCorrectnessPerQuestion;
  }

  public void setBaselineCorrectnessPerQuestion(
          Integer baselineCorrectnessPerQuestion) {
    this.baselineCorrectnessPerQuestion = baselineCorrectnessPerQuestion;
  }
  
  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getUnknownTerm() {
    return unknownTerm;
  }

  public void setUnknownTerm(String unknownTerm) {
    this.unknownTerm = unknownTerm;
  }

  public String getCoc() {
    return coc;
  }

  public void setCoc(String coc) {
    this.coc = coc;
  }

  public String getFunction() {
    return function;
  }

  public void setFunction(String function) {
    this.function = function;
  }

  public String getCandidateURI() {
    return candidateURI;
  }

  public void setCandidateURI(String candidateURI) {
    this.candidateURI = candidateURI;
  }
  
}
