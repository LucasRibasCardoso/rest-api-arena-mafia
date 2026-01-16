package com.projetoExtensao.arenaMafia.application.schedule.service;

import com.projetoExtensao.arenaMafia.application.priceRule.service.PriceCalculatorService;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.OperatingHoursNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.AvailableSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AvailableSlotGenerationService {

  private final ScheduleAvailabilityService scheduleAvailabilityService;
  private final PriceCalculatorService priceCalculatorService;

  public AvailableSlotGenerationService(
      ScheduleAvailabilityService scheduleAvailabilityService,
      PriceCalculatorService priceCalculatorService) {
    this.scheduleAvailabilityService = scheduleAvailabilityService;
    this.priceCalculatorService = priceCalculatorService;
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
  public List<AvailableSlot> generateAvailableSlotsForCourt(
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
        .sorted(Comparator.comparing(availableSlot -> availableSlot.timeInterval().startTime()))
        .toList();
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
  public List<OperatingHours> filterApplicableOperatingHours(
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
    return new AvailableSlot(court.getId(), slot, price);
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
