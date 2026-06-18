package com.vote.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vote_invites")
public class VoteInvite {

  @Id
  @Column(name = "vote_id")
  private Long voteId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "vote_id")
  private Vote vote;

  @Column(name = "is_enabled", nullable = false)
  private Boolean enabled = false;

  @Column(name = "code_version", nullable = false)
  private Integer codeVersion = 1;

  @Column(name = "code_hash", nullable = false, unique = true, length = 64)
  private String codeHash;

  @Column(name = "code_ciphertext", columnDefinition = "TEXT")
  private String codeCiphertext;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "max_members", nullable = false)
  private Integer maxMembers = 100;

  @Column(name = "reset_at")
  private LocalDateTime resetAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reset_by")
  private User resetBy;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}

