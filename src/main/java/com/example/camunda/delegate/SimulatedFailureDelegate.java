package com.example.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * A demo/test-support delegate that always throws, used exclusively by
 * {@code processes/incident-demo-process.bpmn} to generate a real, reproducible
 * {@code failedJob} incident — both for manual exploration of the incident REST API
 * and for {@code IncidentControllerTest}.
 * <p>
 * Not used by any other process and has no effect unless a deployed process explicitly
 * references it via {@code delegateExpression="${simulatedFailureDelegate}"}.
 */
@Component("simulatedFailureDelegate")
public class SimulatedFailureDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        throw new RuntimeException("Simulated failure for incident demo/testing purposes.");
    }
}
