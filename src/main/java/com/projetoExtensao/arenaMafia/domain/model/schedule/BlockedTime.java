package com.projetoExtensao.arenaMafia.domain.model.schedule;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidBlockedTimeException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;

import java.time.Instant;
import java.util.UUID;

public class BlockedTime extends ScheduleEntry {

  private final String description;
  private final UUID blockedByAdminId;
  private final boolean isFullDay;
  private final UUID recurringBlockedTimeId;

  /**
   * Factory method para criar um bloqueio de horário específico em um único dia. Uso: quando um
   * admin bloqueia um horário específico de uma quadra em uma data.
   *
   * @param courtId id da quadra a ser bloqueada, é obrigatório
   * @param dateTimeSlot slot de data e hora do bloqueio, é obrigatório
   * @param description descrição/motivo do bloqueio, é obrigatório
   * @param blockedByAdminId id do admin que está criando o bloqueio, é obrigatório
   * @return uma nova instância de BlockedTime
   */
  public static BlockedTime createSpecificTime(
      UUID courtId,
      DateTimeSlot dateTimeSlot,
      String description,
      UUID blockedByAdminId) {

    UUID id = UUID.randomUUID();
    Instant createdAt = Instant.now();
    boolean isFullDay = false;

    return new BlockedTime(
        id, courtId, dateTimeSlot, description, blockedByAdminId, isFullDay, null, createdAt);
  }

  /**
   * Factory method para criar um bloqueio de dia inteiro em uma única data. Uso: quando um admin
   * bloqueia uma quadra por um dia inteiro (durante todo o horário de funcionamento).
   *
   * <p>O timeInterval deve corresponder ao horário de funcionamento da quadra naquele dia, obtido
   * através dos OperatingHours. Mesmo que o horário passe da meia-noite (ex: 08:00 - 02:00), o
   * bloqueio cobrirá todo o período de funcionamento.
   *
   * @param courtId id da quadra a ser bloqueada, é obrigatório
   * @param dateTimeSlot slot de data e hora do bloqueio (baseado em OperatingHours), é obrigatório
   * @param description descrição/motivo do bloqueio, é obrigatório
   * @param blockedByAdminId id do admin que está criando o bloqueio, é obrigatório
   * @return uma nova instância de BlockedTime
   */
  public static BlockedTime createFullDay(
      UUID courtId,
      DateTimeSlot dateTimeSlot,
      String description,
      UUID blockedByAdminId) {

    UUID id = UUID.randomUUID();
    Instant createdAt = Instant.now();
    boolean isFullDay = true;

    return new BlockedTime(
        id,
        courtId,
        dateTimeSlot,
        description,
        blockedByAdminId,
        isFullDay,
        null, // recurringBlockedTimeId é null para bloqueios únicos
        createdAt);
  }

  /**
   * Factory method para criar um bloqueio recorrente (parte de um grupo de bloqueios). Uso: quando
   * um admin bloqueia uma quadra por vários dias (ex: férias, manutenção prolongada).
   *
   * @param courtId id da quadra a ser bloqueada, é obrigatório
   * @param dateTimeSlot slot de data e hora do bloqueio, é obrigatório
   * @param description descrição/motivo do bloqueio, é obrigatório
   * @param blockedByAdminId id do admin que está criando o bloqueio, é obrigatório
   * @param isFullDay indica se é bloqueio de dia inteiro
   * @param recurringBlockedTimeId id que agrupa todos os bloqueios recorrentes, é obrigatório
   * @return uma nova instância de BlockedTime
   */
  public static BlockedTime createRecurring(
      UUID courtId,
      DateTimeSlot dateTimeSlot,
      String description,
      UUID blockedByAdminId,
      boolean isFullDay,
      UUID recurringBlockedTimeId) {

    UUID id = UUID.randomUUID();
    Instant createdAt = Instant.now();

    return new BlockedTime(
        id,
        courtId,
        dateTimeSlot,
        description,
        blockedByAdminId,
        isFullDay,
        recurringBlockedTimeId,
        createdAt);
  }

  /**
   * Factory method para reconstituir um bloqueio de horário a partir de dados persistidos.
   * Utilizado pelo MapStruct.
   *
   * @param id id do bloqueio
   * @param courtId id da quadra
   * @param dateTimeSlot slot de data e hora
   * @param description descrição do bloqueio
   * @param blockedByAdminId id do admin que bloqueou
   * @param isFullDay se é bloqueio de dia inteiro
   * @param recurringBlockedTimeId id que agrupa bloqueios recorrentes (null para bloqueios únicos)
   * @param createdAt momento de criação do bloqueio
   * @return uma instância de BlockedTime
   */
  public static BlockedTime reconstitute(
      UUID id,
      UUID courtId,
      DateTimeSlot dateTimeSlot,
      String description,
      UUID blockedByAdminId,
      boolean isFullDay,
      UUID recurringBlockedTimeId,
      Instant createdAt) {
    return new BlockedTime(
        id,
        courtId,
        dateTimeSlot,
        description,
        blockedByAdminId,
        isFullDay,
        recurringBlockedTimeId,
        createdAt);
  }

  private BlockedTime(
      UUID id,
      UUID courtId,
      DateTimeSlot dateTimeSlot,
      String description,
      UUID blockedByAdminId,
      boolean isFullDay,
      UUID recurringBlockedTimeId,
      Instant createdAt) {

    super(id, courtId, dateTimeSlot, createdAt);

    validateDescription(description);
    validateBlockedByAdminId(blockedByAdminId);

    this.description = description;
    this.blockedByAdminId = blockedByAdminId;
    this.isFullDay = isFullDay;
    this.recurringBlockedTimeId = recurringBlockedTimeId;
  }

  // --- Validações ---

  private static void validateDescription(String description) {
    if (description == null || description.isBlank()) {
      throw new InvalidBlockedTimeException(ErrorCode.BLOCKED_TIME_DESCRIPTION_REQUIRED);
    }

    if (description.length() < 3 || description.length() > 500) {
      throw new InvalidBlockedTimeException(ErrorCode.BLOCKED_TIME_DESCRIPTION_INVALID_LENGTH);
    }
  }

  private static void validateBlockedByAdminId(UUID blockedByAdminId) {
    if (blockedByAdminId == null) {
      throw new InvalidBlockedTimeException(ErrorCode.BLOCKED_TIME_ADMIN_ID_REQUIRED);
    }
  }

  // --- Comportamentos de Negócio ---

  /**
   * Verifica se o bloqueio de horário está ativo. BlockedTime sempre retorna true, pois bloqueios
   * não possuem status e estão sempre ativos enquanto existem.
   *
   * @return sempre true
   */
  @Override
  public boolean isActive() {
    return true;
  }

  // --- Getters ---

  public String getDescription() {
    return description;
  }

  public UUID getBlockedByAdminId() {
    return blockedByAdminId;
  }

  public boolean isFullDay() {
    return isFullDay;
  }

  public UUID getRecurringBlockedTimeId() {
    return recurringBlockedTimeId;
  }
}
