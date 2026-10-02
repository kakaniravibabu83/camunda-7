package com.example.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body for cancelling all currently active instances of a named activity in a running
 * process instance - e.g. cancelling a wrapping sub-process to close out a case
 * regardless of which tasks happen to be open inside it at the time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to cancel all instances of an activity")
public class CancelActivityRequest {
    @Schema(description = "BPMN element ID of the activity to cancel", example = "SubProcess_CaseTasks")
    private String activityId;
}
