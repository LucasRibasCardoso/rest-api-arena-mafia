package com.projetoExtensao.arenaMafia.infrastructure.web.admin.mapper;

import com.projetoExtensao.arenaMafia.domain.model.agenda.admin.AdminAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.admin.AdminAvailableSlotAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.admin.AdminScheduleEntryAgendaItem;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.agenda.response.AdminAgendaSlotResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.agenda.response.AdminAvailableSlotResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.agenda.response.AdminScheduleDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ScheduleEntryResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class AdminAgendaMapper {

  private final ScheduleEntryResponseMapper scheduleEntryMapper;

  public AdminAgendaMapper(ScheduleEntryResponseMapper scheduleEntryMapper) {
    this.scheduleEntryMapper = scheduleEntryMapper;
  }

  public AdminAgendaSlotResponseDto toDto(AdminAgendaItem item) {
    return switch (item) {
      case AdminAvailableSlotAgendaItem availableSlotAgendaItem -> mapAdminAvailableSlot(availableSlotAgendaItem);
      case AdminScheduleEntryAgendaItem scheduleEntryAgendaItem -> mapScheduleDetailSlot(scheduleEntryAgendaItem);
    };
  }

  private AdminAvailableSlotResponseDto mapAdminAvailableSlot(AdminAvailableSlotAgendaItem availableSlot) {
    TimeInterval timeInterval = availableSlot.timeInterval();
    TimeIntervalDto timeIntervalDto = new TimeIntervalDto(timeInterval.startTime(), timeInterval.endTime());
    return new AdminAvailableSlotResponseDto(
        availableSlot.courtId(),
        availableSlot.courtName(),
        timeIntervalDto,
        availableSlot.availableModalityIds(),
        availableSlot.price());
  }

  private AdminScheduleDetailResponseDto mapScheduleDetailSlot(AdminScheduleEntryAgendaItem scheduleEntry) {
    return new AdminScheduleDetailResponseDto(scheduleEntryMapper.toDetailDto(scheduleEntry.detail()));
  }
}
