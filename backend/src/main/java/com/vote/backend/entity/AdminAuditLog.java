package com.vote.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "operator_id", nullable = false)
  private Long operatorId;

  @Column(name = "operator_role", nullable = false, length = 20)
  private String operatorRole;

  @Column(name = "action", nullable = false, length = 64)
  private String action;

  @Column(name = "target_type", nullable = false, length = 64)
  private String targetType;

  @Column(name = "target_id", length = 128)
  private String targetId;

  @Column(name = "detail", columnDefinition = "TEXT")
  private String detail;

  @Column(name = "confirmed", nullable = false)
  private Boolean confirmed = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
}
