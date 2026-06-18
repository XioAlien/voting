package com.vote.backend.repository;

import com.vote.backend.entity.VoteMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteMembershipRepository extends JpaRepository<VoteMembership, Long> {
  Optional<VoteMembership> findByVote_IdAndUser_Id(Long voteId, Long userId);

  boolean existsByVote_IdAndUser_IdAndStatus(Long voteId, Long userId, String status);

  long countByVote_IdAndStatus(Long voteId, String status);

  @Query("""
      SELECT COUNT(m)
        FROM VoteMembership m
       WHERE m.user.id = :userId
         AND m.status = 'ACTIVE'
      """)
  long countActiveMembershipsByUserId(@Param("userId") Long userId);
}
