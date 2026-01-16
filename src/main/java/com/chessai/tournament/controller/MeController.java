package com.chessai.tournament.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for OAuth2 user information
 * 
 * This controller provides endpoints to retrieve information about the
 * currently authenticated OAuth2 user (GitHub user in our case).
 * 
 * Useful for:
 * - Testing OAuth2 authentication flow
 * - Getting user profile information
 * - Debugging authentication issues
 * 
 * @author Tournament Service Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/me")
@Tag(name = "User Profile", description = "OAuth2 user profile endpoints")
@Slf4j
public class MeController {

    /**
     * Get current authenticated user information
     * 
     * Returns the OAuth2 user attributes from GitHub, including:
     * - User ID
     * - Username (login)
     * - Display name
     * - Email
     * - Avatar URL
     * - Profile URL
     * 
     * @param user OAuth2User principal (automatically injected by Spring Security)
     * @return Map containing user attributes and metadata
     */
    @GetMapping
    @Operation(
        summary = "Get current user profile",
        description = "Returns the profile information of the currently authenticated GitHub user",
        security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
        responseCode = "200",
        description = "User profile retrieved successfully"
    )
    @ApiResponse(
        responseCode = "401",
        description = "User not authenticated - redirect to /oauth2/authorization/github"
    )
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OAuth2User user) {
        log.info("Getting profile for OAuth2 user: {}", user.getName());
        
        Map<String, Object> response = new HashMap<>();
        
        // Add user attributes from GitHub
        response.put("user", user.getAttributes());
        
        // Add metadata
        response.put("authenticated", true);
        response.put("provider", "github");
        response.put("authorities", user.getAuthorities());
        
        // Extract common fields for convenience
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getAttribute("id"));
        profile.put("username", user.getAttribute("login"));
        profile.put("name", user.getAttribute("name"));
        profile.put("email", user.getAttribute("email"));
        profile.put("avatarUrl", user.getAttribute("avatar_url"));
        profile.put("profileUrl", user.getAttribute("html_url"));
        
        response.put("profile", profile);
        
        return response;
    }

    /**
     * Get user authorities and roles
     * 
     * Returns the Spring Security authorities granted to the current user.
     * In OAuth2 context, this typically includes ROLE_USER and OAuth2-specific authorities.
     * 
     * @param user OAuth2User principal
     * @return Map containing authorities information
     */
    @GetMapping("/authorities")
    @Operation(
        summary = "Get user authorities",
        description = "Returns the Spring Security authorities for the current user",
        security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(responseCode = "200", description = "Authorities retrieved successfully")
    public Map<String, Object> getUserAuthorities(@AuthenticationPrincipal OAuth2User user) {
        log.info("Getting authorities for user: {}", user.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getAttribute("login"));
        response.put("authorities", user.getAuthorities());
        response.put("principalName", user.getName());
        
        return response;
    }
}