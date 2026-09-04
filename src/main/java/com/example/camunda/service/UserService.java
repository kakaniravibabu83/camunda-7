package com.example.camunda.service;

import com.example.camunda.dto.DeletionResponse;
import com.example.camunda.dto.RoleResponse;
import com.example.camunda.dto.UserRequest;
import com.example.camunda.dto.UserResponse;
import com.example.camunda.entity.Role;
import com.example.camunda.entity.User;
import com.example.camunda.repository.GroupRepository;
import com.example.camunda.repository.RoleRepository;
import com.example.camunda.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRUD for users. A user can be associated with any number of roles, assigned by id at
 * creation or update time — roles themselves are managed independently via
 * {@link RoleService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user with email '" + request.getEmail() + "' already exists.");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .businessUnit(request.getBusinessUnit())
                .roles(resolveRoles(request.getRoleIds()))
                .build();

        User saved = userRepository.save(user);
        log.info("Created user {} ({}) with {} role(s)", saved.getId(), saved.getEmail(), saved.getRoles().size());
        return toResponse(saved);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUser(Long id) {
        return toResponse(requireUser(id));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = requireUser(id);

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(request.getEmail());
        if (emailChanged && userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user with email '" + request.getEmail() + "' already exists.");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setBusinessUnit(request.getBusinessUnit());
        // Full replacement of the role set, consistent with a PUT (update) semantics -
        // pass the complete desired role id list, not just ones to add.
        user.setRoles(resolveRoles(request.getRoleIds()));

        User saved = userRepository.save(user);
        log.info("Updated user {}", id);
        return toResponse(saved);
    }

    @Transactional
    public DeletionResponse deleteUser(Long id) {
        User user = requireUser(id);
        if (groupRepository.existsByUsers_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete user " + id + ": still a member of one or more groups. "
                            + "Remove them from those groups first.");
        }
        userRepository.delete(user);
        log.info("Deleted user {}", id);
        return DeletionResponse.builder()
                .id(id)
                .deleted(true)
                .message("User " + id + " deleted successfully.")
                .build();
    }

    private Set<Role> resolveRoles(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return new HashSet<>();
        }
        Set<Long> distinctIds = new HashSet<>(roleIds);
        List<Role> found = roleRepository.findAllById(distinctIds);
        if (found.size() != distinctIds.size()) {
            Set<Long> foundIds = found.stream().map(Role::getId).collect(Collectors.toSet());
            List<Long> missing = distinctIds.stream()
                    .filter(roleId -> !foundIds.contains(roleId))
                    .collect(Collectors.toList());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role id(s): " + missing);
        }
        return new HashSet<>(found);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No user found with id " + id + "."));
    }

    private UserResponse toResponse(User user) {
        List<RoleResponse> roles = user.getRoles().stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .description(role.getDescription())
                        .build())
                .sorted(Comparator.comparing(RoleResponse::getName))
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .businessUnit(user.getBusinessUnit())
                .roles(roles)
                .build();
    }
}
