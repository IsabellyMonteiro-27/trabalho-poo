package consertatudo.model;

import consertatudo.exception.RegraNegocioException;

/** Técnico especializado em um tipo de serviço. */
public class Tecnico extends Pessoa {

    private TipoServico especialidade;
    private boolean ativo;

    public Tecnico(int id, String nome, String telefone, TipoServico especialidade, boolean ativo) {
        super(id, nome, telefone);
        setEspecialidade(especialidade);
        this.ativo = ativo;
    }

    public TipoServico getEspecialidade() {
        return especialidade;
    }

    public final void setEspecialidade(TipoServico especialidade) {
        if (especialidade == null) {
            throw new RegraNegocioException("A especialidade do técnico é obrigatória.");
        }
        this.especialidade = especialidade;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /**
     * Elétrica e hidráulica exigem a especialidade correspondente;
     * reparos gerais podem ser feitos por qualquer técnico.
     */
    public boolean atende(TipoServico tipo) {
        return tipo == TipoServico.GERAL || especialidade == tipo;
    }

    @Override
    public String getPapel() {
        return "Técnico";
    }
}
