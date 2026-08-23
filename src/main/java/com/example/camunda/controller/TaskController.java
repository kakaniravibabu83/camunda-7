package com.example.camunda.controller;

import com.example.camunda.dto.CompleteTaskRequest;
import com.example.camunda.dto.TaskInfo;
import com.example.camunda.dto.UserIdRequest;
import com.example.camunda.service.TaskManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Generic REST API for Camunda user tasks — works for a task belonging to ANY
 * deployed process definition.
 */
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskManagementService taskManagementService;

    /**
     * List/search user tasks. All query params are optional and combine as AND filters.
     *
     * GET /api/camunda/tasks?processInstanceId=...&processDefinitionKey=...&assignee=...
     *     &candidateGroup=...&candidateUser=...&taskDefinitionKey=...&unassigned=true
     */
    @GetMapping("/api/camunda/tasks")
    public List<TaskInfo> findTasks(
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String candidateGroup,
            @RequestParam(required = false) String candidateUser,
            @RequestParam(required = false) String taskDefinitionKey,
            @RequestParam(required = false) Boolean unassigned) {
        return taskManagementService.findTasks(processInstanceId, processDefinitionKey, assignee,
                candidateGroup, candidateUser, taskDefinitionKey, unassigned);
    }

    /** GET /api/camunda/tasks/{taskId} */
    @GetMapping("/api/camunda/tasks/{taskId}")
    public TaskInfo getTask(@PathVariable String taskId) {
        return taskManagementService.getTask(taskId);
    }

    /** GET /api/camunda/tasks/{taskId}/variables */
    @GetMapping("/api/camunda/tasks/{taskId}/variables")
    public Map<String, Object> getVariables(@PathVariable String taskId) {
        return taskManagementService.getVariables(taskId);
    }

    /**
     * Add/update one or more variables on the task without completing it.
     * Body is a plain JSON object of variable name -> value, e.g. {"comment": "looks good"}.
     */
    @PostMapping("/api/camunda/tasks/{taskId}/variables")
    public Map<String, Object> setVariables(@PathVariable String taskId,
                                             @RequestBody(required = false) Map<String, Object> variables) {
        return taskManagementService.setVariables(taskId, variables);
    }

    /** POST /api/camunda/tasks/{taskId}/assign  { "userId": "jane" } */
    @PostMapping("/api/camunda/tasks/{taskId}/assign")
    public TaskInfo assign(@PathVariable String taskId, @RequestBody UserIdRequest request) {
        return taskManagementService.assign(taskId, request.getUserId());
    }

    /** POST /api/camunda/tasks/{taskId}/unassign */
    @PostMapping("/api/camunda/tasks/{taskId}/unassign")
    public TaskInfo unassign(@PathVariable String taskId) {
        return taskManagementService.unassign(taskId);
    }

    /** POST /api/camunda/tasks/{taskId}/claim  { "userId": "jane" } */
    @PostMapping("/api/camunda/tasks/{taskId}/claim")
    public TaskInfo claim(@PathVariable String taskId, @RequestBody UserIdRequest request) {
        return taskManagementService.claim(taskId, request.getUserId());
    }

    /** POST /api/camunda/tasks/{taskId}/unclaim */
    @PostMapping("/api/camunda/tasks/{taskId}/unclaim")
    public TaskInfo unclaim(@PathVariable String taskId) {
        return taskManagementService.unclaim(taskId);
    }

    /**
     * POST /api/camunda/tasks/{taskId}/complete
     * Body (optional): { "variables": { "approved": true, "comment": "lgtm" } }
     */
    @PostMapping("/api/camunda/tasks/{taskId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable String taskId,
                          @RequestBody(required = false) CompleteTaskRequest request) {
        Map<String, Object> variables = request != null ? request.getVariables() : null;
        taskManagementService.complete(taskId, variables);
    }
}
