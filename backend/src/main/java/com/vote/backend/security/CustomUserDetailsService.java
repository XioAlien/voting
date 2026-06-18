package com.vote.backend.security;

import com.vote.backend.entity.User;
import com.vote.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  @Autowired
  private UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(usernameOrEmail)
        .or(() -> userRepository.findByEmail(usernameOrEmail))
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return UserPrincipal.from(user);
  }
}

