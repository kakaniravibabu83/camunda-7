package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInfo {
    private String id;
    private String jobDefinitionId;
    private String processInstanceId;
    private String processDefinitionId;
    private String executionId;
    private int retries;
    private String exceptionMessage;
    private Date dueDate;
    private int priority;
    private boolean suspended;
}
