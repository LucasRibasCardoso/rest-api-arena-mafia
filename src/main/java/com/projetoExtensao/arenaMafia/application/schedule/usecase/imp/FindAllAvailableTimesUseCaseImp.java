package com.projetoExtensao.arenaMafia.application.schedule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.ports.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.ports.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.service.PriceCalculatorService;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleAvailabilityService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.FindAllAvailableTimesUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.PastDateException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.OperatingHoursNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PriceRuleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.domain.model.schedule.AvailableSlot;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.OperatingHoursSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindAllAvailableTimesUseCaseImp implements FindAllAvailableTimesUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final OperatingHoursRepositoryPort operatingHoursRepositoryPort;
  private final PriceRuleRepositoryPort priceRuleRepositoryPort;
  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final PriceCalculatorService priceCalculatorService;
  private final ScheduleAvailabilityService scheduleAvailabilityService;

  public FindAllAvailableTimesUseCaseImp(
      CourtRepositoryPort courtRepositoryPort,
      OperatingHoursRepositoryPort operatingHoursRepositoryPort,
      PriceRuleRepositoryPort priceRuleRepositoryPort,
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort,
      PriceCalculatorService priceCalculatorService,
      ScheduleAvailabilityService scheduleAvailabilityService) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.operatingHoursRepositoryPort = operatingHoursRepositoryPort;
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.priceCalculatorService = priceCalculatorService;
    this.scheduleAvailabilityService = scheduleAvailabilityService;
  }

  @Override
  public List<AvailableSlot> execute(UUID modalityId, LocalDate date) {
    validateDate(date);

    List<Court> courts = getActiveCourtsByModality(modalityId);
    List<OperatingHours> operatingHours = getActiveOperatingHours();
    List<PriceRule> priceRules = getActivePriceRules();

    DayOfWeek dayOfWeek = DayOfWeek.convertToDayOfWeek(date);
    List<OperatingHours> applicableOperatingHours = filterApplicableOperatingHours(operatingHours, dayOfWeek);

    return courts.stream()
        .map(
            court -> {
              var schedules =
                  scheduleEntryRepositoryPort.findConfirmedSchedulesByCourtAndDate(
                      court.getId(), date);
              return generateAvailableSlotsForCourt(
                  court, applicableOperatingHours, schedules, priceRules, dayOfWeek);
            })
        .flatMap(List::stream)
        .sorted(Comparator.comparing(slot -> slot.getTimeInterval().startTime()))
        .toList();
  }

  /**
   * Valida a data fornecida. Lança exceção se a data for no passado.
   *
   * @param date data a ser validada
   * @throws PastDateException se a data for no passado
   */
  private void validateDate(LocalDate date) {
    if (date.isBefore(LocalDate.now())) {
      throw new PastDateException();
    }
  }

  /**
   * Busca quadras ativas por modalidade. Lança exceção se nenhuma for encontrada.
   *
   * @param modalityId ID da modalidade
   * @return lista de quadras ativas
   * @throws CourtNotFoundException se nenhuma quadra for encontrada
   */
  private List<Court> getActiveCourtsByModality(UUID modalityId) {
    var courts = courtRepositoryPort.findActiveCourtsByModalityId(modalityId);
    if (courts.isEmpty()) {
      throw new CourtNotFoundException(ErrorCode.COURT_NOT_FOUND_BY_MODALITY);
    }
    return courts;
  }

  /**
   * Busca horários de funcionamento ativos. Lança exceção se nenhum for encontrado.
   *
   * @return lista de horários de funcionamento ativos
   * @throws OperatingHoursNotFoundException se nenhum horário for encontrado
   */
  private List<OperatingHours> getActiveOperatingHours() {
    var operatingHours =
        operatingHoursRepositoryPort.findAll(OperatingHoursSpecification.byActiveStatus(true));
    if (operatingHours.isEmpty()) {
      throw new OperatingHoursNotFoundException();
    }
    return operatingHours;
  }

  /**
   * Busca regras de preço ativas. Lança exceção se nenhuma for encontrada.
   *
   * @return lista de regras de preço ativas
   * @throws PriceRuleNotFoundException se nenhuma regra for encontrada
   */
  private List<PriceRule> getActivePriceRules() {
    var priceRules = priceRuleRepositoryPort.findAll(PriceRuleSpecification.byActiveStatus(true));
    if (priceRules.isEmpty()) {
      throw new PriceRuleNotFoundException();
    }
    return priceRules;
  }

  /**
   * Filtra os horários de funcionamento aplicáveis para o dia da semana especificado. Lança exceção
   * se não houver horários de funcionamento para o dia especificado.
   *
   * @param allOperatingHours lista completa de horários de funcionamento
   * @param dayOfWeek dia da semana
   * @return lista de horários de funcionamento aplicáveis
   * @throws OperatingHoursNotFoundException se não houver horários aplicáveis para o dia da semana
   */
  private List<OperatingHours> filterApplicableOperatingHours(
      List<OperatingHours> allOperatingHours, DayOfWeek dayOfWeek) {
    List<OperatingHours> applicableOperatingHours =
        allOperatingHours.stream()
            .filter(oh -> oh.getDaysOfWeek() == null || oh.getDaysOfWeek().contains(dayOfWeek))
            .toList();

    if (applicableOperatingHours.isEmpty()) {
      throw new OperatingHoursNotFoundException(ErrorCode.OPERATING_HOURS_APPLICABLE_NOT_FOUND);
    }
    return applicableOperatingHours;
  }

  /**
   * Gera slots disponíveis para uma quadra específica.
   *
   * @param court quadra
   * @param operatingHoursList horários de funcionamento aplicáveis
   * @param confirmedSchedules reservas confirmadas para a quadra na data
   * @param priceRules regras de preço
   * @param dayOfWeek dia da semana
   * @return lista de slots disponíveis
   */
  private List<AvailableSlot> generateAvailableSlotsForCourt(
      Court court,
      List<OperatingHours> operatingHoursList,
      List<ScheduleEntry> confirmedSchedules,
      List<PriceRule> priceRules,
      DayOfWeek dayOfWeek) {

    return operatingHoursList.stream()
        .map(OperatingHours::getTimeInterval)
        .flatMap(interval -> generateSlots(interval, court.getOffsetMinutes()).stream())
        .filter(slot -> !scheduleAvailabilityService.isSlotOccupied(slot, confirmedSchedules))
        .map(slot -> buildAvailableSlot(court, slot, priceRules, dayOfWeek))
        .sorted(Comparator.comparing(availableSlot -> availableSlot.getTimeInterval().startTime()))
        .toList();
  }

  /**
   * Cria um objeto AvailableSlot com o preço calculado.
   *
   * @param court quadra
   * @param slot intervalo de tempo do slot
   * @param priceRules regras de preço
   * @param dayOfWeek dia da semana
   * @return AvailableSlot criado
   */
  private AvailableSlot buildAvailableSlot(
      Court court, TimeInterval slot, List<PriceRule> priceRules, DayOfWeek dayOfWeek) {
    BigDecimal price = priceCalculatorService.calculateSlotPrice(slot, priceRules, dayOfWeek);
    return AvailableSlot.create(court.getId(), slot, price);
  }

  /**
   * Gera slots de 1 hora dentro de um intervalo de tempo, respeitando o offset e os limites de
   * funcionamento.
   *
   * @param operatingInterval intervalo total de funcionamento (ex: 08:00 - 22:00)
   * @param courtOffset minutos de offset (ex: 30)
   * @return lista de slots de 1 hora válidos
   */
  private List<TimeInterval> generateSlots(
      TimeInterval operatingInterval, OffsetMinutes courtOffset) {
    List<TimeInterval> slots = new ArrayList<>();

    LocalTime currentSlotStartTime =
        alignStartTimeToOffset(courtOffset, operatingInterval.startTime());

    while (canFitFullSlot(currentSlotStartTime, operatingInterval)) {
      LocalTime slotEndTime = currentSlotStartTime.plusHours(1);

      try {
        slots.add(new TimeInterval(currentSlotStartTime, slotEndTime));
      } catch (InvalidTimeIntervalException e) {
        // Ignorar slots inválidos
      }
      currentSlotStartTime = currentSlotStartTime.plusHours(1);
    }
    return slots;
  }

  /**
   * Verifica se um slot de 1 hora começando em 'slotStart' cabe totalmente no horário de
   * funcionamento.
   *
   * @param slotStart início do slot
   * @param operatingInterval intervalo de funcionamento
   * @return true se cabe totalmente no horário de funcionamento
   */
  private boolean canFitFullSlot(LocalTime slotStart, TimeInterval operatingInterval) {
    LocalTime slotEnd = slotStart.plusHours(1);
    return operatingInterval.contains(slotEnd) || slotEnd.equals(operatingInterval.endTime());
  }

  /**
   * Alinha o início do horário de funcionamento com o offset da quadra. Se o ajuste fizer o horário
   * ficar antes do início do funcionamento, avança para o próximo slot válido.
   *
   * @param courtOffset offset da quadra (0 ou 30 minutos)
   * @param operatingStartTime horário de início do funcionamento
   * @return horário ajustado para o offset da quadra
   */
  private LocalTime alignStartTimeToOffset(
      OffsetMinutes courtOffset, LocalTime operatingStartTime) {

    if (operatingStartTime.getMinute() == courtOffset.getValue()) {
      return operatingStartTime;
    }

    LocalTime aligned = operatingStartTime.withMinute(courtOffset.getValue());

    if (aligned.isBefore(operatingStartTime)) {
      return aligned.plusHours(1);
    }

    return aligned;
  }
}
