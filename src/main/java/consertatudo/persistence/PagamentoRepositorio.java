package consertatudo.persistence;

import consertatudo.model.FormaPagamento;
import consertatudo.model.Pagamento;
import consertatudo.util.Dinheiro;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class PagamentoRepositorio extends RepositorioCsv<Pagamento> {

    public PagamentoRepositorio(Path arquivo) {
        super(arquivo);
    }

    @Override
    protected String cabecalho() {
        return "id;ordemId;clienteId;valorServico;descontoFidelidade;descontoPontos;pontosUsados;"
                + "valorPago;pontosGanhos;data;forma";
    }

    @Override
    protected Pagamento deCampos(List<String> c) {
        return new Pagamento(Integer.parseInt(c.get(0)), Integer.parseInt(c.get(1)), Integer.parseInt(c.get(2)),
                Dinheiro.ler(c.get(3)), Dinheiro.ler(c.get(4)), Dinheiro.ler(c.get(5)),
                Integer.parseInt(c.get(6)), Dinheiro.ler(c.get(7)), Integer.parseInt(c.get(8)),
                LocalDate.parse(c.get(9)), FormaPagamento.valueOf(c.get(10)));
    }

    @Override
    protected List<String> paraCampos(Pagamento p) {
        return List.of(String.valueOf(p.getId()), String.valueOf(p.getOrdemId()),
                String.valueOf(p.getClienteId()), p.getValorServico().toPlainString(),
                p.getDescontoFidelidade().toPlainString(), p.getDescontoPontos().toPlainString(),
                String.valueOf(p.getPontosUsados()), p.getValorPago().toPlainString(),
                String.valueOf(p.getPontosGanhos()), p.getData().toString(), p.getForma().name());
    }

    @Override
    protected int idDe(Pagamento p) {
        return p.getId();
    }

    @Override
    protected String nomeEntidade() {
        return "Pagamento";
    }
}
