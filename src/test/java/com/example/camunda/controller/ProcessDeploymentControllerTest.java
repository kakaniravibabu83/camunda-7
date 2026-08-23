package com.example.camunda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProcessDeploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deploy_withValidBpmnFile_returnsCreatedDeployment() throws Exception {
        MockMultipartFile file = bpmnFile();

        mockMvc.perform(multipart("/api/camunda/deployments")
                        .file(file)
                        .param("deploymentName", "Integration Test Deployment"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deploymentId").isNotEmpty())
                .andExpect(jsonPath("$.deploymentName").value("Integration Test Deployment"))
                .andExpect(jsonPath("$.deployedProcessDefinitions[0].key").value("uploadTestProcess"));
    }

    @Test
    void deploy_withoutFile_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/camunda/deployments"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deploy_withUnsupportedFileType_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "not-a-process.txt", "text/plain", "hello world".getBytes());

        mockMvc.perform(multipart("/api/camunda/deployments").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    private MockMultipartFile bpmnFile() throws IOException {
        try (InputStream is = new ClassPathResource("bpmn/upload-test-process.bpmn").getInputStream()) {
            return new MockMultipartFile(
                    "file", "upload-test-process.bpmn", "text/xml", is.readAllBytes());
        }
    }
}
