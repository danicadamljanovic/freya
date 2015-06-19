package org.freya.dialogue;

import org.freya.model.NoneElement;
import org.freya.model.SemanticConcept;
import org.freya.model.Vote;

public class VoteGenerator {
  
  public static Vote generateNoneVote(long id){
  // add none to the options
  Vote vote = new Vote();

  vote.setId(id);
  SemanticConcept scEl = new SemanticConcept();
  NoneElement noneElement = new NoneElement();
  scEl.setOntologyElement(noneElement);
  vote.setCandidate(scEl);
  vote.setVote(-1.0);
  return vote;
  
  }
  
}
