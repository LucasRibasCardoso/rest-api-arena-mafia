package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalTimeIntervalValidator.class)
@Documented
public @interface ValidConditionalTimeInterval {

  String message() default "BLOCKED_TIME_TIME_INTERVAL_REQUIRED_WHEN_NOT_FULL_DAY";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
