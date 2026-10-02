package com.example.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Response body for process instance modification operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response after modifying a process instance")
public class ProcessInstanceModificationResponse {
    @Schema(description = "ID of the modified process instance", example = "abc123-def456-ghi789")
    private String processInstanceId;

    @Schema(description = "Success message", example = "Process instance modified successfully")
    private String message;
}
