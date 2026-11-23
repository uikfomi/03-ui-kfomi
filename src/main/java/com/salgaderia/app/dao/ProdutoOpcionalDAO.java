package com.salgaderia.app.dao;

import com.salgaderia.app.model.ProdutoOpcional;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface ProdutoOpcionalDAO {
    Map<Long, List<ProdutoOpcional>> listarPorProduto(Connection c) throws SQLException;
    List<ProdutoOpcional> listar(Connection c, long produtoId, boolean bloquear) throws SQLException;
    void salvar(Connection c, long produtoId, List<ProdutoOpcional> opcoes) throws SQLException;
}
