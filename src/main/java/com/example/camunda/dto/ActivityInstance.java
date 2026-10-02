package com.example.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Represents an activity instance in a process instance tree")
public class ActivityInstance {
    @Schema(description = "Activity instance ID", example = "UserTask_Review:abc123")
    private String id;

    @Schema(description = "Parent activity instance ID", example = "SubProcess_1:def456")
    private String parentActivityInstanceId;

    @Schema(description = "Activity ID from BPMN definition", example = "UserTask_Review")
    private String activityId;

    @Schema(description = "Activity type", example = "userTask")
    private String activityType;

    @Schema(description = "Process instance ID", example = "abc123-def456-ghi789")
    private String processInstanceId;

    @Schema(description = "Process definition ID", example = "sampleApprovalProcess:1:abc123")
    private String processDefinitionId;

    @Schema(description = "Nested child activity instances")
    private List<ActivityInstance> childActivityInstances;

    @Schema(description = "Nested child transition instances")
    private List<TransitionInstance> childTransitionInstances;

    @Schema(description = "Execution IDs")
    private List<String> executionIds;

    @Schema(description = "Activity name", example = "Review Approval")
    private String activityName;

    @Schema(description = "Incident IDs")
    private List<String> incidentIds;

    @Schema(description = "Incidents associated with this activity")
    private List<Incident> incidents;
}
