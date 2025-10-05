package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ModalityMapper {

  ModalityEntity toEntity(Modality modality);

  default Modality toDomain(ModalityEntity entity) {
    if (entity == null) {
      return null;
    }
    return Modality.reconstitute(entity.getId(), entity.getName(), entity.getCreatedAt());
  }

  default ModalityResponseDto toResponseDto(Modality modality) {
    if (modality == null) {
      return null;
    }
    return new ModalityResponseDto(modality.getId(), modality.getName(), modality.getCreatedAt());
  }
}
