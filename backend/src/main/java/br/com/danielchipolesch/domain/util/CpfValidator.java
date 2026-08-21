package br.com.danielchipolesch.domain.util;

// Algoritmo padrão de validação de CPF (dígitos verificadores mod 11), o
// mesmo publicado pela Receita Federal e replicado em qualquer referência
// sobre o assunto. Rejeita também sequências de um único dígito repetido
// (ex.: 111.111.111-11), que passam no cálculo mas nunca são CPFs válidos.
public final class CpfValidator {

    private CpfValidator() {}

    public static boolean isValid(String cpf) {
        if (cpf == null) return false;
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int[] n = new int[11];
        for (int i = 0; i < 11; i++) n[i] = digits.charAt(i) - '0';

        if (calcCheckDigit(n, 9, 10) != n[9]) return false;
        return calcCheckDigit(n, 10, 11) == n[10];
    }

    public static String onlyDigits(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    private static int calcCheckDigit(int[] digits, int length, int startWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) sum += digits[i] * (startWeight - i);
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }
}
