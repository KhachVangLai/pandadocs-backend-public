package com.pandadocs.api.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.pandadocs.api.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);

  Optional<User> findByResetPasswordToken(String token);

  Optional<User> findByVerificationToken(String token);

  long countByCreatedAtAfter(Instant startDate);

  // Eager load roles for pagination (avoid LazyInitializationException)
  @Query(value = "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles",
         countQuery = "SELECT COUNT(DISTINCT u) FROM User u")
  Page<User> findAllWithRoles(Pageable pageable);

  // Eager load roles for findById (avoid LazyInitializationException)
  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
  Optional<User> findByIdWithRoles(Long id);

  // Eager load roles for findByUsername (avoid LazyInitializationException)
  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
  Optional<User> findByUsernameWithRoles(@Param("username") String username);
}
