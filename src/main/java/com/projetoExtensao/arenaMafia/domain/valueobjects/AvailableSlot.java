package com.projetoExtensao.arenaMafia.domain.valueobjects;

import java.math.BigDecimal;
import java.util.UUID;

public record AvailableSlot(UUID courtId, TimeInterval timeInterval, BigDecimal price) {}
