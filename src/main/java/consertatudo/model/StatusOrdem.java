package consertatudo.model;

/** Ciclo de vida de uma ordem de serviço. */
public enum StatusOrdem {

    PENDENTE("Pendente"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusOrdem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Ordem ainda em aberto (ocupa a agenda do técnico). */
    public boolean isAtiva() {
        return this == PENDENTE || this == EM_ANDAMENTO;
    }
}
