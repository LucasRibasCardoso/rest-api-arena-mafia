package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.response.AdminUserResponseDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {

  @Mapping(source = "id", target = "userId")
  AdminUserResponseDto toDto(User user);

  default LocalDate instantToLocalDate(Instant instant) {
    if (instant == null) {
      return null;
    }
    return instant.atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate();
  }
}
