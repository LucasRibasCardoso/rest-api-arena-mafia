package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.user.usecase.admin.AdminListUsersUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.admin.AdminUpdateUserRoleUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.admin.AdminUpdateUserStatusUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.AdminUserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request.AdminUserSearchRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request.UpdateUserRoleRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request.UpdateUserStatusRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.response.AdminUserResponseDto;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final AdminUserMapper adminUserMapper;
  private final AdminListUsersUseCase adminListUsersUseCase;
  private final AdminUpdateUserStatusUseCase adminUpdateUserStatusUseCase;
  private final AdminUpdateUserRoleUseCase adminUpdateUserRoleUseCase;

  public AdminUserController(
      AdminUserMapper adminUserMapper,
      AdminListUsersUseCase adminListUsersUseCase,
      AdminUpdateUserStatusUseCase adminUpdateUserStatusUseCase,
      AdminUpdateUserRoleUseCase adminUpdateUserRoleUseCase) {
    this.adminUserMapper = adminUserMapper;
    this.adminListUsersUseCase = adminListUsersUseCase;
    this.adminUpdateUserStatusUseCase = adminUpdateUserStatusUseCase;
    this.adminUpdateUserRoleUseCase = adminUpdateUserRoleUseCase;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Page<AdminUserResponseDto>> listUsers(
      @Valid AdminUserSearchRequestDto request, Pageable pageable) {

    Page<User> users = adminListUsersUseCase.execute(request, pageable);
    Page<AdminUserResponseDto> responseDtoPage = users.map(adminUserMapper::toDto);
    return ResponseEntity.ok(responseDtoPage);
  }

  @PatchMapping("/{userId}/status")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> updateUserStatus(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin,
      @PathVariable UUID userId,
      @RequestBody @Valid UpdateUserStatusRequestDto request) {

    UUID adminId = authenticatedAdmin.user().getId();
    adminUpdateUserStatusUseCase.execute(adminId, userId, request.status());
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{userId}/role")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> updateUserRole(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedAdmin,
      @PathVariable UUID userId,
      @RequestBody @Valid UpdateUserRoleRequestDto request) {

    UUID adminId = authenticatedAdmin.user().getId();
    adminUpdateUserRoleUseCase.execute(adminId, userId, request.role());
    return ResponseEntity.noContent().build();
  }
}
