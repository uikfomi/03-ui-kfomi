package com.salgaderia.app.service;

import com.salgaderia.app.dao.impl.*;
import com.salgaderia.app.exception.EstoqueInsuficienteException;
import com.salgaderia.app.exception.ProdutoIndisponivelException;
import com.salgaderia.app.model.*;
import com.salgaderia.app.model.enums.*;
import com.salgaderia.app.service.impl.*;
import com.salgaderia.app.service.pix.PixPagamentoService;
import com.salgaderia.app.util.ConnectionFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SALGADERIA_TESTE_MYSQL",matches="true")
class OperacaoV162IntegracaoTest {
    @Test void singletonPersisteExpedienteSemRegravarChaveManual() throws SQLException {
        var dao=new ConfiguracaoLojaDAOImpl();
        try(Connection c=ConnectionFactory.getConnection()){
            c.setAutoCommit(false);
            try{
                var minimo = dao.buscar(c,false).pedidoMinimoEntrega();
                dao.definirAberturaManual(c,false);
                dao.salvarExpediente(c,new ConfiguracaoLoja(true,LocalTime.of(18,0),LocalTime.of(1,0),true));
                assertEquals(new ConfiguracaoLoja(false,LocalTime.of(18,0),LocalTime.of(1,0),true,minimo),dao.buscar(c,true));
                dao.definirAberturaManual(c,true);
                assertEquals(new ConfiguracaoLoja(true,LocalTime.of(18,0),LocalTime.of(1,0),true,minimo),dao.buscar(c,true));
            }finally{c.rollback();}
        }
    }

    @Test void checkoutFechadoAceitaSoAgendamentoComExpedienteEEntregaExigeDespacho(){
        var estoque=new ProdutoEstoqueServiceImpl();
        var fechada=new ConfiguracaoLoja(false,LocalTime.of(18,0),LocalTime.of(1,0),true,BigDecimal.ZERO);
        var vendas=VendaTeste.vendas(new MovimentacaoEstoqueDAOImpl(),PixPagamentoService.demonstracao(),fechada);
        String chave=UUID.randomUUID().toString().replace("-","");
        var cliente=new Cliente(null,"Cliente "+chave,null,null,"85999999999","Rua das Flores",null);
        var usuario=VendaTeste.cadastrar("Cliente teste","oper"+chave,"senha-teste",cliente);
        var produto=new Produto(null,null,"Coxinha "+chave,null,new BigDecimal("5.00"),BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,null);
        var admin=new UsuarioAdmin(2L,"Admin","admin","não utilizada",true);
        List<Long> pdvIds=new ArrayList<>();
        try{
            estoque.cadastrar(produto);
            Pedido entrega=pedido(usuario,cliente,produto,TipoEntrega.ENTREGA);
            assertThrows(IllegalArgumentException.class,()->vendas.finalizar(entrega));
            entrega.setAgendadoPara(VendaTeste.AGORA.toLocalDate().plusDays(1).atTime(1,0));
            assertThrows(IllegalArgumentException.class,()->vendas.finalizar(entrega));
            assertNull(entrega.getId());assertTrue(vendas.listarDoCliente(usuario).isEmpty());
            entrega.setAgendadoPara(VendaTeste.AGORA.toLocalDate().plusDays(1).atTime(0,30));
            var cotacao=vendas.simular(entrega,null);
            assertTrue(estoque.alternarDisponibilidade(produto.getId()));
            assertThrows(ProdutoIndisponivelException.class,()->vendas.simular(entrega,null));
            assertThrows(ProdutoIndisponivelException.class,()->vendas.finalizar(entrega,null,cotacao));
            var vendaPdv=pedido(admin,null,produto,TipoEntrega.RETIRADA);
            assertThrows(ProdutoIndisponivelException.class,()->vendas.finalizarPdv(vendaPdv,FormaPagamento.DINHEIRO,BigDecimal.TEN));
            assertNull(entrega.getId());assertNull(vendaPdv.getId());
            assertTrue(estoque.atualizar(produto));
            assertFalse(estoque.buscarPorId(produto.getId()).orElseThrow().isDisponivelVenda());
            assertTrue(estoque.alternarDisponibilidade(produto.getId()));
            pdvIds.add(vendas.finalizarPdv(vendaPdv,FormaPagamento.DINHEIRO,BigDecimal.TEN));
            long id=vendas.finalizar(entrega,null,cotacao);
            vendas.transicionar(id,StatusPedido.PREPARANDO);vendas.transicionar(id,StatusPedido.PRONTO);
            assertThrows(IllegalStateException.class,()->vendas.transicionar(id,StatusPedido.ENTREGUE));
            assertThrows(IllegalArgumentException.class,()->vendas.transicionar(id,StatusPedido.SAIU_ENTREGA," "));
            assertThrows(IllegalArgumentException.class,()->vendas.transicionar(id,StatusPedido.SAIU_ENTREGA,"a".repeat(101)));
            vendas.transicionar(id,StatusPedido.SAIU_ENTREGA,"  José  ");
            assertTrue(vendas.listarFila().stream().anyMatch(p->p.id()==id&&p.status()==StatusPedido.SAIU_ENTREGA&&"José".equals(p.nomeEntregador())));
            assertEquals("José",vendas.listarDoCliente(usuario).get(0).getNomeEntregador());
            vendas.transicionar(id,StatusPedido.ENTREGUE);
            assertEquals(StatusPedido.ENTREGUE,vendas.listarDoCliente(usuario).get(0).getStatus());
            assertEquals("José",vendas.listarDoCliente(usuario).get(0).getNomeEntregador());
            assertTrue(vendas.listarFila().stream().noneMatch(p->p.id()==id));

            for(TipoEntrega modalidade:List.of(TipoEntrega.RETIRADA,TipoEntrega.LOCAL)){
                Pedido pedido=pedido(usuario,cliente,produto,modalidade);
                pedido.setAgendadoPara(VendaTeste.AGORA.toLocalDate().atTime(20,0));
                long outro=vendas.finalizar(pedido);
                vendas.transicionar(outro,StatusPedido.PREPARANDO);vendas.transicionar(outro,StatusPedido.PRONTO);
                assertThrows(IllegalStateException.class,()->vendas.transicionar(outro,StatusPedido.SAIU_ENTREGA,"José"));
                vendas.transicionar(outro,StatusPedido.ENTREGUE);
            }
            Pedido cancelar=pedido(usuario,cliente,produto,TipoEntrega.LOCAL);
            cancelar.setAgendadoPara(VendaTeste.AGORA.toLocalDate().atTime(20,0));
            vendas.transicionar(vendas.finalizar(cancelar),StatusPedido.CANCELADO);
            assertEquals(0,estoque.buscarPorId(produto.getId()).orElseThrow().getEstoqueAtual().signum());
            assertThrows(EstoqueInsuficienteException.class,()->estoque.registrarPerda(produto.getId(),BigDecimal.ONE,"perda"));
            assertTrue(estoque.atualizar(produto,new BigDecimal("3")));
            assertEquals(0,new BigDecimal("3").compareTo(estoque.buscarPorId(produto.getId()).orElseThrow().getEstoqueAtual()));
        }finally{
            ConnectionFactory.executarEmTransacao(c->{
                excluir(c,"DELETE i FROM pedido_itens i JOIN pedidos p ON p.id=i.pedido_id WHERE p.usuario_id=?",usuario.getId());
                excluir(c,"DELETE FROM pedidos WHERE usuario_id=?",usuario.getId());
                for(long pdv:pdvIds){excluir(c,"DELETE FROM pedido_itens WHERE pedido_id=?",pdv);excluir(c,"DELETE FROM pedidos WHERE id=?",pdv);}
                if(produto.getId()!=null){
                    excluir(c,"DELETE FROM movimentacoes_estoque WHERE produto_id=?",produto.getId());
                    excluir(c,"DELETE FROM produtos WHERE id=?",produto.getId());
                }
                excluir(c,"DELETE FROM usuarios WHERE id=?",usuario.getId());
                excluir(c,"DELETE FROM contatos WHERE id=?",cliente.getId());return null;
            });
        }
    }
    private static Pedido pedido(Usuario usuario,Cliente cliente,Produto produto,TipoEntrega tipo){
        var pedido=new Pedido(null,usuario,cliente,StatusPedido.PENDENTE,tipo,tipo.exigeEndereco()?cliente.getEndereco():null,VendaTeste.AGORA,
                List.of(new PedidoItem(null,produto,BigDecimal.ONE,produto.getPrecoVenda())));
        pedido.setFormaPagamento(FormaPagamento.DINHEIRO);return pedido;
    }
    private static void excluir(Connection c,String sql,long id) throws SQLException {
        try(var ps=c.prepareStatement(sql)){ps.setLong(1,id);ps.executeUpdate();}
    }
}
