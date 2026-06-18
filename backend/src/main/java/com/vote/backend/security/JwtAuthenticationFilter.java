package com.vote.backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String AUTH_FAILURE_REASON_ATTR = "authFailureReason";
  public static final String AUTH_FAILURE_TOKEN_EXPIRED = "token_expired";
  public static final String AUTH_FAILURE_TOKEN_INVALID = "token_invalid";

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  @Autowired
  private JwtService jwtService;

  @Autowired
  private CustomUserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);
    if (token.isBlank()) {
      request.setAttribute(AUTH_FAILURE_REASON_ATTR, AUTH_FAILURE_TOKEN_INVALID);
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String username = jwtService.extractUsername(token);
      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserPrincipal user = (UserPrincipal) userDetailsService.loadUserByUsername(username);
        if (jwtService.isTokenValid(token, user)) {
          request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, AuthenticatedUser.from(user));
          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              user, null, user.getAuthorities());
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
          request.setAttribute(AUTH_FAILURE_REASON_ATTR, AUTH_FAILURE_TOKEN_INVALID);
        }
      }
    } catch (ExpiredJwtException ex) {
      SecurityContextHolder.clearContext();
      request.removeAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE);
      request.setAttribute(AUTH_FAILURE_REASON_ATTR, AUTH_FAILURE_TOKEN_EXPIRED);
      log.debug("JWT expired: {}", ex.getMessage());
    } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
      SecurityContextHolder.clearContext();
      request.removeAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE);
      request.setAttribute(AUTH_FAILURE_REASON_ATTR, AUTH_FAILURE_TOKEN_INVALID);
      log.debug("JWT invalid: {}", ex.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}
