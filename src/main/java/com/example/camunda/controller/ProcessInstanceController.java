package com.example.camunda.controller;

import com.example.camunda.dto.ActivityInstance;
import com.example.camunda.dto.CancelActivityRequest;
import com.example.camunda.dto.ProcessInstanceStatusResponse;
import com.example.camunda.dto.StartProcessRequest;
import com.example.camunda.dto.StartProcessResponse;
import com.example.camunda.dto.TaskInfo;
import com.example.camunda.dto.TriggerActivityRequest;
import com.example.camunda.service.ProcessInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.camunda.dto.ProcessInstanceModificationRequest;
import com.example.camunda.dto.ProcessInstanceModificationResponse;
import java.util.List;
import java.util.Map;

/**
 * Generic REST API to start a process instance for ANY deployed process definition,
 * with variables that are entirely optional.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Process Instances", description = "APIs for managing Camunda process instances")
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
    @Operation(summary = "Start a process instance", description = "Starts a new process instance for any deployed process definition with optional variables")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Process instance started successfully",
                    content = @Content(schema = @Schema(implementation = StartProcessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Process definition not found")
    })
    @PostMapping("/api/camunda/process-instances/start")
    @ResponseStatus(HttpStatus.CREATED)
    public StartProcessResponse start(@RequestBody StartProcessRequest request) {
        return processInstanceService.start(request);
    }

    @Operation(summary = "Get process instance variables", description = "Retrieves all variables for a given process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variables retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Process instance not found")
    })
    @GetMapping("/api/camunda/process-instances/{processInstanceId}/variables")
    public Map<String, Object> getVariables(@Parameter(description = "Process instance ID") @PathVariable String processInstanceId) {
        return processInstanceService.getVariables(processInstanceId);
    }

    /**
     * Add one or more new variables, or update the value of existing ones, on a running
     * process instance. Existing variables not included in the body are left untouched.
     * Only works while the process instance is still active � 409 if it has already
     * ended.
     *
     * POST /api/camunda/process-instances/{processInstanceId}/variables
     * { "amount": 300.00, "approved": true }
     */
    @Operation(summary = "Set process instance variables", description = "Adds or updates variables on a running process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variables updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Process instance not found"),
            @ApiResponse(responseCode = "409", description = "Process instance has already ended")
    })
    @PostMapping("/api/camunda/process-instances/{processInstanceId}/variables")
    public Map<String, Object> setVariables(@Parameter(description = "Process instance ID") @PathVariable String processInstanceId,
                                             @RequestBody Map<String, Object> variables) {
        return processInstanceService.setVariables(processInstanceId, variables);
    }

    @Operation(summary = "Get process instance status", description = "Retrieves the status of a process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Process instance not found")
    })
    @GetMapping("/api/camunda/process-instances/{processInstanceId}")
    public ProcessInstanceStatusResponse getInstance(@Parameter(description = "Process instance ID") @PathVariable String processInstanceId) {
        return processInstanceService.getStatus(processInstanceId);
    }

    /**
     * Correlates a named BPMN message to a running process instance � a generic
     * building block for processes that model branches as message-triggered Receive
     * Tasks / message event sub-processes. {@code variables} is the message's payload
     * and is entirely optional. 409 if the process instance isn't currently able to
     * receive this message (e.g. it has already ended, or isn't currently waiting for
     * it).
     *
     * POST /api/camunda/process-instances/{processInstanceId}/messages/{messageName}
     */
    @Operation(summary = "Correlate a message", description = "Correlates a named BPMN message to a running process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Message correlated successfully"),
            @ApiResponse(responseCode = "404", description = "Process instance not found"),
            @ApiResponse(responseCode = "409", description = "Process instance cannot receive this message")
    })
    @PostMapping("/api/camunda/process-instances/{processInstanceId}/messages/{messageName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void correlateMessage(@Parameter(description = "Process instance ID") @PathVariable String processInstanceId,
                                  @Parameter(description = "Message name") @PathVariable String messageName,
                                  @RequestBody(required = false) Map<String, Object> variables) {
        processInstanceService.correlateMessage(processInstanceId, messageName, variables);
    }

    /**
     * Dynamically triggers any named activity in a running process instance on demand �
     * the mechanism behind letting an external caller (e.g. a case management UI, or,
     * until that UI exists, a direct API call) decide at runtime which task to create
     * next, in any order, any number of times, independent of the process definition's
     * own default flow. Returns the resulting task(s), if any were created.
     * <p>
     * See {@code case-management-process.bpmn}: after starting a case (which
     * auto-creates a "SAM" task by default), call this repeatedly with activityId
     * "UserTask_BusinessConfirmation", "UserTask_LegalReview", "UserTask_BusinessApproval",
     * "UserTask_FinanceApproval", or "UserTask_Procurement" � in whatever order � to
     * create each on demand.
     *
     * POST /api/camunda/process-instances/{processInstanceId}/trigger-activity
     * { "activityId": "UserTask_LegalReview", "variables": {"note": "please expedite"} }
     */
    @Operation(summary = "Trigger an activity", description = "Dynamically triggers any named activity in a running process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity triggered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Process instance not found"),
            @ApiResponse(responseCode = "409", description = "Process instance has already ended")
    })
    @PostMapping("/api/camunda/process-instances/{processInstanceId}/trigger-activity")
    public List<TaskInfo> triggerActivity(@Parameter(description = "Process instance ID") @PathVariable String processInstanceId,
                                           @RequestBody TriggerActivityRequest request) {
        return processInstanceService.triggerActivity(processInstanceId, request.getActivityId(), request.getVariables());
    }

    /**
     * Cancels all currently active instances of a named activity in a running process
     * instance, regardless of what's currently open inside it. Used e.g. to close a case
     * by cancelling its wrapping "case tasks" sub-process in one call, whatever tasks
     * (SAM, or any of the five on-demand tasks) happen to be open at the time � the
     * process instance then proceeds along that activity's own outgoing flow as normal.
     * <p>
     * See {@code case-management-process.bpmn}: to close a case,
     * activityId="SubProcess_CaseTasks".
     *
     * POST /api/camunda/process-instances/{processInstanceId}/cancel-activity
     * { "activityId": "SubProcess_CaseTasks" }
     */
    @Operation(summary = "Cancel an activity", description = "Cancels all currently active instances of a named activity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Activity cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Process instance not found"),
            @ApiResponse(responseCode = "409", description = "Process instance has already ended")
    })
    @PostMapping("/api/camunda/process-instances/{processInstanceId}/cancel-activity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelActivity(@Parameter(description = "Process instance ID") @PathVariable String processInstanceId, @RequestBody CancelActivityRequest request) {
        processInstanceService.cancelActivity(processInstanceId, request.getActivityId());
    }

    /**
     * Modifies a running process instance by executing modification instructions.
     * Supports canceling activities, starting before/after activities, and starting transitions.
     *
     * POST /api/camunda/process-instances/{processInstanceId}/modification
     */
    @Operation(summary = "Modify a process instance", description = "Modifies a running process instance by executing modification instructions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Process instance modified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or modification failed"),
            @ApiResponse(responseCode = "404", description = "Process instance not found")
    })
    @PostMapping("/api/camunda/process-instances/{processInstanceId}/modification")
    @ResponseStatus(HttpStatus.OK)
    public ProcessInstanceModificationResponse modifyProcessInstance(
            @Parameter(description = "Process instance ID") @PathVariable String processInstanceId,
            @RequestBody ProcessInstanceModificationRequest request) {
        return processInstanceService.modifyProcessInstance(processInstanceId, request);
    }

    /**
     * Retrieves the activity instance tree for a given process instance.
     * Returns a hierarchical structure of all activity instances and transition instances.
     *
     * GET /api/camunda/process-instances/{processInstanceId}/activity-instances
     */
    @Operation(summary = "Get activity instances", description = "Retrieves the activity instance tree for a given process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity instances retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Process instance not found"),
            @ApiResponse(responseCode = "400", description = "Failed to retrieve activity instances")
    })
    @GetMapping("/api/camunda/process-instances/{processInstanceId}/activity-instances")
    public ActivityInstance getActivityInstances(@Parameter(description = "Process instance ID") @PathVariable String processInstanceId) {
        return processInstanceService.getActivityInstances(processInstanceId);
    }
}
