package com.salgaderia.app.model;
import com.salgaderia.app.model.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PedidoFila(long id, String cliente, StatusPedido status, TipoEntrega entrega,
                         String enderecoEntrega, LocalDateTime criadoEm, BigDecimal total, String itens, String telefone,
                         LocalDateTime agendadoPara, StatusPagamento statusPagamento, boolean pixDemonstracao, boolean pixReal, String nomeEntregador, java.util.List<String> detalhesPreparo, Integer tempoEstimadoMin, String bairro, boolean cartaoApp) {
    public PedidoFila(long id, String cliente, StatusPedido status, TipoEntrega entrega,
                      String enderecoEntrega, LocalDateTime criadoEm, BigDecimal total, String itens, String telefone,
                      LocalDateTime agendadoPara, StatusPagamento pagamento, boolean demo, boolean pixReal, String entregador,
                      java.util.List<String> detalhes, Integer prazo, String bairro) {
        this(id,cliente,status,entrega,enderecoEntrega,criadoEm,total,itens,telefone,agendadoPara,pagamento,demo,pixReal,entregador,detalhes,prazo,bairro,false);
    }
    public boolean atrasadoEm(LocalDateTime agora) {
        return statusPagamento != StatusPagamento.AGUARDANDO_CARTAO && tempoEstimadoMin != null && status.proximo(entrega) != null && agora.isAfter(criadoEm.plusMinutes(tempoEstimadoMin));
    }
    public PedidoFila {
        detalhesPreparo = java.util.List.copyOf(detalhesPreparo);
    }
    public PedidoFila(long id,String cliente,StatusPedido status,TipoEntrega entrega,String enderecoEntrega,LocalDateTime criadoEm,BigDecimal total,String itens,String telefone,LocalDateTime agendadoPara){
        this(id,cliente,status,entrega,enderecoEntrega,criadoEm,total,itens,telefone,agendadoPara,StatusPagamento.NA_ENTREGA,false,false,null,java.util.List.of(),null,null);
    }
    public PedidoFila(long id,String cliente,StatusPedido status,TipoEntrega entrega,String enderecoEntrega,LocalDateTime criadoEm,BigDecimal total,String itens,String telefone){
        this(id,cliente,status,entrega,enderecoEntrega,criadoEm,total,itens,telefone,null);
    }
}
