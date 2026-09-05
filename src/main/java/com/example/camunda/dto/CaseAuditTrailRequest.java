package com.example.camunda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CaseAuditTrailRequest {

    @NotNull(message = "is required")
    private Long caseId;

    @NotBlank(message = "is required")
    @Size(max = 100, message = "must be at most 100 characters")
    private String action;

    @Size(max = 50, message = "must be at most 50 characters")
    private String status;

    @Size(max = 2000, message = "must be at most 2000 characters")
    private String details;

    @Size(max = 100, message = "must be at most 100 characters")
    private String createdBy;
}
