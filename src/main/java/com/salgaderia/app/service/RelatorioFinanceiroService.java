package com.salgaderia.app.service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public interface RelatorioFinanceiroService {
    /** Intervalo semiaberto: início inclusivo, fim exclusivo; vendas filtradas pela criação do pedido entregue. */
    Relatorio calcular(LocalDateTime inicio,LocalDateTime fim);
    java.util.Map<String,BigDecimal> produtosMaisVendidos(LocalDateTime inicio,LocalDateTime fim);
    java.util.Map<String,BigDecimal> receitaPorCategoria(LocalDateTime inicio,LocalDateTime fim);
    java.util.List<com.salgaderia.app.model.RepasseEntregador> repasses(LocalDateTime inicio, LocalDateTime fim);
    void exportarCsv(Relatorio relatorio, java.nio.file.Path destino) throws java.io.IOException;
    record Relatorio(BigDecimal faturamentoBruto,BigDecimal faturamentoAposDescontos,BigDecimal custoTotalProducao,BigDecimal perdas,BigDecimal lucroLiquidoReal) { }
}
