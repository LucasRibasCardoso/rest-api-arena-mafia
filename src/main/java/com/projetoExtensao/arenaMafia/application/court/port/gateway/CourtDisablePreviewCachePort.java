package com.projetoExtensao.arenaMafia.application.court.port.gateway;

import com.projetoExtensao.arenaMafia.application.court.preview.CourtDisablePreview;

import java.util.UUID;

public interface CourtDisablePreviewCachePort {

  void save(String key, CourtDisablePreview preview);

  CourtDisablePreview getPreviewOrElseThrow(String key, UUID userId);

  void delete(String key);

  String generateKey(UUID userId);
}
