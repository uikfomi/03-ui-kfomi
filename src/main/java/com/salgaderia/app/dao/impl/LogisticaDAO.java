package com.salgaderia.app.dao.impl;

import com.salgaderia.app.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LogisticaDAO {
    public List<BairroEntrega> bairros(Connection c, boolean somenteAtivos) throws SQLException {
        try (var ps = c.prepareStatement("SELECT * FROM bairros_entrega" + (somenteAtivos ? " WHERE ativo=TRUE" : "") + " ORDER BY nome");
             var rs = ps.executeQuery()) {
            List<BairroEntrega> bairros = new ArrayList<>();
            while (rs.next()) bairros.add(bairro(rs));
            return bairros;
        }
    }
    public Optional<BairroEntrega> bairroAtivo(Connection c, long id, boolean bloquear) throws SQLException {
        try (var ps = c.prepareStatement("SELECT * FROM bairros_entrega WHERE id=? AND ativo=TRUE" + (bloquear ? " FOR SHARE" : ""))) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) { return rs.next() ? Optional.of(bairro(rs)) : Optional.empty(); }
        }
    }
    private static BairroEntrega bairro(ResultSet rs) throws SQLException {
        return new BairroEntrega(rs.getLong("id"), rs.getString("nome"), rs.getBigDecimal("taxa_entrega"),
                rs.getInt("tempo_estimado_min"), rs.getBoolean("ativo"));
    }
    public void salvar(Connection c, BairroEntrega b) throws SQLException {
        try (var ps = c.prepareStatement(b.id() == null
                ? "INSERT INTO bairros_entrega(nome,taxa_entrega,tempo_estimado_min,ativo) VALUES (?,?,?,?)"
                : "UPDATE bairros_entrega SET nome=?,taxa_entrega=?,tempo_estimado_min=?,ativo=? WHERE id=?")) {
            ps.setString(1, b.nome()); ps.setBigDecimal(2, b.taxaEntrega());
            ps.setInt(3, b.tempoEstimadoMin()); ps.setBoolean(4, b.ativo());
            if (b.id() != null) ps.setLong(5, b.id());
            if (ps.executeUpdate() != 1) throw new SQLException("Bairro não salvo.");
        }
    }
    public List<Entregador> entregadores(Connection c, boolean somenteAtivos) throws SQLException {
        try (var ps = c.prepareStatement("SELECT * FROM entregadores" + (somenteAtivos ? " WHERE ativo=TRUE" : "") + " ORDER BY nome,id");
             var rs = ps.executeQuery()) {
            List<Entregador> entregadores = new ArrayList<>();
            while (rs.next()) entregadores.add(entregador(rs));
            return entregadores;
        }
    }
    public Optional<Entregador> entregadorAtivo(Connection c, long id) throws SQLException {
        try (var ps = c.prepareStatement("SELECT * FROM entregadores WHERE id=? AND ativo=TRUE FOR SHARE")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) { return rs.next() ? Optional.of(entregador(rs)) : Optional.empty(); }
        }
    }
    private static Entregador entregador(ResultSet rs) throws SQLException {
        return new Entregador(rs.getLong("id"), rs.getString("nome"), rs.getString("telefone"),
                rs.getString("placa_veiculo"), rs.getBoolean("ativo"));
    }
    public void salvar(Connection c, Entregador e) throws SQLException {
        try (var ps = c.prepareStatement(e.id() == null
                ? "INSERT INTO entregadores(nome,telefone,placa_veiculo,ativo) VALUES (?,?,?,?)"
                : "UPDATE entregadores SET nome=?,telefone=?,placa_veiculo=?,ativo=? WHERE id=?")) {
            ps.setString(1, e.nome()); ps.setString(2, e.telefone()); ps.setString(3, e.placaVeiculo()); ps.setBoolean(4, e.ativo());
            if (e.id() != null) ps.setLong(5, e.id());
            if (ps.executeUpdate() != 1) throw new SQLException("Entregador não salvo.");
        }
    }
    public List<RepasseEntregador> repasses(Connection c, LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        try (var ps = c.prepareStatement("""
                SELECT e.id,e.nome,COUNT(*),SUM(CASE WHEN p.estrategia_desconto='FRETE' THEN 0 ELSE p.taxa_entrega END)
                FROM pedidos p JOIN entregadores e ON e.id=p.entregador_id
                WHERE p.tipo_entrega='ENTREGA' AND p.status='ENTREGUE'
                  AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO') AND p.criado_em>=? AND p.criado_em<?
                GROUP BY e.id,e.nome ORDER BY e.nome,e.id
                """)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio)); ps.setTimestamp(2, Timestamp.valueOf(fim));
            try (var rs = ps.executeQuery()) {
                List<RepasseEntregador> repasses = new ArrayList<>();
                while (rs.next()) repasses.add(new RepasseEntregador(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getBigDecimal(4)));
                return repasses;
            }
        }
    }
}
