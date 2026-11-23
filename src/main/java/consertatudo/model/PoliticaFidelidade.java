package consertatudo.model;

import java.math.BigDecimal;

/**
 * Regras do programa de fidelidade, concentradas em um só lugar:
 * - 10% de desconto por ordem concluída anteriormente, limitado a 40%;
 * - 1 ponto a cada R$ 10,00 pagos;
 * - 100 pontos podem ser trocados por R$ 20,00 de desconto.
 */
public final class PoliticaFidelidade {

    public static final int DESCONTO_POR_ORDEM = 10;
    public static final int DESCONTO_MAXIMO = 40;
    public static final int VALOR_POR_PONTO = 10;
    public static final int PONTOS_PARA_RESGATE = 100;
    public static final BigDecimal DESCONTO_RESGATE = new BigDecimal("20.00");

    private PoliticaFidelidade() {
    }

    /** Percentual de desconto (0 a 40) para quem já tem N ordens concluídas. */
    public static int percentualDesconto(int ordensConcluidas) {
        return Math.min(Math.max(ordensConcluidas, 0) * DESCONTO_POR_ORDEM, DESCONTO_MAXIMO);
    }

    /** Pontos ganhos por um valor pago (parte inteira de valor / 10). */
    public static int pontosGanhos(BigDecimal valorPago) {
        return valorPago.divideToIntegralValue(BigDecimal.valueOf(VALOR_POR_PONTO)).intValue();
    }
}
