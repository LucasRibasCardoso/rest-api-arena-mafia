package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.modality.usecase.CreateModalityUseCase;
import com.projetoExtensao.arenaMafia.application.modality.usecase.DisableModalityUseCase;
import com.projetoExtensao.arenaMafia.application.modality.usecase.FindByIdModalityUseCase;
import com.projetoExtensao.arenaMafia.application.modality.usecase.UpdateModalityUseCase;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ModalityMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateModalityRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.UpdateModalityRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import jakarta.validation.Valid;
import java.net.URI;
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
  private final DisableModalityUseCase disableModalityUseCase;
  private final UpdateModalityUseCase updateModalityUseCase;
  private final FindByIdModalityUseCase findByIdModalityUseCase;

  public AdminModalityController(
      ModalityMapper modalityMapper,
      CreateModalityUseCase createModalityUseCase,
      DisableModalityUseCase disableModalityUseCase,
      UpdateModalityUseCase updateModalityUseCase,
      FindByIdModalityUseCase findByIdModalityUseCase) {
    this.modalityMapper = modalityMapper;
    this.createModalityUseCase = createModalityUseCase;
    this.disableModalityUseCase = disableModalityUseCase;
    this.updateModalityUseCase = updateModalityUseCase;
    this.findByIdModalityUseCase = findByIdModalityUseCase;
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

  @GetMapping("/{modalityId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ModalityResponseDto> getById(@PathVariable UUID modalityId) {
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
}
