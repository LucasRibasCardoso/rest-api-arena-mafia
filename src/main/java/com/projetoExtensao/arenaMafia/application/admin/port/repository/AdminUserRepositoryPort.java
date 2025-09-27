package com.projetoExtensao.arenaMafia.application.admin.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface AdminUserRepositoryPort {

  /**
   * Busca usuários de forma paginada com base em uma especificação dinâmica.
   *
   * @param spec A especificação que define os critérios de filtro.
   * @param pageable As informações de paginação e ordenação.
   * @return Uma Página (Page) de objetos de domínio User.
   */
  Page<User> search(Specification<UserEntity> spec, Pageable pageable);
}
