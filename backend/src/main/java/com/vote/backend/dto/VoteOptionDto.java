package com.vote.backend.dto;

import lombok.Data;

@Data
public class VoteOptionDto {
  private Long id;
  private String text;
  private int votes;
  private Integer maxScore;
  private Double averageScore;
}
