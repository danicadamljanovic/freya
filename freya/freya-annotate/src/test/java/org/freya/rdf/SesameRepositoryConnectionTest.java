package org.freya.rdf;

import org.junit.Test;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.http.HTTPRepository;

public class SesameRepositoryConnectionTest {

	 @Test
	    public void testConnection(){
		String  repositoryURL = "http://localhost:8080/openrdf-sesame";
		String repositoryId = "mooney";
		Repository repository = new HTTPRepository(repositoryURL, repositoryId);
		repository.initialize();
		RepositoryConnection conn=repository.getConnection();
	}
	
}
