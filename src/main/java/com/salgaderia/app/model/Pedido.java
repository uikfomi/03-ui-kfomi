package com.salgaderia.app.model;

import com.salgaderia.app.model.enums.StatusPedido;
import com.salgaderia.app.model.enums.TipoEntrega;
import com.salgaderia.app.model.enums.FormaPagamento;
import com.salgaderia.app.model.enums.StatusPagamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Raiz do agregado: alterações dos itens e do status passam pelo pedido. */
public class Pedido {
    private Long id;
    private final Usuario usuario;
    private final Cliente cliente;
    private StatusPedido status;
    private final TipoEntrega tipoEntrega;
    private final String enderecoEntrega;
    private final LocalDateTime criadoEm;
    private final List<PedidoItem> itens;
    private BigDecimal desconto = BigDecimal.ZERO;
    private FormaPagamento formaPagamento;
    private LocalDateTime agendadoPara;
    private boolean roletaLiberada;
    private StatusPagamento statusPagamento=StatusPagamento.NA_ENTREGA;
    private BigDecimal taxaEntrega=BigDecimal.ZERO;
    private String brinde;
    private String emailPix,documentoPix;
    private boolean pixDemonstracao;
    private String nomeEntregador;
    private Long bairroId, entregadorId;
    private Integer tempoEstimadoMin;

    public Long getBairroId() { return bairroId; }
    public void setBairroId(Long id) { bairroId = Validacao.id(id); }
    public Long getEntregadorId() { return entregadorId; }
    public void setEntregadorId(Long id) { entregadorId = Validacao.id(id); }
    public Integer getTempoEstimadoMin() { return tempoEstimadoMin; }
    public void setTempoEstimadoMin(Integer minutos) { tempoEstimadoMin = minutos; }

    public Pedido(Long id, Usuario usuario, Cliente cliente, StatusPedido status, TipoEntrega tipoEntrega,
                  String enderecoEntrega, LocalDateTime criadoEm, List<PedidoItem> itens) {
        this.id = Validacao.id(id);
        this.usuario = usuario;
        this.cliente = cliente;
        this.status = Validacao.obrigatorio(status, "status");
        this.tipoEntrega = Validacao.obrigatorio(tipoEntrega, "tipo de entrega");
        this.enderecoEntrega = tipoEntrega == TipoEntrega.ENTREGA
                ? Validacao.texto(enderecoEntrega, "endereço de entrega") : enderecoEntrega;
        this.criadoEm = Validacao.obrigatorio(criadoEm, "data de criação");
        this.itens = new ArrayList<>(Validacao.obrigatorio(itens, "itens"));
        if (this.itens.stream().anyMatch(java.util.Objects::isNull)) throw new IllegalArgumentException("item não pode ser nulo");
    }
    public String getNomeEntregador() { return nomeEntregador; }
    public void setNomeEntregador(String nome) { this.nomeEntregador = nome; }
    public static String validarNomeEntregador(String nome) {
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isEmpty() || limpo.codePointCount(0, limpo.length()) > 100 ||
                limpo.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Informe o nome do entregador (até 100 caracteres).");
        return limpo;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = Validacao.id(id); }
    public Usuario getUsuario() { return usuario; }
    public Cliente getCliente() { return cliente; }
    public StatusPedido getStatus() { return status; }
    public TipoEntrega getTipoEntrega() { return tipoEntrega; }
    public String getEnderecoEntrega() { return enderecoEntrega; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAgendadoPara() { return agendadoPara; }
    public boolean isRoletaLiberada() { return roletaLiberada; }
    public void setRoletaLiberada(boolean roletaLiberada) { this.roletaLiberada = roletaLiberada; }
    public StatusPagamento getStatusPagamento(){return statusPagamento;}
    public void setStatusPagamento(StatusPagamento status){this.statusPagamento=Validacao.obrigatorio(status,"status de pagamento");}
    public BigDecimal getTaxaEntrega(){return taxaEntrega;}
    public void setTaxaEntrega(BigDecimal taxa){this.taxaEntrega=Validacao.naoNegativo(taxa,"taxa de entrega");}
    public String getBrinde(){return brinde;}
    public void setBrinde(String brinde){this.brinde=brinde;}
    public String getEmailPix(){return emailPix;}
    public String getDocumentoPix(){return documentoPix;}
    public void setPagadorPix(String email,String documento){this.emailPix=email;this.documentoPix=documento;}
    public boolean isPixDemonstracao(){return pixDemonstracao;}
    public void setPixDemonstracao(boolean pixDemonstracao){this.pixDemonstracao=pixDemonstracao;}
    /** Pedidos históricos também podem ser carregados com agendamentos já passados. */
    public void setAgendadoPara(LocalDateTime agendadoPara) { this.agendadoPara = agendadoPara; }
    public List<PedidoItem> getItens() { return List.copyOf(itens); }
    public void adicionarItem(PedidoItem item) { exigirPendente(); itens.add(Validacao.obrigatorio(item, "item")); }
    public void removerItem(PedidoItem item) { exigirPendente(); itens.remove(Validacao.obrigatorio(item, "item")); }
    public BigDecimal calcularSubtotal() {
        return itens.stream().map(PedidoItem::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public BigDecimal getDesconto() { return desconto; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    /** Valores nulos representam pedidos históricos anteriores ao registro de pagamento. */
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public void setDesconto(BigDecimal desconto) {
        if (desconto == null || desconto.signum() < 0 || desconto.compareTo(calcularSubtotal().add(taxaEntrega)) > 0)
            throw new IllegalArgumentException("Desconto inválido");
        this.desconto = desconto;
    }
    public BigDecimal calcularTotal() { return calcularSubtotal().add(taxaEntrega).subtract(desconto); }
    public void transicionarPara(StatusPedido destino) {
        transicionarPara(destino, null);
    }
    public void transicionarPara(StatusPedido destino, String entregador) {
        if (!status.podeTransicionarPara(destino, tipoEntrega)) throw new IllegalStateException("Transição de pedido inválida: " + status + " -> " + destino);
        if (destino == StatusPedido.SAIU_ENTREGA) nomeEntregador = validarNomeEntregador(entregador);
        status = destino;
    }
    private void exigirPendente() {
        if (status != StatusPedido.PENDENTE) throw new IllegalStateException("Itens só podem ser alterados em pedido pendente");
    }
}
