package com.vote.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoteDetailDto {
  private Long id;
  private String title;
  private String description;
  private String status;
  private String type; // CHOICE, SLIDER
  private Integer minChoices;
  private Integer maxChoices;
  private Boolean forceAllOptions;
  private Boolean allowCustomOptions;
  private int participants;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  private List<VoteOptionDto> options;
  private int totalVotes;
  private boolean hasVoted;
}
