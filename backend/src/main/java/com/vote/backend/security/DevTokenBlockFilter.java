package com.vote.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vote.backend.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DevTokenBlockFilter extends OncePerRequestFilter {

  static final String DEV_TOKEN_PATH = "/api/auth/dev-token";

  private final Environment environment;
  private final ObjectMapper objectMapper;

  public DevTokenBlockFilter(Environment environment, ObjectMapper objectMapper) {
    this.environment = environment;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path == null || !path.startsWith(DEV_TOKEN_PATH);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    boolean isDevOrLocal = environment.acceptsProfiles(Profiles.of("dev", "local"));
    if (!isDevOrLocal) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json;charset=UTF-8");
      objectMapper.writeValue(response.getWriter(), ApiResponse.error("dev-token 接口仅允许在 dev/local 环境访问"));
      return;
    }
    filterChain.doFilter(request, response);
  }
}
