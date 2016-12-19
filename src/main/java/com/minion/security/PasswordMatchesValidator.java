package com.minion.security;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.qanairy.models.dto.UserDto;

public class PasswordMatchesValidator 
	implements ConstraintValidator<PasswordMatches, Object> { 
   
  public void initialize(PasswordMatches constraintAnnotation) {       
  }

  public boolean isValid(Object obj, ConstraintValidatorContext context){   
      UserDto user = (UserDto) obj;
      return user.getPassword().equals(user.getMatchingPassword());    
  }

}