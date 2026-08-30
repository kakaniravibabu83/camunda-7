//package com.example.camunda.controller;
//
//import com.example.camunda.dto.CancelActivityRequest;
//import com.example.camunda.dto.StartProcessRequest;
//import com.example.camunda.dto.TriggerActivityRequest;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * End-to-end proof that {@code caseManagementProcess} (auto-deployed at startup) works
// * as designed for an external case management UI: starting a case creates a default
// * "SAM" task, and any of the five named tasks can be triggered on demand, in any order,
// * any number of times, via {@code trigger-activity} — completely independent of the
// * process definition's own gateway logic, which only ever routes to SAM by default.
// * <p>
// * Exercised entirely through the app's REST API (process-instances/start,
// * trigger-activity, cancel-activity, plus the existing generic task endpoints), exactly
// * as an external UI (or, for now, a direct API caller) would use it.
// */
//@SpringBootTest
//@AutoConfigureMockMvc
//class CaseManagementProcessTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private String startCase() throws Exception {
//        StartProcessRequest request = new StartProcessRequest();
//        request.setProcessDefinitionKey("caseManagementProcess");
//
//        MvcResult result = mockMvc.perform(post("/api/camunda/process-instances/start")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        return objectMapper.readTree(result.getResponse().getContentAsString())
//                .get("processInstanceId").asText();
//    }
//
//    private String triggerActivityAndGetTaskId(String processInstanceId, String activityId) throws Exception {
//        TriggerActivityRequest request = new TriggerActivityRequest();
//        request.setActivityId(activityId);
//
//        MvcResult result = mockMvc.perform(post("/api/camunda/process-instances/{id}/trigger-activity", processInstanceId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].taskDefinitionKey").value(activityId))
//                .andReturn();
//        return objectMapper.readTree(result.getResponse().getContentAsString())
//                .get(0).get("id").asText();
//    }
//
//    @Test
//    void startingACase_autoCreatesTheDefaultSamTask() throws Exception {
//        String processInstanceId = startCase();
//
//        mockMvc.perform(get("/api/camunda/tasks").param("processInstanceId", processInstanceId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].taskDefinitionKey").value("UserTask_Sam"))
//                .andExpect(jsonPath("$[0].name").value("SAM"));
//    }
//
//    @Test
//    void triggeringLegalReview_createsItAlongsideSam_bothVisibleInTaskList() throws Exception {
//        String processInstanceId = startCase();
//
//        triggerActivityAndGetTaskId(processInstanceId, "UserTask_LegalReview");
//
//        mockMvc.perform(get("/api/camunda/tasks").param("processInstanceId", processInstanceId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2))
//                .andExpect(jsonPath("$[?(@.taskDefinitionKey == 'UserTask_Sam')]").exists())
//                .andExpect(jsonPath("$[?(@.taskDefinitionKey == 'UserTask_LegalReview')]").exists());
//    }
//
//    @Test
//    void anyOfTheFiveTasksCanBeTriggeredInAnyOrder_notJustBusinessConfirmationFirst() throws Exception {
//        String processInstanceId = startCase();
//
//        // Deliberately out of "numbered" order to prove there's no enforced sequence:
//        // Procurement, then Finance Approval, then Business Confirmation.
//        String procurementTaskId = triggerActivityAndGetTaskId(processInstanceId, "UserTask_Procurement");
//        String financeTaskId = triggerActivityAndGetTaskId(processInstanceId, "UserTask_FinanceApproval");
//        String confirmationTaskId = triggerActivityAndGetTaskId(processInstanceId, "UserTask_BusinessConfirmation");
//
//        mockMvc.perform(get("/api/camunda/tasks").param("processInstanceId", processInstanceId))
//                .andExpect(status().isOk())
//                // SAM + the 3 just triggered = 4 open tasks
//                .andExpect(jsonPath("$.length()").value(4));
//
//        // All three complete independently via the existing generic task-complete endpoint.
//        mockMvc.perform(post("/api/camunda/tasks/{id}/complete", procurementTaskId))
//                .andExpect(status().isNoContent());
//        mockMvc.perform(post("/api/camunda/tasks/{id}/complete", financeTaskId))
//                .andExpect(status().isNoContent());
//        mockMvc.perform(post("/api/camunda/tasks/{id}/complete", confirmationTaskId))
//                .andExpect(status().isNoContent());
//
//        // SAM remains open throughout - completing the on-demand tasks doesn't touch it.
//        mockMvc.perform(get("/api/camunda/tasks").param("processInstanceId", processInstanceId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].taskDefinitionKey").value("UserTask_Sam"));
//    }
//
//    @Test
//    void sameTaskCanBeTriggeredMultipleTimesConcurrently() throws Exception {
//        String processInstanceId = startCase();
//
//        String legalReview1 = triggerActivityAndGetTaskId(processInstanceId, "UserTask_LegalReview");
//        // Trigger a second, independent Legal Review instance before the first completes.
//        TriggerActivityRequest secondRequest = new TriggerActivityRequest();
//        secondRequest.setActivityId("UserTask_LegalReview");
//
//        mockMvc.perform(post("/api/camunda/process-instances/{id}/trigger-activity", processInstanceId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(secondRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2)); // both concurrent instances returned
//
//        mockMvc.perform(get("/api/camunda/tasks")
//                        .param("processInstanceId", processInstanceId)
//                        .param("taskDefinitionKey", "UserTask_LegalReview"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2));
//    }
//
//    @Test
//    void triggerActivity_withVariables_setsThemOnTheProcessInstance() throws Exception {
//        String processInstanceId = startCase();
//
//        TriggerActivityRequest request = new TriggerActivityRequest();
//        request.setActivityId("UserTask_BusinessApproval");
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("requestedBy", "case-management-ui");
//        request.setVariables(variables);
//
//        mockMvc.perform(post("/api/camunda/process-instances/{id}/trigger-activity", processInstanceId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk());
//
//        mockMvc.perform(get("/api/camunda/process-instances/{id}/variables", processInstanceId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.requestedBy").value("case-management-ui"));
//    }
//
//    @Test
//    void triggerActivity_missingActivityId_returnsBadRequest() throws Exception {
//        String processInstanceId = startCase();
//        TriggerActivityRequest request = new TriggerActivityRequest();
//
//        mockMvc.perform(post("/api/camunda/process-instances/{id}/trigger-activity", processInstanceId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void triggerActivity_unknownActivityId_returnsBadRequest() throws Exception {
//        String processInstanceId = startCase();
//        TriggerActivityRequest request = new TriggerActivityRequest();
//        request.setActivityId("NotARealActivityId");
//
//        mockMvc.perform(post("/api/camunda/process-instances/{id}/trigger-activity", processInstanceId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void triggerActivity_onUnknownProcessInstance_returnsNotFound() throws Exception {
//        TriggerActivityRequest request = new TriggerActivityRequest();
//        request.setActivityId("UserTask_LegalReview");
//
//        mockMvc.perform(post("/api/camunda/process-instances/{id}/trigger-activity", "does-not-exist")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void closingACase_cancelsSamAndAnyOpenTasks_processInstanceEnds() throws Exception {
//        String processInstanceId = startCase();
//        triggerActivityAndGetTaskId(processInstanceId, "UserTask_LegalReview");
//        triggerActivityAndGetTaskId(processInstanceId, "UserTask_FinanceApproval");
//
//        CancelActivityRequest closeRequest = new CancelActivityRequest();
//        closeRequest.setActivityId("SubProcess_CaseTasks");
//
//        mockMvc.perform(post("/api/camunda/process-instances/{id}/cancel-activity", processInstanceId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(closeRequest)))
//                .andExpect(status().isNoContent());
//
//        // Process instance has completed - no more open tasks, and it's gone from the runtime view.
//        mockMvc.perform(get("/api/camunda/tasks").param("processInstanceId", processInstanceId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(0));
//
//        mockMvc.perform(get("/api/camunda/process-instances/{id}", processInstanceId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.state").value("COMPLETED"));
//    }
//
//    @Test
//    void cancelActivity_onAlreadyEndedProcessInstance_returnsConflict() throws Exception {
//        String processInstanceId = startCase();
//        CancelActivityRequest closeRequest = new CancelActivityRequest();
//        closeRequest.setActivityId("SubProcess_CaseTasks");
//
//        mockMvc.perform(post("/api/camunda/process-instances/{id}/cancel-activity", processInstanceId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(closeRequest)))
//                .andExpect(status().isNoContent());
//
//        // Case is now closed; closing it again should report a conflict, not silently succeed.
//        mockMvc.perform(post("/api/camunda/process-instances/{id}/cancel-activity", processInstanceId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(closeRequest)))
//                .andExpect(status().isConflict());
//    }
//}
