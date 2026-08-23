package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body for both the "assign" and "claim" endpoints — both simply need a target user id.
 * (Assign sets the assignee unconditionally; claim additionally enforces that the task
 * isn't already claimed by someone else.)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserIdRequest {
    private String userId;
}
