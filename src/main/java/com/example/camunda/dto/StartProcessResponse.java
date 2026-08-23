package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartProcessResponse {
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String businessKey;
    private boolean ended;
    private Map<String, Object> variables;
}
