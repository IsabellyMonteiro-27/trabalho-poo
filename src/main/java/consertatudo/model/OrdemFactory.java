package consertatudo.model;

import consertatudo.exception.RegraNegocioException;

import java.time.LocalDate;

/** Fábrica que decide qual subclasse de {@link OrdemServico} instanciar. */
public final class OrdemFactory {

    private OrdemFactory() {
    }

    public static OrdemServico nova(boolean emergencia, int id, int clienteId, TipoServico tipo,
                                    String descricao, LocalDate prazo, LocalDate criadaEm,
                                    int descontoFidelidade) {
        return restaurar(emergencia ? "EMERGENCIA" : "NORMAL", id, clienteId, tipo, descricao, prazo,
                criadaEm, descontoFidelidade, StatusOrdem.PENDENTE, null, false);
    }

    public static OrdemServico restaurar(String categoria, int id, int clienteId, TipoServico tipo,
                                         String descricao, LocalDate prazo, LocalDate criadaEm,
                                         int descontoFidelidade, StatusOrdem status,
                                         Integer tecnicoId, boolean paga) {
        switch (categoria) {
            case "NORMAL":
                return new OrdemNormal(id, clienteId, tipo, descricao, prazo, criadaEm,
                        descontoFidelidade, status, tecnicoId, paga);
            case "EMERGENCIA":
                return new OrdemEmergencia(id, clienteId, tipo, descricao, prazo, criadaEm,
                        descontoFidelidade, status, tecnicoId, paga);
            default:
                throw new RegraNegocioException("Categoria de ordem desconhecida: " + categoria);
        }
    }
}
