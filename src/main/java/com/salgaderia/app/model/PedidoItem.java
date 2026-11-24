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
    private final Integer pontosResgate;
    private final BigDecimal valorResgate;
    public static PedidoItem resgate(Produto produto) {
        if (produto.getPontosResgate() == null) throw new IllegalArgumentException("Produto não habilitado para resgate.");
        return new PedidoItem(null, produto, BigDecimal.ONE, BigDecimal.ZERO, null, null,
                Set.of(), null, produto.getPontosResgate(), produto.getPrecoVenda());
    }
    public PedidoItem(Long id, Produto produto, BigDecimal quantidade, BigDecimal precoUnitario) {
        this(id, produto, quantidade, precoUnitario, null, null);
    }
    public PedidoItem(Long id, Produto produto, BigDecimal quantidade, BigDecimal precoUnitario,
                      String observacaoPersonalizada, PersonalizacaoItem personalizacao) {
        this(id, produto, quantidade, precoUnitario, observacaoPersonalizada, personalizacao, Set.of(), null);
    }
    public PedidoItem(Long id, Produto produto, BigDecimal quantidade, BigDecimal precoUnitario,
                      String observacaoPersonalizada, PersonalizacaoItem personalizacao, Set<Long> opcionaisIds, String adicionaisResumo) {
        this(id, produto, quantidade, precoUnitario, observacaoPersonalizada, personalizacao, opcionaisIds, adicionaisResumo, null, BigDecimal.ZERO);
    }
    public PedidoItem(Long id, Produto produto, BigDecimal quantidade, BigDecimal precoUnitario,
                      String observacaoPersonalizada, PersonalizacaoItem personalizacao, Set<Long> opcionaisIds, String adicionaisResumo,
                      Integer pontosResgate, BigDecimal valorResgate) {
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
        this.pontosResgate = pontosResgate;
        this.valorResgate = Validacao.naoNegativo(valorResgate, "valor do resgate");
        if (pontosResgate != null && (pontosResgate <= 0 || quantidade.compareTo(BigDecimal.ONE) != 0 || precoUnitario.signum() != 0
                || observacaoPersonalizada != null || personalizacao != null || !opcionaisIds.isEmpty() || adicionaisResumo != null))
            throw new IllegalArgumentException("Um resgate é uma unidade sem personalização ou cobrança.");
    }
    public boolean isResgate() { return pontosResgate != null; }
    public Integer getPontosResgate() { return pontosResgate; }
    public BigDecimal getValorResgate() { return valorResgate; }
    public String descricaoResgate() { return "Resgate de " + pontosResgate + " pontos";
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
        if (isResgate()) return descricaoResgate();
        String adicionais = adicionaisResumo == null ? "" : adicionaisResumo;
        if (personalizacao != null || observacaoPersonalizada == null) return adicionais;
        return adicionais + (adicionais.isEmpty() ? "" : "\n") + "Observação: " + observacaoPersonalizada;
    }
    public PedidoItem comProdutoAtual(Produto atual) {
        if (isResgate()) return this;
        BigDecimal preco = opcionaisIds.isEmpty() ? atual.getPrecoVenda() : precoUnitario;
        return new PedidoItem(null, atual, quantidade, preco, observacaoPersonalizada, personalizacao, opcionaisIds, adicionaisResumo);
    }
    public BigDecimal calcularSubtotal() { return precoUnitario.multiply(quantidade); }
}
