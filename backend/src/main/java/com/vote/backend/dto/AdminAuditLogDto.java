package com.vote.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAuditLogDto {
  private Long id;
  private Long operatorId;
  private String operatorRole;
  private String action;
  private String targetType;
  private String targetId;
  private String detail;
  private Boolean confirmed;
  private LocalDateTime createdAt;
}
