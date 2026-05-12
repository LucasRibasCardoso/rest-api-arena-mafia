package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.FindAllBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.BlockedTimeEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.BlockedTimeSpecification;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindAllBlockedTimeUseCaseImp implements FindAllBlockedTimeUseCase {

  private final BlockedTimeRepositoryPort blockedTimeRepository;
  private final ScheduleEntryEnrichmentService enrichmentService;

  public FindAllBlockedTimeUseCaseImp(
      BlockedTimeRepositoryPort blockedTimeRepository,
      ScheduleEntryEnrichmentService enrichmentService) {
    this.blockedTimeRepository = blockedTimeRepository;
    this.enrichmentService = enrichmentService;
  }

  @Override
  public Page<BlockedTimeDetail> execute(UUID courtId, Pageable pageable) {
    // Define a ordenação padrão por data decrescente, se nenhuma ordenação for fornecida
    if (pageable.getSort().isUnsorted()) {
      pageable =
          PageRequest.of(
              pageable.getPageNumber(),
              pageable.getPageSize(),
              Sort.by(Sort.Direction.DESC, "dateTimeSlot.date"));
    }

    // Monta a especificação com base nos filtros fornecidos e realiza a busca
    Specification<BlockedTimeEntity> spec = buildSpecification(courtId);
    Page<BlockedTime> blockedTimePage = blockedTimeRepository.search(spec, pageable);

    // Enriquecer e retornar os resultados
    return enrichBlockedTime(blockedTimePage);
  }

  private Specification<BlockedTimeEntity> buildSpecification(UUID courtId) {
    Specification<BlockedTimeEntity> specification = Specification.unrestricted();

    if (courtId != null) {
      specification = specification.and(BlockedTimeSpecification.byCourtId(courtId));
    }

    return specification;
  }

  private Page<BlockedTimeDetail> enrichBlockedTime(Page<BlockedTime> blockedTimePage) {
    return blockedTimePage.map(enrichmentService::enrichBlockedTime);
  }
}
