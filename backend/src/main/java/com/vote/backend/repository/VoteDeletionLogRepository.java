package com.vote.backend.repository;

import com.vote.backend.entity.VoteDeletionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteDeletionLogRepository extends JpaRepository<VoteDeletionLog, Long> {
}
