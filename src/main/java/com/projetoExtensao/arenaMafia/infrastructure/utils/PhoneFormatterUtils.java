package com.projetoExtensao.arenaMafia.infrastructure.utils;

public class PhoneFormatterUtils {

  public static String maskPhoneNumber(String phone) {
    if (phone == null || phone.length() <= 4) {
      return "****";
    }
    return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
  }
}
