package consertatudo.exception;

/**
 * Erro causado por uma violação de regra de negócio ou por dado inválido
 * informado pelo usuário. A interface mostra a mensagem e segue o fluxo normal.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
