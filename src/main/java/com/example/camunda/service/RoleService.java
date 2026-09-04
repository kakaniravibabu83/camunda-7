package com.example.camunda.service;

import com.example.camunda.dto.DeletionResponse;
import com.example.camunda.dto.RoleRequest;
import com.example.camunda.dto.RoleResponse;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CRUD for roles. Roles are managed independently of users (per spec) and referenced by
 * id when assigning them to a user via {@link UserService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A role named '" + request.getName() + "' already exists.");
        }
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Role saved = roleRepository.save(role);
        log.info("Created role {} ('{}')", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse getRole(Long id) {
        return toResponse(requireRole(id));
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = requireRole(id);
        boolean nameChanged = !role.getName().equalsIgnoreCase(request.getName());
        if (nameChanged && roleRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A role named '" + request.getName() + "' already exists.");
        }
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        Role saved = roleRepository.save(role);
        log.info("Updated role {}", id);
        return toResponse(saved);
    }

    @Transactional
    public DeletionResponse deleteRole(Long id) {
        Role role = requireRole(id);

        List<User> usersWithRole = userRepository.findByRoleId(id);
        if (!usersWithRole.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete role '" + role.getName() + "': currently assigned to "
                            + usersWithRole.size() + " user(s). Remove it from those users first.");
        }

        if (groupRepository.existsByRole_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete role '" + role.getName() + "': one or more groups are still scoped to it. "
                            + "Delete or reassign those groups first.");
        }

        roleRepository.delete(role);
        log.info("Deleted role {}", id);
        return DeletionResponse.builder()
                .id(id)
                .deleted(true)
                .message("Role " + id + " (\"" + role.getName() + "\") deleted successfully.")
                .build();
    }

    private Role requireRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No role found with id " + id + "."));
    }

    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}
