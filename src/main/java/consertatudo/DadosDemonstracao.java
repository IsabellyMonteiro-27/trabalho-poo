package consertatudo;

import consertatudo.exception.RegraNegocioException;
import consertatudo.model.Cliente;
import consertatudo.model.FormaPagamento;
import consertatudo.model.OrdemServico;
import consertatudo.model.TipoServico;

import java.time.LocalDate;

/**
 * Carrega um cenário de exemplo para demonstrar o sistema na apresentação:
 * cliente com histórico (desconto e pontos), técnico bloqueado por emergência
 * e uma ordem normal esperando na fila.
 */
public final class DadosDemonstracao {

    private DadosDemonstracao() {
    }

    public static void carregar(Sistema s, LocalDate hoje) {
        if (!s.clientes().listar().isEmpty() || !s.tecnicos().listar().isEmpty()
                || !s.ordens().listar().isEmpty()) {
            throw new RegraNegocioException("Os dados de demonstração só podem ser carregados com o sistema vazio.");
        }
        Cliente ana = s.clientes().cadastrar("Ana Souza", "(86) 99999-1111");
        Cliente bruno = s.clientes().cadastrar("Bruno Lima", "(86) 98888-2222");
        Cliente carla = s.clientes().cadastrar("Carla Mendes", "(86) 97777-3333");

        s.tecnicos().cadastrar("Carlos Eletricista", "(86) 96666-0001", TipoServico.ELETRICA);
        s.tecnicos().cadastrar("Marina Encanadora", "(86) 96666-0002", TipoServico.HIDRAULICA);
        s.tecnicos().cadastrar("João Faz-Tudo", "(86) 96666-0003", TipoServico.GERAL);

        // Histórico da Ana: duas ordens concluídas e pagas (gera fidelidade e pontos)
        OrdemServico o1 = s.ordens().criar(ana.getId(), TipoServico.ELETRICA,
                "Troca de disjuntor do quadro de luz", hoje.plusDays(5), false);
        s.ordens().iniciar(o1.getId());
        s.ordens().concluir(o1.getId());
        s.pagamentos().pagar(o1.getId(), FormaPagamento.PIX, false);

        OrdemServico o2 = s.ordens().criar(ana.getId(), TipoServico.GERAL,
                "Instalação de prateleiras na sala", hoje.plusDays(6), false);
        s.ordens().iniciar(o2.getId());
        s.ordens().concluir(o2.getId());
        s.pagamentos().pagar(o2.getId(), FormaPagamento.DINHEIRO, false);

        // Emergência elétrica do Bruno: ocupa o único eletricista
        s.ordens().criar(bruno.getId(), TipoServico.ELETRICA,
                "Curto-circuito e cheiro de queimado na cozinha", hoje.plusDays(1), true);

        // Ordem normal elétrica da Carla: fica na fila (eletricista bloqueado por emergência)
        s.ordens().criar(carla.getId(), TipoServico.ELETRICA,
                "Instalar tomadas novas no escritório", hoje.plusDays(10), false);

        // Nova ordem da Ana: já nasce com 20% de desconto (2 ordens concluídas)
        s.ordens().criar(ana.getId(), TipoServico.HIDRAULICA,
                "Vazamento na torneira da pia", hoje.plusDays(3), false);
    }
}
