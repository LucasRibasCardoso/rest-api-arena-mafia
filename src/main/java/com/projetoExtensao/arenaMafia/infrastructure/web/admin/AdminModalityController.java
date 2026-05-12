package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.modality.usecase.*;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ModalityMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.modality.request.CreateModalityRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.modality.request.UpdateModalityRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/modalities")
@PreAuthorize("hasRole('ADMIN')")
public class AdminModalityController {

  private final ModalityMapper modalityMapper;
  private final CreateModalityUseCase createModalityUseCase;
  private final EnableModalityUseCase enableModalityUseCase;
  private final DisableModalityUseCase disableModalityUseCase;
  private final UpdateModalityUseCase updateModalityUseCase;
  private final FindByIdModalityUseCase findByIdModalityUseCase;
  private final FindAllModalitiesUseCase findAllModalitiesUseCase;

  public AdminModalityController(
      ModalityMapper modalityMapper,
      CreateModalityUseCase createModalityUseCase,
      EnableModalityUseCase enableModalityUseCase,
      DisableModalityUseCase disableModalityUseCase,
      UpdateModalityUseCase updateModalityUseCase,
      FindByIdModalityUseCase findByIdModalityUseCase,
      FindAllModalitiesUseCase findAllModalitiesUseCase) {
    this.modalityMapper = modalityMapper;
    this.createModalityUseCase = createModalityUseCase;
    this.enableModalityUseCase = enableModalityUseCase;
    this.disableModalityUseCase = disableModalityUseCase;
    this.updateModalityUseCase = updateModalityUseCase;
    this.findByIdModalityUseCase = findByIdModalityUseCase;
    this.findAllModalitiesUseCase = findAllModalitiesUseCase;
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ModalityResponseDto> create(
      @RequestBody @Valid CreateModalityRequestDto request) {

    Modality modality = createModalityUseCase.execute(request.name());
    ModalityResponseDto response = modalityMapper.toDto(modality);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(modality.getId())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<ModalityResponseDto>> findAll(
      @RequestParam(required = false) Boolean isActive) {
    List<Modality> modalities = findAllModalitiesUseCase.execute(isActive);
    List<ModalityResponseDto> response = modalities.stream().map(modalityMapper::toDto).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{modalityId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ModalityResponseDto> getModalityDetails(@PathVariable UUID modalityId) {
    Modality modality = findByIdModalityUseCase.execute(modalityId);
    ModalityResponseDto response = modalityMapper.toDto(modality);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{modalityId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ModalityResponseDto> update(
      @PathVariable UUID modalityId, @RequestBody @Valid UpdateModalityRequestDto request) {

    Modality modality = updateModalityUseCase.execute(modalityId, request.name());
    ModalityResponseDto response = modalityMapper.toDto(modality);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{modalityId}/disable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> disable(@PathVariable UUID modalityId) {
    disableModalityUseCase.execute(modalityId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{modalityId}/enable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> enable(@PathVariable UUID modalityId) {
    enableModalityUseCase.execute(modalityId);
    return ResponseEntity.noContent().build();
  }
}
