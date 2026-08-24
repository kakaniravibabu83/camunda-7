package com.example.camunda.controller;

import com.example.camunda.dto.StartProcessRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof that {@code mainErrorDemoProcess} and the reusable
 * {@code genericErrorHandlerProcess} it calls (both auto-deployed at startup) work
 * together correctly — exercised entirely through the app's own existing generic
 * "start process instance" REST API, for several unrelated error codes, to demonstrate
 * that the catch-all boundary event handles ANY BpmnError rather than one hardcoded type.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MainErrorDemoProcessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RuntimeService runtimeService;

    private StartProcessRequest startRequest(Map<String, Object> variables) {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessDefinitionKey("mainErrorDemoProcess");
        request.setVariables(variables);
        return request;
    }

    /**
     * DIAGNOSTIC (temporary): pinpoints exactly which activity mainErrorDemoProcess is
     * parked at when it doesn't complete synchronously as expected, and whether the
     * called genericErrorHandlerProcess sub-instance was even created. Run with
     * {@code mvn test -Dtest=MainErrorDemoProcessTest#diagnostic_whereIsTheProcessStuck}
     * and read the console output (not just pass/fail) — this test always passes, it
     * only prints information.
     */
    @Test
    void diagnostic_whereIsTheProcessStuck() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("mainErrorDemoProcess");
        System.out.println("=== DIAGNOSTIC: mainErrorDemoProcess instance " + instance.getId() + " ===");
        System.out.println("isEnded() on returned object: " + instance.isEnded());

        boolean stillInRuntime = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getId()).count() > 0;
        System.out.println("Still present in runtime tables: " + stillInRuntime);

        if (stillInRuntime) {
            List<String> activeActivityIds = runtimeService.getActiveActivityIds(instance.getId());
            System.out.println("Active activity ids on main instance: " + activeActivityIds);

            List<Execution> executions = runtimeService.createExecutionQuery()
                    .processInstanceId(instance.getId()).list();
            System.out.println("Execution count: " + executions.size());
            for (Execution e : executions) {
                System.out.println("  execution id=" + e.getId() + " isEnded=" + e.isEnded());
            }

            List<ProcessInstance> subInstances = runtimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(instance.getId()).list();
            System.out.println("Called sub-process instances found: " + subInstances.size());
            for (ProcessInstance sub : subInstances) {
                System.out.println("  sub-instance id=" + sub.getId()
                        + " processDefinitionId=" + sub.getProcessDefinitionId()
                        + " isEnded=" + sub.isEnded());
                boolean subStillInRuntime = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(sub.getId()).count() > 0;
                if (subStillInRuntime) {
                    System.out.println("  sub-instance active activity ids: "
                            + runtimeService.getActiveActivityIds(sub.getId()));
                }
            }

            Map<String, Object> vars = runtimeService.getVariables(instance.getId());
            System.out.println("Current variables on main instance: " + vars);
        }
        System.out.println("=== END DIAGNOSTIC ===");
    }

    @Test
    void defaultRun_takesErrorPath_andGenericHandlerRuns() throws Exception {
        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ended").value(true))
                .andExpect(jsonPath("$.variables.errorCode").value("GENERIC_ERROR"))
                .andExpect(jsonPath("$.variables.errorHandled").value(true))
                .andExpect(jsonPath("$.variables.errorHandledAt").isNotEmpty());
    }

    @Test
    void customErrorCode_paymentDeclined_isCaughtAndHandledGenerically() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("simulateErrorCode", "PAYMENT_DECLINED");
        variables.put("simulateErrorMessage", "Card issuer declined the transaction");

        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(variables))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ended").value(true))
                .andExpect(jsonPath("$.variables.errorCode").value("PAYMENT_DECLINED"))
                .andExpect(jsonPath("$.variables.errorMessage").value("Card issuer declined the transaction"))
                .andExpect(jsonPath("$.variables.errorHandled").value(true));
    }

    @Test
    void customErrorCode_inventoryShortage_isAlsoCaughtByTheSameGenericBoundaryEvent() throws Exception {
        // A second, completely unrelated error code proves the boundary event (no
        // errorRef) and the shared handler subprocess are generic, not hardcoded to
        // one specific error type.
        Map<String, Object> variables = new HashMap<>();
        variables.put("simulateErrorCode", "INVENTORY_SHORTAGE");
        variables.put("simulateErrorMessage", "Requested quantity exceeds available stock");

        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(variables))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ended").value(true))
                .andExpect(jsonPath("$.variables.errorCode").value("INVENTORY_SHORTAGE"))
                .andExpect(jsonPath("$.variables.errorHandled").value(true));
    }

    @Test
    void simulateErrorFalse_takesSuccessPath_handlerNeverRuns() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("simulateError", false);

        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(variables))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ended").value(true))
                .andExpect(jsonPath("$.variables.errorHandled").doesNotExist())
                .andExpect(jsonPath("$.variables.errorCode").doesNotExist());
    }
}
