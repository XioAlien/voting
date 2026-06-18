package com.vote.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vote_memberships", uniqueConstraints = {
    @UniqueConstraint(name = "uk_vote_membership_vote_user", columnNames = { "vote_id", "user_id" })
})
public class VoteMembership {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vote_id", nullable = false)
  private Vote vote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "ACTIVE";

  @Column(name = "joined_at", nullable = false)
  private LocalDateTime joinedAt = LocalDateTime.now();

  @Column(name = "left_at")
  private LocalDateTime leftAt;

  @Column(name = "joined_code_version")
  private Integer joinedCodeVersion;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
}
