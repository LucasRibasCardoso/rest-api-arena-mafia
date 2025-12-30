package com.projetoExtensao.arenaMafia.application.admin.usecase.users;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request.AdminUserSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminListUsersUseCase {

  Page<User> execute(AdminUserSearchRequestDto criteria, Pageable pageable);
}
