package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import org.mapstruct.Mapper;

/**
 * Mapper auxiliar para conversão entre OffsetMinutes (enum) e Integer.
 *
 * <p>Usado por outros mappers através da propriedade 'uses' do MapStruct.
 */
@Mapper(componentModel = "spring")
public interface OffsetMinutesMapper {

  default Integer toInteger(OffsetMinutes offsetMinutes) {
    return offsetMinutes != null ? offsetMinutes.getValue() : null;
  }

  default OffsetMinutes toOffsetMinutes(Integer value) {
    return value != null ? OffsetMinutes.fromValue(value) : null;
  }
}
