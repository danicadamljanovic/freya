package org.freya.rdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component("rdfRepository")
public class SesameRepositoryManager implements RepositoryManager {
	static Logger log = LoggerFactory.getLogger(SesameRepositoryManager.class);

	RepositoryConnection connection = null;

	private org.openrdf.repository.manager.RepositoryManager repositoryManager;
	private static final String PREFIX_FILE_URI = "file://";
	Repository repository;

	protected RepositoryConfig repConfig;

	@Value("${org.freya.rdf.repository.url}")
	String repositoryURL;

	@Value("${org.freya.rdf.repository.id}")
	String repositoryId;

	@Value("${org.freya.rdf.sesame.repository.username}")
	String username;

	@Value("${org.freya.rdf.sesame.repository.password}")
	String password;

	@Value("${org.freya.rdf.sesame.repository.ontology.load.pattern}")
	String ontologyLoadPattern;

	// @Value("${org.freya.rdf.owlim.config}")
	// String config;

	@Value("${org.freya.rdf.sesame.repository.localmanagerstoragedir}")
	String localRepositoryManagerDir;

	public String getRepositoryURL() {
		return repositoryURL;
	}

	public void setRepositoryURL(String repositoryURL) {
		this.repositoryURL = repositoryURL;
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public TupleQueryResult executeQuery(final String query) {
		return executeQuery(query, true);
	}

	public TupleQueryResult executeQuery(final String query,
			boolean includeInferred) {
		try {
			final TupleQuery tuple = connection.prepareTupleQuery(
					QueryLanguage.SPARQL, query);
			tuple.setIncludeInferred(includeInferred);
			return tuple.evaluate();

		} catch (final Exception e) {
			log.error("error executing query:{}", query, e);
			throw new RuntimeException(e);
		}
	}

	public void loadResources(org.springframework.core.io.Resource[] resources) {
		int numberOfFilesToLoad = resources.length;
		int currentFileNumber = 1;
		for (org.springframework.core.io.Resource resource : resources) {
			String uri;
			try {
				uri = resource.getURI().toString();
			} catch (IOException e) {
				log.error("couldn't fetch uri from resource:{}", resource, e);
				throw new RuntimeException(e);
			}
			String ext = uri.substring(uri.lastIndexOf('.') + 1);
			RDFFormat format = null;
			boolean skip = false;
			if ("owl".equalsIgnoreCase(ext) || "rdf".equalsIgnoreCase(ext)
					|| "rdfs".equalsIgnoreCase(ext)) {
				format = RDFFormat.RDFXML;
			} else if ("nt".equalsIgnoreCase(ext)) {
				format = RDFFormat.NTRIPLES;
			} else if ("n3".equalsIgnoreCase(ext)) {
				format = RDFFormat.N3;
			} else if ("ttl".equalsIgnoreCase(ext)) {
				format = RDFFormat.TURTLE;
			} else {
				log.info("Skipping uri:" + uri);
				skip = true;
			}
			if (!skip) {
				assert format != null : ext;
				// log.info("Started loading {} ({}/{})", uri,
				// currentFileNumber, numberOfFilesToLoad);
				try {
					connection.add(resource.getInputStream(),
							"http://example.org#", format);
				} catch (Exception /*
									 * RDFParseException | RepositoryException |
									 * IOException
									 */e) {
					log.error(
							"There was a problem with this uri, uri not loaded:{}",
							uri, e);
					throw new RuntimeException(e);
				}
				log.info("{} loaded.", uri);
			}
			currentFileNumber++;
		}
		try {
			connection.commit();
		} catch (RepositoryException e) {
			log.error("couldn't commit repo changes", e);
			throw new RuntimeException(e);
		}
	}

	public void loadData() throws IOException, QueryEvaluationException {
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		org.springframework.core.io.Resource[] resources = resolver
				.getResources(ontologyLoadPattern);
		loadResources(resources);
	}

	/**
	 * load ontologies from URLs
	 */
	void initLocalRepository() {
		// InputStream configFileInputStream;
		try {
			// ResourcePatternResolver resolver = new
			// PathMatchingResourcePatternResolver();
			// org.springframework.core.io.Resource[] resources = resolver
			// .getResources(config);
			// configFileInputStream = resources[0].getInputStream();

			// repositoryManager = new LocalRepositoryManager(new File(
			// localRepositoryManagerDir));
			//
			// repositoryManager.initialize();

			repository = new SailRepository(
					new ForwardChainingRDFSInferencer(new MemoryStore()));
			repository.initialize();

			log.debug("Local repository initialised, ontologies not yet added");
		} catch (Exception e) {
			log.error("error loading local repository", e);
			throw new RuntimeException(e);
		}

		// final Graph graph = new GraphImpl();
		// RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
		// RDFHandler handler = new RDFHandler() {
		// public void endRDF() throws RDFHandlerException {
		// }
		//
		// public void handleComment(String arg0) throws RDFHandlerException {
		// }
		//
		// public void handleNamespace(String arg0, String arg1)
		// throws RDFHandlerException {
		// }
		//
		// public void handleStatement(Statement arg0)
		// throws RDFHandlerException {
		// graph.add(arg0);
		// }
		//
		// public void startRDF() throws RDFHandlerException {
		// }
		// };
		// parser.setRDFHandler(handler);

		// try {
		// parser.parse(configFileInputStream, "http://example.org#");
		// Iterator<Statement> iter = graph.match(null, RDF.TYPE, new URIImpl(
		// "http://www.openrdf.org/config/repository#Repository"));
		// org.openrdf.model.Resource repNode = null;
		// if (iter.hasNext()) {
		// Statement st = iter.next();
		// repNode = st.getSubject();
		// }
		// repConfig = RepositoryConfig.create(graph, repNode);
		// repositoryManager.addRepositoryConfig(repConfig);

		// } catch (Exception e) {
		// log.error("error parsing file:{}", config, e);
		// throw new RuntimeException(e);
		// }
	}

	@PostConstruct
	public void afterPropertiesSet() throws RepositoryConfigException,
			RepositoryException {
		log.info("afterPropertiesSet() - initialising");

		boolean loadLocal = false;
		if (repositoryURL != null && !repositoryURL.isEmpty()) {
			if (username != null && password != null) {
				repository = new HTTPRepository(repositoryURL, repositoryId);
				((HTTPRepository) repository).setUsernameAndPassword(username,
						password);
			} else {
				throw new RuntimeException(
						"Not valid username & password for the http repository");
			}
		} else {
			loadLocal = true;
			initLocalRepository();
			if (repositoryManager == null) {
				throw new RuntimeException(
						"couldn't create local repository manager");
			}
			// repository = repositoryManager.getRepository(repositoryId);
		}

		try {
			repository.initialize();
			connection = repository.getConnection();
			if (connection == null) {
				throw new RuntimeException("Connection NOT established!");
			} else {
				log.debug("Connection established");
			}

			if (loadLocal)
				loadData();
		} catch (final RepositoryException e) {
			// log.error("Not able to connect to the Repository - repositoryId:{}, repositoryURL:{}",
			// repositoryId, repositoryURL, e);
			throw new RuntimeException(e);
		} catch (final QueryEvaluationException e) {
			log.error("Problem evaluating SPARQL query.");
			throw new RuntimeException(e);
		} catch (IOException e) {
			log.error("Problem reading ontology files from the file system.");
			throw new RuntimeException(e);
		}
		log.info(
				"afterPropertiesSet() - Successfully initialised Sesame Repository using impl: {}",
				repository.getClass().getName());
	}

	/**
	 * This method is called to reload {@link #preloadInputStream} to
	 * repository.
	 * 
	 * @param sourceUri
	 *            A string of source file URI.
	 * @throws RepositoryException
	 */
	public void loadByInputStream(String sourceUri,
			InputStream preloadInputStream) throws RepositoryException {
		if (this.repositoryURL == null) {
			throw new NullPointerException(
					"Please set repositoryURL where you want to upload.");
		}
		if (this.repositoryId == null) {
			throw new NullPointerException("Please set a repositoryId.");
		}

		if (this.repository.getConnection() == null) {
			throw new NullPointerException("Please initialize SparqlUtils");
		}

		RDFFormat format = null;
		boolean skip = false;
		String ext = sourceUri.substring(sourceUri.lastIndexOf('.') + 1);
		if ("owl".equalsIgnoreCase(ext) || "rdf".equalsIgnoreCase(ext)
				|| "rdfs".equalsIgnoreCase(ext)) {
			format = RDFFormat.RDFXML;
		} else if ("nt".equalsIgnoreCase(ext)) {
			format = RDFFormat.NTRIPLES;
		} else if ("n3".equalsIgnoreCase(ext)) {
			format = RDFFormat.N3;
		} else if ("ttl".equalsIgnoreCase(ext)) {
			format = RDFFormat.TURTLE;
		} else {
			log.info("Skipping uri:" + sourceUri);
			skip = true;
		}

		if (skip) {
			log.warn("Skipped unknown source: " + sourceUri);
			return;
		}

		try {
			log.info("Loading:" + sourceUri);
			Resource resource = getFileResourceFromUri(sourceUri);
			repository.getConnection().clear(resource);
			repository.getConnection().add(preloadInputStream,
					"http://example.org#", format, resource);
			repository.getConnection().commit();
			// this.conn.clear(resource);
			// this.conn.add(preloadInputStream, "http://example.org#", format,
			// resource);
			// this.conn.commit();
		} catch (IOException e) {
			log.error(e.getMessage());
		} catch (RDFParseException e) {
			log.error(e.getMessage());
		} catch (RepositoryException e) {
			log.error(e.getMessage());
		} finally {
			try {
				preloadInputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private Resource getFileResourceFromUri(String sourceUri) {
		if (sourceUri == null || "".equals(sourceUri)) {
			throw new NullPointerException("Resource URI cannot be empty!");
		}

		int beginIndex = sourceUri.lastIndexOf("/");
		if (beginIndex != -1) {
			sourceUri = PREFIX_FILE_URI + sourceUri.substring(beginIndex + 1);
		}

		return new URIImpl(sourceUri);
	}

	@PreDestroy
	public void destroy() throws Exception {
		if (repository != null)
			repository.shutDown();
		if (repositoryManager != null)
			repositoryManager.shutDown();
		if (connection != null)
			connection.close();
	}
}
