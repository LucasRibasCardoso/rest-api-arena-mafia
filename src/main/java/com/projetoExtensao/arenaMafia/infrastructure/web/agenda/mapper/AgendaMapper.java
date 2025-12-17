package com.projetoExtensao.arenaMafia.infrastructure.web.agenda.mapper;

import com.projetoExtensao.arenaMafia.domain.model.agenda.AgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.AvailableSlotAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.GroupedAvailableSlotAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.ScheduleEntryAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.AgendaSlotResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.enums.AgendaSlotType;
import org.springframework.stereotype.Component;

@Component
public class AgendaMapper {

  /**
   * Converte um AgendaItem para DTO de agenda pública (sem dados sensíveis).
   *
   * @param agendaItem item da agenda (ScheduleEntry ou AvailableSlot)
   * @return DTO com informações públicas apenas
   */
  public AgendaSlotResponseDto toPublicDto(AgendaItem agendaItem) {
    return switch (agendaItem) {
      case ScheduleEntryAgendaItem scheduleEntry -> mapScheduleEntry(scheduleEntry.scheduleEntry());
      case AvailableSlotAgendaItem availableSlot -> mapAvailableSlot(availableSlot);
      case GroupedAvailableSlotAgendaItem groupedSlot -> mapGroupedAvailableSlot(groupedSlot);
    };
  }

  private AgendaSlotResponseDto mapScheduleEntry(ScheduleEntry scheduleEntry) {
    AgendaSlotType slotType = determineSlotType(scheduleEntry);
    TimeIntervalDto timeInterval =
        new TimeIntervalDto(
            scheduleEntry.getDateTimeSlot().timeInterval().startTime(),
            scheduleEntry.getDateTimeSlot().timeInterval().endTime());

    return new AgendaSlotResponseDto(scheduleEntry.getCourtId(), timeInterval, slotType, null);
  }

  private AgendaSlotResponseDto mapAvailableSlot(AvailableSlotAgendaItem availableSlotAgendaItem) {
    TimeIntervalDto timeInterval =
        new TimeIntervalDto(
            availableSlotAgendaItem.availableSlot().timeInterval().startTime(),
            availableSlotAgendaItem.availableSlot().timeInterval().endTime());

    return new AgendaSlotResponseDto(
        availableSlotAgendaItem.availableSlot().courtId(),
        timeInterval,
        AgendaSlotType.AVAILABLE,
        null);
  }

  private AgendaSlotResponseDto mapGroupedAvailableSlot(GroupedAvailableSlotAgendaItem groupedSlot) {
    TimeIntervalDto timeInterval =
        new TimeIntervalDto(
            groupedSlot.timeInterval().startTime(), groupedSlot.timeInterval().endTime());

    return new AgendaSlotResponseDto(
        null,
        timeInterval,
        AgendaSlotType.AVAILABLE,
        groupedSlot.availableModalityIds());
  }

  private AgendaSlotType determineSlotType(ScheduleEntry scheduleEntry) {
    if (scheduleEntry instanceof Reservation) {
      return AgendaSlotType.RESERVED;
    }
    // Futuros tipos:
    // if (scheduleEntry instanceof Training) return AgendaSlotType.TRAINING;
    // if (scheduleEntry instanceof BlockedTime) return AgendaSlotType.BLOCKED;

    return AgendaSlotType.RESERVED; // Fallback
  }
}
