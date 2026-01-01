package com.crawlerApi.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SimpleCORSFilter extends OncePerRequestFilter {
	
	@Autowired
	private Environment environment;
	
	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) 
    		throws ServletException, IOException {
		
		// Only apply permissive CORS in non-production environments
		// In production, rely on SecurityConfig's CORS configuration
		if (!isProductionProfile()) {
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE, PATCH");
			response.setHeader("Access-Control-Max-Age", "3600");
			response.setHeader("Access-Control-Allow-Headers", "Authorization, Access-Control-Allow-Headers, Origin, Accept, X-Requested-With, " +
					"Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
		}
        chain.doFilter(request, response);
    }
	
	/**
	 * Check if the application is running in production profile
	 * @return true if production profile is active
	 */
	private boolean isProductionProfile() {
		if (environment == null) {
			return false;
		}
		String[] activeProfiles = environment.getActiveProfiles();
		for (String profile : activeProfiles) {
			if (profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production")) {
				return true;
			}
		}
		return false;
	}
}
