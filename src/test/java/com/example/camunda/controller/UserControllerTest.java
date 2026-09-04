//package com.example.camunda.controller;
//
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
//class UserControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    /** Every test gets a fresh, globally-unique email so tests never collide on the uniqueness constraint. */
//    private String uniqueEmail() {
//        return "user-" + UUID.randomUUID() + "@example.com";
//    }
//
//    private Long createRole(String name, String description) throws Exception {
//        RoleRequest request = new RoleRequest();
//        request.setName(name);
//        request.setDescription(description);
//        MvcResult result = mockMvc.perform(post("/api/roles")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
//    }
//
//    private UserRequest userRequest(String firstName, String lastName, String email, List<Long> roleIds) {
//        UserRequest request = new UserRequest();
//        request.setFirstName(firstName);
//        request.setLastName(lastName);
//        request.setPhone("+1-555-0100");
//        request.setEmail(email);
//        request.setBusinessUnit("Operations");
//        request.setRoleIds(roleIds);
//        return request;
//    }
//
//    private Long createUser(UserRequest request) throws Exception {
//        MvcResult result = mockMvc.perform(post("/api/users")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
//    }
//
//    @Test
//    void addUser_withRoles_createsAndReturnsThemSorted() throws Exception {
//        Long legalId = createRole("ROLE_LEGAL_" + UUID.randomUUID(), "Legal reviewer");
//        Long financeId = createRole("ROLE_FINANCE_" + UUID.randomUUID(), "Finance approver");
//
//        UserRequest request = userRequest("Jane", "Doe", uniqueEmail(), List.of(legalId, financeId));
//
//        mockMvc.perform(post("/api/users")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").isNotEmpty())
//                .andExpect(jsonPath("$.firstName").value("Jane"))
//                .andExpect(jsonPath("$.lastName").value("Doe"))
//                .andExpect(jsonPath("$.businessUnit").value("Operations"))
//                .andExpect(jsonPath("$.roles.length()").value(2));
//    }
//
//    @Test
//    void addUser_withNoRoles_createsSuccessfullyWithEmptyRoleList() throws Exception {
//        UserRequest request = userRequest("NoRole", "Person", uniqueEmail(), null);
//
//        mockMvc.perform(post("/api/users")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.roles.length()").value(0));
//    }
//
//    @Test
//    void addUser_duplicateEmail_returnsConflict() throws Exception {
//        String email = uniqueEmail();
//        createUser(userRequest("First", "User", email, null));
//
//        mockMvc.perform(post("/api/users")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(userRequest("Second", "User", email, null))))
//                .andExpect(status().isConflict());
//    }
//
//    @Test
//    void addUser_missingRequiredFields_returnsBadRequest() throws Exception {
//        UserRequest request = new UserRequest();
//        request.setEmail(uniqueEmail());
//        // firstName and lastName intentionally omitted
//
//        mockMvc.perform(post("/api/users")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void addUser_invalidEmail_returnsBadRequest() throws Exception {
//        UserRequest request = userRequest("Bad", "Email", "not-an-email", null);
//
//        mockMvc.perform(post("/api/users")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void addUser_unknownRoleId_returnsBadRequest() throws Exception {
//        UserRequest request = userRequest("Bad", "Role", uniqueEmail(), List.of(999999L));
//
//        mockMvc.perform(post("/api/users")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void getUser_returnsDetailsWithRoles_andUnknownIdReturnsNotFound() throws Exception {
//        Long roleId = createRole("ROLE_GET_TEST_" + UUID.randomUUID(), "for get test");
//        Long userId = createUser(userRequest("Get", "Test", uniqueEmail(), List.of(roleId)));
//
//        mockMvc.perform(get("/api/users/{id}", userId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(userId))
//                .andExpect(jsonPath("$.roles[0].id").value(roleId));
//
//        mockMvc.perform(get("/api/users/{id}", 999999))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void getAllUsers_includesCreatedUser() throws Exception {
//        String email = uniqueEmail();
//        createUser(userRequest("List", "Test", email, null));
//
//        mockMvc.perform(get("/api/users"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[?(@.email == '" + email + "')]").exists());
//    }
//
//    @Test
//    void updateUser_replacesFieldsAndRoleSet() throws Exception {
//        Long roleA = createRole("ROLE_A_" + UUID.randomUUID(), "role a");
//        Long roleB = createRole("ROLE_B_" + UUID.randomUUID(), "role b");
//        Long userId = createUser(userRequest("Before", "Update", uniqueEmail(), List.of(roleA)));
//
//        UserRequest update = userRequest("After", "Update", uniqueEmail(), List.of(roleB));
//
//        mockMvc.perform(put("/api/users/{id}", userId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(update)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.firstName").value("After"))
//                .andExpect(jsonPath("$.roles.length()").value(1))
//                .andExpect(jsonPath("$.roles[0].id").value(roleB));
//    }
//
//    @Test
//    void updateUser_toEmailOwnedByAnotherUser_returnsConflict() throws Exception {
//        String takenEmail = uniqueEmail();
//        createUser(userRequest("Owner", "OfEmail", takenEmail, null));
//        Long userId = createUser(userRequest("Wants", "ThatEmail", uniqueEmail(), null));
//
//        mockMvc.perform(put("/api/users/{id}", userId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(userRequest("Wants", "ThatEmail", takenEmail, null))))
//                .andExpect(status().isConflict());
//    }
//
//    @Test
//    void updateUser_unknownId_returnsNotFound() throws Exception {
//        mockMvc.perform(put("/api/users/{id}", 999999)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(userRequest("Ghost", "User", uniqueEmail(), null))))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void deleteUser_succeedsWithConfirmation() throws Exception {
//        Long userId = createUser(userRequest("Delete", "Me", uniqueEmail(), null));
//
//        mockMvc.perform(delete("/api/users/{id}", userId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(userId))
//                .andExpect(jsonPath("$.deleted").value(true));
//
//        mockMvc.perform(get("/api/users/{id}", userId))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void deleteUser_unknownId_returnsNotFound() throws Exception {
//        mockMvc.perform(delete("/api/users/{id}", 999999))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void deletingARoleAssignedToAUser_isRejected_untilUnassigned() throws Exception {
//        Long roleId = createRole("ROLE_IN_USE_" + UUID.randomUUID(), "assigned to a user");
//        Long userId = createUser(userRequest("Has", "TheRole", uniqueEmail(), List.of(roleId)));
//
//        // Role is in use -> deletion blocked.
//        mockMvc.perform(delete("/api/roles/{id}", roleId))
//                .andExpect(status().isConflict());
//
//        // Update the user to no longer reference the role...
//        mockMvc.perform(put("/api/users/{id}", userId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(userRequest("Has", "TheRole", uniqueEmail(), null))))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.roles.length()").value(0));
//
//        // ...now the role can be deleted.
//        mockMvc.perform(delete("/api/roles/{id}", roleId))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void userCanHaveMultipleRolesSimultaneously() throws Exception {
//        Long role1 = createRole("ROLE_MULTI_1_" + UUID.randomUUID(), "first");
//        Long role2 = createRole("ROLE_MULTI_2_" + UUID.randomUUID(), "second");
//        Long role3 = createRole("ROLE_MULTI_3_" + UUID.randomUUID(), "third");
//
//        Long userId = createUser(userRequest("Multi", "Role", uniqueEmail(), List.of(role1, role2, role3)));
//
//        mockMvc.perform(get("/api/users/{id}", userId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.roles.length()").value(3));
//    }
//}
