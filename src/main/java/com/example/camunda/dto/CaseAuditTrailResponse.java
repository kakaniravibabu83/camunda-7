package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAuditTrailResponse {
    private Long id;
    private Long caseId;
    private String caseNumber;
    private String action;
    private String status;
    private String details;
    private String camundaProcessInstanceId;
    private String createdBy;
    private LocalDateTime createdAt;
}
