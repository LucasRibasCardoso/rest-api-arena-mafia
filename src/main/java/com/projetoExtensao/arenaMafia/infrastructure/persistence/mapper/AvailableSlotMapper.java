package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.valueobjects.AvailableSlot;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.AvailableSlotResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AvailableSlotMapper {

  AvailableSlotResponseDto toDto(AvailableSlot availableSlot);
}
