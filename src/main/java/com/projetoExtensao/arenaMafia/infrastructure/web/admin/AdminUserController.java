package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.admin.usecase.AdminListUsersUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.AdminUserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.AdminUserSearchRequest;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.response.UserAdminResponseDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final AdminListUsersUseCase adminListUsersUseCase;
  private final AdminUserMapper adminUserMapper;

  public AdminUserController(
      AdminListUsersUseCase adminListUsersUseCase, AdminUserMapper adminUserMapper) {
    this.adminListUsersUseCase = adminListUsersUseCase;
    this.adminUserMapper = adminUserMapper;
  }

  @GetMapping
  public ResponseEntity<Page<UserAdminResponseDto>> listUsers(
      @Valid AdminUserSearchRequest request, Pageable pageable) {
    Page<UserAdminResponseDto> users =
        adminListUsersUseCase.execute(request, pageable).map(adminUserMapper::toDto);
    return ResponseEntity.ok(users);
  }
}
