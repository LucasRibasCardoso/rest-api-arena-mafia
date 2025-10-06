package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.court.usecase.CreateCourtUseCase;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ModalityMapper;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateCourtRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.response.AdminCourtResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/courts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourtController {

  private final ModalityMapper modalityMapper;
  private final ModalityRepositoryPort modalityRepositoryPort;
  private final CreateCourtUseCase createCourtUseCase;

  public AdminCourtController(
      ModalityMapper modalityMapper,
      ModalityRepositoryPort modalityRepositoryPort,
      CreateCourtUseCase createCourtUseCase) {
    this.modalityMapper = modalityMapper;
    this.modalityRepositoryPort = modalityRepositoryPort;
    this.createCourtUseCase = createCourtUseCase;
  }

  @PostMapping
  public ResponseEntity<AdminCourtResponseDto> create(
      @Valid @RequestBody CreateCourtRequestDto request) {

    Court court = createCourtUseCase.execute(request);
    Set<ModalityResponseDto> modalityResponses =
        modalityRepositoryPort.findAllByIds(court.getModalityIds()).stream()
            .map(modalityMapper::toResponseDto)
            .collect(Collectors.toSet());

    var response = AdminCourtResponseDto.fromDomain(court, modalityResponses);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(court.getId())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }
}
