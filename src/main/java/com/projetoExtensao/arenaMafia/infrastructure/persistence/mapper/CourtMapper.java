package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {OffsetMinutesMapper.class})
public abstract class CourtMapper {

  @Mapping(target = "modalities", ignore = true)
  public abstract CourtEntity toEntity(Court court);

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
