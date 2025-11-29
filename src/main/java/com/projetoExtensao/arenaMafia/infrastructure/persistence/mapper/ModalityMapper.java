package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public abstract class ModalityMapper {

  public abstract ModalityEntity toEntity(Modality modality);

  public abstract Modality toDomain(ModalityEntity entity);

  @Mapping(target = "isActive", source = "active")
  public abstract ModalityResponseDto toDto(Modality modality);

  @ObjectFactory
  public Modality createModality(ModalityEntity entity) {
    return Modality.reconstitute(
        entity.getId(), entity.getName(), entity.isActive(), entity.getCreatedAt());
  }
}
