package com.minion.api;

import java.net.MalformedURLException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.qanairy.models.TestUser;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.repository.TestUserRepository;

/**
 * REST controller that defines endpoints to access test users
 */
@Controller
@RequestMapping("/test_users")
public class TestUserController {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestUserController.class);

	@Autowired
	private TestUserRepository test_user_repo;
	
	/**
	 * Create a new test user and add it to the domain
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * 
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
    @PreAuthorize("hasAuthority('create:test_user')")
    @RequestMapping(path="{user_id}", method = RequestMethod.PUT)
    public @ResponseBody void updateUser(HttpServletRequest request,
    									@PathVariable(value="user_id", required=true) long user_id,
    									@RequestParam(value="isEnabled", required=true) boolean isEnabled,
    									@RequestParam(value="password", required=true) String password,
    									@RequestParam(value="username", required=true) String username,
    									@RequestParam(value="role", required=false) String role)  {
    	Optional<TestUser> optional_user = test_user_repo.findById(user_id);
    	if(optional_user.isPresent()){
    		TestUser test_user_record = optional_user.get();
    		    		
    		test_user_record.setIsEnabled(isEnabled);
    		test_user_record.setPassword(password);
    		test_user_record.setRole(role);
    		test_user_record.setUsername(username);
    		test_user_repo.save(test_user_record);
    	}
    	else{
    		throw new TestUserNotFoundException();
    	}
    }
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class TestUserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public TestUserNotFoundException() {
		super("Test user could not be found.");
	}
}

