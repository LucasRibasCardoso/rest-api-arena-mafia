package com.projetoExtensao.arenaMafia.infrastructure.web.modality;

import com.projetoExtensao.arenaMafia.application.modality.usecase.FindAllModalitiesUseCase;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ModalityMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modalities")
public class ModalityController {

  private final FindAllModalitiesUseCase findAllModalitiesUseCase;
  private final ModalityMapper modalityMapper;

  public ModalityController(
      FindAllModalitiesUseCase findAllModalitiesUseCase, ModalityMapper modalityMapper) {
    this.findAllModalitiesUseCase = findAllModalitiesUseCase;
    this.modalityMapper = modalityMapper;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<ModalityResponseDto>> findAll() {
    List<Modality> modalities = findAllModalitiesUseCase.execute();
    var response = modalities.stream().map(modalityMapper::toDto).toList();
    return ResponseEntity.ok(response);
  }
}
