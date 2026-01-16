package com.projetoExtensao.arenaMafia.infrastructure.web.admin.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalSelectedDaysOfWeekValidator.class)
@Documented
public @interface ValidConditionalSelectedDaysOfWeek {

  String message() default "BLOCKED_TIME_SELECTED_DAYS_NOT_ALLOWED_FOR_SINGLE_DATE";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
