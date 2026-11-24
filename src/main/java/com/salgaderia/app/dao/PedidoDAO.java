package com.salgaderia.app.dao;
import com.salgaderia.app.model.Pedido;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;
import com.salgaderia.app.model.enums.StatusPedido;
import com.salgaderia.app.model.enums.TipoEntrega;
public interface PedidoDAO {
    java.util.List<Pedido> listarDoCliente(Connection c, com.salgaderia.app.model.UsuarioCliente cliente) throws SQLException;
    java.util.List<com.salgaderia.app.model.PedidoFila> listarFila(Connection c) throws SQLException;
    void registrarPdv(Connection c,long id,com.salgaderia.app.model.enums.FormaPagamento forma,BigDecimal recebido,BigDecimal troco) throws SQLException;
    java.util.Map<String,BigDecimal> produtosMaisVendidos(Connection c,java.time.LocalDateTime inicio,java.time.LocalDateTime fim) throws SQLException;
    java.util.Map<String,BigDecimal> receitaPorCategoria(Connection c,java.time.LocalDateTime inicio,java.time.LocalDateTime fim) throws SQLException;
    long inserir(Connection c, Pedido pedido, Map<Long,BigDecimal> custos, Map<Long,BigDecimal> baixas) throws SQLException;
    record Fluxo(StatusPedido status, TipoEntrega modalidade) { }
    Fluxo bloquearFluxo(Connection c,long id) throws SQLException;
    Fluxo bloquearFluxoDoCliente(Connection c,long id,long usuarioId) throws SQLException;
    void registrarSaidaEntrega(Connection c,long id,long entregadorId,String entregador) throws SQLException;
    com.salgaderia.app.model.enums.StatusPagamento statusPagamento(Connection c,long id) throws SQLException;
    void prepararCartaoApp(Connection c,long id) throws SQLException;
    void confirmarCartaoApp(Connection c,long id) throws SQLException;
    void registrarSemCobranca(Connection c,long id) throws SQLException;
    boolean cartaoAppPago(Connection c,long id) throws SQLException;
    Map<Long,BigDecimal> quantidadesBaixadas(Connection c,long id) throws SQLException;
    void atualizarStatus(Connection c,long id,com.salgaderia.app.model.enums.StatusPedido status) throws SQLException;
    boolean possuiPedidoEntregue(Connection c,long usuarioId,long produtoId) throws SQLException;
    BigDecimal[] somarVendasEntregues(Connection c,LocalDateTime inicio,LocalDateTime fim) throws SQLException;
    long contarEntreguesDoCliente(Connection c, long clienteId) throws SQLException;
    void registrarDesconto(Connection c, long pedidoId, com.salgaderia.app.service.desconto.ResultadoDesconto desconto) throws SQLException;
    long contarFilaCozinha(Connection c) throws SQLException;
}
