package org.freya.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.Question;
import org.freya.model.service.FreyaResponse;
import org.freya.util.ProfilerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FreyaService {

	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	FreyaServiceHelper helper;

	/**
	 * This method is called for loading ontology files to RDF repository.
	 * Client should set ontology source data in request body as InputStream.
	 * Use this method if you want to upload files from your local dir for
	 * example.
	 * 
	 * @param repositoryURL
	 *            Sesame repository server.
	 * @param repositoryId
	 *            Sesame repository ID.
	 * @param source
	 *            Ontology source name that needs to load into repository.
	 * @return String message.
	 */
	@ResponseBody
	@RequestMapping(value = "/load", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String load(@RequestParam(value = "url") String repositoryURL,
			@RequestParam(value = "id") String repositoryId,
			@RequestParam(value = "src") String source,
			HttpServletRequest request) {

		logger.info(String.format("params: { url => %s, id => %s, src => %s }",
				repositoryURL, repositoryId, source));

		if (StringUtils.isEmpty(repositoryURL)
				|| StringUtils.isEmpty(repositoryId)) {
			return "query params: url, id and src cannot be empty!";
		}
		try {
			helper.load(repositoryURL, repositoryId, source,
					request.getInputStream());
		} catch (Exception e) {
			logger.error(e);
			return "Exception: " + e.getLocalizedMessage();
		}

		return String.format("Updating from %s to %s.", source, repositoryURL);
	}

	/**
	 * This method is called for loading ontology files to RDF repository. The
	 * ontology directory is provided through source parameter.
	 * 
	 * @param repositoryURL
	 *            Sesame repository server.
	 * @param repositoryId
	 *            Sesame repository ID.
	 * @param source
	 *            Ontology source name that needs to load into repository.
	 * @return String message.
	 */
	@ResponseBody
	@RequestMapping(value = "/loadBulk", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String loadBulk(@RequestParam(value = "url") String repositoryURL,
			@RequestParam(value = "id") String repositoryId,
			@RequestParam(value = "src") String source,
			HttpServletRequest request) {

		logger.info(String.format("params: { url => %s, id => %s, src => %s }",
				repositoryURL, repositoryId, source));

		if (StringUtils.isEmpty(repositoryURL)
				|| StringUtils.isEmpty(repositoryId)) {
			return "query params: url, id and src cannot be empty!";
		}
		try {
			File initialFolder = new File(source);
			if (!initialFolder.isDirectory()) {
				return String.format(
						"Specified src parameter is not a directory: %s",
						source);
			}
			Collection<File> files = FileUtils.listFiles(initialFolder, null,
					true);
			logger.info("Attempting to load:" + files.size() + " files.");
			for (File file : files) {
				InputStream targetStream = new FileInputStream(file);
				helper.load(repositoryURL, repositoryId, file.toURI()
						.toString(), targetStream);
			}
		} catch (Exception e) {
			logger.error(e);
			return "Exception: " + e.getLocalizedMessage();
		}

		return String.format("Updating from %s to %s.", source, repositoryURL);
	}

	/**
	 * this method is called for the query processing at the click of Submit
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/getSparql", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> getSparql(
			@RequestParam(value = "query") String query,
			HttpServletRequest request

	) throws Exception {
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();
		List<FreyaResponse> responses = helper
				.processQuestionAutomatically(query);

		long stopTime = System.currentTimeMillis();
		long runTime = stopTime - startTime;
		logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
				null));
		return responses;
	}

	/**
	 * this method is called for the query processing at the click of Submit
	 * 
	 * @return Test with:
	 * 
	 *         <pre>
	 * curl --data "query=What is a city?" http://localhost:8080/freya/service/ask
	 * </pre>
	 * @param query
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/ask", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> ask(@RequestParam(value = "query") String query,
			HttpServletRequest request) throws Exception {
		List<FreyaResponse> responses = new ArrayList<FreyaResponse>();
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();
		List<Question> questions = helper.processQuestion(query);
		session.setAttribute(query, questions);
		for (Question question : questions) {
			FreyaResponse response = helper.dialogOrNot(question, query,
					session);
			responses.add(response);
		}
		long stopTime = System.currentTimeMillis();
		long runTime = stopTime - startTime;
		logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
				null));
		return responses;
	}

	/**
	 * this method is called when the user selects one of the suggestions
	 * 
	 * @param query
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/refine", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> refine(
			@RequestParam(value = "query") String query,
			@RequestParam(value = "voteId") String[] voteIds,

			HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession(true);
		List responses = new ArrayList();
		FreyaResponse response;
		if (voteIds != null) {
			response = helper.refineQuestion(query, voteIds, session);
			responses.add(response);
			if (responses.size() > 1)
				logger.info("Refining based on several sentences input is not supported yet.");
		} else
			throw new Exception("Vote id is null! This should never happen!");
		return responses;
	}

	/**
	 * this method is called for the query processing at the click of Submit
	 * when freya is in auto mode; it will similate that all the first options
	 * are being clicked and generate some result based on them
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/askNoDialog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> askNoDialog(
			@RequestParam(value = "query") String query,
			HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();

		List<FreyaResponse> responses = helper
				.processQuestionAutomatically(query);
		long stopTime = System.currentTimeMillis();
		long runTime = stopTime - startTime;
		logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
				null));
		return responses;
	}

	/**
	 * this method sets true for forceDialog
	 * 
	 * @param query
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/askForceDialog", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> askForceDialog(
			@RequestParam(value = "query") String query,
			HttpServletRequest request) throws Exception {
		List<FreyaResponse> responses = new ArrayList<FreyaResponse>();
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();

		List<Question> questions = helper.processQuestion(query, true);
		session.setAttribute(query, questions);
		for (Question question : questions) {
			FreyaResponse response = helper.dialogOrNot(question, query,
					session);
			responses.add(response);
		}
		long stopTime = System.currentTimeMillis();
		long runTime = stopTime - startTime;

		logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
				null));
		return responses;
	}

	/**
	 * this method sets preferLonger to false
	 * 
	 * @param query
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/askNoFilter", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> askNoFilter(
			@RequestParam(value = "query") String query,
			HttpServletRequest request) throws Exception {
		List responses = new ArrayList<String>();
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();
		// force dialog and no filter
		List<Question> questions = helper.processQuestion(query, true, false);
		session.setAttribute(query, questions);
		for (Question question : questions) {
			FreyaResponse response = helper.dialogOrNot(question, query,
					session);
			responses.add(response);
		}
		long stopTime = System.currentTimeMillis();
		long runTime = stopTime - startTime;
		logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
				null));
		return responses;
	}

	/**
	 * analysis of input with subject predicate object
	 * 
	 * @param input
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/analyse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> analyse(
			@RequestParam(value = "query") String input,
			HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();
		// force dialog and no filter
		List<Question> sentences = helper.processQuestion(input);
		long stopTime = System.currentTimeMillis();
		long runTime = stopTime - startTime;
		logger.info(ProfilerUtil.profileString(session.getId(), input, runTime,
				null));
		List<FreyaResponse> freyaResponses = helper
				.extractAnnotationsFromQuestion(sentences);
		return freyaResponses;
	}

}
