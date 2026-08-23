package com.example.camunda.controller;

import com.example.camunda.dto.CompleteTaskRequest;
import com.example.camunda.dto.UserIdRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the generic user-task REST API against the pre-deployed
 * {@code sampleApprovalProcess} (auto-deployed at startup), which has a single
 * unassigned, candidate-group-only user task ("Review Request").
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    private String startSampleProcessAndGetTaskId() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("sampleApprovalProcess");
        return taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult()
                .getId();
    }

    @Test
    void findTasks_filteredByProcessInstanceId_returnsTheTask() throws Exception {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("sampleApprovalProcess");

        mockMvc.perform(get("/api/camunda/tasks")
                        .param("processInstanceId", instance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].processInstanceId").value(instance.getId()))
                .andExpect(jsonPath("$[0].name").value("Review Request"))
                .andExpect(jsonPath("$[0].assignee").doesNotExist());
    }

    @Test
    void findTasks_withUnassignedFilter_includesFreshlyCreatedTask() throws Exception {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("sampleApprovalProcess");
        String taskId = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult().getId();

        mockMvc.perform(get("/api/camunda/tasks")
                        .param("processDefinitionKey", "sampleApprovalProcess")
                        .param("unassigned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + taskId + "')]").exists());
    }

    @Test
    void getTask_returnsTaskDetails() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();

        mockMvc.perform(get("/api/camunda/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.name").value("Review Request"))
                .andExpect(jsonPath("$.taskDefinitionKey").value("UserTask_Review"));
    }

    @Test
    void getTask_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/camunda/tasks/{taskId}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void assign_setsAssignee() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();

        mockMvc.perform(post("/api/camunda/tasks/{taskId}/assign", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserIdRequest("bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value("bob"));
    }

    @Test
    void assign_missingUserId_returnsBadRequest() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();

        mockMvc.perform(post("/api/camunda/tasks/{taskId}/assign", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserIdRequest(null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assign_unknownTask_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/camunda/tasks/{taskId}/assign", "does-not-exist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserIdRequest("bob"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unassign_clearsAssignee() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();
        taskService.setAssignee(taskId, "bob");

        mockMvc.perform(post("/api/camunda/tasks/{taskId}/unassign", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").doesNotExist());
    }

    @Test
    void claim_setsAssignee() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();

        mockMvc.perform(post("/api/camunda/tasks/{taskId}/claim", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserIdRequest("alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value("alice"));
    }

    @Test
    void claim_alreadyClaimedByAnotherUser_returnsConflict() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();
        taskService.claim(taskId, "alice");

        mockMvc.perform(post("/api/camunda/tasks/{taskId}/claim", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserIdRequest("bob"))))
                .andExpect(status().isConflict());
    }

    @Test
    void unclaim_clearsAssignee() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();
        taskService.claim(taskId, "alice");

        mockMvc.perform(post("/api/camunda/tasks/{taskId}/unclaim", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").doesNotExist());
    }

    @Test
    void setVariables_addsVariables_thenGetVariablesReturnsThem() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("comment", "looks good");
        variables.put("score", 8);

        mockMvc.perform(post("/api/camunda/tasks/{taskId}/variables", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variables)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("looks good"))
                .andExpect(jsonPath("$.score").value(8));

        mockMvc.perform(get("/api/camunda/tasks/{taskId}/variables", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("looks good"));
    }

    @Test
    void getVariables_unknownTask_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/camunda/tasks/{taskId}/variables", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void complete_withVariables_completesTheTask() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();

        CompleteTaskRequest request = new CompleteTaskRequest();
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        request.setVariables(variables);

        mockMvc.perform(post("/api/camunda/tasks/{taskId}/complete", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Task no longer exists once completed.
        mockMvc.perform(get("/api/camunda/tasks/{taskId}", taskId))
                .andExpect(status().isNotFound());
    }

    @Test
    void complete_withoutAnyVariables_stillCompletesTheTask() throws Exception {
        String taskId = startSampleProcessAndGetTaskId();

        mockMvc.perform(post("/api/camunda/tasks/{taskId}/complete", taskId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/camunda/tasks/{taskId}", taskId))
                .andExpect(status().isNotFound());
    }

    @Test
    void complete_unknownTask_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/camunda/tasks/{taskId}/complete", "does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
