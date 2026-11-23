package com.salgaderia.app.service;

import com.salgaderia.app.model.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

public final class ProdutoPersonalizacaoService {
    private ProdutoPersonalizacaoService() { }

    private static String chave(String nome) {
        return Normalizer.normalize(nome.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public static void validarCatalogo(List<ProdutoOpcional> opcoes) {
        Map<String, ProdutoOpcional> grupos = new HashMap<>();
        Map<String, Set<String>> nomes = new HashMap<>();
        for (var o : opcoes) {
            String chave = chave(o.grupoNome());
            var anterior = grupos.putIfAbsent(chave, o);
            if (anterior != null && (!anterior.grupoNome().equals(o.grupoNome()) || anterior.maximoEscolhas() != o.maximoEscolhas()
                    || anterior.gratisAteLimite() != o.gratisAteLimite()))
                throw new IllegalArgumentException("Use o mesmo nome, máximo e franquia em todas as opções do grupo " + anterior.grupoNome() + ".");
            if (!nomes.computeIfAbsent(chave, g -> new HashSet<>()).add(chave(o.nome())))
                throw new IllegalArgumentException("Opção repetida no grupo " + o.grupoNome() + ": " + o.nome());
        }
    }

    /** Precifica na ordem do catálogo (independente da ordem dos cliques). Opções já gratuitas não consomem franquia. */
    public static PedidoItem montar(Produto produto, List<ProdutoOpcional> catalogo, Set<Long> ids, String observacao, BigDecimal quantidade) {
        if (!ids.isEmpty()) validarCatalogo(catalogo);
        Map<String, Integer> escolhas = new HashMap<>(), cobraveis = new HashMap<>();
        Map<String, StringJoiner> resumo = new LinkedHashMap<>();
        BigDecimal preco = produto.getPrecoVenda();
        int encontradas = 0;
        for (var o : catalogo) {
            if (!ids.contains(o.id())) continue;
            encontradas++;
            String grupo = o.grupoNome();
            if (escolhas.merge(grupo, 1, Integer::sum) > o.maximoEscolhas())
                throw new IllegalArgumentException(grupo + ": escolha até " + o.maximoEscolhas() + " opção(ões).");
            if (o.precoAdicional().signum() > 0 && cobraveis.merge(grupo, 1, Integer::sum) > o.gratisAteLimite())
                preco = preco.add(o.precoAdicional());
            resumo.computeIfAbsent(grupo, g -> new StringJoiner(", ", g + ": ", "")).add(o.nome());
        }
        if (encontradas != ids.size()) throw new IllegalArgumentException("Um opcional não está mais disponível. Escolha novamente.");
        if (preco.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("Preço unitário com opcionais excede o limite permitido.");
        String texto = observacao == null || observacao.isBlank() ? null : observacao.strip();
        String adicionais = resumo.isEmpty() ? null : String.join("\n", resumo.values().stream().map(StringJoiner::toString).toList());
        if (adicionais != null && adicionais.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65535)
            throw new IllegalArgumentException("O resumo dos opcionais é muito longo.");
        return new PedidoItem(null, produto, quantidade, preco, texto, null, ids, adicionais);
    }
}
