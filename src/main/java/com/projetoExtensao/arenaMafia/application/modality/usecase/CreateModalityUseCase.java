package com.projetoExtensao.arenaMafia.application.modality.usecase;

import com.projetoExtensao.arenaMafia.domain.model.Modality;

public interface CreateModalityUseCase {

  Modality execute(String name);
}
