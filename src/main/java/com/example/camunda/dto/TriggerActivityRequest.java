package com.example.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Body for dynamically instantiating any named activity in a running process instance
 * on demand - the mechanism behind on-demand, UI-driven task triggering (e.g. a case
 * management UI deciding at runtime which task to create next, in any order).
 * <p>
 * {@link #activityId} must be the BPMN element id (not name) as defined in the process
 * definition, e.g. "UserTask_LegalReview". {@link #variables} is optional.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to trigger an activity in a running process instance")
public class TriggerActivityRequest {
    @Schema(description = "BPMN element ID of the activity to trigger", example = "UserTask_LegalReview")
    private String activityId;

    @Schema(description = "Optional variables to pass to the activity", example = "{\"note\": \"please expedite\"}")
    private Map<String, Object> variables;
}
