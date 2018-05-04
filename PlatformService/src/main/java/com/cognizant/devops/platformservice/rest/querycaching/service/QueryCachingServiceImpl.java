/*******************************************************************************
 * Copyright 2017 Cognizant Technology Solutions
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
package com.cognizant.devops.platformservice.rest.querycaching.service;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.cognizant.devops.platformcommons.config.ApplicationConfigProvider;
import com.cognizant.devops.platformcommons.core.util.InsightsUtils;
import com.cognizant.devops.platformcommons.dal.neo4j.GraphDBException;
import com.cognizant.devops.platformcommons.dal.neo4j.GraphResponse;
import com.cognizant.devops.platformcommons.dal.neo4j.Neo4jDBHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

@Service("queryCachingService")
public class QueryCachingServiceImpl implements QueryCachingService {

	private static Logger log = Logger.getLogger(QueryCachingServiceImpl.class);

	@Override
	public JsonObject getCacheResults(String requestPayload) {
		JsonObject resultJson = null;
		JsonParser parser = new JsonParser();
		JsonObject requestJson = parser.parse(requestPayload).getAsJsonObject();
		String isTestDB = requestJson.get("metadata").getAsJsonArray().get(0).getAsJsonObject().get("testDB")
				.toString();
		try {
			if (isTestDB.equals("true")) {
				resultJson = getNeo4jDatasource(requestPayload);
			} else {
				resultJson = getEsCachedResults(requestPayload);
			}
		} catch (GraphDBException e) {
			log.error("Error caught - ", e);
		}
		return resultJson;

	}

	private JsonObject getNeo4jDatasource(String queryjson) throws GraphDBException {
		Neo4jDBHandler dbHandler = new Neo4jDBHandler();
		String query = null;
		GraphResponse response = null;
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(queryjson).getAsJsonObject();
		if (json.get("statements").getAsJsonArray().get(0).getAsJsonObject().has("statement")) {
			query = json.get("statements").getAsJsonArray().get(0).getAsJsonObject().get("statement").getAsString();
		}
		try {
			response = dbHandler.executeCypherQuery(query);
		} catch (GraphDBException e) {
			log.error("Exception in neo4j query execution", e);
			throw e;
		}
		return response.getJson();
	}

	private JsonObject getEsCachedResults(String requestPayload) {

		try {
			JsonParser parser = new JsonParser();
			JsonObject requestJson = parser.parse(requestPayload).getAsJsonObject();

			boolean isCacheResult = requestJson.get("metadata").getAsJsonArray().get(0).getAsJsonObject()
					.get("resultCache").getAsBoolean();
			String cachingType = requestJson.get("metadata").getAsJsonArray().get(0).getAsJsonObject()
					.get("cachingType").getAsString();

			if (isCacheResult) {
				String sourceESCacheUrl = ApplicationConfigProvider.getInstance().getQueryCache().getEsCacheIndex();

				String statement = requestJson.get("statements").getAsJsonArray().get(0).getAsJsonObject()
						.get("statement").getAsString();
				String startTimeStr = requestJson.get("metadata").getAsJsonArray().get(0).getAsJsonObject()
						.get("startTime").getAsString();
				String endTimeStr = requestJson.get("metadata").getAsJsonArray().get(0).getAsJsonObject().get("endTime")
						.getAsString();
				statement = getStatementWithoutTime(statement, startTimeStr, endTimeStr);
				Long startTime = requestJson.get("metadata").getAsJsonArray().get(0).getAsJsonObject().get("startTime")
						.getAsLong();
				Long endTime = requestJson.get("metadata").getAsJsonArray().get(0).getAsJsonObject().get("endTime")
						.getAsLong();
				Long currentTime = InsightsUtils.getCurrentTimeInSeconds();

				String queryHash = DigestUtils.md5Hex(statement).toUpperCase();
				String esQuery = "";
				int cacheTime = 0;
				int cacheVariance = 0;
				String cacheDetailsHash = "";

				if (cachingType.equalsIgnoreCase("Cache Time")) {
					cacheTime = requestJson.get("metadata").getAsJsonArray().get(0).getAsJsonObject().get("cacheTime")
							.getAsInt();
					cacheDetailsHash = getCacheDetailsHash(cachingType, cacheTime);
					esQuery = esQueryWithCacheTime(currentTime, cacheTime, queryHash, cacheDetailsHash);
				} else {
					cacheVariance = requestJson.get("metadata").getAsJsonArray().get(0).getAsJsonObject()
							.get("cacheVariance").getAsInt();
					cacheDetailsHash = getCacheDetailsHash(cachingType, cacheVariance);
					esQuery = esQueryTemplateWithVariance(cacheVariance, startTime, endTime, queryHash,
							cacheDetailsHash);
				}
				JsonObject esResponse = queryES(sourceESCacheUrl + "/_search", esQuery);
				String cacheResult = "cacheResult";

				JsonArray esResponseArray = esResponse.get("hits").getAsJsonObject().get("hits").getAsJsonArray();
				if (esResponseArray.size() != 0) {
					esResponse = esResponseArray.get(0).getAsJsonObject().get("_source").getAsJsonObject();
				}

				if (esResponseArray.size() == 0 || esResponse.isJsonNull()) {
					JsonObject saveCache = new JsonObject();

					JsonObject graphResponse = null;
					graphResponse = getNeo4jDatasource(requestPayload);
					saveCache.addProperty("queryHash", queryHash);
					saveCache.addProperty("cacheDetailsHash", cacheDetailsHash);
					saveCache.addProperty("cachingType", cachingType);
					if (cachingType.equalsIgnoreCase("Cache Time"))
						saveCache.addProperty("cacheTime", cacheTime);
					else
						saveCache.addProperty("cacheVariance", cacheVariance);
					saveCache.addProperty("startTimeRange", startTime);
					saveCache.addProperty("endTimeRange", endTime);
					saveCache.addProperty(cacheResult, graphResponse.toString());
					saveCache.addProperty("hasExpired", false);
					saveCache.addProperty("creationTime", currentTime);

					queryES(sourceESCacheUrl, saveCache.toString());
					return parser.parse(saveCache.get(cacheResult).getAsString()).getAsJsonObject();
				}
				return parser.parse(esResponse.get(cacheResult).getAsString()).getAsJsonObject();
			} else {
				return getNeo4jDatasource(requestPayload);
			}
		} catch (Exception e) {
			log.error("Error in capturing Elasticsearch response", e);
		}

		return null;
	}

	private static String getCacheDetailsHash(String cacheType, int cachingValue) {
		String cacheDetails = "";
		String cacheDetailsHash = "";
		if (cacheType.equalsIgnoreCase("Cache Time")) {
			cacheDetails = "Cache Type: " + cacheType + " and Cache Time: " + cachingValue;
		} else {
			cacheDetails = "Cache Type: " + cacheType + " and Cache Variance: " + cachingValue;
		}
		cacheDetailsHash = DigestUtils.md5Hex(cacheDetails).toUpperCase();
		return cacheDetailsHash;
	}

	private static String getStatementWithoutTime(String statement, String startTime, String endTime) {
		return statement.replace(String.valueOf(startTime), "?START_TIME?").replace(String.valueOf(endTime),
				"?END_TIME?");
	}

	private static String esQueryTemplateWithVariance(int cacheVariance, Long startTime, Long endTime, String queryHash,
			String cacheDetailsHash) {

		String esQuery = "{\"query\": {    \"bool\": {      \"must\": [        {          \"match\": {            "
				+ "\"queryHash\": \"__queryCache__\"          }        },"
				+ "        {          \"match\": {            \"cacheDetailsHash\": \"__cacheDetailsHash__\"          }        },"
				+ "        {          \"bool\": {"
				+ "            \"must\": [              {                \"range\": {                  \"startTimeRange\": {"
				+ "                    \"gte\": \"__cachedStartTime__\",                    \"lte\": \"__startTime__\",                  "
				+ "  \"format\": \"epoch_millis\"                  }                }              }            ],\"must\": [ "
				+ "             {                \"range\": {                  \"endTimeRange\": {                    "
				+ "\"gte\": \"__cachedEndTime__\",                    \"lte\": \"__endTime__\",                    \"format\": \"epoch_millis\""
				+ "                  }                }              }            ]          }        }      ]    }  },"
				+ "\"sort\":		{		\"creationTime\"		:{		\"order\"		:\"desc\"}		} }";

		long varianceSeconds = getVarianceEpochSeconds(cacheVariance, endTime - startTime);

		esQuery = esQuery
				.replace(String.valueOf("__cachedStartTime__"),
						InsightsUtils.subtractVarianceTime(startTime, varianceSeconds).toString())
				.replace(String.valueOf("__startTime__"),
						InsightsUtils.addVarianceTime(startTime, varianceSeconds).toString())
				.replace(String.valueOf("__endTime__"),
						InsightsUtils.addVarianceTime(endTime, varianceSeconds).toString())
				.replace(String.valueOf("__cachedEndTime__"),
						InsightsUtils.subtractVarianceTime(endTime, varianceSeconds).toString())
				.replace(String.valueOf("__queryCache__"), queryHash)
				.replace(String.valueOf("__cacheDetailsHash__"), cacheDetailsHash);
		return esQuery;
	}

	private static String esQueryWithCacheTime(Long currentTime, int cacheDuration, String queryHash,
			String cacheDetailsHash) {
		String esQuery = "{\"query\": {    \"bool\": {      \"must\": [        {          \"match\": {"
				+ "            \"queryHash\": \"__queryCache__\"          }        },"
				+ "        {          \"match\": {            \"cacheDetailsHash\": \"__cacheDetailsHash__\"          }        },"
				+ "        {          \"bool\": {"
				+ "            \"must\": [              {                \"range\": {                  \"creationTime\": {                    \"gte\": \"__cachePreviousTime__\","
				+ "                    \"lte\": \"__currentTime__\",                    \"format\": \"epoch_millis\"                  }                }              }"
				+ "            ]          }        }      ]    }  },\"sort\":		{		\"creationTime\"		:{		\"order\"		:\"desc\"}		} }";
		esQuery = esQuery.replace(String.valueOf("__queryCache__"), queryHash)
				.replace(String.valueOf("__cacheDetailsHash__"), cacheDetailsHash)
				.replace(String.valueOf("__cachePreviousTime__"),
						InsightsUtils.subtractTimeInHours(currentTime, cacheDuration).toString())
				.replace(String.valueOf("__currentTime__"), currentTime.toString());
		return esQuery;
	}

	private static long getVarianceEpochSeconds(int cacheVariance, Long durationSeconds) {
		return (durationSeconds * cacheVariance) / 100;
	}

	private JsonObject queryES(String sourceESUrl, String query) throws Exception {

		ClientResponse response = null;
		JsonObject data = null;
		try {
			WebResource resource = Client.create().resource(sourceESUrl);
			response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
					.post(ClientResponse.class, query);
			if (response.getStatus() == 201) {
				log.debug("New object created at index - " + sourceESUrl);
			} else if (response.getStatus() != 200) {
				throw new Exception("Failed to get response from ElasticSeach for query - " + query
						+ "-- HTTP response code -" + response.getStatus());
			}
			data = new JsonParser().parse(response.getEntity(String.class)).getAsJsonObject();
		} catch (Exception e) {
			log.error("Exception while getting data from ES for query - " + query, e);
			throw new Exception(e.getMessage());
		} finally {
			if (response != null) {
				response.close();
			}
		}
		return data;
	}
}
