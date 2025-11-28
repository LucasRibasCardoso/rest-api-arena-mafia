package com.projetoExtensao.arenaMafia.application.schedule.service;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ScheduleConflictException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ScheduleAvailabilityService {

  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;

  public ScheduleAvailabilityService(ScheduleEntryRepositoryPort scheduleEntryRepositoryPort) {
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
  }

  /**
   * Valida se um horário específico está disponível para reserva. Lança exceção se houver conflito
   * com outras reservas confirmadas.
   *
   * @param courtId ID da quadra
   * @param date data da reserva
   * @param timeInterval intervalo de tempo desejado
   * @throws ScheduleConflictException se o horário já estiver ocupado
   */
  public void validateAvailability(UUID courtId, LocalDate date, TimeInterval timeInterval) {
    List<ScheduleEntry> confirmedSchedules =
        scheduleEntryRepositoryPort.findConfirmedSchedulesByCourtAndDate(courtId, date);

    boolean isOccupied = isSlotOccupied(timeInterval, confirmedSchedules);

    if (isOccupied) {
      throw new ScheduleConflictException(ErrorCode.SCHEDULE_ENTRY_NOT_AVAILABLE);
    }
  }

  /**
   * Verifica se um slot está ocupado por alguma reserva confirmada. Este método público é útil
   * quando você já possui a lista de schedules carregados e quer evitar múltiplas consultas ao
   * banco de dados.
   *
   * @param slot intervalo de tempo a verificar
   * @param confirmedSchedules lista de reservas confirmadas já carregadas
   * @return true se o slot está ocupado (há sobreposição)
   */
  public boolean isSlotOccupied(TimeInterval slot, List<ScheduleEntry> confirmedSchedules) {
    return confirmedSchedules.stream()
        .anyMatch(schedule -> slot.overlaps(schedule.getDateTimeSlot().timeInterval()));
  }
}
