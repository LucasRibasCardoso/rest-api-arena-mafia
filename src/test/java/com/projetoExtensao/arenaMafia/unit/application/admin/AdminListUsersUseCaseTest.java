package com.projetoExtensao.arenaMafia.unit.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.projetoExtensao.arenaMafia.application.admin.port.repository.AdminUserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.admin.usecase.users.imp.AdminListUsersUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDateRangeException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request.AdminUserSearchRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para AdminListUsersUseCase")
public class AdminListUsersUseCaseTest {

  @Mock private AdminUserRepositoryPort adminUserRepository;
  @InjectMocks private AdminListUsersUseCaseImp adminListUsersUseCase;

  @Captor private ArgumentCaptor<Specification<UserEntity>> specificationCaptor;
  @Captor private ArgumentCaptor<Pageable> pageableCaptor; // Novo Captor

  @Test
  @DisplayName("Deve listar usuários aplicando ordenação padrão por data de criação")
  void testListUsersWithCriteria_andApplyDefaultSort() {
    // Arrange
    var criteria = new AdminUserSearchRequestDto("john", null, null, null, null);

    Pageable originalPageable = PageRequest.of(0, 10);

    User user = TestDataProvider.UserBuilder.defaultUser().withFullName("John Doe").build();
    Page<User> expectedPage = new PageImpl<>(List.of(user), originalPageable, 1);

    when(adminUserRepository.search(any(Specification.class), any(Pageable.class)))
        .thenReturn(expectedPage);

    // Act
    Page<User> resultPage = adminListUsersUseCase.execute(criteria, originalPageable);

    // Assert
    assertThat(resultPage).isNotNull();
    assertThat(resultPage.getContent()).hasSize(1);
    assertThat(resultPage.getContent().getFirst().getFullName()).isEqualTo("John Doe");

    verify(adminUserRepository).search(specificationCaptor.capture(), pageableCaptor.capture());

    Pageable capturedPageable = pageableCaptor.getValue();
    assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
    assertThat(capturedPageable.getPageSize()).isEqualTo(10);
    assertThat(capturedPageable.getSort().getOrderFor("createdAt")).isNotNull();
    assertThat(Objects.requireNonNull(capturedPageable.getSort().getOrderFor("createdAt")).getDirection())
        .isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("Deve respeitar a ordenação fornecida pelo cliente se já existir")
  void execute_shouldKeepProvidedSort_whenPageableIsAlreadySorted() {
    // Arrange
    var criteria = new AdminUserSearchRequestDto(null, null, null, null, null);

    // Pageable COM ordenação (por Nome ASC)
    Pageable sortedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "fullName"));

    when(adminUserRepository.search(any(Specification.class), any(Pageable.class)))
        .thenReturn(Page.empty());

    // Act
    adminListUsersUseCase.execute(criteria, sortedPageable);

    // Assert
    verify(adminUserRepository).search(any(Specification.class), pageableCaptor.capture());

    Pageable capturedPageable = pageableCaptor.getValue();
    // Garante que NÃO mudou para createdAt
    assertThat(capturedPageable.getSort().getOrderFor("fullName")).isNotNull();
    assertThat(Objects.requireNonNull(capturedPageable.getSort().getOrderFor("fullName")).getDirection())
        .isEqualTo(Sort.Direction.ASC);
    assertThat(capturedPageable.getSort().getOrderFor("createdAt")).isNull();
  }

  @Test
  @DisplayName("Deve retornar uma página vazia quando nenhum usuário corresponder aos critérios")
  void execute_shouldReturnEmptyPage_whenNoUsersMatchCriteria() {
    // Arrange
    var criteria = new AdminUserSearchRequestDto("nonexistent", null, null, null, null);
    Pageable pageable = PageRequest.of(0, 10);
    Page<User> emptyPage = Page.empty(pageable);

    when(adminUserRepository.search(any(Specification.class), any(Pageable.class)))
        .thenReturn(emptyPage);

    // Act
    Page<User> resultPage = adminListUsersUseCase.execute(criteria, pageable);

    // Assert
    assertThat(resultPage).isNotNull();
    assertThat(resultPage.isEmpty()).isTrue();
    verify(adminUserRepository).search(any(Specification.class), any(Pageable.class));
  }

  @Test
  @DisplayName(
      "Deve lançar InvalidDateRangeException quando a data inicial for posterior à data final")
  void execute_shouldThrowException_whenStartDateIsAfterEndDate() {
    // Arrange
    LocalDate startDate = LocalDate.of(2025, 9, 30);
    LocalDate endDate = LocalDate.of(2025, 9, 29);
    var criteria = new AdminUserSearchRequestDto(null, startDate, endDate, null, null);
    Pageable pageable = PageRequest.of(0, 10);

    // Act & Assert
    assertThatThrownBy(() -> adminListUsersUseCase.execute(criteria, pageable))
        .satisfies(
            ex -> {
              InvalidDateRangeException exception = (InvalidDateRangeException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.START_DATE_AFTER_END_DATE);
            });
  }

  @Test
  @DisplayName(
      "Deve chamar o repositório com uma especificação irrestrita quando nenhum critério é fornecido")
  void execute_shouldUseUnrestrictedSpecification_whenNoCriteriaAreProvided() {
    // Arrange
    var criteria = new AdminUserSearchRequestDto(null, null, null, null, null);
    Pageable pageable = PageRequest.of(0, 10); // Unsorted

    when(adminUserRepository.search(any(Specification.class), any(Pageable.class)))
        .thenReturn(Page.empty(pageable));

    // Act
    adminListUsersUseCase.execute(criteria, pageable);

    // Assert
    verify(adminUserRepository).search(specificationCaptor.capture(), pageableCaptor.capture());

    // Valida Specification
    Specification<UserEntity> capturedSpec = specificationCaptor.getValue();
    assertThat(capturedSpec).isNotNull();

    // Valida Default Sort (efeito colateral esperado)
    assertThat(pageableCaptor.getValue().getSort().isSorted()).isTrue();
  }
}
