package com.salgaderia.app.service.desconto;

import com.salgaderia.app.model.PedidoItem;
import java.math.BigDecimal;
import java.util.List;

public record ContextoDesconto(List<PedidoItem> itens, long pedidosEntregues, Cupom cupom, BigDecimal percentualAssinante,BigDecimal taxaEntrega) {
    public ContextoDesconto(List<PedidoItem> itens,long pedidosEntregues,Cupom cupom) {
        this(itens,pedidosEntregues,cupom,BigDecimal.ZERO,BigDecimal.ZERO);
    }
    public ContextoDesconto(List<PedidoItem> itens,long pedidosEntregues,Cupom cupom,BigDecimal percentual){
        this(itens,pedidosEntregues,cupom,percentual,BigDecimal.ZERO);
    }
    public ContextoDesconto {
        itens = List.copyOf(itens);
        if (pedidosEntregues < 0) throw new IllegalArgumentException("Quantidade inválida");
        if (percentualAssinante == null || percentualAssinante.signum()<0 || percentualAssinante.compareTo(BigDecimal.valueOf(100))>0)
            throw new IllegalArgumentException("Percentual de assinante inválido");
        if(taxaEntrega==null||taxaEntrega.signum()<0)throw new IllegalArgumentException("Taxa de entrega inválida");
    }
    public List<PedidoItem> itensElegiveis(){return itensElegiveis(itens);}
    public static List<PedidoItem> itensElegiveis(List<PedidoItem> itens){
        return itens.stream().filter(i->!i.isResgate() && (i.getPersonalizacao()==null||i.getPersonalizacao().tipo().orcamento()==null)).toList();
    }
    public BigDecimal subtotal() {
        return itens.stream().map(PedidoItem::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
