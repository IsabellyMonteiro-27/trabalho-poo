package consertatudo.ui;

/** Fim da entrada de dados (Ctrl+D / Ctrl+Z): o programa encerra de forma limpa. */
public class EntradaEncerradaException extends RuntimeException {

    public EntradaEncerradaException() {
        super("Entrada encerrada");
    }
}
