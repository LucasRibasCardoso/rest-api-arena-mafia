package com.projetoExtensao.arenaMafia.infrastructure.web.schedule;

import com.projetoExtensao.arenaMafia.application.schedule.usecase.CancelReservationUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.CreateReservationUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.FindAllReservationUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.FindByIdReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request.CreateReservationRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.ScheduleEntryResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ScheduleEntryResponseMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users/me/reservations")
public class ReservationController {

  private final ScheduleEntryResponseMapper scheduleEntryResponseMapper;
  private final FindByIdReservationUseCase findByIdReservationUseCase;
  private final FindAllReservationUseCase findAllReservationUseCase;
  private final CreateReservationUseCase createReservationUseCase;
  private final CancelReservationUseCase cancelReservationUseCase;

  public ReservationController(
      ScheduleEntryResponseMapper scheduleEntryResponseMapper,
      FindByIdReservationUseCase findByIdReservationUseCase,
      FindAllReservationUseCase findAllReservationUseCase,
      CreateReservationUseCase createReservationUseCase,
      CancelReservationUseCase cancelReservationUseCase) {
    this.scheduleEntryResponseMapper = scheduleEntryResponseMapper;
    this.findByIdReservationUseCase = findByIdReservationUseCase;
    this.findAllReservationUseCase = findAllReservationUseCase;
    this.createReservationUseCase = createReservationUseCase;
    this.cancelReservationUseCase = cancelReservationUseCase;
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ScheduleEntryResponseDto> create(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody CreateReservationRequestDto request) {

    UUID authenticateUserId = authenticatedUser.getUser().getId();
    ScheduleEntry scheduleEntry = createReservationUseCase.execute(authenticateUserId, request);
    ScheduleEntryResponseDto response = scheduleEntryResponseMapper.toDto(scheduleEntry);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(scheduleEntry.getId())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Page<ScheduleEntryResponseDto>> findAll(
      Pageable pageable, @AuthenticationPrincipal UserDetailsAdapter authenticatedUser) {

    UUID userId = extractUserId(authenticatedUser);
    Page<Reservation> reservationsPage = findAllReservationUseCase.execute(userId, pageable);
    Page<ScheduleEntryResponseDto> responsePage =
        reservationsPage.map(scheduleEntryResponseMapper::toDto);

    return ResponseEntity.ok(responsePage);
  }

  @GetMapping("/{id}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ScheduleEntryResponseDto> getById(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser, @PathVariable UUID id) {

    UUID userId = extractUserId(authenticatedUser);
    ScheduleEntry scheduleEntry = findByIdReservationUseCase.execute(userId, id);
    ScheduleEntryResponseDto response = scheduleEntryResponseMapper.toDto(scheduleEntry);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{reservationId}/cancel")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> cancelReservation(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @PathVariable UUID reservationId) {

    UUID userId = extractUserId(authenticatedUser);
    cancelReservationUseCase.execute(userId, reservationId);
    return ResponseEntity.noContent().build();
  }

  private UUID extractUserId(UserDetailsAdapter authenticatedUser) {
    return authenticatedUser.getUser().getId();
  }
}
