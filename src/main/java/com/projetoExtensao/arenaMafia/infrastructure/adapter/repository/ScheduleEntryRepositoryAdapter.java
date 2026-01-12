package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ScheduleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ScheduleEntryEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ScheduleEntryMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ScheduleEntryJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class ScheduleEntryRepositoryAdapter implements ScheduleEntryRepositoryPort {

  private final ScheduleEntryMapper scheduleEntryMapper;
  private final ScheduleEntryJpaRepository scheduleEntryJpaRepository;

  public ScheduleEntryRepositoryAdapter(
      ScheduleEntryMapper scheduleEntryMapper,
      ScheduleEntryJpaRepository scheduleEntryJpaRepository) {
    this.scheduleEntryMapper = scheduleEntryMapper;
    this.scheduleEntryJpaRepository = scheduleEntryJpaRepository;
  }

  @Override
  public ScheduleEntry save(ScheduleEntry scheduleEntry) {
    ScheduleEntryEntity entity = scheduleEntryMapper.toEntity(scheduleEntry);
    ScheduleEntryEntity savedEntity = scheduleEntryJpaRepository.save(entity);
    return scheduleEntryMapper.toDomain(savedEntity);
  }

  @Override
  public ScheduleEntry findByIdOrElseThrow(UUID id) {
    return scheduleEntryJpaRepository
        .findById(id)
        .map(scheduleEntryMapper::toDomain)
        .orElseThrow(() -> new ScheduleNotFoundException(ErrorCode.SCHEDULE_ENTRY_NOT_FOUND));
  }

  @Override
  public List<ScheduleEntry> findAllActiveSchedulesByCourtAndDate(UUID courtId, LocalDate date) {
    return scheduleEntryJpaRepository
        .findSchedulesByCourtAndDate(courtId, date)
        .stream()
        .map(scheduleEntryMapper::toDomain)
        .filter(ScheduleEntry::isActive)
        .toList();
  }

  @Override
  public List<ScheduleEntry> findAllActiveSchedulesByDate(LocalDate date) {
    return scheduleEntryJpaRepository.findAllSchedulesByDate(date).stream()
        .map(scheduleEntryMapper::toDomain)
        .filter(ScheduleEntry::isActive)
        .toList();
  }

  @Override
  public List<ScheduleEntry> findAllActiveSchedulesByCourtIdFromToday(UUID courtId) {
    LocalDate today = LocalDate.now();
    return scheduleEntryJpaRepository
        .findAllSchedulesByCourtIdAfterDate(courtId, today)
        .stream()
        .map(scheduleEntryMapper::toDomain)
        .filter(ScheduleEntry::isActive)
        .toList();
  }

  @Override
  public List<ScheduleEntry> findAllActiveSchedulesConflicts(
      List<UUID> courtIds,
      LocalDate startDate,
      LocalDate endDate,
      TimeInterval timeInterval,
      Set<DayOfWeek> selectedDaysOfWeek) {

    Set<Integer> valuesDaysOfWeek = null;
    if (selectedDaysOfWeek != null && !selectedDaysOfWeek.isEmpty()) {
      valuesDaysOfWeek = selectedDaysOfWeek.stream()
          .map(DayOfWeek::getDayOfWeekValue)
          .collect(Collectors.toSet());
    }

    // Buscar agendamentos filtrados por data e dia da semana (no SQL)
    List<ScheduleEntryEntity> entities = scheduleEntryJpaRepository.findConflictingSchedules(
        courtIds,
        startDate,
        endDate,
        selectedDaysOfWeek,
        valuesDaysOfWeek
    );

    return entities.stream()
        .map(scheduleEntryMapper::toDomain)
        .filter(ScheduleEntry::isActive)
        .filter(scheduleEntry -> scheduleEntry.getDateTimeSlot().timeInterval().overlaps(timeInterval))
        .toList();
  }

  @Override
  public List<ScheduleEntry> findAllActiveSchedulesFromTodayByDaysOfWeekAndTimeInterval(
          Set<DayOfWeek> daysOfWeek,
          TimeInterval timeInterval) {

    // Se nulo/vazio, pega todos os dias
    Set<DayOfWeek> searchDays = (daysOfWeek == null || daysOfWeek.isEmpty())
            ? Set.of(DayOfWeek.values())
            : daysOfWeek;

    // Converte de DayOfWeek para valores inteiros que representam os dias
    Set<Integer> valuesDaysWeek = searchDays.stream()
            .map(DayOfWeek::getDayOfWeekValue)
            .collect(Collectors.toSet());

    LocalTime startTime = timeInterval.startTime();
    LocalTime endTime = timeInterval.endTime();
    LocalDate today = LocalDate.now();

    if (!timeInterval.crossesMidnight()) {
      // Cenário Simples
      return scheduleEntryJpaRepository
          .findSchedulesByDaysOfWeekAndTimeInterval(today, valuesDaysWeek, startTime, endTime)
          .stream()
          .map(scheduleEntryMapper::toDomain)
          .filter(ScheduleEntry::isActive)
          .toList();
    } else {
      // CENÁRIO COMPLEXO (Ex: 22:00 as 02:00)

      // Parte A: Verificar o final da noite nos dias originais (startTime -> 23:59)
      List<ScheduleEntryEntity> schedulesBeforeMidnight =
          scheduleEntryJpaRepository
              .findSchedulesByDaysOfWeekAndTimeInterval(today, valuesDaysWeek, startTime, LocalTime.MAX)
              .stream()
              .toList();

      // Parte B: Verificar o início da manhã no dia seguinte (00:00 -> endTime)
      Set<Integer> valuesNextDays = searchDays.stream()
              .map(DayOfWeek::next)
              .map(DayOfWeek::getDayOfWeekValue)
              .collect(Collectors.toSet());

      List<ScheduleEntryEntity> scheduleAfterMidnight =
              scheduleEntryJpaRepository
                      .findSchedulesByDaysOfWeekAndTimeInterval(today, valuesNextDays, LocalTime.MIN, endTime)
                      .stream()
                      .toList();

      // Combina as duas listas de agendamentos
      return Stream.concat(schedulesBeforeMidnight.stream(), scheduleAfterMidnight.stream())
          .map(scheduleEntryMapper::toDomain)
          .filter(ScheduleEntry::isActive)
          .toList();
    }
  }
}
