package org.freya.service;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import java.net.URI;
import java.util.Map;
import org.freya.model.service.FreyaResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:META-INF/spring/freya-applicationContext.xml" })
@Configurable
public class FreyaServiceTest {
	private static final Logger log = LoggerFactory.getLogger(FreyaServiceTest.class);
	ObjectMapper objectMapper = new ObjectMapper();
	private static ApplicationContext applicationContext;

	@BeforeClass
	public static void setUpUnitTest() throws Exception {
		applicationContext = new MockWebAppContext("src/main/webapp", "freya");
	}

	@Test
	public void getSparql() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("query", "City california.");

		FreyaResponse expected = new FreyaResponse();
		// expected.setRepositoryId("mooney-native");
		// expected.setRepositoryUrl("http://localhost:8080/openrdf-sesame");
		expected.setSparqlQuery(
				"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> prefix xsd: <http://www.w3.org/2001/XMLSchema#> select distinct ?firstJoker0 where { ?firstJoker0  ?p0 ?d1 .  FILTER ( ?p0=<http://www.mooney.net/geo#cityPopulation>  ) .  FILTER REGEX(str(?d1), \"^California$\",\"i\") .  FILTER (?p0=<http://www.w3.org/2000/01/rdf-schema#label>)} LIMIT 10000");

		FreyaResponse[] actual = objectMapper.readValue(send(GET, "/getSparql", parameters).getContentAsString(),
				FreyaResponse[].class);
		// assertEquals(expected.getRepositoryId(), actual.getRepositoryId());
		// assertEquals(expected.getRepositoryUrl(), actual.getRepositoryUrl());
		assertNotNull(actual[0]);
		assertNotNull(actual[0].getPreciseSparql());
		assertTrue(actual[0].getPreciseSparql().contains("i1"));
	}

	@Test
	public void ask() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("query", "List capitals.");

		// FreyaResponse[] actual = objectMapper.readValue(
		// send(GET, "/ask", parameters).getContentAsString(),
		// FreyaResponse[].class);
		// log.info(actual[0].getTextResponse().toString());

		String response = send(GET, "/ask", parameters).getContentAsString();
		FreyaResponse[] actual = objectMapper.readValue(response, FreyaResponse[].class);
		System.out.println(response);
		assertNotNull(actual[0].getTextResponse().toString());
	}

	@Test
	public void askStem() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("query", "List cities in California.");

		FreyaResponse[] actual = objectMapper.readValue(send(GET, "/ask", parameters).getContentAsString(),
				FreyaResponse[].class);
		log.info(actual[0].getTextResponse().toString());
		// assertTrue( actual.getTextResponse());
	}

	@Test
	public void askNoDialogue() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("query", "rivers in texas");
		MockHttpServletResponse response = send(GET, "/askNoDialog", parameters);
		FreyaResponse[] actual = objectMapper.readValue(response.getContentAsString(), FreyaResponse[].class);
		String sparql = actual[0].getPreciseSparql();
		assertTrue(sparql.contains("joker1"));

		String q1 = "?c0  ?typeRelationc0 <http://www.mooney.net/geo#River>";
		String q2 = "filter (?i1=<http://www.mooney.net/geo#texas>";
		assertTrue(actual[0].getPreciseSparql().contains(q1));
		assertTrue(actual[0].getPreciseSparql().contains(q2));
		/**
		 * prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> prefix xsd:
		 * <http://www.w3.org/2001/XMLSchema#> select distinct ?c0 ?joker1 ?i1
		 * where {{{ ?c0 ?typeRelationc0 <http://www.mooney.net/geo#River> . }}
		 * {{ ?i1 ?joker1 ?c0 } UNION { ?c0 ?joker1 ?i1 }} ?i1 ?typeRelationi1
		 * <http://www.mooney.net/geo#State> . filter (?i1=
		 * <http://www.mooney.net/geo#texas>) . } LIMIT 10000
		 **/
	}

	@Test
	public void analyseClassStemInstance() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("query", "cities of california");
		MockHttpServletResponse response = send(GET, "/analyse", parameters);

		String stringContent = response.getContentAsString();
		FreyaResponse[] actual = objectMapper.readValue(stringContent, FreyaResponse[].class);

		assertEquals(2, actual[0].getAnnotations().size());

		log.info(actual[0].getAnnotations().toString());

		assertEquals("california", actual[0].getAnnotations().get(0).getText());
		assertEquals("http://www.mooney.net/geo#california", actual[0].getAnnotations().get(0).getUri());
		assertEquals("http://www.mooney.net/geo#State", actual[0].getAnnotations().get(0).getType());
	}

	@Test
	public void analyseClassInstance() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("query", "city of california");
		MockHttpServletResponse response = send(GET, "/analyse", parameters);

		String stringContent = response.getContentAsString();
		FreyaResponse[] actual = objectMapper.readValue(stringContent, FreyaResponse[].class);

		assertEquals(2, actual[0].getAnnotations().size());

		log.info(actual[0].getAnnotations().toString());

		assertEquals("california", actual[0].getAnnotations().get(0).getText());
		assertEquals("http://www.mooney.net/geo#california", actual[0].getAnnotations().get(0).getUri());
		assertEquals("http://www.mooney.net/geo#State", actual[0].getAnnotations().get(0).getType());
	}

	@Test
	public void analyseInstance() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("query", "what is california?");
		MockHttpServletResponse response = send(GET, "/analyse", parameters);

		String stringContent = response.getContentAsString();
		FreyaResponse[] actual = objectMapper.readValue(stringContent, FreyaResponse[].class);

		assertEquals(1, actual[0].getAnnotations().size());

		log.info(actual[0].getAnnotations().toString());

		assertEquals("california", actual[0].getAnnotations().get(0).getText());
		assertEquals("http://www.mooney.net/geo#california", actual[0].getAnnotations().get(0).getUri());
		assertEquals("http://www.mooney.net/geo#State", actual[0].getAnnotations().get(0).getType());
	}

	@Test
	public void analyseClass() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("query", "city");
		MockHttpServletResponse response = send(GET, "/analyse", parameters);

		String stringContent = response.getContentAsString();
		FreyaResponse[] actual = objectMapper.readValue(stringContent, FreyaResponse[].class);

		assertEquals(1, actual[0].getAnnotations().size());

		log.info(actual[0].getAnnotations().toString());

		assertEquals("city", actual[0].getAnnotations().get(0).getText());
		assertTrue(actual[0].getAnnotations().get(0).getUri().toLowerCase().contains("city"));
		assertNotNull(actual[0].getAnnotations().get(0).getUri());
	}

	@Test
	public void analyseProperty() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("query", "runs through");
		MockHttpServletResponse response = send(GET, "/analyse", parameters);

		String stringContent = response.getContentAsString();
		FreyaResponse[] actual = objectMapper.readValue(stringContent, FreyaResponse[].class);

		assertEquals(1, actual[0].getAnnotations().size());

		log.info(actual[0].getAnnotations().toString());

		assertEquals("runs through", actual[0].getAnnotations().get(0).getText());
		assertEquals("http://www.mooney.net/geo#runsThrough", actual[0].getAnnotations().get(0).getUri());
		assertEquals("property", actual[0].getAnnotations().get(0).getType());
	}

	// uncomment this to run the loadBulk service
	// @Test
	public void testLoadBulk() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put("url", "http://localhost:8080/openrdf-sesame");
		parameters.put("id", "fibo");
		// URI srcURI = new
		// URI("/Users/danica/projects/nli-branch/freya/freya-annotate/src/main/resources/ontologies/mooney/");
		URI srcURI = new URI(
				"/Users/danica/projects/experiments/nlu/ontologies/again/www.omg.org/spec/EDMC-FIBO/FND/20141101");
		parameters.put("src", srcURI.toString());

		String response = send(POST, "/loadBulk", parameters).toString();

		log.info("response:" + response);
	}

	private MockHttpServletResponse send(HttpMethod method, String path) throws Exception {
		return send(method, path, null);
	}

	private MockHttpServletResponse send(HttpMethod method, String path, Map<String, String> parameters)
			throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method.name(), path);
		request.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
		request.addHeader("Content-type", MediaType.APPLICATION_JSON_VALUE);
		if (parameters != null) {
			request.addParameters(parameters);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();

		DispatcherServlet servlet = applicationContext.getBean(DispatcherServlet.class);
		servlet.service(request, response);

		return response;
	}

	private void asserts(MockHttpServletResponse res, String expected) throws Exception {
		assertThat(res.getStatus(), is(HttpStatus.OK.value()));
		assertThat(res.getContentType(), is(MediaType.APPLICATION_JSON_VALUE));
		String content = res.getContentAsString();
		log.info("expected:{}", expected);
		log.info("content :{}", content);
		assertEquals(expected, content);
	}

}
