package com.vote.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vote_deletion_logs")
public class VoteDeletionLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vote_id", nullable = false)
  private Long voteId;

  @Column(name = "vote_title", nullable = false, length = 200)
  private String voteTitle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "operator_id", nullable = false)
  private User operator;

  @Column(name = "reason", length = 255)
  private String reason;

  @CreationTimestamp
  @Column(name = "deleted_at", updatable = false)
  private LocalDateTime deletedAt;
}
