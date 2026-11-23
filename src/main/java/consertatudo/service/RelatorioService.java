package consertatudo.service;

import consertatudo.exception.PersistenciaException;
import consertatudo.exception.RegraNegocioException;
import consertatudo.model.Cliente;
import consertatudo.model.OrdemServico;
import consertatudo.model.Pagamento;
import consertatudo.model.StatusOrdem;
import consertatudo.model.Tecnico;
import consertatudo.model.TipoServico;
import consertatudo.persistence.ClienteRepositorio;
import consertatudo.persistence.OrdemRepositorio;
import consertatudo.persistence.PagamentoRepositorio;
import consertatudo.persistence.TecnicoRepositorio;
import consertatudo.util.Datas;
import consertatudo.util.Dinheiro;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Geração dos relatórios em texto (console ou arquivo .txt). */
public class RelatorioService {

    private final ClienteRepositorio clientes;
    private final TecnicoRepositorio tecnicos;
    private final OrdemRepositorio ordens;
    private final PagamentoRepositorio pagamentos;
    private final Clock relogio;

    public RelatorioService(ClienteRepositorio clientes, TecnicoRepositorio tecnicos,
                            OrdemRepositorio ordens, PagamentoRepositorio pagamentos, Clock relogio) {
        this.clientes = clientes;
        this.tecnicos = tecnicos;
        this.ordens = ordens;
        this.pagamentos = pagamentos;
        this.relogio = relogio;
    }

    /** Relatório geral; datas nulas significam "sem limite". */
    public String gerarRelatorioGeral(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new RegraNegocioException("A data final não pode ser anterior à data inicial.");
        }
        List<OrdemServico> doPeriodo = ordens.listar().stream()
                .filter(o -> dentro(o.getCriadaEm(), inicio, fim)).collect(Collectors.toList());
        List<Pagamento> pagosNoPeriodo = pagamentos.listar().stream()
                .filter(p -> dentro(p.getData(), inicio, fim)).collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("==============================================\n");
        sb.append("   RELATÓRIO DE ORDENS DE SERVIÇO - CONSERTA TUDO\n");
        sb.append("==============================================\n");
        sb.append("Gerado em: ")
                .append(LocalDateTime.now(relogio).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append('\n');
        sb.append("Período: ").append(descreverPeriodo(inicio, fim)).append("\n\n");

        sb.append("ORDENS DE SERVIÇO (criadas no período)\n");
        sb.append("  Total de ordens......: ").append(doPeriodo.size()).append('\n');
        for (StatusOrdem s : StatusOrdem.values()) {
            sb.append(String.format("  %-20s: %d%n", s.getDescricao(), contar(doPeriodo, s)));
        }
        long emergencias = doPeriodo.stream().filter(OrdemServico::isEmergencia).count();
        sb.append("  Emergenciais.........: ").append(emergencias).append("\n\n");

        BigDecimal faturamento = soma(pagosNoPeriodo.stream().map(Pagamento::getValorPago));
        BigDecimal descontos = soma(pagosNoPeriodo.stream().map(Pagamento::getTotalDescontos));
        sb.append("FATURAMENTO (pagamentos no período)\n");
        sb.append("  Ordens realizadas e pagas: ").append(pagosNoPeriodo.size()).append('\n');
        sb.append("  Faturamento total........: ").append(Dinheiro.formatar(faturamento)).append('\n');
        sb.append("  Descontos concedidos.....: ").append(Dinheiro.formatar(descontos)).append("\n\n");

        sb.append("FATURAMENTO POR TIPO DE SERVIÇO\n");
        for (TipoServico tipo : TipoServico.values()) {
            BigDecimal total = soma(pagosNoPeriodo.stream()
                    .filter(p -> ordens.buscarPorId(p.getOrdemId()).map(o -> o.getTipo() == tipo).orElse(false))
                    .map(Pagamento::getValorPago));
            sb.append(String.format("  %-16s: %s%n", tipo.getDescricao(), Dinheiro.formatar(total)));
        }

        sb.append("\nORDENS CONCLUÍDAS POR TÉCNICO\n");
        if (tecnicos.listar().isEmpty()) {
            sb.append("  (nenhum técnico cadastrado)\n");
        }
        for (Tecnico t : tecnicos.listar()) {
            long concluidas = doPeriodo.stream()
                    .filter(o -> o.getStatus() == StatusOrdem.CONCLUIDA
                            && o.getTecnicoId() != null && o.getTecnicoId() == t.getId())
                    .count();
            sb.append(String.format("  %-25s (%s): %d%n", t.getNome(), t.getEspecialidade().getDescricao(), concluidas));
        }
        return sb.toString();
    }

    /** Histórico de ordens e pagamentos de um cliente. */
    public String gerarHistoricoCliente(int clienteId) {
        Cliente cliente = clientes.exigirPorId(clienteId);
        List<OrdemServico> doCliente = ordens.listar().stream()
                .filter(o -> o.getClienteId() == clienteId)
                .sorted(Comparator.comparingInt(OrdemServico::getId))
                .collect(Collectors.toList());
        List<Pagamento> pagosDoCliente = pagamentos.listar().stream()
                .filter(p -> p.getClienteId() == clienteId).collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("HISTÓRICO DO CLIENTE: ").append(cliente.getNome())
                .append(" (ID ").append(cliente.getId()).append(")\n");
        sb.append("Pontos de fidelidade: ").append(cliente.getPontos()).append("\n\n");
        sb.append("Ordens de serviço:\n");
        if (doCliente.isEmpty()) {
            sb.append("  (nenhuma ordem registrada)\n");
        }
        for (OrdemServico o : doCliente) {
            sb.append(String.format("  OS #%-3d %-10s %-15s %-13s %s  %s%n", o.getId(),
                    o.isEmergencia() ? "Emergência" : "Normal", o.getTipo().getDescricao(),
                    o.getStatus().getDescricao(), Dinheiro.formatar(o.getValorComDesconto()),
                    o.isPaga() ? "[paga]" : ""));
        }
        sb.append("\nPagamentos:\n");
        if (pagosDoCliente.isEmpty()) {
            sb.append("  (nenhum pagamento registrado)\n");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Pagamento p : pagosDoCliente) {
            sb.append(String.format("  %s  OS #%-3d pago %s (%s)  +%d pts  -%d pts%n", Datas.formatar(p.getData()),
                    p.getOrdemId(), Dinheiro.formatar(p.getValorPago()), p.getForma().getDescricao(),
                    p.getPontosGanhos(), p.getPontosUsados()));
            total = total.add(p.getValorPago());
        }
        sb.append("\nTotal pago pelo cliente: ").append(Dinheiro.formatar(total)).append('\n');
        return sb.toString();
    }

    /** Grava o texto do relatório em um arquivo dentro da pasta indicada. */
    public Path exportar(String conteudo, Path pasta, String nomeArquivo) {
        try {
            Files.createDirectories(pasta);
            Path destino = pasta.resolve(nomeArquivo);
            Files.write(destino, conteudo.getBytes(StandardCharsets.UTF_8));
            return destino;
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível gravar o relatório", e);
        }
    }

    // ---------- auxiliares ----------

    private static boolean dentro(LocalDate data, LocalDate inicio, LocalDate fim) {
        return (inicio == null || !data.isBefore(inicio)) && (fim == null || !data.isAfter(fim));
    }

    private static long contar(List<OrdemServico> lista, StatusOrdem status) {
        return lista.stream().filter(o -> o.getStatus() == status).count();
    }

    private static BigDecimal soma(java.util.stream.Stream<BigDecimal> valores) {
        return Dinheiro.arredondar(valores.reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static String descreverPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null && fim == null) {
            return "todo o histórico";
        }
        return (inicio == null ? "início" : Datas.formatar(inicio)) + " a "
                + (fim == null ? "hoje" : Datas.formatar(fim));
    }
}
