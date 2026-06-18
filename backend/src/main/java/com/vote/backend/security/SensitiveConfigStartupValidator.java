package com.vote.backend.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class SensitiveConfigStartupValidator {

  static final String SENSITIVE_CONFIG_ERR_CODE = "S-1001";
  static final String DEV_TOKEN_ERR_CODE = "S-1002";
  static final String BOOTSTRAP_ADMIN_ERR_CODE = "S-1003";

  private static final String INSECURE_DB_PASSWORD = "123456";
  private static final String INSECURE_JWT_SECRET = "vote-application-super-secret-key-for-jwt-authentication-needs-to-be-long-enough";

  private final Environment environment;

  @Value("${spring.datasource.password:}")
  private String datasourcePassword;

  @Value("${jwt.secret:}")
  private String jwtSecret;

  @Value("${app.security.enforce-sensitive-config:true}")
  private boolean enforceSensitiveConfig;

  @Value("${app.security.dev-token-enabled:false}")
  private boolean devTokenEnabled;

  @Value("${app.bootstrap-admin.enabled:false}")
  private boolean bootstrapAdminEnabled;

  @Value("${app.bootstrap-admin.username:}")
  private String bootstrapAdminUsername;

  @Value("${app.bootstrap-admin.email:}")
  private String bootstrapAdminEmail;

  @Value("${app.bootstrap-admin.password:}")
  private String bootstrapAdminPassword;

  public SensitiveConfigStartupValidator(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  public void validate() {
    if (!enforceSensitiveConfig) {
      return;
    }

    boolean isDevOrLocal = environment.acceptsProfiles(Profiles.of("dev", "local"));
    boolean isTest = environment.acceptsProfiles(Profiles.of("test"));

    if (!isDevOrLocal && devTokenEnabled) {
      throw new IllegalStateException(DEV_TOKEN_ERR_CODE + ": 非 dev/local 环境禁止启用 dev-token");
    }

    if (bootstrapAdminEnabled && (isBlank(bootstrapAdminUsername)
        || isBlank(bootstrapAdminEmail)
        || isBlank(bootstrapAdminPassword))) {
      throw new IllegalStateException(
          BOOTSTRAP_ADMIN_ERR_CODE + ": 启用 bootstrap-admin 时必须同时提供用户名、邮箱和密码");
    }

    if (isDevOrLocal || isTest) {
      return;
    }

    if (isBlank(datasourcePassword) || INSECURE_DB_PASSWORD.equals(datasourcePassword.trim())) {
      throw new IllegalStateException(SENSITIVE_CONFIG_ERR_CODE + ": DB_PASSWORD 未配置或使用了不安全默认值");
    }

    if (isBlank(jwtSecret) || INSECURE_JWT_SECRET.equals(jwtSecret.trim())) {
      throw new IllegalStateException(SENSITIVE_CONFIG_ERR_CODE + ": JWT_SECRET 未配置或使用了不安全默认值");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
