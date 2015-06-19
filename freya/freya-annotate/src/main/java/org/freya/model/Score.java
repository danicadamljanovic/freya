package org.freya.model;

import java.io.Serializable;

public class Score implements Serializable {

	public double getSimilarityScore() {
		return similarityScore;
	}

	public void setSimilarityScore(double similarityScore) {
		this.similarityScore = similarityScore;
	}

	public Double getSpecificityScore() {
		return specificityScore;
	}

	public void setSpecificityScore(double specificityScore) {
		this.specificityScore = specificityScore;
	}
    private double solrScore;
	
	private double specificityScore;

	private double similarityScore;

	private double distanceScore;

	public double getDistanceScore() {
		return distanceScore;
	}

	public void setDistanceScore(double distanceScore) {
		this.distanceScore = distanceScore;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer result = new StringBuffer(this.getClass().getSimpleName()
				.toString());
		result.append("\nSpecificity Score: ").append(this.specificityScore)
				.append("\nSimilarity Score: ").append(this.similarityScore)
				.append("\nDistance Score: ").append(this.distanceScore);
		return result.toString();
	}

	public boolean equals(Score anotherObject) {
		if ((this.getDistanceScore() == anotherObject.getDistanceScore())
				&& this.getSimilarityScore() == anotherObject
						.getSimilarityScore()
				&& this.getSpecificityScore() == anotherObject
						.getSpecificityScore())
			return true;
		else
			return false;
	}

	public double getSolrScore() {
		return solrScore;
	}

	public void setSolrScore(double solrScore) {
		this.solrScore = solrScore;
	}
}
