package com.vote.backend.dto;

import lombok.Data;

@Data
public class AdminUserRoleUpdateRequest {
  private String role;
  private String confirmToken;
}
