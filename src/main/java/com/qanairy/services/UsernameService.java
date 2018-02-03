package com.qanairy.services;

import com.qanairy.models.dto.DomainRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Demonstration of method level Role based authorization
 * Only an authenticated and authorized User with Admin
 * rights can access this resource.
 *
 * Also demonstrates how to retrieve the UserDetails object
 * representing the Authentication's principal from within
 * a service
 *
 */
@Service
public class UsernameService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UsernameService() {
        //this.auth0Client = auth0Client;
    }


   // @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String getUsername() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //final Auth0UserDetails principal = (Auth0UserDetails) authentication.getPrincipal();
        //logger.info("Current user accessed Admin secured resource: " + principal.getUsername());
        // we already have the username.. but for this sample lets call Auth0 service anyway..
        return "brandon.kindred@gmail.com"; //auth0Client.getUsername((Auth0JWTToken) authentication);
    }
}

