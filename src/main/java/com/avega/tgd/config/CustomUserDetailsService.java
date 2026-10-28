package com.avega.tgd.config;

import com.avega.tgd.domain.entity.UserEntity;
import com.avega.tgd.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom {@link UserDetailsService} bridging Spring Security's authentication provider
 * with the application's database-backed user management.
 *
 * <p>Fulfills the technical requirement:
 * <i>"Spring Security (authentication & authorization using a User table with prefilled data in DB)"</i>.
 *
 * <p>When an incoming HTTP request presents Basic Authentication credentials:
 * <ol>
 *   <li>Spring Security invokes {@link #loadUserByUsername(String)}.</li>
 *   <li>The service queries {@link UserRepository} for the user record.</li>
 *   <li>The BCrypt hashed password and granted role authorities (e.g. {@code ROLE_AUDITOR})
 *       are wrapped into a Spring Security {@link UserDetails} principal.</li>
 *   <li>The authentication manager validates the provided password against the hash.</li>
 * </ol>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by their unique username from the database.
     *
     * @param username the username to authenticate
     * @return a populated {@link UserDetails} object containing credentials and granted authorities
     * @throws UsernameNotFoundException if no user exists with the requested username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
