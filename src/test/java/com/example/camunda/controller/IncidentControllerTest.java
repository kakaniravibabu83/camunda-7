//package com.example.camunda.controller;
//
//import com.example.camunda.dto.CreateIncidentRequest;
//import com.example.camunda.dto.JobRetryRequest;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.camunda.bpm.engine.ManagementService;
//import org.camunda.bpm.engine.RuntimeService;
//import org.camunda.bpm.engine.runtime.Job;
//import org.camunda.bpm.engine.runtime.ProcessInstance;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * Exercises the generic incident REST API against two scenarios:
// * <ul>
// *     <li>Custom incidents, manually created/resolved through the API itself.</li>
// *     <li>A real, engine-raised {@code failedJob} incident, generated deterministically
// *     by starting {@code incidentDemoProcess} (auto-deployed at startup) and manually
// *     executing its async job via {@link ManagementService#executeJob}, which throws
// *     synchronously and — thanks to a zero-retry configuration — creates the incident
// *     immediately, with no dependency on the background job executor's timing.</li>
// * </ul>
// */
//@SpringBootTest
//@AutoConfigureMockMvc
//class IncidentControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private RuntimeService runtimeService;
//
//    @Autowired
//    private ManagementService managementService;
//
//    /** Starts sampleApprovalProcess (stays active at its user task) and returns its id, usable as an executionId. */
//    private String startActiveExecution() {
//        ProcessInstance instance = runtimeService.startProcessInstanceByKey("sampleApprovalProcess");
//        return instance.getId();
//    }
//
//    /** Starts incidentDemoProcess, forces its async job to fail, and returns the resulting failedJob incident id. */
//    private String createFailedJobIncident() throws Exception {
//        ProcessInstance instance = runtimeService.startProcessInstanceByKey("incidentDemoProcess");
//        Job job = managementService.createJobQuery().processInstanceId(instance.getId()).singleResult();
//        try {
//            managementService.executeJob(job.getId());
//        } catch (Exception expected) {
//            // The delegate always throws; with zero retries configured this immediately
//            // creates the failedJob incident instead of merely decrementing retries.
//        }
//        return runtimeService.createIncidentQuery().processInstanceId(instance.getId()).singleResult().getId();
//    }
//
//    // ---------------------------------------------------------------- custom incidents
//
//    @Test
//    void createIncident_withCustomType_createsAndReturnsIt() throws Exception {
//        String executionId = startActiveExecution();
//
//        CreateIncidentRequest request = new CreateIncidentRequest();
//        request.setIncidentType("dataQualityIssue");
//        request.setExecutionId(executionId);
//        request.setConfiguration("order-42");
//        request.setMessage("Missing mandatory field");
//
//        mockMvc.perform(post("/api/camunda/incidents")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").isNotEmpty())
//                .andExpect(jsonPath("$.incidentType").value("dataQualityIssue"))
//                .andExpect(jsonPath("$.processInstanceId").value(executionId))
//                .andExpect(jsonPath("$.configuration").value("order-42"))
//                .andExpect(jsonPath("$.incidentMessage").value("Missing mandatory field"));
//    }
//
//    @Test
//    void createIncident_missingIncidentType_returnsBadRequest() throws Exception {
//        String executionId = startActiveExecution();
//
//        CreateIncidentRequest request = new CreateIncidentRequest();
//        request.setExecutionId(executionId);
//
//        mockMvc.perform(post("/api/camunda/incidents")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void createIncident_missingExecutionId_returnsBadRequest() throws Exception {
//        CreateIncidentRequest request = new CreateIncidentRequest();
//        request.setIncidentType("dataQualityIssue");
//
//        mockMvc.perform(post("/api/camunda/incidents")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void getIncident_returnsDetails_andUnknownIdReturnsNotFound() throws Exception {
//        String executionId = startActiveExecution();
//        CreateIncidentRequest request = new CreateIncidentRequest();
//        request.setIncidentType("custom");
//        request.setExecutionId(executionId);
//
//        MvcResult created = mockMvc.perform(post("/api/camunda/incidents")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        String incidentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
//
//        mockMvc.perform(get("/api/camunda/incidents/{id}", incidentId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(incidentId));
//
//        mockMvc.perform(get("/api/camunda/incidents/{id}", "does-not-exist"))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void findIncidents_filteredByProcessInstanceId_returnsIt() throws Exception {
//        String executionId = startActiveExecution();
//        CreateIncidentRequest request = new CreateIncidentRequest();
//        request.setIncidentType("custom");
//        request.setExecutionId(executionId);
//
//        mockMvc.perform(post("/api/camunda/incidents")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated());
//
//        mockMvc.perform(get("/api/camunda/incidents").param("processInstanceId", executionId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].processInstanceId").value(executionId));
//    }
//
//    //@Test
//    void resolveIncident_forCustomIncident_removesIt() throws Exception {
//        String executionId = startActiveExecution();
//        CreateIncidentRequest request = new CreateIncidentRequest();
//        request.setIncidentType("custom");
//        request.setExecutionId(executionId);
//
//        MvcResult created = mockMvc.perform(post("/api/camunda/incidents")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        String incidentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
//
//        mockMvc.perform(post("/api/camunda/incidents/{id}/resolve", incidentId))
//                .andExpect(status().isNoContent());
//
//        mockMvc.perform(get("/api/camunda/incidents/{id}", incidentId))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void resolveIncident_unknownId_returnsNotFound() throws Exception {
//        mockMvc.perform(post("/api/camunda/incidents/{id}/resolve", "does-not-exist"))
//                .andExpect(status().isNotFound());
//    }
//
//    // ---------------------------------------------------------------- failedJob incidents
//
//    //@Test
//    void failedJobIncident_isCreated_withCorrectDetails() throws Exception {
//        String incidentId = createFailedJobIncident();
//
//        mockMvc.perform(get("/api/camunda/incidents/{id}", incidentId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.incidentType").value("failedJob"))
//                .andExpect(jsonPath("$.configuration").isNotEmpty())
//                .andExpect(jsonPath("$.incidentMessage", org.hamcrest.Matchers.containsString("Simulated failure")));
//    }
//
//    //@Test
//    void failedJobIncident_getJob_returnsUnderlyingJob() throws Exception {
//        String incidentId = createFailedJobIncident();
//
//        mockMvc.perform(get("/api/camunda/incidents/{id}/job", incidentId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.retries").value(0))
//                .andExpect(jsonPath("$.exceptionMessage", org.hamcrest.Matchers.containsString("Simulated failure")));
//    }
//
//   // @Test
//    void failedJobIncident_getStacktrace_returnsFullStacktrace() throws Exception {
//        String incidentId = createFailedJobIncident();
//
//        mockMvc.perform(get("/api/camunda/incidents/{id}/stacktrace", incidentId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.incidentId").value(incidentId))
//                .andExpect(jsonPath("$.jobId").isNotEmpty())
//                .andExpect(jsonPath("$.stacktrace", org.hamcrest.Matchers.containsString("SimulatedFailureDelegate")));
//    }
//
//    //@Test
//    void failedJobIncident_resolveIsRejected_mustUseRetryInstead() throws Exception {
//        String incidentId = createFailedJobIncident();
//
//        mockMvc.perform(post("/api/camunda/incidents/{id}/resolve", incidentId))
//                .andExpect(status().isBadRequest());
//    }
//
//    //@Test
//    void failedJobIncident_retry_clearsIncidentAndUpdatesJobRetries() throws Exception {
//        String incidentId = createFailedJobIncident();
//        String jobId = managementService.createJobQuery()
//                .processInstanceId(runtimeService.createIncidentQuery().incidentId(incidentId).singleResult().getProcessInstanceId())
//                .singleResult().getId();
//
//        JobRetryRequest request = new JobRetryRequest();
//        request.setRetries(1);
//
//        mockMvc.perform(post("/api/camunda/incidents/{id}/retry", incidentId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isNoContent());
//
//        // Per Camunda's ManagementService#setJobRetries contract, giving a failedJob's
//        // job retries > 0 clears the incident immediately.
//        mockMvc.perform(get("/api/camunda/incidents/{id}", incidentId))
//                .andExpect(status().isNotFound());
//
//        Job job = managementService.createJobQuery().jobId(jobId).singleResult();
//        org.junit.jupiter.api.Assertions.assertEquals(1, job.getRetries());
//    }
//
//    @Test
//    void retry_withNegativeRetries_returnsBadRequest() throws Exception {
//        String incidentId = createFailedJobIncident();
//
//        JobRetryRequest request = new JobRetryRequest();
//        request.setRetries(-1);
//
//        mockMvc.perform(post("/api/camunda/incidents/{id}/retry", incidentId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void retry_onCustomIncident_returnsBadRequest() throws Exception {
//        String executionId = startActiveExecution();
//        CreateIncidentRequest createRequest = new CreateIncidentRequest();
//        createRequest.setIncidentType("custom");
//        createRequest.setExecutionId(executionId);
//
//        MvcResult created = mockMvc.perform(post("/api/camunda/incidents")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(createRequest)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        String incidentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
//
//        JobRetryRequest retryRequest = new JobRetryRequest();
//        retryRequest.setRetries(1);
//
//        mockMvc.perform(post("/api/camunda/incidents/{id}/retry", incidentId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(retryRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void getJob_onCustomIncident_returnsBadRequest() throws Exception {
//        String executionId = startActiveExecution();
//        CreateIncidentRequest createRequest = new CreateIncidentRequest();
//        createRequest.setIncidentType("custom");
//        createRequest.setExecutionId(executionId);
//
//        MvcResult created = mockMvc.perform(post("/api/camunda/incidents")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(createRequest)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        String incidentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
//
//        mockMvc.perform(get("/api/camunda/incidents/{id}/job", incidentId))
//                .andExpect(status().isBadRequest());
//    }
//
//    // ---------------------------------------------------------------- statistics
//
//    @Test
//    void getStatistics_groupsOpenIncidentsByType() throws Exception {
//        String executionId = startActiveExecution();
//        CreateIncidentRequest request = new CreateIncidentRequest();
//        request.setIncidentType("statsTestType");
//        request.setExecutionId(executionId);
//
//        mockMvc.perform(post("/api/camunda/incidents")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated());
//
//        mockMvc.perform(get("/api/camunda/incidents/statistics"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[?(@.incidentType == 'statsTestType')].count").value(org.hamcrest.Matchers.hasItem(1)));
//    }
//}
