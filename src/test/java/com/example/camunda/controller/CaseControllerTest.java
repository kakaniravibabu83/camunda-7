package com.example.camunda.controller;

import com.example.camunda.dto.CaseAuditTrailRequest;
import com.example.camunda.dto.CaseAuditTrailUpdateRequest;
import com.example.camunda.dto.CaseRequest;
import com.example.camunda.repository.CaseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CaseControllerTest {

    private static final DateTimeFormatter CASE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMddyyyy");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private CaseRepository caseRepository;

    @Test
    void addCase_createsCaseStartsWorkflowAndLinksProcessInstance() throws Exception {
        CaseRequest request = new CaseRequest("New case", "Needs review", null);

        MvcResult result = mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("New case"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.camundaProcessInstanceId").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String caseNumber = body.get("caseNumber").asText();
        String processInstanceId = body.get("camundaProcessInstanceId").asText();

        assertThat(caseNumber).matches("\\d{5}-" + LocalDate.now().format(CASE_DATE_FORMAT));
        assertThat(caseRepository.existsByCaseNumber(caseNumber)).isTrue();

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getBusinessKey()).isEqualTo(caseNumber);
        assertThat(runtimeService.getVariable(processInstanceId, "caseNumber")).isEqualTo(caseNumber);
    }

    @Test
    void caseCrudEndpointsCreateReadUpdateListAndDelete() throws Exception {
        Long caseId = createCase("Before update");

        mockMvc.perform(get("/api/cases/{id}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.title").value("Before update"));

        CaseRequest update = new CaseRequest("After update", "Updated description", "IN_PROGRESS");
        mockMvc.perform(put("/api/cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("After update"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + caseId + ")]").exists());

        mockMvc.perform(delete("/api/cases/{id}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.deleted").value(true));

        mockMvc.perform(get("/api/cases/{id}", caseId))
                .andExpect(status().isNotFound());
    }

    @Test
    void caseAuditTrailTracksCaseLifecycle() throws Exception {
        Long caseId = createCase("Audit lifecycle");

        mockMvc.perform(get("/api/cases/{caseId}/audit-trail", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("CASE_CREATED"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].camundaProcessInstanceId").isNotEmpty());

        CaseRequest closeRequest = new CaseRequest("Audit lifecycle", "Closed after review", "Closed");
        mockMvc.perform(put("/api/cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Closed"));

        mockMvc.perform(get("/api/cases/{caseId}/audit-trail", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("CASE_CREATED"))
                .andExpect(jsonPath("$[1].action").value("CASE_CLOSED"))
                .andExpect(jsonPath("$[1].status").value("Closed"));
    }

    @Test
    void caseAuditTrailCrudEndpointsCreateReadUpdateAndList() throws Exception {
        Long caseId = createCase("Manual audit");
        CaseAuditTrailRequest request = new CaseAuditTrailRequest(
                caseId, "NOTE_ADDED", null, "Requester attached renewal details.", "ravi");

        MvcResult result = mockMvc.perform(post("/api/case-audit-trails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").value(caseId))
                .andExpect(jsonPath("$.action").value("NOTE_ADDED"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdBy").value("ravi"))
                .andReturn();

        Long auditId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/case-audit-trails/{id}", auditId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auditId))
                .andExpect(jsonPath("$.details").value("Requester attached renewal details."));

        CaseAuditTrailUpdateRequest update = new CaseAuditTrailUpdateRequest(
                "NOTE_UPDATED", "OPEN", "Requester attached corrected renewal details.", "ravi");
        mockMvc.perform(put("/api/case-audit-trails/{id}", auditId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("NOTE_UPDATED"))
                .andExpect(jsonPath("$.details").value("Requester attached corrected renewal details."));

        mockMvc.perform(get("/api/case-audit-trails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + auditId + ")]").exists());
    }

    private Long createCase(String title) throws Exception {
        CaseRequest request = new CaseRequest(title, "Description", null);
        MvcResult result = mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
