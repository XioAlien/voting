package com.vote.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VoteDto {
  private Long id;
  private String title;
  private String description;
  private String status; // active, closed
  private String type; // CHOICE, SLIDER
  private int participants;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}
