/*******************************************************************************
 * Copyright 2020 Cognizant Technology Solutions
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.cognizant.devops.platformservice.test.grafanadashboard;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.cognizant.devops.platformcommons.constants.ConfigOptions;
import com.cognizant.devops.platformcommons.constants.PlatformServiceConstants;
import com.cognizant.devops.platformcommons.constants.UnitTestConstant;
import com.cognizant.devops.platformcommons.core.util.JsonUtils;
import com.cognizant.devops.platformcommons.dal.grafana.GrafanaHandler;
import com.cognizant.devops.platformcommons.exception.InsightsCustomException;
import com.cognizant.devops.platformdal.grafana.pdf.GrafanaDashboardPdfConfig;
import com.cognizant.devops.platformdal.grafana.pdf.GrafanaDashboardPdfConfigDAL;
import com.cognizant.devops.platformservice.grafanadashboard.service.GrafanaPdfServiceImpl;
import com.cognizant.devops.platformservice.rest.util.PlatformServiceUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@Test
@WebAppConfiguration
@ContextConfiguration(locations = { "classpath:spring-test-config.xml" })
public class GrafanaDashboardReportTest extends AbstractTestNGSpringContextTests  {

	private static final Logger log = LogManager.getLogger(GrafanaDashboardReportTest.class);
	private static final String AUTHORIZATION = "authorization";

	Map<String, String> testAuthData = new HashMap<>();
	
	GrafanaDashboardReportData grafanaDashboard = new GrafanaDashboardReportData();

	MockHttpServletRequest httpRequest = new MockHttpServletRequest();
	ResultMatcher ok = MockMvcResultMatchers.status().isOk();
	String cookiesString;

	GrafanaPdfServiceImpl grafanaPdfServiceImpl = new GrafanaPdfServiceImpl();

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@DataProvider
	public void getData() {
		String path = System.getenv().get(ConfigOptions.INSIGHTS_HOME) + File.separator
				+ UnitTestConstant.TESTNG_TESTDATA + File.separator + "grafanaAuth.json";
		JsonElement jsonData;
		try {
			jsonData = JsonUtils.parseReader(new FileReader(new File(path).getCanonicalPath()));
		} catch (JsonIOException | JsonSyntaxException | IOException e) {
			throw new SkipException("skipped this test case as grafana auth file not found.");
		}
		testAuthData = new Gson().fromJson(jsonData, Map.class);
	}

	@BeforeClass
	public void onInit() throws InterruptedException, IOException, InsightsCustomException {
		getData();
		httpRequest.addHeader("Authorization", testAuthData.get(AUTHORIZATION));
		Map<String, String> cookiesMap = PlatformServiceUtil.getGrafanaCookies(httpRequest);

		cookiesString = cookiesMap.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(";"));
		log.debug(" cookiesString " + cookiesString);
		for (Map.Entry<String, String> entry : cookiesMap.entrySet()) {
			Cookie cookie = new Cookie(entry.getKey(), entry.getValue());
			cookie.setHttpOnly(true);
			cookie.setMaxAge(60 * 30);
			cookie.setPath("/");
			httpRequest.setCookies(cookie);
		}
	}

	private MockHttpServletRequestBuilder mockHttpServletRequestBuilderPost(String url, String content) {
		log.debug(" cookies " + httpRequest.getCookies());
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(url).cookie(httpRequest.getCookies())
				.header("Authorization", testAuthData.get(AUTHORIZATION)).header("Cookie", cookiesString)
				.content(content).contentType(MediaType.APPLICATION_JSON_VALUE);
		return builder;
	}

	private MockMvc getMacMvc() {
		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
		return builder.build();
	}

	private MockHttpServletRequestBuilder mockHttpServletRequestBuilderPostWithRequestParam(String url,
			String content) {

		return MockMvcRequestBuilders.post(url).cookie(httpRequest.getCookies())
				.header("Authorization", testAuthData.get(AUTHORIZATION)).header("Cookie", cookiesString);
	}
	
	private void deleteApiKeys() {
		try {
			GrafanaHandler grafanaHandler = new GrafanaHandler();

			Map<String, String> headers = PlatformServiceUtil.prepareGrafanaHeader(httpRequest);
			String responseAuthKey = grafanaHandler.grafanaGet("/api/auth/keys", headers);
			JsonArray detailsOfAPIJsonArray = JsonUtils.parseStringAsJsonArray(responseAuthKey);

			for (JsonElement jsonElement : detailsOfAPIJsonArray) {
				if (jsonElement.getAsJsonObject().has("id")
						&& jsonElement.getAsJsonObject().get("name").getAsString().equalsIgnoreCase("pdftoken")) {
					try {
						grafanaHandler.grafanaDelete("/api/auth/keys/" + jsonElement.getAsJsonObject().get("id"),
								headers);
					} catch (Exception e) {
						log.error(" Unable to delete API token ", e);
						log.error(e);
					}
				}
			}
		} catch (Exception e) {
			log.error(" Unable to delete API token ", e);
		}
	}

	@Test(priority = 1)
	public void publishGrafanaDashboardDetails() throws InsightsCustomException {

		try {
			deleteApiKeys();
			this.mockMvc = getMacMvc();
			log.debug(" cookies " + httpRequest.getCookies());
			MockHttpServletRequestBuilder builder = mockHttpServletRequestBuilderPost(
					"/dashboardReport/exportPDF/saveDashboardAsPDF", grafanaDashboard.dashboardJson);
			this.mockMvc.perform(builder).andExpect(ok);
		} catch (Exception e) {
			log.error("Error while testing Save Dashboard " + e);
		}
		log.debug("Test case Save Dashboard successfully ");

	}

	@Test(priority = 2)
	public void getAllGrafanaDashboardConfigs() throws InsightsCustomException {
		try {
			deleteApiKeys();
			this.mockMvc = getMacMvc();
			log.debug(" cookies " + httpRequest.getCookies());

			MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
					.get("/dashboardReport/exportPDF/fetchGrafanaDashboardConfigs").cookie(httpRequest.getCookies())
					.header("Authorization", testAuthData.get(AUTHORIZATION)).header("Cookie", cookiesString);

			this.mockMvc.perform(builder).andExpect(ok);
		} catch (Exception e) {
			log.error("Error while testing Fetch Dashboard Records" + e);
		}
		log.debug("Test case Fetch Dashboard successfully ");
	}

	@Test(priority = 3)
	public void updateGrafanaDashboardDetails() throws InsightsCustomException {
		try {
			deleteApiKeys();
			int id = 0;
			List<GrafanaDashboardPdfConfig> list = grafanaPdfServiceImpl.getAllGrafanaDashboardConfigs();
			for (GrafanaDashboardPdfConfig g : list) {
				if (g.getTitle().equalsIgnoreCase("5-sprint-score-card-updated")) {
					id = g.getId();
				}
			}
			JsonObject detailsJson = JsonUtils.parseStringAsJsonObject(grafanaDashboard.updateJson);
			detailsJson.toString();
			detailsJson.addProperty("id", id);
			this.mockMvc = getMacMvc();
			log.debug(" cookies " + httpRequest.getCookies());
			MockHttpServletRequestBuilder builder = mockHttpServletRequestBuilderPost(
					"/dashboardReport/exportPDF/updateDashboardConfig", grafanaDashboard.updateJson);
			grafanaPdfServiceImpl.updateGrafanaDashboardDetails(detailsJson);
			this.mockMvc.perform(builder).andExpect(ok);
		} catch (Exception e) {
			log.error("Error while testing Update Dashboard " + e);
		}
		log.debug("Test case Update Dashboard successfully ");

	}

	@Test(priority = 4)
	public void fetchGrafanaDashboardDetailsByWorkflowId() throws InsightsCustomException {
		GrafanaDashboardPdfConfigDAL grafanaDashboardConfigDAL = new GrafanaDashboardPdfConfigDAL();
		try {
			deleteApiKeys();
			grafanaDashboardConfigDAL.fetchGrafanaDashboardDetailsByWorkflowId("GRAFANADASHBOARDPDFREPORT_1620379924");
		} catch (AssertionError e) {
			AssertJUnit.fail(e.getMessage());
		}
	}

	@Test(priority = 5)
	public void testUpdateDashboardPdfConfigStatus() throws InsightsCustomException {
		try {
			deleteApiKeys();
			int id = 0;
			List<GrafanaDashboardPdfConfig> list = grafanaPdfServiceImpl.getAllGrafanaDashboardConfigs();
			for (GrafanaDashboardPdfConfig g : list) {
				if (g.getTitle().equalsIgnoreCase("5-sprint-score-card-updated")) {
					id = g.getId();
				}
			}
			String dashboardString = "{\"id\":" + id + ",\"status\":\"RESTART\"}";
			JsonObject detailsJson =JsonUtils.parseStringAsJsonObject(dashboardString);
			this.mockMvc = getMacMvc();
			log.debug(" cookies " + httpRequest.getCookies());
			MockHttpServletRequestBuilder builder = mockHttpServletRequestBuilderPost(
					"/dashboardReport/updateDasboardStatus", detailsJson.toString());
			String status = grafanaPdfServiceImpl.updateDashboardPdfConfigStatus(detailsJson);
			this.mockMvc.perform(builder).andExpect(ok);
		} catch (Exception e) {
			log.error("Error while testing Update Dashboard Status" + e);
		}
	}

	@Test(priority = 6)
	public void testgetEmailConfiguration() throws InsightsCustomException {
		try {
			deleteApiKeys();
			MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
					.get("/dashboardReport/getEmailConfigurationStatus");
			this.mockMvc.perform(builder).andExpect(ok);
		} catch (Exception e) {
			log.error("Error while testing  Get Email Configuration status " + e);
		}
	}

	
	@Test(priority = 7, expectedExceptions = InsightsCustomException.class)
	public void saveGrafanaDashboardConfigExceptionTest() throws Exception {
		JsonObject dashboardJsonObject = new JsonObject();		
		grafanaPdfServiceImpl.saveGrafanaDashboardConfig(dashboardJsonObject);
	}

	@Test(priority = 8)
	public void getAllGrafanaDashboardConfigsTest() throws InsightsCustomException {
		List<GrafanaDashboardPdfConfig> allGrafanaDashboardConfigs = grafanaPdfServiceImpl
				.getAllGrafanaDashboardConfigs();
		assertTrue(allGrafanaDashboardConfigs.size() > 0);
	}
	
	@Test(priority = 9)
	public void updateGrafanaDashboardDetailsTest() throws InsightsCustomException {
		List<GrafanaDashboardPdfConfig> allGrafanaDashboardConfigs = grafanaPdfServiceImpl
				.getAllGrafanaDashboardConfigs();
		int id = 0;
		for (GrafanaDashboardPdfConfig grafanaDashboardPdfConfig : allGrafanaDashboardConfigs) {
			if (grafanaDashboardPdfConfig.getTitle().equalsIgnoreCase("5-sprint-score-card-updated")) {
				id = grafanaDashboardPdfConfig.getId();
			}
		}
		
		JsonObject dashObject = JsonUtils.parseStringAsJsonObject(grafanaDashboard.updateJson);
		dashObject.addProperty("id", id);
		grafanaPdfServiceImpl.updateGrafanaDashboardDetails(dashObject);
		List<GrafanaDashboardPdfConfig> updateDashboardPdfConfigs = grafanaPdfServiceImpl
				.getAllGrafanaDashboardConfigs();
		assertEquals("5-sprint-score-card-updated", updateDashboardPdfConfigs.get(0).getTitle());

	}
	
	@Test(priority = 10, expectedExceptions = InsightsCustomException.class)
	public void updateGrafanaDashboardDetailsExceptionTest() throws InsightsCustomException {
		
		int id = 0;
		JsonObject dashObject = JsonUtils.parseStringAsJsonObject(grafanaDashboard.updateJson);
		dashObject.addProperty("id", id);
		grafanaPdfServiceImpl.updateGrafanaDashboardDetails(dashObject);
	}
	

	@Test(priority = 11)
	public void updateDashboardPdfConfigStatusTest() throws InsightsCustomException {
		int id = 0;
		List<GrafanaDashboardPdfConfig> list = grafanaPdfServiceImpl.getAllGrafanaDashboardConfigs();
		for (GrafanaDashboardPdfConfig g : list) {
			if (g.getTitle().equalsIgnoreCase("5-sprint-score-card-updated")) {
				id = g.getId();
			}
		}
		String dashboardString = "{\"id\":" + id + ",\"status\":\"RESTART\"}";
		JsonObject detailsJson = JsonUtils.parseStringAsJsonObject(dashboardString);
		
		String status = grafanaPdfServiceImpl.updateDashboardPdfConfigStatus(detailsJson);
		
		assertEquals(status, PlatformServiceConstants.SUCCESS);
		
	}
	
	@Test(priority = 12, expectedExceptions = InsightsCustomException.class)
	public void updateDashboardPdfConfigStatusExceptionTest() throws InsightsCustomException {
		int id = 0;
		String dashboardString = "{\"id\":" + id + ",\"status\":\"RESTART\"}";
		JsonObject detailsJson =JsonUtils.parseStringAsJsonObject(dashboardString);
		grafanaPdfServiceImpl.updateDashboardPdfConfigStatus(detailsJson);
	}
	
	@Test(priority = 13)
	public void setDashboardActiveStatusTest() throws InsightsCustomException {
		int id = 0;
		List<GrafanaDashboardPdfConfig> list = grafanaPdfServiceImpl.getAllGrafanaDashboardConfigs();
		for (GrafanaDashboardPdfConfig g : list) {
			if (g.getTitle().equalsIgnoreCase("5-sprint-score-card-updated")) {
				id = g.getId();
			}
		}
		String dashboardJsonString = "{\"id\":" + id  + ", \"isActive\": \"false\"}";
		JsonObject dashboardJson =JsonUtils.parseStringAsJsonObject(dashboardJsonString);
		String status = grafanaPdfServiceImpl.setDashboardActiveState(dashboardJson);
		assertEquals(status, PlatformServiceConstants.SUCCESS);
	}
	
	@Test(priority = 14, expectedExceptions = InsightsCustomException.class)
	public void setDashboardActiveStatusExceptionTest() throws InsightsCustomException {
		int id = 0;
		String dashboardJsonString = "{\"id\":" + id  + ", \"isActive\": \"false\"}";
		JsonObject dashboardJson = JsonUtils.parseStringAsJsonObject(dashboardJsonString);
		String status = grafanaPdfServiceImpl.setDashboardActiveState(dashboardJson);
	}
	
	@Test(priority = 15)
	public void deleteGrafanaDashboardDetailsTest() throws InsightsCustomException {
		int id = 0;
		List<GrafanaDashboardPdfConfig> list = grafanaPdfServiceImpl.getAllGrafanaDashboardConfigs();
		for (GrafanaDashboardPdfConfig g : list) {
			if (g.getTitle().equalsIgnoreCase("5-sprint-score-card-updated")) {
				id = g.getId();
			}
		}
		grafanaPdfServiceImpl.deleteGrafanaDashboardDetails(id);
		
		List<GrafanaDashboardPdfConfig> allGrafanaDashboardConfigs = grafanaPdfServiceImpl.getAllGrafanaDashboardConfigs();
		assertEquals(allGrafanaDashboardConfigs.size(), 0);
	}
	
	@Test(priority = 16)
	public void deleteGrafanaDashboardDetails() throws InsightsCustomException {
		try {
			deleteApiKeys();
			
			this.mockMvc = getMacMvc();

			log.debug(" cookies " + httpRequest.getCookies());

			int id = 0;
			List<GrafanaDashboardPdfConfig> list = grafanaPdfServiceImpl.getAllGrafanaDashboardConfigs();
			for (GrafanaDashboardPdfConfig g : list) {
				if (g.getTitle().equalsIgnoreCase("5-sprint-score-card-updated")) {
					id = g.getId();
					String url = "/dashboardReport/exportPDF/deleteDashboardConfig?id=" + id;

					MockHttpServletRequestBuilder builder = mockHttpServletRequestBuilderPostWithRequestParam(url, "");

					this.mockMvc.perform(builder).andExpect(ok);
				}
			}

		} catch (Exception e) {
			log.error("Error while testing Delete Dashboard " + e);
		}
		log.debug("Test case Deleted Dashboard successfully ");

	}
}
