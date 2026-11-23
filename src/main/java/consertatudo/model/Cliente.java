package consertatudo.model;

import consertatudo.exception.RegraNegocioException;

/** Cliente da empresa; acumula pontos do programa de fidelidade. */
public class Cliente extends Pessoa {

    private int pontos;

    public Cliente(int id, String nome, String telefone) {
        this(id, nome, telefone, 0);
    }

    public Cliente(int id, String nome, String telefone, int pontos) {
        super(id, nome, telefone);
        if (pontos < 0) {
            throw new RegraNegocioException("Pontos não podem ser negativos.");
        }
        this.pontos = pontos;
    }

    public int getPontos() {
        return pontos;
    }

    public void adicionarPontos(int quantidade) {
        if (quantidade < 0) {
            throw new RegraNegocioException("Quantidade de pontos inválida.");
        }
        pontos += quantidade;
    }

    public void debitarPontos(int quantidade) {
        if (quantidade < 0 || quantidade > pontos) {
            throw new RegraNegocioException("Pontos insuficientes para o resgate.");
        }
        pontos -= quantidade;
    }

    @Override
    public String getPapel() {
        return "Cliente";
    }
}
