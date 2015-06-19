/**
 * 
 */
package org.freya.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * @author danica
 * 
 */
public class DatatypePropertyValueElement implements OntologyElement {

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

	/**
	 * 
	 */
	private static final long serialVersionUID = -4392076964893243529L;
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

	private DatatypePropertyValueIdentifier data;

	public Object getData() {
		return (DatatypePropertyValueIdentifier) data;
	}

	public void setData(Object data) {
		this.data = (DatatypePropertyValueIdentifier) data;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("");
		result.append(this.getClass().toString());
		result.append(" Is it the main subject? ").append(isMainSubject());
		if (annotation != null)
			result.append(annotation.toString());
		result.append(" Data: ").append(getData()).toString();
		if (this.variable != null)
			result.append(" \n Variable:").append(this.variable);
		result.append("\nResults:" + results);
		return result.toString();

	}

	Score score;

	public Score getScore() {
		return this.score;
	}

	public void setScore(Score score) {
		this.score = score;
	}

	public Object clone() {
		Object deepCopy = null;
		ByteArrayOutputStream byteArrOs = new ByteArrayOutputStream();
		ObjectOutputStream objOs;
		try {
			objOs = new ObjectOutputStream(byteArrOs);

			objOs.writeObject(this);

			ByteArrayInputStream byteArrIs = new ByteArrayInputStream(byteArrOs
					.toByteArray());
			ObjectInputStream objIs = new ObjectInputStream(byteArrIs);
			deepCopy = objIs.readObject();
			return deepCopy;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return deepCopy;
	}

	public boolean equals(Object anotherObject) {
		if (this == anotherObject)
			return true;
		if (!(anotherObject instanceof DatatypePropertyValueElement))
			return false;
		if ((this.annotation != null)
				&& (((DatatypePropertyValueElement) anotherObject)
						.getAnnotation() != null)
				&& (this.getAnnotation()
						.equals(((DatatypePropertyValueElement) anotherObject)
								.getAnnotation()))
				&& this.isMainSubject() == ((DatatypePropertyValueElement) anotherObject)
						.isMainSubject()
				&& this.isAlreadyAdded() == ((DatatypePropertyValueElement) anotherObject)
						.isAlreadyAdded()
				&& ((DatatypePropertyValueIdentifier) this.getData())
						.equals(((DatatypePropertyValueIdentifier) ((DatatypePropertyValueElement) anotherObject)
								.getData()).toString())
	)
			return true;
		else
			return false;
	}
}
