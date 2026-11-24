package com.salgaderia.app.model;

import java.math.BigDecimal;

public record RepasseEntregador(long entregadorId, String nome, long entregas, BigDecimal totalTaxas) { }
