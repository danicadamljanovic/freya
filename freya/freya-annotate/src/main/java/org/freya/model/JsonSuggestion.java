package org.freya.model;

import java.util.List;

public class JsonSuggestion {
	
	List<JsonVote> votes;
	String id;
	String text;
	String function;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<JsonVote> getVotes() {
		return votes;
	}

	public void setVotes(List<JsonVote> votes) {
		this.votes = votes;
	}

	public String toString() {
		StringBuffer b = new StringBuffer("Json Suggestion: ");
		b.append("id:").append(this.id);
		b.append("\ntext:").append(text);
		b.append("\nfunction:").append(function);
		b.append("\nvotes:" + votes.toString());
		return b.toString();
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

}
