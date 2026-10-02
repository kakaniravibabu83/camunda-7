package com.example.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an incident associated with an activity or transition instance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Represents an incident associated with an activity or transition instance")
public class Incident {
    @Schema(description = "Incident ID", example = "incident123")
    private String id;

    @Schema(description = "Activity ID where the incident occurred", example = "AttachedTimerBoundaryEvent")
    private String activityId;
}
