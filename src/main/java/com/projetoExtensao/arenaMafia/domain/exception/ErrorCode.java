package com.projetoExtensao.arenaMafia.domain.exception;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum ErrorCode {
  // Erros globais
  MALFORMED_JSON_REQUEST("Requisição JSON malformada."),
  UNEXPECTED_ERROR("Ocorreu um erro inesperado. Por favor, tente novamente mais tarde."),
  ACCESS_DENIED("Acesso negado. Você não tem permissão para acessar este recurso."),
  DATA_INTEGRITY_VIOLATION("O recurso que você está tentando criar ou atualizar já existe."),
  INVALID_REQUEST_PARAMETER("Um parâmetro da requisição é inválido ou está malformado."),
  VALIDATION_FAILED("A validação falhou. Verifique os detalhes dos campos para mais informações."),
  SESSION_EXPIRED("Sua sessão expirou. Por favor, faça login novamente."),
  INVALID_SORT_PARAMETER("O parâmetro de ordenação fornecido é inválido."),
  INVALID_FIELD_VALUE("O valor fornecido para um campo é inválido."),

  TOO_MANY_REQUESTS("Limite de requisições excedido. Por favor, tente novamente mais tarde."),
  TOO_MANY_LOGIN_ATTEMPTS(
      "Você realizou muitas tentativas de login. Por favor, aguarde um minuto antes de tentar novamente."),

  JWT_TOKEN_INVALID_OR_EXPIRED("Token JWT expirado ou inválido."),
  INVALID_CREDENTIALS("Credenciais inválidas. Por favor, verifique seu usuário e senha."),

  ACCOUNT_STATUS_REQUIRED("O status da conta é obrigatório."),
  INVALID_ACCOUNT_STATUS("O status da conta fornecido é inválido."),
  ACCOUNT_STATE_CONFLICT("O status atual da conta não permite essa operação."),
  ACCOUNT_PENDING_VERIFICATION("Você precisa ativar sua conta. Conclua o processo de cadastro."),
  ACCOUNT_LOCKED("Sua conta está bloqueada. Por favor, contate o suporte."),
  ACCOUNT_DISABLED(
      "Sua conta está desativada e será deletada em breve. Entre em contato com o suporte para reativá-la."),
  ACCOUNT_NOT_PENDING_VERIFICATION(
      "Esta operação só pode ser executada em contas com verificação pendente."),
  ACCOUNT_ALREADY_ACTIVE("A conta já está ativa."),
  ACCOUNT_ALREADY_LOCKED("A conta já está bloqueada."),
  ACCOUNT_ALREADY_DISABLED("A conta já está desativada."),

  USER_NOT_FOUND("Usuário não encontrado."),
  USERNAME_ALREADY_EXISTS("Esse nome de usuário já está em uso."),
  PHONE_ALREADY_EXISTS("Esse número de telefone já está em uso."),

  ROLE_REQUIRED("O papel (role) do usuário é obrigatório."),

  USERNAME_REQUIRED("O nome de usuário é obrigatório."),
  USERNAME_INVALID_FORMAT("O nome de usuário pode conter apenas letras, números e underscore (_)."),
  USERNAME_INVALID_LENGTH("O nome de usuário deve ter entre 3 e 50 caracteres."),

  PASSWORD_REQUIRED("A senha é obrigatória."),
  PASSWORD_INVALID_LENGTH("A senha deve ter entre 6 e 20 caracteres."),
  PASSWORD_NO_WHITESPACE("A senha não pode conter espaços em branco."),
  CONFIRM_PASSWORD_REQUIRED("A confirmação de senha é obrigatória."),
  PASSWORD_HASH_REQUIRED("O hash da senha é obrigatório."),
  PASSWORD_CURRENT_INCORRECT("A Senha atual está incorreta."),
  PASSWORDS_DO_NOT_MATCH("A senha de confirmação não corresponde à senha."),

  PHONE_REQUIRED("O número de telefone é obrigatório."),
  PHONE_INVALID_FORMAT(
      "O número de telefone não está em um formato válido. Use o padrão internacional E.164 (ex: +5511987654321)."),
  PHONE_INVALID("O número de telefone é inválido. Verifique o código do país e o número."),
  PHONE_CHANGE_NOT_INITIATED(
      "Sua solicitação de alteração de telefone não foi encontrada ou já expirou. Por favor, inicie o processo novamente."),

  FULL_NAME_REQUIRED("O nome completo é obrigatório."),
  FULL_NAME_INVALID_LENGTH("O nome completo deve ter entre 3 e 100 caracteres."),

  OTP_CODE_REQUIRED("O código de verificação é obrigatório."),
  OTP_CODE_INCORRECT_OR_EXPIRED("Código de verificação incorreto ou expirado."),
  OTP_CODE_INVALID_FORMAT("O código de verificação deve ser composto por 6 dígitos numéricos."),

  OTP_SESSION_ID_REQUIRED("O ID da sessão OTP é obrigatório."),
  OTP_SESSION_ID_INVALID_FORMAT("O formato do ID da sessão OTP é inválido."),
  OTP_SESSION_ID_INCORRECT_OR_EXPIRED("ID da sessão OTP incorreto ou expirado."),

  RESET_TOKEN_REQUIRED("O token de redefinição de senha é obrigatório."),
  RESET_TOKEN_INVALID_FORMAT("O formato do token de redefinição de senha é inválido."),
  RESET_TOKEN_INCORRECT_OR_EXPIRED("Token de redefinição de senha incorreto ou expirado."),

  REFRESH_TOKEN_REQUIRED("O token de atualização é obrigatório."),
  REFRESH_TOKEN_INVALID_FORMAT("O formato do token de atualização é inválido."),
  REFRESH_TOKEN_NOT_FOUND("Sua sessão expirou. Por favor, faça login novamente."),
  REFRESH_TOKEN_INCORRECT_OR_EXPIRED("Sua sessão expirou. Por favor, faça login novamente."),

  TERM_TOO_LONG("O termo de busca é muito longo. O máximo permitido é 100 caracteres."),
  START_DATE_AFTER_END_DATE("A data de início não pode ser posterior à data de término."),

  INVALID_ROLE("O papel (role) fornecido é inválido."),

  USER_ALREADY_ADMIN("O usuário já possui privilégios de administrador."),
  USER_ALREADY_USER("O usuário já possui privilégios padrão."),

  ADMIN_CANNOT_UPDATE_OWN_STATUS("Um administrador não pode alterar o próprio status."),
  ADMIN_CANNOT_UPDATE_OWN_ROLE("Um administrador não pode alterar o próprio papel (role)."),
  ADMIN_CANNOT_UPDATE_ROLE_OF_UNVERIFIED_USER(
      "Não é possivel alterar a permissão de um usuário não verificado."),
  ADMIN_CANNOT_UPDATE_STATUS_OF_UNVERIFIED_USER(
      "Um administrador não pode alterar o status de um usuário não verificado.");

  private static final Map<Class<?>, ErrorCode> ENUM_ERROR_MAP = new HashMap<>();

  static {
    ENUM_ERROR_MAP.put(AccountStatus.class, INVALID_ACCOUNT_STATUS);
    ENUM_ERROR_MAP.put(RoleEnum.class, INVALID_ROLE);
  }

  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  /**
   * Retorna o ErrorCode associado a um tipo de enum específico, se existir.
   *
   * @param enumType O tipo de enum para o qual se deseja obter o ErrorCode.
   * @return Um Optional contendo o ErrorCode correspondente, ou vazio se não houver associação.
   */
  public static Optional<ErrorCode> getForEnumType(Class<?> enumType) {
    return Optional.ofNullable(ENUM_ERROR_MAP.get(enumType));
  }
}
