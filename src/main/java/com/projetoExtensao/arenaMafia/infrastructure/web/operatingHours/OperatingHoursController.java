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

  private final FindAllOperatingHoursUseCase findAllOperatingHoursUseCase;
  private final OperatingHoursMapper operatingHoursMapper;

  public OperatingHoursController(
      FindAllOperatingHoursUseCase findAllOperatingHoursUseCase,
      OperatingHoursMapper operatingHoursMapper) {
    this.findAllOperatingHoursUseCase = findAllOperatingHoursUseCase;
    this.operatingHoursMapper = operatingHoursMapper;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<OperatingHoursResponseDto>> getAllActiveOperatingHours() {

    List<OperatingHoursResponseDto> operatingHours =
        findAllOperatingHoursUseCase.execute(true).stream()
            .map(operatingHoursMapper::toResponseDto)
            .toList();

    return ResponseEntity.ok(operatingHours);
  }
}
