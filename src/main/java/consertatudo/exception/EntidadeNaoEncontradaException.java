package consertatudo.exception;

/** Lançada quando um cadastro procurado pelo ID não existe. */
public class EntidadeNaoEncontradaException extends RegraNegocioException {

    public EntidadeNaoEncontradaException(String entidade, int id) {
        super(entidade + " com ID " + id + " não foi encontrado(a).");
    }
}
