package consertatudo;

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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Testes das regras de negócio, sem dependências externas.
 * Execute com: java -cp out consertatudo.TestesSistema
 */
public class TestesSistema {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 20);
    private static final Clock RELOGIO = Clock.fixed(
            HOJE.atStartOfDay(ZoneId.of("America/Fortaleza")).toInstant(), ZoneId.of("America/Fortaleza"));

    private static int aprovados = 0;
    private static final List<String> falhas = new ArrayList<>();

    private interface Teste {
        void executar() throws Exception;
    }

    public static void main(String[] args) {
        System.setOut(new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new java.io.PrintStream(System.err, true, StandardCharsets.UTF_8));
        rodar("valor da ordem normal e emergencial (taxa de 50%)", TestesSistema::testeValores);
        rodar("desconto de fidelidade: 10% por ordem concluída, máximo 40%", TestesSistema::testeDescontoFidelidade);
        rodar("política de pontos: 1 ponto por R$ 10 e resgate de 100 pontos", TestesSistema::testePontos);
        rodar("pagamento aplica desconto, resgate de pontos e acumula novos pontos", TestesSistema::testePagamentoCompleto);
        rodar("só ordens concluídas podem ser pagas (e apenas uma vez)", TestesSistema::testeSoConcluidaPaga);
        rodar("transições de status inválidas são recusadas", TestesSistema::testeTransicoesInvalidas);
        rodar("técnico em emergência fica bloqueado para novos agendamentos", TestesSistema::testeBloqueioEmergencia);
        rodar("emergência tem prioridade na fila de alocação", TestesSistema::testePrioridadeNaFila);
        rodar("alocação respeita a especialidade do técnico", TestesSistema::testeEspecialidade);
        rodar("prazo no passado e dados inválidos são recusados", TestesSistema::testeValidacoes);
        rodar("persistência em CSV preserva dados (inclusive ';' e aspas)", TestesSistema::testePersistencia);
        rodar("arquivos ausentes e linhas corrompidas não quebram o sistema", TestesSistema::testeArquivosAusentesECorrompidos);
        rodar("não remove cliente/técnico com histórico", TestesSistema::testeRemocaoComHistorico);
        rodar("relatório traz total de ordens realizadas e faturamento", TestesSistema::testeRelatorio);

        System.out.println();
        System.out.println("Aprovados: " + aprovados + " | Falhas: " + falhas.size());
        for (String f : falhas) {
            System.out.println("  FALHOU: " + f);
        }
        System.exit(falhas.isEmpty() ? 0 : 1);
    }

    // ---------- testes ----------

    private static void testeValores() {
        Sistema s = novoSistema();
        Cliente c = s.clientes().cadastrar("Ana", "");
        OrdemServico normal = s.ordens().criar(c.getId(), TipoServico.ELETRICA, "Fiação", HOJE.plusDays(3), false);
        OrdemServico emerg = s.ordens().criar(c.getId(), TipoServico.ELETRICA, "Curto", HOJE.plusDays(1), true);
        igual(new BigDecimal("150.00"), normal.getValorBruto());
        igual(new BigDecimal("225.00"), emerg.getValorBruto());
        igual(new BigDecimal("195.00"), s.ordens().criar(c.getId(), TipoServico.HIDRAULICA, "Cano", HOJE, true).getValorBruto());
        igual(new BigDecimal("100.00"), s.ordens().criar(c.getId(), TipoServico.GERAL, "Reparo", HOJE, false).getValorBruto());
    }

    private static void testeDescontoFidelidade() {
        igual(0, PoliticaFidelidade.percentualDesconto(0));
        igual(10, PoliticaFidelidade.percentualDesconto(1));
        igual(30, PoliticaFidelidade.percentualDesconto(3));
        igual(40, PoliticaFidelidade.percentualDesconto(4));
        igual(40, PoliticaFidelidade.percentualDesconto(9));

        Sistema s = novoSistema();
        s.tecnicos().cadastrar("João", "", TipoServico.GERAL);
        Cliente c = s.clientes().cadastrar("Ana", "");
        for (int i = 0; i < 5; i++) {
            OrdemServico o = s.ordens().criar(c.getId(), TipoServico.GERAL, "Reparo " + i, HOJE.plusDays(2), false);
            igual(Math.min(i * 10, 40), o.getDescontoFidelidade());
            s.ordens().iniciar(o.getId());
            s.ordens().concluir(o.getId());
        }
        // 5 concluídas: teto de 40% -> R$ 100 vira R$ 60
        OrdemServico sexta = s.ordens().criar(c.getId(), TipoServico.GERAL, "Reparo final", HOJE.plusDays(2), false);
        igual(new BigDecimal("60.00"), sexta.getValorComDesconto());
    }

    private static void testePontos() {
        igual(0, PoliticaFidelidade.pontosGanhos(new BigDecimal("9.99")));
        igual(1, PoliticaFidelidade.pontosGanhos(new BigDecimal("10.00")));
        igual(22, PoliticaFidelidade.pontosGanhos(new BigDecimal("225.00")));
        igual(10, PoliticaFidelidade.pontosGanhos(new BigDecimal("100.00")));
    }

    private static void testePagamentoCompleto() {
        Sistema s = novoSistema();
        s.tecnicos().cadastrar("João", "", TipoServico.GERAL);
        Cliente c = s.clientes().cadastrar("Ana", "");

        // várias ordens pagas para acumular pontos (o desconto de fidelidade sobe até 40%)
        for (int i = 0; i < 16; i++) {
            OrdemServico o = s.ordens().criar(c.getId(), TipoServico.GERAL, "Reparo " + i, HOJE.plusDays(2), false);
            s.ordens().iniciar(o.getId());
            s.ordens().concluir(o.getId());
            s.pagamentos().pagar(o.getId(), FormaPagamento.PIX, false);
        }
        Cliente atualizado = s.clientes().exigir(c.getId());
        check(atualizado.getPontos() >= 100, "cliente deveria ter ao menos 100 pontos, tem " + atualizado.getPontos());

        int pontosAntes = atualizado.getPontos();
        OrdemServico o = s.ordens().criar(c.getId(), TipoServico.GERAL, "Ordem com resgate", HOJE.plusDays(2), false);
        igual(new BigDecimal("60.00"), o.getValorComDesconto()); // 40% de desconto
        s.ordens().iniciar(o.getId());
        s.ordens().concluir(o.getId());

        Cotacao cotacao = s.pagamentos().cotar(o.getId(), true);
        igual(100, cotacao.pontosUsados);
        igual(new BigDecimal("20.00"), cotacao.descontoPontos);
        igual(new BigDecimal("40.00"), cotacao.valorFinal);
        igual(4, cotacao.pontosGanhos);

        Pagamento p = s.pagamentos().pagar(o.getId(), FormaPagamento.CARTAO, true);
        igual(new BigDecimal("40.00"), p.getValorPago());
        igual(pontosAntes - 100 + 4, s.clientes().exigir(c.getId()).getPontos());
        check(s.ordens().exigir(o.getId()).isPaga(), "ordem deveria estar paga");
    }

    private static void testeSoConcluidaPaga() {
        Sistema s = novoSistema();
        s.tecnicos().cadastrar("João", "", TipoServico.GERAL);
        Cliente c = s.clientes().cadastrar("Ana", "");
        OrdemServico o = s.ordens().criar(c.getId(), TipoServico.GERAL, "Reparo", HOJE.plusDays(2), false);
        lanca(() -> s.pagamentos().pagar(o.getId(), FormaPagamento.PIX, false), "pendente não pode ser paga");
        s.ordens().iniciar(o.getId());
        lanca(() -> s.pagamentos().pagar(o.getId(), FormaPagamento.PIX, false), "em andamento não pode ser paga");
        s.ordens().concluir(o.getId());
        s.pagamentos().pagar(o.getId(), FormaPagamento.PIX, false);
        lanca(() -> s.pagamentos().pagar(o.getId(), FormaPagamento.PIX, false), "pagamento duplicado");

        OrdemServico cancelada = s.ordens().criar(c.getId(), TipoServico.GERAL, "Outra", HOJE.plusDays(2), false);
        s.ordens().cancelar(cancelada.getId());
        lanca(() -> s.pagamentos().pagar(cancelada.getId(), FormaPagamento.PIX, false), "cancelada não pode ser paga");
    }

    private static void testeTransicoesInvalidas() {
        Sistema s = novoSistema();
        s.tecnicos().cadastrar("João", "", TipoServico.GERAL);
        Cliente c = s.clientes().cadastrar("Ana", "");
        OrdemServico o = s.ordens().criar(c.getId(), TipoServico.GERAL, "Reparo", HOJE.plusDays(2), false);
        lanca(() -> s.ordens().concluir(o.getId()), "concluir sem iniciar");
        s.ordens().iniciar(o.getId());
        lanca(() -> s.ordens().iniciar(o.getId()), "iniciar duas vezes");
        s.ordens().concluir(o.getId());
        lanca(() -> s.ordens().cancelar(o.getId()), "cancelar concluída");

        // ordem sem técnico não pode ser iniciada
        Sistema vazio = novoSistema();
        Cliente c2 = vazio.clientes().cadastrar("Bia", "");
        OrdemServico semTecnico = vazio.ordens().criar(c2.getId(), TipoServico.GERAL, "Reparo", HOJE, false);
        check(semTecnico.getTecnicoId() == null, "sem técnicos cadastrados a ordem deveria ficar sem técnico");
        lanca(() -> vazio.ordens().iniciar(semTecnico.getId()), "iniciar sem técnico");
    }

    private static void testeBloqueioEmergencia() {
        Sistema s = novoSistema();
        Tecnico carlos = s.tecnicos().cadastrar("Carlos", "", TipoServico.ELETRICA);
        Cliente c = s.clientes().cadastrar("Ana", "");

        OrdemServico emerg = s.ordens().criar(c.getId(), TipoServico.ELETRICA, "Curto", HOJE.plusDays(1), true);
        igual(carlos.getId(), emerg.getTecnicoId());

        // novo agendamento normal NÃO pode ir para o técnico em emergência
        OrdemServico normal = s.ordens().criar(c.getId(), TipoServico.ELETRICA, "Tomada", HOJE.plusDays(5), false);
        check(normal.getTecnicoId() == null, "técnico em emergência não deveria receber nova ordem");
        lanca(() -> s.ordens().alocarManualmente(normal.getId(), carlos.getId()), "alocação manual bloqueada");

        // concluindo a emergência, o técnico é liberado e a fila anda sozinha
        s.ordens().iniciar(emerg.getId());
        s.ordens().concluir(emerg.getId());
        igual(carlos.getId(), s.ordens().exigir(normal.getId()).getTecnicoId());
    }

    private static void testePrioridadeNaFila() {
        Sistema s = novoSistema();
        Cliente c = s.clientes().cadastrar("Ana", "");
        // sem técnicos: tudo cai na fila
        OrdemServico normalCedo = s.ordens().criar(c.getId(), TipoServico.ELETRICA, "Normal urgente no prazo", HOJE, false);
        OrdemServico emergTarde = s.ordens().criar(c.getId(), TipoServico.ELETRICA, "Emergência", HOJE.plusDays(9), true);
        List<OrdemServico> fila = s.ordens().filaDeAlocacao();
        igual(emergTarde.getId(), fila.get(0).getId()); // emergência vem antes mesmo com prazo maior
        igual(normalCedo.getId(), fila.get(1).getId());

        // aparece um único eletricista: a emergência é atendida primeiro
        Tecnico carlos = s.tecnicos().cadastrar("Carlos", "", TipoServico.ELETRICA);
        s.ordens().alocarPendentes();
        igual(carlos.getId(), s.ordens().exigir(emergTarde.getId()).getTecnicoId());
        check(s.ordens().exigir(normalCedo.getId()).getTecnicoId() == null,
                "a ordem normal deveria esperar enquanto o técnico está em emergência");
    }

    private static void testeEspecialidade() {
        Sistema s = novoSistema();
        Tecnico encanador = s.tecnicos().cadastrar("Marina", "", TipoServico.HIDRAULICA);
        Cliente c = s.clientes().cadastrar("Ana", "");

        OrdemServico eletrica = s.ordens().criar(c.getId(), TipoServico.ELETRICA, "Fiação", HOJE, false);
        check(eletrica.getTecnicoId() == null, "encanador não deve receber serviço elétrico");
        lanca(() -> s.ordens().alocarManualmente(eletrica.getId(), encanador.getId()), "especialidade errada");

        OrdemServico geral = s.ordens().criar(c.getId(), TipoServico.GERAL, "Reparo", HOJE, false);
        igual(encanador.getId(), geral.getTecnicoId()); // reparos gerais: qualquer técnico

        // o técnico tem uma ordem em aberto: não pode ser desativado
        lanca(() -> s.tecnicos().alterarAtivo(encanador.getId(), false), "desativar técnico com ordem em aberto");
    }

    private static void testeValidacoes() {
        Sistema s = novoSistema();
        Cliente c = s.clientes().cadastrar("Ana", "(86) 99999-0000");
        lanca(() -> s.ordens().criar(c.getId(), TipoServico.GERAL, "Reparo", HOJE.minusDays(1), false), "prazo no passado");
        lanca(() -> s.ordens().criar(c.getId(), TipoServico.GERAL, "  ", HOJE, false), "descrição vazia");
        lanca(() -> s.ordens().criar(999, TipoServico.GERAL, "Reparo", HOJE, false), "cliente inexistente");
        lanca(() -> s.clientes().cadastrar("   ", ""), "nome vazio");
        lanca(() -> s.clientes().cadastrar("Beto", "abc"), "telefone inválido");
        lanca(() -> s.tecnicos().cadastrar("Téc", "", null), "especialidade nula");
    }

    private static void testePersistencia() throws Exception {
        Path pasta = pastaTemporaria();
        Sistema s1 = new Sistema(pasta, RELOGIO);
        s1.tecnicos().cadastrar("Carlos", "(86) 96666-0001", TipoServico.ELETRICA);
        Cliente c = s1.clientes().cadastrar("Maria \"Mimi\" Silva", "(86) 99999-0000");
        String descricao = "Trocar fiação; disjuntor 20A e tomada de 3\" na parede";
        OrdemServico o = s1.ordens().criar(c.getId(), TipoServico.ELETRICA, descricao, HOJE.plusDays(4), true);
        s1.ordens().iniciar(o.getId());
        s1.ordens().concluir(o.getId());
        s1.pagamentos().pagar(o.getId(), FormaPagamento.PIX, false);

        Sistema s2 = new Sistema(pasta, RELOGIO); // recarrega dos arquivos
        Cliente c2 = s2.clientes().exigir(c.getId());
        igual("Maria \"Mimi\" Silva", c2.getNome());
        igual(22, c2.getPontos());
        OrdemServico o2 = s2.ordens().exigir(o.getId());
        igual(descricao, o2.getDescricao());
        check(o2.isEmergencia(), "categoria emergência deveria ser preservada");
        igual(StatusOrdem.CONCLUIDA, o2.getStatus());
        check(o2.isPaga(), "flag paga deveria ser preservada");
        igual(1, s2.pagamentos().listar().size());
        igual(new BigDecimal("225.00"), s2.pagamentos().listar().get(0).getValorPago());
    }

    private static void testeArquivosAusentesECorrompidos() throws Exception {
        // pasta que nem existe ainda
        Path inexistente = pastaTemporaria().resolve("nao/existe/ainda");
        Sistema vazio = new Sistema(inexistente, RELOGIO);
        igual(0, vazio.clientes().listar().size());
        vazio.clientes().cadastrar("Ana", ""); // cria a pasta ao salvar
        check(Files.exists(inexistente.resolve("clientes.csv")), "deveria ter criado a pasta e o arquivo");

        // arquivo com lixo no meio
        Path pasta = pastaTemporaria();
        String csv = "id;nome;telefone;pontos\n"
                + "1;Ana;;0\n"
                + "isto não é uma linha válida\n"
                + "x;Beto;;abc\n"
                + "\n"
                + "3;\"Caio;Sem fechar;;0\n"
                + "4;Dani;(86) 99999-0000;5\n";
        Files.write(pasta.resolve("clientes.csv"), csv.getBytes(StandardCharsets.UTF_8));
        Sistema s = new Sistema(pasta, RELOGIO);
        igual(2, s.clientes().listar().size()); // só Ana e Dani
        igual("Dani", s.clientes().exigir(4).getNome());
        igual(5, s.clientes().exigir(4).getPontos());
    }

    private static void testeRemocaoComHistorico() {
        Sistema s = novoSistema();
        Tecnico t = s.tecnicos().cadastrar("João", "", TipoServico.GERAL);
        Cliente c = s.clientes().cadastrar("Ana", "");
        Cliente semOrdens = s.clientes().cadastrar("Beto", "");
        s.ordens().criar(c.getId(), TipoServico.GERAL, "Reparo", HOJE, false);
        lanca(() -> s.clientes().remover(c.getId()), "cliente com ordens");
        lanca(() -> s.tecnicos().remover(t.getId()), "técnico com ordens");
        s.clientes().remover(semOrdens.getId());
        igual(1, s.clientes().listar().size());
    }

    private static void testeRelatorio() {
        Sistema s = novoSistema();
        s.tecnicos().cadastrar("Carlos", "", TipoServico.ELETRICA);
        Cliente c = s.clientes().cadastrar("Ana", "");
        OrdemServico o = s.ordens().criar(c.getId(), TipoServico.ELETRICA, "Curto", HOJE.plusDays(1), true);
        s.ordens().iniciar(o.getId());
        s.ordens().concluir(o.getId());
        s.pagamentos().pagar(o.getId(), FormaPagamento.PIX, false);
        s.ordens().criar(c.getId(), TipoServico.GERAL, "Outra", HOJE.plusDays(1), false);

        String relatorio = s.relatorios().gerarRelatorioGeral(null, null);
        check(relatorio.contains("Total de ordens......: 2"), "total de ordens");
        check(relatorio.contains("Concluída") && relatorio.contains(": 1"), "ordens concluídas");
        check(relatorio.contains("225,00"), "faturamento de R$ 225,00");
        check(s.relatorios().gerarHistoricoCliente(c.getId()).contains("OS #1"), "histórico do cliente");

        // período sem movimento
        String vazio = s.relatorios().gerarRelatorioGeral(HOJE.plusDays(10), HOJE.plusDays(20));
        check(vazio.contains("Total de ordens......: 0"), "período vazio");
        lanca(() -> s.relatorios().gerarRelatorioGeral(HOJE, HOJE.minusDays(1)), "período invertido");
    }

    // ---------- infraestrutura de testes ----------

    private static Sistema novoSistema() {
        return new Sistema(pastaTemporaria(), RELOGIO);
    }

    private static Path pastaTemporaria() {
        try {
            return Files.createTempDirectory("conserta-tudo-teste");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void rodar(String nome, Teste teste) {
        try {
            teste.executar();
            aprovados++;
            System.out.println("[ok]    " + nome);
        } catch (Throwable e) {
            falhas.add(nome + " -> " + e);
            System.out.println("[FALHA] " + nome + " -> " + e);
        }
    }

    private static void igual(Object esperado, Object atual) {
        boolean iguais = esperado instanceof BigDecimal && atual instanceof BigDecimal
                ? ((BigDecimal) esperado).compareTo((BigDecimal) atual) == 0
                : esperado.equals(atual);
        if (!iguais) {
            throw new AssertionError("esperado <" + esperado + "> mas foi <" + atual + ">");
        }
    }

    private static void check(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }

    private static void lanca(Runnable acao, String descricao) {
        try {
            acao.run();
        } catch (RegraNegocioException e) {
            return;
        }
        throw new AssertionError("deveria ter lançado RegraNegocioException: " + descricao);
    }
}
