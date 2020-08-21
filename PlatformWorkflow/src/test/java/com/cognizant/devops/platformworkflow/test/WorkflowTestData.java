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
package com.cognizant.devops.platformworkflow.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.cognizant.devops.platformcommons.core.enums.WorkflowTaskEnum;
import com.cognizant.devops.platformcommons.core.util.InsightsUtils;
import com.cognizant.devops.platformcommons.exception.InsightsCustomException;
import com.cognizant.devops.platformdal.assessmentreport.InsightsAssessmentConfiguration;
import com.cognizant.devops.platformdal.assessmentreport.InsightsAssessmentReportTemplate;
import com.cognizant.devops.platformdal.assessmentreport.InsightsContentConfig;
import com.cognizant.devops.platformdal.assessmentreport.InsightsKPIConfig;
import com.cognizant.devops.platformdal.assessmentreport.InsightsReportsKPIConfig;
import com.cognizant.devops.platformdal.assessmentreport.ReportConfigDAL;
import com.cognizant.devops.platformdal.workflow.InsightsWorkflowConfiguration;
import com.cognizant.devops.platformdal.workflow.InsightsWorkflowExecutionHistory;
import com.cognizant.devops.platformdal.workflow.InsightsWorkflowTask;
import com.cognizant.devops.platformdal.workflow.InsightsWorkflowTaskSequence;
import com.cognizant.devops.platformdal.workflow.InsightsWorkflowType;
import com.cognizant.devops.platformdal.workflow.WorkflowDAL;
import com.cognizant.devops.platformworkflow.workflowtask.core.WorkflowDataHandler;
import com.cognizant.devops.platformworkflow.workflowtask.message.factory.WorkflowTaskSubscriberHandler;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WorkflowTestData {
	
	WorkflowDAL workflowDAL = new WorkflowDAL();
	ReportConfigDAL reportConfigDAL = new ReportConfigDAL();

	
	String kpiDefinition = "{\"kpiId\":1111265,\"name\":\"Avg all employee productivity for threshold \",\"toolName\":\"PRODUCTIVITY\",\"category\":\"THRESHOLD_RANGE\",\"group\":\"PRODUCTIVITY\",\"isActive\":true,\"DBQuery\":\"MATCH (n:PRODUCTIVITY) where n.completionDateEpochTime {startTime} AND n.completionDateEpochTime {endTime} WITH  avg(n.storyPoints*8) as StoryPoint, avg(n.authorTimeSpent) as authorTimeSpent  return   StoryPoint, authorTimeSpent, round((toFloat(StoryPoint)authorTimeSpent)*100) as Productivity\",\"resultField\":\"Productivity\",\"datasource\":\"NEO4J\"}";
	JsonObject kpiDefinitionJson = new JsonParser().parse(kpiDefinition).getAsJsonObject();

	String contentDefinition = "{\"contentId\":1111204,\"isActive\":\"TRUE\",\"expectedTrend\":\"UPWARDS\",\"contentName\":\"Avg Employee Productivity in percentage w.r.t different zones \",\"kpiId\":1111265,\"noOfResult\":15,\"thresholds\":{\"red\":44,\"amber\":50,\"green\":55},\"action\":\"PERCENTAGE\",\"directionOfThreshold\":\"ABOVE\",\"message\":{\"contentMessage\":\"Employees as per productivity percentage w.r.t zones are red {red}%, amber {amber}% , green {green}%\"}}";
	JsonObject contentDefinitionJson = new JsonParser().parse(contentDefinition).getAsJsonObject();

	String workflowTask = "{\"description\":\"TEST_TASK_Execute\",\"mqChannel\":\"WORKFLOW.TEST.TASK.EXCECUTION\",\"componentName\":\"com.cognizant.devops.platformworkflow.test.WorkflowTestTask1Subscriber\",\"dependency\":1,\"workflowType\":\"Report\"}";
	String failWorkflowTask = "{\"description\":\"TEST_FAIL_TASK_Execute\",\"mqChannel\":\"WORKFLOW.TEST.FAIL.TASK.EXCECUTION\",\"componentName\":\"com.cognizant.devops.platformworkflow.test.WorkflowTestFailTaskSubscriber\",\"dependency\":2,\"workflowType\":\"Report\"}";
	String wrongWorkflowTask = "{\"description\":\"WRONG_TEST_TASK\",\"mqChannel\":\"WORKFLOW.TEST.WRONG.TASK\",\"componentName\":\"com.cognizant.devops.platformworkflow.test\",\"dependency\":3,\"workflowType\":\"Report\"}";
	
	String reportTemplate = "{\"reportId\":\"111600\",\"reportName\":\"Productivity\",\"description\":\"Backend Team\",\"isActive\":true,\"kpiConfigs\":[{\"kpiId\":1111265,\"visualizationConfigs\":[{\"vType\":\"1111265_line\",\"vQuery\":\"\"}]}]}";
	JsonObject reportTemplateJson = new JsonParser().parse(reportTemplate).getAsJsonObject();

	String assessmentReport = "{\"reportName\":\"workflow_test\",\"reportTemplate\":111600,\"emailList\":\"xyz@xyz.com\",\"schedule\":\"DAILY\",\"startdate\":null,\"isReoccuring\":true,\"datasource\":\"\"}";
	String assessmentReportFail = "{\"reportName\":\"workflow_fail_test\",\"reportTemplate\":111600,\"emailList\":\"xyz@xyz.com\",\"schedule\":\"DAILY\",\"startdate\":null,\"isReoccuring\":false,\"datasource\":\"\"}";
	String assessmentReportWith2Task = "{\"reportName\":\"workflow_test_with2Task\",\"reportTemplate\":111600,\"emailList\":\"xyz@xyz.com\",\"schedule\":\"DAILY\",\"startdate\":null,\"isReoccuring\":true,\"datasource\":\"\"}";
	String reportWithWrongTask = "{\"reportName\":\"workflow_test_wrongTask\",\"reportTemplate\":111600,\"emailList\":\"xyz@xyz.com\",\"schedule\":\"DAILY\",\"startdate\":null,\"isReoccuring\":false,\"datasource\":\"\"}";
	String assessmentReportTest = "{\"reportName\":\"workflow_testing\",\"reportTemplate\":111600,\"emailList\":\"xyz@xyz.com\",\"schedule\":\"DAILY\",\"startdate\":null,\"isReoccuring\":true,\"datasource\":\"\"}";
	
	public static String workflowId = WorkflowTaskEnum.WorkflowType.REPORT.getValue() + "_" + "1234567";
	public static String failWorkflowId = WorkflowTaskEnum.WorkflowType.REPORT.getValue() + "_" + "123456789";
	public static String WorkflowIdWith2Task = WorkflowTaskEnum.WorkflowType.REPORT.getValue() + "_" + "1234512345";
	public static String WorkflowIdWrongTask = WorkflowTaskEnum.WorkflowType.REPORT.getValue() + "_" + "1234321";
	public static String WorkflowIdTest = WorkflowTaskEnum.WorkflowType.REPORT.getValue() + "_" + "1122334455";
	
	String mqChannel = "WORKFLOW.TEST.TASK.EXCECUTION";
	String mqChannelFail = "WORKFLOW.TEST.FAIL.TASK.EXCECUTION";
	String wrongMqChannel = "WORKFLOW.TEST.WRONG.TASK";
	
	long nextRunDaily = 0;
	
	public static List<Integer> taskidList = new ArrayList<Integer>();


	
	public int saveKpiDefinition(String kpiDefinition) {
		JsonObject kpiJson = new JsonParser().parse(kpiDefinition).getAsJsonObject();
		InsightsKPIConfig kpiConfig = new InsightsKPIConfig();
		int kpiId = kpiJson.get("kpiId").getAsInt();
		InsightsKPIConfig existingConfig = reportConfigDAL.getKPIConfig(kpiId);
		if(existingConfig == null) {
			boolean isActive = kpiJson.get("isActive").getAsBoolean();
			String kpiName = kpiJson.get("name").getAsString();
			String dBQuery = kpiJson.get("DBQuery").getAsString();
			String resultField = kpiJson.get("resultField").getAsString();
			String group = kpiJson.get("group").getAsString();
			String toolName = kpiJson.get("toolName").getAsString();
			String dataSource = kpiJson.get("datasource").getAsString();
			String category = kpiJson.get("category").getAsString();
			kpiConfig.setKpiId(kpiId);
			kpiConfig.setActive(isActive);
			kpiConfig.setKpiName(kpiName);
			kpiConfig.setdBQuery(dBQuery);
			kpiConfig.setResultField(resultField);
			kpiConfig.setToolname(toolName);
			kpiConfig.setGroupName(group);
			kpiConfig.setDatasource(dataSource);
			kpiConfig.setCategory(category);
			reportConfigDAL.saveKpiConfig(kpiConfig);
		}
		return kpiId;
	}
	
	public int saveContentDefinition(String contentDefinition) throws InsightsCustomException {
		InsightsContentConfig contentConfig = new InsightsContentConfig();
		JsonObject contentJson = new JsonParser().parse(contentDefinition).getAsJsonObject();
		Gson gson = new Gson();
		int kpiId = contentJson.get("kpiId").getAsInt();
		int contentId = contentJson.get("contentId").getAsInt();
		InsightsContentConfig existingContentConfig = reportConfigDAL.getContentConfig(contentId);
		if(existingContentConfig == null) {
			boolean contentisActive = contentJson.get("isActive").getAsBoolean();
			String contentName = contentJson.get("contentName").getAsString();
			String contentString = gson.toJson(contentJson);
			contentConfig.setContentId(contentId);
			InsightsKPIConfig kpiConfig = reportConfigDAL.getKPIConfig(kpiId);
			String contentCategory = kpiConfig.getCategory();
			contentConfig.setKpiConfig(kpiConfig);
			contentConfig.setActive(contentisActive);
			contentConfig.setContentJson(contentString);
			contentConfig.setContentName(contentName);
			contentConfig.setCategory(contentCategory);
			reportConfigDAL.saveContentConfig(contentConfig);
		}
		return contentId;
	}
	
	public int saveWorkflowTask(String task) {
		JsonObject taskJson = new JsonParser().parse(task).getAsJsonObject();
		InsightsWorkflowTask taskConfig = new InsightsWorkflowTask();
		InsightsWorkflowTask existingTask = workflowDAL.getTaskbyTaskDescription(taskJson.get("description").getAsString());
		int taskId = -1;
		if(existingTask == null) {
			String description = taskJson.get("description").getAsString();
			String mqChannel = taskJson.get("mqChannel").getAsString();
			String componentName = taskJson.get("componentName").getAsString();
			int dependency = taskJson.get("dependency").getAsInt();
			String workflowType = taskJson.get("workflowType").getAsString();
			taskConfig.setDescription(description);
			taskConfig.setMqChannel(mqChannel);
			taskConfig.setCompnentName(componentName);
			taskConfig.setDependency(dependency);
			InsightsWorkflowType workflowTypeEntity = new InsightsWorkflowType();
			workflowTypeEntity.setWorkflowType(workflowType);
			taskConfig.setWorkflowType(workflowTypeEntity);
			taskId = workflowDAL.saveInsightsWorkflowTaskConfig(taskConfig);
			
		}
		taskidList.add(taskId);
		return taskId;
	}
	
	public int getTaskId(String mqChannel) {
		int taskId = workflowDAL.getTaskId(mqChannel);
		return taskId;
		
	}
	
	public int saveReportTemplate(String reportTemplate) {
		JsonObject reportJson = new JsonParser().parse(reportTemplate).getAsJsonObject();
		Gson gson = new Gson();
		Set<InsightsReportsKPIConfig> reportsKPIConfigSet = new HashSet<>();
		int reportId = reportJson.get("reportId").getAsInt();
		InsightsAssessmentReportTemplate reportEntity = (InsightsAssessmentReportTemplate) reportConfigDAL
				.getActiveReportTemplateByReportId(reportId);
		if(reportEntity == null) {
			String reportName = reportJson.get("reportName").getAsString();
			boolean isActive = reportJson.get("isActive").getAsBoolean();
			String description = reportJson.get("description").getAsString();
			reportEntity = new InsightsAssessmentReportTemplate();
			reportEntity.setReportId(reportId);
			reportEntity.setActive(isActive);
			reportEntity.setDescription(description);
			reportEntity.setTemplateName(reportName);
			JsonArray kpiConfigArray = reportJson.get("kpiConfigs").getAsJsonArray();
			for (JsonElement eachKpiConfig : kpiConfigArray) {
				JsonObject KpiObject = eachKpiConfig.getAsJsonObject();
				int kpiId = KpiObject.get("kpiId").getAsInt();
				String vConfig = gson.toJson(KpiObject.get("visualizationConfigs").getAsJsonArray());
				InsightsKPIConfig kpiConfig = reportConfigDAL.getKPIConfig(kpiId);
				if(kpiConfig != null) {
					InsightsReportsKPIConfig reportsKPIConfig = new InsightsReportsKPIConfig();
					reportsKPIConfig.setKpiConfig(kpiConfig);
					reportsKPIConfig.setvConfig(vConfig);
					reportsKPIConfig.setReportTemplateEntity(reportEntity);
					reportsKPIConfigSet.add(reportsKPIConfig);
				}
			}
			reportEntity.setReportsKPIConfig(reportsKPIConfigSet);
			reportConfigDAL.saveReportConfig(reportEntity);
		}
		return reportId;

	}
	
	public String saveAssessmentReport(String workflowid, String mqChannel, String assessmentReport, String mqChannel2) throws InsightsCustomException {
		int taskId = getTaskId(mqChannel);
		int task2Id = 0;
		if(mqChannel2 != null) {
			task2Id = getTaskId(mqChannel2);
		}
		JsonObject assessmentReportJson = addTask(taskId, assessmentReport, task2Id);
		int reportId = assessmentReportJson.get("reportTemplate").getAsInt();
		InsightsAssessmentReportTemplate reportTemplate = (InsightsAssessmentReportTemplate) reportConfigDAL
				.getActiveReportTemplateByReportId(reportId);
		String workflowType = WorkflowTaskEnum.WorkflowType.REPORT.getValue();
		String reportName = assessmentReportJson.get("reportName").getAsString();
		boolean isActive = true;
		String schedule = assessmentReportJson.get("schedule").getAsString();
		String emailList = assessmentReportJson.get("emailList").getAsString();
		String datasource = assessmentReportJson.get("datasource").getAsString();
		boolean reoccurence = assessmentReportJson.get("isReoccuring").getAsBoolean();
		long epochStartDate = 0;
		long epochEndDate = 0;
		String reportStatus = WorkflowTaskEnum.WorkflowStatus.NOT_STARTED.toString();
		JsonArray taskList = assessmentReportJson.get("tasklist").getAsJsonArray();
//		workflowId = WorkflowTaskEnum.WorkflowType.REPORT.getValue() + "_"
//				+ InsightsUtils.getCurrentTimeInSeconds();
		
		InsightsAssessmentConfiguration assessmentConfig = new InsightsAssessmentConfiguration();
		InsightsWorkflowConfiguration workflowConfig = saveWorkflowConfig(workflowid, isActive,
				reoccurence, schedule, reportStatus, workflowType, taskList, epochStartDate, epochEndDate);
		assessmentConfig.setActive(isActive);
		assessmentConfig.setEmails(emailList);
		assessmentConfig.setInputDatasource(datasource);
		assessmentConfig.setAsseementreportname(reportName);
		assessmentConfig.setStartDate(epochStartDate);
		assessmentConfig.setEndDate(epochEndDate);
		assessmentConfig.setReportTemplateEntity(reportTemplate);
		assessmentConfig.setWorkflowConfig(workflowConfig);
		workflowConfig.setAssessmentConfig(assessmentConfig);
		reportConfigDAL.saveInsightsAssessmentConfig(assessmentConfig);
		return workflowid;
	}
	
	public InsightsWorkflowConfiguration saveWorkflowConfig(String workflowId, boolean isActive, boolean reoccurence,
			String schedule, String reportStatus, String workflowType, JsonArray taskList, long startdate, long enddate) throws InsightsCustomException {
		InsightsWorkflowConfiguration workflowConfig = new InsightsWorkflowConfiguration();
		workflowConfig.setWorkflowId(workflowId);
		workflowConfig.setActive(isActive);
		nextRunDaily = InsightsUtils.getNextRunTime(InsightsUtils.getCurrentTimeInSeconds(), schedule, true);
		workflowConfig.setNextRun(nextRunDaily);
		workflowConfig.setLastRun(0);
		workflowConfig.setReoccurence(reoccurence);
		workflowConfig.setScheduleType(schedule);
		workflowConfig.setStatus(reportStatus);
		workflowConfig.setWorkflowType(workflowType);
		Set<InsightsWorkflowTaskSequence> sequneceEntitySet = setSequence(taskList, workflowConfig);
		workflowConfig.setTaskSequenceEntity(sequneceEntitySet);
		return workflowConfig;
	}
	
	public Set<InsightsWorkflowTaskSequence> setSequence(JsonArray taskList,
			InsightsWorkflowConfiguration workflowConfig) throws InsightsCustomException {
		Set<InsightsWorkflowTaskSequence> sequneceEntitySet = new HashSet<>();
		try {
			Set<InsightsWorkflowTaskSequence> taskSequenceSet = workflowConfig.getTaskSequenceEntity();
			if (!taskSequenceSet.isEmpty()) {
				workflowDAL.deleteWorkflowTaskSequence(workflowConfig.getWorkflowId());
			}
			ArrayList<Integer> sortedTask = new ArrayList<>();
			taskList.forEach(taskObj -> sortedTask.add(taskObj.getAsJsonObject().get("taskId").getAsInt()));
			@SuppressWarnings("unchecked")

			/*
			 * make a clone of list as sortedTask list will be iterated so same list can not
			 * used to get next element
			 */
			ArrayList<Integer> taskListClone = (ArrayList<Integer>) sortedTask.clone();

			int sequenceNo = 1;
			int nextTask = -1;

			ListIterator<Integer> listIterator = sortedTask.listIterator();
			while (listIterator.hasNext()) {

				int taskId = listIterator.next();
				int nextIndex = listIterator.nextIndex();
				if (nextIndex == taskListClone.size()) {
					nextTask = -1;
				} else {
					nextTask = taskListClone.get(nextIndex);
				}
				InsightsWorkflowTask taskEntity = workflowDAL.getTaskByTaskId(taskId);
				InsightsWorkflowTaskSequence taskSequenceEntity = new InsightsWorkflowTaskSequence();
				// Attach each task to sequence
				taskSequenceEntity.setWorkflowTaskEntity(taskEntity);
				taskSequenceEntity.setWorkflowConfig(workflowConfig);
				taskSequenceEntity.setSequence(sequenceNo);
				taskSequenceEntity.setNextTask(nextTask);
				sequneceEntitySet.add(taskSequenceEntity);
				sequenceNo++;

			}

			return sequneceEntitySet;
		} catch (Exception e) {
			throw new InsightsCustomException("Something went wrong while attaching task to workflow");
		}
	}
	
	public JsonObject addTask(int taskId, String assessmentReport, int task2Id) {
		JsonObject assessmentReportJson = new JsonParser().parse(assessmentReport).getAsJsonObject();
		JsonObject task = new JsonObject();
		JsonObject task2 = new JsonObject();
		task.addProperty("taskId", taskId);
		task.addProperty("sequence", 0);
		JsonArray tasklist = new JsonArray();
		tasklist.add(task);
		if(task2Id > 0) {
			task2.addProperty("taskId", task2Id);
			task2.addProperty("sequence", 1);
			tasklist.add(task2);
		}
		assessmentReportJson.add("tasklist", tasklist);
		return assessmentReportJson;
	}
	
	public void deleteExecutionHistory(String workflowId) {
		List<InsightsWorkflowExecutionHistory> executionHistory = workflowDAL.getWorkflowExecutionHistoryByWorkflowId(workflowId);
		if(executionHistory.size() > 0) {
			for(InsightsWorkflowExecutionHistory eachExecutionRecord: executionHistory) {
				if(eachExecutionRecord.getWorkflowConfig().getWorkflowId().equalsIgnoreCase(workflowId)) {
					workflowDAL.deleteExecutionHistory(eachExecutionRecord.getId());
				}
			}
		}
	}
	
	public void initializeTask() {
		try {
			WorkflowTaskSubscriberHandler testsubscriberobject = new WorkflowTestTaskSubscriber(mqChannel);
			WorkflowDataHandler.registry.put(getTaskId(mqChannel), testsubscriberobject);
			WorkflowTaskSubscriberHandler testFailsubscriberobject = new WorkflowTestFailTaskSubscriber(mqChannelFail);
			WorkflowDataHandler.registry.put(getTaskId(mqChannelFail), testFailsubscriberobject);
			
			Thread.sleep(1000);
		} catch (Exception e) {

		}
	}
	
	
	public void delete(String workflowId) {
		deleteExecutionHistory(workflowId);
		InsightsWorkflowConfiguration workflowConfig = workflowDAL.getWorkflowConfigByWorkflowId(workflowId);		
		int id = workflowConfig.getAssessmentConfig().getId();
		reportConfigDAL.deleteAssessmentReport(id);
	}
}