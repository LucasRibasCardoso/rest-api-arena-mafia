package com.projetoExtensao.arenaMafia.infrastructure.web.agenda.mapper;

import com.projetoExtensao.arenaMafia.domain.model.agenda.user.AgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.user.AvailableSlotAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.user.GroupedBlockedTimeAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.user.ScheduleEntryAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.PublicAgendaItemResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.PublicAvailableItemResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.PublicScheduleEntryResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import org.springframework.stereotype.Component;

@Component
public class AgendaMapper {

  public PublicAgendaItemResponseDto toDto(AgendaItem agendaItem) {
    return switch (agendaItem) {
      case ScheduleEntryAgendaItem scheduleEntry -> mapScheduleEntry(scheduleEntry);
      case AvailableSlotAgendaItem availableSlot -> mapAvailableSlot(availableSlot);
      case GroupedBlockedTimeAgendaItem groupedBlockedTime ->
          mapGroupedBlockedTime(groupedBlockedTime);
    };
  }

  private PublicScheduleEntryResponseDto mapScheduleEntry(
      ScheduleEntryAgendaItem scheduleEntryAgendaItem) {
    return switch (scheduleEntryAgendaItem.scheduleEntry()) {
      case Reservation reservation -> mapReservation(reservation);
      case BlockedTime blockedTime -> mapBlockedTime(blockedTime);
      default -> throw new IllegalStateException("Tipo de ScheduleEntry não suportado.");
    };
  }

  private PublicScheduleEntryResponseDto mapReservation(Reservation reservation) {
    TimeIntervalDto timeInterval = toTimeIntervalDto(reservation.getDateTimeSlot().timeInterval());
    return new PublicScheduleEntryResponseDto(timeInterval, ScheduleEntryType.RESERVATION, null);
  }

  private PublicScheduleEntryResponseDto mapBlockedTime(BlockedTime blockedTime) {
    TimeIntervalDto timeInterval = toTimeIntervalDto(blockedTime.getDateTimeSlot().timeInterval());

    return new PublicScheduleEntryResponseDto(
        timeInterval, ScheduleEntryType.BLOCKED_TIME, blockedTime.getDescription());
  }

  private PublicAvailableItemResponseDto mapAvailableSlot(
      AvailableSlotAgendaItem availableSlotAgendaItem) {
    TimeIntervalDto timeInterval = toTimeIntervalDto(availableSlotAgendaItem.timeInterval());

    return new PublicAvailableItemResponseDto(
        timeInterval, availableSlotAgendaItem.modalityIds(), availableSlotAgendaItem.price());
  }

  private PublicScheduleEntryResponseDto mapGroupedBlockedTime(
      GroupedBlockedTimeAgendaItem groupedBlockedTime) {
    TimeIntervalDto timeInterval = toTimeIntervalDto(groupedBlockedTime.timeInterval());
    return new PublicScheduleEntryResponseDto(
        timeInterval, ScheduleEntryType.BLOCKED_TIME, groupedBlockedTime.description());
  }

  private TimeIntervalDto toTimeIntervalDto(TimeInterval timeInterval) {
    return new TimeIntervalDto(timeInterval.startTime(), timeInterval.endTime());
  }
}
