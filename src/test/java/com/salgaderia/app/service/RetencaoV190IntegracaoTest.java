package com.salgaderia.app.service;

import com.salgaderia.app.dao.impl.*;
import com.salgaderia.app.model.*;
import com.salgaderia.app.model.IndicadoresComerciais.Fechamento;
import com.salgaderia.app.model.enums.*;
import com.salgaderia.app.service.impl.*;
import com.salgaderia.app.service.pix.PixPagamentoService;
import com.salgaderia.app.util.ConnectionFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SALGADERIA_TESTE_MYSQL",matches="true")
class RetencaoV190IntegracaoTest {
    private static final UsuarioAdmin ADMIN = new UsuarioAdmin(2L,"Admin","toor","não usada",true);

    @Test void favoritosIsoladosEResgateConsomeLotesMaisAntigosSemUsarExpirados() throws Exception {
        try (var f = new Cenario()) {
            var ana=f.cliente();var bia=f.cliente();
            long produtoId=f.recompensa.getId();
            assertEquals("Novidade",f.catalogo.buscarPorId(produtoId).orElseThrow().getEtiquetaDestaque());
            assertEquals(12,f.catalogo.buscarPorId(produtoId).orElseThrow().getPontosResgate());
            f.fidelidade.definirFavorito(ana,produtoId,true);
            f.fidelidade.definirFavorito(ana,produtoId,true);
            assertEquals(Set.of(produtoId),f.fidelidade.carteira(ana).favoritos());
            assertTrue(f.fidelidade.carteira(bia).favoritos().isEmpty());
            f.fidelidade.definirFavorito(ana,produtoId,false);
            assertTrue(f.fidelidade.carteira(ana).favoritos().isEmpty());

            long expirado = f.pagar(ana,FormaPagamento.DINHEIRO);
            long antigo = f.pagar(ana,FormaPagamento.DINHEIRO);
            long novo = f.pagar(ana,FormaPagamento.DINHEIRO);
            assertEquals(30,f.saldo(ana)); // R$ 10,99 gera 10 pontos por pedido, sem arredondar para cima.
            ConnectionFactory.executarEmTransacao(c -> {
                try (var ps=c.prepareStatement("""
                        SELECT TIMESTAMPDIFF(DAY,p.pago_em,l.expira_em) FROM cliente_pontos_lotes l
                        JOIN pedidos p ON p.id=l.pedido_id WHERE p.id=?
                        """)) {
                    ps.setLong(1,novo);
                    try(var rs=ps.executeQuery()){assertTrue(rs.next());assertEquals(60,rs.getInt(1));}
                }
                executar(c,"UPDATE cliente_pontos_lotes SET expira_em=NOW() WHERE pedido_id=?",expirado);
                executar(c,"UPDATE cliente_pontos_lotes SET expira_em=NOW()+INTERVAL 1 DAY WHERE pedido_id=?",antigo);
                return null;
            });
            assertEquals(20,f.saldo(ana));
            Pedido resgate = f.resgate(ana);
            var cotado = f.vendas.simular(resgate,null);
            assertEquals(0,cotado.total().signum());
            long id=f.vendas.finalizar(resgate,null,cotado);
            assertEquals(StatusPagamento.PAGO,resgate.getStatusPagamento());
            var historico=f.vendas.listarDoCliente(ana).stream().filter(p -> p.getId()==id).findFirst().orElseThrow();
            assertTrue(historico.getItens().get(0).isResgate());
            assertEquals(12,historico.getItens().get(0).getPontosResgate());
            assertEquals(0,pontosDoLote(antigo));assertEquals(8,pontosDoLote(novo));assertEquals(10,pontosDoLote(expirado));
            assertEquals(8,f.saldo(ana));
            assertEquals(new BigDecimal("6.50"),f.comercial.resumoDoMes(ana).economia());
            assertThrows(IllegalArgumentException.class,() -> f.vendas.finalizar(f.resgate(ana)));
            assertThrows(IllegalArgumentException.class,() -> f.vendas.simular(f.resgate(bia),null));
            f.vendas.cancelarDoCliente(ana,id);
            assertEquals(20,f.saldo(ana));

            // Duas requisições concorrentes não conseguem consumir os mesmos pontos.
            var pool = Executors.newFixedThreadPool(2);
            try {
                Callable<Boolean> tentativa = () -> {
                    try { f.vendas.finalizar(f.resgate(ana));return true; }
                    catch (IllegalArgumentException e) { return false; }
                };
                var tentativas = pool.invokeAll(List.of(tentativa,tentativa));
                assertNotEquals(tentativas.get(0).get(),tentativas.get(1).get());
            } finally { pool.shutdownNow(); }
            assertEquals(8,f.saldo(ana));
        }
    }

    @Test void cartaoAppCaixaEValorLiquidoNaoDuplicamCredito() {
        try (var f = new Cenario()) {
            var cliente=f.cliente();
            var data=LocalDate.now(InteligenciaComercialService.FUSO);
            var antes=f.comercial.fechamento(ADMIN,data,data);
            long pendente=f.vendas.finalizar(f.compra(cliente,FormaPagamento.CARTAO_APP,TipoEntrega.RETIRADA));
            assertEquals(StatusPagamento.AGUARDANDO_CARTAO, f.vendas.listarFila().stream().filter(p -> p.id()==pendente).findFirst().orElseThrow().statusPagamento());
            assertThrows(IllegalStateException.class,() -> f.vendas.transicionar(pendente,StatusPedido.PREPARANDO));
            assertEquals(0,f.saldo(cliente));
            f.vendas.confirmarCartaoApp(pendente,ADMIN);
            assertThrows(IllegalArgumentException.class,() -> f.vendas.confirmarCartaoApp(pendente,ADMIN));
            assertThrows(IllegalArgumentException.class,() -> f.vendas.cancelarDoCliente(cliente,pendente));
            assertEquals(10,f.saldo(cliente));
            f.entregar(pendente);
            assertEquals(10,f.saldo(cliente));

            f.pagar(cliente,FormaPagamento.DINHEIRO);
            var demo=f.compra(cliente,FormaPagamento.PIX,TipoEntrega.RETIRADA);
            f.vendas.finalizar(demo);
            f.pix.confirmarOffline(cliente,demo.getId());
            f.entregar(demo.getId());
            assertEquals(20,f.saldo(cliente));

            long bairro=f.bairro();long parceiro=f.parceiro();
            var entrega=f.compra(cliente,FormaPagamento.CARTAO,TipoEntrega.ENTREGA);entrega.setBairroId(bairro);
            long emRota=f.vendas.finalizar(entrega);
            f.vendas.transicionar(emRota,StatusPedido.PREPARANDO);f.vendas.transicionar(emRota,StatusPedido.PRONTO);
            f.vendas.despachar(emRota,parceiro);f.vendas.transicionar(emRota,StatusPedido.ENTREGUE);
            assertEquals(33,f.saldo(cliente));

            var depois=f.comercial.fechamento(ADMIN,data,data);
            assertEquals(new BigDecimal("10.99"),total(depois,"Cartão App").subtract(total(antes,"Cartão App")));
            assertEquals(new BigDecimal("10.99"),total(depois,"Dinheiro").subtract(total(antes,"Dinheiro")));
            assertEquals(new BigDecimal("13.99"),total(depois,"Maquininha na Entrega").subtract(total(antes,"Maquininha na Entrega")));
            assertEquals(0,total(depois,"PIX").compareTo(total(antes,"PIX")));
            assertEquals(1,depois.bairros().stream().filter(b -> b.bairro().equals(f.chave)).findFirst().orElseThrow().pedidos());
            assertEquals(new BigDecimal("35.97"),f.comercial.resumoDoMes(cliente).gasto());
        }
    }

    @Test void radarRevalidaInatividadeEGeraVolta10ExclusivoSemDuplicar() {
        try (var f = new Cenario()) {
            var ana=f.cliente();var vip=f.cliente();
            long venda=f.pagar(ana,FormaPagamento.DINHEIRO);
            assertTrue(f.comercial.radar(ADMIN).stream().noneMatch(c -> c.usuarioId()==ana.getId()));
            ConnectionFactory.executarEmTransacao(c -> {
                executar(c,"UPDATE pedidos SET criado_em=NOW()-INTERVAL 11 DAY WHERE id=?",venda);return null;
            });
            var clube=new ClubeServiceImpl();
            clube.solicitar(vip,clube.vitrine(vip).planos().get(0).id());
            clube.confirmar(ADMIN,vip.getId(),clube.vitrine(vip).pendencia().id());
            var radar=f.comercial.radar(ADMIN);
            assertTrue(radar.stream().anyMatch(c -> c.usuarioId()==vip.getId() && c.vip() && !c.sumido()));
            var sumida=radar.stream().filter(c -> c.usuarioId()==ana.getId()).findFirst().orElseThrow();
            assertTrue(sumida.sumido());
            var cupom=f.comercial.reativar(ADMIN,sumida);
            assertEquals(cupom,f.comercial.reativar(ADMIN,sumida));
            var pedido=f.compra(ana,FormaPagamento.DINHEIRO,TipoEntrega.RETIRADA);
            var cotacao=f.vendas.simular(pedido,"volta10");
            assertEquals(cupom.codigo(),cotacao.cupom());
            assertThrows(IllegalArgumentException.class,() -> f.vendas.simular(f.compra(vip,FormaPagamento.DINHEIRO,TipoEntrega.RETIRADA),"VOLTA10"));
            f.vendas.finalizar(pedido,"VOLTA10",cotacao);
            assertThrows(IllegalArgumentException.class,() -> f.vendas.simular(pedido,"VOLTA10"));
            assertThrows(IllegalArgumentException.class,() -> f.comercial.reativar(ADMIN,sumida));
        }
    }

    private static BigDecimal total(Fechamento f,String canal) {
        return f.entradas().stream().filter(e -> e.canal().equals(canal)).findFirst().map(e -> e.total()).orElse(BigDecimal.ZERO);
    }
    private static long pontosDoLote(long pedido) {
        return ConnectionFactory.executarEmTransacao(c -> {
            try (var ps=c.prepareStatement("SELECT pontos_disponiveis FROM cliente_pontos_lotes WHERE pedido_id=?")) {
                ps.setLong(1,pedido);try(var rs=ps.executeQuery()){assertTrue(rs.next());return rs.getLong(1);}
            }
        });
    }
    private static void executar(Connection c,String sql,long id) throws SQLException {
        try(var ps=c.prepareStatement(sql)){ps.setLong(1,id);ps.executeUpdate();}
    }
    private static final class Cenario implements AutoCloseable {
        final String chave=UUID.randomUUID().toString().replace("-", "");
        final List<UsuarioCliente> clientes=new ArrayList<>();
        final ProdutoEstoqueService catalogo=new ProdutoEstoqueServiceImpl();
        final FidelidadeService fidelidade=new FidelidadeService();
        final InteligenciaComercialService comercial=new InteligenciaComercialService();
        final PixPagamentoService pix=PixPagamentoService.demonstracao();
        final PedidoVendaService vendas=VendaTeste.vendas(new MovimentacaoEstoqueDAOImpl(),pix,
                new ConfiguracaoLoja(true,LocalTime.of(8,0),LocalTime.of(23,0),false,BigDecimal.ZERO));
        final Produto produto = criar("Pedido", "10.99", null);
        final Produto recompensa = criar("Recompensa", "6.50", 12);
        Long bairro,parceiro;

        Produto criar(String rotulo,String preco,Integer pontos) {
            var p=new Produto(null,null,rotulo+" "+chave,null,new BigDecimal(preco),BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,null);
            p.setEtiquetaDestaque("Novidade");p.setPontosResgate(pontos);catalogo.cadastrar(p);return p;
        }
        UsuarioCliente cliente() {
            var u=VendaTeste.cadastrar("Cliente teste","r"+clientes.size()+chave,"senha-teste",
                    new Cliente(null,"Cliente "+chave,null,null,"85999999999","Rua de teste",null));
            clientes.add(u);return u;
        }
        Pedido compra(UsuarioCliente cliente,FormaPagamento forma,TipoEntrega tipo) {
            var pedido=new Pedido(null,cliente,cliente.getCliente(),StatusPedido.PENDENTE,tipo,
                    tipo==TipoEntrega.ENTREGA?cliente.getCliente().getEndereco():null,LocalDateTime.now(InteligenciaComercialService.FUSO),
                    List.of(new PedidoItem(null,produto,BigDecimal.ONE,produto.getPrecoVenda())));
            pedido.setFormaPagamento(forma);return pedido;
        }
        Pedido resgate(UsuarioCliente cliente) {
            return new Pedido(null,cliente,cliente.getCliente(),StatusPedido.PENDENTE,TipoEntrega.RETIRADA,null,
                    LocalDateTime.now(InteligenciaComercialService.FUSO),List.of(PedidoItem.resgate(recompensa)));
        }
        long pagar(UsuarioCliente cliente,FormaPagamento forma) {
            long id=vendas.finalizar(compra(cliente,forma,TipoEntrega.RETIRADA));entregar(id);return id;
        }
        void entregar(long pedido) {
            for(var status:List.of(StatusPedido.PREPARANDO,StatusPedido.PRONTO,StatusPedido.ENTREGUE)) vendas.transicionar(pedido,status);
        }
        long saldo(UsuarioCliente cliente) { return fidelidade.carteira(cliente).saldo(); }
        long bairro() {
            LogisticaService l=new LogisticaService();l.salvar(new BairroEntrega(null,chave,new BigDecimal("3.00"),25,true));
            return bairro=l.bairros(false).stream().filter(b -> b.nome().equals(chave)).findFirst().orElseThrow().id();
        }
        long parceiro() {
            LogisticaService l=new LogisticaService();l.salvar(new Entregador(null,chave,null,null,true));
            return parceiro=l.entregadores(false).stream().filter(e -> e.nome().equals(chave)).findFirst().orElseThrow().id();
        }
        @Override public void close() {
            ConnectionFactory.executarEmTransacao(c -> {
                for(var cliente:clientes) {
                    long id=cliente.getCliente().getId();
                    executar(c,"DELETE i FROM pedido_itens i JOIN pedidos p ON p.id=i.pedido_id WHERE p.contato_id=?",id);
                    executar(c,"DELETE FROM pedidos WHERE contato_id=?",id);
                    executar(c,"DELETE FROM usuarios WHERE id=?",cliente.getId());
                    executar(c,"DELETE FROM contatos WHERE id=?",id);
                }
                for(var p:List.of(produto,recompensa)) {
                    executar(c,"DELETE FROM movimentacoes_estoque WHERE produto_id=?",p.getId());
                    executar(c,"DELETE FROM produtos WHERE id=?",p.getId());
                }
                if (bairro!=null) executar(c,"DELETE FROM bairros_entrega WHERE id=?",bairro);
                if (parceiro!=null) executar(c,"DELETE FROM entregadores WHERE id=?",parceiro);
                return null;
            });
        }
    }
}
