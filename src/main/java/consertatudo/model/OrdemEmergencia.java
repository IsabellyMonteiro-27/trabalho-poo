package consertatudo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ordem emergencial: cobra taxa adicional de 50%, tem prioridade na alocação
 * e, enquanto estiver ativa, bloqueia novos agendamentos do técnico.
 */
public class OrdemEmergencia extends OrdemServico {

    public static final BigDecimal TAXA_EMERGENCIA = new BigDecimal("0.50");

    public OrdemEmergencia(int id, int clienteId, TipoServico tipo, String descricao, LocalDate prazo,
                           LocalDate criadaEm, int descontoFidelidade, StatusOrdem status,
                           Integer tecnicoId, boolean paga) {
        super(id, clienteId, tipo, descricao, prazo, criadaEm, descontoFidelidade, status, tecnicoId, paga);
    }

    @Override
    public BigDecimal getTaxaAdicional() {
        return TAXA_EMERGENCIA;
    }

    @Override
    public boolean isEmergencia() {
        return true;
    }

    @Override
    public String getCategoria() {
        return "EMERGENCIA";
    }
}
