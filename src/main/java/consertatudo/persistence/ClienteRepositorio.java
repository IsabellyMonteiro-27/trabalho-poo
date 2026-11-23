package consertatudo.persistence;

import consertatudo.model.Cliente;

import java.nio.file.Path;
import java.util.List;

public class ClienteRepositorio extends RepositorioCsv<Cliente> {

    public ClienteRepositorio(Path arquivo) {
        super(arquivo);
    }

    @Override
    protected String cabecalho() {
        return "id;nome;telefone;pontos";
    }

    @Override
    protected Cliente deCampos(List<String> c) {
        return new Cliente(Integer.parseInt(c.get(0)), c.get(1), c.get(2), Integer.parseInt(c.get(3)));
    }

    @Override
    protected List<String> paraCampos(Cliente c) {
        return List.of(String.valueOf(c.getId()), c.getNome(), c.getTelefone(), String.valueOf(c.getPontos()));
    }

    @Override
    protected int idDe(Cliente c) {
        return c.getId();
    }

    @Override
    protected String nomeEntidade() {
        return "Cliente";
    }
}
