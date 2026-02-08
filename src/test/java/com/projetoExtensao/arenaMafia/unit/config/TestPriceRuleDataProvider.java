package com.projetoExtensao.arenaMafia.unit.config;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class TestPriceRuleDataProvider {

  private TestPriceRuleDataProvider() {}

  public static final String defaultName = "Preço Horário Nobre";
  public static final Set<DayOfWeek> defaultDaysOfWeek =
      Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY);
  public static final TimeInterval defaultTimeInterval =
      new TimeInterval(LocalTime.of(19, 0), LocalTime.of(23, 0));
  public static final BigDecimal defaultPrice = new BigDecimal("80.00");
  public static final int defaultPriority = 1;

  public static PriceRule createActivePriceRule() {
    return PriceRuleBuilder.defaultPriceRule().withIsActive(true).build();
  }

  public static PriceRule createDisabledPriceRule() {
    return PriceRuleBuilder.defaultPriceRule().withIsActive(false).build();
  }

  public static PriceRule createDefaultPriceRule() {
    return PriceRule.createDefault();
  }

  public static PriceRule createWeekendPriceRule() {
    return PriceRuleBuilder.defaultPriceRule()
        .withName("Preço Final de Semana")
        .withDaysOfWeek(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        .withPrice(new BigDecimal("100.00"))
        .build();
  }

  public static PriceRule createAllDayPriceRule() {
    return PriceRuleBuilder.defaultPriceRule()
        .withName("Preço Dia Inteiro")
        .withDaysOfWeek(null)
        .withTimeInterval(null)
        .build();
  }

  public static PriceRule createFullDayPriceRule() {
    return createAllDayPriceRule();
  }

  public static PriceRule createAllDaysPriceRule() {
    return createAllDayPriceRule();
  }

  public static class PriceRuleBuilder {
    private UUID id = UUID.randomUUID();
    private String name = defaultName;
    private Set<DayOfWeek> daysOfWeek = defaultDaysOfWeek;
    private TimeInterval timeInterval = defaultTimeInterval;
    private BigDecimal price = defaultPrice;
    private int priority = defaultPriority;
    private boolean isDefault = false;
    private boolean isActive = true;
    private Instant createdAt = Instant.now();

    public static PriceRuleBuilder defaultPriceRule() {
      return new PriceRuleBuilder();
    }

    public PriceRuleBuilder withId(UUID id) {
      this.id = id;
      return this;
    }

    public PriceRuleBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public PriceRuleBuilder withDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
      this.daysOfWeek = daysOfWeek;
      return this;
    }

    public PriceRuleBuilder withTimeInterval(TimeInterval timeInterval) {
      this.timeInterval = timeInterval;
      return this;
    }

    public PriceRuleBuilder withPrice(BigDecimal price) {
      this.price = price;
      return this;
    }

    public PriceRuleBuilder withPriority(int priority) {
      this.priority = priority;
      return this;
    }

    public PriceRuleBuilder withIsDefault(boolean isDefault) {
      this.isDefault = isDefault;
      return this;
    }

    public PriceRuleBuilder withIsActive(boolean isActive) {
      this.isActive = isActive;
      return this;
    }

    public PriceRuleBuilder withCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public PriceRule build() {
      return PriceRule.reconstitute(
          id, name, daysOfWeek, timeInterval, price, priority, isDefault, isActive, createdAt);
    }
  }

  public static Stream<Arguments> invalidNameProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.PRICE_RULE_NAME_REQUIRED),
        Arguments.of("", ErrorCode.PRICE_RULE_NAME_REQUIRED));
  }

  public static Stream<Arguments> invalidPriceProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.PRICE_RULE_PRICE_REQUIRED),
        Arguments.of(new BigDecimal("-10.00"), ErrorCode.PRICE_RULE_PRICE_INVALID));
  }

  public static Stream<Arguments> invalidPriorityProvider() {
    return Stream.of(
        Arguments.of(0, ErrorCode.PRICE_RULE_PRIORITY_INVALID),
        Arguments.of(-1, ErrorCode.PRICE_RULE_PRIORITY_INVALID));
  }
}
