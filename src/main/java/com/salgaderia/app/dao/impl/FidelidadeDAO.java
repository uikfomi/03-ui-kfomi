package com.salgaderia.app.dao.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FidelidadeDAO {
    public record Lote(long id, long saldo) { }

    public Set<Long> favoritos(Connection c, long cliente) throws SQLException {
        try (var ps = c.prepareStatement("SELECT produto_id FROM cliente_favoritos WHERE cliente_id=?")) {
            ps.setLong(1, cliente);
            try (var rs = ps.executeQuery()) {
                Set<Long> ids = new LinkedHashSet<>();
                while (rs.next()) ids.add(rs.getLong(1));
                return ids;
            }
        }
    }
    public void definirFavorito(Connection c, long cliente, long produto, boolean favorito) throws SQLException {
        String sql = favorito ? "INSERT INTO cliente_favoritos(cliente_id,produto_id) VALUES (?,?) ON DUPLICATE KEY UPDATE produto_id=produto_id"
                : "DELETE FROM cliente_favoritos WHERE cliente_id=? AND produto_id=?";
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, cliente); ps.setLong(2, produto); ps.executeUpdate();
        }
    }
    public long saldo(Connection c, long cliente) throws SQLException {
        try (var ps = c.prepareStatement("SELECT COALESCE(SUM(pontos_disponiveis),0) FROM cliente_pontos_lotes WHERE cliente_id=? AND expira_em>NOW()")) {
            ps.setLong(1, cliente);
            try (var rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }
    public List<Lote> lotesParaResgate(Connection c, long cliente) throws SQLException {
        try (var ps = c.prepareStatement("""
                SELECT id,pontos_disponiveis FROM cliente_pontos_lotes
                WHERE cliente_id=? AND expira_em>NOW() AND pontos_disponiveis>0 ORDER BY expira_em,id FOR UPDATE
                """)) {
            ps.setLong(1, cliente);
            try (var rs = ps.executeQuery()) {
                List<Lote> lotes = new ArrayList<>();
                while (rs.next()) lotes.add(new Lote(rs.getLong(1), rs.getLong(2)));
                return lotes;
            }
        }
    }
    public void consumir(Connection c, long pedido, long lote, int pontos) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE cliente_pontos_lotes SET pontos_disponiveis=pontos_disponiveis-? WHERE id=? AND pontos_disponiveis>=? AND expira_em>NOW()")) {
            ps.setInt(1, pontos); ps.setLong(2, lote); ps.setInt(3, pontos);
            if (ps.executeUpdate()!=1) throw new IllegalArgumentException("O saldo de pontos mudou. Confira o resgate novamente.");
        }
        try (var ps = c.prepareStatement("INSERT INTO cliente_pontos_resgates(pedido_id,lote_id,pontos) VALUES (?,?,?)")) {
            ps.setLong(1, pedido); ps.setLong(2, lote); ps.setInt(3, pontos); ps.executeUpdate();
        }
    }
    public void creditarCompraPaga(Connection c, long pedido) throws SQLException {
        try (var ps = c.prepareStatement("""
                INSERT INTO cliente_pontos_lotes (cliente_id,pedido_id,pontos_disponiveis,expira_em)
                SELECT p.contato_id,p.id,FLOOR(ROUND(SUM(i.quantidade*i.preco_unitario)+p.taxa_entrega-p.desconto,2)),
                       DATE_ADD(p.pago_em, INTERVAL 60 DAY)
                FROM pedidos p JOIN pedido_itens i ON i.pedido_id=p.id JOIN contatos ct ON ct.id=p.contato_id AND ct.tipo='CLIENTE'
                WHERE p.id=? AND p.status_pagamento='PAGO' AND p.pago_em IS NOT NULL AND p.status<>'CANCELADO'
                      AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO')
                GROUP BY p.id,p.contato_id,p.pago_em,p.taxa_entrega,p.desconto
                HAVING FLOOR(ROUND(SUM(i.quantidade*i.preco_unitario)+p.taxa_entrega-p.desconto,2))>0
                ON DUPLICATE KEY UPDATE pontos_disponiveis=cliente_pontos_lotes.pontos_disponiveis
                """)) {
            ps.setLong(1, pedido); ps.executeUpdate();
        }
    }
    public void estornar(Connection c, long pedido) throws SQLException {
        try (var ps = c.prepareStatement("""
                UPDATE cliente_pontos_lotes l JOIN cliente_pontos_resgates r ON r.lote_id=l.id
                SET l.pontos_disponiveis=l.pontos_disponiveis+r.pontos WHERE r.pedido_id=? AND l.expira_em>NOW()
                """)) { ps.setLong(1, pedido); ps.executeUpdate(); }
        try (var ps = c.prepareStatement("DELETE FROM cliente_pontos_resgates WHERE pedido_id=?")) {
            ps.setLong(1, pedido); ps.executeUpdate();
        }
        try (var ps = c.prepareStatement("UPDATE cliente_pontos_lotes SET pontos_disponiveis=0 WHERE pedido_id=?")) {
            ps.setLong(1, pedido); ps.executeUpdate();
        }
    }
}
