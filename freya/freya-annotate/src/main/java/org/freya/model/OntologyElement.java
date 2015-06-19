package org.freya.model;

import java.io.Serializable;
import java.util.List;

public interface OntologyElement extends Serializable {

	public String getFunction();

	public void setFunction(String function);

	public boolean isAlreadyAdded();

	public void setAlreadyAdded(boolean alreadyAdded);

	public Annotation getAnnotation();
	
	public void setAnnotation(Annotation annotation);

	public String getVariable();

	public void setVariable(String key);

	public boolean isMainSubject();

	public void setMainSubject(boolean value);

	public List<String> getResults();

	public void setResults(List<String> results);

	public Object getData();

	public void setData(Object data);

	public void setScore(Score score);

	public Score getScore();

	public boolean isAnswer();

	public void setAnswer(boolean isAnswer);

	public boolean equals(Object o);

}
