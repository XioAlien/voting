package com.vote.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public record AuthenticatedUser(Long userId, String role) {

  public static final String REQUEST_ATTRIBUTE = AuthenticatedUser.class.getName();

  public static AuthenticatedUser from(UserPrincipal principal) {
    if (principal == null) {
      return null;
    }
    return new AuthenticatedUser(principal.getId(), extractRole(principal));
  }

  public static AuthenticatedUser from(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      return null;
    }
    return from(principal);
  }

  private static String extractRole(UserPrincipal principal) {
    return principal.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority != null && authority.startsWith("ROLE_"))
        .map(authority -> authority.substring("ROLE_".length()))
        .findFirst()
        .orElse("USER");
  }
}
