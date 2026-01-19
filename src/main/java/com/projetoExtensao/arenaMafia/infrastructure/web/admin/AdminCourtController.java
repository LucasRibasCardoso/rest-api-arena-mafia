package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.court.aggregate.CourtWithModalities;
import com.projetoExtensao.arenaMafia.application.court.preview.CourtDisablePreview;
import com.projetoExtensao.arenaMafia.application.court.usecase.*;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ModalityMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.CourtDisableConfirmRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.CreateCourtRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.UpdateCourtRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.response.AdminCourtResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.response.CourtDisablePreviewResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.BlockedTimeDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ScheduleEntryResponseMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/courts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourtController {

  private final ModalityMapper modalityMapper;
  private final ScheduleEntryResponseMapper scheduleEntryMapper;

  private final CreateCourtUseCase createCourtUseCase;
  private final EnableCourtUseCase enableCourtUseCase;
  private final UpdateCourtUseCase updateCourtUseCase;
  private final FindAllCourtUseCase findAllCourtUseCase;
  private final FindCourtByIdUseCase findCourtByIdUseCase;
  private final PreviewCourtDisableUseCase previewCourtDisableUseCase;
  private final ConfirmCourtDisableUseCase confirmCourtDisableUseCase;

  public AdminCourtController(
      ModalityMapper modalityMapper,
      ScheduleEntryResponseMapper scheduleEntryMapper,
      CreateCourtUseCase createCourtUseCase,
      EnableCourtUseCase enableCourtUseCase,
      UpdateCourtUseCase updateCourtUseCase,
      FindAllCourtUseCase findAllCourtUseCase,
      FindCourtByIdUseCase findCourtByIdUseCase,
      PreviewCourtDisableUseCase previewCourtDisableUseCase,
      ConfirmCourtDisableUseCase confirmCourtDisableUseCase) {
    this.modalityMapper = modalityMapper;
    this.scheduleEntryMapper = scheduleEntryMapper;
    this.createCourtUseCase = createCourtUseCase;
    this.updateCourtUseCase = updateCourtUseCase;
    this.enableCourtUseCase = enableCourtUseCase;
    this.findAllCourtUseCase = findAllCourtUseCase;
    this.findCourtByIdUseCase = findCourtByIdUseCase;
    this.previewCourtDisableUseCase = previewCourtDisableUseCase;
    this.confirmCourtDisableUseCase = confirmCourtDisableUseCase;
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<AdminCourtResponseDto> create(
      @Valid @RequestBody CreateCourtRequestDto request) {

    CourtWithModalities result = createCourtUseCase.execute(request);
    AdminCourtResponseDto response = mapToResponse(result);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(result.court().getId())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<AdminCourtResponseDto>> getAll(
      @RequestParam(required = false) Boolean isActive) {
    List<CourtWithModalities> courts = findAllCourtUseCase.execute(isActive);
    List<AdminCourtResponseDto> response = mapToResponseList(courts);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{courtId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<AdminCourtResponseDto> getCourtDetails(@PathVariable UUID courtId) {
    CourtWithModalities result = findCourtByIdUseCase.execute(courtId);
    AdminCourtResponseDto response = mapToResponse(result);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/{courtId}/preview-disable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<CourtDisablePreviewResponseDto> previewDisable(
      @PathVariable UUID courtId, @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin) {

    UUID adminId = authenticatedAdmin.user().getId();
    CourtDisablePreview preview = previewCourtDisableUseCase.execute(courtId, adminId);
    CourtDisablePreviewResponseDto response = buildResponsePreviewDto(preview);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/confirm-disable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> confirmDisable(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin,
      @RequestBody @Valid CourtDisableConfirmRequestDto requestDto) {
    UUID adminId = authenticatedAdmin.user().getId();
    confirmCourtDisableUseCase.execute(adminId, requestDto);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{courtId}/enable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> enable(@PathVariable UUID courtId) {
    enableCourtUseCase.execute(courtId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{courtId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<AdminCourtResponseDto> update(
      @PathVariable UUID courtId, @Valid @RequestBody UpdateCourtRequestDto request) {
    CourtWithModalities result = updateCourtUseCase.execute(courtId, request);
    AdminCourtResponseDto response = mapToResponse(result);
    return ResponseEntity.ok(response);
  }

  /**
   * Monta o DTO de resposta a partir do preview de desativação de quadra.
   *
   * @param preview O preview de desativação gerado pelo caso de uso.
   * @return O DTO de resposta contendo os detalhes dos conflitos.
   */
  private CourtDisablePreviewResponseDto buildResponsePreviewDto(CourtDisablePreview preview) {

    List<BlockedTimeDetailResponseDto> blockedTimesDetails =
        scheduleEntryMapper.toDetailDtoList(preview.affectedBlockedTimes());

    List<ReservationDetailResponseDto> reservationsDetails =
        scheduleEntryMapper.toDetailDtoList(preview.affectedReservations());

    List<ReservationDetailResponseDto> inProgressReservationsDetails =
        scheduleEntryMapper.toDetailDtoList(preview.inProgressReservations());

    return new CourtDisablePreviewResponseDto(
        preview.previewKey(),
        preview.courtId(),
        preview.courtName(),
        preview.usersAffectedCount(),
        preview.blockedTimesAffectedCount(),
        preview.reservationsAffectedCount(),
        blockedTimesDetails,
        reservationsDetails,
        inProgressReservationsDetails);
  }

  /**
   * Mapeia uma lista de CourtWithModalitiesResult para uma lista de AdminCourtResponseDto.
   *
   * @param courts Lista de CourtWithModalitiesResult a ser mapeada.
   * @return Lista de AdminCourtResponseDto mapeada.
   */
  private List<AdminCourtResponseDto> mapToResponseList(List<CourtWithModalities> courts) {
    return courts.stream().map(this::mapToResponse).collect(Collectors.toList());
  }

  /**
   * Mapeia um CourtWithModalitiesResult para um AdminCourtResponseDto.
   *
   * @param result CourtWithModalitiesResult a ser mapeado.
   * @return AdminCourtResponseDto mapeado.
   */
  private AdminCourtResponseDto mapToResponse(CourtWithModalities result) {
    List<ModalityResponseDto> modalityResponses =
        result.modalities().stream().map(modalityMapper::toDto).collect(Collectors.toList());
    return AdminCourtResponseDto.fromDomain(result.court(), modalityResponses);
  }
}
