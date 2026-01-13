package com.projetoExtensao.arenaMafia.infrastructure.web.admin.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDate;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

  private String startDateField;
  private String endDateField;

  @Override
  public void initialize(ValidDateRange constraintAnnotation) {
    this.startDateField = constraintAnnotation.startDateField();
    this.endDateField = constraintAnnotation.endDateField();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true; // Deixa a validação @NotNull lidar com isso
    }

    try {
      LocalDate startDate = getFieldValue(value, startDateField);
      LocalDate endDate = getFieldValue(value, endDateField);

      if (startDate == null || endDate == null) {
        return true;
      }

      LocalDate today = LocalDate.now();

      // Validação 1: startDate não pode ser no passado
      if (startDate.isBefore(today)) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate("BLOCKED_TIME_START_DATE_IN_PAST")
            .addPropertyNode(startDateField)
            .addConstraintViolation();
        return false;
      }

      // Validação 2: startDate deve ser <= endDate
      if (startDate.isAfter(endDate)) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate("BLOCKED_TIME_START_DATE_AFTER_END_DATE")
            .addPropertyNode(startDateField)
            .addConstraintViolation();
        return false;
      }

      return true;

    } catch (Exception e) {
      // Em caso de erro na reflexão, considera inválido
      return false;
    }
  }

  /** Obtém o valor de um campo usando reflexão */
  private LocalDate getFieldValue(Object object, String fieldName) throws Exception {
    Field field = object.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return (LocalDate) field.get(object);
  }
}
