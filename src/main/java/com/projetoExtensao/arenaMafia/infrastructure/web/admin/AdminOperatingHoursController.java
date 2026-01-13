package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.operatingHours.preview.OperatingHoursDisablePreview;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.*;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.OperatingHoursMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.request.CreateOperatingHoursRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.request.OperatingHoursDisableConfirmRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.response.OperatingHoursDisablePreviewResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.BlockedTimeDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ScheduleEntryResponseMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/operating-hours")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperatingHoursController {

  private final ScheduleEntryResponseMapper scheduleEntryMapper;
  private final OperatingHoursMapper operatingHoursMapper;
  private final CreateOperatingHoursUseCase createOperatingHoursUseCase;
  private final EnableOperatingHoursUseCase enableOperatingHoursUseCase;
  private final FindByIdOperatingHoursUseCase findByIdOperatingHoursUseCase;
  private final FindAllOperatingHoursUseCase findAllOperatingHoursUseCase;
  private final PreviewOperatingHoursDisableUseCase previewOperatingHoursDisableUseCase;
  private final ConfirmOperatingHoursDisableUseCase confirmDisableOperatingHoursUseCase;

  public AdminOperatingHoursController(
      ScheduleEntryResponseMapper scheduleEntryMapper,
      OperatingHoursMapper operatingHoursMapper,
      CreateOperatingHoursUseCase createOperatingHoursUseCase,
      EnableOperatingHoursUseCase enableOperatingHoursUseCase,
      FindAllOperatingHoursUseCase findAllOperatingHoursUseCase,
      FindByIdOperatingHoursUseCase findByIdOperatingHoursUseCase,
      PreviewOperatingHoursDisableUseCase previewOperatingHoursDisableUseCase,
      ConfirmOperatingHoursDisableUseCase confirmDisableOperatingHoursUseCase) {
    this.scheduleEntryMapper = scheduleEntryMapper;
    this.operatingHoursMapper = operatingHoursMapper;
    this.createOperatingHoursUseCase = createOperatingHoursUseCase;
    this.enableOperatingHoursUseCase = enableOperatingHoursUseCase;
    this.findAllOperatingHoursUseCase = findAllOperatingHoursUseCase;
    this.findByIdOperatingHoursUseCase = findByIdOperatingHoursUseCase;
    this.previewOperatingHoursDisableUseCase = previewOperatingHoursDisableUseCase;
    this.confirmDisableOperatingHoursUseCase = confirmDisableOperatingHoursUseCase;
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<OperatingHoursResponseDto> create(
      @RequestBody @Valid CreateOperatingHoursRequestDto request) {

    OperatingHours operatingHours = createOperatingHoursUseCase.execute(request);
    OperatingHoursResponseDto response = operatingHoursMapper.toDto(operatingHours);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(operatingHours.getId())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<OperatingHoursResponseDto>> getAll(
      @RequestParam(required = false) Boolean isActive) {

    List<OperatingHoursResponseDto> operatingHours =
        findAllOperatingHoursUseCase.execute(isActive).stream()
            .map(operatingHoursMapper::toDto)
            .toList();

    return ResponseEntity.ok(operatingHours);
  }

  @GetMapping("/{hourId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<OperatingHoursResponseDto> getById(@PathVariable UUID hourId) {
    OperatingHours operatingHours = findByIdOperatingHoursUseCase.execute(hourId);
    OperatingHoursResponseDto response = operatingHoursMapper.toDto(operatingHours);
    return ResponseEntity.ok().body(response);
  }

  @PatchMapping("/{hourId}/enable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> enableOperatingHours(@PathVariable UUID hourId) {
    enableOperatingHoursUseCase.execute(hourId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{hourId}/preview-disable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<OperatingHoursDisablePreviewResponseDto> previewDisable(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin, @PathVariable UUID hourId) {

    UUID adminId = authenticatedAdmin.user().getId();
    OperatingHoursDisablePreview preview =
        previewOperatingHoursDisableUseCase.execute(adminId, hourId);
    OperatingHoursDisablePreviewResponseDto response = buildResponsePreviewDto(preview);
    return ResponseEntity.ok().body(response);
  }

  @PostMapping("/confirm-disable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> confirmDisable(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin,
      @RequestBody @Valid OperatingHoursDisableConfirmRequestDto requestDto) {

    UUID adminId = authenticatedAdmin.user().getId();
    confirmDisableOperatingHoursUseCase.execute(adminId, requestDto);
    return ResponseEntity.noContent().build();
  }

  /**
   * Monta o DTO de resposta a partir do preview de desativação de quadra.
   *
   * @param preview O preview de desativação gerado pelo caso de uso.
   * @return O DTO de resposta contendo os detalhes dos conflitos.
   */
  private OperatingHoursDisablePreviewResponseDto buildResponsePreviewDto(
      OperatingHoursDisablePreview preview) {

    List<BlockedTimeDetailResponseDto> blockedTimesDetails =
        scheduleEntryMapper.toDetailDtoList(preview.affectedBlockedTimes());

    List<ReservationDetailResponseDto> reservationsDetails =
        scheduleEntryMapper.toDetailDtoList(preview.affectedReservations());

    List<ReservationDetailResponseDto> inProgressReservationsDetails =
        scheduleEntryMapper.toDetailDtoList(preview.inProgressReservations());

    return new OperatingHoursDisablePreviewResponseDto(
        preview.previewKey(),
        preview.operatingHoursId(),
        preview.usersAffectedCount(),
        preview.blockedTimesAffectedCount(),
        preview.reservationsAffectedCount(),
        blockedTimesDetails,
        reservationsDetails,
        inProgressReservationsDetails);
  }
}
