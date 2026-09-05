package com.example.camunda.service;

import com.example.camunda.dto.CaseAuditTrailRequest;
import com.example.camunda.dto.CaseAuditTrailResponse;
import com.example.camunda.dto.CaseAuditTrailUpdateRequest;
import com.example.camunda.entity.CaseAuditTrail;
import com.example.camunda.entity.CaseRecord;
import com.example.camunda.repository.CaseAuditTrailRepository;
import com.example.camunda.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseAuditTrailService {

    private final CaseAuditTrailRepository auditTrailRepository;
    private final CaseRepository caseRepository;

    @Transactional
    public CaseAuditTrailResponse createAuditTrail(CaseAuditTrailRequest request) {
        CaseRecord caseRecord = requireCase(request.getCaseId());
        CaseAuditTrail auditTrail = CaseAuditTrail.builder()
                .caseId(caseRecord.getId())
                .caseNumber(caseRecord.getCaseNumber())
                .action(request.getAction())
                .status(StringUtils.hasText(request.getStatus()) ? request.getStatus() : caseRecord.getStatus())
                .details(request.getDetails())
                .camundaProcessInstanceId(caseRecord.getCamundaProcessInstanceId())
                .createdBy(StringUtils.hasText(request.getCreatedBy()) ? request.getCreatedBy() : "system")
                .build();

        CaseAuditTrail saved = auditTrailRepository.save(auditTrail);
        log.info("Created audit trail entry {} for case {}", saved.getId(), saved.getCaseNumber());
        return toResponse(saved);
    }

    public List<CaseAuditTrailResponse> findAll() {
        return auditTrailRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CaseAuditTrailResponse> findByCaseId(Long caseId) {
        return auditTrailRepository.findByCaseIdOrderByCreatedAtAscIdAsc(caseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CaseAuditTrailResponse getAuditTrail(Long id) {
        return toResponse(requireAuditTrail(id));
    }

    @Transactional
    public CaseAuditTrailResponse updateAuditTrail(Long id, CaseAuditTrailUpdateRequest request) {
        CaseAuditTrail auditTrail = requireAuditTrail(id);
        auditTrail.setAction(request.getAction());
        auditTrail.setStatus(request.getStatus());
        auditTrail.setDetails(request.getDetails());
        auditTrail.setCreatedBy(request.getCreatedBy());

        CaseAuditTrail saved = auditTrailRepository.save(auditTrail);
        log.info("Updated audit trail entry {} for case {}", saved.getId(), saved.getCaseNumber());
        return toResponse(saved);
    }

    @Transactional
    public void recordCaseEvent(CaseRecord caseRecord, String action, String details) {
        recordCaseEvent(caseRecord, action, details, "system");
    }

    @Transactional
    public void recordCaseEvent(CaseRecord caseRecord, String action, String details, String createdBy) {
        CaseAuditTrail auditTrail = CaseAuditTrail.builder()
                .caseId(caseRecord.getId())
                .caseNumber(caseRecord.getCaseNumber())
                .action(action)
                .status(caseRecord.getStatus())
                .details(details)
                .camundaProcessInstanceId(caseRecord.getCamundaProcessInstanceId())
                .createdBy(createdBy)
                .build();
        auditTrailRepository.save(auditTrail);
    }

    private CaseRecord requireCase(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No case found with id " + caseId + "."));
    }

    private CaseAuditTrail requireAuditTrail(Long id) {
        return auditTrailRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No case audit trail entry found with id " + id + "."));
    }

    private CaseAuditTrailResponse toResponse(CaseAuditTrail auditTrail) {
        return CaseAuditTrailResponse.builder()
                .id(auditTrail.getId())
                .caseId(auditTrail.getCaseId())
                .caseNumber(auditTrail.getCaseNumber())
                .action(auditTrail.getAction())
                .status(auditTrail.getStatus())
                .details(auditTrail.getDetails())
                .camundaProcessInstanceId(auditTrail.getCamundaProcessInstanceId())
                .createdBy(auditTrail.getCreatedBy())
                .createdAt(auditTrail.getCreatedAt())
                .build();
    }
}
