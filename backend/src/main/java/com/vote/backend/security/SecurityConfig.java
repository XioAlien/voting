package com.vote.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final DevTokenBlockFilter devTokenBlockFilter;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RestAuthenticationEntryPoint authenticationEntryPoint;
  private final RestAccessDeniedHandler accessDeniedHandler;
  private final Environment environment;

  public SecurityConfig(
      DevTokenBlockFilter devTokenBlockFilter,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler,
      Environment environment) {
    this.devTokenBlockFilter = devTokenBlockFilter;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
    this.environment = environment;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(devTokenBlockFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(jwtAuthenticationFilter, DevTokenBlockFilter.class)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(new AntPathRequestMatcher("/api/auth/login")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/auth/register")).permitAll()
            .requestMatchers(new AntPathRequestMatcher(DevTokenBlockFilter.DEV_TOKEN_PATH)).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/admin/**")).hasRole("ADMIN")
            .requestMatchers(new AntPathRequestMatcher("/error")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/**", "OPTIONS")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/votes/**", "GET")).permitAll()
            .anyRequest().authenticated());
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    String rawOrigins = environment.getProperty("app.cors.allowed-origins",
        "http://localhost:5173,http://127.0.0.1:5173");
    List<String> origins = Arrays.stream(rawOrigins.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("Authorization"));
    config.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
