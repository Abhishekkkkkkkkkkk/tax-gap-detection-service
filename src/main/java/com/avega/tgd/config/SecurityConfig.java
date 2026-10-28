package com.avega.tgd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration establishing authentication and Role-Based Access Control (RBAC).
 *
 * <p>Technical Requirements Addressed:
 * <ul>
 *   <li><b>Database-Driven Authentication:</b> Delegates user credential verification to
 *       {@link CustomUserDetailsService}, which queries the prefilled {@code users} database table.</li>
 *   <li><b>Password Hashing:</b> Uses {@link BCryptPasswordEncoder} with salt rounds to prevent
 *       plaintext credential exposure.</li>
 *   <li><b>HTTP Basic Authentication:</b> Employs standard HTTP Basic Auth headers, enabling simple
 *       integration with automated CI/CD pipelines, curl commands, and Postman suites.</li>
 *   <li><b>Stateless Architecture:</b> Disables HTTP sessions ({@link SessionCreationPolicy#STATELESS})
 *       to eliminate server-side session affinity and support horizontal scaling.</li>
 *   <li><b>CSRF Protection:</b> Disabled because the API is stateless and does not utilize ambient
 *       cookie credentials, adhering to Spring Security REST API guidelines.</li>
 *   <li><b>Role-Based Access Control (RBAC):</b>
 *     <ul>
 *       <li>{@code ROLE_AUDITOR}: Ingest transactions, view tax gap analyses, search exceptions,
 *           access aggregation reports, and inspect audit trails.</li>
 *       <li>{@code ROLE_ADMIN}: Inherits all auditor permissions plus administrative authority to
 *           create, update JSON configurations, and toggle tax compliance rules.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Password encoder bean utilizing the BCrypt strong hashing algorithm.
     *
     * @return {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the {@link AuthenticationManager} bean configured with Spring's authentication pipeline.
     *
     * @param authConfig Spring authentication configuration
     * @return configured {@link AuthenticationManager}
     * @throws Exception if manager initialization fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Defines the HTTP security filter chain configuring endpoint authorization, basic auth,
     * session policy, and frame options.
     *
     * @param http the {@link HttpSecurity} builder
     * @return constructed {@link SecurityFilterChain}
     * @throws Exception if security filter configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless REST endpoints that do not rely on browser session cookies
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // Enforce stateless session management: no HTTP session is created or stored by Spring Security
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public OpenAPI/Swagger documentation, actuator health checks, and H2 dev console
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/h2-console/**"
                        ).permitAll()

                        // Rule management: Administrators can create/update/toggle rules; Auditors can only read
                        .requestMatchers(HttpMethod.GET, "/api/v1/rules/**").hasAnyAuthority("ROLE_AUDITOR", "ROLE_ADMIN")
                        .requestMatchers("/api/v1/rules/**").hasAuthority("ROLE_ADMIN")

                        // Transactions, Exceptions, Reports, and Audit Logs accessible by both AUDITOR and ADMIN
                        .requestMatchers("/api/v1/**").hasAnyAuthority("ROLE_AUDITOR", "ROLE_ADMIN")

                        // Any other unmapped requests require valid authentication
                        .anyRequest().authenticated()
                )
                // Use HTTP Basic Authentication
                .httpBasic(Customizer.withDefaults())
                // Disable frame options to allow H2 web console rendering in development/testing environments
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
