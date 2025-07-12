package com.crawlerApi.api;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crawlerApi.service.Auth0Service;
import com.looksee.models.Account;

/**
 * Controller for retrieving user information from Auth0
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {
    
    @Autowired
    private Auth0Service auth0Service;
    
    /**
     * Get current user's account information
     * @param principal The authenticated principal
     * @return Account information
     */
    @GetMapping("/account")
    public ResponseEntity<Account> getCurrentUserAccount(Principal principal) {
        Optional<Account> accountOpt = auth0Service.getCurrentUserAccount(principal);
        
        if (accountOpt.isPresent()) {
            return ResponseEntity.ok(accountOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get user information from Auth0
     * @param authorization The Authorization header containing the Bearer token
     * @return User information from Auth0
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @RequestHeader("Authorization") String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        
        String accessToken = authorization.substring(7); // Remove "Bearer " prefix
        Optional<Map<String, Object>> userInfoOpt = auth0Service.getUserInfo(accessToken);
        
        if (userInfoOpt.isPresent()) {
            return ResponseEntity.ok(userInfoOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get username from Auth0
     * @param authorization The Authorization header containing the Bearer token
     * @return Username
     */
    @GetMapping("/username")
    public ResponseEntity<String> getUsername(
            @RequestHeader("Authorization") String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        
        String accessToken = authorization.substring(7);
        Optional<String> usernameOpt = auth0Service.getUsername(accessToken);
        
        if (usernameOpt.isPresent()) {
            return ResponseEntity.ok(usernameOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get user email from Auth0
     * @param authorization The Authorization header containing the Bearer token
     * @return Email address
     */
    @GetMapping("/email")
    public ResponseEntity<String> getEmail(
            @RequestHeader("Authorization") String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        
        String accessToken = authorization.substring(7);
        Optional<String> emailOpt = auth0Service.getEmail(accessToken);
        
        if (emailOpt.isPresent()) {
            return ResponseEntity.ok(emailOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Check if Auth0 is properly configured
     * @return Configuration status
     */
    @GetMapping("/config/status")
    public ResponseEntity<Map<String, Object>> getConfigStatus() {
        boolean isConfigured = auth0Service.isConfigured();
        
        Map<String, Object> status = Map.of(
            "configured", isConfigured,
            "message", isConfigured ? "Auth0 is properly configured" : "Auth0 is not configured"
        );
        
        return ResponseEntity.ok(status);
    }
} 