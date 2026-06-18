package com.vote.backend.repository;

import com.vote.backend.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {
  boolean existsByUserIdAndVoteId(Long userId, Long voteId);

  List<VoteRecord> findByVoteId(Long voteId);

  int countByOptionId(Long optionId);

  @Query("SELECT AVG(r.score) FROM VoteRecord r WHERE r.option.id = :optionId")
  Optional<Double> findAverageScoreByOptionId(Long optionId);
}