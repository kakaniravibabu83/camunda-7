package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Represents an activity instance in a process instance tree structure.
 * Contains nested child activity instances and transition instances.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityInstance {
    private String id;
    private String parentActivityInstanceId;
    private String activityId;
    private String activityType;
    private String processInstanceId;
    private String processDefinitionId;
    private List<ActivityInstance> childActivityInstances;
    private List<TransitionInstance> childTransitionInstances;
    private List<String> executionIds;
    private String activityName;
    private List<String> incidentIds;
    private List<Incident> incidents;
}
