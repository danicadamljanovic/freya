package org.freya.rdf;

import org.openrdf.query.TupleQueryResult;


public interface RepositoryManager {

    TupleQueryResult executeQuery(String query) throws Exception;

    String getRepositoryId();

}
