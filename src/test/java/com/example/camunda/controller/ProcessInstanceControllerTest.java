package com.example.camunda.controller;

import com.example.camunda.dto.StartProcessRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises starting process instances for the pre-deployed {@code sampleApprovalProcess}
 * (see src/main/resources/processes/sample-approval-process.bpmn, auto-deployed at startup),
 * both WITH and WITHOUT variables, proving variables are genuinely optional.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProcessInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void start_withVariablesAndBusinessKey_startsProcessInstance() throws Exception {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessDefinitionKey("sampleApprovalProcess");
        request.setBusinessKey("ORDER-1001");

        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 250.75);
        variables.put("approved", false);
        variables.put("requester", "jane");
        request.setVariables(variables);

        MvcResult result = mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processInstanceId").isNotEmpty())
                .andExpect(jsonPath("$.businessKey").value("ORDER-1001"))
                .andExpect(jsonPath("$.ended").value(false))
                .andExpect(jsonPath("$.variables.amount").value(250.75))
                .andExpect(jsonPath("$.variables.requester").value("jane"))
                .andReturn();

        String processInstanceId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("processInstanceId").asText();

        // Variables are retrievable afterwards via the dedicated endpoint.
        mockMvc.perform(get("/api/camunda/process-instances/{id}/variables", processInstanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requester").value("jane"));

        // Status endpoint reports the instance as ACTIVE (waiting at the user task).
        mockMvc.perform(get("/api/camunda/process-instances/{id}", processInstanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.businessKey").value("ORDER-1001"));
    }

    @Test
    void start_withoutAnyVariables_stillStartsProcessInstance() throws Exception {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessDefinitionKey("sampleApprovalProcess");
        // No businessKey, no variables at all — both are optional.

        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processInstanceId").isNotEmpty())
                .andExpect(jsonPath("$.ended").value(false));
    }

    @Test
    void start_withNeitherKeyNorId_returnsBadRequest() throws Exception {
        StartProcessRequest request = new StartProcessRequest();

        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void start_withBothKeyAndId_returnsBadRequest() throws Exception {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessDefinitionKey("sampleApprovalProcess");
        request.setProcessDefinitionId("sampleApprovalProcess:1:some-id");

        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void start_withUnknownProcessDefinitionKey_returnsNotFound() throws Exception {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessDefinitionKey("thisProcessDoesNotExist");

        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void start_processThatCompletesImmediately_reportsEndedAndHistoricVariables() throws Exception {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessDefinitionKey("uploadTestProcess");
        Map<String, Object> variables = new HashMap<>();
        variables.put("note", "no wait states in this process");
        request.setVariables(variables);

        MvcResult result = mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ended").value(true))
                .andExpect(jsonPath("$.variables.note").value("no wait states in this process"))
                .andReturn();

        String processInstanceId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("processInstanceId").asText();

        mockMvc.perform(get("/api/camunda/process-instances/{id}", processInstanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"));
    }

    @Test
    void getVariables_forUnknownProcessInstance_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/camunda/process-instances/{id}/variables", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- add/update variables

    @Test
    void setVariables_addsNewVariableAndUpdatesExisting_leavesOthersUntouched() throws Exception {
        StartProcessRequest startRequest = new StartProcessRequest();
        startRequest.setProcessDefinitionKey("sampleApprovalProcess");
        Map<String, Object> initialVariables = new HashMap<>();
        initialVariables.put("amount", 100.0);
        initialVariables.put("approved", false);
        startRequest.setVariables(initialVariables);

        MvcResult started = mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String processInstanceId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("processInstanceId").asText();

        // Update "amount" (existing) and add "reviewerComment" (new); "approved" is
        // intentionally left out of this call and should remain unchanged.
        Map<String, Object> update = new HashMap<>();
        update.put("amount", 500.0);
        update.put("reviewerComment", "looks good");

        mockMvc.perform(post("/api/camunda/process-instances/{id}/variables", processInstanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.reviewerComment").value("looks good"))
                .andExpect(jsonPath("$.approved").value(false));

        // Confirm the update was actually persisted, not just echoed back.
        mockMvc.perform(get("/api/camunda/process-instances/{id}/variables", processInstanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.reviewerComment").value("looks good"))
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    void setVariables_onUnknownProcessInstance_returnsNotFound() throws Exception {
        Map<String, Object> update = Map.of("foo", "bar");

        mockMvc.perform(post("/api/camunda/process-instances/{id}/variables", "does-not-exist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void setVariables_onAlreadyEndedProcessInstance_returnsConflict() throws Exception {
        StartProcessRequest startRequest = new StartProcessRequest();
        startRequest.setProcessDefinitionKey("uploadTestProcess"); // completes immediately, no wait states

        MvcResult started = mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ended").value(true))
                .andReturn();
        String processInstanceId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("processInstanceId").asText();

        Map<String, Object> update = Map.of("foo", "bar");

        mockMvc.perform(post("/api/camunda/process-instances/{id}/variables", processInstanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict());
    }

    @Test
    void setVariables_withEmptyBody_returnsBadRequest() throws Exception {
        StartProcessRequest startRequest = new StartProcessRequest();
        startRequest.setProcessDefinitionKey("sampleApprovalProcess");

        MvcResult started = mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String processInstanceId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("processInstanceId").asText();

        mockMvc.perform(post("/api/camunda/process-instances/{id}/variables", processInstanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setVariables_withMissingBody_returnsBadRequest() throws Exception {
        StartProcessRequest startRequest = new StartProcessRequest();
        startRequest.setProcessDefinitionKey("sampleApprovalProcess");

        MvcResult started = mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String processInstanceId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("processInstanceId").asText();

        mockMvc.perform(post("/api/camunda/process-instances/{id}/variables", processInstanceId))
                .andExpect(status().isBadRequest());
    }
}
