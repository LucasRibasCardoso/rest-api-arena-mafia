package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.domain.dto.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.PreviewBlockedTimeConflictsUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response.BlockedTimeConflictsPreviewResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/blocked-times")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlockedTimesController {

  private final PreviewBlockedTimeConflictsUseCase previewBlockedTimeConflictsUseCase;

  public AdminBlockedTimesController(
      PreviewBlockedTimeConflictsUseCase previewBlockedTimeConflictsUseCase) {
    this.previewBlockedTimeConflictsUseCase = previewBlockedTimeConflictsUseCase;
  }

  @PostMapping("/preview-conflicts")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<BlockedTimeConflictsPreviewResponseDto> previewConflicts(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin,
      @RequestBody @Valid BlockedTimeConflictsPreviewRequestDto requestDto) {

    UUID adminId = authenticatedAdmin.getUser().getId();
    BlockedTimeConflictsPreview preview = previewBlockedTimeConflictsUseCase.execute(requestDto, adminId);
    BlockedTimeConflictsPreviewResponseDto response = buildResponseDto(preview);
    return ResponseEntity.ok(response);
  }

  /**
   * Monta o DTO de resposta a partir do preview de conflitos.
   *
   * @param preview O preview de conflitos gerado pelo caso de uso.
   * @return O DTO de resposta contendo os detalhes dos conflitos.
   */
  private BlockedTimeConflictsPreviewResponseDto buildResponseDto(
      BlockedTimeConflictsPreview preview) {
    return new BlockedTimeConflictsPreviewResponseDto(
        preview.previewKey(),
        preview.usersAffected(),
        preview.blockedTimesAffected(),
        preview.reservationsAffected(),
        preview.conflictingBlockedTimes(),
        preview.conflictingReservations());
  }

  // TODO: Implementar endpoint POST para cadastrar novos horários bloqueados

  // TODO: Implementar endpoint DELETE para remover horários bloqueados existentes

  // TODO: Implementar endpoint GET para listar todos os horários bloqueados

  // TODO: Implementar endpoint PUT para atualizar horários bloqueados existentes

}
