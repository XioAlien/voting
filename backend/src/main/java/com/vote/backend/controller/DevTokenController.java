package com.vote.backend.controller;

import com.vote.backend.dto.ApiResponse;
import com.vote.backend.dto.AuthResponse;
import com.vote.backend.entity.User;
import com.vote.backend.repository.UserRepository;
import com.vote.backend.security.JwtService;
import com.vote.backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class DevTokenController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final boolean devTokenEnabled;

  public DevTokenController(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      @Value("${app.security.dev-token-enabled:false}") boolean devTokenEnabled) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.devTokenEnabled = devTokenEnabled;
  }

  @GetMapping("/dev-token")
  public ApiResponse<AuthResponse> issueDevTokenByQuery(
      @RequestParam(required = false) String identity,
      @RequestParam(required = false) String role) {
    return issueDevToken(identity, role);
  }

  @PostMapping("/dev-token")
  public ApiResponse<AuthResponse> issueDevTokenByBody(
      @RequestParam(required = false) String identity,
      @RequestParam(required = false) String role,
      @RequestBody(required = false) Map<String, String> requestBody) {
    String resolvedIdentity = firstNonBlank(
        identity,
        getValue(requestBody, "identity"),
        getValue(requestBody, "username"),
        getValue(requestBody, "email"));
    String resolvedRole = firstNonBlank(role, getValue(requestBody, "role"));
    return issueDevToken(resolvedIdentity, resolvedRole);
  }

  private ApiResponse<AuthResponse> issueDevToken(String identity, String role) {
    if (!devTokenEnabled) {
      throw new ApiException(HttpStatus.FORBIDDEN, "dev-token 功能未启用");
    }

    User user = resolveOrCreateUser(identity, role);
    String token = jwtService.generateToken(UserPrincipal.from(user));
    return ApiResponse.success("获取成功", new AuthResponse(token, user.getRole(), false));
  }

  private User resolveOrCreateUser(String identity, String role) {
    String normalizedRole = normalizeRole(role);
    String resolvedIdentity = StringUtils.hasText(identity) ? identity.trim() : defaultIdentityForRole(normalizedRole);

    return userRepository.findByUsernameOrEmail(resolvedIdentity)
        .map(existing -> updateRoleIfNecessary(existing, normalizedRole))
        .orElseGet(() -> userRepository.save(buildUser(resolvedIdentity, normalizedRole)));
  }

  private User updateRoleIfNecessary(User existing, String normalizedRole) {
    if (normalizedRole.equalsIgnoreCase(existing.getRole())) {
      return existing;
    }
    existing.setRole(normalizedRole);
    return userRepository.save(existing);
  }

  private User buildUser(String identity, String normalizedRole) {
    String username = identity.contains("@") ? identity.substring(0, identity.indexOf('@')) : identity;
    username = sanitizeUsername(username);
    String email = identity.contains("@") ? identity : username + "@local.dev";

    if (Boolean.TRUE.equals(userRepository.existsByUsername(username))
        || Boolean.TRUE.equals(userRepository.existsByEmail(email))) {
      throw new ApiException(HttpStatus.CONFLICT, "dev-token 目标用户已存在，请改用已存在身份获取 token");
    }

    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setRole(normalizedRole);
    user.setPasswordHash(passwordEncoder.encode("dev-token-" + UUID.randomUUID()));
    user.setIsBuiltinAdmin(false);
    user.setMustChangePassword(false);
    return user;
  }

  private String normalizeRole(String role) {
    String normalized = StringUtils.hasText(role) ? role.trim().toUpperCase(Locale.ROOT) : "ADMIN";
    if (!"ADMIN".equals(normalized) && !"USER".equals(normalized)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "角色不合法");
    }
    return normalized;
  }

  private String defaultIdentityForRole(String normalizedRole) {
    return "ADMIN".equals(normalizedRole) ? "dev-admin" : "dev-user";
  }

  private String sanitizeUsername(String username) {
    String sanitized = username == null ? ""
        : username.trim()
            .replaceAll("[^A-Za-z0-9._-]", "-")
            .replaceAll("-{2,}", "-");
    if (!StringUtils.hasText(sanitized)) {
      sanitized = "dev-user";
    }
    return sanitized.length() > 50 ? sanitized.substring(0, 50) : sanitized;
  }

  private String getValue(Map<String, String> requestBody, String key) {
    return requestBody == null ? null : requestBody.get(key);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }
}
