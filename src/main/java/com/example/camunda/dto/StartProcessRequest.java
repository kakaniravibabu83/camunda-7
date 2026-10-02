package com.example.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request body to start a process instance")
public class StartProcessRequest {

    /** Process definition key, e.g. "sampleApprovalProcess". Starts the latest version. */
    @Schema(description = "Process definition key to start the latest version", example = "sampleApprovalProcess")
    private String processDefinitionKey;

    /** Specific process definition id/version, e.g. "sampleApprovalProcess:2:abcd1234". */
    @Schema(description = "Specific process definition ID to start a specific version", example = "sampleApprovalProcess:2:abcd1234")
    private String processDefinitionId;

    /** Optional business key correlated with the new process instance. */
    @Schema(description = "Optional business key for the process instance", example = "ORDER-1001")
    private String businessKey;

    /** Optional process variables. May be null or empty. */
    @Schema(description = "Optional process variables as key-value pairs", example = "{\"amount\": 250.75, \"approved\": false, \"requester\": \"jane\"}")
    private Map<String, Object> variables;
}
