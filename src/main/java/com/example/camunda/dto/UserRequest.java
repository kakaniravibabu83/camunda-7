package com.example.camunda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Body for creating or updating a user. {@link #roleIds} references existing roles by
 * id — roles themselves are managed independently via {@code /api/roles} and assigned
 * here rather than created inline. Optional: a user may have no roles yet.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @NotBlank(message = "is required")
    @Size(max = 100, message = "must be at most 100 characters")
    private String firstName;

    @NotBlank(message = "is required")
    @Size(max = 100, message = "must be at most 100 characters")
    private String lastName;

    @Size(max = 30, message = "must be at most 30 characters")
    private String phone;

    @NotBlank(message = "is required")
    @Email(message = "must be a valid email address")
    @Size(max = 255, message = "must be at most 255 characters")
    private String email;

    @Size(max = 150, message = "must be at most 150 characters")
    private String businessUnit;

    private List<Long> roleIds;
}
