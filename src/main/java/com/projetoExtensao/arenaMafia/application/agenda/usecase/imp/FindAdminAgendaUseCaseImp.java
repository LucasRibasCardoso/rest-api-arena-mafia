package com.projetoExtensao.arenaMafia.application.agenda.usecase.imp;

import com.projetoExtensao.arenaMafia.application.agenda.usecase.FindAdminAgendaUseCase;
import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.port.repository.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ScheduleEntryDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.result.ScheduleEntriesEnrichedResult;
import com.projetoExtensao.arenaMafia.application.schedule.service.AvailableSlotGenerationService;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.OperatingHoursNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PriceRuleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.agenda.admin.AdminAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.admin.AdminAvailableSlotAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.admin.AdminScheduleEntryAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.AvailableSlotWithModalities;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.CourtSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.OperatingHoursSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindAdminAgendaUseCaseImp implements FindAdminAgendaUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final OperatingHoursRepositoryPort operatingHoursRepositoryPort;
  private final PriceRuleRepositoryPort priceRuleRepositoryPort;
  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final AvailableSlotGenerationService availableSlotGenerationService;
  private final ScheduleEntryEnrichmentService enrichmentService;

  public FindAdminAgendaUseCaseImp(
      CourtRepositoryPort courtRepositoryPort,
      OperatingHoursRepositoryPort operatingHoursRepositoryPort,
      PriceRuleRepositoryPort priceRuleRepositoryPort,
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort,
      AvailableSlotGenerationService availableSlotGenerationService,
      ScheduleEntryEnrichmentService enrichmentService) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.operatingHoursRepositoryPort = operatingHoursRepositoryPort;
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.availableSlotGenerationService = availableSlotGenerationService;
    this.enrichmentService = enrichmentService;
  }

  @Override
  public List<AdminAgendaItem> execute(LocalDate date, Optional<UUID> courtId) {
    DayOfWeek dayOfWeek = DayOfWeek.convertToDayOfWeek(date);

    List<Court> courts = getActiveCourts(courtId);
    List<PriceRule> priceRules = getActivePriceRules();
    List<ScheduleEntry> schedules = getActiveSchedulesByDateAndCourts(date, courts);
    List<OperatingHours> operatingHours = getApplicableOperatingHours(dayOfWeek);

    List<AvailableSlotWithModalities> availableSlots =
        availableSlotGenerationService.generateAvailableSlotsDetailsForCourts(
            courts, schedules, operatingHours, priceRules, dayOfWeek);

    ScheduleEntriesEnrichedResult enrichedResult =
        enrichmentService.enrichScheduleEntries(schedules);
    List<ScheduleEntryDetail> scheduleDetails = enrichedResult.allEnrichedEntries();

    List<AdminAvailableSlotAgendaItem> availableSlotItems =
        mapAvailableSlotsToAgendaItems(availableSlots, courts);
    List<AdminScheduleEntryAgendaItem> scheduleEntryAgendaItems =
        mapScheduleDetailToAgendaItem(scheduleDetails);

    return buildAgendaListAndSort(availableSlotItems, scheduleEntryAgendaItems);
  }

  private List<PriceRule> getActivePriceRules() {
    List<PriceRule> priceRules =
        priceRuleRepositoryPort.findAll(PriceRuleSpecification.byActiveStatus(true));
    if (priceRules.isEmpty()) {
      throw new PriceRuleNotFoundException();
    }
    return priceRules;
  }

  private List<Court> getActiveCourts(Optional<UUID> courtIdOpt) {
    if (courtIdOpt.isPresent()) {
      Court court = courtRepositoryPort.findByIdOrElseThrow(courtIdOpt.get());
      return List.of(court);
    }

    List<Court> courts = courtRepositoryPort.findAll(CourtSpecification.byActiveStatus(true));
    if (courts.isEmpty()) {
      throw new CourtNotFoundException();
    }
    return courts;
  }

  private List<OperatingHours> getApplicableOperatingHours(DayOfWeek dayOfWeek) {
    List<OperatingHours> operatingHours =
        operatingHoursRepositoryPort.findAll(OperatingHoursSpecification.byActiveStatus(true));
    if (operatingHours.isEmpty()) {
      throw new OperatingHoursNotFoundException();
    }

    return availableSlotGenerationService.filterApplicableOperatingHours(operatingHours, dayOfWeek);
  }

  private List<ScheduleEntry> getActiveSchedulesByDateAndCourts(
      LocalDate date, List<Court> courts) {
    List<ScheduleEntry> allSchedules =
        scheduleEntryRepositoryPort.findAllActiveSchedulesByDate(date);

    // Filtrar apenas schedules das quadras selecionadas
    List<UUID> courtIds = courts.stream().map(Court::getId).toList();
    return allSchedules.stream().filter(s -> courtIds.contains(s.getCourtId())).toList();
  }

  private List<AdminAvailableSlotAgendaItem> mapAvailableSlotsToAgendaItems(
      List<AvailableSlotWithModalities> availableSlots, List<Court> courts) {

    Map<UUID, String> courtNamesMap =
        courts.stream().collect(Collectors.toMap(Court::getId, Court::getName));

    return availableSlots.stream()
        .map(
            slot -> {
              String courtName = courtNamesMap.getOrDefault(slot.courtId(), "Quadra Desconhecida");
              return new AdminAvailableSlotAgendaItem(
                  slot.courtId(), courtName, slot.timeInterval(), slot.modalityIds(), slot.price());
            })
        .toList();
  }

  private List<AdminScheduleEntryAgendaItem> mapScheduleDetailToAgendaItem(
      List<ScheduleEntryDetail> scheduleDetails) {
    return scheduleDetails.stream().map(AdminScheduleEntryAgendaItem::new).toList();
  }

  private List<AdminAgendaItem> buildAgendaListAndSort(
      List<AdminAvailableSlotAgendaItem> availableSlotAgendaItems,
      List<AdminScheduleEntryAgendaItem> scheduleEntryAgendaItems) {

    return Stream.concat(availableSlotAgendaItems.stream(), scheduleEntryAgendaItems.stream())
        .sorted(
            Comparator.comparing((AdminAgendaItem item) -> item.getTimeInterval().startTime())
                .thenComparing(this::getCourtName))
        .collect(Collectors.toList());
  }

  private String getCourtName(AdminAgendaItem item) {
    if (item instanceof AdminAvailableSlotAgendaItem s) return s.courtName();
    if (item instanceof AdminScheduleEntryAgendaItem(ScheduleEntryDetail detail))
      return detail.courtName();
    return "";
  }
}
