package com.projetoExtensao.arenaMafia.infrastructure.web.schedule;

import com.projetoExtensao.arenaMafia.application.schedule.usecase.FindAllAvailableTimesUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.AvailableSlotResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.AvailableSlotMapper;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedules/available-times")
public class AvailableTimesController {

  private final AvailableSlotMapper availableSlotMapper;
  private final FindAllAvailableTimesUseCase findAllAvailableTimesUseCase;

  public AvailableTimesController(
      AvailableSlotMapper availableSlotMapper,
      FindAllAvailableTimesUseCase findAllAvailableTimesUseCase) {
    this.availableSlotMapper = availableSlotMapper;
    this.findAllAvailableTimesUseCase = findAllAvailableTimesUseCase;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<AvailableSlotResponseDto>> getAvailableTimes(
      @RequestParam("modalityId") @NotNull UUID modalityId,
      @RequestParam("date") @NotNull LocalDate date) {

    List<AvailableSlotResponseDto> availableSlots =
        findAllAvailableTimesUseCase.execute(modalityId, date).stream()
            .map(availableSlotMapper::toDto)
            .toList();

    return ResponseEntity.ok().body(availableSlots);
  }
}
