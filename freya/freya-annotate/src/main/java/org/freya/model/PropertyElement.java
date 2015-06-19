/**
 * 
 */
package org.freya.model;

import java.util.List;

/**
 * @author danica
 * 
 */
public class PropertyElement implements OntologyElement {

	boolean isAnswer;

	public boolean isAnswer() {
		return isAnswer;
	}

	public void setAnswer(boolean isAnswer) {
		this.isAnswer = isAnswer;
	}

	/** max min sum */
	String function;

	/**
	 * governor is set for datatype properties and refers to the domain of this
	 * property
	 */
	OntologyElement governor;

	public OntologyElement getGovernor() {
		return this.governor;
	}

	public void setGovernor(OntologyElement ontologyElement) {
		this.governor = ontologyElement;
	}

	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		this.range = range;
	}

	/* uri of the property range */
	String range;

	String domain;
	
	public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
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

	public boolean mainSubject = false;
	public List<String> results;
	boolean isDatatypeProperty;

	public boolean isDatatypeProperty() {
		return isDatatypeProperty;
	}

	public void setDatatypeProperty(boolean isDatatypeProperty) {
		this.isDatatypeProperty = isDatatypeProperty;
	}

	Annotation annotation = new Annotation();

	public Annotation getAnnotation() {
		return annotation;
	}

	public void setAnnotation(Annotation annotation) {
		this.annotation = annotation;
	}

	// public OntologyElement closestOntologyConcept;
	//
	// public OntologyElement getClosestOntologyConcept() {
	// return closestOntologyConcept;
	// }
	//
	// public void setClosestOntologyConcept(OntologyElement
	// closestOntologyElement) {
	// this.closestOntologyConcept = closestOntologyElement;
	// }

	public void setData(SerializableURI data) {
		this.data = data;
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

	private String variable = null;

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	private SerializableURI data;

	private boolean inverseProperty;

	public boolean isInverseProperty() {
		return inverseProperty;
	}

	public void setInverseProperty(boolean inverseProperty) {
		this.inverseProperty = inverseProperty;
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
		StringBuffer result = new StringBuffer(this.getClass().getSimpleName()
				.toString());
		result.append(" Is it the main subject? ").append(isMainSubject());
		result.append("\nProperty URI: ").append(this.data);
		if (annotation != null)
			result.append(getAnnotation().toString());
		if (score != null)
			result.append(getScore().toString()).append("\nInverse property:")
					.append(isInverseProperty()).append("\nVariable:").append(
							this.variable);
		result.append("\nResults:" + results);
		result.append("\nRange:" + range);
	  result.append("\nIsDatatypeproperty?:" + isDatatypeProperty);
		return result.toString();
	}

	Score score;

	public Score getScore() {
		return this.score;
	}

	public void setScore(Score score) {
		this.score = score;
	}

	@Override
	public boolean equals(Object anotherObject) {
		if (this == anotherObject)
			return true;
		if (!(anotherObject instanceof PropertyElement))
			return false;
		if ((this.annotation != null)
				&& (((PropertyElement) anotherObject).getAnnotation() != null)
				&& (this.getAnnotation()
						.equals(((PropertyElement) anotherObject)
								.getAnnotation()))
				&& this.isMainSubject() == ((PropertyElement) anotherObject)
						.isMainSubject()
				&& this.isAlreadyAdded() == ((PropertyElement) anotherObject)
						.isAlreadyAdded()
				&& this.getData().toString().equals(
						((PropertyElement) anotherObject).getData().toString()))
			return true;
		else
			return false;
	}

}
