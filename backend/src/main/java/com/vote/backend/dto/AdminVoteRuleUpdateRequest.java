package com.vote.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminVoteRuleUpdateRequest {
  private String title;
  private String description;
  private String type;
  private Integer minChoices;
  private Integer maxChoices;
  private Boolean forceAllOptions;
  private Boolean allowCustomOptions;
  private LocalDateTime endTime;
  private String confirmToken;
}
