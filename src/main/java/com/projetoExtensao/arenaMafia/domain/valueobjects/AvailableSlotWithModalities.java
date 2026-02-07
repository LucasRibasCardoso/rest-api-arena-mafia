package com.projetoExtensao.arenaMafia.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record AvailableSlotWithModalities(UUID courtId, TimeInterval timeInterval, BigDecimal price, Set<UUID> modalityIds) {}
