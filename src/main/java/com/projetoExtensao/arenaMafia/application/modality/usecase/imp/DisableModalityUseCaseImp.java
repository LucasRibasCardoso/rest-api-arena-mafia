package com.projetoExtensao.arenaMafia.application.modality.usecase.imp;

import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.usecase.DisableModalityUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ModalityInUseException;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisableModalityUseCaseImp implements DisableModalityUseCase {

    private final ModalityRepositoryPort modalityRepositoryPort;

    public DisableModalityUseCaseImp(ModalityRepositoryPort modalityRepositoryPort) {
        this.modalityRepositoryPort = modalityRepositoryPort;
    }

    @Override
    public void execute(UUID id) {
        Modality modality = modalityRepositoryPort.findByIdOrElseThrow(id);
        validateModalityNotInUse(id);
        modality.disable();
        modalityRepositoryPort.save(modality);
    }

    private void validateModalityNotInUse(UUID modalityId) {
        if (modalityRepositoryPort.existsCourtsByModalityId(modalityId)) {
            throw new ModalityInUseException();
        }
    }
}
