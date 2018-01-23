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
import org.freya.model.POC;
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
	 * This method is called for the query processing at the click of Submit
	 * button of the client. It will return the response which either contains
	 * the answer, or the dialogue information.
	 * 
	 * @return Test with:
	 * 
	 *         <pre>
	 * curl --data "query=What is a city?" http://localhost:8080/freya/service/ask
	 *         </pre>
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/ask", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> ask(@RequestParam(value = "query") String query, HttpServletRequest request)
			throws Exception {
		List<FreyaResponse> responses = new ArrayList<FreyaResponse>();
		try {
			HttpSession session = request.getSession(true);
			long startTime = System.currentTimeMillis();
			List<Question> questions = helper.processQuestion(query);
			session.setAttribute(query, questions);
			for (Question question : questions) {
				FreyaResponse response = helper.dialogOrNot(question, query, session);
				responses.add(response);
			}
			long stopTime = System.currentTimeMillis();
			long runTime = stopTime - startTime;
			logger.info(ProfilerUtil.profileString(session.getId(), query, runTime, null));
		} catch (Exception e) {
			throw e;
		}
		return responses;
	}

	/**
	 * This method is used for uploading ontology files to RDF repository.
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
			@RequestParam(value = "id") String repositoryId, @RequestParam(value = "src") String source,
			HttpServletRequest request) {

		logger.info(String.format("params: { url => %s, id => %s, src => %s }", repositoryURL, repositoryId, source));

		if (StringUtils.isEmpty(repositoryURL) || StringUtils.isEmpty(repositoryId)) {
			return "query params: url, id and src cannot be empty!";
		}
		try {
			helper.load(repositoryURL, repositoryId, source, request.getInputStream());
		} catch (Exception e) {
			logger.error(e);
			return "Exception: " + e.getLocalizedMessage();
		}

		return String.format("Updating from %s to %s.", source, repositoryURL);
	}

	/**
	 * This method is called for loading ontology files to RDF repository. The
	 * ontology directory is provided through src parameter.
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
			@RequestParam(value = "id") String repositoryId, @RequestParam(value = "src") String source,
			HttpServletRequest request) {

		logger.info(String.format("params: { url => %s, id => %s, src => %s }", repositoryURL, repositoryId, source));

		if (StringUtils.isEmpty(repositoryURL) || StringUtils.isEmpty(repositoryId)) {
			return "query params: url, id and src cannot be empty!";
		}
		try {
			File initialFolder = new File(source);
			if (!initialFolder.isDirectory()) {
				return String.format("Specified src parameter is not a directory: %s", source);
			}
			Collection<File> files = FileUtils.listFiles(initialFolder, null, true);
			logger.info("Attempting to load:" + files.size() + " files.");
			for (File file : files) {
				InputStream targetStream = new FileInputStream(file);
				helper.load(repositoryURL, repositoryId, file.toURI().toString(), targetStream);
			}
		} catch (Exception e) {
			logger.error(e);
			return "Exception: " + e.getLocalizedMessage();
		}

		return String.format("Updating from %s to %s.", source, repositoryURL);
	}

	/**
	 * This method returns a SPARQL interpretation of a Natural Language query.
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/getSparql", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> getSparql(@RequestParam(value = "query") String query, HttpServletRequest request)
			throws Exception {
		List<FreyaResponse> responses = null;
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();
		try {
			responses = helper.processQuestionAutomatically(query);
			long stopTime = System.currentTimeMillis();
			long runTime = stopTime - startTime;
			logger.info(ProfilerUtil.profileString(session.getId(), query, runTime, null));
		} catch (Exception e) {
			throw e;
		}
		return responses;
	}

	/**
	 * This method is called when the user selects one of the suggestions in the
	 * dialogue.
	 * 
	 * @param query
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/refine", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> refine(@RequestParam(value = "query") String query,
			@RequestParam(value = "voteId") String[] voteIds, HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession(true);
		List<FreyaResponse> responses = new ArrayList<FreyaResponse>();
		FreyaResponse response;
		if (voteIds != null) {
			try {
				response = helper.refineQuestion(query, voteIds, session);
				responses.add(response);
				if (responses.size() > 1)
					logger.info("Refining based on several sentences input is not supported yet.");
			} catch (Exception e) {
				throw e;
			}
		} else
			throw new Exception("voteId is null! This should never happen!");
		return responses;
	}

	/**
	 * This method is called for the query processing when FREyA is in auto
	 * mode. This means that no dialogue will be generated, and if there is a
	 * need to do so, FREyA will assume that the top ranked suggestion is the
	 * correct one.
	 * 
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/askNoDialog", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> askNoDialog(@RequestParam(value = "query") String query, HttpServletRequest request)
			throws Exception {
		List<FreyaResponse> responses = null;
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();
		try {
			responses = helper.processQuestionAutomatically(query);
			long stopTime = System.currentTimeMillis();
			long runTime = stopTime - startTime;
			logger.info(ProfilerUtil.profileString(session.getId(), query, runTime, null));
		} catch (Exception e) {
			throw e;
		}
		return responses;
	}

	/**
	 * This method sets true to forceDialog parameter meaning that the dialogue
	 * will be generated for each query.
	 * 
	 * @param query
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/askForceDialog", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> askForceDialog(@RequestParam(value = "query") String query, HttpServletRequest request)
			throws Exception {
		List<FreyaResponse> responses = new ArrayList<FreyaResponse>();
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();
		try {
			List<Question> questions = helper.processQuestion(query, true);
			session.setAttribute(query, questions);
			for (Question question : questions) {
				FreyaResponse response = helper.dialogOrNot(question, query, session);
				responses.add(response);
			}
			long stopTime = System.currentTimeMillis();
			long runTime = stopTime - startTime;
			logger.info(ProfilerUtil.profileString(session.getId(), query, runTime, null));
		} catch (Exception e) {
			throw e;
		}
		return responses;
	}

	/**
	 * This method sets preferLonger to false and forceDialogue to true. If
	 * there are ambiguous annotations the default behaviour is that FREyA
	 * chooses the longest one. When preferLonger is set to false, all
	 * annotations will be considered, therefore the dialogues will be generated
	 * for the user to choose the correct one.
	 * 
	 * @param query
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/askNoFilter", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> askNoFilter(@RequestParam(value = "query") String query, HttpServletRequest request)
			throws Exception {
		List<FreyaResponse> responses = new ArrayList<FreyaResponse>();
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();
		// force dialog and no filter
		try {
			List<Question> questions = helper.processQuestion(query, true, false);
			session.setAttribute(query, questions);
			for (Question question : questions) {
				FreyaResponse response = helper.dialogOrNot(question, query, session);
				responses.add(response);
			}
			long stopTime = System.currentTimeMillis();
			long runTime = stopTime - startTime;
			logger.info(ProfilerUtil.profileString(session.getId(), query, runTime, null));
		} catch (Exception e) {
			throw e;
		}
		return responses;
	}

	/**
	 * Analysis of input with subject predicate object.
	 * 
	 * @param input
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/analyse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FreyaResponse> analyse(@RequestParam(value = "query") String input, HttpServletRequest request)
			throws Exception {
		HttpSession session = request.getSession(true);
		long startTime = System.currentTimeMillis();
		List<FreyaResponse> freyaResponses = null;
		try {
			List<Question> sentences = helper.processQuestion(input);
			long stopTime = System.currentTimeMillis();
			long runTime = stopTime - startTime;
			logger.info(ProfilerUtil.profileString(session.getId(), input, runTime, null));
			freyaResponses = helper.extractAnnotationsFromQuestion(sentences);
		} catch (Exception e) {
			throw e;
		}
		return freyaResponses;
	}
/**
 * Extract POC text only
 * @param input
 * @param request
 * @return
 * @throws Exception
 */
	   @ResponseBody
	    @RequestMapping(value = "/extractPOCText", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	    public List<String> extractPOCText(@RequestParam(value = "query") String input, HttpServletRequest request)
	            throws Exception {
	        HttpSession session = request.getSession(true);
	        long startTime = System.currentTimeMillis();
	        List<FreyaResponse> freyaResponses = null;
	        List<String> pocStrings = new ArrayList<String>();
	        try {
	            List<Question> sentences = helper.processQuestion(input);
	            long stopTime = System.currentTimeMillis();
	            long runTime = stopTime - startTime;
	            logger.info(ProfilerUtil.profileString(session.getId(), input, runTime, null));
	            freyaResponses = helper.extractAnnotationsFromQuestion(sentences);
	            
	            for (FreyaResponse r:freyaResponses) {
	              List<POC> pocs = r.getPocs();
	              for (POC poc:pocs) {
	                pocStrings.add(poc.getAnnotation().getText());
	              }
	            }
	        } catch (Exception e) {
	            throw e;
	        }
	        return pocStrings;
	    }
	
}
