package com.projetoExtensao.arenaMafia.application.schedule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.ports.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.ports.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.AvailableSlotGenerationService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.FindAllAvailableTimesUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.PastDateException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.OperatingHoursNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PriceRuleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.AvailableSlot;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.OperatingHoursSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindAllAvailableTimesUseCaseImp implements FindAllAvailableTimesUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final PriceRuleRepositoryPort priceRuleRepositoryPort;
  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final OperatingHoursRepositoryPort operatingHoursRepositoryPort;
  private final AvailableSlotGenerationService availableSlotGenerationService;

  public FindAllAvailableTimesUseCaseImp(
      CourtRepositoryPort courtRepositoryPort,
      PriceRuleRepositoryPort priceRuleRepositoryPort,
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort,
      OperatingHoursRepositoryPort operatingHoursRepositoryPort,
      AvailableSlotGenerationService availableSlotGenerationService) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.operatingHoursRepositoryPort = operatingHoursRepositoryPort;
    this.availableSlotGenerationService = availableSlotGenerationService;
  }

  @Override
  public List<AvailableSlot> execute(UUID modalityId, LocalDate date) {
    validateDate(date);

    List<Court> courts = getActiveCourtsByModality(modalityId);
    List<OperatingHours> operatingHours = getActiveOperatingHours();
    List<PriceRule> priceRules = getActivePriceRules();

    DayOfWeek dayOfWeek = DayOfWeek.convertToDayOfWeek(date);
    List<OperatingHours> applicableOperatingHours =
        availableSlotGenerationService.filterApplicableOperatingHours(operatingHours, dayOfWeek);

    return courts.stream()
        .map(
            court -> {
              var schedules =
                  scheduleEntryRepositoryPort.findConfirmedSchedulesByCourtAndDate(
                      court.getId(), date);
              return availableSlotGenerationService.generateAvailableSlotsForCourt(
                  court, applicableOperatingHours, schedules, priceRules, dayOfWeek);
            })
        .flatMap(List::stream)
        .sorted(Comparator.comparing(slot -> slot.timeInterval().startTime()))
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
}
