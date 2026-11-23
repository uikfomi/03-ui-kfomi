package com.salgaderia.app.service;
import com.salgaderia.app.model.Produto;
import java.math.BigDecimal;
import java.util.*;
public interface ProdutoEstoqueService {
    java.util.List<com.salgaderia.app.model.Categoria> listarCategorias();
    List<com.salgaderia.app.model.Categoria> listarCategoriasAtivas();
    void salvarCatalogo(Produto produto, String categoria, BigDecimal ajuste);
    long cadastrar(Produto produto);
    boolean atualizar(Produto produto);
    boolean atualizar(Produto produto, BigDecimal ajustePara);
    boolean alternarDisponibilidade(long id);
    boolean excluir(long id);
    Optional<Produto> buscarPorId(long id);
    List<Produto> listar();
    List<Produto> listarEstoqueBaixo();
    void entradaLote(long produtoId,long fornecedorId,BigDecimal quantidade,String observacao);
    void registrarPerda(long produtoId,BigDecimal quantidade,String observacao);
}
