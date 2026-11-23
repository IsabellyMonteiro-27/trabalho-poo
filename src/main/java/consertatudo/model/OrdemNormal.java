package consertatudo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ordem de serviço comum: sem taxa adicional e sem prioridade. */
public class OrdemNormal extends OrdemServico {

    public OrdemNormal(int id, int clienteId, TipoServico tipo, String descricao, LocalDate prazo,
                       LocalDate criadaEm, int descontoFidelidade, StatusOrdem status,
                       Integer tecnicoId, boolean paga) {
        super(id, clienteId, tipo, descricao, prazo, criadaEm, descontoFidelidade, status, tecnicoId, paga);
    }

    @Override
    public BigDecimal getTaxaAdicional() {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean isEmergencia() {
        return false;
    }

    @Override
    public String getCategoria() {
        return "NORMAL";
    }
}
