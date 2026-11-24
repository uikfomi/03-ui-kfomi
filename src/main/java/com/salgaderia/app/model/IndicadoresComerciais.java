package com.salgaderia.app.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class IndicadoresComerciais {
    private IndicadoresComerciais() { }
    public record ResumoCliente(BigDecimal gasto, BigDecimal economia, boolean topComprador) { }
    public record ClienteRadar(long usuarioId, long clienteId, String nome, String telefone, boolean vip, boolean sumido,
                               LocalDateTime ultimoPedido) { }
    public record CupomReativacao(String codigo, LocalDateTime validoAte) { }
    public record EntradaCaixa(String canal, long recebimentos, BigDecimal total) { }
    public record BairroRanking(String bairro, long pedidos) { }
    public record Fechamento(List<EntradaCaixa> entradas, List<BairroRanking> bairros) { }
}
