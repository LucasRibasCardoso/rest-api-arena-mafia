package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.CreateOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.DisableOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.EnableOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.FindAllOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.FindByIdOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.OperatingHoursMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateOperatingHoursRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  private final OperatingHoursMapper operatingHoursMapper;
  private final CreateOperatingHoursUseCase createOperatingHoursUseCase;
  private final EnableOperatingHoursUseCase enableOperatingHoursUseCase;
  private final DisableOperatingHoursUseCase disableOperatingHoursUseCase;
  private final FindByIdOperatingHoursUseCase getOperatingHoursByIdUseCase;
  private final FindAllOperatingHoursUseCase findAllOperatingHoursUseCase;

  public AdminOperatingHoursController(
      CreateOperatingHoursUseCase createOperatingHoursUseCase,
      EnableOperatingHoursUseCase enableOperatingHoursUseCase,
      DisableOperatingHoursUseCase disableOperatingHoursUseCase,
      FindByIdOperatingHoursUseCase FindByIdOperatingHoursUseCase,
      FindAllOperatingHoursUseCase findAllOperatingHoursUseCase,
      OperatingHoursMapper operatingHoursMapper) {
    this.createOperatingHoursUseCase = createOperatingHoursUseCase;
    this.enableOperatingHoursUseCase = enableOperatingHoursUseCase;
    this.disableOperatingHoursUseCase = disableOperatingHoursUseCase;
    this.getOperatingHoursByIdUseCase = FindByIdOperatingHoursUseCase;
    this.findAllOperatingHoursUseCase = findAllOperatingHoursUseCase;
    this.operatingHoursMapper = operatingHoursMapper;
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<OperatingHoursResponseDto> createOperatingHours(
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
  public ResponseEntity<List<OperatingHoursResponseDto>> getAllOperatingHours(
      @RequestParam(required = false) Boolean isActive) {

    List<OperatingHoursResponseDto> operatingHours =
        findAllOperatingHoursUseCase.execute(isActive).stream()
            .map(operatingHoursMapper::toDto)
            .toList();

    return ResponseEntity.ok(operatingHours);
  }

  @GetMapping("/{hourId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<OperatingHoursResponseDto> getOperatingHoursById(
      @PathVariable UUID hourId) {
    OperatingHours operatingHours = getOperatingHoursByIdUseCase.execute(hourId);
    OperatingHoursResponseDto response = operatingHoursMapper.toDto(operatingHours);
    return ResponseEntity.ok().body(response);
  }

  @PatchMapping("/{hourId}/enable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> enableOperatingHours(@PathVariable UUID hourId) {
    enableOperatingHoursUseCase.execute(hourId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{hourId}/disable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> disableOperatingHours(@PathVariable UUID hourId) {
    disableOperatingHoursUseCase.execute(hourId);
    return ResponseEntity.noContent().build();
  }
}
