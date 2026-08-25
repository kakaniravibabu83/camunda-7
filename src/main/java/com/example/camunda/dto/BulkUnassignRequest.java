package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Body for bulk-clearing the assignee on multiple tasks in one call. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkUnassignRequest {
    private List<String> taskIds;
}
