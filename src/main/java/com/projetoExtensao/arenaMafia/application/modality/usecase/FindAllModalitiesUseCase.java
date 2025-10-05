package com.projetoExtensao.arenaMafia.application.modality.usecase;

import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.List;

public interface FindAllModalitiesUseCase {

  List<Modality> execute();
}
