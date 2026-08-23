package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentInfo {
    private String id;
    private Date incidentTimestamp;
    private String incidentType;
    private String incidentMessage;
    private String executionId;
    private String activityId;
    private String failedActivityId;
    private String processInstanceId;
    private String processDefinitionId;
    private String causeIncidentId;
    private String rootCauseIncidentId;
    private String configuration;
    private String tenantId;
    private String jobDefinitionId;
    private String annotation;
}
