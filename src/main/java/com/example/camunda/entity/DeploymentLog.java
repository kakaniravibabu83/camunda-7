package com.example.camunda.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Local JPA-managed audit record of every BPMN deployment performed through the
 * generic REST API. Persisted via MySQL/JPA, independent from Camunda's own
 * ACT_RE_DEPLOYMENT table, purely to demonstrate/justify the JPA dependency and
 * to give callers a simple audit trail.
 */
@Entity
@Table(name = "deployment_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camunda_deployment_id", nullable = false, length = 64)
    private String camundaDeploymentId;

    @Column(name = "deployment_name", nullable = false)
    private String deploymentName;

    @Column(name = "resource_name", nullable = false)
    private String resourceName;

    @Column(name = "deployed_process_definition_keys", length = 1024)
    private String deployedProcessDefinitionKeys;

    @Column(name = "deployed_at", nullable = false)
    private LocalDateTime deployedAt;
}
