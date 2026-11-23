package consertatudo.service;

import consertatudo.exception.RegraNegocioException;
import consertatudo.model.Tecnico;
import consertatudo.model.TipoServico;
import consertatudo.persistence.OrdemRepositorio;
import consertatudo.persistence.TecnicoRepositorio;

import java.util.List;

public class TecnicoService {

    private final TecnicoRepositorio tecnicos;
    private final OrdemRepositorio ordens;

    public TecnicoService(TecnicoRepositorio tecnicos, OrdemRepositorio ordens) {
        this.tecnicos = tecnicos;
        this.ordens = ordens;
    }

    public Tecnico cadastrar(String nome, String telefone, TipoServico especialidade) {
        Tecnico tecnico = new Tecnico(tecnicos.proximoId(), nome, telefone, especialidade, true);
        tecnicos.adicionar(tecnico);
        tecnicos.salvar();
        return tecnico;
    }

    public Tecnico editar(int id, String novoNome, String novoTelefone, TipoServico novaEspecialidade) {
        Tecnico tecnico = tecnicos.exigirPorId(id);
        if (novaEspecialidade != null && novaEspecialidade != tecnico.getEspecialidade()
                && temOrdemAtiva(id)) {
            throw new RegraNegocioException(
                    "O técnico tem ordens em aberto; conclua ou reatribua antes de mudar a especialidade.");
        }
        if (novoNome != null && !novoNome.isBlank()) {
            tecnico.setNome(novoNome);
        }
        if (novoTelefone != null && !novoTelefone.isBlank()) {
            tecnico.setTelefone(novoTelefone);
        }
        if (novaEspecialidade != null) {
            tecnico.setEspecialidade(novaEspecialidade);
        }
        tecnicos.salvar();
        return tecnico;
    }

    public Tecnico alterarAtivo(int id, boolean ativo) {
        Tecnico tecnico = tecnicos.exigirPorId(id);
        if (!ativo && temOrdemAtiva(id)) {
            throw new RegraNegocioException(
                    "O técnico tem ordens em aberto; conclua ou reatribua antes de desativá-lo.");
        }
        tecnico.setAtivo(ativo);
        tecnicos.salvar();
        return tecnico;
    }

    /** Técnicos que já atenderam alguma ordem não são removidos (use desativar). */
    public void remover(int id) {
        Tecnico tecnico = tecnicos.exigirPorId(id);
        boolean temHistorico = ordens.listar().stream()
                .anyMatch(o -> o.getTecnicoId() != null && o.getTecnicoId() == id);
        if (temHistorico) {
            throw new RegraNegocioException(
                    "Este técnico possui ordens registradas e não pode ser removido. Desative-o.");
        }
        tecnicos.remover(tecnico);
        tecnicos.salvar();
    }

    public Tecnico exigir(int id) {
        return tecnicos.exigirPorId(id);
    }

    public List<Tecnico> listar() {
        return tecnicos.listar();
    }

    public String nomeDe(Integer id) {
        if (id == null) {
            return "(sem técnico)";
        }
        return tecnicos.buscarPorId(id).map(Tecnico::getNome).orElse("(removido)");
    }

    private boolean temOrdemAtiva(int tecnicoId) {
        return ordens.listar().stream()
                .anyMatch(o -> o.getStatus().isAtiva() && o.getTecnicoId() != null && o.getTecnicoId() == tecnicoId);
    }
}
