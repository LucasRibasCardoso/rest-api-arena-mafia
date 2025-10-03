package com.projetoExtensao.arenaMafia.application.admin.usecase.users;

import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.util.UUID;

public interface AdminUpdateUserRoleUseCase {
  void execute(UUID authenticatedAdminId, UUID targetUserId, RoleEnum role);
}
