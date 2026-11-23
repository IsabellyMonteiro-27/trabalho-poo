package consertatudo.persistence;

import consertatudo.model.Tecnico;
import consertatudo.model.TipoServico;

import java.nio.file.Path;
import java.util.List;

public class TecnicoRepositorio extends RepositorioCsv<Tecnico> {

    public TecnicoRepositorio(Path arquivo) {
        super(arquivo);
    }

    @Override
    protected String cabecalho() {
        return "id;nome;telefone;especialidade;ativo";
    }

    @Override
    protected Tecnico deCampos(List<String> c) {
        return new Tecnico(Integer.parseInt(c.get(0)), c.get(1), c.get(2),
                TipoServico.valueOf(c.get(3)), Boolean.parseBoolean(c.get(4)));
    }

    @Override
    protected List<String> paraCampos(Tecnico t) {
        return List.of(String.valueOf(t.getId()), t.getNome(), t.getTelefone(),
                t.getEspecialidade().name(), String.valueOf(t.isAtivo()));
    }

    @Override
    protected int idDe(Tecnico t) {
        return t.getId();
    }

    @Override
    protected String nomeEntidade() {
        return "Técnico";
    }
}
