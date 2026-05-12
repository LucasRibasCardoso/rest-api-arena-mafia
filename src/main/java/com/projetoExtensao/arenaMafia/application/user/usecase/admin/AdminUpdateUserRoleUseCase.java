package com.projetoExtensao.arenaMafia.application.user.usecase.admin;

import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.util.UUID;

public interface AdminUpdateUserRoleUseCase {
  void execute(UUID authenticatedAdminId, UUID targetUserId, RoleEnum role);
}
