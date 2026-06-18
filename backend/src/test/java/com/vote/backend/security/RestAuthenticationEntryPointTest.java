package com.vote.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestAuthenticationEntryPointTest {

  private RestAuthenticationEntryPoint entryPoint;

  @BeforeEach
  void setUp() {
    entryPoint = new RestAuthenticationEntryPoint(new ObjectMapper());
  }

  @Test
  void commence_ShouldReturnDefaultUnauthorizedMessage() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new InsufficientAuthenticationException("missing"));

    assertEquals(401, response.getStatus());
    assertTrue(response.getContentAsString().contains("\"message\":\"未登录\""));
  }

  @Test
  void commence_ShouldReturnExpiredTokenMessage() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(JwtAuthenticationFilter.AUTH_FAILURE_REASON_ATTR,
        JwtAuthenticationFilter.AUTH_FAILURE_TOKEN_EXPIRED);
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new InsufficientAuthenticationException("expired"));

    assertEquals(401, response.getStatus());
    assertTrue(response.getContentAsString().contains("\"message\":\"登录状态已过期，请重新登录\""));
  }

  @Test
  void commence_ShouldReturnInvalidTokenMessage() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(JwtAuthenticationFilter.AUTH_FAILURE_REASON_ATTR,
        JwtAuthenticationFilter.AUTH_FAILURE_TOKEN_INVALID);
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new InsufficientAuthenticationException("invalid"));

    assertEquals(401, response.getStatus());
    assertTrue(response.getContentAsString().contains("\"message\":\"登录状态无效，请重新登录\""));
  }
}
