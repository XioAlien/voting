package com.vote.backend.controller;

import com.vote.backend.dto.ApiResponse;
import com.vote.backend.dto.AuthResponse;
import com.vote.backend.dto.LoginRequest;
import com.vote.backend.dto.RegisterRequest;
import com.vote.backend.entity.User;
import com.vote.backend.repository.UserRepository;
import com.vote.backend.security.JwtService;
import com.vote.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  @PostMapping("/register")
  public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    if (Boolean.TRUE.equals(userRepository.existsByUsername(request.getUsername().trim()))) {
      throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
    }
    if (Boolean.TRUE.equals(userRepository.existsByEmail(request.getEmail().trim()))) {
      throw new ApiException(HttpStatus.CONFLICT, "邮箱已存在");
    }

    User user = new User();
    user.setUsername(request.getUsername().trim());
    user.setEmail(request.getEmail().trim());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole("USER");
    User saved = userRepository.save(user);

    UserPrincipal principal = UserPrincipal.from(saved);
    String token = jwtService.generateToken(principal);
    return ApiResponse.success("注册成功", new AuthResponse(token, saved.getRole(), false));
  }

  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    String key = request.getUsernameOrEmail().trim();
    User user = userRepository.findByUsername(key)
        .or(() -> userRepository.findByEmail(key))
        .orElse(null);
    if (user == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "用户名或密码错误");
    }
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "用户名或密码错误");
    }

    user.setLastLoginAt(LocalDateTime.now());
    userRepository.save(user);
    UserPrincipal principal = UserPrincipal.from(user);
    String token = jwtService.generateToken(principal);
    return ApiResponse.success(
        "登录成功",
        new AuthResponse(token, user.getRole(), Boolean.TRUE.equals(user.getMustChangePassword())));
  }
}
