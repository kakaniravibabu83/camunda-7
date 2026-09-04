package com.example.camunda.controller;

import com.example.camunda.dto.DeletionResponse;
import com.example.camunda.dto.RoleRequest;
import com.example.camunda.dto.RoleResponse;
import com.example.camunda.service.RoleService;
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
 * Role management. Roles are managed independently of users and referenced by id when
 * assigning them to a user via {@link UserController}.
 */
@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /** POST /api/roles — add a role. */
    @PostMapping("/api/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse addRole(@Valid @RequestBody RoleRequest request) {
        return roleService.createRole(request);
    }

    /** PUT /api/roles/{id} — update a role. */
    @PutMapping("/api/roles/{id}")
    public RoleResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return roleService.updateRole(id, request);
    }

    /**
     * DELETE /api/roles/{id} — delete a role. Rejected with 409 if the role is
     * currently assigned to one or more users; remove it from them first.
     */
    @DeleteMapping("/api/roles/{id}")
    public DeletionResponse deleteRole(@PathVariable Long id) {
        return roleService.deleteRole(id);
    }

    /** GET /api/roles — list all roles. */
    @GetMapping("/api/roles")
    public List<RoleResponse> getAllRoles() {
        return roleService.findAll();
    }
}
