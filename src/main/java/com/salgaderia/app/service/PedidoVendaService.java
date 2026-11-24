package com.salgaderia.app.service;
import com.salgaderia.app.model.Pedido;
public interface PedidoVendaService {
    java.util.List<Pedido> listarDoCliente(com.salgaderia.app.model.UsuarioCliente usuario);
    java.util.List<com.salgaderia.app.model.PedidoFila> listarFila();
    long finalizarPdv(Pedido pedido,com.salgaderia.app.model.enums.FormaPagamento forma,java.math.BigDecimal recebido);
    /** Compartilhado pelas vendas de PDV e vitrine; grava e baixa estoque atomicamente. */
    long finalizar(Pedido pedido);
    com.salgaderia.app.service.desconto.ResultadoDesconto simular(Pedido pedido, String codigo);
    long finalizar(Pedido pedido, String codigo, com.salgaderia.app.service.desconto.ResultadoDesconto esperado);
    long finalizarPdv(Pedido pedido, com.salgaderia.app.model.enums.FormaPagamento forma, java.math.BigDecimal recebido,
                      String codigo, com.salgaderia.app.service.desconto.ResultadoDesconto esperado);
    void transicionar(long pedidoId, com.salgaderia.app.model.enums.StatusPedido destino);
    String despachar(long pedidoId, long entregadorId);
    void cancelarDoCliente(com.salgaderia.app.model.UsuarioCliente cliente, long pedidoId);
    long contarFilaCozinha();
    java.util.List<com.salgaderia.app.model.PedidoItem> repetirUltimoPedido(com.salgaderia.app.model.UsuarioCliente usuario);
}
