package consertatudo.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Leitura e escrita de linhas CSV com separador ponto e vírgula.
 * Campos com ';' ou aspas são protegidos entre aspas (aspas internas são duplicadas).
 */
public final class Csv {

    public static final char SEPARADOR = ';';

    private Csv() {
    }

    public static String juntar(List<String> campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.size(); i++) {
            if (i > 0) {
                sb.append(SEPARADOR);
            }
            sb.append(escapar(campos.get(i)));
        }
        return sb.toString();
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        String v = valor.replace("\r", " ").replace("\n", " ");
        if (v.indexOf(SEPARADOR) >= 0 || v.indexOf('"') >= 0) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    public static List<String> separar(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == SEPARADOR) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        if (entreAspas) {
            throw new IllegalArgumentException("aspas não fechadas");
        }
        campos.add(atual.toString());
        return campos;
    }
}
