package com.example.camunda.service;

import com.example.camunda.dto.DeletionResponse;
import com.example.camunda.dto.GroupMemberResponse;
import com.example.camunda.dto.GroupRequest;
import com.example.camunda.dto.GroupResponse;
import com.example.camunda.dto.RoleResponse;
import com.example.camunda.entity.Group;
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
 * CRUD for groups. A group is scoped to exactly one role, and can hold any number of
 * users — but only users who currently hold that role; membership is validated against
 * this rule on every create/update.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupResponse createGroup(GroupRequest request) {
        if (groupRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A group named '" + request.getName() + "' already exists.");
        }
        Role role = requireRole(request.getRoleId());
        Set<User> members = resolveMembers(request.getUserIds(), role);

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .role(role)
                .users(members)
                .build();

        Group saved = groupRepository.save(group);
        log.info("Created group {} ('{}') with {} member(s), scoped to role '{}'",
                saved.getId(), saved.getName(), members.size(), role.getName());
        return toResponse(saved);
    }

    public List<GroupResponse> findAll() {
        return groupRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse getGroup(Long id) {
        return toResponse(requireGroup(id));
    }

    @Transactional
    public GroupResponse updateGroup(Long id, GroupRequest request) {
        Group group = requireGroup(id);

        boolean nameChanged = !group.getName().equalsIgnoreCase(request.getName());
        if (nameChanged && groupRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A group named '" + request.getName() + "' already exists.");
        }

        Role role = requireRole(request.getRoleId());
        Set<User> members = resolveMembers(request.getUserIds(), role);

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setRole(role);
        // Full replacement of membership, consistent with how User.roleIds works on update.
        group.setUsers(members);

        Group saved = groupRepository.save(group);
        log.info("Updated group {}", id);
        return toResponse(saved);
    }

    @Transactional
    public DeletionResponse deleteGroup(Long id) {
        Group group = requireGroup(id);
        groupRepository.delete(group);
        log.info("Deleted group {}", id);
        return DeletionResponse.builder()
                .id(id)
                .deleted(true)
                .message("Group " + id + " (\"" + group.getName() + "\") deleted successfully.")
                .build();
    }

    /**
     * Resolves the given user ids and enforces the group's core invariant: every member
     * must currently hold the group's role. Unknown ids -> 400; known ids belonging to
     * users without the role -> 400 naming exactly which users and why.
     */
    private Set<User> resolveMembers(List<Long> userIds, Role role) {
        if (CollectionUtils.isEmpty(userIds)) {
            return new HashSet<>();
        }

        Set<Long> distinctIds = new HashSet<>(userIds);
        List<User> found = userRepository.findAllById(distinctIds);
        if (found.size() != distinctIds.size()) {
            Set<Long> foundIds = found.stream().map(User::getId).collect(Collectors.toSet());
            List<Long> missing = distinctIds.stream()
                    .filter(userId -> !foundIds.contains(userId))
                    .collect(Collectors.toList());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown user id(s): " + missing);
        }

        List<User> withoutRole = found.stream()
                .filter(user -> user.getRoles().stream().noneMatch(r -> r.getId().equals(role.getId())))
                .collect(Collectors.toList());
        if (!withoutRole.isEmpty()) {
            String names = withoutRole.stream()
                    .map(user -> user.getFirstName() + " " + user.getLastName() + " (id=" + user.getId() + ")")
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The following user(s) do not have role '" + role.getName() + "' and cannot be added to "
                            + "this group: " + names + ". Assign them the role first.");
        }

        return new HashSet<>(found);
    }

    private Role requireRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No role found with id " + id + "."));
    }

    private Group requireGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No group found with id " + id + "."));
    }

    private GroupResponse toResponse(Group group) {
        List<GroupMemberResponse> members = group.getUsers().stream()
                .map(user -> GroupMemberResponse.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .build())
                .sorted(Comparator.comparing(GroupMemberResponse::getLastName)
                        .thenComparing(GroupMemberResponse::getFirstName))
                .collect(Collectors.toList());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .role(RoleResponse.builder()
                        .id(group.getRole().getId())
                        .name(group.getRole().getName())
                        .description(group.getRole().getDescription())
                        .build())
                .members(members)
                .build();
    }
}
