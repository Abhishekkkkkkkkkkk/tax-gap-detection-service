package com.avega.tgd.controller;

import com.avega.tgd.dto.response.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for inspecting the current authenticated user session and authorities.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for inspecting authenticated session details")
public class AuthController {

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info and roles")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(ApiResponseDto.ok("Anonymous", Map.of("authenticated", false)));
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", authentication.getName());
        userInfo.put("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        userInfo.put("authenticated", true);

        return ResponseEntity.ok(ApiResponseDto.ok(userInfo));
    }
}
