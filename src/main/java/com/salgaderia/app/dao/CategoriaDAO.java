package com.salgaderia.app.dao;
import com.salgaderia.app.model.Categoria;
import java.sql.*;
import java.util.*;
public interface CategoriaDAO {
    long inserir(Connection conexao, Categoria categoria) throws SQLException;
    boolean atualizar(Connection conexao, Categoria categoria) throws SQLException;
    boolean excluir(Connection conexao, long id) throws SQLException;
    Optional<Categoria> buscarPorId(Connection conexao, long id) throws SQLException;
    List<Categoria> listarAtivas(Connection conexao) throws SQLException;
    long obterOuCriarAtiva(Connection conexao, String nome) throws SQLException;
    List<Categoria> listar(Connection conexao) throws SQLException;
}
