package com.example.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Demo/test-support delegate used by {@code main-error-demo-process.bpmn} to simulate
 * business logic that can fail with an arbitrary BPMN error code, proving that the
 * catch-all boundary event (and the generic error handler subprocess behind it) works
 * for ANY error code — not just one hardcoded type.
 * <p>
 * Reads three optional input variables:
 * <ul>
 *     <li>{@code simulateError} (Boolean, default {@code true}) — set to {@code false}
 *     to let this task complete normally instead of throwing.</li>
 *     <li>{@code simulateErrorCode} (String, default {@code "GENERIC_ERROR"}) — the
 *     BPMN error code to throw.</li>
 *     <li>{@code simulateErrorMessage} (String, default a generic message) — the error
 *     message to throw.</li>
 * </ul>
 */
@Component("businessErrorSimulatingDelegate")
@Slf4j
public class BusinessErrorSimulatingDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        Object simulateErrorVar = execution.getVariable("simulateError");
        boolean simulateError = !(simulateErrorVar instanceof Boolean) || (Boolean) simulateErrorVar;

        if (!simulateError) {
            return;
        }

        Object errorCodeVar = execution.getVariable("simulateErrorCode");
        String errorCode = errorCodeVar != null ? errorCodeVar.toString() : "GENERIC_ERROR";

        log.warn("Handling BusinessErrorSimulatingDelegate with  errorCode={}",
                errorCode);

        Object errorMessageVar = execution.getVariable("simulateErrorMessage");
        String errorMessage = errorMessageVar != null
                ? errorMessageVar.toString()
                : "Simulated business error for demonstration purposes.";

        throw new BpmnError(errorCode, errorMessage);
    }
}
