package com.salgaderia.app.service.desconto;

import java.math.BigDecimal;

public record ResultadoDesconto(BigDecimal subtotal, BigDecimal desconto, BigDecimal total, String estrategia, String cupom,
                                BigDecimal taxaEntrega,String brinde,boolean freteGratisVip, Integer tempoEstimadoMin) {
    public ResultadoDesconto(BigDecimal subtotal,BigDecimal desconto,BigDecimal total,String estrategia,String cupom,
                             BigDecimal taxaEntrega,String brinde,boolean freteGratisVip) {
        this(subtotal, desconto, total, estrategia, cupom, taxaEntrega, brinde, freteGratisVip, null);
    }
    public ResultadoDesconto comPrazo(Integer minutos) {
        return new ResultadoDesconto(subtotal, desconto, total, estrategia, cupom, taxaEntrega, brinde, freteGratisVip, minutos);
    }
    public ResultadoDesconto(BigDecimal subtotal,BigDecimal desconto,BigDecimal total,String estrategia,String cupom){
        this(subtotal,desconto,total,estrategia,cupom,BigDecimal.ZERO,null,false);
    }
}
