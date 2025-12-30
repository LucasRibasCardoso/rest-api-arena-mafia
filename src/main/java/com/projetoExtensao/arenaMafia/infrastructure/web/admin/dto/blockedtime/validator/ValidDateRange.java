package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {

  String message() default "BLOCKED_TIME_INVALID_DATE_RANGE";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};


  String startDateField() default "startDate";


  String endDateField() default "endDate";
}

