package com.salgaderia.app.service;

import com.salgaderia.app.dao.impl.LogisticaDAO;
import com.salgaderia.app.model.*;
import com.salgaderia.app.util.ConnectionFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class LogisticaService {
    private final LogisticaDAO dao = new LogisticaDAO();
    public static final int PRAZO_BALCAO_MIN = 30;
    public record Cotacao(BigDecimal taxa, int minutos) { }

    public List<BairroEntrega> bairros(boolean somenteAtivos) {
        return ConnectionFactory.executarEmTransacao(c -> dao.bairros(c, somenteAtivos));
    }
    public List<Entregador> entregadores(boolean somenteAtivos) {
        return ConnectionFactory.executarEmTransacao(c -> dao.entregadores(c, somenteAtivos));
    }
    public void salvar(BairroEntrega bairro) {
        String nome = texto(bairro.nome(), 100, "Nome do bairro", true);
        BigDecimal taxa = ConfiguracaoLoja.validarValorMonetario(bairro.taxaEntrega(), "taxa de entrega");
        if (bairro.tempoEstimadoMin() < 1 || bairro.tempoEstimadoMin() > 1440)
            throw new IllegalArgumentException("Informe um tempo de entrega entre 1 e 1440 minutos.");
        var validado = new BairroEntrega(bairro.id(), nome, taxa, bairro.tempoEstimadoMin(), bairro.ativo());
        ConnectionFactory.executarEmTransacao(c -> { dao.salvar(c, validado); return null; });
    }
    public void salvar(Entregador entregador) {
        var validado = new Entregador(entregador.id(), Pedido.validarNomeEntregador(entregador.nome()),
                texto(entregador.telefone(), 30, "Telefone", false), texto(entregador.placaVeiculo(), 10, "Placa", false), entregador.ativo());
        ConnectionFactory.executarEmTransacao(c -> { dao.salvar(c, validado); return null; });
    }
    public Cotacao cotar(Connection c, Long bairroId, ConfiguracaoLoja config, boolean bloquear) throws SQLException {
        if (bairroId == null) throw new IllegalArgumentException("Selecione um bairro atendido para entrega.");
        var bairro = dao.bairroAtivo(c, bairroId, bloquear).orElseThrow(() ->
                new IllegalArgumentException("Bairro não atendido. Selecione outro bairro ou retire no local."));
        Cotacao cotacao = cotar(bairro, config);
        ConfiguracaoLoja.validarValorMonetario(cotacao.taxa(), "frete com acréscimo");
        return cotacao;
    }
    public static Cotacao cotar(BairroEntrega bairro, ConfiguracaoLoja config) {
        return config.modoChuvaAtivo()
                ? new Cotacao(bairro.taxaEntrega().add(config.acrescimoTaxaChuva()), bairro.tempoEstimadoMin() + config.acrescimoTempoChuvaMin())
                : new Cotacao(bairro.taxaEntrega(), bairro.tempoEstimadoMin());
    }
    public Entregador exigirEntregadorAtivo(Connection c, long id) throws SQLException {
        return dao.entregadorAtivo(c, id).orElseThrow(() -> new IllegalArgumentException("Selecione um entregador ativo."));
    }
    public List<RepasseEntregador> repasses(LocalDateTime inicio, LocalDateTime fim) {
        if (!inicio.isBefore(fim)) throw new IllegalArgumentException("Informe um período válido.");
        return ConnectionFactory.executarEmTransacao(c -> dao.repasses(c, inicio, fim));
    }
    private static String texto(String valor, int limite, String campo, boolean obrigatorio) {
        String limpo = valor == null ? "" : valor.strip();
        if (limpo.isEmpty() && !obrigatorio) return null;
        if (limpo.isEmpty() || limpo.codePointCount(0, limpo.length()) > limite || limpo.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(campo + ": informe até " + limite + " caracteres válidos.");
        return limpo;
    }
}
