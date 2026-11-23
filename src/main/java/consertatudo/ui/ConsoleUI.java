package consertatudo.ui;

import consertatudo.DadosDemonstracao;
import consertatudo.Sistema;
import consertatudo.exception.PersistenciaException;
import consertatudo.exception.RegraNegocioException;
import consertatudo.model.Cliente;
import consertatudo.model.FormaPagamento;
import consertatudo.model.OrdemServico;
import consertatudo.model.Pagamento;
import consertatudo.model.PoliticaFidelidade;
import consertatudo.model.StatusOrdem;
import consertatudo.model.Tecnico;
import consertatudo.model.TipoServico;
import consertatudo.service.PagamentoService.Cotacao;
import consertatudo.util.Datas;
import consertatudo.util.Dinheiro;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Interface de console: menus, leitura de dados e exibição. Nenhuma regra de negócio fica aqui. */
public class ConsoleUI {

    private final Sistema sistema;
    private final Entrada in;
    private final PrintStream out = System.out;

    public ConsoleUI(Sistema sistema, Entrada entrada) {
        this.sistema = sistema;
        this.in = entrada;
    }

    public void executar() {
        out.println("==============================================");
        out.println("        CONSERTA TUDO - Gestão de Serviços");
        out.println("==============================================");
        try {
            while (true) {
                int opcao = menu("MENU PRINCIPAL", "Sair", "Clientes", "Técnicos", "Catálogo de serviços",
                        "Ordens de serviço", "Pagamentos e fidelidade", "Relatórios",
                        "Carregar dados de demonstração");
                if (opcao == 0) {
                    break;
                }
                switch (opcao) {
                    case 1: menuClientes(); break;
                    case 2: menuTecnicos(); break;
                    case 3: mostrarCatalogo(); break;
                    case 4: menuOrdens(); break;
                    case 5: menuPagamentos(); break;
                    case 6: menuRelatorios(); break;
                    case 7: seguro(this::carregarDemonstracao); break;
                    default: break;
                }
            }
        } catch (EntradaEncerradaException e) {
            out.println();
        }
        out.println("Até logo! Os dados foram salvos em: " + sistema.getPastaDados().toAbsolutePath());
    }

    // =====================================================================
    // Infraestrutura de menus
    // =====================================================================

    private int menu(String titulo, String textoSaida, String... opcoes) {
        out.println();
        out.println("--- " + titulo + " ---");
        for (int i = 0; i < opcoes.length; i++) {
            out.println((i + 1) + " - " + opcoes[i]);
        }
        out.println("0 - " + textoSaida);
        return in.inteiroEntre("Escolha: ", 0, opcoes.length);
    }

    /** Executa uma ação mostrando qualquer erro em vez de derrubar o programa. */
    private void seguro(Runnable acao) {
        try {
            acao.run();
        } catch (EntradaEncerradaException e) {
            throw e;
        } catch (RegraNegocioException e) {
            out.println("[!] " + e.getMessage());
        } catch (PersistenciaException e) {
            out.println("[!] Erro de arquivo: " + e.getMessage());
        } catch (RuntimeException e) {
            out.println("[!] Erro inesperado: " + e.getMessage());
        }
    }

    // =====================================================================
    // Clientes
    // =====================================================================

    private void menuClientes() {
        while (true) {
            int op = menu("CLIENTES", "Voltar", "Cadastrar", "Listar", "Editar", "Remover");
            if (op == 0) {
                return;
            }
            switch (op) {
                case 1: seguro(this::cadastrarCliente); break;
                case 2: listarClientes(); break;
                case 3: seguro(this::editarCliente); break;
                case 4: seguro(this::removerCliente); break;
                default: break;
            }
        }
    }

    private void cadastrarCliente() {
        String nome = in.textoObrigatorio("Nome: ");
        String telefone = in.texto("Telefone (opcional): ");
        Cliente cliente = sistema.clientes().cadastrar(nome, telefone);
        out.println("[OK] Cliente cadastrado com ID " + cliente.getId() + ".");
    }

    private boolean listarClientes() {
        List<Cliente> lista = sistema.clientes().listar();
        if (lista.isEmpty()) {
            out.println("Nenhum cliente cadastrado.");
            return false;
        }
        out.println(String.format("%-4s %-26s %-18s %7s %9s", "ID", "Nome", "Telefone", "Pontos", "Desconto"));
        for (Cliente c : lista) {
            int desconto = PoliticaFidelidade.percentualDesconto(sistema.ordens().contarConcluidas(c.getId()));
            out.println(String.format("%-4d %-26s %-18s %7d %8d%%", c.getId(), cortar(c.getNome(), 26),
                    c.getTelefone(), c.getPontos(), desconto));
        }
        return true;
    }

    private void editarCliente() {
        if (!listarClientes()) {
            return;
        }
        Cliente c = sistema.clientes().exigir(in.id("ID do cliente: "));
        String nome = in.texto("Novo nome [" + c.getNome() + "] (Enter mantém): ");
        String telefone = in.texto("Novo telefone [" + c.getTelefone() + "] (Enter mantém): ");
        sistema.clientes().editar(c.getId(), nome, telefone);
        out.println("[OK] Cliente atualizado.");
    }

    private void removerCliente() {
        if (!listarClientes()) {
            return;
        }
        int id = in.id("ID do cliente a remover: ");
        Cliente c = sistema.clientes().exigir(id);
        if (in.confirmar("Remover " + c.getNome() + "? (s/n): ")) {
            sistema.clientes().remover(id);
            out.println("[OK] Cliente removido.");
        }
    }

    // =====================================================================
    // Técnicos
    // =====================================================================

    private void menuTecnicos() {
        while (true) {
            int op = menu("TÉCNICOS", "Voltar", "Cadastrar", "Listar", "Editar", "Ativar/desativar", "Remover");
            if (op == 0) {
                return;
            }
            switch (op) {
                case 1: seguro(this::cadastrarTecnico); break;
                case 2: listarTecnicos(); break;
                case 3: seguro(this::editarTecnico); break;
                case 4: seguro(this::alternarTecnico); break;
                case 5: seguro(this::removerTecnico); break;
                default: break;
            }
        }
    }

    private void cadastrarTecnico() {
        String nome = in.textoObrigatorio("Nome: ");
        String telefone = in.texto("Telefone (opcional): ");
        TipoServico especialidade = escolherTipo("Especialidade:");
        Tecnico tecnico = sistema.tecnicos().cadastrar(nome, telefone, especialidade);
        out.println("[OK] Técnico cadastrado com ID " + tecnico.getId() + ".");
        int alocadas = sistema.ordens().alocarPendentes();
        if (alocadas > 0) {
            out.println("[OK] " + alocadas + " ordem(ns) da fila foram alocadas automaticamente.");
        }
    }

    private boolean listarTecnicos() {
        List<Tecnico> lista = sistema.tecnicos().listar();
        if (lista.isEmpty()) {
            out.println("Nenhum técnico cadastrado.");
            return false;
        }
        out.println(String.format("%-4s %-24s %-16s %-8s %-7s %s", "ID", "Nome", "Especialidade", "Situação",
                "Ordens", "Observação"));
        for (Tecnico t : lista) {
            boolean bloqueado = sistema.ordens().temEmergenciaAtiva(t.getId(), null);
            out.println(String.format("%-4d %-24s %-16s %-8s %-7d %s", t.getId(), cortar(t.getNome(), 24),
                    t.getEspecialidade().getDescricao(), t.isAtivo() ? "Ativo" : "Inativo",
                    sistema.ordens().cargaAtiva(t.getId()),
                    bloqueado ? "BLOQUEADO: em serviço emergencial" : ""));
        }
        return true;
    }

    private void editarTecnico() {
        if (!listarTecnicos()) {
            return;
        }
        Tecnico t = sistema.tecnicos().exigir(in.id("ID do técnico: "));
        String nome = in.texto("Novo nome [" + t.getNome() + "] (Enter mantém): ");
        String telefone = in.texto("Novo telefone [" + t.getTelefone() + "] (Enter mantém): ");
        TipoServico nova = null;
        if (in.confirmar("Alterar a especialidade (" + t.getEspecialidade().getDescricao() + ")? (s/n): ")) {
            nova = escolherTipo("Nova especialidade:");
        }
        sistema.tecnicos().editar(t.getId(), nome, telefone, nova);
        out.println("[OK] Técnico atualizado.");
    }

    private void alternarTecnico() {
        if (!listarTecnicos()) {
            return;
        }
        Tecnico t = sistema.tecnicos().exigir(in.id("ID do técnico: "));
        sistema.tecnicos().alterarAtivo(t.getId(), !t.isAtivo());
        out.println("[OK] " + t.getNome() + " agora está " + (t.isAtivo() ? "ativo" : "inativo") + ".");
        if (t.isAtivo()) {
            int alocadas = sistema.ordens().alocarPendentes();
            if (alocadas > 0) {
                out.println("[OK] " + alocadas + " ordem(ns) da fila foram alocadas automaticamente.");
            }
        }
    }

    private void removerTecnico() {
        if (!listarTecnicos()) {
            return;
        }
        int id = in.id("ID do técnico a remover: ");
        Tecnico t = sistema.tecnicos().exigir(id);
        if (in.confirmar("Remover " + t.getNome() + "? (s/n): ")) {
            sistema.tecnicos().remover(id);
            out.println("[OK] Técnico removido.");
        }
    }

    // =====================================================================
    // Catálogo
    // =====================================================================

    private void mostrarCatalogo() {
        out.println();
        out.println("--- CATÁLOGO DE SERVIÇOS ---");
        for (TipoServico tipo : TipoServico.values()) {
            out.println(String.format("%-16s valor base %s | emergência %s", tipo.getDescricao(),
                    Dinheiro.formatar(tipo.getValorBase()),
                    Dinheiro.formatar(tipo.getValorBase().multiply(new java.math.BigDecimal("1.50")))));
        }
        out.println("Emergência: +50% e prioridade na alocação. Elétrica/hidráulica exigem o técnico "
                + "especialista; reparos gerais podem ser feitos por qualquer técnico.");
    }

    // =====================================================================
    // Ordens de serviço
    // =====================================================================

    private void menuOrdens() {
        while (true) {
            int op = menu("ORDENS DE SERVIÇO", "Voltar", "Criar ordem", "Listar ordens", "Detalhar ordem",
                    "Iniciar ordem", "Concluir ordem", "Cancelar ordem", "Trocar técnico de uma ordem",
                    "Fila de alocação (prioridade)", "Alocar ordens pendentes agora");
            if (op == 0) {
                return;
            }
            switch (op) {
                case 1: seguro(this::criarOrdem); break;
                case 2: seguro(this::listarOrdens); break;
                case 3: seguro(this::detalharOrdem); break;
                case 4: seguro(() -> mudarStatus("iniciar")); break;
                case 5: seguro(() -> mudarStatus("concluir")); break;
                case 6: seguro(() -> mudarStatus("cancelar")); break;
                case 7: seguro(this::trocarTecnico); break;
                case 8: mostrarFila(); break;
                case 9: seguro(this::alocarAgora); break;
                default: break;
            }
        }
    }

    private void criarOrdem() {
        if (!listarClientes()) {
            out.println("Cadastre um cliente antes de criar uma ordem.");
            return;
        }
        Cliente cliente = sistema.clientes().exigir(in.id("ID do cliente: "));
        int desconto = PoliticaFidelidade.percentualDesconto(sistema.ordens().contarConcluidas(cliente.getId()));
        out.println("Desconto de fidelidade deste cliente: " + desconto + "%");
        TipoServico tipo = escolherTipo("Tipo de serviço:");
        String descricao = in.textoObrigatorio("Descrição do problema: ");
        LocalDate prazo = in.data("Prazo de conclusão (dd/MM/aaaa): ");
        boolean emergencia = in.confirmar("É um serviço emergencial (taxa de 50%)? (s/n): ");

        OrdemServico ordem = sistema.ordens().criar(cliente.getId(), tipo, descricao, prazo, emergencia);
        out.println("[OK] Ordem #" + ordem.getId() + " criada (" + (emergencia ? "EMERGÊNCIA" : "normal") + ").");
        if (ordem.getTecnicoId() == null) {
            out.println("[i] Nenhum técnico disponível agora. A ordem entrou na fila de alocação"
                    + (emergencia ? " com prioridade." : "."));
        } else {
            out.println("[i] Técnico alocado: " + sistema.tecnicos().nomeDe(ordem.getTecnicoId()));
        }
        mostrarValores(ordem);
    }

    private void mostrarValores(OrdemServico o) {
        out.println("    Valor base........: " + Dinheiro.formatar(o.getTipo().getValorBase()));
        if (o.isEmergencia()) {
            out.println("    Taxa emergência (50%): " + Dinheiro.formatar(o.getValorTaxaAdicional()));
        }
        if (o.getDescontoFidelidade() > 0) {
            out.println("    Desconto fidelidade (" + o.getDescontoFidelidade() + "%): -"
                    + Dinheiro.formatar(o.getValorDescontoFidelidade()));
        }
        out.println("    Valor da ordem....: " + Dinheiro.formatar(o.getValorComDesconto()));
    }

    private void listarOrdens() {
        StatusOrdem[] opcoes = StatusOrdem.values();
        out.println("Filtrar por status (0 = todas):");
        for (int i = 0; i < opcoes.length; i++) {
            out.println("  " + (i + 1) + " - " + opcoes[i].getDescricao());
        }
        int escolha = in.inteiroEntre("Escolha: ", 0, opcoes.length);
        List<OrdemServico> lista = escolha == 0 ? sistema.ordens().listar()
                : sistema.ordens().listarPorStatus(opcoes[escolha - 1]);
        imprimirOrdens(lista);
    }

    /** @return true se havia ordens para mostrar */
    private boolean imprimirOrdens(List<OrdemServico> lista) {
        if (lista.isEmpty()) {
            out.println("Nenhuma ordem encontrada.");
            return false;
        }
        out.println(String.format("%-4s %-10s %-15s %-13s %-18s %-18s %-10s %12s", "ID", "Tipo", "Serviço",
                "Status", "Cliente", "Técnico", "Prazo", "Valor"));
        for (OrdemServico o : lista) {
            out.println(String.format("%-4d %-10s %-15s %-13s %-18s %-18s %-10s %12s", o.getId(),
                    o.isEmergencia() ? "EMERGÊNCIA" : "Normal", o.getTipo().getDescricao(),
                    o.getStatus().getDescricao(), cortar(sistema.clientes().nomeDe(o.getClienteId()), 18),
                    cortar(sistema.tecnicos().nomeDe(o.getTecnicoId()), 18), Datas.formatar(o.getPrazo()),
                    Dinheiro.formatar(o.getValorComDesconto())));
        }
        return true;
    }

    private void detalharOrdem() {
        if (!imprimirOrdens(sistema.ordens().listar())) {
            return;
        }
        OrdemServico o = sistema.ordens().exigir(in.id("ID da ordem: "));
        out.println("Ordem #" + o.getId() + " - " + (o.isEmergencia() ? "EMERGENCIAL" : "normal"));
        out.println("    Cliente.....: " + sistema.clientes().nomeDe(o.getClienteId()));
        out.println("    Técnico.....: " + sistema.tecnicos().nomeDe(o.getTecnicoId()));
        out.println("    Serviço.....: " + o.getTipo().getDescricao());
        out.println("    Problema....: " + o.getDescricao());
        out.println("    Criada em...: " + Datas.formatar(o.getCriadaEm()));
        out.println("    Prazo.......: " + Datas.formatar(o.getPrazo()));
        out.println("    Status......: " + o.getStatus().getDescricao() + (o.isPaga() ? " (paga)" : ""));
        mostrarValores(o);
    }

    private void mudarStatus(String acao) {
        List<OrdemServico> candidatas = sistema.ordens().listar().stream()
                .filter(o -> o.getStatus().isAtiva()).collect(java.util.stream.Collectors.toList());
        if (!imprimirOrdens(candidatas)) {
            return;
        }
        int id = in.id("ID da ordem: ");
        switch (acao) {
            case "iniciar":
                sistema.ordens().iniciar(id);
                out.println("[OK] Ordem #" + id + " em andamento.");
                break;
            case "concluir":
                sistema.ordens().concluir(id);
                out.println("[OK] Ordem #" + id + " concluída. Já pode ser paga.");
                break;
            default:
                if (in.confirmar("Cancelar a ordem #" + id + "? (s/n): ")) {
                    sistema.ordens().cancelar(id);
                    out.println("[OK] Ordem #" + id + " cancelada.");
                }
                break;
        }
    }

    private void trocarTecnico() {
        List<OrdemServico> pendentes = sistema.ordens().listarPorStatus(StatusOrdem.PENDENTE);
        if (!imprimirOrdens(pendentes)) {
            return;
        }
        int ordemId = in.id("ID da ordem: ");
        if (!listarTecnicos()) {
            return;
        }
        int tecnicoId = in.id("ID do novo técnico: ");
        sistema.ordens().alocarManualmente(ordemId, tecnicoId);
        out.println("[OK] Ordem #" + ordemId + " alocada para " + sistema.tecnicos().nomeDe(tecnicoId) + ".");
    }

    private void mostrarFila() {
        out.println("Fila de alocação (emergências primeiro, depois menor prazo):");
        imprimirOrdens(sistema.ordens().filaDeAlocacao());
    }

    private void alocarAgora() {
        int alocadas = sistema.ordens().alocarPendentes();
        out.println(alocadas == 0 ? "Nenhuma ordem pôde ser alocada agora."
                : "[OK] " + alocadas + " ordem(ns) alocada(s).");
    }

    // =====================================================================
    // Pagamentos
    // =====================================================================

    private void menuPagamentos() {
        while (true) {
            int op = menu("PAGAMENTOS E FIDELIDADE", "Voltar", "Registrar pagamento de ordem concluída",
                    "Histórico de pagamentos de um cliente");
            if (op == 0) {
                return;
            }
            if (op == 1) {
                seguro(this::registrarPagamento);
            } else {
                seguro(this::historicoPagamentos);
            }
        }
    }

    private void registrarPagamento() {
        List<OrdemServico> aPagar = sistema.ordens().listarConcluidasNaoPagas();
        if (!imprimirOrdens(aPagar)) {
            out.println("(Somente ordens concluídas podem ser pagas.)");
            return;
        }
        int ordemId = in.id("ID da ordem a pagar: ");
        OrdemServico ordem = sistema.ordens().exigir(ordemId);
        Cliente cliente = sistema.clientes().exigir(ordem.getClienteId());

        boolean usarPontos = false;
        if (cliente.getPontos() >= PoliticaFidelidade.PONTOS_PARA_RESGATE) {
            usarPontos = in.confirmar(cliente.getNome() + " tem " + cliente.getPontos() + " pontos. Trocar "
                    + PoliticaFidelidade.PONTOS_PARA_RESGATE + " pontos por "
                    + Dinheiro.formatar(PoliticaFidelidade.DESCONTO_RESGATE) + " de desconto? (s/n): ");
        }
        Cotacao c = sistema.pagamentos().cotar(ordemId, usarPontos);
        out.println("Resumo do pagamento:");
        out.println("    Valor do serviço....: " + Dinheiro.formatar(c.valorServico));
        out.println("    Desconto fidelidade (" + c.percentualFidelidade + "%): -"
                + Dinheiro.formatar(c.descontoFidelidade));
        if (c.pontosUsados > 0) {
            out.println("    Desconto por pontos (" + c.pontosUsados + " pts): -"
                    + Dinheiro.formatar(c.descontoPontos));
        }
        out.println("    TOTAL A PAGAR.......: " + Dinheiro.formatar(c.valorFinal));
        out.println("    Pontos que serão ganhos: " + c.pontosGanhos);

        FormaPagamento forma = in.escolher("Forma de pagamento:", FormaPagamento.values(),
                FormaPagamento::getDescricao);
        if (in.confirmar("Confirmar pagamento? (s/n): ")) {
            Pagamento p = sistema.pagamentos().pagar(ordemId, forma, usarPontos);
            out.println("[OK] Pagamento #" + p.getId() + " registrado: " + Dinheiro.formatar(p.getValorPago())
                    + " (" + forma.getDescricao() + "). Pontos do cliente: "
                    + sistema.clientes().exigir(cliente.getId()).getPontos() + ".");
        } else {
            out.println("Pagamento cancelado.");
        }
    }

    private void historicoPagamentos() {
        if (!listarClientes()) {
            return;
        }
        int clienteId = in.id("ID do cliente: ");
        out.println();
        out.print(sistema.relatorios().gerarHistoricoCliente(clienteId));
    }

    // =====================================================================
    // Relatórios
    // =====================================================================

    private void menuRelatorios() {
        while (true) {
            int op = menu("RELATÓRIOS", "Voltar", "Relatório geral (todo o histórico)",
                    "Relatório por período", "Histórico de um cliente", "Exportar relatório geral para arquivo .txt");
            if (op == 0) {
                return;
            }
            switch (op) {
                case 1:
                    seguro(() -> out.print(sistema.relatorios().gerarRelatorioGeral(null, null)));
                    break;
                case 2: seguro(this::relatorioPorPeriodo); break;
                case 3: seguro(this::historicoPagamentos); break;
                case 4: seguro(this::exportarRelatorio); break;
                default: break;
            }
        }
    }

    private void relatorioPorPeriodo() {
        LocalDate inicio = in.data("Data inicial (dd/MM/aaaa): ");
        LocalDate fim = in.data("Data final (dd/MM/aaaa): ");
        out.print(sistema.relatorios().gerarRelatorioGeral(inicio, fim));
    }

    private void exportarRelatorio() {
        String texto = sistema.relatorios().gerarRelatorioGeral(null, null);
        String nome = "relatorio_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".txt";
        Path destino = sistema.relatorios().exportar(texto, sistema.getPastaDados().resolve("relatorios"), nome);
        out.println("[OK] Relatório gravado em: " + destino.toAbsolutePath());
    }

    // =====================================================================
    // Demonstração e auxiliares
    // =====================================================================

    private void carregarDemonstracao() {
        DadosDemonstracao.carregar(sistema, LocalDate.now());
        out.println("[OK] Dados de demonstração carregados. Veja a fila de alocação e a lista de técnicos!");
    }

    private TipoServico escolherTipo(String titulo) {
        return in.escolher(titulo, TipoServico.values(),
                t -> t.getDescricao() + " (" + Dinheiro.formatar(t.getValorBase()) + ")");
    }

    private static String cortar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite - 1) + "…";
    }
}
