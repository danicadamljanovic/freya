/**
 * 
 */
package org.freya.model;

import java.util.List;

/**
 * @author danica
 * 
 */
public class ClassElement implements OntologyElement {

	/** max min sum */
	String function;

	Score score;

	boolean isAnswer;

	public boolean isAnswer() {
		return isAnswer;
	}

	public void setAnswer(boolean isAnswer) {
		this.isAnswer = isAnswer;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	/*
	 * this flag indicates whether the element is already in the list of
	 * elements and whether it should be ignored except for the function; this
	 * happens when some existing elements are modified with for example max min
	 * etc
	 */
	boolean alreadyAdded;

	public boolean isAlreadyAdded() {
		return alreadyAdded;
	}

	public void setAlreadyAdded(boolean alreadyAdded) {
		this.alreadyAdded = alreadyAdded;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8004498024299257803L;
	public List<String> results = null;
	public boolean mainSubject = false;
	private SerializableURI data = null;
	private String variable = null;

	// public OntologyElement closestOntologyConcept;
	Annotation annotation = new Annotation();

	public void setAnnotation(Annotation annotation) {
    this.annotation = annotation;
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

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public SerializableURI getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = (SerializableURI) data;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("");
		result.append(this.getClass().getSimpleName().toString());
		result.append(" Is it the main subject? ").append(isMainSubject());
		if (annotation != null)
			result.append(annotation.toString());
		result.append("\nData/URI: ").append(this.data).toString();
		if (this.variable != null)
			result.append(" \n Variable:").append(this.variable);
		else
			result.append(" Variable: null");
		result.append("\nResults:" + results);
		return result.toString();

	}

	public Annotation getAnnotation() {
		return annotation;
	}

	public Score getScore() {
		return this.score;
	}

	public void setScore(Score score) {
		this.score = score;
	}

	@Override
	public boolean equals(Object anotherObject) {
		boolean isEqual = false;
		if (this == anotherObject)
			return true;
		if (!(anotherObject instanceof ClassElement))
			return false;
		
		if ((this.annotation != null)
				&& (((ClassElement) anotherObject).getAnnotation() != null)
				&& (this.getAnnotation().equals(((ClassElement) anotherObject)
						.getAnnotation()))
				&& this.isMainSubject() == ((ClassElement) anotherObject)
						.isMainSubject()
				&& this.isAlreadyAdded() == ((ClassElement) anotherObject)
						.isAlreadyAdded()
				&& this.getData().toString().equals(
						((ClassElement) anotherObject).getData().toString()))
			isEqual = true;
		return isEqual;

	}

}
