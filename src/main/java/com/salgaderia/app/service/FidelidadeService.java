package com.salgaderia.app.service;

import com.salgaderia.app.dao.impl.*;
import com.salgaderia.app.model.*;
import com.salgaderia.app.util.ConnectionFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.text.Normalizer;

public final class FidelidadeService implements PedidoConcluidoObserver {
    private final FidelidadeDAO dao = new FidelidadeDAO();
    private final UsuarioDAOImpl usuarios = new UsuarioDAOImpl(new ContatoDAOImpl());

    public record Carteira(Set<Long> favoritos, long saldo) { }

    public static Optional<Produto> sugestao(List<Produto> catalogo, List<PedidoItem> carrinho) {
        if (carrinho.isEmpty()) return Optional.empty();
        var presentes = PersonalizacaoService.quantidades(carrinho).keySet();
        return catalogo.stream().filter(Produto::isDisponivelVenda).filter(p -> p.getPrecoVenda().signum() > 0)
                .filter(p -> !presentes.contains(p.getId())).filter(p -> PersonalizacaoService.bebida(p) || acompanhamento(p))
                .min(Comparator.comparing(Produto::getPrecoVenda).thenComparing(Produto::getId));
    }
    private static boolean acompanhamento(Produto p) {
        if (p.getFichaVitrine().tipo() == FichaVitrine.TipoCombo.ACOMPANHAMENTO) return true;
        if (p.getCategoria() == null) return false;
        String nome = Normalizer.normalize(p.getCategoria().getNome(), Normalizer.Form.NFD).toLowerCase(Locale.ROOT);
        return nome.contains("acompanh") || nome.contains("molho");
    }
    public Carteira carteira(UsuarioCliente cliente) {
        return ConnectionFactory.executarEmTransacao(c -> {
            long id = exigirCliente(c, cliente);
            return new Carteira(Set.copyOf(dao.favoritos(c, id)), dao.saldo(c, id));
        });
    }
    public void definirFavorito(UsuarioCliente cliente, long produto, boolean favorito) {
        ConnectionFactory.executarEmTransacao(c -> {
            dao.definirFavorito(c, exigirCliente(c, cliente), produto, favorito);
            return null;
        });
    }
    long exigirCliente(Connection c, UsuarioCliente cliente) throws SQLException {
        if (cliente == null || !cliente.isAtivo() || cliente.getId() == null || cliente.getCliente().getId() == null
                || !usuarios.bloquearClienteAtivo(c, cliente.getId(), cliente.getCliente().getId()))
            throw new IllegalArgumentException("Cliente autenticado obrigatório.");
        return cliente.getCliente().getId();
    }
    public void validarSaldo(Connection c, long cliente, int pontos) throws SQLException {
        if (dao.saldo(c, cliente) < pontos) throw saldoInsuficiente();
    }
    public void debitar(Connection c, long cliente, long pedido, int pontos) throws SQLException {
        int restantes = pontos;
        for (var lote : dao.lotesParaResgate(c, cliente)) {
            int usar = (int) Math.min(lote.saldo(), restantes);
            dao.consumir(c, pedido, lote.id(), usar);
            restantes -= usar;
            if (restantes == 0) return;
        }
        throw saldoInsuficiente();
    }
    private static IllegalArgumentException saldoInsuficiente() {
        return new IllegalArgumentException("Saldo de pontos válidos insuficiente para o resgate.");
    }
    @Override public void aoAlterarPedido(Connection c, Evento evento) throws SQLException {
        switch (evento.etapa()) {
            case PAGAMENTO_CONFIRMADO, ENTREGUE -> dao.creditarCompraPaga(c, evento.pedidoId());
            case CANCELADO -> dao.estornar(c, evento.pedidoId());
            case FINALIZADO -> { }
        }
    }
}
