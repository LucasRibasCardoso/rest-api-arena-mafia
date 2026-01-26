package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.agenda.usecase.FindAdminAgendaUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.agenda.response.AdminAgendaSlotResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.mapper.AdminAgendaMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/agenda")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAgendaController {

  private final FindAdminAgendaUseCase findAdminAgendaUseCase;
  private final AdminAgendaMapper adminAgendaMapper;

  public AdminAgendaController(
      FindAdminAgendaUseCase findAdminAgendaUseCase, AdminAgendaMapper adminAgendaMapper) {
    this.findAdminAgendaUseCase = findAdminAgendaUseCase;
    this.adminAgendaMapper = adminAgendaMapper;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<AdminAgendaSlotResponseDto>> getAgenda(
          @RequestParam("date") LocalDate date,
          @RequestParam(value = "courtId", required = false) UUID courtId
  ) {

    List<AdminAgendaSlotResponseDto> response = findAdminAgendaUseCase.execute(date, Optional.ofNullable(courtId))
            .stream()
            .map(adminAgendaMapper::toDto)
            .toList();

    return ResponseEntity.ok(response);
  }
}
