package com.vote.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vote_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_vote_option", columnNames = { "user_id", "vote_id", "option_id" })
})
public class VoteRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vote_id", nullable = false)
  private Vote vote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "option_id", nullable = false)
  private VoteOption option;

  @Column(name = "score")
  private Integer score; // null for choice, value for slider

  @CreationTimestamp
  @Column(name = "voted_at", updatable = false)
  private LocalDateTime votedAt;
}