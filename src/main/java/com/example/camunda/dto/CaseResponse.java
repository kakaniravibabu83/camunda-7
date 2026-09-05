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
public class CaseResponse {
    private Long id;
    private String caseNumber;
    private String title;
    private String description;
    private String status;
    private String camundaProcessInstanceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
