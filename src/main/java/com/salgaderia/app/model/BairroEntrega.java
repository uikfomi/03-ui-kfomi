package com.salgaderia.app.model;

import java.math.BigDecimal;

public record BairroEntrega(Long id, String nome, BigDecimal taxaEntrega, int tempoEstimadoMin, boolean ativo) {
    @Override public String toString() { return nome; }
}
