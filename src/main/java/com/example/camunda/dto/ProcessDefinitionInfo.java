package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDefinitionInfo {
    private String id;
    private String key;
    private String name;
    private int version;
    private String resourceName;
    private String diagramResourceName;
}
