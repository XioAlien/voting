package com.vote.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
  private String token;
  private String role;
  private Boolean requirePasswordChange;

  public AuthResponse(String token) {
    this(token, null, false);
  }
}
