package consertatudo.service;

import consertatudo.exception.RegraNegocioException;
import consertatudo.model.Cliente;
import consertatudo.persistence.ClienteRepositorio;
import consertatudo.persistence.OrdemRepositorio;

import java.util.List;

public class ClienteService {

    private final ClienteRepositorio clientes;
    private final OrdemRepositorio ordens;

    public ClienteService(ClienteRepositorio clientes, OrdemRepositorio ordens) {
        this.clientes = clientes;
        this.ordens = ordens;
    }

    public Cliente cadastrar(String nome, String telefone) {
        Cliente cliente = new Cliente(clientes.proximoId(), nome, telefone);
        clientes.adicionar(cliente);
        clientes.salvar();
        return cliente;
    }

    public Cliente editar(int id, String novoNome, String novoTelefone) {
        Cliente cliente = clientes.exigirPorId(id);
        if (novoNome != null && !novoNome.isBlank()) {
            cliente.setNome(novoNome);
        }
        if (novoTelefone != null && !novoTelefone.isBlank()) {
            cliente.setTelefone(novoTelefone);
        }
        clientes.salvar();
        return cliente;
    }

    /** Clientes com ordens registradas não podem ser removidos (preserva o histórico). */
    public void remover(int id) {
        Cliente cliente = clientes.exigirPorId(id);
        boolean temHistorico = ordens.listar().stream().anyMatch(o -> o.getClienteId() == id);
        if (temHistorico) {
            throw new RegraNegocioException(
                    "Este cliente possui ordens de serviço registradas e não pode ser removido.");
        }
        clientes.remover(cliente);
        clientes.salvar();
    }

    public Cliente exigir(int id) {
        return clientes.exigirPorId(id);
    }

    public List<Cliente> listar() {
        return clientes.listar();
    }

    public String nomeDe(int id) {
        return clientes.buscarPorId(id).map(Cliente::getNome).orElse("(removido)");
    }
}
