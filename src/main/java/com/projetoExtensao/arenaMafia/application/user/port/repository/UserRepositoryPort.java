package com.projetoExtensao.arenaMafia.application.user.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepositoryPort {
  Page<User> search(Specification<UserEntity> spec, Pageable pageable);

  User save(User user);

  Optional<User> findByUsername(String username);

  User findSystemUserOrElseThrow();

  Optional<User> findById(UUID id);

  User findByIdOrElseThrow(UUID id);

  List<User> findAllByIds(Set<UUID> ids);

  Optional<User> findByPhone(String phone);

  boolean existsByUsername(String username);

  boolean existsByPhone(String phone);

  List<User> findByStatusAndCreatedAtBefore(AccountStatus status, Instant dateTime);

  List<User> findByStatusAndUpdateAtBefore(AccountStatus status, Instant dateTime);

  void deleteAll(List<User> users);

  void delete(User user);
}
