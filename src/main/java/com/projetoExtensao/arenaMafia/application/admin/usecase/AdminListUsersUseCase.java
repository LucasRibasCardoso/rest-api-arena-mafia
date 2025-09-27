package com.projetoExtensao.arenaMafia.application.admin.usecase;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.AdminUserSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminListUsersUseCase {

  Page<User> execute(AdminUserSearchRequest criteria, Pageable pageable);
}
