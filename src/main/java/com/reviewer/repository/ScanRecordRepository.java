package com.reviewer.repository;

import com.reviewer.model.ScanRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScanRecordRepository extends JpaRepository<ScanRecord, Long> {
}
