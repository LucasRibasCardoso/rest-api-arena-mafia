package com.projetoExtensao.arenaMafia.application.user.usecase.disable.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.disable.DisableMyAccountUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisableMyAccountUseCaseImp implements DisableMyAccountUseCase {

  private final UserRepositoryPort userRepository;
  private final ReservationRepositoryPort reservationRepository;
  private final ReservationBatchCancellationService reservationBatchCancellationService;

  public DisableMyAccountUseCaseImp(
      UserRepositoryPort userRepository,
      ReservationRepositoryPort reservationRepository,
      ReservationBatchCancellationService reservationBatchCancellationService) {
    this.userRepository = userRepository;
    this.reservationRepository = reservationRepository;
    this.reservationBatchCancellationService = reservationBatchCancellationService;
  }

  @Override
  public void execute(UUID idCurrentUser) {
    User user = userRepository.findByIdOrElseThrow(idCurrentUser);
    user.disableAccount();
    userRepository.save(user);

    cancelFutureReservations(idCurrentUser);
  }

  private void cancelFutureReservations(UUID userId) {
    List<Reservation> futureReservations =
        reservationRepository.findAllFutureActiveReservationsByUser(userId);
    reservationBatchCancellationService.cancelReservationsInBatchSilently(futureReservations);
  }
}
