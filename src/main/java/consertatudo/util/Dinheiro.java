package consertatudo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Utilitários para valores monetários (sempre BigDecimal com 2 casas). */
public final class Dinheiro {

    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal ler(String texto) {
        return arredondar(new BigDecimal(texto.trim()));
    }

    public static String formatar(BigDecimal valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }
}
