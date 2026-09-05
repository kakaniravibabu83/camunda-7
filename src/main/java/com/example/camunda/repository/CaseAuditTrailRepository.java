package com.example.camunda.repository;

import com.example.camunda.entity.CaseAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseAuditTrailRepository extends JpaRepository<CaseAuditTrail, Long> {

    List<CaseAuditTrail> findAllByOrderByCreatedAtDescIdDesc();

    List<CaseAuditTrail> findByCaseIdOrderByCreatedAtAscIdAsc(Long caseId);
}
