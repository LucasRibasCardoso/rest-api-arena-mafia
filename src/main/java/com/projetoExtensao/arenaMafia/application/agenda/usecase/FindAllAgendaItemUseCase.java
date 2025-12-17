package com.projetoExtensao.arenaMafia.application.agenda.usecase;

import com.projetoExtensao.arenaMafia.domain.model.agenda.AgendaItem;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FindAllAgendaItemUseCase {

  List<AgendaItem> execute(LocalDate date);
}
