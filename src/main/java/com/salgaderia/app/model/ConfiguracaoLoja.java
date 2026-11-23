package com.salgaderia.app.model;

import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.Objects;

public record ConfiguracaoLoja(boolean abertaManualmente, LocalTime abertura, LocalTime fechamento,
                               boolean respeitarHorarioAutomatico, BigDecimal pedidoMinimoEntrega) {
    public ConfiguracaoLoja(boolean aberta, LocalTime abertura, LocalTime fechamento, boolean automatico) {
        this(aberta, abertura, fechamento, automatico, new BigDecimal("20.00"));
    }
    public static BigDecimal validarMinimo(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.stripTrailingZeros().scale() > 2
                || valor.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("Informe um pedido mínimo válido, com até duas casas decimais.");
        return valor.setScale(2);
    }
    public ConfiguracaoLoja {
        pedidoMinimoEntrega = validarMinimo(pedidoMinimoEntrega);
        Objects.requireNonNull(abertura);
        Objects.requireNonNull(fechamento);
        if (abertura.equals(fechamento)) throw new IllegalArgumentException("Abertura e fechamento devem ser diferentes.");
    }
}
