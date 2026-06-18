package com.vote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
  @NotBlank(message = "用户名/邮箱不能为空")
  private String usernameOrEmail;

  @NotBlank(message = "密码不能为空")
  private String password;
}
