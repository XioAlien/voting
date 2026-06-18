package com.vote.backend.dto;

import lombok.Data;

@Data
public class AdminConfirmRequest {
  private String action;
  private String target;
}
