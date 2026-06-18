package com.vote.backend.security;

import com.vote.backend.entity.User;
import com.vote.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminInitializerTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Test
  void run_ShouldCreateAdmin_WhenUserDoesNotExist() throws Exception {
    BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
        userRepository,
        passwordEncoder,
        true,
        "admin",
        "admin@example.com",
        "Init@12345Aa!",
        true
    );
    when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("Init@12345Aa!")).thenReturn("encoded");

    initializer.run(new DefaultApplicationArguments(new String[0]));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertEquals("admin", saved.getUsername());
    assertEquals("admin@example.com", saved.getEmail());
    assertEquals("ADMIN", saved.getRole());
    assertTrue(saved.getIsBuiltinAdmin());
    assertTrue(saved.getMustChangePassword());
    assertEquals("encoded", saved.getPasswordHash());
  }

  @Test
  void run_ShouldUpdateAdmin_WhenExistingUserNeedsAlignment() throws Exception {
    User existing = new User();
    existing.setId(1L);
    existing.setUsername("admin");
    existing.setEmail("legacy@example.com");
    existing.setRole("USER");
    existing.setIsBuiltinAdmin(false);
    existing.setMustChangePassword(false);
    existing.setPasswordHash("old-hash");

    BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
        userRepository,
        passwordEncoder,
        true,
        "admin",
        "admin@example.com",
        "Init@12345Aa!",
        true
    );
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.matches("Init@12345Aa!", "old-hash")).thenReturn(false);
    when(passwordEncoder.encode("Init@12345Aa!")).thenReturn("new-hash");

    initializer.run(new DefaultApplicationArguments(new String[0]));

    verify(userRepository).save(existing);
    assertEquals("admin@example.com", existing.getEmail());
    assertEquals("ADMIN", existing.getRole());
    assertTrue(existing.getIsBuiltinAdmin());
    assertTrue(existing.getMustChangePassword());
    assertEquals("new-hash", existing.getPasswordHash());
  }

  @Test
  void run_ShouldSkipSave_WhenAdminAlreadyUpToDate() throws Exception {
    User existing = new User();
    existing.setId(1L);
    existing.setUsername("admin");
    existing.setEmail("admin@example.com");
    existing.setRole("ADMIN");
    existing.setIsBuiltinAdmin(true);
    existing.setMustChangePassword(true);
    existing.setPasswordHash("same-hash");

    BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
        userRepository,
        passwordEncoder,
        true,
        "admin",
        "admin@example.com",
        "Init@12345Aa!",
        true
    );
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));
    when(passwordEncoder.matches("Init@12345Aa!", "same-hash")).thenReturn(true);

    initializer.run(new DefaultApplicationArguments(new String[0]));

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void run_ShouldThrow_WhenUsernameAndEmailBelongToDifferentUsers() {
    User usernameUser = new User();
    usernameUser.setId(1L);
    usernameUser.setUsername("admin");

    User emailUser = new User();
    emailUser.setId(2L);
    emailUser.setEmail("admin@example.com");

    BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
        userRepository,
        passwordEncoder,
        true,
        "admin",
        "admin@example.com",
        "Init@12345Aa!",
        true
    );
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(usernameUser));
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(emailUser));

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> initializer.run(new DefaultApplicationArguments(new String[0])));

    assertTrue(exception.getMessage().contains(SensitiveConfigStartupValidator.BOOTSTRAP_ADMIN_ERR_CODE));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void run_ShouldDoNothing_WhenBootstrapDisabled() throws Exception {
    BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
        userRepository,
        passwordEncoder,
        false,
        "admin",
        "admin@example.com",
        "Init@12345Aa!",
        true
    );

    initializer.run(new DefaultApplicationArguments(new String[0]));

    verify(userRepository, never()).findByUsername(any());
    verify(userRepository, never()).save(any(User.class));
  }
}
