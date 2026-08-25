package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of a bulk operation over multiple task ids. Camunda's engine has no native
 * bulk-assign API, so each task id is processed individually and its outcome recorded
 * here — a single bad/unknown id does not abort the rest of the batch.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTaskOperationResult {
    private int successCount;
    private int failureCount;
    private List<String> successfulTaskIds;
    private List<TaskOperationFailure> failures;
}
