package com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours;

import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.FindAllOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.OperatingHoursMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operating-hours")
public class OperatingHoursController {

  private final OperatingHoursMapper operatingHoursMapper;
  private final FindAllOperatingHoursUseCase findAllOperatingHoursUseCase;

  public OperatingHoursController(
      OperatingHoursMapper operatingHoursMapper,
      FindAllOperatingHoursUseCase findAllOperatingHoursUseCase) {
    this.operatingHoursMapper = operatingHoursMapper;
    this.findAllOperatingHoursUseCase = findAllOperatingHoursUseCase;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<OperatingHoursResponseDto>> getAllActiveOperatingHours() {
    boolean isActive = true;
    List<OperatingHoursResponseDto> response =
        findAllOperatingHoursUseCase.execute(isActive).stream()
            .map(operatingHoursMapper::toDto)
            .toList();
    return ResponseEntity.ok(response);
  }
}
