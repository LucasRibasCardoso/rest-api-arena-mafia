package com.projetoExtensao.arenaMafia.unit.config;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class TestCourtDataProvider {

  private TestCourtDataProvider() {}

  public static final String defaultName = "Quadra Principal";
  public static final String defaultDescription = "Quadra coberta com iluminação";
  public static final OffsetMinutes defaultOffsetMinutes = OffsetMinutes.ZERO;
  public static final Set<UUID> defaultModalityIds = Set.of(UUID.randomUUID(), UUID.randomUUID());

  public static Court createActiveCourt() {
    return CourtBuilder.defaultCourt()
        .withIsActive(true)
        .withModalityIds(Set.of(UUID.randomUUID()))
        .build();
  }

  public static Court createDisabledCourt() {
    return CourtBuilder.defaultCourt()
        .withIsActive(false)
        .withModalityIds(Set.of(UUID.randomUUID()))
        .build();
  }

  public static Court createCourtWithMultipleModalities(int modalityCount) {
    Set<UUID> modalityIds = Set.of();
    for (int i = 0; i < modalityCount; i++) {
      modalityIds = new java.util.HashSet<>(modalityIds);
      modalityIds.add(UUID.randomUUID());
    }
    return CourtBuilder.defaultCourt().withModalityIds(Set.copyOf(modalityIds)).build();
  }

  public static class CourtBuilder {
    private UUID id = UUID.randomUUID();
    private String name = defaultName;
    private String description = defaultDescription;
    private OffsetMinutes offsetMinutes = defaultOffsetMinutes;
    private boolean isActive = true;
    private Set<UUID> modalityIds = defaultModalityIds;
    private Instant createdAt = Instant.now();

    public static CourtBuilder defaultCourt() {
      return new CourtBuilder();
    }

    public CourtBuilder withId(UUID id) {
      this.id = id;
      return this;
    }

    public CourtBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public CourtBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public CourtBuilder withOffsetMinutes(OffsetMinutes offsetMinutes) {
      this.offsetMinutes = offsetMinutes;
      return this;
    }

    public CourtBuilder withIsActive(boolean isActive) {
      this.isActive = isActive;
      return this;
    }

    public CourtBuilder withModalityIds(Set<UUID> modalityIds) {
      this.modalityIds = modalityIds;
      return this;
    }

    public CourtBuilder withCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Court build() {
      return Court.reconstitute(
          id, name, description, offsetMinutes, isActive, modalityIds, createdAt);
    }
  }

  public static Stream<Arguments> invalidCourtNameProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.COURT_NAME_REQUIRED),
        Arguments.of("  ", ErrorCode.COURT_NAME_REQUIRED),
        Arguments.of("A", ErrorCode.COURT_NAME_INVALID_LENGTH),
        Arguments.of("AB", ErrorCode.COURT_NAME_INVALID_LENGTH),
        Arguments.of("A".repeat(101), ErrorCode.COURT_NAME_INVALID_LENGTH));
  }

  public static Stream<Arguments> invalidModalityIdsProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.COURT_MODALITY_REQUIRED),
        Arguments.of(Set.of(), ErrorCode.COURT_MODALITY_REQUIRED));
  }
}
