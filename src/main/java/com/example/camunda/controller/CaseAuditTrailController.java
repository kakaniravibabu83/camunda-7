package com.example.camunda.controller;

import com.example.camunda.dto.CaseAuditTrailRequest;
import com.example.camunda.dto.CaseAuditTrailResponse;
import com.example.camunda.dto.CaseAuditTrailUpdateRequest;
import com.example.camunda.service.CaseAuditTrailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CaseAuditTrailController {

    private final CaseAuditTrailService auditTrailService;

    /** POST /api/case-audit-trails - add an audit entry for an existing case. */
    @PostMapping("/api/case-audit-trails")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseAuditTrailResponse addAuditTrail(@Valid @RequestBody CaseAuditTrailRequest request) {
        return auditTrailService.createAuditTrail(request);
    }

    /** PUT /api/case-audit-trails/{id} - update an audit entry. */
    @PutMapping("/api/case-audit-trails/{id}")
    public CaseAuditTrailResponse updateAuditTrail(@PathVariable Long id,
                                                   @Valid @RequestBody CaseAuditTrailUpdateRequest request) {
        return auditTrailService.updateAuditTrail(id, request);
    }

    /** GET /api/case-audit-trails - list all audit entries. */
    @GetMapping("/api/case-audit-trails")
    public List<CaseAuditTrailResponse> getAllAuditTrails() {
        return auditTrailService.findAll();
    }

    /** GET /api/case-audit-trails/{id} - get one audit entry. */
    @GetMapping("/api/case-audit-trails/{id}")
    public CaseAuditTrailResponse getAuditTrail(@PathVariable Long id) {
        return auditTrailService.getAuditTrail(id);
    }

    /** GET /api/cases/{caseId}/audit-trail - list the history for one case. */
    @GetMapping("/api/cases/{caseId}/audit-trail")
    public List<CaseAuditTrailResponse> getAuditTrailForCase(@PathVariable Long caseId) {
        return auditTrailService.findByCaseId(caseId);
    }
}
