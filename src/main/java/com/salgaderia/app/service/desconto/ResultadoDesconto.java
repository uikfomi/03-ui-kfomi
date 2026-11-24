package com.salgaderia.app.service.desconto;

import java.math.BigDecimal;

public record ResultadoDesconto(BigDecimal subtotal, BigDecimal desconto, BigDecimal total, String estrategia, String cupom,
                                BigDecimal taxaEntrega,String brinde,boolean freteGratisVip, Integer tempoEstimadoMin, BigDecimal freteIsentoVip) {
    public ResultadoDesconto(BigDecimal subtotal,BigDecimal desconto,BigDecimal total,String estrategia,String cupom,
                             BigDecimal taxaEntrega,String brinde,boolean freteGratisVip,Integer prazo) {
        this(subtotal, desconto, total, estrategia, cupom, taxaEntrega, brinde, freteGratisVip, prazo, BigDecimal.ZERO);
    }
    public ResultadoDesconto(BigDecimal subtotal,BigDecimal desconto,BigDecimal total,String estrategia,String cupom,
                             BigDecimal taxaEntrega,String brinde,boolean freteGratisVip) {
        this(subtotal, desconto, total, estrategia, cupom, taxaEntrega, brinde, freteGratisVip, null);
    }
    public ResultadoDesconto comPrazo(Integer minutos) {
        return new ResultadoDesconto(subtotal, desconto, total, estrategia, cupom, taxaEntrega, brinde, freteGratisVip, minutos, freteIsentoVip);
    }
    public ResultadoDesconto(BigDecimal subtotal,BigDecimal desconto,BigDecimal total,String estrategia,String cupom){
        this(subtotal,desconto,total,estrategia,cupom,BigDecimal.ZERO,null,false);
    }
}
