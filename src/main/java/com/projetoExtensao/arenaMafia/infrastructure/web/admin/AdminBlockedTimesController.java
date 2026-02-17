package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.result.ConfirmBlockedTimeResult;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.ConfirmBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.DeleteBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.FindAllBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.PreviewBlockedTimeConflictsUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.UpdateBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConfirmRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeUpdateRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response.BlockedTimeConfirmResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response.BlockedTimeConflictsPreviewResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.BlockedTimeDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ScheduleEntryResponseMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/blocked-times")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlockedTimesController {

  private final ScheduleEntryResponseMapper scheduleEntryMapper;
  private final ConfirmBlockedTimeUseCase confirmBlockedTimeUseCase;
  private final PreviewBlockedTimeConflictsUseCase previewBlockedTimeConflictsUseCase;
  private final FindAllBlockedTimeUseCase findAllBlockedTimeUseCase;
  private final UpdateBlockedTimeUseCase updateBlockedTimeUseCase;
  private final DeleteBlockedTimeUseCase deleteBlockedTimeUseCase;

  public AdminBlockedTimesController(
      ScheduleEntryResponseMapper scheduleEntryMapper,
      ConfirmBlockedTimeUseCase confirmBlockedTimeUseCase,
      PreviewBlockedTimeConflictsUseCase previewBlockedTimeConflictsUseCase,
      FindAllBlockedTimeUseCase findAllBlockedTimeUseCase,
      UpdateBlockedTimeUseCase updateBlockedTimeUseCase,
      DeleteBlockedTimeUseCase deleteBlockedTimeUseCase) {
    this.scheduleEntryMapper = scheduleEntryMapper;
    this.confirmBlockedTimeUseCase = confirmBlockedTimeUseCase;
    this.previewBlockedTimeConflictsUseCase = previewBlockedTimeConflictsUseCase;
    this.findAllBlockedTimeUseCase = findAllBlockedTimeUseCase;
    this.updateBlockedTimeUseCase = updateBlockedTimeUseCase;
    this.deleteBlockedTimeUseCase = deleteBlockedTimeUseCase;
  }

  @PostMapping("/preview-conflicts")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<BlockedTimeConflictsPreviewResponseDto> previewConflictsToCreateBlockedTime(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin,
      @RequestBody @Valid BlockedTimeConflictsPreviewRequestDto requestDto) {

    UUID adminId = authenticatedAdmin.user().getId();
    BlockedTimeConflictsPreview preview =
        previewBlockedTimeConflictsUseCase.execute(requestDto, adminId);
    BlockedTimeConflictsPreviewResponseDto response = buildPreviewResponseDto(preview);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/confirm")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<BlockedTimeConfirmResponseDto> createBlockedTime(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin,
      @RequestBody @Valid BlockedTimeConfirmRequestDto requestDto) {

    UUID adminId = authenticatedAdmin.user().getId();
    ConfirmBlockedTimeResult result = confirmBlockedTimeUseCase.execute(adminId, requestDto);
    BlockedTimeConfirmResponseDto responseDto = buildConfirmResponseDto(result);

    return ResponseEntity.ok(responseDto);
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Page<BlockedTimeDetailResponseDto>> getBlockedTimes(
      @RequestParam(required = false) UUID courtId, Pageable pageable) {
    Page<BlockedTimeDetail> blockedTimesPage = findAllBlockedTimeUseCase.execute(courtId, pageable);
    Page<BlockedTimeDetailResponseDto> responsePage =
        blockedTimesPage.map(scheduleEntryMapper::toDetailDto);
    return ResponseEntity.ok(responsePage);
  }

  @PatchMapping("/{blockedTimeId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<BlockedTimeDetailResponseDto>> updateBlockedTime(
      @PathVariable UUID blockedTimeId,
      @RequestBody @Valid BlockedTimeUpdateRequestDto requestDto) {

    List<BlockedTimeDetail> updatedBlockedTimes =
        updateBlockedTimeUseCase.execute(blockedTimeId, requestDto);
    List<BlockedTimeDetailResponseDto> response =
        scheduleEntryMapper.toDetailDtoList(updatedBlockedTimes);

    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{blockedTimeId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> deleteBlockedTime(
      @PathVariable UUID blockedTimeId,
      @RequestParam(defaultValue = "false") boolean deleteAllRecurring) {

    deleteBlockedTimeUseCase.execute(blockedTimeId, deleteAllRecurring);
    return ResponseEntity.noContent().build();
  }

  /**
   * Monta o DTO de resposta a partir do preview de conflitos.
   *
   * @param preview O preview de conflitos gerado pelo caso de uso.
   * @return O DTO de resposta contendo os detalhes dos conflitos.
   */
  private BlockedTimeConflictsPreviewResponseDto buildPreviewResponseDto(
      BlockedTimeConflictsPreview preview) {
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
        result.usersAffected());
  }
}
