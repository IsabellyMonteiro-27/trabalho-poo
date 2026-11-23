package consertatudo.service;

import consertatudo.exception.RegraNegocioException;
import consertatudo.model.Cliente;
import consertatudo.model.FormaPagamento;
import consertatudo.model.OrdemServico;
import consertatudo.model.Pagamento;
import consertatudo.model.PoliticaFidelidade;
import consertatudo.model.StatusOrdem;
import consertatudo.persistence.ClienteRepositorio;
import consertatudo.persistence.OrdemRepositorio;
import consertatudo.persistence.PagamentoRepositorio;
import consertatudo.util.Dinheiro;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** Cálculo do valor final, resgate de pontos e registro dos pagamentos. */
public class PagamentoService {

    /** Simulação do pagamento (o que será cobrado), sem gravar nada. */
    public static class Cotacao {
        public final BigDecimal valorServico;
        public final int percentualFidelidade;
        public final BigDecimal descontoFidelidade;
        public final int pontosUsados;
        public final BigDecimal descontoPontos;
        public final BigDecimal valorFinal;
        public final int pontosGanhos;

        Cotacao(BigDecimal valorServico, int percentualFidelidade, BigDecimal descontoFidelidade,
                int pontosUsados, BigDecimal descontoPontos, BigDecimal valorFinal, int pontosGanhos) {
            this.valorServico = valorServico;
            this.percentualFidelidade = percentualFidelidade;
            this.descontoFidelidade = descontoFidelidade;
            this.pontosUsados = pontosUsados;
            this.descontoPontos = descontoPontos;
            this.valorFinal = valorFinal;
            this.pontosGanhos = pontosGanhos;
        }
    }

    private final ClienteRepositorio clientes;
    private final OrdemRepositorio ordens;
    private final PagamentoRepositorio pagamentos;
    private final Clock relogio;

    public PagamentoService(ClienteRepositorio clientes, OrdemRepositorio ordens,
                            PagamentoRepositorio pagamentos, Clock relogio) {
        this.clientes = clientes;
        this.ordens = ordens;
        this.pagamentos = pagamentos;
        this.relogio = relogio;
    }

    public Cotacao cotar(int ordemId, boolean usarPontos) {
        OrdemServico ordem = ordens.exigirPorId(ordemId);
        if (ordem.getStatus() != StatusOrdem.CONCLUIDA) {
            throw new RegraNegocioException("Somente ordens concluídas podem ser pagas (status atual: "
                    + ordem.getStatus().getDescricao() + ").");
        }
        if (ordem.isPaga()) {
            throw new RegraNegocioException("Esta ordem já foi paga.");
        }
        Cliente cliente = clientes.exigirPorId(ordem.getClienteId());

        BigDecimal bruto = ordem.getValorBruto();
        BigDecimal descontoFidelidade = ordem.getValorDescontoFidelidade();
        BigDecimal subtotal = bruto.subtract(descontoFidelidade);

        int pontosUsados = 0;
        BigDecimal descontoPontos = Dinheiro.ZERO;
        if (usarPontos) {
            int blocosDisponiveis = cliente.getPontos() / PoliticaFidelidade.PONTOS_PARA_RESGATE;
            int blocosQueCabem = subtotal.divideToIntegralValue(PoliticaFidelidade.DESCONTO_RESGATE).intValue();
            int blocos = Math.min(blocosDisponiveis, blocosQueCabem);
            pontosUsados = blocos * PoliticaFidelidade.PONTOS_PARA_RESGATE;
            descontoPontos = Dinheiro.arredondar(PoliticaFidelidade.DESCONTO_RESGATE.multiply(BigDecimal.valueOf(blocos)));
        }
        BigDecimal valorFinal = subtotal.subtract(descontoPontos);
        int pontosGanhos = PoliticaFidelidade.pontosGanhos(valorFinal);
        return new Cotacao(bruto, ordem.getDescontoFidelidade(), descontoFidelidade, pontosUsados,
                descontoPontos, valorFinal, pontosGanhos);
    }

    public Pagamento pagar(int ordemId, FormaPagamento forma, boolean usarPontos) {
        if (forma == null) {
            throw new RegraNegocioException("A forma de pagamento é obrigatória.");
        }
        Cotacao cotacao = cotar(ordemId, usarPontos); // valida status e duplicidade
        OrdemServico ordem = ordens.exigirPorId(ordemId);
        Cliente cliente = clientes.exigirPorId(ordem.getClienteId());

        Pagamento pagamento = new Pagamento(pagamentos.proximoId(), ordemId, cliente.getId(),
                cotacao.valorServico, cotacao.descontoFidelidade, cotacao.descontoPontos,
                cotacao.pontosUsados, cotacao.valorFinal, cotacao.pontosGanhos,
                LocalDate.now(relogio), forma);

        ordem.marcarComoPaga();
        cliente.debitarPontos(cotacao.pontosUsados);
        cliente.adicionarPontos(cotacao.pontosGanhos);
        pagamentos.adicionar(pagamento);

        pagamentos.salvar();
        ordens.salvar();
        clientes.salvar();
        return pagamento;
    }

    public List<Pagamento> historicoDoCliente(int clienteId) {
        clientes.exigirPorId(clienteId);
        return pagamentos.listar().stream()
                .filter(p -> p.getClienteId() == clienteId)
                .collect(Collectors.toList());
    }

    public List<Pagamento> listar() {
        return pagamentos.listar();
    }
}
