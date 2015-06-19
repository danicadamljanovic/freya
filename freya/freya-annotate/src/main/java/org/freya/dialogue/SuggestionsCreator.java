package org.freya.dialogue;

import java.util.List;

import org.freya.model.POC;
import org.freya.model.SemanticConcept;
import org.freya.model.SuggestionKey;
import org.freya.model.Vote;

public interface SuggestionsCreator {
  public List<Vote> generateVotes(SuggestionKey key, POC poc);
  public List<Vote> generateGenericVotes(SuggestionKey key, POC poc,
          boolean addNone, Integer max, List<SemanticConcept> toSkip) ;
  public List<Vote> generateGenericVotes(SuggestionKey key, POC poc);
  public List<Vote> generateVotes(SuggestionKey key, POC poc, boolean addNone,
          List<SemanticConcept> toSkip);
}
