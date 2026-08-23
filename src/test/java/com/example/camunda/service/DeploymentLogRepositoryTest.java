package com.example.camunda.service;

import com.example.camunda.entity.DeploymentLog;
import com.example.camunda.repository.DeploymentLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test proving our JPA entity/repository (backed by MySQL in production,
 * H2 in tests) reads and writes correctly, independent of the Camunda engine.
 */
@DataJpaTest
class DeploymentLogRepositoryTest {

    @Autowired
    private DeploymentLogRepository deploymentLogRepository;

    @Test
    void savesAndReadsDeploymentLog() {
        DeploymentLog logEntry = DeploymentLog.builder()
                .camundaDeploymentId("dep-123")
                .deploymentName("Test Deployment")
                .resourceName("test.bpmn")
                .deployedProcessDefinitionKeys("testProcess")
                .deployedAt(LocalDateTime.now())
                .build();

        DeploymentLog saved = deploymentLogRepository.save(logEntry);
        assertThat(saved.getId()).isNotNull();

        Optional<DeploymentLog> found = deploymentLogRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCamundaDeploymentId()).isEqualTo("dep-123");
        assertThat(found.get().getDeployedProcessDefinitionKeys()).isEqualTo("testProcess");
    }
}
