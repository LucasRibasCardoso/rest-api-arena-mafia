package com.projetoExtensao.arenaMafia.infrastructure.web.agenda;

import com.projetoExtensao.arenaMafia.application.agenda.usecase.FindPublicAgendaUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.AgendaSlotResponseDto;
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


  public AgendaController(AgendaMapper agendaMapper, FindPublicAgendaUseCase findPublicAgendaUseCase) {
    this.agendaMapper = agendaMapper;
    this.findPublicAgendaUseCase = findPublicAgendaUseCase;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<AgendaSlotResponseDto>> getAgenda(@RequestParam("date") LocalDate date) {

    List<AgendaSlotResponseDto> agenda = findPublicAgendaUseCase.execute(date).stream().map(agendaMapper::toPublicDto).toList();

    return ResponseEntity.ok(agenda);
  }
}
