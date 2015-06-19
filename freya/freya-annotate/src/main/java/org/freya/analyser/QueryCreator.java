package org.freya.analyser;

//import gate.clone.ql.model.ui.OntologyElement;
//import gate.clone.ql.model.ui.QueryElement;
//import gate.util.GateException;

import java.util.List;

import org.freya.model.OntologyElement;
import org.freya.model.QueryElement;

public interface QueryCreator {

  /**
   * This method creates query string from a given set of resources
   * 
   * @param resources
   * @return
   */
  public QueryElement getQueryElementFromOntologyElements(
			List<List<OntologyElement>> els) throws Exception;
}

