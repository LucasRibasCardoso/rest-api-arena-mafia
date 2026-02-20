package com.projetoExtensao.arenaMafia.infrastructure.web.schedule;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CancelReservationUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CreateReservationUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.FindAllReservationUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.FindByIdReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request.CreateReservationRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleNormal.ReservationResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ReservationResponseMapper;
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

  private final ReservationResponseMapper reservationMapper;
  private final FindByIdReservationUseCase findByIdReservationUseCase;
  private final FindAllReservationUseCase findAllReservationUseCase;
  private final CreateReservationUseCase createReservationUseCase;
  private final CancelReservationUseCase cancelReservationUseCase;

  public ReservationController(
      ReservationResponseMapper reservationMapper,
      FindByIdReservationUseCase findByIdReservationUseCase,
      FindAllReservationUseCase findAllReservationUseCase,
      CreateReservationUseCase createReservationUseCase,
      CancelReservationUseCase cancelReservationUseCase) {
    this.reservationMapper = reservationMapper;
    this.findByIdReservationUseCase = findByIdReservationUseCase;
    this.findAllReservationUseCase = findAllReservationUseCase;
    this.createReservationUseCase = createReservationUseCase;
    this.cancelReservationUseCase = cancelReservationUseCase;
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ReservationResponseDto> create(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody CreateReservationRequestDto request) {

    UUID authenticateUserId = authenticatedUser.user().getId();
    Reservation reservation = createReservationUseCase.execute(authenticateUserId, request);
    ReservationResponseDto response = reservationMapper.toDto(reservation);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(reservation.getId())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Page<ReservationResponseDto>> findAll(
      Pageable pageable, @AuthenticationPrincipal UserDetailsAdapter authenticatedUser) {

    UUID userId = extractUserId(authenticatedUser);
    Page<Reservation> reservationsPage = findAllReservationUseCase.execute(userId, pageable);
    Page<ReservationResponseDto> responsePage = reservationsPage.map(reservationMapper::toDto);

    return ResponseEntity.ok(responsePage);
  }

  @GetMapping("/{id}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<ReservationDetailResponseDto> getReservationDetails(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser, @PathVariable UUID id) {

    UUID userId = extractUserId(authenticatedUser);
    ReservationDetail reservation = findByIdReservationUseCase.execute(userId, id);
    ReservationDetailResponseDto response = reservationMapper.toDetailDto(reservation);
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
    return authenticatedUser.user().getId();
  }
}
