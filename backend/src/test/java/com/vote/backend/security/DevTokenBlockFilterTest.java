package com.vote.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DevTokenBlockFilterTest {

  @Test
  void doFilterInternal_ShouldBlockDevTokenOutsideDevOrLocal() throws Exception {
    Environment environment = mock(Environment.class);
    when(environment.acceptsProfiles(Profiles.of("dev", "local"))).thenReturn(false);
    DevTokenBlockFilter filter = new DevTokenBlockFilter(environment, new ObjectMapper());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", DevTokenBlockFilter.DEV_TOKEN_PATH);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {
      throw new AssertionError("blocked request should not reach downstream filter chain");
    });

    assertEquals(403, response.getStatus());
    assertTrue(response.getContentAsString().contains("\"message\":\"dev-token 接口仅允许在 dev/local 环境访问\""));
  }

  @Test
  void doFilterInternal_ShouldPassThroughInDevProfile() throws Exception {
    Environment environment = mock(Environment.class);
    when(environment.acceptsProfiles(Profiles.of("dev", "local"))).thenReturn(true);
    DevTokenBlockFilter filter = new DevTokenBlockFilter(environment, new ObjectMapper());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", DevTokenBlockFilter.DEV_TOKEN_PATH);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> response.addHeader("X-Chain", "passed"));

    assertEquals("passed", response.getHeader("X-Chain"));
  }
}
