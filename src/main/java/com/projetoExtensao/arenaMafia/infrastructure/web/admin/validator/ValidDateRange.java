package com.projetoExtensao.arenaMafia.infrastructure.web.admin.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {

  String message() default "End date must be after start date";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String startDateField() default "startDate";

  String endDateField() default "endDate";
}
