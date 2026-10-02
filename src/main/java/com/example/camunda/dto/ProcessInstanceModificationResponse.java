package com.example.camunda.dto;

import lombok.*;

/**
 * Response body for process instance modification operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessInstanceModificationResponse {
    private String processInstanceId;
    private String message;
}
