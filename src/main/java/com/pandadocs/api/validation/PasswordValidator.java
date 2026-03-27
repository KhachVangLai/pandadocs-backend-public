package com.pandadocs.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // Regex pattern:
    // - (?=.*[a-z])     : at least one lowercase letter
    // - (?=.*[A-Z])     : at least one uppercase letter
    // - (?=.*\d)        : at least one digit
    // - (?=.*[@$!%*?&]) : at least one allowed special character
    // - .{8,}           : at least eight characters
    private static final String PASSWORD_PATTERN =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        boolean isValid = pattern.matcher(password).matches();

        if (!isValid) {
            // Provide a more specific validation message.
            context.disableDefaultConstraintViolation();

            if (password.length() < 8) {
                context.buildConstraintViolationWithTemplate(
                    "Password must be at least 8 characters long"
                ).addConstraintViolation();
            } else if (!password.matches(".*[a-z].*")) {
                context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one lowercase letter"
                ).addConstraintViolation();
            } else if (!password.matches(".*[A-Z].*")) {
                context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one uppercase letter"
                ).addConstraintViolation();
            } else if (!password.matches(".*\\d.*")) {
                context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one number"
                ).addConstraintViolation();
            } else if (!password.matches(".*[@$!%*?&].*")) {
                context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one special character (@$!%*?&)"
                ).addConstraintViolation();
            } else {
                // Catch-all for any other validation failures (e.g., invalid characters)
                context.buildConstraintViolationWithTemplate(
                    "Password must contain only letters, numbers, and special characters (@$!%*?&)"
                ).addConstraintViolation();
            }
        }

        return isValid;
    }
}
