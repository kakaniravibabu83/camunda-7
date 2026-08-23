package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Body for the "complete" endpoint. {@link #variables} is entirely optional, exactly
 * like {@link StartProcessRequest#getVariables()} — some tasks need none, others need
 * many.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTaskRequest {
    private Map<String, Object> variables;
}
