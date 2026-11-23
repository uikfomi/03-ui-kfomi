package com.salgaderia.app.dao.impl;

import com.salgaderia.app.dao.ProdutoOpcionalDAO;
import com.salgaderia.app.model.ProdutoOpcional;
import java.sql.*;
import java.util.*;

public final class ProdutoOpcionalDAOImpl implements ProdutoOpcionalDAO {
    @Override public Map<Long, List<ProdutoOpcional>> listarPorProduto(Connection c) throws SQLException {
        Map<Long, List<ProdutoOpcional>> resultado = new HashMap<>();
        try (var ps = c.prepareStatement("SELECT * FROM produto_opcionais ORDER BY produto_id, grupo_nome, id");
             var rs = ps.executeQuery()) {
            while (rs.next()) resultado.computeIfAbsent(rs.getLong("produto_id"), id -> new ArrayList<>()).add(mapear(rs));
        }
        return resultado;
    }

    @Override public List<ProdutoOpcional> listar(Connection c, long produtoId, boolean bloquear) throws SQLException {
        List<ProdutoOpcional> resultado = new ArrayList<>();
        try (var ps = c.prepareStatement("SELECT * FROM produto_opcionais WHERE produto_id=? ORDER BY grupo_nome,id" + (bloquear ? " FOR SHARE" : ""))) {
            ps.setLong(1, produtoId);
            try (var rs = ps.executeQuery()) { while (rs.next()) resultado.add(mapear(rs)); }
        }
        return resultado;
    }

    private static ProdutoOpcional mapear(ResultSet rs) throws SQLException {
        return new ProdutoOpcional(rs.getLong("id"), rs.getString("grupo_nome"), rs.getString("nome"),
                rs.getBigDecimal("preco_adicional"), rs.getInt("gratis_ate_limite"), rs.getInt("maximo_escolhas"));
    }

    @Override public void salvar(Connection c, long produtoId, List<ProdutoOpcional> opcoes) throws SQLException {
        record Nome(String grupo, String opcao) { }
        Set<Nome> mantidos = new HashSet<>();
        for (var o : opcoes) mantidos.add(new Nome(o.grupoNome(), o.nome()));
        Map<Nome, Long> atuais = new HashMap<>();
        try (var apagar = c.prepareStatement("DELETE FROM produto_opcionais WHERE id=?");
             var atualizar = c.prepareStatement("UPDATE produto_opcionais SET preco_adicional=?,gratis_ate_limite=?,maximo_escolhas=? WHERE id=?");
             var inserir = c.prepareStatement("""
                INSERT INTO produto_opcionais(preco_adicional,gratis_ate_limite,maximo_escolhas,produto_id,grupo_nome,nome)
                VALUES (?,?,?,?,?,?)
                """)) {
            for (var o : listar(c, produtoId, true)) {
                var nome = new Nome(o.grupoNome(), o.nome());
                if (mantidos.contains(nome)) atuais.put(nome, o.id());
                else { apagar.setLong(1, o.id()); apagar.addBatch(); }
            }
            apagar.executeBatch();
            for (var o : opcoes) {
                Long id = atuais.get(new Nome(o.grupoNome(), o.nome()));
                var ps = id == null ? inserir : atualizar;
                ps.setBigDecimal(1, o.precoAdicional()); ps.setInt(2, o.gratisAteLimite()); ps.setInt(3, o.maximoEscolhas());
                if (id == null) {
                    ps.setLong(4, produtoId); ps.setString(5, o.grupoNome()); ps.setString(6, o.nome());
                } else ps.setLong(4, id);
                ps.addBatch();
            }
            atualizar.executeBatch(); inserir.executeBatch();
        }
    }
}
