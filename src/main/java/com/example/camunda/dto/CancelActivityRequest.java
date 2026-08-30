package com.example.camunda.dto;

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
public class CancelActivityRequest {
    private String activityId;
}
