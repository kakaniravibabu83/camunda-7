package com.example.camunda.service;

import com.example.camunda.dto.DeploymentResponse;
import com.example.camunda.dto.ProcessDefinitionInfo;
import com.example.camunda.entity.DeploymentLog;
import com.example.camunda.repository.DeploymentLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Deploys arbitrary Camunda process resources (.bpmn, .bpmn20.xml, .dmn, .cmmn) uploaded
 * through the generic REST API, and records an audit trail via JPA/MySQL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessDeploymentService {

    private static final List<String> SUPPORTED_EXTENSIONS =
            List.of(".bpmn", ".bpmn20.xml", ".dmn", ".dmn11.xml", ".cmmn", ".cmmn10.xml", ".cmmn11.xml");

    private final RepositoryService repositoryService;
    private final DeploymentLogRepository deploymentLogRepository;

    public DeploymentResponse deploy(MultipartFile file, String deploymentNameParam) {
        validateFile(file);

        String resourceName = StringUtils.cleanPath(file.getOriginalFilename());
        String deploymentName = StringUtils.hasText(deploymentNameParam)
                ? deploymentNameParam
                : resourceName;

        Deployment deployment;
        try (InputStream inputStream = file.getInputStream()) {
            deployment = repositoryService.createDeployment()
                    .name(deploymentName)
                    .addInputStream(resourceName, inputStream)
                    .enableDuplicateFiltering(false)
                    .deploy();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file: " + e.getMessage(), e);
        }

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .list();

        List<ProcessDefinitionInfo> processDefinitionInfos = processDefinitions.stream()
                .map(pd -> ProcessDefinitionInfo.builder()
                        .id(pd.getId())
                        .key(pd.getKey())
                        .name(pd.getName())
                        .version(pd.getVersion())
                        .resourceName(pd.getResourceName())
                        .diagramResourceName(pd.getDiagramResourceName())
                        .build())
                .collect(Collectors.toList());

        persistAuditLog(deployment, resourceName, processDefinitionInfos);

        log.info("Deployed '{}' as deployment id {} ({} process definition(s))",
                resourceName, deployment.getId(), processDefinitionInfos.size());

        return DeploymentResponse.builder()
                .deploymentId(deployment.getId())
                .deploymentName(deployment.getName())
                .deploymentTime(LocalDateTime.now())
                .deployedProcessDefinitions(processDefinitionInfos)
                .build();
    }

    private void persistAuditLog(Deployment deployment, String resourceName,
                                  List<ProcessDefinitionInfo> processDefinitionInfos) {
        String keys = processDefinitionInfos.stream()
                .map(ProcessDefinitionInfo::getKey)
                .collect(Collectors.joining(","));

        DeploymentLog auditLog = DeploymentLog.builder()
                .camundaDeploymentId(deployment.getId())
                .deploymentName(deployment.getName())
                .resourceName(resourceName)
                .deployedProcessDefinitionKeys(keys)
                .deployedAt(LocalDateTime.now())
                .build();

        deploymentLogRepository.save(auditLog);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded. Provide a 'file' multipart parameter.");
        }
        String name = file.getOriginalFilename();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Uploaded file has no name.");
        }
        String lowerName = name.toLowerCase();
        boolean supported = SUPPORTED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!supported) {
            throw new IllegalArgumentException(
                    "Unsupported file type '" + name + "'. Supported extensions: " + SUPPORTED_EXTENSIONS);
        }
    }
}
