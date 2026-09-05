package com.example.camunda.repository;

import com.example.camunda.entity.CaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseRepository extends JpaRepository<CaseRecord, Long> {

    boolean existsByCaseNumber(String caseNumber);
}
