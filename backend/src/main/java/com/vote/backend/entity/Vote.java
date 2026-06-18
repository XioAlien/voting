package com.vote.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "votes")
public class Vote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id", nullable = false)
  private User creator;

  @Column(name = "start_time")
  private LocalDateTime startTime;

  @Column(name = "end_time")
  private LocalDateTime endTime;

  @Column(name = "vote_type")
  private String type = "CHOICE"; // "CHOICE" or "SLIDER"

  @Column(name = "min_choices")
  private Integer minChoices = 1;

  @Column(name = "max_choices")
  private Integer maxChoices = 1;

  @Column(name = "force_all_options")
  private Boolean forceAllOptions = false;

  @Column(name = "allow_custom_options")
  private Boolean allowCustomOptions = false;

  @Column(name = "is_active")
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
}