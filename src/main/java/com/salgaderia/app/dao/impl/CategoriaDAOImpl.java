package com.salgaderia.app.dao.impl;
import com.salgaderia.app.dao.CategoriaDAO;
import com.salgaderia.app.model.Categoria;
import java.sql.*;
import java.util.*;
public class CategoriaDAOImpl implements CategoriaDAO {
    @Override public long inserir(Connection c, Categoria categoria) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO categorias(nome,descricao) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoria.getNome()); ps.setString(2, categoria.getDescricao()); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (!rs.next()) throw new SQLException("ID não retornado"); return rs.getLong(1); }
        }
    }
    @Override public boolean atualizar(Connection c, Categoria categoria) throws SQLException {
        if (categoria.getId() == null) throw new IllegalArgumentException("ID obrigatório");
        try (PreparedStatement ps = c.prepareStatement("UPDATE categorias SET nome=?,descricao=? WHERE id=?")) {
            ps.setString(1, categoria.getNome()); ps.setString(2, categoria.getDescricao()); ps.setLong(3, categoria.getId()); return ps.executeUpdate() == 1;
        }
    }
    @Override public boolean excluir(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM categorias WHERE id=?")) { ps.setLong(1,id); return ps.executeUpdate() == 1; }
    }
    @Override public Optional<Categoria> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id,nome,descricao FROM categorias WHERE id=?")) {
            ps.setLong(1,id); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }
    @Override public long obterOuCriarAtiva(Connection c, String nome) throws SQLException {
        try (var ps = c.prepareStatement("INSERT INTO categorias(nome) VALUES (?) ON DUPLICATE KEY UPDATE ativa=TRUE")) {
            ps.setString(1, nome); ps.executeUpdate();
        }
        try (var ps = c.prepareStatement("SELECT id FROM categorias WHERE nome=? FOR UPDATE")) {
            ps.setString(1, nome);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Categoria não encontrada após cadastro.");
                return rs.getLong(1);
            }
        }
    }
    @Override public List<Categoria> listar(Connection c) throws SQLException { return listar(c, false); }
    @Override public List<Categoria> listarAtivas(Connection c) throws SQLException { return listar(c, true); }
    private List<Categoria> listar(Connection c, boolean somenteAtivas) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id,nome,descricao FROM categorias" + (somenteAtivas ? " WHERE ativa" : "") + " ORDER BY nome,id"); ResultSet rs = ps.executeQuery()) {
            List<Categoria> resultado = new ArrayList<>(); while (rs.next()) resultado.add(mapear(rs)); return resultado;
        }
    }
    private Categoria mapear(ResultSet rs) throws SQLException { return new Categoria(rs.getLong("id"),rs.getString("nome"),rs.getString("descricao")); }
}
