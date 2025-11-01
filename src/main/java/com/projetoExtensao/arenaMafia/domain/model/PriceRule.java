package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPriceException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPriceRuleNameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.PriceRuleConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.PriceRuleStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.TimeIntervalOverlapException;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public class PriceRule {

  private final UUID id;
  private String name;
  private final Set<DayOfWeek> daysOfWeek;
  private final TimeInterval timeInterval;
  private BigDecimal price;
  private final int priority;
  private final boolean isDefault;
  private boolean isActive;
  private final Instant createdAt;

  /**
   * Cria uma nova instância de PriceRule com um ID gerado e ativo por padrão.
   *
   * @param name Nome da regra de preço
   * @param daysOfWeek Dias da semana aplicáveis (Null = Todos os dias)
   * @param timeInterval Intervalo de tempo aplicável (Null = Dia inteiro)
   * @param price Preço definido pela regra
   * @param priority Prioridade da regra
   * @return Nova instância de PriceRule
   */
  public static PriceRule create(
      String name,
      Set<DayOfWeek> daysOfWeek,
      TimeInterval timeInterval,
      BigDecimal price,
      int priority) {

    UUID id = UUID.randomUUID();
    Instant now = Instant.now();

    return new PriceRule(id, name, daysOfWeek, timeInterval, price, priority, false, true, now);
  }

  /**
   * Cria a regra de preço base para o sistema, que é aplicada quando nenhuma outra regra é
   * aplicável.
   *
   * @param price Preço base a ser definido
   * @return Nova instância de PriceRule representando o preço base
   */
  public static PriceRule createDefault(BigDecimal price) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();

    String name = "Regra de Preço Padrão";
    int priority = 0;

    return new PriceRule(id, name, null, null, price, priority, true, true, now);
  }

  /**
   * Reconstitui uma instância de PriceRule a partir dos dados fornecidos. Usado principalmente para
   * reconstruir objetos a partir de dados persistidos ou MapStruct.
   *
   * @param id Identificador único
   * @param name Nome da regra de preço
   * @param daysOfWeek Dias da semana aplicáveis (Null = Todos os dias)
   * @param timeInterval Intervalo de tempo aplicável (Null = Dia inteiro)
   * @param price Preço definido pela regra
   * @param priority Prioridade da regra
   * @param isDefault Indica se é a regra padrão
   * @param isActive Indica se a regra está ativa
   * @param createdAt Data de criação da regra
   * @return Nova instância de PriceRule
   */
  public static PriceRule reconstitute(
      UUID id,
      String name,
      Set<DayOfWeek> daysOfWeek,
      TimeInterval timeInterval,
      BigDecimal price,
      int priority,
      boolean isDefault,
      boolean isActive,
      Instant createdAt) {
    return new PriceRule(
        id, name, daysOfWeek, timeInterval, price, priority, isDefault, isActive, createdAt);
  }

  private PriceRule(
      UUID id,
      String name,
      Set<DayOfWeek> daysOfWeek,
      TimeInterval timeInterval,
      BigDecimal price,
      int priority,
      boolean isDefault,
      boolean isActive,
      Instant createdAt) {

    validateName(name);
    validatePrice(price);
    validatePriority(priority, isDefault);
    this.id = id;
    this.name = name;
    this.daysOfWeek = daysOfWeek;
    this.timeInterval = timeInterval;
    this.price = price;
    this.priority = priority;
    this.isDefault = isDefault;
    this.isActive = isActive;
    this.createdAt = createdAt;
  }

  // --- Validações ---
  public static void validateName(String name) {
    if (name == null || name.isEmpty()) {
      throw new InvalidPriceRuleNameFormatException(ErrorCode.PRICE_RULE_NAME_REQUIRED);
    }

    if (name.length() > 100) {
      throw new InvalidPriceRuleNameFormatException(ErrorCode.PRICE_RULE_NAME_INVALID_LENGTH);
    }
  }

  public static void validatePrice(BigDecimal price) {
    if (price == null) {
      throw new InvalidPriceException(ErrorCode.PRICE_RULE_PRICE_REQUIRED);
    }

    if (price.compareTo(BigDecimal.ZERO) < 0) {
      throw new InvalidPriceException(ErrorCode.PRICE_RULE_PRICE_INVALID);
    }
  }

  public static void validatePriority(int priority, boolean isDefault) {
    if (priority <= 0 && !isDefault) {
      throw new InvalidPriceException(ErrorCode.PRICE_RULE_PRIORITY_INVALID);
    }
  }

  // --- Métodos de Negócio ---
  public boolean isApplicable(DayOfWeek daysOfWeek, LocalTime time) {
    if (!this.isActive) {
      return false;
    }

    if (this.isDefault) {
      return true;
    }

    // Verifica se o dia da semana corresponde (ou é nulo, indicando todos os dias)
    boolean dayMatches =
        (daysOfWeek == null) || (this.daysOfWeek == null || this.daysOfWeek.contains(daysOfWeek));
    if (!dayMatches) {
      return false;
    }

    // Verifica se o horário corresponde (ou é nulo, indicando dia inteiro)
    return (this.timeInterval == null) || (this.timeInterval.contains(time));
  }

  public void updatePrice(BigDecimal newPrice) {
    if (newPrice != null) {
      validatePrice(newPrice);
      this.price = newPrice;
    }
  }

  public void updateName(String name) {
    if (name != null) {
      validateName(name);
      this.name = name;
    }
  }

  public void disable() {
    if (this.isDefault) {
      throw new PriceRuleStatusConflictException(ErrorCode.PRICE_RULE_CANNOT_DISABLE_DEFAULT);
    }
    if (!this.isActive) {
      throw new PriceRuleStatusConflictException(ErrorCode.PRICE_RULE_ALREADY_DISABLED);
    }
    this.isActive = false;
  }

  public void enable() {
    if (this.isActive) {
      throw new PriceRuleStatusConflictException(ErrorCode.PRICE_RULE_ALREADY_ENABLED);
    }
    this.isActive = true;
  }

  public void validateOverlapWith(PriceRule otherRule) {
    if (this.priority != otherRule.priority) {
      return;
    }

    boolean hasDaysOverlap = checkDaysOfWeekOverlapWith(otherRule.getDaysOfWeek());

    if (!hasDaysOverlap) {
      return;
    }

    try {
      validateTimeIntervalOverlapWith(otherRule.getTimeInterval());
    } catch (TimeIntervalOverlapException e) {
      throw new PriceRuleConflictException(ErrorCode.PRICE_RULE_PRIORITY_OVERLAP);
    }
  }

  // --- Métodos Auxiliares ---
  private boolean checkDaysOfWeekOverlapWith(Set<DayOfWeek> daysOfWeek) {
    if (this.daysOfWeek == null || daysOfWeek == null) {
      return true;
    }

    return this.daysOfWeek.stream().anyMatch(daysOfWeek::contains);
  }

  private void validateTimeIntervalOverlapWith(TimeInterval timeInterval) {
    if (this.timeInterval == null || timeInterval == null) {
      return;
    }

    this.timeInterval.validateNoOverlapWith(timeInterval);
  }

  // --- Getters ---
  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Set<DayOfWeek> getDaysOfWeek() {
    return daysOfWeek;
  }

  public TimeInterval getTimeInterval() {
    return timeInterval;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public int getPriority() {
    return priority;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public boolean isActive() {
    return isActive;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
