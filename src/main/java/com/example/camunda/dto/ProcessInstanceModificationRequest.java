package com.example.camunda.dto;

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
public class ProcessInstanceModificationRequest {
    private Boolean skipCustomListeners;
    private Boolean skipIoMappings;
    private List<ModificationInstruction> instructions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModificationInstruction {
        private String type; // cancel, startBeforeActivity, startAfterActivity, startTransition
        private String activityId;
        private String transitionId;
        private String activityInstanceId;
        private String transitionInstanceId;
        private String ancestorActivityInstanceId;
        private Map<String, Object> variables;
    }
}
