package com.example.camunda.repository;

import com.example.camunda.entity.DeploymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeploymentLogRepository extends JpaRepository<DeploymentLog, Long> {
}
