package com.vote.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoteInviteDto {
  private Long voteId;
  private String accessType;
  private Boolean enabled;
  private Integer codeVersion;
  private String code;
  private String codeMasked;
  private LocalDateTime expiresAt;
  private Integer maxMembers;
  private Integer activeMembers;
  private Boolean canViewPlainCode;
}
