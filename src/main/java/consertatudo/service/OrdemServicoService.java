package consertatudo.service;

import consertatudo.exception.RegraNegocioException;
import consertatudo.model.OrdemFactory;
import consertatudo.model.OrdemServico;
import consertatudo.model.PoliticaFidelidade;
import consertatudo.model.StatusOrdem;
import consertatudo.model.Tecnico;
import consertatudo.model.TipoServico;
import consertatudo.persistence.ClienteRepositorio;
import consertatudo.persistence.OrdemRepositorio;
import consertatudo.persistence.TecnicoRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Regras das ordens de serviço: criação, alocação de técnicos (com prioridade
 * para emergências), bloqueio de agenda e mudanças de status.
 */
public class OrdemServicoService {

    private final ClienteRepositorio clientes;
    private final TecnicoRepositorio tecnicos;
    private final OrdemRepositorio ordens;
    private final Clock relogio;

    public OrdemServicoService(ClienteRepositorio clientes, TecnicoRepositorio tecnicos,
                               OrdemRepositorio ordens, Clock relogio) {
        this.clientes = clientes;
        this.tecnicos = tecnicos;
        this.ordens = ordens;
        this.relogio = relogio;
    }

    // ---------- criação ----------

    public OrdemServico criar(int clienteId, TipoServico tipo, String descricao, LocalDate prazo,
                              boolean emergencia) {
        clientes.exigirPorId(clienteId);
        if (tipo == null) {
            throw new RegraNegocioException("O tipo de serviço é obrigatório.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new RegraNegocioException("A descrição do problema é obrigatória.");
        }
        if (prazo == null || prazo.isBefore(hoje())) {
            throw new RegraNegocioException("O prazo de conclusão não pode ser anterior a hoje.");
        }
        int desconto = PoliticaFidelidade.percentualDesconto(contarConcluidas(clienteId));
        OrdemServico ordem = OrdemFactory.nova(emergencia, ordens.proximoId(), clienteId, tipo,
                descricao, prazo, hoje(), desconto);
        ordens.adicionar(ordem);
        tentarAlocar(ordem);
        ordens.salvar();
        return ordem;
    }

    /** Quantas ordens concluídas o cliente já tem (base do desconto de fidelidade). */
    public int contarConcluidas(int clienteId) {
        return (int) ordens.listar().stream()
                .filter(o -> o.getClienteId() == clienteId && o.getStatus() == StatusOrdem.CONCLUIDA)
                .count();
    }

    // ---------- alocação ----------

    /**
     * Aloca técnicos às ordens pendentes sem técnico. A fila é ordenada por
     * prioridade: emergências primeiro, depois menor prazo, depois mais antigas.
     *
     * @return quantas ordens foram alocadas
     */
    public int alocarPendentes() {
        int alocadas = 0;
        for (OrdemServico ordem : filaDeAlocacao()) {
            if (tentarAlocar(ordem)) {
                alocadas++;
            }
        }
        if (alocadas > 0) {
            ordens.salvar();
        }
        return alocadas;
    }

    public List<OrdemServico> filaDeAlocacao() {
        return ordens.listar().stream()
                .filter(o -> o.getStatus() == StatusOrdem.PENDENTE && o.getTecnicoId() == null)
                .sorted(Comparator
                        .comparing((OrdemServico o) -> !o.isEmergencia())
                        .thenComparing(OrdemServico::getPrazo)
                        .thenComparingInt(OrdemServico::getId))
                .collect(Collectors.toList());
    }

    private boolean tentarAlocar(OrdemServico ordem) {
        Optional<Tecnico> escolhido = escolherTecnico(ordem);
        escolhido.ifPresent(t -> ordem.atribuirTecnico(t.getId()));
        return escolhido.isPresent();
    }

    /**
     * Técnico elegível: ativo, que atende o tipo e não está alocado em emergência.
     * Entre os elegíveis, vence quem tem menos ordens em aberto; empate favorece
     * o especialista exato e depois o menor ID.
     */
    private Optional<Tecnico> escolherTecnico(OrdemServico ordem) {
        return tecnicos.listar().stream()
                .filter(Tecnico::isAtivo)
                .filter(t -> t.atende(ordem.getTipo()))
                .filter(t -> !temEmergenciaAtiva(t.getId(), ordem))
                .min(Comparator
                        .comparingLong((Tecnico t) -> cargaAtiva(t.getId()))
                        .thenComparing(t -> t.getEspecialidade() == ordem.getTipo() ? 0 : 1)
                        .thenComparingInt(Tecnico::getId));
    }

    public OrdemServico alocarManualmente(int ordemId, int tecnicoId) {
        OrdemServico ordem = ordens.exigirPorId(ordemId);
        Tecnico tecnico = tecnicos.exigirPorId(tecnicoId);
        if (ordem.getStatus() != StatusOrdem.PENDENTE) {
            throw new RegraNegocioException("Só é possível (re)alocar técnico em ordens pendentes.");
        }
        if (!tecnico.isAtivo()) {
            throw new RegraNegocioException("O técnico está inativo.");
        }
        if (!tecnico.atende(ordem.getTipo())) {
            throw new RegraNegocioException("O técnico não é especialista em " + ordem.getTipo().getDescricao() + ".");
        }
        if (temEmergenciaAtiva(tecnicoId, ordem)) {
            throw new RegraNegocioException(
                    "O técnico está alocado em um serviço emergencial e não aceita novos agendamentos.");
        }
        ordem.liberarTecnico();
        ordem.atribuirTecnico(tecnicoId);
        ordens.salvar();
        return ordem;
    }

    /** Verdadeiro se o técnico tem uma emergência em aberto (exceto a ordem informada). */
    public boolean temEmergenciaAtiva(int tecnicoId, OrdemServico ignorar) {
        return ordens.listar().stream().anyMatch(o -> o != ignorar
                && o.isEmergencia()
                && o.getStatus().isAtiva()
                && Objects.equals(o.getTecnicoId(), tecnicoId));
    }

    public long cargaAtiva(int tecnicoId) {
        return ordens.listar().stream()
                .filter(o -> o.getStatus().isAtiva() && Objects.equals(o.getTecnicoId(), tecnicoId))
                .count();
    }

    // ---------- status ----------

    public OrdemServico iniciar(int ordemId) {
        OrdemServico ordem = ordens.exigirPorId(ordemId);
        Integer tecnicoId = ordem.getTecnicoId();
        if (ordem.getStatus() == StatusOrdem.PENDENTE && tecnicoId != null) {
            boolean ocupado = ordens.listar().stream().anyMatch(o -> o != ordem
                    && o.getStatus() == StatusOrdem.EM_ANDAMENTO && Objects.equals(o.getTecnicoId(), tecnicoId));
            if (ocupado) {
                throw new RegraNegocioException("O técnico já está com outra ordem em andamento. Conclua-a antes.");
            }
            if (!ordem.isEmergencia() && temEmergenciaAtiva(tecnicoId, ordem)) {
                throw new RegraNegocioException(
                        "O técnico está alocado em um serviço emergencial, que tem prioridade.");
            }
        }
        ordem.iniciar();
        ordens.salvar();
        return ordem;
    }

    public OrdemServico concluir(int ordemId) {
        OrdemServico ordem = ordens.exigirPorId(ordemId);
        ordem.concluir();
        ordens.salvar();
        alocarPendentes(); // o técnico foi liberado: a fila anda
        return ordem;
    }

    public OrdemServico cancelar(int ordemId) {
        OrdemServico ordem = ordens.exigirPorId(ordemId);
        ordem.cancelar();
        ordens.salvar();
        alocarPendentes();
        return ordem;
    }

    // ---------- consultas ----------

    public OrdemServico exigir(int id) {
        return ordens.exigirPorId(id);
    }

    public List<OrdemServico> listar() {
        return ordens.listar();
    }

    public List<OrdemServico> listarPorStatus(StatusOrdem status) {
        return ordens.listar().stream().filter(o -> o.getStatus() == status).collect(Collectors.toList());
    }

    public List<OrdemServico> listarConcluidasNaoPagas() {
        return ordens.listar().stream()
                .filter(o -> o.getStatus() == StatusOrdem.CONCLUIDA && !o.isPaga())
                .collect(Collectors.toList());
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
