package com.reviewer.repository;

import com.reviewer.model.FindingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FindingRecordRepository extends JpaRepository<FindingRecord, Long> {
    List<FindingRecord> findByScanRecordId(Long scanRecordId);
}
