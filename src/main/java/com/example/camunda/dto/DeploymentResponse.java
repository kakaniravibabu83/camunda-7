package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentResponse {
    private String deploymentId;
    private String deploymentName;
    private LocalDateTime deploymentTime;
    private List<ProcessDefinitionInfo> deployedProcessDefinitions;
}
