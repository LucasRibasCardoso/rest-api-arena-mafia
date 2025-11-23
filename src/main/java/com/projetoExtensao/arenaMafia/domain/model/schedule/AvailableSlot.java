package com.projetoExtensao.arenaMafia.domain.model.schedule;

import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;

import java.math.BigDecimal;
import java.util.UUID;

public class AvailableSlot {

  private final UUID courtId;
  private final TimeInterval timeInterval;
  private final BigDecimal price;

  public static AvailableSlot create(UUID courtId, TimeInterval timeInterval, BigDecimal price) {
    return new AvailableSlot(courtId, timeInterval, price);
  }

  private AvailableSlot(UUID courtId, TimeInterval timeInterval, BigDecimal price) {
    this.courtId = courtId;
    this.timeInterval = timeInterval;
    this.price = price;
  }

  public UUID getCourtId() {
    return courtId;
  }

  public TimeInterval getTimeInterval() {
    return timeInterval;
  }

  public BigDecimal getPrice() {
    return price;
  }
}
