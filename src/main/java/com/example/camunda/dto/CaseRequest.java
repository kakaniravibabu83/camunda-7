package com.example.camunda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CaseRequest {

    @NotBlank(message = "is required")
    @Size(max = 200, message = "must be at most 200 characters")
    private String title;

    @Size(max = 2000, message = "must be at most 2000 characters")
    private String description;

    @Size(max = 50, message = "must be at most 50 characters")
    private String status;
}
