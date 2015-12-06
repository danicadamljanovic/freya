package org.freya.service;

import java.util.Map;
import static org.springframework.http.HttpMethod.POST;
//import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.common.collect.Maps;

public class SolrIndexServiceDemo {
	private static final Logger log = LoggerFactory
			.getLogger(FreyaServiceTest.class);
	private static ApplicationContext applicationContext;

	@BeforeClass
	public static void setUpUnitTest() throws Exception {
		applicationContext = new MockWebAppContext("src/main/webapp", "freya");
	}

	@Test
	public void clearSolr() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
				send(POST, "/clear", parameters);
	}
	@Test
	public void reindexSolr() throws Exception {
		Map<String, String> parameters = Maps.newHashMap();
				send(POST, "/reindex", parameters);
	}
	private MockHttpServletResponse send(HttpMethod method, String path,
			Map<String, String> parameters) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(
				method.name(), path);
		request.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
		request.addHeader("Content-type", MediaType.APPLICATION_JSON_VALUE);
		if (parameters != null) {
			request.addParameters(parameters);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();

		DispatcherServlet servlet = applicationContext
				.getBean(DispatcherServlet.class);
		servlet.service(request, response);

		return response;
	}
}
