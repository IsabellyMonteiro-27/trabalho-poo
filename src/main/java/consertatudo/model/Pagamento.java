package consertatudo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registro imutável de um pagamento de ordem de serviço. */
public class Pagamento {

    private final int id;
    private final int ordemId;
    private final int clienteId;
    private final BigDecimal valorServico;
    private final BigDecimal descontoFidelidade;
    private final BigDecimal descontoPontos;
    private final int pontosUsados;
    private final BigDecimal valorPago;
    private final int pontosGanhos;
    private final LocalDate data;
    private final FormaPagamento forma;

    public Pagamento(int id, int ordemId, int clienteId, BigDecimal valorServico,
                     BigDecimal descontoFidelidade, BigDecimal descontoPontos, int pontosUsados,
                     BigDecimal valorPago, int pontosGanhos, LocalDate data, FormaPagamento forma) {
        this.id = id;
        this.ordemId = ordemId;
        this.clienteId = clienteId;
        this.valorServico = valorServico;
        this.descontoFidelidade = descontoFidelidade;
        this.descontoPontos = descontoPontos;
        this.pontosUsados = pontosUsados;
        this.valorPago = valorPago;
        this.pontosGanhos = pontosGanhos;
        this.data = data;
        this.forma = forma;
    }

    public int getId() {
        return id;
    }

    public int getOrdemId() {
        return ordemId;
    }

    public int getClienteId() {
        return clienteId;
    }

    public BigDecimal getValorServico() {
        return valorServico;
    }

    public BigDecimal getDescontoFidelidade() {
        return descontoFidelidade;
    }

    public BigDecimal getDescontoPontos() {
        return descontoPontos;
    }

    public int getPontosUsados() {
        return pontosUsados;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public int getPontosGanhos() {
        return pontosGanhos;
    }

    public LocalDate getData() {
        return data;
    }

    public FormaPagamento getForma() {
        return forma;
    }

    public BigDecimal getTotalDescontos() {
        return descontoFidelidade.add(descontoPontos);
    }
}
