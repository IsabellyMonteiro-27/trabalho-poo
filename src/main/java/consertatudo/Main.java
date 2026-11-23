package consertatudo;

import consertatudo.exception.PersistenciaException;
import consertatudo.ui.ConsoleUI;
import consertatudo.ui.Entrada;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;

/**
 * Ponto de entrada. Uso: java -cp out consertatudo.Main [pasta-de-dados]
 * Sem argumento, os CSVs ficam na pasta "dados" do diretório atual.
 */
public class Main {

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        Path pasta = Paths.get(args.length > 0 ? args[0] : "dados");
        try {
            Sistema sistema = new Sistema(pasta, Clock.systemDefaultZone());
            new ConsoleUI(sistema, new Entrada(System.in)).executar();
        } catch (PersistenciaException e) {
            System.err.println("Não foi possível iniciar: " + e.getMessage());
            System.exit(1);
        }
    }
}
