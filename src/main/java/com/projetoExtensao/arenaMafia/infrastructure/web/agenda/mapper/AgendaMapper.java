package com.projetoExtensao.arenaMafia.infrastructure.web.agenda.mapper;

import com.projetoExtensao.arenaMafia.domain.model.agenda.AgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.AvailableSlotAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.GroupedAvailableSlotAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.GroupedBlockedTimeAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.agenda.ScheduleEntryAgendaItem;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.AgendaSlotResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.enums.AgendaSlotType;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
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
      case GroupedBlockedTimeAgendaItem groupedBlockedTime ->
          mapGroupedBlockedTime(groupedBlockedTime);
    };
  }

  /**
   * Mapeia um ScheduleEntry (Reservation ou BlockedTime) para DTO.
   *
   * @param scheduleEntry entrada da agenda (Reservation ou BlockedTime)
   * @return DTO com tipo correto (RESERVED ou BLOCKED_TIME)
   */
  private AgendaSlotResponseDto mapScheduleEntry(ScheduleEntry scheduleEntry) {
    return switch (scheduleEntry) {
      case Reservation reservation -> mapReservation(reservation);
      case BlockedTime blockedTime -> mapBlockedTime(blockedTime);
      default ->
          throw new IllegalStateException(
              "Tipo de ScheduleEntry não suportado: " + scheduleEntry.getClass().getName());
    };
  }

  /**
   * Mapeia uma Reservation para DTO público (sem dados sensíveis).
   *
   * @param reservation reserva a ser mapeada
   * @return DTO com tipo RESERVED
   */
  private AgendaSlotResponseDto mapReservation(Reservation reservation) {
    TimeIntervalDto timeInterval = toTimeIntervalDto(reservation.getDateTimeSlot().timeInterval());

    return new AgendaSlotResponseDto(
        reservation.getCourtId(),
        timeInterval,
        AgendaSlotType.RESERVED,
        null, // availableModalityIds
        null // description
        );
  }

  /**
   * Mapeia um BlockedTime para DTO público.
   *
   * @param blockedTime horário bloqueado a ser mapeado
   * @return DTO com tipo BLOCKED_TIME e descrição
   */
  private AgendaSlotResponseDto mapBlockedTime(BlockedTime blockedTime) {
    TimeIntervalDto timeInterval = toTimeIntervalDto(blockedTime.getDateTimeSlot().timeInterval());

    return new AgendaSlotResponseDto(
        blockedTime.getCourtId(),
        timeInterval,
        AgendaSlotType.BLOCKED_TIME,
        null, // availableModalityIds
        blockedTime.getDescription());
  }

  /**
   * Mapeia um AvailableSlotAgendaItem (horário disponível individual) para DTO.
   *
   * @param availableSlotAgendaItem slot disponível individual
   * @return DTO com tipo AVAILABLE
   */
  private AgendaSlotResponseDto mapAvailableSlot(AvailableSlotAgendaItem availableSlotAgendaItem) {
    TimeIntervalDto timeInterval =
        toTimeIntervalDto(availableSlotAgendaItem.availableSlot().timeInterval());

    return new AgendaSlotResponseDto(
        availableSlotAgendaItem.availableSlot().courtId(),
        timeInterval,
        AgendaSlotType.AVAILABLE,
        null, // availableModalityIds
        null // description
        );
  }

  /**
   * Mapeia um GroupedAvailableSlotAgendaItem (horários agrupados por modalidade) para DTO.
   *
   * @param groupedSlot slots agrupados por modalidade
   * @return DTO com tipo AVAILABLE e IDs das modalidades disponíveis
   */
  private AgendaSlotResponseDto mapGroupedAvailableSlot(
      GroupedAvailableSlotAgendaItem groupedSlot) {
    TimeIntervalDto timeInterval = toTimeIntervalDto(groupedSlot.timeInterval());

    return new AgendaSlotResponseDto(
        null, // courtId (agrupado, sem quadra específica)
        timeInterval,
        AgendaSlotType.AVAILABLE,
        groupedSlot.availableModalityIds(),
        null // description
        );
  }

  /**
   * Mapeia um GroupedBlockedTimeAgendaItem (bloqueios agrupados pelo mesmo horário e descrição)
   * para DTO.
   *
   * <p>Quando múltiplas quadras são bloqueadas no mesmo horário com a mesma descrição, este metodo
   * agrupa os bloqueios em um único item sem especificar a quadra.
   *
   * @param groupedBlockedTime bloqueios agrupados
   * @return DTO com tipo BLOCKED_TIME, sem courtId (agrupado) e com descrição
   */
  private AgendaSlotResponseDto mapGroupedBlockedTime(
      GroupedBlockedTimeAgendaItem groupedBlockedTime) {
    TimeIntervalDto timeInterval = toTimeIntervalDto(groupedBlockedTime.timeInterval());

    return new AgendaSlotResponseDto(
        null, // courtId (agrupado, múltiplas quadras bloqueadas)
        timeInterval,
        AgendaSlotType.BLOCKED_TIME,
        null, // availableModalityIds
        groupedBlockedTime.description());
  }

  /**
   * Converte um TimeInterval (VO de domínio) para TimeIntervalDto (DTO de infraestrutura).
   *
   * @param timeInterval value object de intervalo de tempo
   * @return DTO com horário de início e fim
   */
  private TimeIntervalDto toTimeIntervalDto(TimeInterval timeInterval) {
    return new TimeIntervalDto(timeInterval.startTime(), timeInterval.endTime());
  }
}
