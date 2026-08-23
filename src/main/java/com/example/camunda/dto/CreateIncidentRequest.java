package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body for manually reporting a custom incident against a running execution — useful
 * for surfacing failures detected by external systems (a downstream API outage, a
 * validation failure, etc.) as first-class Camunda incidents.
 * <p>
 * {@link #configuration} and {@link #message} are optional; {@link #incidentType} and
 * {@link #executionId} are required.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncidentRequest {
    /** Free-text incident type, e.g. "custom", "dataQualityIssue". Avoid the reserved
     *  types "failedJob"/"failedExternalTask" — those are managed by the engine itself. */
    private String incidentType;
    /** Id of the execution the incident is attached to (e.g. a running process instance id). */
    private String executionId;
    /** Optional free-text payload describing the incident. */
    private String configuration;
    /** Optional human-readable message. */
    private String message;
}
