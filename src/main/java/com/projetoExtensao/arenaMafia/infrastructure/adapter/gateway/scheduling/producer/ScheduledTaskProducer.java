package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.scheduling.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.application.scheduleTask.gateway.ScheduledTaskPort;
import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.scheduling.dto.ScheduledReminderTaskDto;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.scheduling.dto.ScheduledTaskDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

@Component
public class ScheduledTaskProducer implements ScheduledTaskPort {

  private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskProducer.class);
  private static final String TIME_ZONE = "America/Sao_Paulo";

  private static final String TASK_PREFIX = "task-";
  private static final String REMINDER_PREFIX = "reminder-reservation-";

  private final String roleArn;
  private final String groupName;
  private final String taskQueueArn;
  private final String reminderQueueArn;
  private final ObjectMapper objectMapper;
  private final SchedulerClient schedulerClient;

  public ScheduledTaskProducer(
      @Value("${app.aws.scheduler.role-arn}") String roleArn,
      @Value("${app.aws.scheduler.group-name}") String groupName,
      @Value("${app.aws.queue-arn.schedule-task}") String taskQueueArn,
      @Value("${app.aws.queue-arn.whatsapp-reminder}") String reminderQueueArn,
      ObjectMapper objectMapper,
      SchedulerClient schedulerClient) {
    this.roleArn = roleArn;
    this.groupName = groupName;
    this.taskQueueArn = taskQueueArn;
    this.reminderQueueArn = reminderQueueArn;
    this.objectMapper = objectMapper;
    this.schedulerClient = schedulerClient;
  }

  @Override
  public void scheduleTask(
      UUID scheduleEntryId, ScheduleEntryType scheduleEntryType, LocalDateTime executionTime) {
    String scheduleName = generateTaskName(scheduleEntryId, scheduleEntryType);
    String payload = toJson(new ScheduledTaskDto(scheduleEntryId, scheduleEntryType));

    createOneTimeSchedule(scheduleName, executionTime, taskQueueArn, payload);
    logger.info(
        "Agendada task {} ({}) para executar às {}",
        scheduleEntryType,
        scheduleEntryId,
        executionTime);
  }

  @Override
  public void cancelTask(UUID scheduleEntryId, ScheduleEntryType scheduleEntryType) {
    String scheduleName = generateTaskName(scheduleEntryId, scheduleEntryType);
    deleteScheduleFromAws(scheduleName);
  }

  @Override
  public void scheduleReservationReminderTask(UUID reservationId, LocalDateTime executionTime) {
    String scheduleName = REMINDER_PREFIX + reservationId;
    String payload = toJson(new ScheduledReminderTaskDto(reservationId));

    createOneTimeSchedule(scheduleName, executionTime, reminderQueueArn, payload);
    logger.info(
        "Agendado lembrete de WhatsApp da reserva {} para disparar às {}",
        reservationId,
        executionTime);
  }

  @Override
  public void cancelReservationReminderTask(UUID reservationId) {
    String scheduleName = REMINDER_PREFIX + reservationId;
    deleteScheduleFromAws(scheduleName);
  }

  private void deleteScheduleFromAws(String scheduleName) {
    try {
      DeleteScheduleRequest request =
          DeleteScheduleRequest.builder().name(scheduleName).groupName(groupName).build();

      schedulerClient.deleteSchedule(request);
      logger.info("Agendamento {} cancelado com sucesso no AWS EventBridge", scheduleName);

    } catch (ResourceNotFoundException e) {
      logger.debug("Agendamento {} não encontrado na AWS.", scheduleName);
    } catch (Exception e) {
      logger.error("Erro ao cancelar agendamento {} na AWS: {}", scheduleName, e.getMessage());
    }
  }

  private String generateTaskName(UUID id, ScheduleEntryType type) {
    return TASK_PREFIX + type.name().toLowerCase() + "-" + id.toString();
  }

  private void createOneTimeSchedule(
      String scheduleName, LocalDateTime executionTime, String targetArn, String payload) {

    try {
      // Formato exigido pela AWS EventBridge Scheduler: at(yyyy-MM-dd'T'HH:mm:ss)
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
      String scheduleExpression = "at(" + executionTime.format(formatter) + ")";

      Target target = Target.builder().arn(targetArn).roleArn(roleArn).input(payload).build();

      FlexibleTimeWindow timeWindow =
          FlexibleTimeWindow.builder().mode(FlexibleTimeWindowMode.OFF).build();

      CreateScheduleRequest request =
          CreateScheduleRequest.builder()
              .name(scheduleName)
              .groupName(groupName)
              .scheduleExpression(scheduleExpression)
              .scheduleExpressionTimezone(TIME_ZONE)
              .target(target)
              .flexibleTimeWindow(timeWindow)
              .actionAfterCompletion(ActionAfterCompletion.DELETE)
              .build();

      schedulerClient.createSchedule(request);

    } catch (Exception e) {
      logger.error(
          "FALHA CRÍTICA ao criar agendamento {} na AWS: {}", scheduleName, e.getMessage());
      throw new RuntimeException("Erro de comunicação com AWS EventBridge", e);
    }
  }

  private String toJson(Object dto) {
    try {
      return objectMapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao serializar payload para o agendamento AWS", e);
    }
  }
}
