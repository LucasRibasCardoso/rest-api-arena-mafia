package com.projetoExtensao.arenaMafia.application.user.usecase.admin;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import java.util.UUID;

public interface AdminUpdateUserStatusUseCase {
  void execute(UUID authenticatedAdminId, UUID targetUserId, AccountStatus status);
}
