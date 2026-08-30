package com.example.camunda.controller;

import com.example.camunda.dto.CancelActivityRequest;
import com.example.camunda.dto.ProcessInstanceStatusResponse;
import com.example.camunda.dto.StartProcessRequest;
import com.example.camunda.dto.StartProcessResponse;
import com.example.camunda.dto.TaskInfo;
import com.example.camunda.dto.TriggerActivityRequest;
import com.example.camunda.service.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

    /**
     * Add one or more new variables, or update the value of existing ones, on a running
     * process instance. Existing variables not included in the body are left untouched.
     * Only works while the process instance is still active — 409 if it has already
     * ended.
     *
     * POST /api/camunda/process-instances/{processInstanceId}/variables
     * { "amount": 300.00, "approved": true }
     */
    @PostMapping("/api/camunda/process-instances/{processInstanceId}/variables")
    public Map<String, Object> setVariables(@PathVariable String processInstanceId,
                                             @RequestBody Map<String, Object> variables) {
        return processInstanceService.setVariables(processInstanceId, variables);
    }

    @GetMapping("/api/camunda/process-instances/{processInstanceId}")
    public ProcessInstanceStatusResponse getInstance(@PathVariable String processInstanceId) {
        return processInstanceService.getStatus(processInstanceId);
    }

    /**
     * Correlates a named BPMN message to a running process instance — a generic
     * building block for processes that model branches as message-triggered Receive
     * Tasks / message event sub-processes. {@code variables} is the message's payload
     * and is entirely optional. 409 if the process instance isn't currently able to
     * receive this message (e.g. it has already ended, or isn't currently waiting for
     * it).
     *
     * POST /api/camunda/process-instances/{processInstanceId}/messages/{messageName}
     */
    @PostMapping("/api/camunda/process-instances/{processInstanceId}/messages/{messageName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void correlateMessage(@PathVariable String processInstanceId, @PathVariable String messageName,
                                  @RequestBody(required = false) Map<String, Object> variables) {
        processInstanceService.correlateMessage(processInstanceId, messageName, variables);
    }

    /**
     * Dynamically triggers any named activity in a running process instance on demand —
     * the mechanism behind letting an external caller (e.g. a case management UI, or,
     * until that UI exists, a direct API call) decide at runtime which task to create
     * next, in any order, any number of times, independent of the process definition's
     * own default flow. Returns the resulting task(s), if any were created.
     * <p>
     * See {@code case-management-process.bpmn}: after starting a case (which
     * auto-creates a "SAM" task by default), call this repeatedly with activityId
     * "UserTask_BusinessConfirmation", "UserTask_LegalReview", "UserTask_BusinessApproval",
     * "UserTask_FinanceApproval", or "UserTask_Procurement" — in whatever order — to
     * create each on demand.
     *
     * POST /api/camunda/process-instances/{processInstanceId}/trigger-activity
     * { "activityId": "UserTask_LegalReview", "variables": {"note": "please expedite"} }
     */
    @PostMapping("/api/camunda/process-instances/{processInstanceId}/trigger-activity")
    public List<TaskInfo> triggerActivity(@PathVariable String processInstanceId,
                                           @RequestBody TriggerActivityRequest request) {
        return processInstanceService.triggerActivity(processInstanceId, request.getActivityId(), request.getVariables());
    }

    /**
     * Cancels all currently active instances of a named activity in a running process
     * instance, regardless of what's currently open inside it. Used e.g. to close a case
     * by cancelling its wrapping "case tasks" sub-process in one call, whatever tasks
     * (SAM, or any of the five on-demand tasks) happen to be open at the time — the
     * process instance then proceeds along that activity's own outgoing flow as normal.
     * <p>
     * See {@code case-management-process.bpmn}: to close a case,
     * activityId="SubProcess_CaseTasks".
     *
     * POST /api/camunda/process-instances/{processInstanceId}/cancel-activity
     * { "activityId": "SubProcess_CaseTasks" }
     */
    @PostMapping("/api/camunda/process-instances/{processInstanceId}/cancel-activity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelActivity(@PathVariable String processInstanceId, @RequestBody CancelActivityRequest request) {
        processInstanceService.cancelActivity(processInstanceId, request.getActivityId());
    }
}
