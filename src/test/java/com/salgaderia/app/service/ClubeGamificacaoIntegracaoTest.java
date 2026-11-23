package com.salgaderia.app.service;

import com.salgaderia.app.dao.impl.*;
import com.salgaderia.app.model.*;
import com.salgaderia.app.model.enums.*;
import com.salgaderia.app.service.impl.*;
import com.salgaderia.app.service.pix.*;
import com.salgaderia.app.util.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Testes transacionais da v1.6.0, executados apenas no banco local de desenvolvimento. */
@EnabledIfEnvironmentVariable(named="SALGADERIA_TESTE_MYSQL",matches="true")
class ClubeGamificacaoIntegracaoTest {
    @Test void clubePreservaCotacaoAtivaRenovaCancelaERecalculaCheckout() throws Exception {
        try(var fixture=new Cenario()){
            var admin=new UsuarioAdmin(2L,"Admin","toor","não usada",true);
            var clube=new ClubeServiceImpl();
            var cliente=fixture.cliente();
            var planos=clube.vitrine(cliente).planos();
            assertEquals(1,planos.size());assertEquals(PlanoAssinatura.NOME_VIP,planos.get(0).nome());
            long planoId=planos.get(0).id();
            Pedido pedido=fixture.pedido(cliente,1);
            assertEquals("NENHUM",fixture.vendas.simular(pedido,null).estrategia());
            clube.solicitar(cliente,planoId);
            assertThrows(IllegalArgumentException.class,()->clube.solicitar(cliente,planoId));
            var pendencia=clube.vitrine(cliente).pendencia();
            assertEquals(PlanoAssinatura.MENSALIDADE_VIP,pendencia.valor());
            assertEquals("NENHUM",fixture.vendas.simular(pedido,null).estrategia());
            clube.confirmar(admin,cliente.getId(),pendencia.id());
            assertThrows(IllegalArgumentException.class,()->clube.confirmar(admin,cliente.getId(),pendencia.id()));
            var ativa=clube.vitrine(cliente).assinatura();
            assertTrue(ativa.ativa(LocalDate.now(ClubeServiceImpl.FUSO)));assertEquals(PlanoAssinatura.DESCONTO_VIP,ativa.descontoVigente());
            assertTrue(clube.painel(admin).ativos().stream().anyMatch(a->a.usuarioId()==cliente.getId()));
            assertThrows(IllegalArgumentException.class,()->clube.solicitar(cliente,planoId));
            var desconto=fixture.vendas.simular(pedido,null);
            assertEquals("ASSINANTE",desconto.estrategia());assertEquals(new BigDecimal("1.50"),desconto.desconto());
            assertEquals("ASSINANTE",fixture.vendas.simular(pedido,"UIKFOMI10").estrategia());
            var entrega=new Pedido(null,cliente,cliente.getCliente(),StatusPedido.PENDENTE,TipoEntrega.ENTREGA,"Endereço teste",
                    LocalDateTime.now(),fixture.pedido(cliente,2).getItens());
            entrega.setFormaPagamento(FormaPagamento.DINHEIRO);
            var comFrete=fixture.vendas.simular(entrega,null);
            assertEquals(0,comFrete.taxaEntrega().signum());
            assertEquals(new BigDecimal("17.00"),comFrete.total());
            long idEntrega=fixture.vendas.finalizar(entrega,null,comFrete);
            var gravado=fixture.vendas.listarDoCliente(cliente).stream().filter(p->p.getId()==idEntrega).findFirst().orElseThrow();
            assertEquals(0,gravado.getTaxaEntrega().signum());assertEquals(0,new BigDecimal("17.00").compareTo(gravado.calcularTotal()));
            var pdv=new Pedido(null,admin,cliente.getCliente(),StatusPedido.PENDENTE,TipoEntrega.RETIRADA,null,
                    LocalDateTime.now(),pedido.getItens());
            assertEquals(new BigDecimal("8.50"),fixture.vendas.simular(pdv,null).total());

            Clock relogio=Clock.fixed(ativa.validaAte().minusDays(2).atStartOfDay(ClubeServiceImpl.FUSO).toInstant(),ClubeServiceImpl.FUSO);
            var clubeFuturo=new ClubeServiceImpl(new ClubeDAOImpl(),new UsuarioDAOImpl(new ContatoDAOImpl()),relogio);
            clubeFuturo.solicitar(cliente,planoId);
            var renovacao=clubeFuturo.vitrine(cliente).pendencia();assertEquals(PlanoAssinatura.MENSALIDADE_VIP,renovacao.valor());
            clubeFuturo.confirmar(admin,cliente.getId(),renovacao.id());
            assertEquals(ativa.validaAte().plusMonths(1),clubeFuturo.vitrine(cliente).assinatura().validaAte());
            clube.cancelar(cliente);
            assertTrue(clube.vitrine(cliente).assinatura().cancelada());
            assertFalse(clube.painel(admin).ativos().stream().anyMatch(a->a.usuarioId()==cliente.getId()));
            assertEquals("NENHUM",fixture.vendas.simular(pedido,null).estrategia());
            assertThrows(IllegalArgumentException.class,()->fixture.vendas.finalizar(pedido,null,desconto));
        }
    }

    @Test void mensalidadeLegadaMantemReferenciaEValorMasAtivaBeneficioVip(){
        try(var fixture=new Cenario()){
            var admin=new UsuarioAdmin(2L,"Admin","toor","não usada",true);
            var servico=new ClubeServiceImpl();var cliente=fixture.cliente();
            long idVip=servico.vitrine(cliente).planos().get(0).id();
            long idLegado=ConnectionFactory.executarEmTransacao(c->{
                var dao=new ClubeDAOImpl();
                var legado=dao.listarPlanos(c,false).stream().filter(p->!p.ativo()).findFirst().orElseThrow();
                dao.prepararAssinatura(c,cliente.getId(),idVip);
                dao.abrirMensalidade(c,cliente.getId(),legado);
                return legado.id();
            });
            var emitida=servico.vitrine(cliente).pendencia();
            assertEquals(idLegado,emitida.planoId());assertNotEquals(PlanoAssinatura.MENSALIDADE_VIP,emitida.valor());
            servico.confirmar(admin,cliente.getId(),emitida.id());
            assertNull(servico.vitrine(cliente).pendencia());
            var ativa=servico.vitrine(cliente).assinatura();
            assertEquals(idVip,ativa.planoId());assertEquals(PlanoAssinatura.DESCONTO_VIP,ativa.descontoVigente());
            ConnectionFactory.executarEmTransacao(c->{
                try(var ps=c.prepareStatement("SELECT plano_id,valor,status FROM mensalidades_clube WHERE id=?")){
                    ps.setLong(1,emitida.id());
                    try(var rs=ps.executeQuery()){
                        assertTrue(rs.next());assertEquals(idLegado,rs.getLong(1));
                        assertEquals(emitida.valor(),rs.getBigDecimal(2));assertEquals("CONFIRMADA",rs.getString(3));
                    }
                }
                return null;
            });
        }
    }

    @Test void freteEPontosEmDobroAplicamBeneficiosReaisSemAcumulo() throws Exception {
        try(var fixture=new Cenario()){
            var recebeFrete=fixture.cliente();
            long anterior=fixture.vendas.finalizar(fixture.pedido(recebeFrete,3));fixture.entregar(anterior);
            String frete=jogo(1).girar(recebeFrete,anterior).cupom();
            assertThrows(IllegalArgumentException.class,()->fixture.vendas.simular(fixture.pedido(recebeFrete,3),frete));
            var entrega=new Pedido(null,recebeFrete,recebeFrete.getCliente(),StatusPedido.PENDENTE,TipoEntrega.ENTREGA,"Endereço de teste",
                    LocalDateTime.now(),List.of(new PedidoItem(null,fixture.produto,new BigDecimal("3"),fixture.produto.getPrecoVenda())));
            entrega.setFormaPagamento(FormaPagamento.DINHEIRO);
            var antes=fixture.vendas.simular(entrega,null);
            if(antes.taxaEntrega().signum()>0){
                var depois=fixture.vendas.simular(entrega,frete);
                assertEquals("FRETE",depois.estrategia());
                assertEquals(0,antes.taxaEntrega().compareTo(depois.desconto()));
                long id=fixture.vendas.finalizar(entrega,frete,depois);
                assertEquals(0,new BigDecimal("30.00").compareTo(fixture.vendas.listarDoCliente(recebeFrete).stream()
                        .filter(p->p.getId()==id).findFirst().orElseThrow().calcularTotal()));
            }else assertThrows(IllegalArgumentException.class,()->fixture.vendas.simular(entrega,frete));

            var recebePontos=fixture.cliente();
            long primeiro=fixture.vendas.finalizar(fixture.pedido(recebePontos,6));fixture.entregar(primeiro);
            var premio=jogo(2).girar(recebePontos,primeiro);
            assertFalse(premio.vip(),"R$ 60,00 exatos não liberam o modo VIP");
            var proximo=fixture.pedido(recebePontos,3);
            var cotado=fixture.vendas.simular(proximo,premio.cupom());
            assertEquals("PONTOS_DOBRO",cotado.estrategia());assertEquals(0,cotado.desconto().signum());
            long id=fixture.vendas.finalizar(proximo,premio.cupom(),cotado);
            assertEquals(10,jogo(2).perfil(recebePontos).pontos());
            fixture.entregar(id);
            assertEquals(30,jogo(2).perfil(recebePontos).pontos());
        }
    }

    @Test void pixRealSoLiberaPedidoComAprovacaoCompativelERetomaCobrancaComMesmaChave() throws Exception {
        try(var fixture=new Cenario()){
            UsuarioCliente cliente=fixture.cliente(),outro=fixture.cliente();
            var gateway=new PixControlado();
            var pix=new PixPagamentoService(gateway);
            var vendas=VendaTeste.vendas(pix);
            var pedido=fixture.pedido(cliente,7);
            pedido.setFormaPagamento(FormaPagamento.PIX);pedido.setPagadorPix("comprador@example.invalid","19119119100");
            long id=vendas.finalizar(pedido);
            assertEquals(StatusPagamento.AGUARDANDO_PIX,pedido.getStatusPagamento());
            assertFalse(pedido.isRoletaLiberada());
            assertTrue(vendas.listarFila().stream().noneMatch(p->p.id()==id));
            assertThrows(IllegalArgumentException.class,()->pix.consultar(outro,id));
            assertThrows(IllegalStateException.class,()->vendas.transicionar(id,StatusPedido.PREPARANDO));

            gateway.status="sem-rede";
            var contingencia=pix.consultar(cliente,id);
            assertNotNull(contingencia.payload());assertFalse(contingencia.confirmarDemo());
            assertEquals(PixPagamentoService.Estado.INDISPONIVEL,contingencia.estado());
            assertThrows(IllegalArgumentException.class,()->pix.confirmarOffline(cliente,id));
            assertThrows(IllegalArgumentException.class,()->PixPagamentoService.demonstracao().confirmarOffline(cliente,id));
            assertThrows(IllegalStateException.class,()->vendas.transicionar(id,StatusPedido.CANCELADO));

            gateway.status="pending";
            var pendente=pix.consultar(cliente,id);
            assertEquals(2,gateway.postagens.size());
            assertEquals(gateway.postagens.get(0),gateway.postagens.get(1));
            assertEquals(new BigDecimal("70.00"),pendente.valor());
            assertEquals(PixPagamentoService.Estado.AGUARDANDO,pendente.estado());
            gateway.status="approved-errado";
            assertThrows(PixGatewayService.RespostaInvalida.class,()->pix.consultar(cliente,id));
            assertTrue(vendas.listarFila().stream().noneMatch(p->p.id()==id));
            gateway.status="approved";
            assertEquals(PixPagamentoService.Estado.PAGO,pix.consultar(cliente,id).estado());
            assertEquals(PixPagamentoService.Estado.PAGO,pix.consultar(cliente,id).estado());
            assertEquals(2,gateway.postagens.size());
            var fila=vendas.listarFila().stream().filter(p->p.id()==id).findFirst().orElseThrow();
            assertTrue(fila.pixReal());assertEquals(StatusPagamento.PAGO,fila.statusPagamento());
            assertTrue(vendas.listarDoCliente(cliente).get(0).isRoletaLiberada());
            assertTrue(jogo(3).girar(cliente,id).vip());
        }
    }

    @Test void cancelamentoPixVerificadoDevolveEstoqueEModoDemoNaoGeraReceita() throws Exception {
        try(var fixture=new Cenario()){
            var usuario=fixture.cliente();
            var gateway=new PixControlado();
            var pix=new PixPagamentoService(gateway);
            var vendaReal=VendaTeste.vendas(pix);
            var pedido=fixture.pedido(usuario,2);
            pedido.setFormaPagamento(FormaPagamento.PIX);pedido.setPagadorPix("comprador@example.invalid","19119119100");
            long id=vendaReal.finalizar(pedido);
            pix.consultar(usuario,id);
            gateway.status="cancelled";
            assertEquals(PixPagamentoService.Estado.CANCELADO,pix.consultar(usuario,id).estado());
            assertEquals(0,new BigDecimal("200.000").compareTo(ConnectionFactory.executarEmTransacao(c->
                    new ProdutoDAOImpl().buscarPorId(c,fixture.produto.getId(),false).orElseThrow().getEstoqueAtual())));
            assertFalse(vendaReal.listarDoCliente(usuario).stream().filter(p->p.getId()==id).findFirst().orElseThrow().isRoletaLiberada());

            var demo=PixPagamentoService.demonstracao();
            var vendaDemo=VendaTeste.vendas(demo);
            var simulacao=fixture.pedido(usuario,2);simulacao.setFormaPagamento(FormaPagamento.PIX);
            var financeiro=new RelatorioFinanceiroServiceImpl();
            LocalDateTime ini=LocalDateTime.now().minusDays(1),fim=LocalDateTime.now().plusDays(1);
            var antes=financeiro.calcular(ini,fim);
            long demoId=vendaDemo.finalizar(simulacao);
            assertTrue(demo.consultar(usuario,demoId).confirmarDemo());
            assertEquals(PixPagamentoService.Estado.PAGO,demo.confirmarOffline(usuario,demoId).estado());
            assertTrue(vendaDemo.listarFila().stream().anyMatch(p->p.id()==demoId&&p.pixDemonstracao()));
            fixture.entregar(demoId);
            assertEquals(antes,financeiro.calcular(ini,fim));
            var premioDemo=jogo(3).girar(usuario,demoId);
            assertEquals(PremioRoleta.Tipo.CUPOM_10,premioDemo.tipo());
            assertNull(premioDemo.cupom());
            assertEquals(0,jogo(3).perfil(usuario).pontos());
        }
    }

    private static final class PixControlado implements PixGatewayService {
        final String id=Long.toUnsignedString(new java.security.SecureRandom().nextLong());
        final List<Solicitacao> postagens=new ArrayList<>();
        String status="pending";
        @Override public Cobranca criar(Solicitacao solicitacao){postagens.add(solicitacao);return resposta();}
        @Override public Cobranca consultar(String idConsultado){assertEquals(id,idConsultado);return resposta();}
        private Cobranca resposta(){
            if(status.equals("sem-rede"))throw new Indisponivel("erro de teste");
            Solicitacao s=postagens.get(0);
            boolean divergente=status.equals("approved-errado");
            return new Cobranca(id,divergente?"approved":status,status.equals("pending")?"000201TESTE":null,
                    divergente?s.valor().add(BigDecimal.ONE):s.valor(),"BRL","pix",s.referencia());
        }
    }

    @Test void observerRoletaEPremiosSaoPersistentesExclusivosEIdempotentes() throws Exception {
        try(var fixture=new Cenario()){
            UsuarioCliente cliente=fixture.cliente(),outro=fixture.cliente();
            GamificacaoService jogo=jogo(3);
            long primeiro=fixture.vendas.finalizar(fixture.pedido(cliente,100));
            assertEquals(Set.of("PRIMEIRO_PEDIDO"),selos(jogo,cliente));
            assertThrows(IllegalArgumentException.class,()->jogo.girar(cliente,primeiro));
            fixture.entregar(primeiro);
            assertThrows(IllegalArgumentException.class,()->jogo.girar(outro,primeiro));
            PremioRoleta premio;
            var executor=Executors.newFixedThreadPool(2);
            try{
                var corrida1=executor.submit(()->jogo.girar(cliente,primeiro));
                var corrida2=executor.submit(()->jogo.girar(cliente,primeiro));
                premio=corrida1.get(10,TimeUnit.SECONDS);var repetido=corrida2.get(10,TimeUnit.SECONDS);
                assertNotEquals(premio.novo(),repetido.novo());
                assertEquals(premio.cupom(),repetido.cupom());
            }finally{executor.shutdownNow();}
            assertEquals(PremioRoleta.Tipo.CUPOM_15,premio.tipo());assertTrue(premio.vip());
            assertEquals(List.of(premio.cupom()),jogo.perfil(cliente).cupons().stream().map(CupomRoleta::codigo).toList());
            assertEquals(5,Duration.between(premio.registradoEm(),premio.validoAte()).toDays());
            assertEquals(new BigDecimal("25.00"),jogo.perfil(cliente).cupons().get(0).minimo());
            assertThrows(IllegalArgumentException.class,()->fixture.vendas.simular(fixture.pedido(outro,3),premio.cupom()));
            assertThrows(IllegalArgumentException.class,()->fixture.vendas.simular(fixture.pedido(cliente,2),premio.cupom()));
            assertTrue(selos(jogo,cliente).contains("REI_DO_CENTO"));

            Pedido segundo=fixture.pedido(cliente,3);
            var desconto=fixture.vendas.simular(segundo,premio.cupom());
            assertEquals(new BigDecimal("4.50"),desconto.desconto());
            long usado=fixture.vendas.finalizar(segundo,premio.cupom(),desconto);
            assertThrows(IllegalArgumentException.class,()->fixture.vendas.simular(fixture.pedido(cliente,3),premio.cupom()));
            fixture.vendas.transicionar(usado,StatusPedido.CANCELADO);
            assertTrue(jogo.perfil(cliente).cupons().stream().anyMatch(c->c.codigo().equals(premio.cupom())));
            long idSegundo=fixture.vendas.finalizar(fixture.pedido(cliente,3),premio.cupom(),desconto);
            fixture.entregar(idSegundo);
            assertThrows(IllegalArgumentException.class,()->jogo.girar(cliente,idSegundo));
            assertTrue(jogo.perfil(cliente).cupons().isEmpty());
            long terceiro=fixture.vendas.finalizar(fixture.pedido(cliente,1));fixture.entregar(terceiro);
            assertEquals(Set.of("PRIMEIRO_PEDIDO","REI_DO_CENTO","CLIENTE_FIEL"),selos(jogo,cliente));
            assertEquals(30,jogo.perfil(cliente).pontos());
            ConnectionFactory.executarEmTransacao(c->{Cenario.excluir(c,"UPDATE roleta_premios SET criado_em=DATE_SUB(NOW(),INTERVAL 25 HOUR) WHERE pedido_id=?",primeiro);return null;});
            var extra=jogo(4).girar(cliente,terceiro);assertTrue(extra.giroExtra());assertFalse(extra.vip());
            var definitivo=jogo(0).girarExtra(cliente,terceiro);assertEquals(PremioRoleta.Tipo.BRINDE_MINI,definitivo.tipo());
            assertThrows(IllegalArgumentException.class,()->jogo(0).girarExtra(cliente,terceiro));
            var brinde=fixture.vendas.simular(fixture.pedido(cliente,3),definitivo.cupom());
            assertEquals("BRINDE",brinde.estrategia());assertEquals("Mini porção surpresa",brinde.brinde());
            assertEquals(0,brinde.desconto().signum());
            ConnectionFactory.executarEmTransacao(c->{Cenario.excluir(c,"UPDATE cupons SET valido_ate=NOW() WHERE usado_pedido_id IS NULL AND usuario_exclusivo_id=?",cliente.getId());return null;});
            assertThrows(IllegalArgumentException.class,()->fixture.vendas.simular(fixture.pedido(cliente,3),definitivo.cupom()));
            assertTrue(jogo.perfil(cliente).cupons().isEmpty());
        }
    }

    private static GamificacaoService jogo(int indice){
        return new GamificacaoServiceImpl(new GamificacaoDAOImpl(),new UsuarioDAOImpl(new ContatoDAOImpl()),new Random(){
            @Override public int nextInt(int limite){return indice;}
        });
    }
    private static Set<String> selos(GamificacaoService jogo,UsuarioCliente cliente){
        return jogo.perfil(cliente).selos().stream().map(ConquistaCliente::codigo).collect(Collectors.toSet());
    }

    private static final class Cenario implements AutoCloseable {
        final String token=UUID.randomUUID().toString().replace("-","");
        final PedidoVendaService vendas=VendaTeste.vendas();
        final List<UsuarioCliente> usuarios=new ArrayList<>();
        final Produto produto;

        Cenario(){
            produto=ConnectionFactory.executarEmTransacao(c->{
                long id;
                try(var ps=c.prepareStatement("INSERT INTO categorias(nome) VALUES (?)",Statement.RETURN_GENERATED_KEYS)){
                    ps.setString(1,"Fritos teste "+token);ps.executeUpdate();
                    try(var chaves=ps.getGeneratedKeys()){assertTrue(chaves.next());id=chaves.getLong(1);}
                }
                var p=new Produto(null,new Categoria(id,"Fritos teste "+token,null),"Produto teste "+token,null,new BigDecimal("10.00"),
                        BigDecimal.ONE,new BigDecimal("200"),BigDecimal.ZERO,null);
                p.setId(new ProdutoDAOImpl().inserir(c,p));return p;
            });
        }
        UsuarioCliente cliente(){
            var u=VendaTeste.cadastrar("Cliente teste","g"+usuarios.size()+token,"senha-teste",
                    new Cliente(null,"Cliente teste "+token,null,null,null,null,null));
            usuarios.add(u);return u;
        }
        Pedido pedido(UsuarioCliente u,int quantidade){
            var p=new Pedido(null,u,u.getCliente(),StatusPedido.PENDENTE,TipoEntrega.RETIRADA,null,LocalDateTime.now(),
                    List.of(new PedidoItem(null,produto,BigDecimal.valueOf(quantidade),produto.getPrecoVenda())));
            p.setFormaPagamento(FormaPagamento.DINHEIRO);return p;
        }
        void entregar(long id){
            for(StatusPedido status:List.of(StatusPedido.PREPARANDO,StatusPedido.PRONTO,StatusPedido.ENTREGUE))vendas.transicionar(id,status);
        }
        @Override public void close(){
            ConnectionFactory.executarEmTransacao(c->{
                for(var u:usuarios){
                    excluir(c,"DELETE i FROM pedido_itens i JOIN pedidos p ON p.id=i.pedido_id WHERE p.usuario_id=?",u.getId());
                    excluir(c,"DELETE FROM pedidos WHERE usuario_id=?",u.getId());
                    excluir(c,"DELETE FROM usuarios WHERE id=?",u.getId());
                    excluir(c,"DELETE FROM contatos WHERE id=?",u.getCliente().getId());
                }
                excluir(c,"DELETE FROM movimentacoes_estoque WHERE produto_id=?",produto.getId());
                excluir(c,"DELETE FROM produtos WHERE id=?",produto.getId());
                excluir(c,"DELETE FROM categorias WHERE id=?",produto.getCategoria().getId());return null;
            });
        }
        private static void excluir(Connection c,String sql,long id)throws SQLException {
            try(var ps=c.prepareStatement(sql)){ps.setLong(1,id);ps.executeUpdate();}
        }
    }
}
