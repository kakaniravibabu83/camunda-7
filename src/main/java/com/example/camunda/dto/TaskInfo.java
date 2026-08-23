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
public class TaskInfo {
    private String id;
    private String name;
    private String description;
    private String taskDefinitionKey;
    private String processInstanceId;
    private String processDefinitionId;
    private String executionId;
    private String assignee;
    private String owner;
    private int priority;
    private Date createTime;
    private Date dueDate;
    private Date followUpDate;
}
