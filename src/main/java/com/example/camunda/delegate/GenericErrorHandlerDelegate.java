package com.example.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Generic handling logic for {@code genericErrorHandlerProcess}
 * (generic-error-handler-subprocess.bpmn). Works uniformly for ANY BPMN error code —
 * it doesn't branch on the error type, it just records and logs whatever it was given.
 * <p>
 * This is intentionally simple (log + record). In a real system this is the natural
 * extension point for cross-cutting handling such as writing to an incident-tracking
 * system, sending an alert, or applying error-code-specific routing — without touching
 * any of the main processes that call this subprocess.
 */
@Component("genericErrorHandlerDelegate")
@Slf4j
public class GenericErrorHandlerDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String errorCode = stringVar(execution, "errorCode");
        String errorMessage = stringVar(execution, "errorMessage");
        String sourceProcessInstanceId = stringVar(execution, "sourceProcessInstanceId");
        String sourceProcessDefinitionId = stringVar(execution, "sourceProcessDefinitionId");

        log.warn("Handling BPMN error [code={}, message={}] from processInstanceId={}, processDefinitionId={}",
                errorCode, errorMessage, sourceProcessInstanceId, sourceProcessDefinitionId);

        execution.setVariable("errorHandled", true);
        execution.setVariable("errorHandledAt", Instant.now().toString());
    }

    private String stringVar(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value != null ? value.toString() : null;
    }
}
