package com.salgaderia.app.model;

import com.salgaderia.app.exception.EstoqueInsuficienteException;
import com.salgaderia.app.exception.ProdutoIndisponivelException;
import java.math.BigDecimal;

public class Produto {
    private Long id;
    private Categoria categoria;
    private String nome;
    private String descricao;
    private BigDecimal precoVenda;
    private BigDecimal custoProducao;
    private BigDecimal estoqueAtual;
    private BigDecimal estoqueMinimo;
    private String urlImagem;
    private boolean disponivelVenda = true;
    private java.util.List<ProdutoOpcional> opcionais = java.util.List.of();
    private FichaVitrine fichaVitrine = FichaVitrine.VAZIA;
    public Produto(Long id, Categoria categoria, String nome, String descricao, BigDecimal precoVenda,
                   BigDecimal custoProducao, BigDecimal estoqueAtual, BigDecimal estoqueMinimo, String urlImagem) {
        setId(id); setCategoria(categoria); setNome(nome); setDescricao(descricao); setPrecoVenda(precoVenda);
        setCustoProducao(custoProducao); setEstoqueAtual(estoqueAtual); setEstoqueMinimo(estoqueMinimo); setUrlImagem(urlImagem);
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = Validacao.id(id); }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = Validacao.texto(nome, "nome"); }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(BigDecimal valor) { precoVenda = Validacao.naoNegativo(valor, "preço de venda"); }
    public BigDecimal getCustoProducao() { return custoProducao; }
    public void setCustoProducao(BigDecimal valor) { custoProducao = Validacao.naoNegativo(valor, "custo de produção"); }
    public BigDecimal getEstoqueAtual() { return estoqueAtual; }
    public void setEstoqueAtual(BigDecimal valor) { estoqueAtual = Validacao.naoNegativo(valor, "estoque atual"); }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(BigDecimal valor) { estoqueMinimo = Validacao.naoNegativo(valor, "estoque mínimo"); }
    public boolean isDisponivelVenda() { return disponivelVenda; }
    public void setDisponivelVenda(boolean disponivel) { this.disponivelVenda = disponivel; }
    public void exigirDisponivelParaVenda() {
        if (!disponivelVenda) throw new ProdutoIndisponivelException(nome);
    }
    public String getUrlImagem() { return urlImagem; }
    public void setUrlImagem(String urlImagem) { this.urlImagem = urlImagem; }
    public FichaVitrine getFichaVitrine() { return fichaVitrine; }
    public void setFichaVitrine(FichaVitrine ficha) { this.fichaVitrine = Validacao.obrigatorio(ficha, "ficha da vitrine"); }
    public java.util.List<ProdutoOpcional> getOpcionais() { return opcionais; }
    public void setOpcionais(java.util.List<ProdutoOpcional> opcoes) { opcionais = java.util.List.copyOf(opcoes); }
    public BigDecimal calcularLucroUnitario() { return precoVenda.subtract(custoProducao); }
    public boolean isEstoqueBaixo() { return estoqueAtual.compareTo(estoqueMinimo) <= 0; }
    public void adicionarEstoque(BigDecimal quantidade) { estoqueAtual = estoqueAtual.add(Validacao.positivo(quantidade, "quantidade")); }
    public void removerEstoque(BigDecimal quantidade) {
        Validacao.positivo(quantidade, "quantidade");
        if (estoqueAtual.compareTo(quantidade) < 0) throw new EstoqueInsuficienteException(nome, estoqueAtual, quantidade);
        estoqueAtual = estoqueAtual.subtract(quantidade);
    }
}
