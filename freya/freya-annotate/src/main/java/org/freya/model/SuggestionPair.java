package org.freya.model;

import java.util.List;

public class SuggestionPair {

	SuggestionKey key;

	public SuggestionKey getKey() {
		return key;
	}

	public void setKey(SuggestionKey key) {
		this.key = key;
	}

	public List<Vote> getVote() {
		return vote;
	}

	public void setVote(List<Vote> vote) {
		this.vote = vote;
	}

	List<Vote> vote;

	Object currentDialogSubject;
	
	public Object getCurrentDialogSubject() {
		return currentDialogSubject;
	}

	public void setCurrentDialogSubject(Object currentDialogSubject) {
		this.currentDialogSubject = currentDialogSubject;
	}
	
	public String toString() {
		StringBuffer s = new StringBuffer("");
		s.append("Key:");
		if (key != null)
			s.append(key.toString()).append("\n");
		s.append("Votes:");
		if (s != null)
			s.append(vote.toString());
		return s.toString();
	}
}
