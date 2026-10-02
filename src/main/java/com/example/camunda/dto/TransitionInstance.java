package com.example.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Represents a transition instance in an activity instance tree")
public class TransitionInstance {
    @Schema(description = "Transition instance ID", example = "SequenceFlow_1:abc123")
    private String id;

    @Schema(description = "Parent activity instance ID", example = "SubProcess_1:def456")
    private String parentActivityInstanceId;

    @Schema(description = "Activity ID", example = "UserTask_Review")
    private String activityId;

    @Schema(description = "Activity type", example = "sequenceFlow")
    private String activityType;

    @Schema(description = "Process instance ID", example = "abc123-def456-ghi789")
    private String processInstanceId;

    @Schema(description = "Process definition ID", example = "sampleApprovalProcess:1:abc123")
    private String processDefinitionId;

    @Schema(description = "Execution ID", example = "exec123")
    private String executionId;

    @Schema(description = "Activity name", example = "Review Approval")
    private String activityName;

    @Schema(description = "Incident IDs")
    private List<String> incidentIds;

    @Schema(description = "Incidents associated with this transition")
    private List<Incident> incidents;
}
