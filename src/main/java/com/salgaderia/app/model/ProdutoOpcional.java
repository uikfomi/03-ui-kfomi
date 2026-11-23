package com.salgaderia.app.model;

import java.math.BigDecimal;

/** Uma escolha de um grupo de opcionais; a franquia e o máximo valem por unidade. */
public record ProdutoOpcional(Long id, String grupoNome, String nome, BigDecimal precoAdicional,
                              int gratisAteLimite, int maximoEscolhas) {
    public ProdutoOpcional {
        Validacao.id(id);
        grupoNome = texto(grupoNome, "grupo");
        nome = texto(nome, "opcional");
        if (precoAdicional == null || precoAdicional.signum() < 0 || precoAdicional.stripTrailingZeros().scale() > 2
                || precoAdicional.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("Informe um preço adicional válido, com até duas casas decimais.");
        if (maximoEscolhas < 1 || maximoEscolhas > 65535 || gratisAteLimite < 0 || gratisAteLimite > maximoEscolhas)
            throw new IllegalArgumentException("O máximo deve ser positivo e a franquia deve ficar entre zero e o máximo.");
    }

    private static String texto(String valor, String campo) {
        String limpo = Validacao.texto(valor, campo).strip();
        if (limpo.isEmpty() || limpo.codePointCount(0, limpo.length()) > 100 || limpo.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Informe " + campo + " com até 100 caracteres em uma linha.");
        return limpo;
    }
}
