package com.example.camunda.controller;

import com.example.camunda.dto.DeploymentResponse;
import com.example.camunda.service.ProcessDeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Generic REST API to deploy ANY Camunda process/decision resource
 * (.bpmn, .bpmn20.xml, .dmn, .cmmn) without prior knowledge of its content.
 */
@RestController
@RequiredArgsConstructor
public class ProcessDeploymentController {

    private final ProcessDeploymentService processDeploymentService;

    /**
     * Deploy any BPMN/DMN/CMMN file.
     *
     * curl -F "file=@my-process.bpmn" -F "deploymentName=My Deployment" \
     *      http://localhost:8080/api/camunda/deployments
     */
    @PostMapping(value = "/api/camunda/deployments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DeploymentResponse deploy(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "deploymentName", required = false) String deploymentName) {
        return processDeploymentService.deploy(file, deploymentName);
    }
}
