package com.projetoExtensao.arenaMafia.application.admin.usecase.users.imp;

import com.projetoExtensao.arenaMafia.application.admin.usecase.users.AdminUpdateUserStatusUseCase;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AdminCannotUpdateOwnStatusException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AdminCannotUpdateStatusOfUnverifiedUserException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminUpdateUserStatusUseCaseImp implements AdminUpdateUserStatusUseCase {

  private final UserRepositoryPort userRepositoryPort;

  public AdminUpdateUserStatusUseCaseImp(UserRepositoryPort userRepositoryPort) {
    this.userRepositoryPort = userRepositoryPort;
  }

  @Override
  public void execute(UUID authenticatedAdminId, UUID targetUserId, AccountStatus status) {
    validateAdminIsNotUpdatingOwnStatus(authenticatedAdminId, targetUserId);
    User user = userRepositoryPort.findByIdOrElseThrow(targetUserId);
    validateIfUserIsVerified(user);

    user.adminUpdateStatus(status);
    userRepositoryPort.save(user);
  }

  private void validateAdminIsNotUpdatingOwnStatus(UUID authenticatedAdminId, UUID targetUserId) {
    if (authenticatedAdminId.equals(targetUserId)) {
      throw new AdminCannotUpdateOwnStatusException(ErrorCode.ADMIN_CANNOT_UPDATE_OWN_STATUS);
    }
  }

  private void validateIfUserIsVerified(User user) {
    if (user.isPendingVerification()) {
      throw new AdminCannotUpdateStatusOfUnverifiedUserException(
          ErrorCode.ADMIN_CANNOT_UPDATE_STATUS_OF_UNVERIFIED_USER);
    }
  }
}
