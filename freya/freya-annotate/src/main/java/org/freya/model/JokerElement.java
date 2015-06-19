/**
 * 
 */
package org.freya.model;

import java.util.List;

/**
 * @author danica
 * 
 */
public class JokerElement implements OntologyElement {

	boolean isAnswer;

	public boolean isAnswer() {
		return isAnswer;
	}

	public void setAnswer(boolean isAnswer) {
		this.isAnswer = isAnswer;
	}

	/** max min sum */
	String function;

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

	public boolean mainSubject = false;
	public List<String> results;
	Annotation annotation = new Annotation();

	public Annotation getAnnotation() {
		return annotation;
	}

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

	private String variable;

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	private Object data;

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	// whether it's a class or property joker
	private String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean equals(Object anotherObject) {
		if (this == anotherObject)
			return true;
		if (!(anotherObject instanceof JokerElement))
			return false;
		if ((this.annotation != null)
				&& (((JokerElement) anotherObject).getAnnotation() != null)
				&& (this.getAnnotation().equals(((JokerElement) anotherObject)
						.getAnnotation()))
				&& this.isMainSubject() == ((JokerElement) anotherObject)
						.isMainSubject()
				&& this.isAlreadyAdded() == ((JokerElement) anotherObject)
						.isAlreadyAdded()
		)
			return true;
		else
			return false;
	}

	public String toString() {
		StringBuffer buff = new StringBuffer((this.getClass().getSimpleName()
				.toString()));
		buff.append("\nIs it the main subject? ").append(isMainSubject());
		buff.append("\nVariable:");
		buff.append("\nAnnotation:"+this.annotation);
		if (this.variable != null)
			buff.append(this.variable);
		buff.append("\nResults:" + results);
		buff.append("\nType: " + this.type);

		buff.append("\n");
		return buff.toString();
	}

	Score score;

	public Score getScore() {
		return this.score;
	}

	public void setScore(Score score) {
		this.score = score;
	}

}
