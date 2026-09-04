package com.example.camunda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Body for creating or updating a group. {@link #roleId} is required — a group is
 * always scoped to exactly one role, and every id in {@link #userIds} must reference a
 * user who currently holds that role (validated at the service layer). {@link #userIds}
 * is a full replacement of membership on update, not an add.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {

    @NotBlank(message = "is required")
    @Size(max = 150, message = "must be at most 150 characters")
    private String name;

    @Size(max = 500, message = "must be at most 500 characters")
    private String description;

    @NotNull(message = "is required")
    private Long roleId;

    private List<Long> userIds;
}
