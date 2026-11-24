package com.salgaderia.app.service;

import com.salgaderia.app.dao.impl.ConfiguracaoLojaDAOImpl;
import com.salgaderia.app.model.*;
import com.salgaderia.app.model.enums.*;
import com.salgaderia.app.service.impl.ProdutoEstoqueServiceImpl;
import com.salgaderia.app.util.ConnectionFactory;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SALGADERIA_TESTE_MYSQL", matches="true")
class VitrineV170IntegracaoTest {
    @Test void persisteMinimoSemAlterarExpediente() throws SQLException {
        var dao = new ConfiguracaoLojaDAOImpl();
        try (var c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                var anterior = dao.buscar(c, false);
                dao.definirPedidoMinimo(c, new BigDecimal("35.40"));
                assertEquals(new ConfiguracaoLoja(anterior.abertaManualmente(), anterior.abertura(), anterior.fechamento(),
                        anterior.respeitarHorarioAutomatico(), new BigDecimal("35.40")), dao.buscar(c, false));
                dao.salvarExpediente(c, new ConfiguracaoLoja(true, LocalTime.of(7,0), LocalTime.of(19,0), false));
                assertEquals(new BigDecimal("35.40"), dao.buscar(c, false).pedidoMinimoEntrega());
            } finally { c.rollback(); }
        }
    }

    @Test void categoriaOpcionaisMinimoEFluxoDeCancelamento() {
        var estoque = new ProdutoEstoqueServiceImpl();
        var vendas = VendaTeste.vendas();
        String sufixo = UUID.randomUUID().toString();
        String categoria = "Pizzas " + sufixo;
        var produto = new Produto(null,null,"Pizza " + sufixo,null,new BigDecimal("17.00"),BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,null);
        var segundo = new Produto(null,null,"Pizza 2 " + sufixo,null,BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,null);
        var cliente = new Cliente(null,"Ana",null,null,null,"Rua Flores",null);
        var outro = new Cliente(null,"Bia",null,null,null,"Rua Flores",null);
        var ana = VendaTeste.cadastrar("Ana", "ana-"+sufixo,"senha-teste",cliente);
        var bia = VendaTeste.cadastrar("Bia", "bia-"+sufixo,"senha-teste",outro);
        produto.setOpcionais(List.of(
                new ProdutoOpcional(null,"Borda Recheada","Catupiry",new BigDecimal("3.00"),0,1),
                new ProdutoOpcional(null,"Borda Recheada","Cheddar",new BigDecimal("4.00"),0,1),
                new ProdutoOpcional(null,"Adicionais","Azeitona",new BigDecimal("2.00"),1,2),
                new ProdutoOpcional(null,"Adicionais","Tomate",new BigDecimal("2.00"),1,2)));
        try {
            estoque.salvarCatalogo(produto,"  " + categoria + "  ",null);
            ConnectionFactory.executarEmTransacao(c -> {
                executar(c,"UPDATE categorias SET ativa=FALSE WHERE id=?",produto.getCategoria().getId()); return null;
            });
            assertFalse(estoque.listarCategoriasAtivas().stream().anyMatch(c -> categoria.equals(c.getNome())));
            estoque.salvarCatalogo(segundo,categoria.toUpperCase(Locale.ROOT),null);
            assertEquals(produto.getCategoria().getId(),segundo.getCategoria().getId());
            assertTrue(estoque.listarCategoriasAtivas().stream().anyMatch(c -> categoria.equals(c.getNome())));
            assertTrue(estoque.atualizar(produto,new BigDecimal("3")));
            var catalogo = estoque.listar().stream().filter(p -> p.getId().equals(produto.getId())).findFirst().orElseThrow().getOpcionais();
            assertEquals(4,catalogo.size());
            var bordas = catalogo.stream().filter(o -> o.grupoNome().startsWith("Borda")).map(ProdutoOpcional::id).toList();
            var adicionais = catalogo.stream().filter(o -> o.grupoNome().equals("Adicionais")).map(ProdutoOpcional::id).toList();
            assertThrows(IllegalArgumentException.class, () -> ProdutoPersonalizacaoService.montar(produto,catalogo,Set.copyOf(bordas),null,BigDecimal.ONE));
            assertEquals(new BigDecimal("19.00"),ProdutoPersonalizacaoService.montar(produto,catalogo,Set.copyOf(adicionais),null,BigDecimal.ONE).getPrecoUnitario());
            var item = ProdutoPersonalizacaoService.montar(produto,catalogo,Set.of(bordas.get(0),adicionais.get(0))," sem cebola ",BigDecimal.ONE);
            assertEquals(new BigDecimal("20.00"),item.getPrecoUnitario());
            var semOpcionais = new PedidoItem(null,produto,BigDecimal.ONE,produto.getPrecoVenda());
            assertTrue(assertThrows(IllegalArgumentException.class, () -> vendas.finalizar(pedido(ana,TipoEntrega.ENTREGA,semOpcionais)))
                    .getMessage().contains("3,00"));
            var adulterado = new PedidoItem(null,produto,BigDecimal.ONE,produto.getPrecoVenda(),"sem cebola",null,item.getOpcionaisIds(),item.getAdicionaisResumo());
            assertThrows(IllegalArgumentException.class, () -> vendas.finalizar(pedido(ana,TipoEntrega.RETIRADA,adulterado)));
            assertEquals(0,new BigDecimal("3").compareTo(estoque.buscarPorId(produto.getId()).orElseThrow().getEstoqueAtual()));

            produto.setOpcionais(catalogo.stream().map(o -> o.id().equals(bordas.get(0))
                    ? new ProdutoOpcional(o.id(),o.grupoNome(),o.nome(),new BigDecimal("5.00"),o.gratisAteLimite(),o.maximoEscolhas()) : o).toList());
            estoque.salvarCatalogo(produto,categoria,null);
            assertThrows(IllegalArgumentException.class, () -> vendas.finalizar(pedido(ana,TipoEntrega.ENTREGA,item)));
            produto.setOpcionais(catalogo);
            estoque.salvarCatalogo(produto,categoria,null);
            long entrega = vendas.finalizar(pedido(ana,TipoEntrega.ENTREGA,item));
            var fila = vendas.listarFila().stream().filter(p -> p.id()==entrega).findFirst().orElseThrow();
            assertEquals(List.of("1 × " + produto.getNome() + "\nAdicionais: Azeitona\nBorda Recheada: Catupiry\nObservação: sem cebola"),fila.detalhesPreparo());
            assertThrows(IllegalArgumentException.class,() -> vendas.cancelarDoCliente(bia,entrega));
            vendas.cancelarDoCliente(ana,entrega);
            assertThrows(IllegalArgumentException.class,() -> vendas.cancelarDoCliente(ana,entrega));
            assertTrue(vendas.listarFila().stream().noneMatch(p -> p.id()==entrega));
            var historico = vendas.listarDoCliente(ana).get(0);
            assertEquals(StatusPedido.CANCELADO,historico.getStatus());
            assertEquals(item.getAdicionaisResumo(),historico.getItens().get(0).getAdicionaisResumo());
            assertEquals(item.getOpcionaisIds(),historico.getItens().get(0).getOpcionaisIds());
            assertEquals(0,new BigDecimal("3").compareTo(estoque.buscarPorId(produto.getId()).orElseThrow().getEstoqueAtual()));

            for (var tipo : List.of(TipoEntrega.RETIRADA,TipoEntrega.LOCAL)) {
                long id = vendas.finalizar(pedido(ana,tipo,semOpcionais));
                vendas.transicionar(id,StatusPedido.PREPARANDO);
                assertThrows(IllegalArgumentException.class,() -> vendas.cancelarDoCliente(ana,id));
            }
        } finally {
            ConnectionFactory.executarEmTransacao(c -> {
                executar(c,"DELETE i FROM pedido_itens i JOIN pedidos p ON p.id=i.pedido_id WHERE p.usuario_id=?",ana.getId());
                executar(c,"DELETE FROM pedidos WHERE usuario_id=?",ana.getId());
                if (produto.getId()!=null) {
                    executar(c,"DELETE FROM movimentacoes_estoque WHERE produto_id=?",produto.getId());
                    executar(c,"DELETE FROM produtos WHERE id=?",produto.getId());
                }
                if (segundo.getId()!=null) executar(c,"DELETE FROM produtos WHERE id=?",segundo.getId());
                executar(c,"DELETE FROM categorias WHERE nome=?",categoria);
                for (var conta : List.of(ana,bia)) {
                    executar(c,"DELETE FROM usuarios WHERE id=?",conta.getId());
                    executar(c,"DELETE FROM contatos WHERE id=?",conta.getCliente().getId());
                }
                return null;
            });
        }
    }

    private static Pedido pedido(UsuarioCliente cliente, TipoEntrega modalidade, PedidoItem item) {
        var p = new Pedido(null,cliente,cliente.getCliente(),StatusPedido.PENDENTE,modalidade,
                modalidade.exigeEndereco() ? cliente.getCliente().getEndereco() : null,VendaTeste.AGORA,List.of(item));
        if (modalidade == TipoEntrega.ENTREGA) p.setBairroId(VendaTeste.bairroCentro());
        p.setFormaPagamento(FormaPagamento.DINHEIRO);
        return p;
    }

    private static void executar(Connection c, String sql, Object valor) throws SQLException {
        try (var ps = c.prepareStatement(sql)) { ps.setObject(1,valor); ps.executeUpdate(); }
    }
}
