package com.projetoExtensao.arenaMafia.application.schedule.service;

import com.projetoExtensao.arenaMafia.application.operatingHours.port.repository.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidBlockDateException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidBlockedTimeException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.OperatingHoursNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsável por cálculos relacionados a datas e horários de BlockedTime.
 *
 * <p>Centraliza a lógica de:
 * <ul>
 *   <li>Cálculo de datas aplicáveis baseado em intervalo e dias da semana
 *   <li>Resolução de dias da semana efetivos
 *   <li>Validação de horários de funcionamento
 *   <li>Cálculo de intervalos de tempo para bloqueios de dia inteiro
 * </ul>
 */
@Service
public class BlockedTimeDateCalculationService {

  private static final int MAX_BLOCKED_TIME_OCCURRENCES = 1000;

  private final OperatingHoursRepositoryPort operatingHoursRepository;

  public BlockedTimeDateCalculationService(OperatingHoursRepositoryPort operatingHoursRepository) {
    this.operatingHoursRepository = operatingHoursRepository;
  }

  /**
   * Calcula as datas aplicáveis baseado no intervalo e dias da semana selecionado* <p>Filtra as datas no intervalo que correspondem aos dias da semana selecionados.
   *
   * <p>Se dias da semana são fornecidos, filtra apenas as datas que correspondem a esses dias.
   * Caso contrário, retorna todas as datas no intervalo.
   * @param endDate Data final (inclusiva)
   * @param selectedDaysOfWeek Dias da semana selecionados (null ou vazio = todos os dias)
   * @return Lista de LocalDate representando as datas aplicáveis
   */
  public List<LocalDate> calculateApplicableDates(
      LocalDate startDate,
      LocalDate endDate,
      Set<DayOfWeek> selectedDaysOfWeek) {

    Stream<LocalDate> dateStream = startDate.datesUntil(endDate.plusDays(1));

    // Se não há dias da semana selecionados, retorna todas as datas
    if (selectedDaysOfWeek == null || selectedDaysOfWeek.isEmpty()) {
      return dateStream.collect(Collectors.toList());
    }

    // Filtra apenas os dias da semana selecionados
    return dateStream
            .filter(date -> selectedDaysOfWeek.contains(DayOfWeek.convertToDayOfWeek(date)))
            .collect(Collectors.toList());
  }

  /**
   * Resolve os dias da semana efetivos e valida o limite de ocorrências em uma única operação.
   *
   * <p>Metodo otimizado para cenários onde as datas aplicáveis não são utilizadas,
   * apenas sua contagem para validação de limite.
   *
   * @param selectedDaysOfWeek Dias da semana selecionados (pode ser null ou vazio para representar todos os dias)
   * @param startDate Data inicial do intervalo
   * @param endDate Data final do intervalo
   * @param courtsCount Número de quadras selecionadas
   * @return Conjunto de dias da semana efetivos
   * @throws InvalidBlockDateException se algum dia selecionado estiver fora do range de datas ou se exceder limite de ocorrências
   * @throws OperatingHoursNotFoundException se algum dia não possuir horários de funcionamento definidos
   */
  public Set<DayOfWeek> resolveEffectiveDaysOfWeekWithOccurrencesValidation(
      Set<DayOfWeek> selectedDaysOfWeek,
      LocalDate startDate,
      LocalDate endDate,
      int courtsCount) {

    Set<DayOfWeek> effectiveDaysOfWeek;

    if (selectedDaysOfWeek != null && !selectedDaysOfWeek.isEmpty()) {
      validateSelectedDaysWithinDateRange(selectedDaysOfWeek, startDate, endDate);
      effectiveDaysOfWeek = selectedDaysOfWeek;
    } else {
      effectiveDaysOfWeek = getDaysOfWeekInRange(startDate, endDate);
    }

    // Valida se todos os dias efetivos possuem horários de funcionamento
    validateDaysHaveOperatingHours(effectiveDaysOfWeek);

    long datesCount = countApplicableDates(startDate, endDate, effectiveDaysOfWeek);
    validateOccurrencesLimit(courtsCount, (int) datesCount);

    return effectiveDaysOfWeek;
  }


  /**
   * Valida se todos os dias solicitados possuem horários de funcionamento definidos.
   *
   * @param requestedDays Conjunto de dias da semana solicitados
   * @throws OperatingHoursNotFoundException se algum dia solicitado não possuir horários de funcionamento
   */
  public void validateDaysHaveOperatingHours(Set<DayOfWeek> requestedDays) {
    // Busca os horários existentes para os dias solicitados
    List<OperatingHours> foundHours = operatingHoursRepository.findByDaysOfWeek(requestedDays);

    // Extrai quais dias foram encontrados no banco
    Set<DayOfWeek> foundDays =
        foundHours.stream()
            .map(OperatingHours::getDaysOfWeek)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    if (!foundDays.containsAll(requestedDays)) {
      throw new OperatingHoursNotFoundException(ErrorCode.OPERATING_HOURS_APPLICABLE_NOT_FOUND);
    }
  }

  /**
   * Calcula o intervalo de tempo para bloqueio baseado nos dias da semana efetivos.
   *
   * <p>Para bloqueios de dia inteiro (isFullDay = true), calcula o intervalo que cobre
   * desde o horário de abertura mais cedo até o horário de fechamento mais tarde
   * dos horários de funcionamento dos dias afetados.
   *
   * <p>Para bloqueios parciais (isFullDay = false), valida se o intervalo fornecido
   * está completamente contido dentro do horário de funcionamento de TODOS os dias
   * selecionados.
   *
   * @param isFullDay Se o bloqueio é de dia inteiro
   * @param providedInterval Intervalo fornecido na requisição (usado se não for fullDay)
   * @param effectiveDaysOfWeek Dias da semana efetivos
   * @return TimeInterval calculado
   * @throws InvalidBlockedTimeException se o intervalo não for fornecido quando isFullDay é false
   * @throws InvalidBlockDateException se o intervalo fornecido estiver fora do horário de funcionamento de algum dos dias selecionados
   */
  public TimeInterval calculateSearchInterval(
      boolean isFullDay,
      TimeInterval providedInterval,
      Set<DayOfWeek> effectiveDaysOfWeek) {

    List<OperatingHours> hoursList = operatingHoursRepository.findByDaysOfWeek(effectiveDaysOfWeek);

    if (!isFullDay) {
      if (providedInterval == null) {
        throw new InvalidBlockedTimeException(ErrorCode.BLOCKED_TIME_TIME_INTERVAL_REQUIRED_WHEN_NOT_FULL_DAY);
      }
      validateIntervalWithinOperatingHours(providedInterval, hoursList, effectiveDaysOfWeek);
      return providedInterval;
    }

    LocalTime minStart = calculateMinStart(hoursList);
    LocalTime maxEnd = calculateMaxEnd(hoursList);

    return new TimeInterval(minStart, maxEnd);
  }

  /**
   * Conta as datas aplicáveis baseado no intervalo e dias da semana selecionados.
   *
   * <p>Versão otimizada que calcula apenas a quantidade sem gerar a lista completa.
   * Usado para validação de limite de ocorrências.
   *
   * @param startDate Data inicial (inclusiva)
   * @param endDate Data final (inclusiva)
   * @param selectedDaysOfWeek Dias da semana selecionados (null ou vazio = todos os dias)
   * @return Quantidade de datas aplicáveis
   */
  private long countApplicableDates(
          LocalDate startDate,
          LocalDate endDate,
          Set<DayOfWeek> selectedDaysOfWeek) {

    Stream<LocalDate> dateStream = startDate.datesUntil(endDate.plusDays(1));

    // Se não há dias da semana selecionados, conta todas as datas
    if (selectedDaysOfWeek == null || selectedDaysOfWeek.isEmpty()) {
      return dateStream.count();
    }

    // Conta apenas os dias da semana selecionados
    return dateStream
            .filter(date -> selectedDaysOfWeek.contains(DayOfWeek.convertToDayOfWeek(date)))
            .count();
  }

  /**
   * Valida se todos os dias da semana selecionados estão dentro do range de datas fornecido.
   *
   * <p>Para cada dia selecionado, verifica se existe pelo menos uma data no intervalo
   * que corresponda àquele dia da semana.
   *
   * @param selectedDaysOfWeek Dias da semana selecionados pelo usuário
   * @param startDate Data inicial do intervalo
   * @param endDate Data final do intervalo
   * @throws InvalidBlockDateException se algum dia selecionado não estiver presente no range de datas
   */
  private void validateSelectedDaysWithinDateRange(
          Set<DayOfWeek> selectedDaysOfWeek,
          LocalDate startDate,
          LocalDate endDate) {

    Set<DayOfWeek> daysInRange = getDaysOfWeekInRange(startDate, endDate);

    Set<DayOfWeek> invalidDays = selectedDaysOfWeek.stream()
            .filter(day -> !daysInRange.contains(day))
            .collect(Collectors.toSet());

    if (!invalidDays.isEmpty()) {
      throw new InvalidBlockDateException(ErrorCode.BLOCKED_TIME_SELECTED_DAYS_OUTSIDE_DATE_RANGE);
    }
  }

  /**
   * Valida se o intervalo fornecido está completamente contido dentro do horário
   * de funcionamento de TODOS os dias da semana selecionados.
   *
   * <p>Para cada dia da semana nos horários de funcionamento fornecidos, verifica se
   * existe pelo menos um OperatingHours daquele dia que contenha o intervalo solicitado.
   *
   * <p>Se o intervalo não estiver válido para algum dia, lança exceção informando
   * quais dias estão com problema.
   *
   * @param interval Intervalo a ser validado
   * @param operatingHoursList Lista de horários de funcionamento
   * @param effectiveDaysOfWeek Dias da semana que devem ser validados
   * @throws InvalidBlockDateException se o intervalo não estiver dentro do horáriode funcionamento de algum dia
   */
  private void validateIntervalWithinOperatingHours(
      TimeInterval interval,
      List<OperatingHours> operatingHoursList,
      Set<DayOfWeek> effectiveDaysOfWeek) {

    // Para cada dia da semana, verifica se o intervalo está dentro de algum OperatingHours daquele dia
    for (DayOfWeek day : effectiveDaysOfWeek) {

      boolean isValidForDay = operatingHoursList.stream()
          .filter(oh -> oh.getDaysOfWeek().contains(day))
          .map(OperatingHours::getTimeInterval)
          .anyMatch(ohInterval -> ohInterval.containsInterval(interval));

      if (!isValidForDay) {
        throw new InvalidBlockDateException(ErrorCode.BLOCKED_TIME_OUTSIDE_OPERATING_HOURS);
      }
    }
  }

  /**
   * Valida se o número total de ocorrências excede o limite permitido.
   *
   * @param courtsCount Número de quadras selecionadas
   * @param datesCount Número de datas aplicáveis
   * @throws InvalidBlockDateException se o total de ocorrências exceder {@value #MAX_BLOCKED_TIME_OCCURRENCES}
   */
  private void validateOccurrencesLimit(int courtsCount, int datesCount) {
    int totalOccurrences = courtsCount * datesCount;
    if (totalOccurrences > MAX_BLOCKED_TIME_OCCURRENCES) {
      throw new InvalidBlockDateException(ErrorCode.BLOCKED_TIME_TOO_MANY_OCCURRENCES);
    }
  }

  /**
   * Obtém os dias da semana presentes no intervalo de datas fornecido.
   *
   * @param startDate Data de início
   * @param endDate Data de fim
   * @return Conjunto de DayOfWeek representando os dias da semana no intervalo
   */
  private Set<DayOfWeek> getDaysOfWeekInRange(LocalDate startDate, LocalDate endDate) {
    return startDate
        .datesUntil(endDate.plusDays(1))
        .map(DayOfWeek::convertToDayOfWeek)
        .collect(Collectors.toSet());
  }

  /**
   * Calcula o horário de início mínimo a partir da lista de horários de funcionamento.
   *
   * @param hoursList Lista de OperatingHours
   * @return LocalTime representando o horário de início mínimo
   * @throws OperatingHoursNotFoundException se a lista estiver vazia
   */
  private LocalTime calculateMinStart(List<OperatingHours> hoursList) {
    return hoursList.stream()
        .map(oh -> oh.getTimeInterval().startTime())
        .min(LocalTime::compareTo)
        .orElseThrow(() -> new OperatingHoursNotFoundException(ErrorCode.OPERATING_HOURS_APPLICABLE_NOT_FOUND));
  }

  /**
   * Calcula o horário de término máximo a partir da lista de horários de funcionamento.
   *
   * @param hoursList Lista de OperatingHours
   * @return LocalTime representando o horário de término máximo
   * @throws OperatingHoursNotFoundException se a lista estiver vazia
   */
  private LocalTime calculateMaxEnd(List<OperatingHours> hoursList) {
    return hoursList.stream()
        .map(oh -> oh.getTimeInterval().endTime())
        .max(LocalTime::compareTo)
        .orElseThrow(
            () ->
                new OperatingHoursNotFoundException(ErrorCode.OPERATING_HOURS_APPLICABLE_NOT_FOUND));
  }
}

