package consertatudo.model;

import java.math.BigDecimal;

/** Catálogo de serviços com o valor base definido no enunciado. */
public enum TipoServico {

    ELETRICA("Elétrica", "150.00"),
    HIDRAULICA("Hidráulica", "130.00"),
    GERAL("Reparos gerais", "100.00");

    private final String descricao;
    private final BigDecimal valorBase;

    TipoServico(String descricao, String valorBase) {
        this.descricao = descricao;
        this.valorBase = new BigDecimal(valorBase);
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }
}
