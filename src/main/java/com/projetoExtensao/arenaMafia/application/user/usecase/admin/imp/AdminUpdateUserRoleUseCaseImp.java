package com.projetoExtensao.arenaMafia.application.user.usecase.admin.imp;

import com.projetoExtensao.arenaMafia.application.user.usecase.admin.AdminUpdateUserRoleUseCase;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AdminCannotUpdateOwnRoleException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AdminCannotUpdateRoleOfUnverifiedUserException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminUpdateUserRoleUseCaseImp implements AdminUpdateUserRoleUseCase {

  private final UserRepositoryPort userRepositoryPort;

  public AdminUpdateUserRoleUseCaseImp(UserRepositoryPort userRepositoryPort) {
    this.userRepositoryPort = userRepositoryPort;
  }

  @Override
  public void execute(UUID authenticatedAdminId, UUID targetUserId, RoleEnum role) {
    validateAdminIsNotUpdatingOwnRole(authenticatedAdminId, targetUserId);
    User user = userRepositoryPort.findByIdOrElseThrow(targetUserId);
    validateIfUserIsVerified(user);

    user.adminUpdateRole(role);
    userRepositoryPort.save(user);
  }

  private void validateAdminIsNotUpdatingOwnRole(UUID authenticatedAdminId, UUID targetUserId) {
    if (authenticatedAdminId.equals(targetUserId)) {
      throw new AdminCannotUpdateOwnRoleException(ErrorCode.ADMIN_CANNOT_UPDATE_OWN_ROLE);
    }
  }

  private void validateIfUserIsVerified(User user) {
    if (user.isPendingVerification()) {
      throw new AdminCannotUpdateRoleOfUnverifiedUserException(
          ErrorCode.ADMIN_CANNOT_UPDATE_ROLE_OF_UNVERIFIED_USER);
    }
  }
}
