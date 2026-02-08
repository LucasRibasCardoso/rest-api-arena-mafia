package com.projetoExtensao.arenaMafia.domain.exception;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumeração centralizada de códigos de erro da aplicação.
 *
 * <p>Os erros estão organizados por contexto/domínio para facilitar a manutenção. Ao adicionar
 * novos erros, insira-os na seção apropriada.
 */
public enum ErrorCode {

  // ==================== ERROS GLOBAIS/INFRAESTRUTURA ====================
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
  RESOURCE_NOT_FOUND("Recurso não encontrado."),

  // ==================== ERROS DE AUTENTICAÇÃO E AUTORIZAÇÃO ====================
  INVALID_CREDENTIALS("Credenciais inválidas. Por favor, verifique seu usuário e senha."),
  JWT_TOKEN_INVALID_OR_EXPIRED("Token JWT expirado ou inválido."),
  TOO_MANY_LOGIN_ATTEMPTS(
      "Você realizou muitas tentativas de login. Por favor, aguarde um minuto antes de tentar novamente."),

  // ==================== ERROS DE USUÁRIO (USER) ====================
  // User - Geral
  USER_NOT_FOUND("Usuário não encontrado."),
  SYSTEM_USER_NOT_FOUND("Usuário do sistema não foi encontrado"),

  // User - Username
  USERNAME_REQUIRED("O nome de usuário é obrigatório."),
  USERNAME_INVALID_FORMAT("O nome de usuário pode conter apenas letras, números e underscore (_)."),
  USERNAME_INVALID_LENGTH("O nome de usuário deve ter entre 3 e 50 caracteres."),
  USERNAME_ALREADY_EXISTS("Esse nome de usuário já está em uso."),

  // User - Password
  PASSWORD_REQUIRED("A senha é obrigatória."),
  PASSWORD_INVALID_LENGTH("A senha deve ter entre 6 e 20 caracteres."),
  PASSWORD_NO_WHITESPACE("A senha não pode conter espaços em branco."),
  PASSWORD_HASH_REQUIRED("O hash da senha é obrigatório."),
  PASSWORD_CURRENT_INCORRECT("A senha atual está incorreta."),
  CONFIRM_PASSWORD_REQUIRED("A confirmação de senha é obrigatória."),
  PASSWORDS_DO_NOT_MATCH("A senha de confirmação não corresponde à senha."),

  // User - Phone
  PHONE_REQUIRED("O número de telefone é obrigatório."),
  PHONE_INVALID_FORMAT(
      "O número de telefone não está em um formato válido. Use o padrão internacional E.164 (ex: +5511987654321)."),
  PHONE_INVALID("O número de telefone é inválido. Verifique o código do país e o número."),
  PHONE_ALREADY_EXISTS("Esse número de telefone já está em uso."),
  PHONE_CHANGE_NOT_INITIATED(
      "Sua solicitação de alteração de telefone não foi encontrada ou já expirou. Por favor, inicie o processo novamente."),

  // User - Full Name
  FULL_NAME_REQUIRED("O nome completo é obrigatório."),
  FULL_NAME_INVALID_LENGTH("O nome completo deve ter entre 3 e 100 caracteres."),

  // User - Account Status
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

  // User - Role
  ROLE_REQUIRED("O papel (role) do usuário é obrigatório."),
  INVALID_ROLE("O papel (role) fornecido é inválido."),
  USER_ALREADY_ADMIN("O usuário já possui privilégios de administrador."),
  USER_ALREADY_USER("O usuário já possui privilégios padrão."),

  // User - Admin Operations
  ADMIN_CANNOT_UPDATE_OWN_STATUS("Um administrador não pode alterar o próprio status."),
  ADMIN_CANNOT_UPDATE_OWN_ROLE("Um administrador não pode alterar o próprio papel (role)."),
  ADMIN_CANNOT_UPDATE_ROLE_OF_UNVERIFIED_USER(
      "Não é possível alterar a permissão de um usuário não verificado."),
  ADMIN_CANNOT_UPDATE_STATUS_OF_UNVERIFIED_USER(
      "Um administrador não pode alterar o status de um usuário não verificado."),

  // ==================== ERROS DE TOKENS ====================
  // OTP (One-Time Password)
  OTP_CODE_REQUIRED("O código de verificação é obrigatório."),
  OTP_CODE_INCORRECT_OR_EXPIRED("Código de verificação incorreto ou expirado."),
  OTP_CODE_INVALID_FORMAT("O código de verificação deve ser composto por 6 dígitos numéricos."),
  OTP_SESSION_ID_REQUIRED("O ID da sessão OTP é obrigatório."),
  OTP_SESSION_ID_INVALID_FORMAT("O formato do ID da sessão OTP é inválido."),
  OTP_SESSION_ID_INCORRECT_OR_EXPIRED("ID da sessão OTP incorreto ou expirado."),

  // Password Reset Token
  RESET_TOKEN_REQUIRED("O token de redefinição de senha é obrigatório."),
  RESET_TOKEN_INVALID_FORMAT("O formato do token de redefinição de senha é inválido."),
  RESET_TOKEN_INCORRECT_OR_EXPIRED("Token de redefinição de senha incorreto ou expirado."),

  // Refresh Token
  REFRESH_TOKEN_REQUIRED("O token de atualização é obrigatório."),
  REFRESH_TOKEN_INVALID_FORMAT("O formato do token de atualização é inválido."),
  REFRESH_TOKEN_NOT_FOUND("Sua sessão expirou. Por favor, faça login novamente."),
  REFRESH_TOKEN_INCORRECT_OR_EXPIRED("Sua sessão expirou. Por favor, faça login novamente."),

  // ==================== ERROS DE MODALIDADE (MODALITY) ====================
  MODALITY_NOT_FOUND("Modalidade não encontrada."),
  MODALITY_ALREADY_DISABLE("Essa modalidade já está desativada."),
  MODALITY_ALREADY_ENABLE("Essa modalidade já está ativada."),
  MODALITY_NAME_REQUIRED("O nome da modalidade é obrigatório."),
  MODALITY_NAME_INVALID_LENGTH("O nome da modalidade deve ter entre 3 e 100 caracteres."),
  MODALITY_ALREADY_EXISTS("Essa modalidade já está cadastrada."),
  MODALITY_IN_USE(
      "Não é possível deletar esta modalidade pois ela está sendo utilizada por uma ou mais quadras."),

  // ==================== ERROS DE QUADRA (COURT) ====================
  // Court - Geral
  COURT_NOT_FOUND_BY_MODALITY("Nenhuma quadra encontrada para a modalidade especificada."),
  COURT_NOT_SUPPORTS_MODALITY("A quadra selecionada não suporta a modalidade solicitada."),
  COURT_NOT_FOUND("Quadra não encontrada."),
  COURT_ALREADY_EXISTS("Essa quadra já está cadastrada."),

  // Court - Name
  COURT_NAME_REQUIRED("O nome da quadra é obrigatório."),
  COURT_NAME_INVALID_LENGTH("O nome da quadra deve ter entre 3 e 100 caracteres."),
  COURT_MODALITY_REQUIRED("Ao menos uma modalidade é necessária ser informada."),

  // Court - Offset Minutes
  OFFSET_MINUTES_REQUIRED("O offset de minutos é obrigatório."),
  OFFSET_MINUTES_INVALID(
      "O valor de minutos de offset fornecido é inválido. Valores válidos: 0 ou 30."),

  // Court - Status
  COURT_ALREADY_DISABLED("A quadra já está desativada."),
  COURT_ALREADY_ENABLED("A quadra já está ativada."),

  // Court - Desativação
  COURT_DISABLE_DESCRIPTION_REQUIRED("A descrição para desativação da quadra é obrigatória"),
  COURT_DISABLE_DESCRIPTION_INVALID_LENGTH(
      "A descrição para desativação da quadra deve ter entre 3 e 500 caracteres."),
  // ==================== ERROS DE DIA DA SEMANA ====================
  DAY_OF_WEEK_REQUIRED("O dia da semana é obrigatório."),
  DAY_OF_WEEK_INVALID(
      "O dia da semana fornecido é inválido. Valores válidos: 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'."),

  // =================== ERROS DE TIME INTERVAL ====================
  TIME_INTERVAL_REQUIRED("O intervalo de tempo é obrigatório."),
  TIME_INTERVAL_OPEN_AFTER_CLOSE(
      "O horário de abertura deve ser anterior ao horário de fechamento."),
  TIME_INTERVAL_INVALID_MINUTES(
      "Os minutos do horário de abertura ou fechamento são inválidos. Valores válidos: 0 ou 30."),
  TIME_INTERVAL_OVERLAP("Este intervalo de horário sobrepõe-se a um intervalo existente."),
  TIME_INTERVAL_SAME_TIME("O horário de abertura não pode ser igual ao horário de fechamento."),
  TIME_INTERVAL_EXCEEDS_24_HOURS("O intervalo de tempo não pode exceder 24 horas."),

  // ==================== ERROS DE HORÁRIO DE FUNCIONAMENTO (OPERATING HOURS) ====================
  OPERATING_HOURS_ALREADY_EXISTS(
      "Já existe um horário de funcionamento ativo para este dia da semana."),
  OPERATING_HOURS_ALREADY_DISABLED("O horário de funcionamento já está desativado."),
  OPERATING_HOURS_CANNOT_BE_DISABLED_DUE_TO_RESERVATIONS(
      "Não é possível desativar o horário de funcionamento pois existem reservas futuras associadas a ele."),
  OPERATING_HOURS_ALREADY_ENABLED("O horário de funcionamento já está ativado."),
  OPERATING_HOURS_NOT_FOUND("Horário de funcionamento não encontrado."),
  OPERATING_HOURS_APPLICABLE_NOT_FOUND(
      "Não há horários de funcionamento aplicáveis para todos os dias selecionados."),

  // Operating Hours - Desativação
  OPERATING_HOURS_DISABLE_DESCRIPTION_REQUIRED(
      "A descrição para desativação do horário de funcionamento é obrigatória."),
  OPERATING_HOURS_DISABLE_DESCRIPTION_INVALID_LENGTH(
      "A descrição para desativação do horário de funcionamento deve ter entre 3 e 500 caracteres."),

  // ==================== ERROS DE REGRAS DE PREÇO (PRICE RULES) ====================
  PRICE_RULE_NAME_REQUIRED("O nome da regra de preço é obrigatório."),
  PRICE_RULE_NAME_INVALID_LENGTH("O nome da regra de preço deve ter entre 1 e 100 caracteres."),
  PRICE_RULE_PRICE_INVALID("O preço deve ser um valor positivo."),
  PRICE_RULE_PRICE_REQUIRED("O preço da regra de preço é obrigatório."),
  PRICE_RULE_PRIORITY_REQUIRED("A prioridade da regra de preço é obrigatória."),
  PRICE_RULE_PRIORITY_INVALID("O valor de prioridade deve ser um valor maior que 0."),
  PRICE_RULE_ALREADY_DISABLED("A regra de preço já está desativada."),
  PRICE_RULE_CANNOT_DISABLE_DEFAULT("A regra de preço padrão não pode ser desativada."),
  PRICE_RULE_ALREADY_ENABLED("A regra de preço já está ativada."),
  PRICE_RULE_DEFAULT_NOT_FOUND("A regra de preço padrão não foi encontrada."),
  PRICE_RULE_NOT_FOUND("Regra de preço não encontrada."),
  PRICE_RULE_ALREADY_EXISTS("Essa regra de preço já está cadastrada."),
  PRICE_RULE_PRIORITY_OVERLAP("Já existe uma regra de preço com essa prioridade."),

  // ==================== ERROS DE BUSCA E VALIDAÇÃO ====================,
  START_DATE_AFTER_END_DATE("A data de início não pode ser posterior à data de término."),
  TERM_TOO_LONG("O termo de busca é muito longo. O máximo permitido é 100 caracteres."),

  // ==================== ERROS DE DATE TIME SLOT ====================
  DATE_TIME_SLOT_TIME_INTERVAL_REQUIRED("O intervalo de tempo é obrigatório."),
  DATE_TIME_SLOT_DATE_REQUIRED("A data é obrigatória."),

  // ==================== ERROS DE ENTRADAS DE AGENDAMENTO (SCHEDULE ENTRY) ====================
  SCHEDULE_ENTRY_ID_REQUIRED("O ID da entrada de agendamento é obrigatório."),
  SCHEDULE_ENTRY_COURT_ID_REQUIRED("Uma reserva deve estar associada a uma quadra."),
  SCHEDULE_ENTRY_DATE_TIME_SLOT_REQUIRED("O slot de data/hora é obrigatório."),
  SCHEDULE_ENTRY_CREATED_AT_REQUIRED("A data de criação é obrigatória."),
  SCHEDULE_ENTRY_NOT_FOUND("Agendamento não encontrado."),
  SCHEDULE_ENTRY_NOT_AVAILABLE("O horário selecionado não está mais disponível. Por favor, escolha outro horário."),
  SCHEDULE_ENTRY_TOO_MANY_OCCURRENCES("O limite máximo de 1000 agendamentos recorrentes foi atingido, reduza o intervalo de datas."),
  SCHEDULE_ENTRY_TIME_INTERVAL_OUTSIDE_OPERATING_HOURS("O intervalo de tempo informado está fora do horário de funcionamento de um ou mais dias selecionados. Verifique se o horário é válido para todos os dias."),
  SCHEDULE_ENTRY_SELECTED_DAYS_OUTSIDE_DATE_RANGE("Um ou mais dias da semana selecionados estão fora do intervalo de datas informado."),


  // =================== ERROS DE CONSULTA DE HORÁRIOS DISPONÍVEIS ====================
  PAST_DATE_NOT_ALLOWED("Não é possível consultar horários disponíveis para datas passadas."),

  // ==================== ERROS DE RESERVA (RESERVATION) ====================
  RESERVATION_PRICE_INVALID("O preço da reserva deve ser um valor positivo."),
  RESERVATION_MODALITY_ID_REQUIRED("É obrigatório informar a modalidade da reserva."),
  RESERVATION_DATE_REQUIRED("É obrigatório informar a data da reserva."),
  RESERVATION_START_DATE_REQUIRED("É obrigatório informar a data inicial da reserva."),
  RESERVATION_END_DATE_REQUIRED("É obrigatório informar a data final da reserva."),
  RESERVATION_TIME_INTERVAL_REQUIRED("É obrigatório informar o horário da reserva."),
  RESERVATION_COURT_ID_REQUIRED("A quadra da reserva é obrigatória."),
  RESERVATION_USER_ID_REQUIRED("O usuário da reserva é obrigatório."),
  RESERVATION_ADMIN_USER_ID_REQUIRED("O ID do administrador é obrigatório para o cancelamento."),
  RESERVATION_ACCESS_DENIED(
      "Você não tem permissão para acessar esta reserva. Apenas o dono da reserva ou um administrador podem visualizá-la."),
  RESERVATION_PAST_DATE_NOT_ALLOWED(
      "Não é possível fazer reservas para datas passadas. A data deve ser hoje ou no futuro."),
  RESERVATION_ALREADY_CANCELLED("Esta reserva já foi cancelada."),
  RESERVATION_ALREADY_COMPLETED("Esta reserva já foi concluída."),
  RESERVATION_NOT_POSSIBLE_TO_CANCEL(
      "Não é possível cancelar a reserva com menos de 1:30 horas de antecedência."),
  RESERVATION_CANCELLATION_IN_BATCH_FAILED(
      "Falha ao cancelar reservas em lote. O processo foi revertido e nenhuma reserva foi cancelada."),
  RESERVATION_NEW_OWNER_ID_REQUIRED("O ID do novo dono da reserva é obrigatório."),

  // ==================== ERROS DE CONFLITO DE AGENDAMENTO ====================
  UNSUPPORTED_SCHEDULE_ENTRY_TYPE("O tipo de entrada de agendamento fornecido não é suportado."),

  // ==================== ERROS DE BLOQUEIO DE HORÁRIO (BLOCKED TIME) ====================
  // BlockedTime - Geral
  BLOCKED_TIME_NOT_FOUND("Bloqueio de horário não encontrado."),
  BLOCKED_TIME_PAST_DATE_NOT_ALLOWED("Não é possível criar bloqueios para datas no passada."),
  BLOCKED_TIME_START_DATE_IN_PAST("A data inicial do bloqueio não pode estar no passado."),
  BLOCKED_TIME_START_DATE_AFTER_END_DATE("A data inicial não pode ser posterior à data final."),
  BLOCKED_TIME_END_DATE_IN_PAST("A data final do bloqueio não pode estar no passado."),
  SELECTED_DAYS_NOT_ALLOWED_FOR_SINGLE_DATE("Para agendamentos de um único dia (startDate = endDate), não é necessário informar dias da semana específicos."),

  // BlockedTime - Validação de Input
  BLOCKED_TIME_COURT_IDS_REQUIRED("É obrigatório informar ao menos uma quadra para o bloqueio."),
  BLOCKED_TIME_COURT_IDS_SIZE_INVALID("É necessário informar entre 1 e 20 quadras."),
  BLOCKED_TIME_START_DATE_REQUIRED("A data inicial do bloqueio é obrigatória."),
  BLOCKED_TIME_END_DATE_REQUIRED("A data final do bloqueio é obrigatória."),
  BLOCKED_TIME_IS_FULL_DAY_REQUIRED("É obrigatório informar se o bloqueio é para o dia todo."),
  BLOCKED_TIME_TIME_INTERVAL_REQUIRED_WHEN_NOT_FULL_DAY("O intervalo de tempo é obrigatório quando o bloqueio não é para o dia todo."),
  BLOCKED_TIME_TIME_INTERVAL_NOT_ALLOWED_WHEN_FULL_DAY("O intervalo de tempo não deve ser informado quando o bloqueio é para o dia todo. O horário será calculado automaticamente baseado no funcionamento da quadra."),

  // BlockedTime - Description
  BLOCKED_TIME_DESCRIPTION_REQUIRED("A descrição do bloqueio é obrigatória."),
  BLOCKED_TIME_UPDATE_RECURRING_FLAG_REQUIRED("O parâmetro `updateAllRecurring` é obrigatório."),
  BLOCKED_TIME_DESCRIPTION_INVALID_LENGTH("A descrição do bloqueio deve ter entre 3 e 500 caracteres."),

  // BlockedTime - Admin
  BLOCKED_TIME_ADMIN_ID_REQUIRED("O ID do administrador que está bloqueando o horário é obrigatório."),

  // BlockedTime - Preview e Cache
  BLOCKED_TIME_DAYS_OF_WEEK_SIZE_INVALID("É necessário informar entre 1 e 7 dias da semana para bloqueios recorrentes."),

  // ==================== ERROS DE CACHE PARA PREVIEW ====================
  PREVIEW_KEY_REQUIRED("A chave do preview é obrigatória."),
  PREVIEW_KEY_INVALID("A chave do preview é inválida."),
  PREVIEW_NOT_FOUND("Preview não encontrado. Pode ter expirado após 5 minutos."),
  PREVIEW_DATA_STALE("Os dados associados a este preview foram alterados desde a sua geração. Por favor, gere um novo preview antes de confirmar a desativação."),
  PREVIEW_KEY_OWNERSHIP_INVALID("Esta chave de preview não pertence a você. Não é possível usar um preview gerado por outro usuário."),

  // ==================== ERROS DE AGENDA (AGENDA ITEM) ====================
  AVAILABLE_MODALITY_IDS_REQUIRED("Erro interno: Não foi possível gerar a agenda. Nenhuma modalidade disponível encontrada para o horário agrupado."),
  ;

  // ==================== CONFIGURAÇÃO ====================
  private static final Map<Class<?>, ErrorCode> ENUM_ERROR_MAP = new HashMap<>();

  static {
    ENUM_ERROR_MAP.put(AccountStatus.class, INVALID_ACCOUNT_STATUS);
    ENUM_ERROR_MAP.put(RoleEnum.class, INVALID_ROLE);
  }

  private final String message;

  ErrorCode(String message) {
    this.message = message;
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

  public String getMessage() {
    return message;
  }
}
