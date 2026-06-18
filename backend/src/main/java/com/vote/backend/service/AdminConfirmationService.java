package com.vote.backend.service;

import com.vote.backend.controller.ApiException;
import com.vote.backend.dto.AdminConfirmRequest;
import com.vote.backend.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminConfirmationService {

  private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

  private final SecureRandom secureRandom = new SecureRandom();
  private final Map<String, PendingConfirmation> pendingConfirmations = new ConcurrentHashMap<>();

  public String issueToken(UserPrincipal principal, AdminConfirmRequest request) {
    requireAdmin(principal);
    String action = normalize(request == null ? null : request.getAction(), "确认动作不能为空");
    String target = normalize(request == null ? null : request.getTarget(), "确认目标不能为空");

    cleanupExpiredTokens();

    byte[] randomBytes = new byte[24];
    secureRandom.nextBytes(randomBytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    pendingConfirmations.put(token, new PendingConfirmation(principal.getId(), action, target,
        Instant.now().plus(TOKEN_TTL)));
    return token;
  }

  public void validateToken(UserPrincipal principal, String token, String expectedAction, String expectedTarget) {
    requireAdmin(principal);
    if (!StringUtils.hasText(token)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "确认令牌不能为空");
    }

    cleanupExpiredTokens();

    PendingConfirmation pending = pendingConfirmations.remove(token.trim());
    if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "确认令牌无效或已过期");
    }

    String action = normalize(expectedAction, "确认动作不能为空");
    String target = normalize(expectedTarget, "确认目标不能为空");
    if (!pending.matches(principal.getId(), action, target)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "确认令牌与当前操作不匹配");
    }
  }

  public boolean isAdmin(UserPrincipal principal) {
    return principal != null && principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
  }

  private void requireAdmin(UserPrincipal principal) {
    if (principal == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    if (!isAdmin(principal)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "无管理员权限");
    }
  }

  private String normalize(String value, String errorMessage) {
    if (!StringUtils.hasText(value)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
    }
    return value.trim().toUpperCase();
  }

  private void cleanupExpiredTokens() {
    Instant now = Instant.now();
    pendingConfirmations.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
  }

  private record PendingConfirmation(Long operatorId, String action, String target, Instant expiresAt) {
    private boolean matches(Long currentOperatorId, String currentAction, String currentTarget) {
      return operatorId.equals(currentOperatorId)
          && action.equals(currentAction)
          && target.equals(currentTarget);
    }
  }
}
