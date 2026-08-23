package com.example.camunda.service;

import com.example.camunda.dto.IncidentInfo;
import com.example.camunda.dto.IncidentTypeCount;
import com.example.camunda.dto.JobInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.IncidentQuery;
import org.camunda.bpm.engine.runtime.Job;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic operations on Camunda incidents: search, fetch, manually create/resolve
 * custom incidents, and retry/inspect the failed job behind an engine-raised
 * {@code failedJob} incident — independent of which process definition it belongs to.
 * <p>
 * Camunda distinguishes two families of incidents:
 * <ul>
 *     <li><b>Engine-raised</b> ({@code failedJob}, {@code failedExternalTask}) — created
 *     automatically when a job/external task runs out of retries. These can only be
 *     cleared by giving the underlying job/task more retries (see {@link #retryJob}),
 *     <i>not</i> via {@link org.camunda.bpm.engine.RuntimeService#resolveIncident}.</li>
 *     <li><b>Custom</b> (any other type) — created explicitly via
 *     {@link RuntimeService#createIncident}, e.g. to surface a failure detected by an
 *     external system. These are cleared via {@link #resolveIncident}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentManagementService {

    public static final String INCIDENT_TYPE_FAILED_JOB = "failedJob";
    public static final String INCIDENT_TYPE_FAILED_EXTERNAL_TASK = "failedExternalTask";

    private final RuntimeService runtimeService;
    private final ManagementService managementService;

    public List<IncidentInfo> findIncidents(String incidentId, String incidentType, String incidentMessage,
                                             String processInstanceId, String processDefinitionId,
                                             String executionId, String activityId, String failedActivityId,
                                             String causeIncidentId, String rootCauseIncidentId,
                                             String configuration, String jobDefinitionId, String tenantId) {

        IncidentQuery query = runtimeService.createIncidentQuery();

        if (StringUtils.hasText(incidentId)) {
            query.incidentId(incidentId);
        }
        if (StringUtils.hasText(incidentType)) {
            query.incidentType(incidentType);
        }
        if (StringUtils.hasText(incidentMessage)) {
            query.incidentMessage(incidentMessage);
        }
        if (StringUtils.hasText(processInstanceId)) {
            query.processInstanceId(processInstanceId);
        }
        if (StringUtils.hasText(processDefinitionId)) {
            query.processDefinitionId(processDefinitionId);
        }
        if (StringUtils.hasText(executionId)) {
            query.executionId(executionId);
        }
        if (StringUtils.hasText(activityId)) {
            query.activityId(activityId);
        }
        if (StringUtils.hasText(failedActivityId)) {
            query.failedActivityId(failedActivityId);
        }
        if (StringUtils.hasText(causeIncidentId)) {
            query.causeIncidentId(causeIncidentId);
        }
        if (StringUtils.hasText(rootCauseIncidentId)) {
            query.rootCauseIncidentId(rootCauseIncidentId);
        }
        if (StringUtils.hasText(configuration)) {
            query.configuration(configuration);
        }
        if (StringUtils.hasText(jobDefinitionId)) {
            query.jobDefinitionIdIn(jobDefinitionId);
        }
        if (StringUtils.hasText(tenantId)) {
            query.tenantIdIn(tenantId);
        }

        return query.orderByIncidentTimestamp().desc().list().stream()
                .map(this::toIncidentInfo)
                .collect(Collectors.toList());
    }

    public IncidentInfo getIncident(String incidentId) {
        return toIncidentInfo(requireIncident(incidentId));
    }

    /** Manually reports a custom incident against a running execution. */
    public IncidentInfo createIncident(String incidentType, String executionId, String configuration, String message) {
        if (!StringUtils.hasText(incidentType)) {
            throw new IllegalArgumentException("'incidentType' is required.");
        }
        if (!StringUtils.hasText(executionId)) {
            throw new IllegalArgumentException("'executionId' is required.");
        }
        Incident incident = StringUtils.hasText(message)
                ? runtimeService.createIncident(incidentType, executionId, configuration, message)
                : runtimeService.createIncident(incidentType, executionId, configuration);
        log.info("Created custom incident {} of type '{}' on execution {}", incident.getId(), incidentType, executionId);
        return toIncidentInfo(incident);
    }

    /**
     * Resolves (removes) a custom incident. NOT supported for engine-raised
     * {@code failedJob}/{@code failedExternalTask} incidents — use {@link #retryJob}
     * for those instead.
     */
    public void resolveIncident(String incidentId) {
        Incident incident = requireIncident(incidentId);
        if (isEngineManaged(incident.getIncidentType())) {
            throw new IllegalArgumentException(
                    "Incident '" + incidentId + "' is of type '" + incident.getIncidentType() + "', which cannot be "
                            + "resolved directly. Use the /retry endpoint to give the underlying job more retries instead.");
        }
        runtimeService.resolveIncident(incidentId);
        log.info("Resolved custom incident {}", incidentId);
    }

    /**
     * Gives the job behind a {@code failedJob} incident a fresh number of retries,
     * clearing the incident once the job next executes successfully (or immediately,
     * for retries {@code > 0}, per Camunda's ManagementService#setJobRetries contract).
     */
    public void retryJob(String incidentId, Integer retries) {
        if (retries == null || retries < 0) {
            throw new IllegalArgumentException("'retries' is required and must be a non-negative integer.");
        }
        Incident incident = requireIncident(incidentId);
        requireFailedJobType(incident);
        String jobId = incident.getConfiguration();
        managementService.setJobRetries(jobId, retries);
        log.info("Set retries={} on job {} behind incident {}", retries, jobId, incidentId);
    }

    /** Fetches the job behind a {@code failedJob} incident. */
    public JobInfo getJob(String incidentId) {
        Incident incident = requireIncident(incidentId);
        requireFailedJobType(incident);
        return toJobInfo(requireJob(incident.getConfiguration()));
    }

    /** Fetches the full exception stacktrace of the job behind a {@code failedJob} incident. */
    public IncidentStacktraceResult getStacktrace(String incidentId) {
        Incident incident = requireIncident(incidentId);
        requireFailedJobType(incident);
        String jobId = incident.getConfiguration();
        requireJob(jobId);
        String stacktrace = managementService.getJobExceptionStacktrace(jobId);
        return new IncidentStacktraceResult(jobId, stacktrace);
    }

    /** Counts all currently open incidents grouped by incident type. */
    public List<IncidentTypeCount> getStatisticsByType() {
        Map<String, Long> counts = runtimeService.createIncidentQuery().list().stream()
                .collect(Collectors.groupingBy(Incident::getIncidentType, Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> IncidentTypeCount.builder().incidentType(e.getKey()).count(e.getValue()).build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    private boolean isEngineManaged(String incidentType) {
        return INCIDENT_TYPE_FAILED_JOB.equals(incidentType) || INCIDENT_TYPE_FAILED_EXTERNAL_TASK.equals(incidentType);
    }

    private void requireFailedJobType(Incident incident) {
        if (!INCIDENT_TYPE_FAILED_JOB.equals(incident.getIncidentType())) {
            throw new IllegalArgumentException(
                    "This operation is only supported for incidents of type '" + INCIDENT_TYPE_FAILED_JOB
                            + "'; incident '" + incident.getId() + "' is of type '" + incident.getIncidentType() + "'.");
        }
    }

    private Incident requireIncident(String incidentId) {
        Incident incident = runtimeService.createIncidentQuery().incidentId(incidentId).singleResult();
        if (incident == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No incident found with id '" + incidentId + "'.");
        }
        return incident;
    }

    private Job requireJob(String jobId) {
        Job job = managementService.createJobQuery().jobId(jobId).singleResult();
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No job found with id '" + jobId + "' (it may have already been removed).");
        }
        return job;
    }

    private IncidentInfo toIncidentInfo(Incident incident) {
        return IncidentInfo.builder()
                .id(incident.getId())
                .incidentTimestamp(incident.getIncidentTimestamp())
                .incidentType(incident.getIncidentType())
                .incidentMessage(incident.getIncidentMessage())
                .executionId(incident.getExecutionId())
                .activityId(incident.getActivityId())
                .failedActivityId(incident.getFailedActivityId())
                .processInstanceId(incident.getProcessInstanceId())
                .processDefinitionId(incident.getProcessDefinitionId())
                .causeIncidentId(incident.getCauseIncidentId())
                .rootCauseIncidentId(incident.getRootCauseIncidentId())
                .configuration(incident.getConfiguration())
                .tenantId(incident.getTenantId())
                .jobDefinitionId(incident.getJobDefinitionId())
                .annotation(incident.getAnnotation())
                .build();
    }

    private JobInfo toJobInfo(Job job) {
        return JobInfo.builder()
                .id(job.getId())
                .jobDefinitionId(job.getJobDefinitionId())
                .processInstanceId(job.getProcessInstanceId())
                .processDefinitionId(job.getProcessDefinitionId())
                .executionId(job.getExecutionId())
                .retries(job.getRetries())
                .exceptionMessage(job.getExceptionMessage())
                .dueDate(job.getDuedate())
                .priority((int) job.getPriority())
                .suspended(job.isSuspended())
                .build();
    }

    /** Small internal holder so the controller can build the response DTO with the incident id included. */
    public record IncidentStacktraceResult(String jobId, String stacktrace) {
    }
}
