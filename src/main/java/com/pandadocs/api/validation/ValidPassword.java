package com.pandadocs.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password must contain at least 8 characters, including uppercase, lowercase, number and special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
