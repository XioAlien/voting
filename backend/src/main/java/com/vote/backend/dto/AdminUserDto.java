package com.vote.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserDto {
  private Long id;
  private String username;
  private String email;
  private String role;
  private Boolean builtinAdmin;
  private Boolean disabled;
  private Boolean deleted;
  private Boolean mustChangePassword;
  private LocalDateTime lastLoginAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
