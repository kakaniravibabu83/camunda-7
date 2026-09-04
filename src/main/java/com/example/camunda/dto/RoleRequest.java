package com.example.camunda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body for creating or updating a role. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @NotBlank(message = "is required")
    @Size(max = 100, message = "must be at most 100 characters")
    private String name;

    @Size(max = 500, message = "must be at most 500 characters")
    private String description;
}
