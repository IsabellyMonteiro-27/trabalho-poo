package consertatudo.exception;

/** Falha ao ler ou gravar os arquivos de dados. */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
