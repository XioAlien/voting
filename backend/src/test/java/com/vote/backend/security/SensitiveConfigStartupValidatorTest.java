package com.vote.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SensitiveConfigStartupValidatorTest {

  @Test
  void validate_ShouldThrowS1001_WhenSensitiveValueBlankInProd() throws Exception {
    Environment environment = mock(Environment.class);
    when(environment.acceptsProfiles(Profiles.of("dev", "local"))).thenReturn(false);
    when(environment.acceptsProfiles(Profiles.of("test"))).thenReturn(false);

    SensitiveConfigStartupValidator validator = new SensitiveConfigStartupValidator(environment);
    setField(validator, "enforceSensitiveConfig", true);
    setField(validator, "devTokenEnabled", false);
    setField(validator, "datasourcePassword", "");
    setField(validator, "jwtSecret", "secure-secret-with-32-characters-123456");

    IllegalStateException exception = assertThrows(IllegalStateException.class, validator::validate);
    assertTrue(exception.getMessage().contains(SensitiveConfigStartupValidator.SENSITIVE_CONFIG_ERR_CODE));
  }

  @Test
  void validate_ShouldThrowS1002_WhenDevTokenEnabledOutsideDevOrLocal() throws Exception {
    Environment environment = mock(Environment.class);
    when(environment.acceptsProfiles(Profiles.of("dev", "local"))).thenReturn(false);
    when(environment.acceptsProfiles(Profiles.of("test"))).thenReturn(false);

    SensitiveConfigStartupValidator validator = new SensitiveConfigStartupValidator(environment);
    setField(validator, "enforceSensitiveConfig", true);
    setField(validator, "devTokenEnabled", true);
    setField(validator, "datasourcePassword", "secure-db-pass");
    setField(validator, "jwtSecret", "secure-secret-with-32-characters-123456");

    IllegalStateException exception = assertThrows(IllegalStateException.class, validator::validate);
    assertTrue(exception.getMessage().contains(SensitiveConfigStartupValidator.DEV_TOKEN_ERR_CODE));
  }

  @Test
  void validate_ShouldThrowS1003_WhenBootstrapAdminEnabledWithoutRequiredFields() throws Exception {
    Environment environment = mock(Environment.class);
    when(environment.acceptsProfiles(Profiles.of("dev", "local"))).thenReturn(true);
    when(environment.acceptsProfiles(Profiles.of("test"))).thenReturn(false);

    SensitiveConfigStartupValidator validator = new SensitiveConfigStartupValidator(environment);
    setField(validator, "enforceSensitiveConfig", true);
    setField(validator, "devTokenEnabled", false);
    setField(validator, "bootstrapAdminEnabled", true);
    setField(validator, "bootstrapAdminUsername", "admin");
    setField(validator, "bootstrapAdminEmail", "");
    setField(validator, "bootstrapAdminPassword", "password");

    IllegalStateException exception = assertThrows(IllegalStateException.class, validator::validate);
    assertTrue(exception.getMessage().contains(SensitiveConfigStartupValidator.BOOTSTRAP_ADMIN_ERR_CODE));
  }

  @Test
  void validate_ShouldPass_WhenCheckDisabled() throws Exception {
    Environment environment = mock(Environment.class);

    SensitiveConfigStartupValidator validator = new SensitiveConfigStartupValidator(environment);
    setField(validator, "enforceSensitiveConfig", false);
    setField(validator, "devTokenEnabled", false);
    setField(validator, "datasourcePassword", "");
    setField(validator, "jwtSecret", "");

    assertDoesNotThrow(validator::validate);
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
