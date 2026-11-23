package consertatudo.ui;

import consertatudo.exception.RegraNegocioException;
import consertatudo.util.Datas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.function.Function;

/**
 * Leitura segura do teclado: todo método repete a pergunta até o usuário
 * informar um valor válido, então uma digitação errada nunca quebra o programa.
 */
public class Entrada {

    private final BufferedReader leitor;

    public Entrada(InputStream origem) {
        this.leitor = new BufferedReader(new InputStreamReader(origem, StandardCharsets.UTF_8));
    }

    /** Texto livre (pode ser vazio). */
    public String texto(String pergunta) {
        System.out.print(pergunta);
        try {
            String linha = leitor.readLine();
            if (linha == null) {
                throw new EntradaEncerradaException();
            }
            return linha.trim();
        } catch (IOException e) {
            throw new EntradaEncerradaException();
        }
    }

    public String textoObrigatorio(String pergunta) {
        while (true) {
            String valor = texto(pergunta);
            if (!valor.isEmpty()) {
                return valor;
            }
            System.out.println("[!] Este campo é obrigatório.");
        }
    }

    public int inteiroEntre(String pergunta, int minimo, int maximo) {
        while (true) {
            String valor = texto(pergunta);
            try {
                int numero = Integer.parseInt(valor);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
            } catch (NumberFormatException e) {
                // cai na mensagem abaixo
            }
            System.out.println("[!] Digite um número inteiro entre " + minimo + " e " + maximo + ".");
        }
    }

    public int id(String pergunta) {
        return inteiroEntre(pergunta, 1, Integer.MAX_VALUE);
    }

    public LocalDate data(String pergunta) {
        while (true) {
            try {
                return Datas.ler(texto(pergunta));
            } catch (RegraNegocioException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    public boolean confirmar(String pergunta) {
        while (true) {
            String valor = texto(pergunta).toLowerCase();
            if (valor.equals("s") || valor.equals("sim")) {
                return true;
            }
            if (valor.equals("n") || valor.equals("nao") || valor.equals("não")) {
                return false;
            }
            System.out.println("[!] Responda com 's' ou 'n'.");
        }
    }

    public <E> E escolher(String titulo, E[] opcoes, Function<E, String> rotulo) {
        System.out.println(titulo);
        for (int i = 0; i < opcoes.length; i++) {
            System.out.println("  " + (i + 1) + " - " + rotulo.apply(opcoes[i]));
        }
        int escolha = inteiroEntre("Escolha: ", 1, opcoes.length);
        return opcoes[escolha - 1];
    }
}
