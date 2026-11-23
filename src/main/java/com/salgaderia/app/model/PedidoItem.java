package com.salgaderia.app.model;

import java.math.BigDecimal;
import java.util.Set;

/** Item imutável: o preço é registrado no momento da inclusão no pedido. */
public final class PedidoItem {
    private final Long id;
    private final Produto produto;
    private final BigDecimal quantidade;
    private final BigDecimal precoUnitario;
    private final String observacaoPersonalizada;
    private final PersonalizacaoItem personalizacao;
    private final Set<Long> opcionaisIds;
    private final String adicionaisResumo;
    public PedidoItem(Long id, Produto produto, BigDecimal quantidade, BigDecimal precoUnitario) {
        this(id, produto, quantidade, precoUnitario, null, null);
    }
    public PedidoItem(Long id, Produto produto, BigDecimal quantidade, BigDecimal precoUnitario,
                      String observacaoPersonalizada, PersonalizacaoItem personalizacao) {
        this(id, produto, quantidade, precoUnitario, observacaoPersonalizada, personalizacao, Set.of(), null);
    }
    public PedidoItem(Long id, Produto produto, BigDecimal quantidade, BigDecimal precoUnitario,
                      String observacaoPersonalizada, PersonalizacaoItem personalizacao, Set<Long> opcionaisIds, String adicionaisResumo) {
        this.id = Validacao.id(id);
        this.produto = Validacao.obrigatorio(produto, "produto");
        this.quantidade = Validacao.positivo(quantidade, "quantidade");
        this.precoUnitario = Validacao.naoNegativo(precoUnitario, "preço unitário");
        if (observacaoPersonalizada != null && observacaoPersonalizada.length() > 512)
            throw new IllegalArgumentException("Descrição personalizada muito longa.");
        this.observacaoPersonalizada = observacaoPersonalizada;
        this.personalizacao = personalizacao;
        this.opcionaisIds = Set.copyOf(opcionaisIds);
        this.adicionaisResumo = adicionaisResumo;
    }
    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public String getObservacaoPersonalizada() { return observacaoPersonalizada; }
    public PersonalizacaoItem getPersonalizacao() { return personalizacao; }
    public Set<Long> getOpcionaisIds() { return opcionaisIds; }
    public String getAdicionaisResumo() { return adicionaisResumo; }
    public String detalhesPreparo() {
        String adicionais = adicionaisResumo == null ? "" : adicionaisResumo;
        if (personalizacao != null || observacaoPersonalizada == null) return adicionais;
        return adicionais + (adicionais.isEmpty() ? "" : "\n") + "Observação: " + observacaoPersonalizada;
    }
    public PedidoItem comProdutoAtual(Produto atual) {
        BigDecimal preco = opcionaisIds.isEmpty() ? atual.getPrecoVenda() : precoUnitario;
        return new PedidoItem(null, atual, quantidade, preco, observacaoPersonalizada, personalizacao, opcionaisIds, adicionaisResumo);
    }
    public BigDecimal calcularSubtotal() { return precoUnitario.multiply(quantidade); }
}
