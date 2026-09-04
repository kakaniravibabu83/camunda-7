package com.example.camunda.controller;

import com.example.camunda.dto.DeletionResponse;
import com.example.camunda.dto.GroupRequest;
import com.example.camunda.dto.GroupResponse;
import com.example.camunda.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Group management. Each group is scoped to exactly one role, and can hold multiple
 * users — but only users who currently hold that role (enforced by
 * {@link GroupService}).
 */
@RestController
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /** POST /api/groups — add a group. */
    @PostMapping("/api/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse addGroup(@Valid @RequestBody GroupRequest request) {
        return groupService.createGroup(request);
    }

    /** PUT /api/groups/{id} — update a group (userIds is a full replacement of membership). */
    @PutMapping("/api/groups/{id}")
    public GroupResponse updateGroup(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        return groupService.updateGroup(id, request);
    }

    /** DELETE /api/groups/{id} — delete a group. */
    @DeleteMapping("/api/groups/{id}")
    public DeletionResponse deleteGroup(@PathVariable Long id) {
        return groupService.deleteGroup(id);
    }

    /** GET /api/groups/{id} — get a group with its role and members. */
    @GetMapping("/api/groups/{id}")
    public GroupResponse getGroup(@PathVariable Long id) {
        return groupService.getGroup(id);
    }

    /** GET /api/groups — list all groups. */
    @GetMapping("/api/groups")
    public List<GroupResponse> getAllGroups() {
        return groupService.findAll();
    }
}
