package com.projetoExtensao.arenaMafia.infrastructure.web.agenda;

import com.projetoExtensao.arenaMafia.application.agenda.usecase.FindAllAgendaItemUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.mapper.AgendaMapper;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.AgendaSlotResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public/agenda")
public class AgendaController {

  private final AgendaMapper agendaMapper;
  private final FindAllAgendaItemUseCase findAllAgendaItemUseCase;

  public AgendaController(
          AgendaMapper agendaMapper, FindAllAgendaItemUseCase findAllAgendaItemUseCase) {
    this.agendaMapper = agendaMapper;
    this.findAllAgendaItemUseCase = findAllAgendaItemUseCase;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<AgendaSlotResponseDto>> getAgenda(@RequestParam("date") LocalDate date) {

    List<AgendaSlotResponseDto> agenda =
        findAllAgendaItemUseCase.execute(date).stream()
            .map(agendaMapper::toPublicDto)
            .toList();

    return ResponseEntity.ok(agenda);
  }
}
