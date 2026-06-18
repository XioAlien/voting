package com.vote.backend.dto;

import lombok.Data;

@Data
public class AdminTerminateVoteRequest {
  private String confirmToken;
}
