package org.freya.model.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.freya.model.ui.Annotation;
import org.freya.model.POC;
import org.freya.model.SemanticConcept;

@XmlRootElement
public class FreyaResponse {
    
    String repositoryId;
    String repositoryUrl;
    String sparqlQuery;
    String preciseSparql;
    Object textResponse;
    ArrayList clarifications;
    List<List<HashMap>> table;
    List<Object> refinementNodes;
    List<List<SemanticConcept>> semanticConcepts;
    ArrayList<Object> graph;
    List<Annotation> annotations = new ArrayList<Annotation>();
    ArrayList<POC> pocs;

    public List<POC> getPocs() {
      return pocs;
    }

    public void setPocs(ArrayList<POC> pocs) {
      this.pocs = pocs;
    }

    public String getPreciseSparql() {
        return preciseSparql;
    }

    public void setPreciseSparql(String preciseSparql) {
        this.preciseSparql = preciseSparql;
    }
    public List<Object> getRefinementNodes() {
        return refinementNodes;
    }

    public void setRefinementNodes(List<Object> refinementNodes) {
        this.refinementNodes = refinementNodes;
    }

    public ArrayList getClarifications() {
        return clarifications;
    }

    public void setClarifications(ArrayList clarifications) {
        this.clarifications = clarifications;
    }

    public List<List<HashMap>> getTable() {
        return table;
    }

    public void setTable(List<List<HashMap>> table) {
        this.table = table;
    }

    public ArrayList<Object> getGraph() {
        return graph;
    }

    public void setGraph(ArrayList<Object> graph) {
        this.graph = graph;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public List<List<SemanticConcept>> getSemanticConcepts() {
        return semanticConcepts;
    }

    public void setSemanticConcepts(List<List<SemanticConcept>> semanticConcepts) {
        this.semanticConcepts = semanticConcepts;
    }

    public Object getTextResponse() {
        return textResponse;
    }

    public void setTextResponse(Object textResponse) {
        this.textResponse = textResponse;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getSparqlQuery() {
        return sparqlQuery;
    }

    public void setSparqlQuery(String sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }
}
