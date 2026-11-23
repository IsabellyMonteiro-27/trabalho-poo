package consertatudo.model;

import consertatudo.exception.RegraNegocioException;
import consertatudo.util.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Ordem de serviço (classe abstrata). As subclasses {@link OrdemNormal} e
 * {@link OrdemEmergencia} definem a taxa adicional e a prioridade; o restante
 * do comportamento (cálculo de valores e transições de status) é comum.
 */
public abstract class OrdemServico {

    private final int id;
    private final int clienteId;
    private final TipoServico tipo;
    private final String descricao;
    private final LocalDate prazo;
    private final LocalDate criadaEm;
    /** Percentual de desconto de fidelidade "congelado" no momento da criação. */
    private final int descontoFidelidade;

    private StatusOrdem status;
    private Integer tecnicoId;
    private boolean paga;

    protected OrdemServico(int id, int clienteId, TipoServico tipo, String descricao, LocalDate prazo,
                           LocalDate criadaEm, int descontoFidelidade, StatusOrdem status,
                           Integer tecnicoId, boolean paga) {
        if (tipo == null || prazo == null || criadaEm == null || status == null) {
            throw new RegraNegocioException("Dados obrigatórios da ordem de serviço ausentes.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new RegraNegocioException("A descrição do problema é obrigatória.");
        }
        if (descontoFidelidade < 0 || descontoFidelidade > PoliticaFidelidade.DESCONTO_MAXIMO) {
            throw new RegraNegocioException("Percentual de desconto inválido.");
        }
        this.id = id;
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.descricao = descricao.trim();
        this.prazo = prazo;
        this.criadaEm = criadaEm;
        this.descontoFidelidade = descontoFidelidade;
        this.status = status;
        this.tecnicoId = tecnicoId;
        this.paga = paga;
    }

    // ---------- comportamento polimórfico ----------

    /** Taxa adicional sobre o valor base (0 para normal, 0,50 para emergência). */
    public abstract BigDecimal getTaxaAdicional();

    public abstract boolean isEmergencia();

    /** Texto gravado no CSV para recriar a subclasse certa. */
    public abstract String getCategoria();

    // ---------- valores ----------

    public BigDecimal getValorBruto() {
        return Dinheiro.arredondar(tipo.getValorBase().multiply(BigDecimal.ONE.add(getTaxaAdicional())));
    }

    public BigDecimal getValorTaxaAdicional() {
        return getValorBruto().subtract(tipo.getValorBase());
    }

    public BigDecimal getValorDescontoFidelidade() {
        return getValorBruto().multiply(BigDecimal.valueOf(descontoFidelidade))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getValorComDesconto() {
        return getValorBruto().subtract(getValorDescontoFidelidade());
    }

    // ---------- transições de status ----------

    public void atribuirTecnico(int novoTecnicoId) {
        if (status != StatusOrdem.PENDENTE) {
            throw new RegraNegocioException(
                    "Só é possível alocar técnico em ordens pendentes (status atual: " + status.getDescricao() + ").");
        }
        this.tecnicoId = novoTecnicoId;
    }

    public void liberarTecnico() {
        if (status != StatusOrdem.PENDENTE) {
            throw new RegraNegocioException("Só é possível trocar o técnico de ordens pendentes.");
        }
        this.tecnicoId = null;
    }

    public void iniciar() {
        if (status != StatusOrdem.PENDENTE) {
            throw new RegraNegocioException(
                    "Só ordens pendentes podem ser iniciadas (status atual: " + status.getDescricao() + ").");
        }
        if (tecnicoId == null) {
            throw new RegraNegocioException("A ordem ainda não tem técnico alocado.");
        }
        status = StatusOrdem.EM_ANDAMENTO;
    }

    public void concluir() {
        if (status != StatusOrdem.EM_ANDAMENTO) {
            throw new RegraNegocioException(
                    "Só ordens em andamento podem ser concluídas (status atual: " + status.getDescricao() + ").");
        }
        status = StatusOrdem.CONCLUIDA;
    }

    public void cancelar() {
        if (!status.isAtiva()) {
            throw new RegraNegocioException(
                    "Ordens " + status.getDescricao().toLowerCase() + "s não podem ser canceladas.");
        }
        status = StatusOrdem.CANCELADA;
    }

    public void marcarComoPaga() {
        if (status != StatusOrdem.CONCLUIDA) {
            throw new RegraNegocioException(
                    "Somente ordens concluídas podem ser pagas (status atual: " + status.getDescricao() + ").");
        }
        if (paga) {
            throw new RegraNegocioException("Esta ordem já foi paga.");
        }
        paga = true;
    }

    // ---------- getters ----------

    public int getId() {
        return id;
    }

    public int getClienteId() {
        return clienteId;
    }

    public TipoServico getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public LocalDate getCriadaEm() {
        return criadaEm;
    }

    public int getDescontoFidelidade() {
        return descontoFidelidade;
    }

    public StatusOrdem getStatus() {
        return status;
    }

    public Integer getTecnicoId() {
        return tecnicoId;
    }

    public boolean isPaga() {
        return paga;
    }
}
