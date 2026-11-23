package consertatudo;

import consertatudo.persistence.ClienteRepositorio;
import consertatudo.persistence.OrdemRepositorio;
import consertatudo.persistence.PagamentoRepositorio;
import consertatudo.persistence.TecnicoRepositorio;
import consertatudo.service.ClienteService;
import consertatudo.service.OrdemServicoService;
import consertatudo.service.PagamentoService;
import consertatudo.service.RelatorioService;
import consertatudo.service.TecnicoService;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Raiz de composição: cria os repositórios (carregando os CSVs) e injeta nos
 * serviços. Main e testes usam esta classe, o que mantém o acoplamento baixo.
 */
public class Sistema {

    private final Path pastaDados;
    private final ClienteRepositorio clientes;
    private final TecnicoRepositorio tecnicos;
    private final OrdemRepositorio ordens;
    private final PagamentoRepositorio pagamentos;

    private final ClienteService clienteService;
    private final TecnicoService tecnicoService;
    private final OrdemServicoService ordemService;
    private final PagamentoService pagamentoService;
    private final RelatorioService relatorioService;

    public Sistema(Path pastaDados, Clock relogio) {
        this.pastaDados = pastaDados;
        this.clientes = new ClienteRepositorio(pastaDados.resolve("clientes.csv"));
        this.tecnicos = new TecnicoRepositorio(pastaDados.resolve("tecnicos.csv"));
        this.ordens = new OrdemRepositorio(pastaDados.resolve("ordens.csv"));
        this.pagamentos = new PagamentoRepositorio(pastaDados.resolve("pagamentos.csv"));
        clientes.carregar();
        tecnicos.carregar();
        ordens.carregar();
        pagamentos.carregar();

        this.clienteService = new ClienteService(clientes, ordens);
        this.tecnicoService = new TecnicoService(tecnicos, ordens);
        this.ordemService = new OrdemServicoService(clientes, tecnicos, ordens, relogio);
        this.pagamentoService = new PagamentoService(clientes, ordens, pagamentos, relogio);
        this.relatorioService = new RelatorioService(clientes, tecnicos, ordens, pagamentos, relogio);
    }

    public Path getPastaDados() {
        return pastaDados;
    }

    public ClienteService clientes() {
        return clienteService;
    }

    public TecnicoService tecnicos() {
        return tecnicoService;
    }

    public OrdemServicoService ordens() {
        return ordemService;
    }

    public PagamentoService pagamentos() {
        return pagamentoService;
    }

    public RelatorioService relatorios() {
        return relatorioService;
    }
}
