package consertatudo.util;

import consertatudo.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Conversão de datas no formato brasileiro (dd/MM/aaaa). */
public final class Datas {

    private static final DateTimeFormatter BR =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Datas() {
    }

    public static LocalDate ler(String texto) {
        try {
            return LocalDate.parse(texto.trim(), BR);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RegraNegocioException("Data inválida. Use o formato dd/MM/aaaa (ex.: 25/11/2026).");
        }
    }

    public static String formatar(LocalDate data) {
        return data == null ? "-" : data.format(BR);
    }
}
