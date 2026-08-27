package com.example.camunda.controller;

import com.example.camunda.dto.StartProcessRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof that {@code classificationRoutingProcess} — its DMN evaluation
 * (classification-decision.dmn) and its dynamic, calledElementExpression-based Call
 * Activity — works correctly for every classification branch. All four processes
 * involved (the router plus the three handlers) are auto-deployed at startup.
 * <p>
 * Exercised entirely through the app's existing generic "start process instance" REST
 * API, so no new test infrastructure is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClassificationRoutingProcessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private StartProcessRequest startRequest(Object requestAmount) {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessDefinitionKey("classificationRoutingProcess");
        if (requestAmount != null) {
            request.setVariables(Map.of("requestAmount", requestAmount));
        }
        return request;
    }

    @Test
    void lowAmount_isClassifiedStandard_andRoutedToStandardHandler() throws Exception {
        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(250.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ended").value(true))
                .andExpect(jsonPath("$.variables.classificationType").value("STANDARD"))
                .andExpect(jsonPath("$.variables.handledBy").value("standard-handler"));
    }

    @Test
    void midAmount_isClassifiedPriority_andRoutedToPriorityHandler() throws Exception {
        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(2500.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ended").value(true))
                .andExpect(jsonPath("$.variables.classificationType").value("PRIORITY"))
                .andExpect(jsonPath("$.variables.handledBy").value("priority-handler"));
    }

    @Test
    void highAmount_isClassifiedUrgent_andRoutedToUrgentHandler() throws Exception {
        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(50000.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ended").value(true))
                .andExpect(jsonPath("$.variables.classificationType").value("URGENT"))
                .andExpect(jsonPath("$.variables.handledBy").value("urgent-handler"));
    }

    @Test
    void amountExactlyAtPriorityThreshold_isClassifiedPriority() throws Exception {
        // Boundary check: rule is ">= 1000", so 1000 itself must match PRIORITY, not STANDARD.
        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(1000.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variables.classificationType").value("PRIORITY"))
                .andExpect(jsonPath("$.variables.handledBy").value("priority-handler"));
    }

    @Test
    void amountExactlyAtUrgentThreshold_isClassifiedUrgent() throws Exception {
        // Boundary check: rule is ">= 10000", so 10000 itself must match URGENT, not PRIORITY.
        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(10000.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variables.classificationType").value("URGENT"))
                .andExpect(jsonPath("$.variables.handledBy").value("urgent-handler"));
    }

    @Test
    void missingRequestAmount_fallsBackToStandard() throws Exception {
        // No numeric comparison matches a null input, so hitPolicy="FIRST" falls
        // through to the catch-all "-" rule.
        mockMvc.perform(post("/api/camunda/process-instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest(null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variables.classificationType").value("STANDARD"))
                .andExpect(jsonPath("$.variables.handledBy").value("standard-handler"));
    }
}
