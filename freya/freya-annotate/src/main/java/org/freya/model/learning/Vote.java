package org.freya.model.learning;

public class Vote {
	// this is usually URI or DatatyepPropertyValueIdentifier
	Object identifier;
	
	volatile long id;
	volatile double score;
	volatile String function;
	public Object getIdentifier() {
		return identifier;
	}
	public void setIdentifier(Object identifier) {
		this.identifier = identifier;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public String getFunction() {
		return function;
	}
	public void setFunction(String function) {
		this.function = function;
	}
}
