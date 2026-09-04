package com.example.camunda.controller;

import com.example.camunda.dto.DeletionResponse;
import com.example.camunda.dto.UserRequest;
import com.example.camunda.dto.UserResponse;
import com.example.camunda.service.UserService;
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
 * User management. A user can be associated with multiple roles, assigned by id at
 * creation or update time (roles themselves are managed independently via
 * {@link RoleController}).
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** POST /api/users — add a user, including roles. */
    @PostMapping("/api/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse addUser(@Valid @RequestBody UserRequest request) {
        return userService.createUser(request);
    }

    /** PUT /api/users/{id} — update a user, including roles (full replacement of the role set). */
    @PutMapping("/api/users/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.updateUser(id, request);
    }

    /** DELETE /api/users/{id} — delete a user. */
    @DeleteMapping("/api/users/{id}")
    public DeletionResponse deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }

    /** GET /api/users/{id} — get a user with roles. */
    @GetMapping("/api/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    /** GET /api/users — list all users with roles. */
    @GetMapping("/api/users")
    public List<UserResponse> getAllUsers() {
        return userService.findAll();
    }
}
