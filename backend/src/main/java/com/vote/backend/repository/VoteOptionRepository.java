package com.vote.backend.repository;

import com.vote.backend.entity.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {
  List<VoteOption> findByVoteIdOrderBySortOrderAsc(Long voteId);
}