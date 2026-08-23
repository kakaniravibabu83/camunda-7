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
public class ProcessInstanceStatusResponse {
    private String processInstanceId;
    private String processDefinitionId;
    private String businessKey;
    private String state;
    private Date startTime;
    private Date endTime;
    private Long durationInMillis;
}
