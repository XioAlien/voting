package com.vote.backend.dto;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {
  private String username;
  private String email;
}
