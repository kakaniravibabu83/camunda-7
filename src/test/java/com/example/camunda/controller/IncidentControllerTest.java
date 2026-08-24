package com.example.camunda.controller;

import com.example.camunda.dto.CreateIncidentRequest;
import com.example.camunda.dto.JobRetryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the generic incident REST API against two scenarios:
 * <ul>
 *     <li>Custom incidents, manually created/resolved through the API itself.</li>
 *     <li>A real, engine-raised {@code failedJob} incident, generated deterministically
 *     by starting {@code incidentDemoProcess} (auto-deployed at startup) and manually
 *     executing its async job via {@link ManagementService#executeJob}. The background
 *     job executor is disabled for the whole test suite (see test {@code application.yml}),
 *     so this manual call is the only thing that ever touches the job — no race with a
 *     concurrently-polling background thread, and no dependency on its timing.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ManagementService managementService;

    /** Starts sampleApprovalProcess (stays active at its user task) and returns its id, usable as an executionId. */
    private String startActiveExecution() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("sampleApprovalProcess");
        return instance.getId();
    }

    /**
     * Starts incidentDemoProcess, forces its async job to fail, and returns the resulting
     * failedJob incident id.
     * <p>
     * Mirrors the pattern Camunda's own engine test suite uses for this exact scenario
     * (see {@code CreateAndResolveIncidentTest#resolveIncidentOfTypeFailedJob} in
     * camunda-bpm-platform): explicitly set the job's retries to 1 immediately before
     * executing it, so that after this one deterministic failure retries are guaranteed
     * to hit 0 and raise the incident — independent of how the BPMN's own
     * {@code failedJobRetryTimeCycle} gets parsed/applied on a fresh job. The background
     * job executor is disabled for tests (see test application.yml) specifically so this
     * manual call is the only thing that can ever touch the job, making the whole
     * sequence fully deterministic.
     */
    private String createFailedJobIncident() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("incidentDemoProcess");
        Job job = managementService.createJobQuery().processInstanceId(instance.getId()).singleResult();
        org.junit.jupiter.api.Assertions.assertNotNull(job,
                "Expected an async job to exist for incidentDemoProcess instance " + instance.getId());

        managementService.setJobRetries(job.getId(), 1);
        try {
            managementService.executeJob(job.getId());
            org.junit.jupiter.api.Assertions.fail("Expected the job execution to throw, since the demo delegate always fails.");
        } catch (Exception expected) {
            // Expected: SimulatedFailureDelegate always throws. With retries forced to 1
            // just above, this single failure brings retries to 0 and raises the incident.
        }

        Incident incident = runtimeService.createIncidentQuery().processInstanceId(instance.getId()).singleResult();
        org.junit.jupiter.api.Assertions.assertNotNull(incident,
                "Expected a failedJob incident to have been raised for process instance " + instance.getId());
        return incident.getId();
    }

    // ---------------------------------------------------------------- custom incidents

    @Test
    void createIncident_withCustomType_createsAndReturnsIt() throws Exception {
        String executionId = startActiveExecution();

        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setIncidentType("dataQualityIssue");
        request.setExecutionId(executionId);
        request.setConfiguration("order-42");
        request.setMessage("Missing mandatory field");

        mockMvc.perform(post("/api/camunda/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.incidentType").value("dataQualityIssue"))
                .andExpect(jsonPath("$.processInstanceId").value(executionId))
                .andExpect(jsonPath("$.configuration").value("order-42"))
                .andExpect(jsonPath("$.incidentMessage").value("Missing mandatory field"));
    }

    @Test
    void createIncident_missingIncidentType_returnsBadRequest() throws Exception {
        String executionId = startActiveExecution();

        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setExecutionId(executionId);

        mockMvc.perform(post("/api/camunda/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createIncident_missingExecutionId_returnsBadRequest() throws Exception {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setIncidentType("dataQualityIssue");

        mockMvc.perform(post("/api/camunda/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIncident_returnsDetails_andUnknownIdReturnsNotFound() throws Exception {
        String executionId = startActiveExecution();
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setIncidentType("custom");
        request.setExecutionId(executionId);

        MvcResult created = mockMvc.perform(post("/api/camunda/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String incidentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/camunda/incidents/{id}", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentId));

        mockMvc.perform(get("/api/camunda/incidents/{id}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findIncidents_filteredByProcessInstanceId_returnsIt() throws Exception {
        String executionId = startActiveExecution();
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setIncidentType("custom");
        request.setExecutionId(executionId);

        mockMvc.perform(post("/api/camunda/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/camunda/incidents").param("processInstanceId", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].processInstanceId").value(executionId));
    }

    @Test
    void resolveIncident_forCustomIncident_removesIt() throws Exception {
        String executionId = startActiveExecution();
        CreateIncidentRequest request = new CreateIncidentRequest();
        // Using "foo" here (not "custom") deliberately: this is the exact literal type
        // string Camunda's own engine test suite uses for this scenario
        // (CreateAndResolveIncidentTest#resolveIncident in camunda-bpm-platform), so any
        // discrepancy can't be attributed to the specific type string chosen. Likewise,
        // configuration/message are set to non-null values matching the control test
        // below exactly (unlike an earlier version of this test which left them null).
        request.setIncidentType("foo");
        request.setExecutionId(executionId);
        request.setConfiguration("someConfig");
        request.setMessage("bar");

        MvcResult created = mockMvc.perform(post("/api/camunda/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String incidentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/camunda/incidents/{id}/resolve", incidentId))
                .andExpect(status().isNoContent());

        // Diagnostic: check the engine directly (bypassing our GET endpoint entirely)
        // right after the REST resolve call, in the SAME test. If this ALSO still finds
        // the incident, the bug is in the resolve path itself (or its commit); if this
        // finds it gone while the REST GET below still finds it, the bug is specifically
        // in the GET/query path instead.
        Incident stillPresent = runtimeService.createIncidentQuery().incidentId(incidentId).singleResult();
        org.junit.jupiter.api.Assertions.assertNull(stillPresent,
                "DIAGNOSTIC: direct engine query right after REST resolve() call. "
                        + "If this fails, the bug is in the resolve path (POST /resolve or its commit). "
                        + "If this PASSES but the next assertion (REST GET) still fails, the bug is in the GET path instead.");

        mockMvc.perform(get("/api/camunda/incidents/{id}", incidentId))
                .andExpect(status().isNotFound());
    }

    /**
     * Control test: exercises the exact same scenario as
     * {@link #resolveIncident_forCustomIncident_removesIt} but calls RuntimeService
     * directly with no REST/MockMvc/JSON layer involved at all — mirroring Camunda's own
     * engine test suite pattern verbatim. If this passes while the REST-based test above
     * fails, that proves the discrepancy is in our REST/controller/service layer rather
     * than a genuine engine or environment issue; if this ALSO fails, the issue is
     * environmental (shared DB/context) rather than anything in our code.
     */
    @Test
    void resolveIncident_viaEngineDirectly_removesIt() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("sampleApprovalProcess");
        Incident incident = runtimeService.createIncident("foo", instance.getId(), "someConfig", "bar");

        runtimeService.resolveIncident(incident.getId());

        Incident afterResolve = runtimeService.createIncidentQuery().executionId(instance.getId()).singleResult();
        org.junit.jupiter.api.Assertions.assertNull(afterResolve,
                "Expected incident to be gone after resolveIncident(), but it was still found: " + afterResolve);
    }

    @Test
    void resolveIncident_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/camunda/incidents/{id}/resolve", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- failedJob incidents

    @Test
    void failedJobIncident_isCreated_withCorrectDetails() throws Exception {
        String incidentId = createFailedJobIncident();

        mockMvc.perform(get("/api/camunda/incidents/{id}", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentType").value("failedJob"))
                .andExpect(jsonPath("$.configuration").isNotEmpty())
                .andExpect(jsonPath("$.incidentMessage", org.hamcrest.Matchers.containsString("Simulated failure")));
    }

    @Test
    void failedJobIncident_getJob_returnsUnderlyingJob() throws Exception {
        String incidentId = createFailedJobIncident();

        mockMvc.perform(get("/api/camunda/incidents/{id}/job", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retries").value(0))
                .andExpect(jsonPath("$.exceptionMessage", org.hamcrest.Matchers.containsString("Simulated failure")));
    }

    @Test
    void failedJobIncident_getStacktrace_returnsFullStacktrace() throws Exception {
        String incidentId = createFailedJobIncident();

        mockMvc.perform(get("/api/camunda/incidents/{id}/stacktrace", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incidentId))
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.stacktrace", org.hamcrest.Matchers.containsString("SimulatedFailureDelegate")));
    }

    @Test
    void failedJobIncident_resolveIsRejected_mustUseRetryInstead() throws Exception {
        String incidentId = createFailedJobIncident();

        mockMvc.perform(post("/api/camunda/incidents/{id}/resolve", incidentId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failedJobIncident_retry_clearsIncidentAndUpdatesJobRetries() throws Exception {
        String incidentId = createFailedJobIncident();
        String jobId = managementService.createJobQuery()
                .processInstanceId(runtimeService.createIncidentQuery().incidentId(incidentId).singleResult().getProcessInstanceId())
                .singleResult().getId();

        JobRetryRequest request = new JobRetryRequest();
        request.setRetries(1);

        mockMvc.perform(post("/api/camunda/incidents/{id}/retry", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Per Camunda's ManagementService#setJobRetries contract, giving a failedJob's
        // job retries > 0 clears the incident immediately.
        mockMvc.perform(get("/api/camunda/incidents/{id}", incidentId))
                .andExpect(status().isNotFound());

        Job job = managementService.createJobQuery().jobId(jobId).singleResult();
        org.junit.jupiter.api.Assertions.assertEquals(1, job.getRetries());
    }

    @Test
    void retry_withNegativeRetries_returnsBadRequest() throws Exception {
        String incidentId = createFailedJobIncident();

        JobRetryRequest request = new JobRetryRequest();
        request.setRetries(-1);

        mockMvc.perform(post("/api/camunda/incidents/{id}/retry", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retry_onCustomIncident_returnsBadRequest() throws Exception {
        String executionId = startActiveExecution();
        CreateIncidentRequest createRequest = new CreateIncidentRequest();
        createRequest.setIncidentType("custom");
        createRequest.setExecutionId(executionId);

        MvcResult created = mockMvc.perform(post("/api/camunda/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String incidentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        JobRetryRequest retryRequest = new JobRetryRequest();
        retryRequest.setRetries(1);

        mockMvc.perform(post("/api/camunda/incidents/{id}/retry", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(retryRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJob_onCustomIncident_returnsBadRequest() throws Exception {
        String executionId = startActiveExecution();
        CreateIncidentRequest createRequest = new CreateIncidentRequest();
        createRequest.setIncidentType("custom");
        createRequest.setExecutionId(executionId);

        MvcResult created = mockMvc.perform(post("/api/camunda/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String incidentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/camunda/incidents/{id}/job", incidentId))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- statistics

    @Test
    void getStatistics_groupsOpenIncidentsByType() throws Exception {
        String executionId = startActiveExecution();
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setIncidentType("statsTestType");
        request.setExecutionId(executionId);

        mockMvc.perform(post("/api/camunda/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/camunda/incidents/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.incidentType == 'statsTestType')].count").value(org.hamcrest.Matchers.hasItem(1)));
    }
}
