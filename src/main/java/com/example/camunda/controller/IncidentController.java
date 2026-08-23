package com.example.camunda.controller;

import com.example.camunda.dto.CreateIncidentRequest;
import com.example.camunda.dto.IncidentInfo;
import com.example.camunda.dto.IncidentStacktraceResponse;
import com.example.camunda.dto.IncidentTypeCount;
import com.example.camunda.dto.JobInfo;
import com.example.camunda.dto.JobRetryRequest;
import com.example.camunda.service.IncidentManagementService;
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

/**
 * Generic REST API for Camunda incidents — covers both engine-raised incidents
 * ({@code failedJob}, from jobs that ran out of retries) and custom incidents reported
 * manually via {@link #createIncident}, for ANY deployed process definition.
 */
@RestController
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentManagementService incidentManagementService;

    /**
     * List/search incidents. All query params are optional and combine as AND filters.
     *
     * GET /api/camunda/incidents?processInstanceId=...&processDefinitionId=...&incidentType=...
     *     &executionId=...&activityId=...&failedActivityId=...&causeIncidentId=...
     *     &rootCauseIncidentId=...&configuration=...&jobDefinitionId=...&tenantId=...
     */
    @GetMapping("/api/camunda/incidents")
    public List<IncidentInfo> findIncidents(
            @RequestParam(required = false) String incidentId,
            @RequestParam(required = false) String incidentType,
            @RequestParam(required = false) String incidentMessage,
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false) String processDefinitionId,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String activityId,
            @RequestParam(required = false) String failedActivityId,
            @RequestParam(required = false) String causeIncidentId,
            @RequestParam(required = false) String rootCauseIncidentId,
            @RequestParam(required = false) String configuration,
            @RequestParam(required = false) String jobDefinitionId,
            @RequestParam(required = false) String tenantId) {
        return incidentManagementService.findIncidents(incidentId, incidentType, incidentMessage,
                processInstanceId, processDefinitionId, executionId, activityId, failedActivityId,
                causeIncidentId, rootCauseIncidentId, configuration, jobDefinitionId, tenantId);
    }

    /** GET /api/camunda/incidents/{incidentId} */
    @GetMapping("/api/camunda/incidents/{incidentId}")
    public IncidentInfo getIncident(@PathVariable String incidentId) {
        return incidentManagementService.getIncident(incidentId);
    }

    /**
     * Manually reports a custom incident against a running execution.
     * POST /api/camunda/incidents
     * { "incidentType": "dataQualityIssue", "executionId": "...", "message": "..." }
     */
    @PostMapping("/api/camunda/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentInfo createIncident(@RequestBody CreateIncidentRequest request) {
        return incidentManagementService.createIncident(
                request.getIncidentType(), request.getExecutionId(), request.getConfiguration(), request.getMessage());
    }

    /**
     * Resolves a custom incident. Not supported for engine-raised failedJob/failedExternalTask
     * incidents — use /retry for those.
     * POST /api/camunda/incidents/{incidentId}/resolve
     */
    @PostMapping("/api/camunda/incidents/{incidentId}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveIncident(@PathVariable String incidentId) {
        incidentManagementService.resolveIncident(incidentId);
    }

    /**
     * Gives the job behind a failedJob incident a fresh number of retries.
     * POST /api/camunda/incidents/{incidentId}/retry  { "retries": 1 }
     */
    @PostMapping("/api/camunda/incidents/{incidentId}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retryJob(@PathVariable String incidentId, @RequestBody JobRetryRequest request) {
        incidentManagementService.retryJob(incidentId, request.getRetries());
    }

    /** GET /api/camunda/incidents/{incidentId}/job — the failed job behind a failedJob incident. */
    @GetMapping("/api/camunda/incidents/{incidentId}/job")
    public JobInfo getJob(@PathVariable String incidentId) {
        return incidentManagementService.getJob(incidentId);
    }

    /** GET /api/camunda/incidents/{incidentId}/stacktrace — full exception stacktrace of the failed job. */
    @GetMapping("/api/camunda/incidents/{incidentId}/stacktrace")
    public IncidentStacktraceResponse getStacktrace(@PathVariable String incidentId) {
        IncidentManagementService.IncidentStacktraceResult result = incidentManagementService.getStacktrace(incidentId);
        return IncidentStacktraceResponse.builder()
                .incidentId(incidentId)
                .jobId(result.jobId())
                .stacktrace(result.stacktrace())
                .build();
    }

    /** GET /api/camunda/incidents/statistics — counts of all currently open incidents, grouped by type. */
    @GetMapping("/api/camunda/incidents/statistics")
    public List<IncidentTypeCount> getStatistics() {
        return incidentManagementService.getStatisticsByType();
    }
}
