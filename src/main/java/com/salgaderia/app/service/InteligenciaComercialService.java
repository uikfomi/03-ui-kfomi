package com.salgaderia.app.service;

import com.salgaderia.app.dao.impl.*;
import com.salgaderia.app.model.UsuarioAdmin;
import com.salgaderia.app.model.UsuarioCliente;
import com.salgaderia.app.model.IndicadoresComerciais.*;
import com.salgaderia.app.util.ConnectionFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class InteligenciaComercialService {
    public static final ZoneId FUSO = ZoneId.of("America/Fortaleza");
    private final InteligenciaComercialDAO dao = new InteligenciaComercialDAO();
    private final UsuarioDAOImpl usuarios = new UsuarioDAOImpl(new ContatoDAOImpl());
    private final FidelidadeService fidelidade = new FidelidadeService();

    public ResumoCliente resumoDoMes(UsuarioCliente cliente) {
        LocalDate inicio = LocalDate.now(FUSO).withDayOfMonth(1);
        return ConnectionFactory.executarEmTransacao(c -> dao.resumo(c, fidelidade.exigirCliente(c,cliente),
                inicio.atStartOfDay(), inicio.plusMonths(1).atStartOfDay()));
    }
    public List<ClienteRadar> radar(UsuarioAdmin admin) {
        return ConnectionFactory.executarEmTransacao(c -> {
            exigirAdmin(c,admin);
            var agora = LocalDateTime.now(FUSO);
            return dao.radar(c,agora.toLocalDate(),agora.minusDays(10),null);
        });
    }
    public CupomReativacao reativar(UsuarioAdmin admin, ClienteRadar cliente) {
        return ConnectionFactory.executarEmTransacao(c -> {
            // A trava da conta serializa emissões e compras; confira o segmento depois de adquiri-la.
            if (cliente == null || !usuarios.bloquearClienteAtivo(c,cliente.usuarioId(),cliente.clienteId()))
                throw new IllegalArgumentException("Cliente ativo não encontrado.");
            exigirAdmin(c,admin);
            var agora = LocalDateTime.now(FUSO);
            if (dao.radar(c,agora.toLocalDate(),agora.minusDays(10),cliente.usuarioId()).stream().noneMatch(ClienteRadar::sumido))
                throw new IllegalArgumentException("O cliente não está mais há mais de 10 dias sem pedir.");
            var existente = dao.cupomValido(c,cliente.usuarioId());
            if (existente.isPresent()) return existente.get();
            String codigo = "VOLTA10-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
            LocalDateTime validade = agora.plusDays(7).truncatedTo(ChronoUnit.SECONDS);
            dao.inserirReativacao(c,cliente.usuarioId(),codigo,validade);
            return new CupomReativacao(codigo,validade);
        });
    }
    public Fechamento fechamento(UsuarioAdmin admin, LocalDate inicio, LocalDate fimInclusivo) {
        if (inicio == null || fimInclusivo == null || fimInclusivo.isBefore(inicio))
            throw new IllegalArgumentException("Informe um período válido.");
        var de = inicio.atStartOfDay(); var ate = fimInclusivo.plusDays(1).atStartOfDay();
        return ConnectionFactory.executarEmTransacao(c -> {
            exigirAdmin(c,admin);
            return new Fechamento(List.copyOf(dao.entradas(c,de,ate)),List.copyOf(dao.bairros(c,de,ate)));
        });
    }
    private void exigirAdmin(Connection c, UsuarioAdmin admin) throws SQLException {
        if (admin == null || admin.getId() == null || !admin.isAtivo() ||
                usuarios.buscarPorId(c,admin.getId()).filter(u -> u instanceof UsuarioAdmin && u.isAtivo()).isEmpty())
            throw new IllegalArgumentException("Acesso restrito a administradores ativos.");
    }
}
