package com.vote.backend.dto;

import lombok.Data;

@Data
public class AdminUserStatusUpdateRequest {
  private Boolean disabled;
  private String confirmToken;
}
