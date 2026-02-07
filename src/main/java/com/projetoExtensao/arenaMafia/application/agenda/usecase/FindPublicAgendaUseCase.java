package com.projetoExtensao.arenaMafia.application.agenda.usecase;

import com.projetoExtensao.arenaMafia.domain.model.agenda.user.AgendaItem;
import java.time.LocalDate;
import java.util.List;

public interface FindPublicAgendaUseCase {

  List<AgendaItem> execute(LocalDate date);
}
