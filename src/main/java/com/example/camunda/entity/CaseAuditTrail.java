package com.example.camunda.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_audit_trail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseAuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "case_number", nullable = false, length = 20)
    private String caseNumber;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "camunda_process_instance_id", length = 100)
    private String camundaProcessInstanceId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
