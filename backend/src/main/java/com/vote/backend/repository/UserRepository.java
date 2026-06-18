package com.vote.backend.repository;

import com.vote.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  @Query("SELECT u FROM User u WHERE u.username = :identity OR u.email = :identity")
  Optional<User> findByUsernameOrEmail(@Param("identity") String identity);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);
}
