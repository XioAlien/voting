package com.vote.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoteInviteSettingsRequest {
  private String accessType;
  private Boolean enabled;
  private LocalDateTime expiresAt;
  private Integer maxMembers;
}
