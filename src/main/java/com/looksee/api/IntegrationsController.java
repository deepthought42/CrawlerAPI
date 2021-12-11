package com.looksee.api;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.looksee.models.Account;
import com.looksee.security.SecurityConfig;
import com.looksee.services.AccountService;
import com.looksee.services.StripeService;
import com.nimbusds.jwt.JWT;

import io.jsonwebtoken.Jwts;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/integrations")
public class IntegrationsController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("integrations.product-board.private_key")
	private String PRODUCT_BOARD_SECRET_KEY;

    @Autowired
    protected SecurityConfig appConfig;

    @Autowired
    private AccountService account_service;

    /**
     * Create new account
     *
     * @param authorization_header
     * @param account
     * @param principal
     *
     * @return
     * @throws Exception
     */

    @RequestMapping(path="/product-board", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JWT create(
    		HttpServletRequest request,
    		@RequestBody(required=true) Account account
    ) throws Exception{
    	log.warn("product board integratin request");
    	
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	com.looksee.models.dto.integration.User user = 
    						new com.looksee.models.dto.integration.User(acct.getEmail(), acct.getName());
    	/*
    	Instant now = Instant.now();

    	String jwt = Jwts.builder()
    	        .setAudience("https://${yourOktaDomain}/oauth2/default/v1/token")
    	        .setIssuedAt(Date.from(now))
    	        .setExpiration(Date.from(now.plus(5L, ChronoUnit.MINUTES)))
    	        .setIssuer(principal.getName())
    	        .setSubject(principal.getName())
    	        .setId(UUID.randomUUID().toString())
    	        .signWith(PRODUCT_BOARD_SECRET_KEY)
    	        .compact();
    	        */
    	/*
    	var userData = {
		    email: user.email,
		    id: user.id,
		    name: user.name,
		    company_name: user.company_name,
		    company_domain: user.company_domain, 
		  };
		  return jwt.sign(userData, PrivateKey, {algorithm: 'HS256'});
		  */
		return null;
    }

}
