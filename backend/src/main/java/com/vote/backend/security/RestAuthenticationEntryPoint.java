package com.vote.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vote.backend.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
      throws IOException, ServletException {
    if (response.isCommitted()) {
      return;
    }

    Object failureReason = request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_REASON_ATTR);
    String message = "未登录";
    if (JwtAuthenticationFilter.AUTH_FAILURE_TOKEN_EXPIRED.equals(failureReason)) {
      message = "登录状态已过期，请重新登录";
    } else if (JwtAuthenticationFilter.AUTH_FAILURE_TOKEN_INVALID.equals(failureReason)) {
      message = "登录状态无效，请重新登录";
    }

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
  }
}
