package com.avega.tgd.repository;

import com.avega.tgd.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing application user credentials and authorities ({@link UserEntity}).
 *
 * <p>Supports the security requirement:
 * <i>"Spring Security (authentication & authorization using a User table with prefilled data in DB)"</i>.
 * Credentials loaded from this repository are verified by {@link com.avega.tgd.config.CustomUserDetailsService}
 * for HTTP Basic Authentication.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Looks up an active user account by username.
     *
     * @param username the login username to authenticate
     * @return an {@link Optional} containing the user entity if found
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Checks if a user already exists with the given username.
     * Used by {@link com.avega.tgd.config.DatabaseDataInitializer} to prevent duplicate user seeding.
     *
     * @param username the username to check
     * @return true if the username exists, false otherwise
     */
    boolean existsByUsername(String username);
}
