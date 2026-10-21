package com.intellishare.repository;

import com.intellishare.model.CriminalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CriminalRecordRepository extends JpaRepository<CriminalRecord, Long>, JpaSpecificationExecutor<CriminalRecord> {
    boolean existsByRecordNumber(String recordNumber);
}
