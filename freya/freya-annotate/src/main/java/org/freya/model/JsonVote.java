package org.freya.model;

/**
 * this is a variant of Vote used to prepare data for Json
 * 
 * @author danica
 * 
 */
public class JsonVote {
	String vote;
	String candidate;
	String id;
	String function;

	public JsonVote(String text, String vote, String id, String function) {
		this.id = id;
		this.vote = vote;
		this.candidate = text;
		this.function = function;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getVote() {
		return vote;
	}

	public void setVote(String vote) {
		this.vote = vote;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCandidate() {
		return candidate;
	}

	public void setCandidate(String candidate) {
		this.candidate = candidate;
	}

	public String toString() {
		StringBuffer s = new StringBuffer("");
		s.append("JsonSuggestion:\nVote:").append(" id:").append(id).append(
				" Vote:").append(vote).append("Candidate:" + candidate).append(
				"\n");
		return s.toString();
	}
}
