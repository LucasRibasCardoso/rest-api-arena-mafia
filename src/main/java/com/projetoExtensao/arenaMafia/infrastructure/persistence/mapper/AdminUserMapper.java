package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.response.UserAdminResponseDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {

  @Mappings({
    @Mapping(source = "id", target = "userId"),
    @Mapping(source = "username", target = "username"),
    @Mapping(source = "fullName", target = "fullName"),
    @Mapping(source = "phone", target = "phone"),
    @Mapping(source = "status", target = "status"),
    @Mapping(source = "role", target = "role"),
  })
  UserAdminResponseDto toDto(User user);

  default LocalDate instantToLocalDate(Instant instant) {
    if (instant == null) {
      return null;
    }
    return instant.atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate();
  }
}
