//package com.example.camunda.controller;
//
//import com.example.camunda.dto.GroupRequest;
//import com.example.camunda.dto.RoleRequest;
//import com.example.camunda.dto.UserRequest;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//
//import java.util.List;
//import java.util.UUID;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class GroupControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private String uniqueEmail() {
//        return "group-test-" + UUID.randomUUID() + "@example.com";
//    }
//
//    private Long createRole(String name) throws Exception {
//        RoleRequest request = new RoleRequest();
//        request.setName(name);
//        request.setDescription("Created for GroupControllerTest");
//        MvcResult result = mockMvc.perform(post("/api/roles")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
//    }
//
//    private Long createUserWithRoles(String firstName, List<Long> roleIds) throws Exception {
//        UserRequest request = new UserRequest();
//        request.setFirstName(firstName);
//        request.setLastName("Test");
//        request.setEmail(uniqueEmail());
//        request.setBusinessUnit("Operations");
//        request.setRoleIds(roleIds);
//        MvcResult result = mockMvc.perform(post("/api/users")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
//    }
//
//    private GroupRequest groupRequest(String name, Long roleId, List<Long> userIds) {
//        GroupRequest request = new GroupRequest();
//        request.setName(name);
//        request.setDescription("Created for GroupControllerTest");
//        request.setRoleId(roleId);
//        request.setUserIds(userIds);
//        return request;
//    }
//
//    @Test
//    void addGroup_withMembersSharingTheRole_createsAndReturnsIt() throws Exception {
//        Long roleId = createRole("ROLE_GRP_" + UUID.randomUUID());
//        Long user1 = createUserWithRoles("Alice", List.of(roleId));
//        Long user2 = createUserWithRoles("Bob", List.of(roleId));
//
//        mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(
//                                groupRequest("Legal Reviewers", roleId, List.of(user1, user2)))))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").isNotEmpty())
//                .andExpect(jsonPath("$.name").value("Legal Reviewers"))
//                .andExpect(jsonPath("$.role.id").value(roleId))
//                .andExpect(jsonPath("$.members.length()").value(2));
//    }
//
//    @Test
//    void addGroup_withNoMembers_createsSuccessfullyWithEmptyMemberList() throws Exception {
//        Long roleId = createRole("ROLE_EMPTY_GRP_" + UUID.randomUUID());
//
//        mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest("Empty Group", roleId, null))))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.members.length()").value(0));
//    }
//
//    @Test
//    void addGroup_memberWithoutTheGroupsRole_returnsBadRequest() throws Exception {
//        Long groupRoleId = createRole("ROLE_GROUP_SCOPE_" + UUID.randomUUID());
//        Long otherRoleId = createRole("ROLE_OTHER_" + UUID.randomUUID());
//        // This user has a *different* role, not the group's role.
//        Long userWithoutRole = createUserWithRoles("Charlie", List.of(otherRoleId));
//
//        mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(
//                                groupRequest("Mismatched Group", groupRoleId, List.of(userWithoutRole)))))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void addGroup_unknownUserId_returnsBadRequest() throws Exception {
//        Long roleId = createRole("ROLE_UNKNOWN_USER_" + UUID.randomUUID());
//
//        mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(
//                                groupRequest("Ghost Members", roleId, List.of(999999L)))))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void addGroup_unknownRoleId_returnsNotFound() throws Exception {
//        mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest("Ghost Role", 999999L, null))))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void addGroup_missingRoleId_returnsBadRequest() throws Exception {
//        mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest("No Role", null, null))))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void addGroup_duplicateName_returnsConflict() throws Exception {
//        Long roleId = createRole("ROLE_DUP_GRP_" + UUID.randomUUID());
//        String name = "Duplicate Group " + UUID.randomUUID();
//        mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest(name, roleId, null))))
//                .andExpect(status().isCreated());
//
//        mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest(name, roleId, null))))
//                .andExpect(status().isConflict());
//    }
//
//    @Test
//    void getGroup_returnsDetails_andUnknownIdReturnsNotFound() throws Exception {
//        Long roleId = createRole("ROLE_GET_GRP_" + UUID.randomUUID());
//        MvcResult created = mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest("Get Test Group", roleId, null))))
//                .andExpect(status().isCreated())
//                .andReturn();
//        Long groupId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
//
//        mockMvc.perform(get("/api/groups/{id}", groupId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(groupId));
//
//        mockMvc.perform(get("/api/groups/{id}", 999999))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void getAllGroups_includesCreatedGroup() throws Exception {
//        Long roleId = createRole("ROLE_LIST_GRP_" + UUID.randomUUID());
//        String name = "List Test Group " + UUID.randomUUID();
//        mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest(name, roleId, null))))
//                .andExpect(status().isCreated());
//
//        mockMvc.perform(get("/api/groups"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[?(@.name == '" + name + "')]").exists());
//    }
//
//    @Test
//    void updateGroup_replacesMembershipFully() throws Exception {
//        Long roleId = createRole("ROLE_UPDATE_GRP_" + UUID.randomUUID());
//        Long user1 = createUserWithRoles("Dana", List.of(roleId));
//        Long user2 = createUserWithRoles("Eli", List.of(roleId));
//
//        MvcResult created = mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(
//                                groupRequest("Update Test Group", roleId, List.of(user1)))))
//                .andExpect(status().isCreated())
//                .andReturn();
//        Long groupId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
//
//        // Replace membership: drop user1, add user2.
//        mockMvc.perform(put("/api/groups/{id}", groupId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(
//                                groupRequest("Update Test Group", roleId, List.of(user2)))))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.members.length()").value(1))
//                .andExpect(jsonPath("$.members[0].id").value(user2));
//    }
//
//    @Test
//    void updateGroup_unknownId_returnsNotFound() throws Exception {
//        Long roleId = createRole("ROLE_UPDATE_404_" + UUID.randomUUID());
//        mockMvc.perform(put("/api/groups/{id}", 999999)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest("Ghost Group", roleId, null))))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void deleteGroup_succeedsWithConfirmation() throws Exception {
//        Long roleId = createRole("ROLE_DELETE_GRP_" + UUID.randomUUID());
//        MvcResult created = mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest("Delete Me Group", roleId, null))))
//                .andExpect(status().isCreated())
//                .andReturn();
//        Long groupId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
//
//        mockMvc.perform(delete("/api/groups/{id}", groupId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(groupId))
//                .andExpect(jsonPath("$.deleted").value(true));
//
//        mockMvc.perform(get("/api/groups/{id}", groupId))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void deleteGroup_unknownId_returnsNotFound() throws Exception {
//        mockMvc.perform(delete("/api/groups/{id}", 999999))
//                .andExpect(status().isNotFound());
//    }
//
//    // ---------------------------------------------------------------- cross-cutting guards
//
//    @Test
//    void deletingARoleStillScopingAGroup_isRejected_untilTheGroupIsDeleted() throws Exception {
//        Long roleId = createRole("ROLE_SCOPES_GROUP_" + UUID.randomUUID());
//        MvcResult created = mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest("Scoping Group", roleId, null))))
//                .andExpect(status().isCreated())
//                .andReturn();
//        Long groupId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
//
//        mockMvc.perform(delete("/api/roles/{id}", roleId))
//                .andExpect(status().isConflict());
//
//        mockMvc.perform(delete("/api/groups/{id}", groupId))
//                .andExpect(status().isOk());
//
//        mockMvc.perform(delete("/api/roles/{id}", roleId))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void deletingAUserStillInAGroup_isRejected_untilRemovedFromTheGroup() throws Exception {
//        Long roleId = createRole("ROLE_MEMBER_GUARD_" + UUID.randomUUID());
//        Long userId = createUserWithRoles("Frankie", List.of(roleId));
//
//        MvcResult created = mockMvc.perform(post("/api/groups")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(
//                                groupRequest("Guard Group", roleId, List.of(userId)))))
//                .andExpect(status().isCreated())
//                .andReturn();
//        Long groupId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
//
//        mockMvc.perform(delete("/api/users/{id}", userId))
//                .andExpect(status().isConflict());
//
//        // Remove the user from the group (full replacement with an empty member list)...
//        mockMvc.perform(put("/api/groups/{id}", groupId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(groupRequest("Guard Group", roleId, null))))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.members.length()").value(0));
//
//        // ...now the user can be deleted.
//        mockMvc.perform(delete("/api/users/{id}", userId))
//                .andExpect(status().isOk());
//    }
//}
