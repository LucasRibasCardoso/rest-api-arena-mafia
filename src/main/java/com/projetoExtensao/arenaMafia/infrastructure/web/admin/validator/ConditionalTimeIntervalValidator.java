package com.projetoExtensao.arenaMafia.infrastructure.web.admin.validator;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

/**
 * Validador customizado que garante a consistência entre isFullDay e timeInterval: - Se isFullDay =
 * false → timeInterval é obrigatório - Se isFullDay = true → timeInterval deve ser null (será
 * calculado baseado nos OperatingHours)
 */
public class ConditionalTimeIntervalValidator
    implements ConstraintValidator<ValidConditionalTimeInterval, Object> {

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true; // Validação de @NotNull deve ser feita separadamente
    }

    try {
      Boolean isFullDay = getFieldValue(value, "isFullDay", Boolean.class);
      TimeInterval timeInterval = getFieldValue(value, "timeInterval", TimeInterval.class);

      // Se isFullDay for null, deixa a validação @NotNull lidar com isso
      if (isFullDay == null) {
        return true;
      }

      // Caso 1: Se isFullDay = false, então timeInterval é obrigatório
      if (!isFullDay && timeInterval == null) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate(
                "BLOCKED_TIME_TIME_INTERVAL_REQUIRED_WHEN_NOT_FULL_DAY")
            .addPropertyNode("timeInterval")
            .addConstraintViolation();
        return false;
      }

      // Caso 2: Se isFullDay = true, então timeInterval deve ser null
      if (isFullDay && timeInterval != null) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate(
                "BLOCKED_TIME_TIME_INTERVAL_NOT_ALLOWED_WHEN_FULL_DAY")
            .addPropertyNode("timeInterval")
            .addConstraintViolation();
        return false;
      }

      return true;

    } catch (Exception e) {
      return false;
    }
  }

  /** Obtém o valor de um campo usando reflexão */
  private <T> T getFieldValue(Object object, String fieldName, Class<T> fieldType)
      throws Exception {
    Field field = object.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return fieldType.cast(field.get(object));
  }
}
