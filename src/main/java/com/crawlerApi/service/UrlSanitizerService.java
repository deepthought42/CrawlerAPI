package com.crawlerApi.service;

import java.net.URI;
import java.net.URL;

import org.springframework.stereotype.Service;

import com.looksee.utils.BrowserUtils;

/**
 * Service for sanitizing URLs.
 * This wrapper allows BrowserUtils to be mockable in tests.
 */
@Service
public class UrlSanitizerService {
    
    /**
     * Sanitize a user-provided URL string
     * @param url The URL string to sanitize
     * @return The sanitized URL
     * @throws Exception If URL sanitization fails
     */
    public URL sanitizeUrl(String url) throws Exception {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        String lowercaseUrl = url.toLowerCase();
        String sanitizedUrlString = BrowserUtils.sanitizeUserUrl(lowercaseUrl);
        return new URI(sanitizedUrlString).toURL();
    }
}

