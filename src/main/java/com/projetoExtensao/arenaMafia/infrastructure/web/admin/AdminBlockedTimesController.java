package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.result.ConfirmBlockedTimeResult;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.ConfirmBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.PreviewBlockedTimeConflictsUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConfirmRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response.BlockedTimeConfirmResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response.BlockedTimeConflictsPreviewResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.BlockedTimeDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ScheduleEntryResponseMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/blocked-times")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlockedTimesController {

  private final PreviewBlockedTimeConflictsUseCase previewBlockedTimeConflictsUseCase;
  private final ConfirmBlockedTimeUseCase confirmBlockedTimeUseCase;
  private final ScheduleEntryResponseMapper scheduleEntryMapper;

  public AdminBlockedTimesController(
      PreviewBlockedTimeConflictsUseCase previewBlockedTimeConflictsUseCase,
      ConfirmBlockedTimeUseCase confirmBlockedTimeUseCase,
      ScheduleEntryResponseMapper scheduleEntryMapper) {
    this.previewBlockedTimeConflictsUseCase = previewBlockedTimeConflictsUseCase;
    this.confirmBlockedTimeUseCase = confirmBlockedTimeUseCase;
    this.scheduleEntryMapper = scheduleEntryMapper;
  }

  @PostMapping("/preview-conflicts")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<BlockedTimeConflictsPreviewResponseDto> previewConflicts(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin,
      @RequestBody @Valid BlockedTimeConflictsPreviewRequestDto requestDto) {

    UUID adminId = authenticatedAdmin.getUser().getId();
    BlockedTimeConflictsPreview preview = previewBlockedTimeConflictsUseCase.execute(requestDto, adminId);
    BlockedTimeConflictsPreviewResponseDto response = buildPreviewResponseDto(preview);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/confirm")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<BlockedTimeConfirmResponseDto> createBlockedTime(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin,
      @RequestBody @Valid BlockedTimeConfirmRequestDto requestDto) {

    UUID adminId = authenticatedAdmin.getUser().getId();
    ConfirmBlockedTimeResult result = confirmBlockedTimeUseCase.execute(adminId, requestDto);
    BlockedTimeConfirmResponseDto responseDto = buildConfirmResponseDto(result);

    return ResponseEntity.ok(responseDto);
  }

  /**
   * Monta o DTO de resposta a partir do preview de conflitos.
   *
   * @param preview O preview de conflitos gerado pelo caso de uso.
   * @return O DTO de resposta contendo os detalhes dos conflitos.
   */
  private BlockedTimeConflictsPreviewResponseDto buildPreviewResponseDto(BlockedTimeConflictsPreview preview) {
    List<BlockedTimeDetailResponseDto> blockedTimesDetails =
            scheduleEntryMapper.toDetailDtoList(preview.conflictingBlockedTimes());

    List<ReservationDetailResponseDto> reservationsDetails =
            scheduleEntryMapper.toDetailDtoList(preview.conflictingReservations());

    List<ReservationDetailResponseDto> inProgressReservationsDetails =
            scheduleEntryMapper.toDetailDtoList(preview.inProgressReservations());

    return new BlockedTimeConflictsPreviewResponseDto(
        preview.previewKey(),
        preview.usersAffected(),
        preview.blockedTimesAffected(),
        preview.reservationsAffected(),
        blockedTimesDetails,
        reservationsDetails,
        inProgressReservationsDetails);
  }

  /**
   * Monta o DTO de resposta a partir do resultado da confirmação de criação de bloqueio.
   *
   * @param result O resultado da confirmação gerado pelo caso de uso.
   * @return O DTO de resposta contendo os detalhes da operação.
   */
  private BlockedTimeConfirmResponseDto buildConfirmResponseDto(ConfirmBlockedTimeResult result) {
    return new BlockedTimeConfirmResponseDto(
        result.blockedTimesCreated(),
        result.totalBlockedTimesCreated(),
        result.reservationsCancelled(),
        result.blockedTimesCancelled(),
        result.usersAffected()
    );
  }

  // TODO: Implementar endpoint DELETE para deletar um ou todos horários bloqueados

  // TODO: Implementar endpoint GET para listar todos os horários bloqueados

  // TODO: Implementar endpoint GET para obter detalhes de um horário bloqueado específico

  // TODO: Implementar endpoint PUT para atualizar horários bloqueados existentes

}
