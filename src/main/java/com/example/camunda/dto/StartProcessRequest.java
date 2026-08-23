package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Generic request body to start ANY process instance.
 * <p>
 * Exactly one of {@link #processDefinitionKey} (starts the latest deployed version)
 * or {@link #processDefinitionId} (starts a specific version) must be supplied.
 * <p>
 * {@link #variables} is entirely optional — some processes need none, others need
 * many, so callers may omit it, send an empty object, or send any arbitrary set of
 * simple key/value pairs (String, Number, Boolean, List, Map, or null).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartProcessRequest {

    /** Process definition key, e.g. "sampleApprovalProcess". Starts the latest version. */
    private String processDefinitionKey;

    /** Specific process definition id/version, e.g. "sampleApprovalProcess:2:abcd1234". */
    private String processDefinitionId;

    /** Optional business key correlated with the new process instance. */
    private String businessKey;

    /** Optional process variables. May be null or empty. */
    private Map<String, Object> variables;
}
