package org.freya.model;

import java.util.List;

/**
 * modeling suggestions which are used for 1. resolving ambiguities 2. mapping
 * pocs to ocs
 * 
 * this class contains key for modeling suggestions: its attributes are used to
 * model suggestions
 * 
 * @author danica
 * 
 */
public class SuggestionKey {
	/* stringToClarify */
	String text;
	List<SemanticConcept> nearestNeighbours;

	public String getText() {
		return text;
	}

	public void setText(String stringToClarify) {
		this.text = stringToClarify;
	}

	public List<SemanticConcept> getNearestNeighbours() {
		return nearestNeighbours;
	}

	public void setNearestNeighbours(List<SemanticConcept> nearestNeighbours) {
		this.nearestNeighbours = nearestNeighbours;
	}

	public String toString() {
		StringBuffer s = new StringBuffer("Suggestion Key:");
		s.append("Text:");
		s.append(text).append("\n");
		s.append("Nearest Neighbours:");
		if (nearestNeighbours != null)
			s.append(nearestNeighbours.toString());
		return s.toString();
	}

}
