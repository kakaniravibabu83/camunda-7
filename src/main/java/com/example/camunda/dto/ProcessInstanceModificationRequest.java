package com.example.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Request body for modifying a running process instance.
 * Supports canceling activities, starting before/after activities, and starting transitions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for modifying a running process instance")
public class ProcessInstanceModificationRequest {
    @Schema(description = "Skip custom listeners during modification", example = "false")
    private Boolean skipCustomListeners;

    @Schema(description = "Skip input/output mappings during modification", example = "false")
    private Boolean skipIoMappings;

    @Schema(description = "List of modification instructions to execute")
    private List<ModificationInstruction> instructions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Single modification instruction")
    public static class ModificationInstruction {
        @Schema(description = "Instruction type: cancel, startBeforeActivity, startAfterActivity, startTransition", example = "startBeforeActivity")
        private String type;

        @Schema(description = "Activity ID for startBeforeActivity, startAfterActivity, or cancel instructions", example = "UserTask_Review")
        private String activityId;

        @Schema(description = "Transition ID for startTransition instruction", example = "SequenceFlow_1")
        private String transitionId;

        @Schema(description = "Activity instance ID to cancel", example = "UserTask_Review:abc123")
        private String activityInstanceId;

        @Schema(description = "Transition instance ID to cancel", example = "SequenceFlow_1:def456")
        private String transitionInstanceId;

        @Schema(description = "Ancestor activity instance ID for hierarchical processes", example = "SubProcess_1:ghi789")
        private String ancestorActivityInstanceId;

        @Schema(description = "Variables to set for the instruction", example = "{\"note\": \"please expedite\"}")
        private Map<String, Object> variables;
    }
}
