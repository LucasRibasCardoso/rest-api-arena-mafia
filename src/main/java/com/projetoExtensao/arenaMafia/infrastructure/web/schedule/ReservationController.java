package com.projetoExtensao.arenaMafia.infrastructure.web.schedule;

import com.projetoExtensao.arenaMafia.application.schedule.usecase.CreateReservationUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.FindByIdReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request.CreateReservationRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.ScheduleEntryResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ScheduleEntryResponseMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users/me/reservations")
public class ReservationController {

  private final ScheduleEntryResponseMapper scheduleEntryResponseMapper;
  private final FindByIdReservationUseCase findByIdReservationUseCase;
  private final CreateReservationUseCase createReservationUseCase;

  public ReservationController(
      ScheduleEntryResponseMapper scheduleEntryResponseMapper,
      FindByIdReservationUseCase findByIdReservationUseCase,
      CreateReservationUseCase createReservationUseCase) {
    this.scheduleEntryResponseMapper = scheduleEntryResponseMapper;
    this.findByIdReservationUseCase = findByIdReservationUseCase;
    this.createReservationUseCase = createReservationUseCase;
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ScheduleEntryResponseDto> create(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody CreateReservationRequestDto request) {

    UUID authenticateUserId = authenticatedUser.getUser().getId();
    ScheduleEntry scheduleEntry = createReservationUseCase.execute(authenticateUserId, request);
    ScheduleEntryResponseDto response = scheduleEntryResponseMapper.toResponseDto(scheduleEntry);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(scheduleEntry.getId())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{id}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ScheduleEntryResponseDto> getById(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser, @PathVariable UUID id) {

    UUID userId = authenticatedUser.getUser().getId();
    ScheduleEntry scheduleEntry = findByIdReservationUseCase.execute(userId, id);
    ScheduleEntryResponseDto response = scheduleEntryResponseMapper.toResponseDto(scheduleEntry);
    return ResponseEntity.ok(response);
  }
}
