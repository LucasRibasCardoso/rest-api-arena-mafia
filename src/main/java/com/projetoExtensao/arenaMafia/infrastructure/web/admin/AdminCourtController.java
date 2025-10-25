package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.court.dto.CourtWithModalitiesResult;
import com.projetoExtensao.arenaMafia.application.court.usecase.CreateCourtUseCase;
import com.projetoExtensao.arenaMafia.application.court.usecase.DisableCourtUseCase;
import com.projetoExtensao.arenaMafia.application.court.usecase.EnableCourtUseCase;
import com.projetoExtensao.arenaMafia.application.court.usecase.FindAllCourtUseCase;
import com.projetoExtensao.arenaMafia.application.court.usecase.FindCourtByIdUseCase;
import com.projetoExtensao.arenaMafia.application.court.usecase.UpdateCourtUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ModalityMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateCourtRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.UpdateCourtRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.response.AdminCourtResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private final CreateCourtUseCase createCourtUseCase;
  private final EnableCourtUseCase enableCourtUseCase;
  private final UpdateCourtUseCase updateCourtUseCase;
  private final DisableCourtUseCase deleteCourtUseCase;
  private final FindAllCourtUseCase findAllCourtUseCase;
  private final FindCourtByIdUseCase findCourtByIdUseCase;

  public AdminCourtController(
      ModalityMapper modalityMapper,
      CreateCourtUseCase createCourtUseCase,
      EnableCourtUseCase enableCourtUseCase,
      UpdateCourtUseCase updateCourtUseCase,
      DisableCourtUseCase deleteCourtUseCase,
      FindAllCourtUseCase findAllCourtUseCase,
      FindCourtByIdUseCase findCourtByIdUseCase) {
    this.modalityMapper = modalityMapper;
    this.createCourtUseCase = createCourtUseCase;
    this.deleteCourtUseCase = deleteCourtUseCase;
    this.updateCourtUseCase = updateCourtUseCase;
    this.enableCourtUseCase = enableCourtUseCase;
    this.findAllCourtUseCase = findAllCourtUseCase;
    this.findCourtByIdUseCase = findCourtByIdUseCase;
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<AdminCourtResponseDto> create(
      @Valid @RequestBody CreateCourtRequestDto request) {

    CourtWithModalitiesResult result = createCourtUseCase.execute(request);
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
    List<CourtWithModalitiesResult> courts = findAllCourtUseCase.execute(isActive);
    List<AdminCourtResponseDto> response = mapToResponseList(courts);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{courtId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<AdminCourtResponseDto> getById(@PathVariable UUID courtId) {
    CourtWithModalitiesResult result = findCourtByIdUseCase.execute(courtId);
    AdminCourtResponseDto response = mapToResponse(result);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{courtId}/disable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> disable(@PathVariable UUID courtId) {
    deleteCourtUseCase.execute(courtId);
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
    CourtWithModalitiesResult result = updateCourtUseCase.execute(courtId, request);
    AdminCourtResponseDto response = mapToResponse(result);
    return ResponseEntity.ok(response);
  }

  /**
   * Mapeia uma lista de CourtWithModalitiesResult para uma lista de AdminCourtResponseDto.
   *
   * @param courts Lista de CourtWithModalitiesResult a ser mapeada.
   * @return Lista de AdminCourtResponseDto mapeada.
   */
  private List<AdminCourtResponseDto> mapToResponseList(List<CourtWithModalitiesResult> courts) {
    return courts.stream().map(this::mapToResponse).collect(Collectors.toList());
  }

  /**
   * Mapeia um CourtWithModalitiesResult para um AdminCourtResponseDto.
   *
   * @param result CourtWithModalitiesResult a ser mapeado.
   * @return AdminCourtResponseDto mapeado.
   */
  private AdminCourtResponseDto mapToResponse(CourtWithModalitiesResult result) {
    List<ModalityResponseDto> modalityResponses =
        result.modalities().stream()
            .map(modalityMapper::toDto)
            .collect(Collectors.toList());
    return AdminCourtResponseDto.fromDomain(result.court(), modalityResponses);
  }
}
