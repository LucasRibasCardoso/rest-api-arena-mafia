package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class CourtMapper {

  @Autowired protected ModalityMapper modalityMapper;

  public CourtEntity toEntity(Court court) {
    if (court == null) {
      return null;
    }

    CourtEntity entity = new CourtEntity();
    entity.setId(court.getId());
    entity.setName(court.getName());
    entity.setDescription(court.getDescription());
    entity.setOffsetMinutes(court.getOffsetMinutes().getValue());
    entity.setActive(court.isActive());
    entity.setCreatedAt(court.getCreatedAt());

    Set<ModalityEntity> modalityEntities =
        court.getModalityIds().stream()
            .map(
                id -> {
                  ModalityEntity modalityEntity = new ModalityEntity();
                  modalityEntity.setId(id);
                  return modalityEntity;
                })
            .collect(Collectors.toSet());

    entity.setModalities(modalityEntities);
    return entity;
  }

  public Court toDomain(CourtEntity entity) {
    if (entity == null) {
      return null;
    }

    Set<UUID> modalityIds =
        entity.getModalities().stream().map(ModalityEntity::getId).collect(Collectors.toSet());

    return Court.reconstitute(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        OffsetMinutes.fromValue(entity.getOffsetMinutes()),
        entity.isActive(),
        modalityIds,
        entity.getCreatedAt());
  }
}
