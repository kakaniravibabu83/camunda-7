package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Represents a transition instance in an activity instance tree.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransitionInstance {
    private String id;
    private String parentActivityInstanceId;
    private String activityId;
    private String activityType;
    private String processInstanceId;
    private String processDefinitionId;
    private String executionId;
    private String activityName;
    private List<String> incidentIds;
    private List<Incident> incidents;
}
