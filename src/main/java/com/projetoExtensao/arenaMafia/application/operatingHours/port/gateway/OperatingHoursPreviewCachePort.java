package com.projetoExtensao.arenaMafia.application.operatingHours.port.gateway;

import com.projetoExtensao.arenaMafia.application.operatingHours.preview.OperatingHoursDisablePreview;
import java.util.UUID;

public interface OperatingHoursPreviewCachePort {

  void save(String key, OperatingHoursDisablePreview preview);

  OperatingHoursDisablePreview getPreviewOrElseThrow(String key, UUID userId);

  void delete(String key);

  String generateKey(UUID userId);
}
