package com.projetoExtensao.arenaMafia.infrastructure.web.agenda;

import com.projetoExtensao.arenaMafia.application.agenda.usecase.FindPublicAgendaUseCase;
import com.projetoExtensao.arenaMafia.domain.model.agenda.user.AgendaItem;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.PublicAgendaItemResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.mapper.AgendaMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/agenda")
public class AgendaController {

  private final AgendaMapper agendaMapper;
  private final FindPublicAgendaUseCase findPublicAgendaUseCase;

  public AgendaController(
      AgendaMapper agendaMapper, FindPublicAgendaUseCase findPublicAgendaUseCase) {
    this.agendaMapper = agendaMapper;
    this.findPublicAgendaUseCase = findPublicAgendaUseCase;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<PublicAgendaItemResponseDto>> getAgenda(
      @RequestParam("date") LocalDate date) {

    List<AgendaItem> agendaItems = findPublicAgendaUseCase.execute(date);
    List<PublicAgendaItemResponseDto> agendaResponse =
        agendaItems.stream().map(agendaMapper::toDto).toList();

    return ResponseEntity.ok(agendaResponse);
  }
}
