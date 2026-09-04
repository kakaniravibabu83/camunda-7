package com.example.camunda.repository;

import com.example.camunda.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    boolean existsByNameIgnoreCase(String name);

    /** Used to block deleting a role that one or more groups are still scoped to. */
    boolean existsByRole_Id(Long roleId);

    /** Used to block deleting a user who is still a member of one or more groups. */
    boolean existsByUsers_Id(Long userId);
}
