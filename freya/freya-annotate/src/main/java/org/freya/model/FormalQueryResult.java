package org.freya.model;

//import gate.clone.ql.model.ResultTree;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author danica
 * 
 */
public class FormalQueryResult {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	List<String> uris = new ArrayList<String>();
	/* holding results */
	List<List<String>> data;

	/* holding executed sparql  query which is a context query */
	String query;
/**
 * this one returns only concise results, no context
 */
	String preciseSparqlQuery;
	
	public String getPreciseSparqlQuery() {
  return preciseSparqlQuery;
}

public void setPreciseSparqlQuery(String preciseSparqlQuery) {
  this.preciseSparqlQuery = preciseSparqlQuery;
}

//  ResultTree resultTree;
//
//	public ResultTree getResultTree() {
//		return resultTree;
//	}
//
//	public void setResultTree(ResultTree resultTree) {
//		this.resultTree = resultTree;
//	}

	public List<List<String>> getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = (List<List<String>>) data;
	}

	/**
	 * Show only data part
	 * 
	 * @return
	 */
	public String getDataToString() {
		if (this.data != null)
			return data.toString();// new
		// StringBuffer(this.data).append("\n").toString();
		else
			return null;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new StringBuffer(this.getClass().toString()).append(" Query:\n")
				.append(this.query).append(" Precise query:\n")
        .append(this.preciseSparqlQuery).append(" Result data: ").append(this.data)
				.append("\n")
				.toString();
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * @return the uris
	 */
	public List<String> getUris() {
		return uris;
	}

	/**
	 * @param uris
	 *            the uris to set
	 */
	public void setUris(List<String> uris) {
		this.uris = uris;
	}
}
