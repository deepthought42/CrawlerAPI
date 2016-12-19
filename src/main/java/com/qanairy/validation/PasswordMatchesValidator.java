package com.qanairy.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.qanairy.models.dto.UserDto;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    public void initialize(final PasswordMatches constraintAnnotation) {
        //
    }

    public boolean isValid(final Object obj, final ConstraintValidatorContext context) {
        final UserDto user = (UserDto) obj;
        return user.getPassword().equals(user.getMatchingPassword());
    }

}
