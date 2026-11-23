package consertatudo.persistence;

import consertatudo.model.OrdemFactory;
import consertatudo.model.OrdemServico;
import consertatudo.model.StatusOrdem;
import consertatudo.model.TipoServico;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class OrdemRepositorio extends RepositorioCsv<OrdemServico> {

    public OrdemRepositorio(Path arquivo) {
        super(arquivo);
    }

    @Override
    protected String cabecalho() {
        return "id;categoria;clienteId;tipo;descricao;prazo;criadaEm;status;tecnicoId;descontoFidelidade;paga";
    }

    @Override
    protected OrdemServico deCampos(List<String> c) {
        Integer tecnicoId = c.get(8).isBlank() ? null : Integer.valueOf(c.get(8));
        return OrdemFactory.restaurar(c.get(1), Integer.parseInt(c.get(0)), Integer.parseInt(c.get(2)),
                TipoServico.valueOf(c.get(3)), c.get(4), LocalDate.parse(c.get(5)), LocalDate.parse(c.get(6)),
                Integer.parseInt(c.get(9)), StatusOrdem.valueOf(c.get(7)), tecnicoId,
                Boolean.parseBoolean(c.get(10)));
    }

    @Override
    protected List<String> paraCampos(OrdemServico o) {
        return List.of(String.valueOf(o.getId()), o.getCategoria(), String.valueOf(o.getClienteId()),
                o.getTipo().name(), o.getDescricao(), o.getPrazo().toString(), o.getCriadaEm().toString(),
                o.getStatus().name(), o.getTecnicoId() == null ? "" : String.valueOf(o.getTecnicoId()),
                String.valueOf(o.getDescontoFidelidade()), String.valueOf(o.isPaga()));
    }

    @Override
    protected int idDe(OrdemServico o) {
        return o.getId();
    }

    @Override
    protected String nomeEntidade() {
        return "Ordem de serviço";
    }
}
