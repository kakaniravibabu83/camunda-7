package com.example.camunda.service;

import com.example.camunda.dto.CaseRequest;
import com.example.camunda.dto.CaseResponse;
import com.example.camunda.dto.DeletionResponse;
import com.example.camunda.dto.StartProcessRequest;
import com.example.camunda.dto.StartProcessResponse;
import com.example.camunda.entity.CaseRecord;
import com.example.camunda.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppCaseService {

    private static final String CASE_MANAGEMENT_PROCESS_KEY = "caseManagementProcess";
    private static final DateTimeFormatter CASE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMddyyyy");
    private static final int MAX_CASE_NUMBER_ATTEMPTS = 20;

    private final CaseRepository caseRepository;
    private final ProcessInstanceService processInstanceService;
    private final CaseAuditTrailService auditTrailService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public CaseResponse createCase(CaseRequest request) {
        CaseRecord saved = saveWithUniqueCaseNumber(request);

        Map<String, Object> variables = new HashMap<>();
        variables.put("caseNumber", saved.getCaseNumber());

        StartProcessRequest startProcessRequest = new StartProcessRequest();
        startProcessRequest.setProcessDefinitionKey(CASE_MANAGEMENT_PROCESS_KEY);
        startProcessRequest.setBusinessKey(saved.getCaseNumber());
        startProcessRequest.setVariables(variables);

        StartProcessResponse process = processInstanceService.start(startProcessRequest);
        saved.setCamundaProcessInstanceId(process.getProcessInstanceId());

        CaseRecord linked = caseRepository.save(saved);
        auditTrailService.recordCaseEvent(linked, "CASE_CREATED",
                "Case created and case-management workflow started.");
        log.info("Created case {} and linked Camunda process instance {}",
                linked.getCaseNumber(), linked.getCamundaProcessInstanceId());
        return toResponse(linked);
    }

    public List<CaseResponse> findAll() {
        return caseRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CaseResponse getCase(Long id) {
        return toResponse(requireCase(id));
    }

    @Transactional
    public CaseResponse updateCase(Long id, CaseRequest request) {
        CaseRecord caseRecord = requireCase(id);
        String previousStatus = caseRecord.getStatus();
        caseRecord.setTitle(request.getTitle());
        caseRecord.setDescription(request.getDescription());
        caseRecord.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : caseRecord.getStatus());

        CaseRecord saved = caseRepository.save(caseRecord);
        auditTrailService.recordCaseEvent(saved, auditAction(previousStatus, saved.getStatus()),
                "Case updated. Status changed from '" + previousStatus + "' to '" + saved.getStatus() + "'.");
        log.info("Updated case {} ({})", saved.getId(), saved.getCaseNumber());
        return toResponse(saved);
    }

    @Transactional
    public DeletionResponse deleteCase(Long id) {
        CaseRecord caseRecord = requireCase(id);
        String caseNumber = caseRecord.getCaseNumber();
        auditTrailService.recordCaseEvent(caseRecord, "CASE_DELETED", "Case record deleted.");
        caseRepository.delete(caseRecord);

        log.info("Deleted case {} ({})", id, caseNumber);
        return DeletionResponse.builder()
                .id(id)
                .deleted(true)
                .message("Case " + id + " (" + caseNumber + ") deleted successfully.")
                .build();
    }

    private CaseRecord saveWithUniqueCaseNumber(CaseRequest request) {
        for (int attempt = 0; attempt < MAX_CASE_NUMBER_ATTEMPTS; attempt++) {
            String caseNumber = generateCaseNumber();
            if (caseRepository.existsByCaseNumber(caseNumber)) {
                continue;
            }

            CaseRecord caseRecord = CaseRecord.builder()
                    .caseNumber(caseNumber)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .status(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "OPEN")
                    .build();

            try {
                return caseRepository.saveAndFlush(caseRecord);
            } catch (DataIntegrityViolationException ex) {
                log.debug("Generated duplicate case number {}; retrying", caseNumber);
            }
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Unable to generate a unique case number after " + MAX_CASE_NUMBER_ATTEMPTS + " attempts.");
    }

    private String generateCaseNumber() {
        int number = 10000 + random.nextInt(90000);
        return number + "-" + LocalDate.now().format(CASE_DATE_FORMAT);
    }

    private String auditAction(String previousStatus, String currentStatus) {
        if (!StringUtils.hasText(currentStatus) || currentStatus.equalsIgnoreCase(previousStatus)) {
            return "CASE_UPDATED";
        }
        if ("CLOSED".equalsIgnoreCase(currentStatus) || "CLOSE".equalsIgnoreCase(currentStatus)) {
            return "CASE_CLOSED";
        }
        if ("COMPLETED".equalsIgnoreCase(currentStatus) || "COMPLETE".equalsIgnoreCase(currentStatus)) {
            return "CASE_COMPLETED";
        }
        return "CASE_UPDATED";
    }

    private CaseRecord requireCase(Long id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No case found with id " + id + "."));
    }

    private CaseResponse toResponse(CaseRecord caseRecord) {
        return CaseResponse.builder()
                .id(caseRecord.getId())
                .caseNumber(caseRecord.getCaseNumber())
                .title(caseRecord.getTitle())
                .description(caseRecord.getDescription())
                .status(caseRecord.getStatus())
                .camundaProcessInstanceId(caseRecord.getCamundaProcessInstanceId())
                .createdAt(caseRecord.getCreatedAt())
                .updatedAt(caseRecord.getUpdatedAt())
                .build();
    }
}
