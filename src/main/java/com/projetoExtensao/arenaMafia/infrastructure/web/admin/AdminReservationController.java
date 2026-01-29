package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CancelReservationByAdminUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CreateReservationByAdminUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request.AdminReservationCreateRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper.ReservationResponseMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationController {

  private final ReservationResponseMapper reservationResponseMapper;
  private final CancelReservationByAdminUseCase cancelReservationByAdminUseCase;
  private final CreateReservationByAdminUseCase createReservationByAdminUseCase;

  public AdminReservationController(
      ReservationResponseMapper reservationResponseMapper,
      CancelReservationByAdminUseCase cancelReservationByAdminUseCase,
      CreateReservationByAdminUseCase createReservationByAdminUseCase) {
    this.reservationResponseMapper = reservationResponseMapper;
    this.cancelReservationByAdminUseCase = cancelReservationByAdminUseCase;
    this.createReservationByAdminUseCase = createReservationByAdminUseCase;
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

  // TODO: Implementar endpoint para consultar reservas de usuários
}
