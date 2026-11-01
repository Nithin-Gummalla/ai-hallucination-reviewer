package com.reviewer.repository;

import com.reviewer.model.FeedbackRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRecordRepository extends JpaRepository<FeedbackRecord, Long> {
    List<FeedbackRecord> findByFindingId(Long findingId);
}
