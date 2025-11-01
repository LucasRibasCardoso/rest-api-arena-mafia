package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPriceException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPriceRuleNameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.PriceRuleStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.unit.config.TestPriceRuleDataProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayName("Testes unitários para entidade PriceRule")
public class PriceRuleTest {

  private final String defaultName = TestPriceRuleDataProvider.defaultName;
  private final BigDecimal defaultPrice = TestPriceRuleDataProvider.defaultPrice;
  private final int defaultPriority = TestPriceRuleDataProvider.defaultPriority;
  private final Set<DayOfWeek> defaultDaysOfWeek = TestPriceRuleDataProvider.defaultDaysOfWeek;
  private final TimeInterval defaultTimeInterval = TestPriceRuleDataProvider.defaultTimeInterval;

  @Nested
  @DisplayName("Testes para os Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("create() deve criar uma regra de preço com valores padrão corretos")
    void create_shouldCreatePriceRuleSuccessfully() {
      // Arrange
      Instant startTime = Instant.now();

      // Act
      PriceRule priceRule =
          PriceRule.create(
              defaultName, defaultDaysOfWeek, defaultTimeInterval, defaultPrice, defaultPriority);

      // Assert
      assertThat(priceRule).isNotNull();
      assertThat(priceRule.getId()).isNotNull();
      assertThat(priceRule.getName()).isEqualTo(defaultName);
      assertThat(priceRule.getDaysOfWeek()).isEqualTo(defaultDaysOfWeek);
      assertThat(priceRule.getTimeInterval()).isEqualTo(defaultTimeInterval);
      assertThat(priceRule.getPrice()).isEqualByComparingTo(defaultPrice);
      assertThat(priceRule.getPriority()).isEqualTo(defaultPriority);
      assertThat(priceRule.isDefault()).isFalse();
      assertThat(priceRule.isActive()).isTrue();
      assertThat(priceRule.getCreatedAt())
          .isAfterOrEqualTo(startTime)
          .isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("createDefault() deve criar uma regra de preço padrão corretamente")
    void createDefault_shouldCreateDefaultPriceRuleSuccessfully() {
      // Arrange
      BigDecimal basePrice = new BigDecimal("80.00");
      Instant startTime = Instant.now();

      // Act
      PriceRule priceRule = PriceRule.createDefault(basePrice);

      // Assert
      assertThat(priceRule).isNotNull();
      assertThat(priceRule.getId()).isNotNull();
      assertThat(priceRule.getName()).isEqualTo("Regra de Preço Padrão");
      assertThat(priceRule.getDaysOfWeek()).isNull();
      assertThat(priceRule.getTimeInterval()).isNull();
      assertThat(priceRule.getPrice()).isEqualByComparingTo(basePrice);
      assertThat(priceRule.getPriority()).isZero();
      assertThat(priceRule.isDefault()).isTrue();
      assertThat(priceRule.isActive()).isTrue();
      assertThat(priceRule.getCreatedAt())
          .isAfterOrEqualTo(startTime)
          .isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("reconstitute() deve reconstituir uma regra de preço a partir de dados existentes")
    void reconstitute_shouldRebuildPriceRuleSuccessfully() {
      // Arrange
      UUID id = UUID.randomUUID();
      String name = "Regra Especial";
      Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(20, 0));
      BigDecimal price = new BigDecimal("150.00");
      int priority = 5;
      boolean isDefault = false;
      boolean isActive = false;
      Instant createdAt = Instant.now().minusSeconds(3600);

      // Act
      PriceRule priceRule =
          PriceRule.reconstitute(
              id, name, daysOfWeek, timeInterval, price, priority, isDefault, isActive, createdAt);

      // Assert
      assertThat(priceRule.getId()).isEqualTo(id);
      assertThat(priceRule.getName()).isEqualTo(name);
      assertThat(priceRule.getDaysOfWeek()).isEqualTo(daysOfWeek);
      assertThat(priceRule.getTimeInterval()).isEqualTo(timeInterval);
      assertThat(priceRule.getPrice()).isEqualByComparingTo(price);
      assertThat(priceRule.getPriority()).isEqualTo(priority);
      assertThat(priceRule.isDefault()).isFalse();
      assertThat(priceRule.isActive()).isFalse();
      assertThat(priceRule.getCreatedAt()).isEqualTo(createdAt);
    }

    @Nested
    @DisplayName("Cenários de Falha (Validações)")
    class Failure {

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestPriceRuleDataProvider#invalidNameProvider")
      @DisplayName("create() deve lançar InvalidPriceRuleNameFormatException para nomes inválidos")
      void create_shouldThrowException_whenNameIsInvalid(String invalidName, ErrorCode errorCode) {
        // Act & Assert
        assertThatThrownBy(
                () ->
                    PriceRule.create(
                        invalidName,
                        defaultDaysOfWeek,
                        defaultTimeInterval,
                        defaultPrice,
                        defaultPriority))
            .isInstanceOf(InvalidPriceRuleNameFormatException.class)
            .satisfies(
                ex -> {
                  InvalidPriceRuleNameFormatException exception =
                      (InvalidPriceRuleNameFormatException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestPriceRuleDataProvider#invalidPriceProvider")
      @DisplayName("create() deve lançar InvalidPriceException para preços inválidos")
      void create_shouldThrowException_whenPriceIsInvalid(
          BigDecimal invalidPrice, ErrorCode errorCode) {
        // Act & Assert
        assertThatThrownBy(
                () ->
                    PriceRule.create(
                        defaultName,
                        defaultDaysOfWeek,
                        defaultTimeInterval,
                        invalidPrice,
                        defaultPriority))
            .isInstanceOf(InvalidPriceException.class)
            .satisfies(
                ex -> {
                  InvalidPriceException exception = (InvalidPriceException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestPriceRuleDataProvider#invalidPriorityProvider")
      @DisplayName("create() deve lançar InvalidPriceException para prioridades inválidas")
      void create_shouldThrowException_whenPriorityIsInvalid(
          int invalidPriority, ErrorCode errorCode) {
        // Act & Assert
        assertThatThrownBy(
                () ->
                    PriceRule.create(
                        defaultName,
                        defaultDaysOfWeek,
                        defaultTimeInterval,
                        defaultPrice,
                        invalidPriority))
            .isInstanceOf(InvalidPriceException.class)
            .satisfies(
                ex -> {
                  InvalidPriceException exception = (InvalidPriceException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }
    }
  }

  @Nested
  @DisplayName("Testes para os Métodos de Atualização (update...)")
  class AttributeUpdateTests {

    @Test
    @DisplayName("updatePrice() deve alterar o preço com um valor válido")
    void updatePrice_shouldUpdatePrice_whenValid() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      BigDecimal newPrice = new BigDecimal("200.00");

      // Act
      priceRule.updatePrice(newPrice);

      // Assert
      assertThat(priceRule.getPrice()).isEqualByComparingTo(newPrice);
    }

    @Test
    @DisplayName("updatePrice() deve aceitar diferentes preços válidos")
    void updatePrice_shouldUpdatePrice_withValidPrices() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      BigDecimal validPrice = new BigDecimal("50.00");

      // Act
      priceRule.updatePrice(validPrice);

      // Assert
      assertThat(priceRule.getPrice()).isEqualByComparingTo(validPrice);
    }

    @Test
    @DisplayName("updatePrice() deve lançar InvalidPriceException para preço inválido")
    void updatePrice_shouldThrowException_whenPriceIsInvalid() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      BigDecimal invalidPrice = new BigDecimal("-50.00");

      // Act & Assert
      assertThatThrownBy(() -> priceRule.updatePrice(invalidPrice))
          .isInstanceOf(InvalidPriceException.class)
          .satisfies(
              ex -> {
                InvalidPriceException exception = (InvalidPriceException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRICE_RULE_PRICE_INVALID);
              });
    }

    @Test
    @DisplayName("updatePrice() não deve fazer nada quando o preço é null")
    void updatePrice_shouldNotUpdate_whenPriceIsNull() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      BigDecimal originalPrice = priceRule.getPrice();

      // Act
      priceRule.updatePrice(null);

      // Assert
      assertThat(priceRule.getPrice()).isEqualByComparingTo(originalPrice);
    }

    @Test
    @DisplayName("updateName() deve alterar o nome com um valor válido")
    void updateName_shouldUpdateName_whenValid() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      String newName = "Nova Regra";

      // Act
      priceRule.updateName(newName);

      // Assert
      assertThat(priceRule.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("updateName() deve aceitar diferentes nomes válidos")
    void updateName_shouldUpdateName_withValidNames() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      String validName = "Regra Especial";

      // Act
      priceRule.updateName(validName);

      // Assert
      assertThat(priceRule.getName()).isEqualTo(validName);
    }

    @Test
    @DisplayName("updateName() deve lançar exceção quando o nome é vazio")
    void updateName_shouldThrowException_whenNameIsEmpty() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      String originalName = priceRule.getName();

      // Act & Assert
      assertThatThrownBy(() -> priceRule.updateName(""))
          .isInstanceOf(InvalidPriceRuleNameFormatException.class);
      assertThat(priceRule.getName()).isEqualTo(originalName);
    }

    @Test
    @DisplayName("updateName() não deve fazer nada quando o nome é null")
    void updateName_shouldNotUpdate_whenNameIsNull() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      String originalName = priceRule.getName();

      // Act
      priceRule.updateName(null);

      // Assert
      assertThat(priceRule.getName()).isEqualTo(originalName);
    }
  }

  @Nested
  @DisplayName("Testes para Gerenciamento de Status (enable/disable)")
  class StatusManagementTests {

    @Test
    @DisplayName("disable() deve desativar uma regra de preço ativa")
    void disable_shouldDisablePriceRule_whenPriceRuleIsActive() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      assertThat(priceRule.isActive()).isTrue();

      // Act
      priceRule.disable();

      // Assert
      assertThat(priceRule.isActive()).isFalse();
    }

    @Test
    @DisplayName(
        "disable() deve lançar PriceRuleStatusConflictException quando a regra já está desativada")
    void disable_shouldThrowException_whenPriceRuleIsAlreadyDisabled() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createDisabledPriceRule();
      assertThat(priceRule.isActive()).isFalse();

      // Act & Assert
      assertThatThrownBy(priceRule::disable)
          .isInstanceOf(PriceRuleStatusConflictException.class)
          .satisfies(
              ex -> {
                PriceRuleStatusConflictException exception = (PriceRuleStatusConflictException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PRICE_RULE_ALREADY_DISABLED);
              });
    }

    @Test
    @DisplayName(
        "disable() deve lançar PriceRuleStatusConflictException quando tentando desativar regra padrão")
    void disable_shouldThrowException_whenPriceRuleIsDefault() {
      // Arrange
      PriceRule defaultPriceRule =
          TestPriceRuleDataProvider.createDefaultPriceRule(new BigDecimal("100.00"));
      assertThat(defaultPriceRule.isDefault()).isTrue();

      // Act & Assert
      assertThatThrownBy(defaultPriceRule::disable)
          .isInstanceOf(PriceRuleStatusConflictException.class)
          .satisfies(
              ex -> {
                PriceRuleStatusConflictException exception = (PriceRuleStatusConflictException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PRICE_RULE_CANNOT_DISABLE_DEFAULT);
              });
    }

    @Test
    @DisplayName("enable() deve ativar uma regra de preço desativada")
    void enable_shouldEnablePriceRule_whenPriceRuleIsDisabled() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createDisabledPriceRule();
      assertThat(priceRule.isActive()).isFalse();

      // Act
      priceRule.enable();

      // Assert
      assertThat(priceRule.isActive()).isTrue();
    }

    @Test
    @DisplayName(
        "enable() deve lançar PriceRuleStatusConflictException quando a regra já está ativada")
    void enable_shouldThrowException_whenPriceRuleIsAlreadyEnabled() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();
      assertThat(priceRule.isActive()).isTrue();

      // Act & Assert
      assertThatThrownBy(priceRule::enable)
          .isInstanceOf(PriceRuleStatusConflictException.class)
          .satisfies(
              ex -> {
                PriceRuleStatusConflictException exception = (PriceRuleStatusConflictException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PRICE_RULE_ALREADY_ENABLED);
              });
    }

    @Test
    @DisplayName("Deve permitir ativar e desativar uma regra de preço alternadamente")
    void shouldAllowToggleActiveStatus() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();

      // Act & Assert - Ciclo 1
      assertThat(priceRule.isActive()).isTrue();
      priceRule.disable();
      assertThat(priceRule.isActive()).isFalse();
      priceRule.enable();
      assertThat(priceRule.isActive()).isTrue();

      // Ciclo 2
      priceRule.disable();
      assertThat(priceRule.isActive()).isFalse();
      priceRule.enable();
      assertThat(priceRule.isActive()).isTrue();
    }
  }

  @Nested
  @DisplayName("Testes para Métodos de Validação Estáticos")
  class StaticValidationTests {

    @Test
    @DisplayName("validateName() não deve lançar exceção para um nome válido")
    void validateName_shouldNotThrowException_whenNameIsValid() {
      // Act & Assert
      assertDoesNotThrow(() -> PriceRule.validateName("Regra Válida"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName(
        "validateName() deve lançar InvalidPriceRuleNameFormatException para nomes inválidos")
    void validateName_shouldThrowException_whenNameIsInvalid(String invalidName) {
      // Act & Assert
      assertThatThrownBy(() -> PriceRule.validateName(invalidName))
          .isInstanceOf(InvalidPriceRuleNameFormatException.class)
          .satisfies(
              ex -> {
                InvalidPriceRuleNameFormatException exception =
                    (InvalidPriceRuleNameFormatException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRICE_RULE_NAME_REQUIRED);
              });
    }

    @Test
    @DisplayName("validatePrice() não deve lançar exceção para preços válidos")
    void validatePrice_shouldNotThrowException_whenPriceIsValid() {
      // Act & Assert
      assertDoesNotThrow(() -> PriceRule.validatePrice(new BigDecimal("100.00")));
      assertDoesNotThrow(() -> PriceRule.validatePrice(BigDecimal.ZERO));
    }

    @ParameterizedTest
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.config.TestPriceRuleDataProvider#invalidPriceProvider")
    @DisplayName("validatePrice() deve lançar InvalidPriceException para preços inválidos")
    void validatePrice_shouldThrowException_whenPriceIsInvalid(
        BigDecimal invalidPrice, ErrorCode errorCode) {
      // Act & Assert
      assertThatThrownBy(() -> PriceRule.validatePrice(invalidPrice))
          .isInstanceOf(InvalidPriceException.class)
          .satisfies(
              ex -> {
                InvalidPriceException exception = (InvalidPriceException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
              });
    }

    @Test
    @DisplayName("validatePriority() não deve lançar exceção para prioridades válidas")
    void validatePriority_shouldNotThrowException_whenPriorityIsValid() {
      // Act & Assert
      assertDoesNotThrow(() -> PriceRule.validatePriority(0, true));
      assertDoesNotThrow(() -> PriceRule.validatePriority(1, false));
      assertDoesNotThrow(() -> PriceRule.validatePriority(100, false));
    }

    @ParameterizedTest
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.config.TestPriceRuleDataProvider#invalidPriorityProvider")
    @DisplayName("validatePriority() deve lançar InvalidPriceException para prioridades inválidas")
    void validatePriority_shouldThrowException_whenPriorityIsInvalid(
        int invalidPriority, ErrorCode errorCode) {
      // Act & Assert
      assertThatThrownBy(() -> PriceRule.validatePriority(invalidPriority, false))
          .isInstanceOf(InvalidPriceException.class)
          .satisfies(
              ex -> {
                InvalidPriceException exception = (InvalidPriceException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
              });
    }
  }

  @Nested
  @DisplayName("Testes para Método isApplicable()")
  class IsApplicableTests {

    @Test
    @DisplayName("isApplicable() deve retornar true para regra padrão ativa")
    void isApplicable_shouldReturnTrue_forActiveDefaultRule() {
      // Arrange
      PriceRule defaultRule =
          TestPriceRuleDataProvider.createDefaultPriceRule(new BigDecimal("100.00"));

      // Act & Assert
      assertThat(defaultRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(10, 0))).isTrue();
      assertThat(defaultRule.isApplicable(DayOfWeek.SUNDAY, LocalTime.of(22, 0))).isTrue();
      assertThat(defaultRule.isApplicable(null, null)).isTrue();
    }

    @Test
    @DisplayName("isApplicable() deve retornar false para regra desativada")
    void isApplicable_shouldReturnFalse_forInactiveRule() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createDisabledPriceRule();

      // Act & Assert
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(10, 0))).isFalse();
    }

    @Test
    @DisplayName("isApplicable() deve retornar true quando o dia corresponde")
    void isApplicable_shouldReturnTrue_whenDayMatches() {
      // Arrange
      PriceRule priceRule =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
              .withTimeInterval(null)
              .build();

      // Act & Assert
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(10, 0))).isTrue();
      assertThat(priceRule.isApplicable(DayOfWeek.TUESDAY, LocalTime.of(15, 0))).isTrue();
    }

    @Test
    @DisplayName("isApplicable() deve retornar false quando o dia não corresponde")
    void isApplicable_shouldReturnFalse_whenDayDoesNotMatch() {
      // Arrange
      PriceRule priceRule =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
              .withTimeInterval(null)
              .build();

      // Act & Assert
      assertThat(priceRule.isApplicable(DayOfWeek.SATURDAY, LocalTime.of(10, 0))).isFalse();
      assertThat(priceRule.isApplicable(DayOfWeek.SUNDAY, LocalTime.of(15, 0))).isFalse();
    }

    @Test
    @DisplayName("isApplicable() deve retornar true quando daysOfWeek é null (todos os dias)")
    void isApplicable_shouldReturnTrue_whenDaysOfWeekIsNull() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createAllDaysPriceRule();

      // Act & Assert - Quando daysOfWeek da regra é null, qualquer dia deve ser aceito
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(10, 0))).isTrue();
      assertThat(priceRule.isApplicable(DayOfWeek.SUNDAY, LocalTime.of(10, 0))).isTrue();
      assertThat(priceRule.isApplicable(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0))).isTrue();
      // Também deve aceitar null (representando qualquer dia)
      assertThat(priceRule.isApplicable(null, LocalTime.of(10, 0))).isTrue();
    }

    @Test
    @DisplayName("isApplicable() deve retornar true quando o horário está dentro do intervalo")
    void isApplicable_shouldReturnTrue_whenTimeIsWithinInterval() {
      // Arrange
      PriceRule priceRule =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withDaysOfWeek(null)
              .withTimeInterval(new TimeInterval(LocalTime.of(8, 0), LocalTime.of(18, 0)))
              .build();

      // Act & Assert
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(8, 0))).isTrue();
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(12, 0))).isTrue();
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(17, 30))).isTrue();
    }

    @Test
    @DisplayName("isApplicable() deve retornar false quando o horário está fora do intervalo")
    void isApplicable_shouldReturnFalse_whenTimeIsOutsideInterval() {
      // Arrange
      PriceRule priceRule =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withDaysOfWeek(null)
              .withTimeInterval(new TimeInterval(LocalTime.of(8, 0), LocalTime.of(18, 0)))
              .build();

      // Act & Assert
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(7, 30))).isFalse();
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(18, 0))).isFalse();
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(20, 0))).isFalse();
    }

    @Test
    @DisplayName("isApplicable() deve retornar true quando timeInterval é null (dia inteiro)")
    void isApplicable_shouldReturnTrue_whenTimeIntervalIsNull() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createFullDayPriceRule();

      // Act & Assert
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(0, 0))).isTrue();
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(12, 0))).isTrue();
      assertThat(priceRule.isApplicable(DayOfWeek.MONDAY, LocalTime.of(23, 30))).isTrue();
    }
  }

  @Nested
  @DisplayName("Testes para Validação de Sobreposição (validateOverlapWith)")
  class OverlapValidationTests {

    @Test
    @DisplayName("validateOverlapWith() não deve lançar exceção quando prioridades são diferentes")
    void validateOverlapWith_shouldNotThrowException_whenPrioritiesAreDifferent() {
      // Arrange
      PriceRule rule1 =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withPriority(1)
              .withDaysOfWeek(Set.of(DayOfWeek.MONDAY))
              .build();
      PriceRule rule2 =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withPriority(2)
              .withDaysOfWeek(Set.of(DayOfWeek.MONDAY))
              .build();

      // Act & Assert
      assertDoesNotThrow(() -> rule1.validateOverlapWith(rule2));
    }

    

    @Test
    @DisplayName(
        "validateOverlapWith() não deve lançar exceção quando daysOfWeek é null em uma das regras")
    void validateOverlapWith_shouldNotThrowException_whenDaysOfWeekIsNull() {
      // Arrange
      PriceRule rule1 =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withPriority(1)
              .withDaysOfWeek(null)
              .withTimeInterval(new TimeInterval(LocalTime.of(8, 0), LocalTime.of(12, 0)))
              .build();
      PriceRule rule2 =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withPriority(1)
              .withDaysOfWeek(Set.of(DayOfWeek.MONDAY))
              .withTimeInterval(new TimeInterval(LocalTime.of(12, 0), LocalTime.of(18, 0)))
              .build();

      // Act & Assert
      assertDoesNotThrow(() -> rule1.validateOverlapWith(rule2));
    }

    @Test
    @DisplayName("validateOverlapWith() não deve lançar exceção quando não há sobreposição de dias")
    void validateOverlapWith_shouldNotThrowException_whenNoDaysOverlap() {
      // Arrange
      PriceRule rule1 =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withPriority(1)
              .withDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
              .withTimeInterval(null)
              .build();
      PriceRule rule2 =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withPriority(1)
              .withDaysOfWeek(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
              .withTimeInterval(null)
              .build();

      // Act & Assert
      assertDoesNotThrow(() -> rule1.validateOverlapWith(rule2));
    }
  }
}
