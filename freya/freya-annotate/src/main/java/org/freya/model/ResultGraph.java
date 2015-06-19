package org.freya.model;

/**
 * @author danica
 */
import java.util.ArrayList;
import java.util.List;

public class ResultGraph {

	List data = new ArrayList();
	String type;
	Integer id;
	private Boolean mainSubject;

	public Boolean isMainSubject() {
		return mainSubject;
	}

	public void setMainSubject(boolean mainSubject) {
		this.mainSubject = mainSubject;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List getAdjacencies() {
		return adjacencies;
	}

	public void setAdjacencies(List adjacencies) {
		this.adjacencies = adjacencies;
	}

	List adjacencies = new ArrayList();

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List getData() {
		return data;
	}

	public void setData(List data) {
		this.data = data;
	}

	String URI;

	public String getURI() {
		return URI;
	}

	public void setURI(String uri) {
		URI = uri;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("URI:").append(this.URI).append(
				" ID:").append(this.id).append(
				" ADJACENCIES:" + this.getAdjacencies()).append("TYPE:")
				.append(this.getType()).append("\n");
		return result.toString();
	}
}
