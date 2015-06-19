package org.freya.model;

/**
 * this class preserves scores which are calculated initially before users
 * select anything and also recalculated with the learning function whenever the
 * user makes a click
 * 
 * @author danica
 */
public class Vote {
  double vote;

  SemanticConcept candidate;

  long id;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public double getVote() {
    return vote;
  }

  public void setVote(double vote) {
    this.vote = vote;
  }

  public SemanticConcept getCandidate() {
    return candidate;
  }

  public void setCandidate(SemanticConcept candidate) {
    this.candidate = candidate;
  }

  public String toString() {
    StringBuffer s = new StringBuffer("");
    s.append("Vote id:").append(id).append(" Score:").append(vote);
    if(candidate != null)
      s.append("\nCandidate:" + candidate.toString()).append("\n");
    return s.toString();
  }
}
