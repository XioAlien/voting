package com.vote.backend.security;

import com.vote.backend.entity.User;
import com.vote.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final boolean bootstrapAdminEnabled;
  private final String bootstrapAdminUsername;
  private final String bootstrapAdminEmail;
  private final String bootstrapAdminPassword;
  private final boolean bootstrapAdminMustChangePassword;

  public BootstrapAdminInitializer(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.bootstrap-admin.enabled:false}") boolean bootstrapAdminEnabled,
      @Value("${app.bootstrap-admin.username:}") String bootstrapAdminUsername,
      @Value("${app.bootstrap-admin.email:}") String bootstrapAdminEmail,
      @Value("${app.bootstrap-admin.password:}") String bootstrapAdminPassword,
      @Value("${app.bootstrap-admin.must-change-password:true}") boolean bootstrapAdminMustChangePassword
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.bootstrapAdminEnabled = bootstrapAdminEnabled;
    this.bootstrapAdminUsername = bootstrapAdminUsername;
    this.bootstrapAdminEmail = bootstrapAdminEmail;
    this.bootstrapAdminPassword = bootstrapAdminPassword;
    this.bootstrapAdminMustChangePassword = bootstrapAdminMustChangePassword;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!bootstrapAdminEnabled) {
      return;
    }

    String username = requireText(bootstrapAdminUsername, "bootstrap admin username");
    String email = requireText(bootstrapAdminEmail, "bootstrap admin email");
    String rawPassword = requireText(bootstrapAdminPassword, "bootstrap admin password");

    Optional<User> usernameMatch = userRepository.findByUsername(username);
    Optional<User> emailMatch = userRepository.findByEmail(email);
    if (usernameMatch.isPresent() && emailMatch.isPresent()
        && !Objects.equals(usernameMatch.get().getId(), emailMatch.get().getId())) {
      throw new IllegalStateException(
          SensitiveConfigStartupValidator.BOOTSTRAP_ADMIN_ERR_CODE
              + ": bootstrap-admin 用户名和邮箱分别被不同账号占用，无法自动初始化");
    }

    User target = usernameMatch.orElseGet(() -> emailMatch.orElse(null));
    boolean created = false;
    if (target == null) {
      target = new User();
      target.setUsername(username);
      target.setEmail(email);
      created = true;
    }

    boolean changed = created;
    if (!username.equals(target.getUsername())) {
      target.setUsername(username);
      changed = true;
    }
    if (!email.equals(target.getEmail())) {
      target.setEmail(email);
      changed = true;
    }
    if (!"ADMIN".equalsIgnoreCase(target.getRole())) {
      target.setRole("ADMIN");
      changed = true;
    }
    if (!Boolean.TRUE.equals(target.getIsBuiltinAdmin())) {
      target.setIsBuiltinAdmin(true);
      changed = true;
    }
    if (!Objects.equals(target.getMustChangePassword(), bootstrapAdminMustChangePassword)) {
      target.setMustChangePassword(bootstrapAdminMustChangePassword);
      changed = true;
    }
    if (!StringUtils.hasText(target.getPasswordHash())
        || !passwordEncoder.matches(rawPassword, target.getPasswordHash())) {
      target.setPasswordHash(passwordEncoder.encode(rawPassword));
      changed = true;
    }

    if (changed) {
      userRepository.save(target);
      if (created) {
        log.info("Bootstrap admin created: username={}, email={}", username, email);
      } else {
        log.info("Bootstrap admin updated: username={}, email={}", username, email);
      }
    } else {
      log.info("Bootstrap admin already up-to-date: username={}, email={}", username, email);
    }
  }

  private String requireText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException(
          SensitiveConfigStartupValidator.BOOTSTRAP_ADMIN_ERR_CODE
              + ": missing " + fieldName);
    }
    return value.trim();
  }
}
