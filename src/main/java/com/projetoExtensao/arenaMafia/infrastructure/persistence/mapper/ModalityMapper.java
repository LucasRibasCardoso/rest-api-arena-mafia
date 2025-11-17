package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface ModalityMapper {

  ModalityEntity toEntity(Modality modality);

  Modality toDomain(ModalityEntity entity);

  @Mapping(target = "isActive", source = "active")
  ModalityResponseDto toDto(Modality modality);

  @ObjectFactory
  default Modality createModality(ModalityEntity entity) {
    return Modality.reconstitute(
        entity.getId(), entity.getName(), entity.isActive(), entity.getCreatedAt());
  }
}
