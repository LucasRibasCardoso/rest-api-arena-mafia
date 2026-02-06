package com.projetoExtensao.arenaMafia.infrastructure.web.admin.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Set;

/**
 * Validador customizado que garante que selectedDaysOfWeek seja null quando startDate é igual a
 * endDate (bloqueio para um único dia).
 *
 * <p>Lógica:
 *
 * <ul>
 *   <li>Se startDate == endDate → selectedDaysOfWeek deve ser null ou vazio
 *   <li>Se startDate != endDate → selectedDaysOfWeek pode ser qualquer valor
 * </ul>
 */
public class ConditionalSelectedDaysOfWeekValidator
    implements ConstraintValidator<ValidConditionalSelectedDaysOfWeek, Object> {

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true; // Validação de @NotNull deve ser feita separadamente
    }

    try {
      LocalDate startDate = getFieldValue(value, "startDate", LocalDate.class);
      LocalDate endDate = getFieldValue(value, "endDate", LocalDate.class);
      Set<?> selectedDaysOfWeek = getFieldValue(value, "selectedDaysOfWeek", Set.class);

      // Se startDate ou endDate for null, deixa outras validações lidarem com isso
      if (startDate == null || endDate == null) {
        return true;
      }

      if (startDate.equals(endDate)) {
        if (selectedDaysOfWeek != null && !selectedDaysOfWeek.isEmpty()) {
          context.disableDefaultConstraintViolation();
          context
              .buildConstraintViolationWithTemplate(
                  "SELECTED_DAYS_NOT_ALLOWED_FOR_SINGLE_DATE")
              .addPropertyNode("selectedDaysOfWeek")
              .addConstraintViolation();
          return false;
        }
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
