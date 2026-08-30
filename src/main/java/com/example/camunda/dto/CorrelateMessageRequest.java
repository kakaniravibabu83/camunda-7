package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Body for triggering a named BPMN message against a specific, already-running process
 * instance — the generic mechanism this API exposes for "trigger a specific task/step
 * on demand", e.g. a case management UI deciding at runtime which of several possible
 * next steps (each modeled as a non-interrupting message event sub-process) to invoke,
 * in any order, any number of times, for as long as the process instance stays active.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorrelateMessageRequest {
    /** Required. Must match a message name the process instance is currently able to receive. */
    private String messageName;
    /** Optional payload passed to the resumed/started execution as process variables. */
    private Map<String, Object> variables;
}
