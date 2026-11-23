package com.salgaderia.app.service;

import com.salgaderia.app.dao.ConfiguracaoLojaDAO;
import com.salgaderia.app.dao.MovimentacaoEstoqueDAO;
import com.salgaderia.app.dao.impl.*;
import com.salgaderia.app.model.*;
import com.salgaderia.app.service.impl.AutenticacaoServiceImpl;
import com.salgaderia.app.service.impl.PedidoVendaServiceImpl;
import com.salgaderia.app.service.pix.PixPagamentoService;
import java.sql.Connection;
import java.time.*;

/** Desacopla os testes de outras regras da hora do dia e da configuração real da loja. */
final class VendaTeste {
    static final LocalDateTime AGORA = LocalDateTime.of(2026, 1, 1, 12, 0);
    static final ZoneId FORTALEZA = ZoneId.of("America/Fortaleza");
    static final ConfiguracaoLoja ABERTA = new ConfiguracaoLoja(true,LocalTime.of(8,0),LocalTime.of(23,0),true);

    static UsuarioCliente cadastrar(String nome,String prefixo,String senha,Cliente contato){
        var auth=new AutenticacaoServiceImpl();
        var usuario=auth.cadastrar(prefixo+"@example.test",senha);
        contato.setId(usuario.getCliente().getId());
        return auth.atualizarPerfil(usuario,nome,contato.getWhatsapp(),contato.getEndereco());
    }

    static PedidoVendaServiceImpl vendas() { return vendas(PixPagamentoService.demonstracao()); }
    static PedidoVendaServiceImpl vendas(PixPagamentoService pix) {
        return vendas(new MovimentacaoEstoqueDAOImpl(),pix,ABERTA);
    }
    static PedidoVendaServiceImpl vendas(MovimentacaoEstoqueDAO movimentos,PixPagamentoService pix,ConfiguracaoLoja config) {
        return new PedidoVendaServiceImpl(new PedidoDAOImpl(),new ProdutoDAOImpl(),movimentos,pix,operacao(config));
    }
    static OperacaoLojaService operacao(ConfiguracaoLoja config) {
        var dao = new ConfiguracaoLojaDAO() {
            public ConfiguracaoLoja buscar(Connection c,boolean bloquear) { return config; }
            public void salvarExpediente(Connection c,ConfiguracaoLoja nova) { throw new UnsupportedOperationException(); }
            public void definirPedidoMinimo(Connection c,java.math.BigDecimal valor) { throw new UnsupportedOperationException(); }
            public void definirAberturaManual(Connection c,boolean aberta) { throw new UnsupportedOperationException(); }
        };
        return new OperacaoLojaService(dao,Clock.fixed(AGORA.atZone(FORTALEZA).toInstant(),FORTALEZA));
    }
}
