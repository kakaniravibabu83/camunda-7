package com.example.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after starting a process instance")
public class StartProcessResponse {
    @Schema(description = "ID of the created process instance", example = "abc123-def456-ghi789")
    private String processInstanceId;

    @Schema(description = "ID of the process definition used", example = "sampleApprovalProcess:1:abc123")
    private String processDefinitionId;

    @Schema(description = "Key of the process definition", example = "sampleApprovalProcess")
    private String processDefinitionKey;

    @Schema(description = "Business key associated with the process instance", example = "ORDER-1001")
    private String businessKey;

    @Schema(description = "Whether the process instance has already ended", example = "false")
    private boolean ended;

    @Schema(description = "Current process variables")
    private Map<String, Object> variables;
}
