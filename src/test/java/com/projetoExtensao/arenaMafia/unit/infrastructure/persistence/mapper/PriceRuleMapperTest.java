package com.projetoExtensao.arenaMafia.unit.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.PriceRuleEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.PriceRuleMapper;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.priceRule.dto.response.PriceRuleResponseDto;
import com.projetoExtensao.arenaMafia.unit.config.TestPriceRuleDataProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("Testes Unitários para PriceRuleMapper")
class PriceRuleMapperTest {

  private PriceRuleMapper priceRuleMapper;

  @BeforeEach
  void setup() {
    priceRuleMapper = Mappers.getMapper(PriceRuleMapper.class);
  }

  @Nested
  @DisplayName("Testes para o método toEntity()")
  class ToEntityTests {

    @Test
    @DisplayName("Deve converter PriceRule para PriceRuleEntity com sucesso")
    void toEntity_shouldConvertPriceRuleToPriceRuleEntity_successfully() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();

      // Act
      PriceRuleEntity entity = priceRuleMapper.toEntity(priceRule);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getId()).isEqualTo(priceRule.getId());
      assertThat(entity.getName()).isEqualTo(priceRule.getName());
      assertThat(entity.getDaysOfWeek()).isEqualTo(priceRule.getDaysOfWeek());
      assertThat(entity.getTimeInterval()).isEqualTo(priceRule.getTimeInterval());
      assertThat(entity.getPrice()).isEqualTo(priceRule.getPrice());
      assertThat(entity.getPriority()).isEqualTo(priceRule.getPriority());
      assertThat(entity.isDefault()).isEqualTo(priceRule.isDefault());
      assertThat(entity.isActive()).isEqualTo(priceRule.isActive());
      assertThat(entity.getCreatedAt()).isEqualTo(priceRule.getCreatedAt());
    }

    @Test
    @DisplayName("Deve retornar null quando PriceRule for null")
    void toEntity_shouldReturnNull_whenPriceRuleIsNull() {
      // Arrange
      PriceRule priceRule = null;

      // Act
      PriceRuleEntity entity = priceRuleMapper.toEntity(priceRule);

      // Assert
      assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Deve converter PriceRule inativo para PriceRuleEntity")
    void toEntity_shouldConvertInactivePriceRule() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createDisabledPriceRule();

      // Act
      PriceRuleEntity entity = priceRuleMapper.toEntity(priceRule);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve converter PriceRule padrão para PriceRuleEntity")
    void toEntity_shouldConvertDefaultPriceRule() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createDefaultPriceRule();

      // Act
      PriceRuleEntity entity = priceRuleMapper.toEntity(priceRule);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.isDefault()).isTrue();
      assertThat(entity.getDaysOfWeek()).isNull();
      assertThat(entity.getTimeInterval()).isNull();
      assertThat(entity.getPriority()).isZero();
    }

    @Test
    @DisplayName("Deve converter PriceRule de final de semana")
    void toEntity_shouldConvertWeekendPriceRule() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createWeekendPriceRule();

      // Act
      PriceRuleEntity entity = priceRuleMapper.toEntity(priceRule);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getDaysOfWeek())
          .containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
      assertThat(entity.getPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Deve converter PriceRule aplicável a todos os dias")
    void toEntity_shouldConvertAllDayPriceRule() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createAllDayPriceRule();

      // Act
      PriceRuleEntity entity = priceRuleMapper.toEntity(priceRule);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getDaysOfWeek()).isNull();
      assertThat(entity.getTimeInterval()).isNull();
    }

    @Test
    @DisplayName("Deve converter PriceRule com diferentes prioridades")
    void toEntity_shouldConvertPriceRuleWithDifferentPriorities() {
      // Arrange
      for (int priority = 1; priority <= 5; priority++) {
        PriceRule priceRule =
            TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
                .withPriority(priority)
                .build();

        // Act
        PriceRuleEntity entity = priceRuleMapper.toEntity(priceRule);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getPriority()).isEqualTo(priority);
      }
    }
  }

  @Nested
  @DisplayName("Testes para o método toDomain()")
  class ToDomainTests {

    @Test
    @DisplayName("Deve converter PriceRuleEntity para PriceRule com sucesso")
    void toDomain_shouldConvertEntityToPriceRule_successfully() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(23, 0));
      Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
      BigDecimal price = new BigDecimal("80.00");

      PriceRuleEntity entity = new PriceRuleEntity();
      entity.setId(id);
      entity.setName("Preço Noturno");
      entity.setDaysOfWeek(daysOfWeek);
      entity.setTimeInterval(timeInterval);
      entity.setPrice(price);
      entity.setPriority(2);
      entity.setDefault(false);
      entity.setActive(true);
      entity.setCreatedAt(now);

      // Act
      PriceRule priceRule = priceRuleMapper.toDomain(entity);

      // Assert
      assertThat(priceRule).isNotNull();
      assertThat(priceRule.getId()).isEqualTo(id);
      assertThat(priceRule.getName()).isEqualTo("Preço Noturno");
      assertThat(priceRule.getDaysOfWeek()).isEqualTo(daysOfWeek);
      assertThat(priceRule.getTimeInterval()).isEqualTo(timeInterval);
      assertThat(priceRule.getPrice()).isEqualByComparingTo(price);
      assertThat(priceRule.getPriority()).isEqualTo(2);
      assertThat(priceRule.isDefault()).isFalse();
      assertThat(priceRule.isActive()).isTrue();
      assertThat(priceRule.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve retornar null quando PriceRuleEntity for null")
    void toDomain_shouldReturnNull_whenEntityIsNull() {
      // Arrange
      PriceRuleEntity entity = null;

      // Act
      PriceRule priceRule = priceRuleMapper.toDomain(entity);

      // Assert
      assertThat(priceRule).isNull();
    }

    @Test
    @DisplayName("Deve converter PriceRuleEntity inativo para PriceRule")
    void toDomain_shouldConvertInactiveEntity() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(12, 0));

      PriceRuleEntity entity = new PriceRuleEntity();
      entity.setId(id);
      entity.setName("Preço Matinal");
      entity.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY));
      entity.setTimeInterval(timeInterval);
      entity.setPrice(new BigDecimal("60.00"));
      entity.setPriority(1);
      entity.setDefault(false);
      entity.setActive(false);
      entity.setCreatedAt(now);

      // Act
      PriceRule priceRule = priceRuleMapper.toDomain(entity);

      // Assert
      assertThat(priceRule).isNotNull();
      assertThat(priceRule.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve converter PriceRuleEntity padrão para PriceRule")
    void toDomain_shouldConvertDefaultEntity() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();

      PriceRuleEntity entity = new PriceRuleEntity();
      entity.setId(id);
      entity.setName("Preço Base");
      entity.setDaysOfWeek(null);
      entity.setTimeInterval(null);
      entity.setPrice(new BigDecimal("50.00"));
      entity.setPriority(1);
      entity.setDefault(true);
      entity.setActive(true);
      entity.setCreatedAt(now);

      // Act
      PriceRule priceRule = priceRuleMapper.toDomain(entity);

      // Assert
      assertThat(priceRule).isNotNull();
      assertThat(priceRule.isDefault()).isTrue();
      assertThat(priceRule.getDaysOfWeek()).isNull();
      assertThat(priceRule.getTimeInterval()).isNull();
      assertThat(priceRule.getPriority()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve converter PriceRuleEntity com daysOfWeek vazio para null")
    void toDomain_shouldConvertEntityWithEmptyDaysOfWeekToNull() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();

      PriceRuleEntity entity = new PriceRuleEntity();
      entity.setId(id);
      entity.setName("Teste");
      entity.setDaysOfWeek(Set.of());
      entity.setTimeInterval(null);
      entity.setPrice(new BigDecimal("70.00"));
      entity.setPriority(1);
      entity.setDefault(false);
      entity.setActive(true);
      entity.setCreatedAt(now);

      // Act
      PriceRule priceRule = priceRuleMapper.toDomain(entity);

      // Assert
      assertThat(priceRule).isNotNull();
      assertThat(priceRule.getDaysOfWeek()).isNull();
    }
  }

  @Nested
  @DisplayName("Testes para o método toDto()")
  class ToDtoTests {

    @Test
    @DisplayName("Deve converter PriceRule para PriceRuleResponseDto com sucesso")
    void toDto_shouldConvertPriceRuleToPriceRuleResponseDto_successfully() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createActivePriceRule();

      // Act
      PriceRuleResponseDto dto = priceRuleMapper.toDto(priceRule);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.id()).isEqualTo(priceRule.getId());
      assertThat(dto.name()).isEqualTo(priceRule.getName());
      assertThat(dto.daysOfWeek()).isEqualTo(priceRule.getDaysOfWeek());
      assertThat(dto.timeInterval()).isNotNull();
      assertThat(dto.timeInterval().startTime()).isEqualTo(priceRule.getTimeInterval().startTime());
      assertThat(dto.timeInterval().endTime()).isEqualTo(priceRule.getTimeInterval().endTime());
      assertThat(dto.price()).isEqualByComparingTo(priceRule.getPrice());
      assertThat(dto.priority()).isEqualTo(priceRule.getPriority());
      assertThat(dto.isDefault()).isEqualTo(priceRule.isDefault());
      assertThat(dto.isActive()).isEqualTo(priceRule.isActive());
    }

    @Test
    @DisplayName("Deve retornar null quando PriceRule for null")
    void toDto_shouldReturnNull_whenPriceRuleIsNull() {
      // Arrange
      PriceRule priceRule = null;

      // Act
      PriceRuleResponseDto dto = priceRuleMapper.toDto(priceRule);

      // Assert
      assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Deve converter PriceRule com daysOfWeek null para DTO com daysOfWeek null")
    void toDto_shouldConvertPriceRuleWithNullDaysOfWeek() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createDefaultPriceRule();

      // Act
      PriceRuleResponseDto dto = priceRuleMapper.toDto(priceRule);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.daysOfWeek()).isNull();
    }

    @Test
    @DisplayName("Deve converter PriceRule com timeInterval null para DTO com timeInterval null")
    void toDto_shouldConvertPriceRuleWithNullTimeInterval() {
      // Arrange
      PriceRule priceRule = TestPriceRuleDataProvider.createAllDayPriceRule();

      // Act
      PriceRuleResponseDto dto = priceRuleMapper.toDto(priceRule);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.timeInterval()).isNull();
    }

    @Test
    @DisplayName("Deve converter TimeInterval corretamente para TimeIntervalDto")
    void toDto_shouldConvertTimeIntervalCorrectly() {
      // Arrange
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 30), LocalTime.of(18, 0));
      PriceRule priceRule =
          TestPriceRuleDataProvider.PriceRuleBuilder.defaultPriceRule()
              .withTimeInterval(timeInterval)
              .build();

      // Act
      PriceRuleResponseDto dto = priceRuleMapper.toDto(priceRule);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.timeInterval()).isNotNull();
      assertThat(dto.timeInterval().startTime()).isEqualTo(LocalTime.of(14, 30));
      assertThat(dto.timeInterval().endTime()).isEqualTo(LocalTime.of(18, 0));
    }
  }

  @Nested
  @DisplayName("Testes de conversão bidirecional")
  class BidirectionalConversionTests {

    @Test
    @DisplayName(
        "Deve manter os mesmos dados após conversão bidirecional PriceRule -> Entity -> PriceRule")
    void shouldMaintainDataAfterBidirectionalConversion_domainToEntityToDomain() {
      // Arrange
      PriceRule originalPriceRule = TestPriceRuleDataProvider.createActivePriceRule();

      // Act
      PriceRuleEntity entity = priceRuleMapper.toEntity(originalPriceRule);
      PriceRule convertedPriceRule = priceRuleMapper.toDomain(entity);

      // Assert
      assertThat(convertedPriceRule).isNotNull();
      assertThat(convertedPriceRule.getId()).isEqualTo(originalPriceRule.getId());
      assertThat(convertedPriceRule.getName()).isEqualTo(originalPriceRule.getName());
      assertThat(convertedPriceRule.getDaysOfWeek()).isEqualTo(originalPriceRule.getDaysOfWeek());
      assertThat(convertedPriceRule.getTimeInterval())
          .isEqualTo(originalPriceRule.getTimeInterval());
      assertThat(convertedPriceRule.getPrice()).isEqualByComparingTo(originalPriceRule.getPrice());
      assertThat(convertedPriceRule.getPriority()).isEqualTo(originalPriceRule.getPriority());
      assertThat(convertedPriceRule.isDefault()).isEqualTo(originalPriceRule.isDefault());
      assertThat(convertedPriceRule.isActive()).isEqualTo(originalPriceRule.isActive());
      assertThat(convertedPriceRule.getCreatedAt()).isEqualTo(originalPriceRule.getCreatedAt());
    }

    @Test
    @DisplayName(
        "Deve manter os mesmos dados após conversão bidirecional Entity -> PriceRule -> Entity")
    void shouldMaintainDataAfterBidirectionalConversion_entityToDomainToEntity() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));
      Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

      PriceRuleEntity originalEntity = new PriceRuleEntity();
      originalEntity.setId(id);
      originalEntity.setName("Preço Weekend");
      originalEntity.setDaysOfWeek(daysOfWeek);
      originalEntity.setTimeInterval(timeInterval);
      originalEntity.setPrice(new BigDecimal("120.00"));
      originalEntity.setPriority(3);
      originalEntity.setDefault(false);
      originalEntity.setActive(true);
      originalEntity.setCreatedAt(now);

      // Act
      PriceRule priceRule = priceRuleMapper.toDomain(originalEntity);
      PriceRuleEntity convertedEntity = priceRuleMapper.toEntity(priceRule);

      // Assert
      assertThat(convertedEntity).isNotNull();
      assertThat(convertedEntity.getId()).isEqualTo(originalEntity.getId());
      assertThat(convertedEntity.getName()).isEqualTo(originalEntity.getName());
      assertThat(convertedEntity.getDaysOfWeek()).isEqualTo(originalEntity.getDaysOfWeek());
      assertThat(convertedEntity.getTimeInterval()).isEqualTo(originalEntity.getTimeInterval());
      assertThat(convertedEntity.getPrice()).isEqualByComparingTo(originalEntity.getPrice());
      assertThat(convertedEntity.getPriority()).isEqualTo(originalEntity.getPriority());
      assertThat(convertedEntity.isDefault()).isEqualTo(originalEntity.isDefault());
      assertThat(convertedEntity.isActive()).isEqualTo(originalEntity.isActive());
      assertThat(convertedEntity.getCreatedAt()).isEqualTo(originalEntity.getCreatedAt());
    }

    @Test
    @DisplayName("Deve manter valores null após conversão bidirecional")
    void shouldMaintainNullValuesAfterBidirectionalConversion() {
      // Arrange
      PriceRule originalPriceRule = TestPriceRuleDataProvider.createDefaultPriceRule();

      // Act
      PriceRuleEntity entity = priceRuleMapper.toEntity(originalPriceRule);
      PriceRule convertedPriceRule = priceRuleMapper.toDomain(entity);

      // Assert
      assertThat(convertedPriceRule).isNotNull();
      assertThat(convertedPriceRule.getDaysOfWeek()).isNull();
      assertThat(convertedPriceRule.getTimeInterval()).isNull();
    }
  }

  @Nested
  @DisplayName("Testes de métodos auxiliares")
  class HelperMethodsTests {

    @Test
    @DisplayName("toTimeIntervalDto() deve converter TimeInterval corretamente")
    void toTimeIntervalDto_shouldConvertTimeIntervalCorrectly() {
      // Arrange
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(12, 0));

      // Act
      TimeIntervalDto dto = priceRuleMapper.toTimeIntervalDto(timeInterval);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.startTime()).isEqualTo(LocalTime.of(8, 0));
      assertThat(dto.endTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("toTimeIntervalDto() deve retornar null quando TimeInterval for null")
    void toTimeIntervalDto_shouldReturnNull_whenTimeIntervalIsNull() {
      // Arrange
      TimeInterval timeInterval = null;

      // Act
      TimeIntervalDto dto = priceRuleMapper.toTimeIntervalDto(timeInterval);

      // Assert
      assertThat(dto).isNull();
    }

    @Test
    @DisplayName("getDaysOfWeek() deve retornar null quando Set estiver vazio")
    void getDaysOfWeek_shouldReturnNull_whenSetIsEmpty() {
      // Arrange
      Set<DayOfWeek> daysOfWeek = Set.of();

      // Act
      Set<DayOfWeek> result = priceRuleMapper.getDaysOfWeek(daysOfWeek);

      // Assert
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("getDaysOfWeek() deve retornar null quando Set for null")
    void getDaysOfWeek_shouldReturnNull_whenSetIsNull() {
      // Arrange
      Set<DayOfWeek> daysOfWeek = null;

      // Act
      Set<DayOfWeek> result = priceRuleMapper.getDaysOfWeek(daysOfWeek);

      // Assert
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("getDaysOfWeek() deve retornar o mesmo Set quando não está vazio")
    void getDaysOfWeek_shouldReturnSameSet_whenNotEmpty() {
      // Arrange
      Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

      // Act
      Set<DayOfWeek> result = priceRuleMapper.getDaysOfWeek(daysOfWeek);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(daysOfWeek);
    }
  }
}
