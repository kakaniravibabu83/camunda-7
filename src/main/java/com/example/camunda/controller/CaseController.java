package com.example.camunda.controller;

import com.example.camunda.dto.CaseRequest;
import com.example.camunda.dto.CaseResponse;
import com.example.camunda.dto.DeletionResponse;
import com.example.camunda.service.AppCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class CaseController {

    private final AppCaseService caseService;

    /** POST /api/cases - create a case and start the case-management workflow. */
    @PostMapping("/api/cases")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseResponse addCase(@Valid @RequestBody CaseRequest request) {
        return caseService.createCase(request);
    }

    /** PUT /api/cases/{id} - update case metadata. */
    @PutMapping("/api/cases/{id}")
    public CaseResponse updateCase(@PathVariable Long id, @Valid @RequestBody CaseRequest request) {
        return caseService.updateCase(id, request);
    }

    /** DELETE /api/cases/{id} - delete a case record. */
    @DeleteMapping("/api/cases/{id}")
    public DeletionResponse deleteCase(@PathVariable Long id) {
        return caseService.deleteCase(id);
    }

    /** GET /api/cases/{id} - get a single case. */
    @GetMapping("/api/cases/{id}")
    public CaseResponse getCase(@PathVariable Long id) {
        return caseService.getCase(id);
    }

    /** GET /api/cases - list all cases. */
    @GetMapping("/api/cases")
    public List<CaseResponse> getAllCases() {
        return caseService.findAll();
    }
}
