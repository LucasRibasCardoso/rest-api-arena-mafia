package com.projetoExtensao.arenaMafia.application.agenda.usecase.imp;

import com.projetoExtensao.arenaMafia.application.agenda.usecase.FindAllAgendaItemUseCase;
import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.port.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.AvailableSlotGenerationService;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.PastDateException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.OperatingHoursNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PriceRuleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.agenda.AgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.GroupedAvailableSlotAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.ScheduleEntryAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.AvailableSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.AvailableSlotWithModalities;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.CourtSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.OperatingHoursSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FindAllAgendaItemUseCaseImp implements FindAllAgendaItemUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final OperatingHoursRepositoryPort operatingHoursRepositoryPort;
  private final PriceRuleRepositoryPort priceRuleRepositoryPort;
  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final AvailableSlotGenerationService availableSlotGenerationService;

  public FindAllAgendaItemUseCaseImp(
      CourtRepositoryPort courtRepositoryPort,
      OperatingHoursRepositoryPort operatingHoursRepositoryPort,
      PriceRuleRepositoryPort priceRuleRepositoryPort,
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort,
      AvailableSlotGenerationService availableSlotGenerationService) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.operatingHoursRepositoryPort = operatingHoursRepositoryPort;
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.availableSlotGenerationService = availableSlotGenerationService;
  }

  /**
   * Busca todos os itens da agenda para uma data específica.
   *
   * <p>Processa a agenda completa do dia incluindo agendamentos confirmados e slots disponíveis,
   * agrupados por intervalo de tempo e ordenados cronologicamente.
   *
   * @param date data para buscar a agenda (não pode ser no passado)
   * @return lista de itens da agenda ordenados por horário de início
   * @throws PastDateException se a data for anterior à data atual
   * @throws CourtNotFoundException se não houver quadras ativas no sistema
   * @throws PriceRuleNotFoundException se não houver regras de preço ativas
   * @throws OperatingHoursNotFoundException se não houver horários de funcionamento ativos ou
   *     aplicáveis ao dia da semana
   */
  @Override
  public List<AgendaItem> execute(LocalDate date) {
    validateDate(date);

    List<ScheduleEntry> activeSchedules = getActiveSchedulesByDate(date);
    List<Court> allActiveCourts = getActiveCourts();
    List<PriceRule> priceRules = getActivePriceRules();
    List<OperatingHours> applicableOperatingHours = getApplicableOperatingHours(date);
    DayOfWeek dayOfWeek = DayOfWeek.convertToDayOfWeek(date);

    List<AvailableSlotWithModalities> availableSlots =
        generateAvailableSlotsForAllCourts(
            allActiveCourts, activeSchedules, applicableOperatingHours, priceRules, dayOfWeek);

    return groupSlotsByTimeInterval(activeSchedules, availableSlots);
  }

  /**
   * Valida se a data fornecida é válida para consulta da agenda.
   *
   * @param date data a ser validada
   * @throws PastDateException se a data for anterior à data atual
   */
  private void validateDate(LocalDate date) {
    if (date.isBefore(LocalDate.now())) {
      throw new PastDateException();
    }
  }

  /**
   * Busca todos os agendamentos ativos (confirmados) para uma data específica.
   *
   * <p>Filtra apenas agendamentos com status ativo, excluindo reservas canceladas ou completadas.
   *
   * @param date data para buscar os agendamentos
   * @return lista de agendamentos ativos ordenados por horário de início
   */
  private List<ScheduleEntry> getActiveSchedulesByDate(LocalDate date) {
    return scheduleEntryRepositoryPort.findAllActiveSchedulesByDate(date);
  }

  /**
   * Busca todas as quadras ativas do sistema.
   *
   * @return lista de quadras ativas
   * @throws CourtNotFoundException se não houver nenhuma quadra ativa cadastrada
   */
  private List<Court> getActiveCourts() {
    List<Court> courts = courtRepositoryPort.findAll(CourtSpecification.byActiveStatus(true));
    if (courts.isEmpty()) {
      throw new CourtNotFoundException();
    }
    return courts;
  }

  /**
   * Busca todas as regras de preço ativas do sistema.
   *
   * @return lista de regras de preço ativas
   * @throws PriceRuleNotFoundException se não houver nenhuma regra de preço ativa cadastrada
   */
  private List<PriceRule> getActivePriceRules() {
    List<PriceRule> priceRules =
        priceRuleRepositoryPort.findAll(PriceRuleSpecification.byActiveStatus(true));
    if (priceRules.isEmpty()) {
      throw new PriceRuleNotFoundException();
    }
    return priceRules;
  }

  /**
   * Busca os horários de funcionamento aplicáveis para uma data específica.
   *
   * <p>Filtra horários ativos e aplicáveis ao dia da semana correspondente à data fornecida.
   *
   * @param date data para buscar os horários de funcionamento
   * @return lista de horários de funcionamento aplicáveis ao dia da semana
   * @throws OperatingHoursNotFoundException se não houver horários de funcionamento ativos ou se
   *     não houver horários aplicáveis ao dia da semana especificado
   */
  private List<OperatingHours> getApplicableOperatingHours(LocalDate date) {
    var filterActive = OperatingHoursSpecification.byActiveStatus(true);
    List<OperatingHours> operatingHours = operatingHoursRepositoryPort.findAll(filterActive);
    if (operatingHours.isEmpty()) {
      throw new OperatingHoursNotFoundException();
    }

    DayOfWeek dayOfWeek = DayOfWeek.convertToDayOfWeek(date);
    return availableSlotGenerationService.filterApplicableOperatingHours(operatingHours, dayOfWeek);
  }

  /**
   * Gera slots disponíveis para todas as quadras ativas do sistema.
   *
   * <p>Para cada quadra, gera os slots de 1 hora disponíveis considerando:
   *
   * <ul>
   *   <li>Horários de funcionamento aplicáveis ao dia
   *   <li>Agendamentos já confirmados (para excluir horários ocupados)
   *   <li>Regras de preço para calcular o valor de cada slot
   *   <li>Modalidades suportadas pela quadra
   * </ul>
   *
   * @param courts lista de quadras ativas
   * @param activeSchedules agendamentos confirmados do dia
   * @param applicableOperatingHours horários de funcionamento aplicáveis
   * @param priceRules regras de preço ativas
   * @param dayOfWeek dia da semana
   * @return lista de slots disponíveis com suas respectivas modalidades suportadas
   */
  private List<AvailableSlotWithModalities> generateAvailableSlotsForAllCourts(
      List<Court> courts,
      List<ScheduleEntry> activeSchedules,
      List<OperatingHours> applicableOperatingHours,
      List<PriceRule> priceRules,
      DayOfWeek dayOfWeek) {

    return courts.stream()
        .flatMap(
            court -> {
              // Filtrar schedules da quadra
              List<ScheduleEntry> courtSchedules =
                  activeSchedules.stream()
                      .filter(schedule -> schedule.getCourtId().equals(court.getId()))
                      .toList();

              List<AvailableSlot> courtSlots =
                  availableSlotGenerationService.generateAvailableSlotsForCourt(
                      court, applicableOperatingHours, courtSchedules, priceRules, dayOfWeek);

              return courtSlots.stream()
                  .map(
                      slot ->
                          new AvailableSlotWithModalities(
                              slot.courtId(),
                              slot.timeInterval(),
                              slot.price(),
                              court.getModalityIds()));
            })
        .toList();
  }

  /**
   * Agrupa slots disponíveis e agendamentos por intervalo de tempo, criando a estrutura final da
   * agenda.
   *
   * <p>Este metodo realiza o processamento final da agenda:
   *
   * <ol>
   *   <li>Agrupa slots disponíveis por horário
   *   <li>Agrupa agendamentos confirmados por horário
   *   <li>Mescla ambos em uma lista única de itens da agenda
   *   <li>Ordena cronologicamente
   * </ol>
   *
   * <p>Para cada horário, podem existir:
   *
   * <ul>
   *   <li>Múltiplos agendamentos (uma ou mais quadras ocupadas)
   *   <li>Um item agrupado de slots disponíveis (se houver quadras livres)
   *   <li>Ambos (agendamentos E slots disponíveis no mesmo horário)
   * </ul>
   *
   * @param scheduleEntries agendamentos confirmados (reservas, treinos, bloqueios)
   * @param availableSlots slots disponíveis com modalidades suportadas
   * @return lista de itens da agenda ordenados cronologicamente por horário de início
   */
  private List<AgendaItem> groupSlotsByTimeInterval(
      List<ScheduleEntry> scheduleEntries, List<AvailableSlotWithModalities> availableSlots) {

    Map<TimeInterval, List<AvailableSlotWithModalities>> slotsByTime =
        groupAvailableSlotsByTimeInterval(availableSlots);

    Map<TimeInterval, List<ScheduleEntry>> schedulesByTime =
        groupScheduleEntriesByTimeInterval(scheduleEntries);

    Set<TimeInterval> allTimeIntervals = getAllUniqueTimeIntervals(slotsByTime, schedulesByTime);

    return allTimeIntervals.stream()
        .flatMap(
            timeInterval ->
                createAgendaItemsForTimeInterval(
                    timeInterval,
                    schedulesByTime.getOrDefault(timeInterval, List.of()),
                    slotsByTime.getOrDefault(timeInterval, List.of()))
                    .stream())
        .sorted(Comparator.comparing(item -> item.getTimeInterval().startTime()))
        .toList();
  }

  /**
   * Agrupa slots disponíveis pelo intervalo de tempo.
   *
   * @param availableSlots lista de slots disponíveis
   * @return mapa com intervalos de tempo como chave e lista de slots como valor
   */
  private Map<TimeInterval, List<AvailableSlotWithModalities>> groupAvailableSlotsByTimeInterval(
      List<AvailableSlotWithModalities> availableSlots) {
    return availableSlots.stream()
        .collect(Collectors.groupingBy(AvailableSlotWithModalities::timeInterval));
  }

  /**
   * Agrupa agendamentos pelo intervalo de tempo.
   *
   * @param scheduleEntries lista de agendamentos confirmados
   * @return mapa com intervalos de tempo como chave e lista de agendamentos como valor
   */
  private Map<TimeInterval, List<ScheduleEntry>> groupScheduleEntriesByTimeInterval(
      List<ScheduleEntry> scheduleEntries) {
    return scheduleEntries.stream()
        .collect(Collectors.groupingBy(entry -> entry.getDateTimeSlot().timeInterval()));
  }

  /**
   * Obtém todos os intervalos de tempo únicos presentes nos slots disponíveis e agendamentos.
   *
   * <p>Combina os horários de ambas as fontes (slots e agendamentos) para garantir que todos os
   * horários sejam processados na agenda final.
   *
   * @param slotsByTime mapa de slots agrupados por horário
   * @param schedulesByTime mapa de agendamentos agrupados por horário
   * @return conjunto com todos os intervalos de tempo únicos
   */
  private Set<TimeInterval> getAllUniqueTimeIntervals(
      Map<TimeInterval, ?> slotsByTime, Map<TimeInterval, ?> schedulesByTime) {
    Set<TimeInterval> allTimeIntervals = new HashSet<>(slotsByTime.keySet());
    allTimeIntervals.addAll(schedulesByTime.keySet());
    return allTimeIntervals;
  }

  /**
   * Cria os itens da agenda para um intervalo de tempo específico.
   *
   * <p>Para cada horário, cria:
   *
   * <ul>
   *   <li>Um item para cada agendamento confirmado (reserva, treino, bloqueio)
   *   <li>Um item agrupado de slots disponíveis (se houver quadras livres neste horário)
   * </ul>
   *
   * <p>Exemplo: se há 2 quadras, uma reservada e outra disponível às 10:00, este metodo retorna 2
   * itens: um ScheduleEntryAgendaItem (RESERVED) e um GroupedAvailableSlotAgendaItem (AVAILABLE).
   *
   * @param timeInterval intervalo de tempo (ex: 10:00-11:00)
   * @param schedules lista de agendamentos confirmados neste horário
   * @param availableSlots lista de slots disponíveis neste horário
   * @return lista de itens da agenda para este intervalo de tempo específico
   */
  private List<AgendaItem> createAgendaItemsForTimeInterval(
      TimeInterval timeInterval,
      List<ScheduleEntry> schedules,
      List<AvailableSlotWithModalities> availableSlots) {

    List<AgendaItem> items = new ArrayList<>(createScheduleEntryAgendaItems(schedules));

    // Adicionar slot disponível agrupado (se houver quadras livres)
    createGroupedAvailableSlotItem(timeInterval, availableSlots).ifPresent(items::add);

    return items;
  }

  /**
   * Converte agendamentos em itens da agenda.
   *
   * <p>Cada agendamento (reserva, treino ou bloqueio) é convertido em um item individual da agenda.
   *
   * @param schedules lista de agendamentos
   * @return lista de itens da agenda representando os agendamentos
   */
  private List<AgendaItem> createScheduleEntryAgendaItems(List<ScheduleEntry> schedules) {
    return schedules.stream().map(ScheduleEntryAgendaItem::new).collect(Collectors.toList());
  }

  /* Cria um item de slot disponível agrupado com todas as modalidades disponíveis.
   *
   * <p>Agrupa múltiplos slots disponíveis do mesmo horário (de quadras diferentes) em um único item
   * da agenda, consolidando todas as modalidades suportadas pelas quadras disponíveis.
   *
   * <p>Exemplo: se às 10:00 há 2 quadras disponíveis, uma suportando Futebol e Vôlei e outra
   * suportando apenas Basquete, este metodo cria um único item com modalidades [Futebol, Vôlei,
   * Basquete].
   *
   * @param timeInterval intervalo de tempo
   * @param availableSlots lista de slots disponíveis neste horário (pode estar vazia)
   * @return Optional contendo o item agrupado com todas as modalidades, ou Optional.empty() se não
   *     houver slots disponíveis
   */
  private Optional<AgendaItem> createGroupedAvailableSlotItem(
      TimeInterval timeInterval, List<AvailableSlotWithModalities> availableSlots) {

    if (availableSlots.isEmpty()) {
      return Optional.empty();
    }

    Set<UUID> allModalityIds =
        availableSlots.stream()
            .flatMap(slot -> slot.modalityIds().stream())
            .collect(Collectors.toSet());

    return Optional.of(new GroupedAvailableSlotAgendaItem(timeInterval, allModalityIds));
  }
}
