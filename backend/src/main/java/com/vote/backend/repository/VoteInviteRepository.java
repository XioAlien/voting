package com.vote.backend.repository;

import com.vote.backend.entity.VoteInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteInviteRepository extends JpaRepository<VoteInvite, Long> {
  Optional<VoteInvite> findByCodeHash(String codeHash);
}

