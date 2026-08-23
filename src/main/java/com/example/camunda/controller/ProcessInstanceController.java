package com.example.camunda.controller;

import com.example.camunda.dto.ProcessInstanceStatusResponse;
import com.example.camunda.dto.StartProcessRequest;
import com.example.camunda.dto.StartProcessResponse;
import com.example.camunda.service.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Generic REST API to start a process instance for ANY deployed process definition,
 * with variables that are entirely optional.
 */
@RestController
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;

    /**
     * Start any process instance, with or without variables.
     *
     * POST /api/camunda/process-instances/start
     * {
     *   "processDefinitionKey": "sampleApprovalProcess",
     *   "businessKey": "ORDER-1001",
     *   "variables": { "amount": 250.75, "approved": false, "requester": "jane" }
     * }
     */
    @PostMapping("/api/camunda/process-instances/start")
    @ResponseStatus(HttpStatus.CREATED)
    public StartProcessResponse start(@RequestBody StartProcessRequest request) {
        return processInstanceService.start(request);
    }

    @GetMapping("/api/camunda/process-instances/{processInstanceId}/variables")
    public Map<String, Object> getVariables(@PathVariable String processInstanceId) {
        return processInstanceService.getVariables(processInstanceId);
    }

    @GetMapping("/api/camunda/process-instances/{processInstanceId}")
    public ProcessInstanceStatusResponse getInstance(@PathVariable String processInstanceId) {
        return processInstanceService.getStatus(processInstanceId);
    }
}
