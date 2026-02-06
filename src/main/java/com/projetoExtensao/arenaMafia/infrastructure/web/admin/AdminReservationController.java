package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.AdminListReservationsUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CancelReservationByAdminUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CreateReservationByAdminUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request.AdminReservationCreateRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request.AdminReservationSearchRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ReservationResponseMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationController {

  private final ReservationResponseMapper reservationResponseMapper;
  private final AdminListReservationsUseCase adminListReservationsUseCase;
  private final CancelReservationByAdminUseCase cancelReservationByAdminUseCase;
  private final CreateReservationByAdminUseCase createReservationByAdminUseCase;

  public AdminReservationController(
      ReservationResponseMapper reservationResponseMapper,
      AdminListReservationsUseCase adminListReservationsUseCase,
      CancelReservationByAdminUseCase cancelReservationByAdminUseCase,
      CreateReservationByAdminUseCase createReservationByAdminUseCase) {
    this.reservationResponseMapper = reservationResponseMapper;
    this.adminListReservationsUseCase = adminListReservationsUseCase;
    this.cancelReservationByAdminUseCase = cancelReservationByAdminUseCase;
    this.createReservationByAdminUseCase = createReservationByAdminUseCase;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Page<ReservationDetailResponseDto>> searchReservations(
          @Valid AdminReservationSearchRequestDto requestDto,
          Pageable pageable) {

    Page<ReservationDetail> reservations = adminListReservationsUseCase.execute(requestDto, pageable);
    Page<ReservationDetailResponseDto> responseDtoPage = reservations.map(reservationResponseMapper::toDetailDto);
    return ResponseEntity.ok(responseDtoPage);
  }

  @PostMapping("/{reservationId}/cancel")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> cancelUserReservationByAdmin(
      @PathVariable UUID reservationId,
      @RequestParam(defaultValue = "false") boolean cancelAllRecurring,
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser) {

    UUID adminId = authenticatedUser.user().getId();
    cancelReservationByAdminUseCase.execute(adminId, reservationId, cancelAllRecurring);
    return ResponseEntity.noContent().build();
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<ReservationDetailResponseDto>> createReservationByAdmin(
      @RequestBody @Valid AdminReservationCreateRequestDto requestDto,
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser) {

    UUID adminId = authenticatedUser.user().getId();
    List<ReservationDetailResponseDto> response = createReservationByAdminUseCase.execute(adminId, requestDto)
            .stream()
            .map(reservationResponseMapper::toDetailDto)
            .toList();

    return ResponseEntity.ok(response);
  }

}
