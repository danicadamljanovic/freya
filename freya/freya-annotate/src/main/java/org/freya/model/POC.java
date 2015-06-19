package org.freya.model;

import java.util.List;

/**
 * representation of Potential Ontology Concept
 * 
 * @author danica
 * 
 */
public class POC {

	Annotation annotation = new Annotation();

	public Annotation getAnnotation() {
		return annotation;
	}

	public void setAnnotation(Annotation annotation) {
		this.annotation = annotation;
	}

	POC head;

	MainSubject mainSubject;
	List<String> modifiers;

	public POC getHead() {
		return head;
	}

	public void setHead(POC head) {
		this.head = head;
	}

	public List<String> getModifiers() {
		return modifiers;
	}

	public void setModifiers(List<String> modifiers) {
		this.modifiers = modifiers;
	}

	public MainSubject getMainSubject() {
		return mainSubject;
	}

	public void setMainSubject(MainSubject mainSubject) {
		this.mainSubject = mainSubject;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer("");
		buffer.append("POC:" + getAnnotation().toString());
		return buffer.toString();
	}
}
