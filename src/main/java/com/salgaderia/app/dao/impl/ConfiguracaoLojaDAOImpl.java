package com.salgaderia.app.dao.impl;

import com.salgaderia.app.dao.ConfiguracaoLojaDAO;
import com.salgaderia.app.model.ConfiguracaoLoja;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;

public final class ConfiguracaoLojaDAOImpl implements ConfiguracaoLojaDAO {
    @Override public ConfiguracaoLoja buscar(Connection c, boolean bloquearAlteracoes) throws SQLException {
        String sql = "SELECT * FROM configuracao_loja WHERE id=1" + (bloquearAlteracoes ? " FOR SHARE" : "");
        try (var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Configuração operacional da loja não encontrada.");
            return new ConfiguracaoLoja(rs.getBoolean("aberta_manualmente"),
                    rs.getObject("horario_abertura", LocalTime.class), rs.getObject("horario_fechamento", LocalTime.class),
                    rs.getBoolean("respeitar_horario_automatico"), rs.getBigDecimal("pedido_minimo_entrega"), rs.getBoolean("modo_chuva_ativo"),
                    rs.getInt("acrescimo_tempo_chuva_min"), rs.getBigDecimal("acrescimo_taxa_chuva"));
        }
    }
    @Override public void salvarExpediente(Connection c, ConfiguracaoLoja config) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE configuracao_loja SET horario_abertura=?, horario_fechamento=?, respeitar_horario_automatico=? WHERE id=1")) {
            ps.setObject(1, config.abertura()); ps.setObject(2, config.fechamento());
            ps.setBoolean(3, config.respeitarHorarioAutomatico());
            if (ps.executeUpdate() != 1) throw new SQLException("Expediente não atualizado.");
        }
    }
    @Override public void definirPedidoMinimo(Connection c, java.math.BigDecimal valor) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE configuracao_loja SET pedido_minimo_entrega=? WHERE id=1")) {
            ps.setBigDecimal(1, valor);
            if (ps.executeUpdate() != 1) throw new SQLException("Pedido mínimo não atualizado.");
        }
    }
    @Override public void definirModoChuva(Connection c, boolean ativo) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE configuracao_loja SET modo_chuva_ativo=? WHERE id=1")) {
            ps.setBoolean(1, ativo);
            if (ps.executeUpdate() != 1) throw new SQLException("Modo Chuva/Pico não atualizado.");
        }
    }
    @Override public void definirAberturaManual(Connection c, boolean aberta) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE configuracao_loja SET aberta_manualmente=? WHERE id=1")) {
            ps.setBoolean(1, aberta);
            if (ps.executeUpdate() != 1) throw new SQLException("Abertura manual não atualizada.");
        }
    }
}
