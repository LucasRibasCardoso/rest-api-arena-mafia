package com.projetoExtensao.arenaMafia.unit.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.OperatingHoursMapper;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("Testes Unitários para OperatingHoursMapper")
class OperatingHoursMapperTest {

  private OperatingHoursMapper operatingHoursMapper;

  @BeforeEach
  void setup() {
    operatingHoursMapper = Mappers.getMapper(OperatingHoursMapper.class);
  }

  @Nested
  @DisplayName("Testes para o método toEntity()")
  class ToEntityTests {

    @Test
    @DisplayName("Deve converter OperatingHours para OperatingHoursEntity com sucesso")
    void toEntity_shouldConvertOperatingHoursToEntity_successfully() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(18, 0));
      OperatingHours operatingHours =
          OperatingHours.reconstitute(id, Set.of(DayOfWeek.MONDAY), timeInterval, true, now);

      // Act
      OperatingHoursEntity entity = operatingHoursMapper.toEntity(operatingHours);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getId()).isEqualTo(id);
      assertThat(entity.getDaysOfWeek()).isEqualTo(Set.of(DayOfWeek.MONDAY));
      assertThat(entity.getTimeInterval()).isEqualTo(timeInterval);
      assertThat(entity.isActive()).isTrue();
      assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve retornar null quando OperatingHours for null")
    void toEntity_shouldReturnNull_whenOperatingHoursIsNull() {
      // Arrange
      OperatingHours operatingHours = null;

      // Act
      OperatingHoursEntity entity = operatingHoursMapper.toEntity(operatingHours);

      // Assert
      assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Deve converter OperatingHours inativo para OperatingHoursEntity")
    void toEntity_shouldConvertInactiveOperatingHours() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(22, 0));
      OperatingHours operatingHours =
          OperatingHours.reconstitute(id, Set.of(DayOfWeek.FRIDAY), timeInterval, false, now);

      // Act
      OperatingHoursEntity entity = operatingHoursMapper.toEntity(operatingHours);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve converter OperatingHours com diferentes dias da semana")
    void toEntity_shouldConvertOperatingHoursWithDifferentDaysOfWeek() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(9, 30), LocalTime.of(17, 30));

      for (DayOfWeek day : DayOfWeek.values()) {
        OperatingHours operatingHours =
            OperatingHours.reconstitute(id, Set.of(day), timeInterval, true, now);

        // Act
        OperatingHoursEntity entity = operatingHoursMapper.toEntity(operatingHours);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getDaysOfWeek()).isEqualTo(Set.of(day));
      }
    }

    @Test
    @DisplayName("Deve converter OperatingHours com horários em minutos 00 e 30")
    void toEntity_shouldConvertOperatingHoursWithValidMinutes() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 30), LocalTime.of(18, 30));
      OperatingHours operatingHours =
          OperatingHours.reconstitute(id, Set.of(DayOfWeek.WEDNESDAY), timeInterval, true, now);

      // Act
      OperatingHoursEntity entity = operatingHoursMapper.toEntity(operatingHours);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getTimeInterval().openTime().getMinute()).isEqualTo(30);
      assertThat(entity.getTimeInterval().closeTime().getMinute()).isEqualTo(30);
    }
  }

  @Nested
  @DisplayName("Testes para o método toDomain()")
  class ToDomainTests {

    @Test
    @DisplayName("Deve converter OperatingHoursEntity para OperatingHours com sucesso")
    void toDomain_shouldConvertEntityToOperatingHours_successfully() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(18, 0));

      OperatingHoursEntity entity = new OperatingHoursEntity();
      entity.setId(id);
      entity.setDaysOfWeek(Set.of(DayOfWeek.MONDAY));
      entity.setTimeInterval(timeInterval);
      entity.setActive(true);
      entity.setCreatedAt(now);

      // Act
      OperatingHours operatingHours = operatingHoursMapper.toDomain(entity);

      // Assert
      assertThat(operatingHours).isNotNull();
      assertThat(operatingHours.getId()).isEqualTo(id);
      assertThat(operatingHours.getDaysOfWeek()).isEqualTo(Set.of(DayOfWeek.MONDAY));
      assertThat(operatingHours.getTimeInterval()).isEqualTo(timeInterval);
      assertThat(operatingHours.isActive()).isTrue();
      assertThat(operatingHours.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve retornar null quando OperatingHoursEntity for null")
    void toDomain_shouldReturnNull_whenEntityIsNull() {
      // Arrange
      OperatingHoursEntity entity = null;

      // Act
      OperatingHours operatingHours = operatingHoursMapper.toDomain(entity);

      // Assert
      assertThat(operatingHours).isNull();
    }

    @Test
    @DisplayName("Deve converter OperatingHoursEntity inativo para OperatingHours")
    void toDomain_shouldConvertInactiveEntity() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(22, 0));

      OperatingHoursEntity entity = new OperatingHoursEntity();
      entity.setId(id);
      entity.setDaysOfWeek(Set.of(DayOfWeek.FRIDAY));
      entity.setTimeInterval(timeInterval);
      entity.setActive(false);
      entity.setCreatedAt(now);

      // Act
      OperatingHours operatingHours = operatingHoursMapper.toDomain(entity);

      // Assert
      assertThat(operatingHours).isNotNull();
      assertThat(operatingHours.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve converter OperatingHoursEntity com diferentes dias da semana")
    void toDomain_shouldConvertEntityWithDifferentDaysOfWeek() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(9, 30), LocalTime.of(17, 30));

      for (DayOfWeek day : DayOfWeek.values()) {
        OperatingHoursEntity entity = new OperatingHoursEntity();
        entity.setId(id);
        entity.setDaysOfWeek(Set.of(day));
        entity.setTimeInterval(timeInterval);
        entity.setActive(true);
        entity.setCreatedAt(now);

        // Act
        OperatingHours operatingHours = operatingHoursMapper.toDomain(entity);

        // Assert
        assertThat(operatingHours).isNotNull();
        assertThat(operatingHours.getDaysOfWeek()).isEqualTo(Set.of(day));
      }
    }

    @Test
    @DisplayName("Deve converter OperatingHoursEntity com horários em minutos 00 e 30")
    void toDomain_shouldConvertEntityWithValidMinutes() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 30), LocalTime.of(18, 30));

      OperatingHoursEntity entity = new OperatingHoursEntity();
      entity.setId(id);
      entity.setDaysOfWeek(Set.of(DayOfWeek.WEDNESDAY));
      entity.setTimeInterval(timeInterval);
      entity.setActive(true);
      entity.setCreatedAt(now);

      // Act
      OperatingHours operatingHours = operatingHoursMapper.toDomain(entity);

      // Assert
      assertThat(operatingHours).isNotNull();
      assertThat(operatingHours.getTimeInterval().openTime().getMinute()).isEqualTo(30);
      assertThat(operatingHours.getTimeInterval().closeTime().getMinute()).isEqualTo(30);
    }
  }

  @Nested
  @DisplayName("Testes de conversão bidirecional")
  class BidirectionalConversionTests {

    @Test
    @DisplayName(
        "Deve manter os mesmos dados após conversão bidirecional OperatingHours -> Entity ->"
            + " OperatingHours")
    void shouldMaintainDataAfterBidirectionalConversion_domainToEntityToDomain() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(20, 0));
      OperatingHours originalOperatingHours =
          OperatingHours.reconstitute(id, Set.of(DayOfWeek.SATURDAY), timeInterval, true, now);

      // Act
      OperatingHoursEntity entity = operatingHoursMapper.toEntity(originalOperatingHours);
      OperatingHours convertedOperatingHours = operatingHoursMapper.toDomain(entity);

      // Assert
      assertThat(convertedOperatingHours).isNotNull();
      assertThat(convertedOperatingHours.getId()).isEqualTo(originalOperatingHours.getId());
      assertThat(convertedOperatingHours.getDaysOfWeek())
          .isEqualTo(originalOperatingHours.getDaysOfWeek());
      assertThat(convertedOperatingHours.getTimeInterval())
          .isEqualTo(originalOperatingHours.getTimeInterval());
      assertThat(convertedOperatingHours.isActive()).isEqualTo(originalOperatingHours.isActive());
      assertThat(convertedOperatingHours.getCreatedAt())
          .isEqualTo(originalOperatingHours.getCreatedAt());
    }

    @Test
    @DisplayName(
        "Deve manter os mesmos dados após conversão bidirecional Entity -> OperatingHours ->"
            + " Entity")
    void shouldMaintainDataAfterBidirectionalConversion_entityToDomainToEntity() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(20, 0));

      OperatingHoursEntity originalEntity = new OperatingHoursEntity();
      originalEntity.setId(id);
      originalEntity.setDaysOfWeek(Set.of(DayOfWeek.SUNDAY));
      originalEntity.setTimeInterval(timeInterval);
      originalEntity.setActive(false);
      originalEntity.setCreatedAt(now);

      // Act
      OperatingHours operatingHours = operatingHoursMapper.toDomain(originalEntity);
      OperatingHoursEntity convertedEntity = operatingHoursMapper.toEntity(operatingHours);

      // Assert
      assertThat(convertedEntity).isNotNull();
      assertThat(convertedEntity.getId()).isEqualTo(originalEntity.getId());
      assertThat(convertedEntity.getDaysOfWeek()).isEqualTo(originalEntity.getDaysOfWeek());
      assertThat(convertedEntity.getTimeInterval()).isEqualTo(originalEntity.getTimeInterval());
      assertThat(convertedEntity.isActive()).isEqualTo(originalEntity.isActive());
      assertThat(convertedEntity.getCreatedAt()).isEqualTo(originalEntity.getCreatedAt());
    }

    @Test
    @DisplayName(
        "Deve manter integridade dos dados em múltiplas conversões bidirecionais para todos os dias"
            + " da semana")
    void shouldMaintainDataIntegrityInMultipleBidirectionalConversions() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(7, 30), LocalTime.of(19, 30));

      for (DayOfWeek day : DayOfWeek.values()) {
        OperatingHours originalOperatingHours =
            OperatingHours.reconstitute(id, Set.of(day), timeInterval, true, now);

        // Act - primeira conversão
        OperatingHoursEntity entity1 = operatingHoursMapper.toEntity(originalOperatingHours);
        OperatingHours domain1 = operatingHoursMapper.toDomain(entity1);

        // Act - segunda conversão
        OperatingHoursEntity entity2 = operatingHoursMapper.toEntity(domain1);
        OperatingHours domain2 = operatingHoursMapper.toDomain(entity2);

        // Assert - os dados devem permanecer idênticos
        assertThat(domain2.getId()).isEqualTo(originalOperatingHours.getId());
        assertThat(domain2.getDaysOfWeek()).isEqualTo(originalOperatingHours.getDaysOfWeek());
        assertThat(domain2.getTimeInterval()).isEqualTo(originalOperatingHours.getTimeInterval());
        assertThat(domain2.isActive()).isEqualTo(originalOperatingHours.isActive());
        assertThat(domain2.getCreatedAt()).isEqualTo(originalOperatingHours.getCreatedAt());
      }
    }
  }
}
