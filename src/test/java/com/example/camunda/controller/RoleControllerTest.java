//package com.example.camunda.controller;
//
//import com.example.camunda.dto.RoleRequest;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
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
//class RoleControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private RoleRequest roleRequest(String name, String description) {
//        RoleRequest request = new RoleRequest();
//        request.setName(name);
//        request.setDescription(description);
//        return request;
//    }
//
//    private Long createRole(String name, String description) throws Exception {
//        MvcResult result = mockMvc.perform(post("/api/roles")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(roleRequest(name, description))))
//                .andExpect(status().isCreated())
//                .andReturn();
//        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
//    }
//
//    @Test
//    void addRole_createsAndReturnsIt() throws Exception {
//        mockMvc.perform(post("/api/roles")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(roleRequest("ROLE_APPROVER", "Can approve requests"))))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").isNotEmpty())
//                .andExpect(jsonPath("$.name").value("ROLE_APPROVER"))
//                .andExpect(jsonPath("$.description").value("Can approve requests"));
//    }
//
//    @Test
//    void addRole_duplicateName_returnsConflict() throws Exception {
//        createRole("ROLE_DUPLICATE", "first");
//
//        mockMvc.perform(post("/api/roles")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(roleRequest("ROLE_DUPLICATE", "second"))))
//                .andExpect(status().isConflict());
//    }
//
//    @Test
//    void addRole_missingName_returnsBadRequest() throws Exception {
//        mockMvc.perform(post("/api/roles")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(roleRequest(null, "no name"))))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void getAllRoles_includesCreatedRole() throws Exception {
//        createRole("ROLE_LIST_TEST", "for list test");
//
//        mockMvc.perform(get("/api/roles"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[?(@.name == 'ROLE_LIST_TEST')]").exists());
//    }
//
//    @Test
//    void updateRole_changesNameAndDescription() throws Exception {
//        Long id = createRole("ROLE_BEFORE_UPDATE", "before");
//
//        mockMvc.perform(put("/api/roles/{id}", id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(roleRequest("ROLE_AFTER_UPDATE", "after"))))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.name").value("ROLE_AFTER_UPDATE"))
//                .andExpect(jsonPath("$.description").value("after"));
//    }
//
//    @Test
//    void updateRole_toNameOwnedByAnotherRole_returnsConflict() throws Exception {
//        createRole("ROLE_TAKEN", "taken");
//        Long id = createRole("ROLE_TO_RENAME", "will try to rename");
//
//        mockMvc.perform(put("/api/roles/{id}", id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(roleRequest("ROLE_TAKEN", "conflict"))))
//                .andExpect(status().isConflict());
//    }
//
//    @Test
//    void updateRole_unknownId_returnsNotFound() throws Exception {
//        mockMvc.perform(put("/api/roles/{id}", 999999)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(roleRequest("ROLE_GHOST", "does not exist"))))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void deleteRole_notInUse_succeedsWithConfirmation() throws Exception {
//        Long id = createRole("ROLE_DELETE_ME", "to be deleted");
//
//        mockMvc.perform(delete("/api/roles/{id}", id))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(id))
//                .andExpect(jsonPath("$.deleted").value(true));
//
//        mockMvc.perform(get("/api/roles"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[?(@.id == " + id + ")]").doesNotExist());
//    }
//
//    @Test
//    void deleteRole_unknownId_returnsNotFound() throws Exception {
//        mockMvc.perform(delete("/api/roles/{id}", 999999))
//                .andExpect(status().isNotFound());
//    }
//}
