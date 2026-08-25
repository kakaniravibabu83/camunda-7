package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Body for bulk-assigning multiple tasks to the SAME user in one call.
 * Both fields are required; {@link #taskIds} must be non-empty.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignRequest {
    private List<String> taskIds;
    private String userId;
}
