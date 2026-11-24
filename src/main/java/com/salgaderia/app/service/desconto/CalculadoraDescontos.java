package com.salgaderia.app.service.desconto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CalculadoraDescontos {
    private final List<EstrategiaDesconto> estrategias = List.of(new DescontoAssinante(),new DescontoCombo(),
            new DescontoCupomPercentual(),new DescontoCentoSalgados(),new DescontoFidelidade());

    public ResultadoDesconto calcular(ContextoDesconto contexto) {
        BigDecimal subtotal=contexto.subtotal().setScale(2,RoundingMode.HALF_UP);
        BigDecimal ajusteCaixas=DescontoCaixaMisteriosa.calcularCaixas(contexto.itens());
        boolean freteVip=contexto.percentualAssinante().signum()>0&&contexto.taxaEntrega().signum()>0;
        BigDecimal taxa=freteVip?BigDecimal.ZERO.setScale(2):contexto.taxaEntrega();
        var elegivel=new ContextoDesconto(contexto.itensElegiveis(),contexto.pedidosEntregues(),contexto.cupom(),contexto.percentualAssinante(),taxa);
        BigDecimal melhor=BigDecimal.ZERO.setScale(2);
        String nome="NENHUM";
        for(EstrategiaDesconto estrategia:estrategias){
            BigDecimal valor=estrategia.calcular(elegivel);
            if(valor.compareTo(melhor)>0){melhor=valor;nome=estrategia.nome();}
        }
        Cupom cupom=elegivel.cupom();
        String codigo=nome.equals("CUPOM")?cupom.codigo():null;
        String brinde=null;
        if(cupom!=null&&elegivel.subtotal().compareTo(cupom.minimo())>=0){
            switch(cupom.beneficio()){
                case FRETE -> {
                    if(taxa.compareTo(melhor)>0){melhor=taxa;nome="FRETE";codigo=cupom.codigo();}
                }
                case BRINDE -> {
                    if(elegivel.subtotal().signum()>0&&elegivel.percentualAssinante().signum()==0&&cupom.valorBeneficio().compareTo(melhor)>0){
                        melhor=BigDecimal.ZERO.setScale(2);nome="BRINDE";codigo=cupom.codigo();brinde=cupom.brinde();
                    }
                }
                case PONTOS_DOBRO -> {
                    if(nome.equals("NENHUM")){nome="PONTOS_DOBRO";codigo=cupom.codigo();}
                }
                default -> { }
            }
        }
        if(nome.equals("NENHUM")&&ajusteCaixas.signum()>0)nome="CAIXA_MISTERIOSA";
        BigDecimal desconto=melhor.add(ajusteCaixas);
        return new ResultadoDesconto(subtotal,desconto,subtotal.add(taxa).subtract(desconto),nome,codigo,taxa,brinde,freteVip,null,freteVip?contexto.taxaEntrega():BigDecimal.ZERO);
    }
}
