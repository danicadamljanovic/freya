package org.freya.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MainSubject {

	/*
	 * defines the priority for the main subject: for example, if we want to
	 * give priority to the main subject wrt the ontology concepts e.g. in cases
	 * of 'how long' when long is a mountain
	 */
	Integer priority;

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	
}
