package com.salgaderia.app.controller;

import com.salgaderia.app.model.*;
import com.salgaderia.app.model.enums.*;
import com.salgaderia.app.service.*;
import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SALGADERIA_TESTE_JAVAFX",matches="true")
class VitrineJavaFXTest {
    @BeforeAll static void iniciar() throws Exception {
        CountDownLatch latch=new CountDownLatch(1);
        try { Platform.startup(()->{Platform.setImplicitExit(false);latch.countDown();}); }
        catch(IllegalStateException jaIniciado){Platform.runLater(latch::countDown);}
        assertTrue(latch.await(15,TimeUnit.SECONDS));
    }
    @Test void catalogoCarrinhoFiltrosEntregaEPedidos() throws Exception {
        Categoria categoria=new Categoria(1L,"Assados",null);
        Produto produto=new Produto(1L,categoria,"Coxinha de frango","Frango cremoso",new BigDecimal("8.00"),new BigDecimal("3"),BigDecimal.ZERO,BigDecimal.ONE,null);
        Produto bloqueado=new Produto(2L,null,"Pastel",null,new BigDecimal("5"),BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ONE,null);
        bloqueado.setDisponivelVenda(false);
        Cliente cliente=new Cliente(10L,"Ana",null,null,"123","Rua das Flores",null);
        UsuarioCliente usuario=new UsuarioCliente(20L,"Ana","ana","senha",true,cliente);
        PedidoItem item=new PedidoItem(1L,produto,BigDecimal.ONE,new BigDecimal("8.00"));
        Pedido pendente=new Pedido(101L,usuario,cliente,StatusPedido.PENDENTE,TipoEntrega.RETIRADA,null,LocalDateTime.now(),List.of(item));
        Pedido entregue=new Pedido(102L,usuario,cliente,StatusPedido.ENTREGUE,TipoEntrega.RETIRADA,null,LocalDateTime.now(),List.of(item));
        var estoque=new ProdutosTeste(List.of(produto,bloqueado));var pedidos=new PedidosTeste(List.of(pendente,entregue));var avaliacoes=new AvaliacoesTeste();
        var loaderRef=new java.util.concurrent.atomic.AtomicReference<FXMLLoader>();var stageRef=new java.util.concurrent.atomic.AtomicReference<Stage>();
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/cliente-vitrine.fxml"));
            loader.setControllerFactory(c->vitrineTeste(estoque,pedidos,avaliacoes));
            Parent root=loader.load();Stage stage=new Stage();stage.setScene(new Scene(root,1000,700));stage.show();
            root.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());root.applyCss();
            ((ClienteVitrineController)loader.getController()).definirUsuario(usuario);loaderRef.set(loader);stageRef.set(stage);return null;
        });
        try {
            FXMLLoader loader=loaderRef.get();
            aguardar(()->fx(()->((FlowPane)loader.getNamespace().get("cards")).getChildren().size()==2));
            aguardar(()->fx(()->((Label)loader.getNamespace().get("statusLoja")).getText().contains("Loja Aberta")));
            fx(()->{
                Parent root=stageRef.get().getScene().getRoot();root.applyCss();
                assertTrue(((Button)root.lookup("#adicionar-2")).isDisabled());
                assertNotNull(root.lookup(".imagem-fallback"));
                assertEquals(Set.of("Disponível"),root.lookupAll(".estoque-disponivel").stream().map(n->((Label)n).getText()).collect(java.util.stream.Collectors.toSet()));
                assertEquals("Indisponível no momento",((Label)root.lookup(".estoque-esgotado")).getText());
                assertNotNull(root.lookup("#montarCombo"));assertNotNull(root.lookup("#caixaMisteriosa"));
                assertTrue(root.lookupAll(".produto-estrelas").stream().anyMatch(n->((Label)n).getText().contains("4,5")));
                ((Button)root.lookup("#adicionar-1")).fire();((Button)root.lookup("#adicionar-1")).fire();
                assertEquals("2 itens",((Label)loader.getNamespace().get("contador")).getText());
                assertTrue(((Label)loader.getNamespace().get("total")).getText().contains("16,00"));
                ((RadioButton)loader.getNamespace().get("entrega")).fire();assertEquals("Rua das Flores",((Label)loader.getNamespace().get("endereco")).getText());
                assertTrue(((Button)loader.getNamespace().get("finalizar")).isDisabled());
                assertTrue(((Label)loader.getNamespace().get("mensagemMinimo")).getText().contains("4,00"));
                var bairros = (ComboBox<?>) loader.getNamespace().get("bairroEntrega");
                bairros.getSelectionModel().selectLast();
                assertTrue(((Label)loader.getNamespace().get("mensagemFrete")).getText().contains("Retire"));
                assertTrue(((Button)loader.getNamespace().get("finalizar")).isDisabled());
                bairros.getSelectionModel().selectFirst();
                ((Button)root.lookup("#adicionar-1")).fire();
                ((TextField)loader.getNamespace().get("busca")).setText("FRANGO");return null;
            });
            assertEquals(1,fx(()->((FlowPane)loader.getNamespace().get("cards")).getChildren().size()));
            fx(()->{((TextField)loader.getNamespace().get("cupom")).setText("UIKFOMI10");return null;});
            aguardar(()->fx(()->((Label)loader.getNamespace().get("total")).getText().contains("24,60")));
            fx(()->{
                assertTrue(((Button)loader.getNamespace().get("finalizar")).isDisabled());
                @SuppressWarnings("unchecked") ComboBox<FormaPagamento> pagamento=(ComboBox<FormaPagamento>)loader.getNamespace().get("formaPagamento");
                assertEquals(List.of(FormaPagamento.PIX,FormaPagamento.DINHEIRO,FormaPagamento.CARTAO,FormaPagamento.CARTAO_APP),List.copyOf(pagamento.getItems()));
                pagamento.setValue(FormaPagamento.PIX);
                var pix=(com.salgaderia.app.util.PixPainel)loader.getNamespace().get("pixCheckout");
                assertTrue(pix.isVisible());assertTrue(((Label)pix.getChildren().get(0)).getText().contains("24,60"));
                assertTrue(pix.lookup("#pix-copiar").isVisible());
                pagamento.setValue(FormaPagamento.CARTAO);assertFalse(((Button)loader.getNamespace().get("finalizar")).isDisabled());return null;
            });
            fx(()->{((TextField)loader.getNamespace().get("busca")).setText("nada encontrado");return null;});
            assertTrue(fx(()->((Label)loader.getNamespace().get("mensagemProdutos")).getText().contains("Nenhum salgado encontrado")));
            var timer=fx(()->{
                javafx.animation.Timeline t=new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(50),event->{
                    for(javafx.stage.Window janela:List.copyOf(javafx.stage.Window.getWindows())){
                        if(janela instanceof Stage dialogo && dialogo!=stageRef.get() && dialogo.getScene()!=null){
                            Parent raiz=dialogo.getScene().getRoot();
                            if(raiz instanceof DialogPane pane){
                                NodeBusca.preencherNota(pane);
                                javafx.scene.Node ok=pane.lookupButton(ButtonType.OK);
                                if(ok instanceof Button botao && !botao.isDisabled())botao.fire();
                            }
                        }
                    }
                }));t.setCycleCount(javafx.animation.Animation.INDEFINITE);t.play();return t;
            });
            fx(()->{((Button)loader.getNamespace().get("finalizar")).fire();return null;});
            aguardar(()->pedidos.enviado.get()!=null);
            aguardar(()->fx(()->((Label)loader.getNamespace().get("contador")).getText().equals("0 itens")));
            assertEquals(TipoEntrega.ENTREGA,pedidos.enviado.get().getTipoEntrega());
            assertEquals(1L,pedidos.enviado.get().getBairroId());
            assertEquals(FormaPagamento.CARTAO,pedidos.enviado.get().getFormaPagamento());
            assertEquals("UIKFOMI10",pedidos.codigo.get());
            assertEquals("Rua das Flores",pedidos.enviado.get().getEnderecoEntrega());
            assertEquals(new BigDecimal("3"),pedidos.enviado.get().getItens().get(0).getQuantidade());
            aguardar(()->fx(()->stageRef.get().getScene().getRoot().lookup("#avaliar-102-1")!=null));
            fx(()->{
                Parent root=stageRef.get().getScene().getRoot();
                assertTrue(((Button)root.lookup("#avaliar-101-1")).isDisabled());
                assertFalse(((Button)root.lookup("#avaliar-102-1")).isDisabled());
                ((Button)root.lookup("#avaliar-102-1")).fire();return null;
            });
            aguardar(()->avaliacoes.salva.get()!=null);
            assertEquals(5,avaliacoes.salva.get().getNota());
            fx(()->{timer.stop();return null;});
        } finally {fx(()->{stageRef.get().close();return null;});}
    }
    @Test void fechamentoManualPausaCheckoutImediatoMasOfereceAgendamento() throws Exception {
        var config=new java.util.concurrent.atomic.AtomicReference<>(new ConfiguracaoLoja(false,java.time.LocalTime.of(8,0),java.time.LocalTime.of(23,0),true));
        var produto=new Produto(2L,null,"Pastel",null,BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,null);
        var pedidos=new PedidosTeste(List.of());
        var referencia=new java.util.concurrent.atomic.AtomicReference<Stage>();
        var loader=fx(()->{
            FXMLLoader l=new FXMLLoader(getClass().getResource("/fxml/cliente-vitrine.fxml"));
            l.setControllerFactory(c->new ClienteVitrineController(new ProdutosTeste(List.of(produto)),pedidos,new AvaliacoesTeste(),
                    new com.salgaderia.app.service.impl.ClubeServiceImpl(),new com.salgaderia.app.service.impl.GamificacaoServiceImpl(),
                    com.salgaderia.app.service.pix.PixPagamentoService.demonstracao(),config::get,List::of));
            Stage s=new Stage();s.setScene(new Scene(l.load(),1000,700));s.show();referencia.set(s);
            var cliente=new Cliente(10L,"Ana",null,null,null,null,null);
            ((ClienteVitrineController)l.getController()).definirUsuario(new UsuarioCliente(20L,"Ana","ana","senha",true,cliente));
            return l;
        });
        try{
            var n=loader.getNamespace();
            aguardar(()->fx(()->((Label)n.get("statusLoja")).getText().contains("fechamento manual")&&
                    referencia.get().getScene().getRoot().lookup("#adicionar-2")!=null));
            fx(()->{
                ((Button)referencia.get().getScene().getRoot().lookup("#adicionar-2")).fire();
                @SuppressWarnings("unchecked") var forma=(ComboBox<FormaPagamento>)n.get("formaPagamento");
                forma.setValue(FormaPagamento.DINHEIRO);
                return null;
            });
            aguardar(()->fx(()->((Label)n.get("mensagemDesconto")).getText().startsWith("Desconto:")));
            fx(()->{
                assertTrue(((Button)n.get("finalizar")).isDisabled());
                assertTrue(((Label)n.get("orientacaoLoja")).getText().contains("Agende"));
                ((CheckBox)n.get("agendar")).setSelected(true);
                assertFalse(((Button)n.get("finalizar")).isDisabled());
                ((CheckBox)n.get("agendar")).setSelected(false);
                return null;
            });
            config.set(lojaAberta());
            aguardar(()->fx(()->((Label)n.get("statusLoja")).getText().contains("Loja Aberta")&&
                    !((Button)n.get("finalizar")).isDisabled()));
            assertNull(pedidos.enviado.get());
        }finally{fx(()->{referencia.get().close();return null;});}
    }
    @Test void catalogoVazioNaoExibeProdutosFicticios() throws Exception {
        var loaderRef=new java.util.concurrent.atomic.AtomicReference<FXMLLoader>();var stageRef=new java.util.concurrent.atomic.AtomicReference<Stage>();
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/cliente-vitrine.fxml"));
            loader.setControllerFactory(c->vitrineTeste(new ProdutosTeste(List.of()),new PedidosTeste(List.of()),new AvaliacoesTeste()));
            Parent root=loader.load();Stage stage=new Stage();stage.setScene(new Scene(root,900,650));stage.show();
            Cliente cliente=new Cliente(10L,"Ana",null,null,null,null,null);
            ((ClienteVitrineController)loader.getController()).definirUsuario(new UsuarioCliente(20L,"Ana","ana","senha",true,cliente));
            loaderRef.set(loader);stageRef.set(stage);return null;
        });
        try {
            aguardar(()->fx(()->((Label)loaderRef.get().getNamespace().get("mensagemProdutos")).getText().contains("Nenhum salgado cadastrado")));
            assertEquals(0,fx(()->((FlowPane)loaderRef.get().getNamespace().get("cards")).getChildren().size()));
        } finally {fx(()->{stageRef.get().close();return null;});}
    }
    @Test void vitrineCarregaCategoriaEAbrePersonalizacaoComLimite() throws Exception {
        var categoria = new Categoria(33L,"Pizzas artesanais",null);
        var pizza = new Produto(30L,categoria,"Pizza",null,BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,null);
        pizza.setOpcionais(List.of(new ProdutoOpcional(31L,"Borda Recheada","Cheddar",new BigDecimal("3"),0,1),
                new ProdutoOpcional(32L,"Borda Recheada","Catupiry",new BigDecimal("4"),0,1)));
        var stage = new java.util.concurrent.atomic.AtomicReference<Stage>();
        var loader = new java.util.concurrent.atomic.AtomicReference<FXMLLoader>();
        try {
            fx(()->{
                var l = new FXMLLoader(getClass().getResource("/fxml/cliente-vitrine.fxml"));
                l.setControllerFactory(c->vitrineTeste(new ProdutosTeste(List.of(pizza)),new PedidosTeste(List.of()),new AvaliacoesTeste()));
                Stage janela = new Stage(); janela.setScene(new Scene(l.load(),1100,800)); janela.show();
                loader.set(l); stage.set(janela);
                var cliente = new Cliente(10L,"Ana",null,null,null,null,null);
                ((ClienteVitrineController)l.getController()).definirUsuario(new UsuarioCliente(20L,"Ana","ana","senha",true,cliente));
                return null;
            });
            aguardar(()->fx(()->stage.get().getScene().getRoot().lookup("#adicionar-30")!=null));
            fx(()->{
                var filtro = (ComboBox<?>) loader.get().getNamespace().get("categorias");
                assertEquals(2,filtro.getItems().size());
                filtro.getSelectionModel().select(1);
                assertTrue(filtro.getValue().toString().contains(categoria.getNome()));
                Platform.runLater(()->((Button)stage.get().getScene().getRoot().lookup("#adicionar-30")).fire());
                return null;
            });
            aguardar(()->fx(()->dialogoAberto(stage.get()).map(p->p.lookup("#produto-observacao")!=null).orElse(false)));
            fx(()->{
                var pane = dialogoAberto(stage.get()).orElseThrow();
                ((CheckBox)pane.lookup("#opcional-31")).fire();
                assertTrue(pane.lookup("#opcional-32").isDisabled());
                ((TextArea)pane.lookup("#produto-observacao")).setText("bem assada");
                confirmar(pane); return null;
            });
            aguardar(()->fx(()->((Label)loader.get().getNamespace().get("total")).getText().contains("13,00")));
            assertTrue(fx(()->((javafx.scene.layout.VBox)loader.get().getNamespace().get("itensCarrinho")).lookupAll(".label").stream()
                    .filter(Label.class::isInstance).map(Label.class::cast).anyMatch(l->l.getText().contains("Observação: bem assada"))));
        } finally {
            fx(()->{if (stage.get()!=null) { dialogoAberto(stage.get()).ifPresent(p->p.getScene().getWindow().hide()); stage.get().close(); } return null;});
        }
    }
    @Test void meusPedidosAtualizaStatusAutomaticamente() throws Exception {
        Categoria categoria=new Categoria(1L,"Fritos",null);
        Produto produto=new Produto(1L,categoria,"Coxinha",null,BigDecimal.TEN,BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ONE,null);
        Cliente cliente=new Cliente(10L,"Ana",null,null,"85999999999",null,null);
        UsuarioCliente usuario=new UsuarioCliente(20L,"Ana","ana","senha",true,cliente);
        List<Pedido> lista=new CopyOnWriteArrayList<>();
        lista.add(new Pedido(101L,usuario,cliente,StatusPedido.PENDENTE,TipoEntrega.RETIRADA,null,LocalDateTime.now(),List.of(new PedidoItem(null,produto,BigDecimal.ONE,BigDecimal.TEN))));
        var loaderRef=new java.util.concurrent.atomic.AtomicReference<FXMLLoader>();var stageRef=new java.util.concurrent.atomic.AtomicReference<Stage>();
        fx(()->{FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/cliente-vitrine.fxml"));loader.setControllerFactory(c->vitrineTeste(new ProdutosTeste(List.of()),new PedidosTeste(lista),new AvaliacoesTeste()));Parent root=loader.load();Stage stage=new Stage();stage.setScene(new Scene(root,1000,700));stage.show();((ClienteVitrineController)loader.getController()).definirUsuario(usuario);loaderRef.set(loader);stageRef.set(stage);((Button)loader.getNamespace().get("botaoPedidos")).fire();return null;});
        try{
            aguardar(()->fx(()->stageRef.get().getScene().getRoot().lookup("#cancelar-pedido-101")!=null));
            Pedido emRota=new Pedido(101L,usuario,cliente,StatusPedido.SAIU_ENTREGA,TipoEntrega.ENTREGA,"Rua das Flores",LocalDateTime.now(),
                    List.of(new PedidoItem(null,produto,BigDecimal.ONE,BigDecimal.TEN)));
            emRota.setNomeEntregador("João");lista.set(0,emRota);
            aguardar(()->fx(()->stageRef.get().getScene().getRoot().lookup("#entrega-em-rota-101") instanceof Label l&&l.getText().contains("João")));
            assertNull(fx(()->stageRef.get().getScene().getRoot().lookup("#cancelar-pedido-101")));
            assertNotNull(fx(()->stageRef.get().getScene().getRoot().lookup("#whatsapp-pedido-101")));
        }finally{fx(()->{stageRef.get().close();return null;});}
    }
    @Test void repetePedidoEAgendaConsumoLocal() throws Exception {
        Produto produto=new Produto(1L,null,"Coxinha",null,new BigDecimal("8.00"),BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ONE,null);
        Cliente cliente=new Cliente(10L,"Ana",null,null,null,null,null);UsuarioCliente usuario=new UsuarioCliente(20L,"Ana","ana","senha",true,cliente);
        Pedido anterior=new Pedido(101L,usuario,cliente,StatusPedido.ENTREGUE,TipoEntrega.RETIRADA,null,LocalDateTime.now(),List.of(new PedidoItem(null,produto,BigDecimal.ONE,produto.getPrecoVenda())));
        PedidosTeste pedidos=new PedidosTeste(List.of(anterior));var loader=new java.util.concurrent.atomic.AtomicReference<FXMLLoader>();var stage=new java.util.concurrent.atomic.AtomicReference<Stage>();
        fx(()->{FXMLLoader l=new FXMLLoader(getClass().getResource("/fxml/cliente-vitrine.fxml"));l.setControllerFactory(c->vitrineTeste(new ProdutosTeste(List.of(produto)),pedidos,new AvaliacoesTeste()));Parent root=l.load();Stage s=new Stage();s.setScene(new Scene(root,1100,800));s.show();((ClienteVitrineController)l.getController()).definirUsuario(usuario);loader.set(l);stage.set(s);((Button)l.getNamespace().get("pedirDeSempre")).fire();return null;});
        try{
            aguardar(()->fx(()->((Label)loader.get().getNamespace().get("contador")).getText().equals("1 item")));
            LocalDateTime futuro=LocalDate.now(java.time.ZoneId.of("America/Fortaleza")).plusDays(2).atTime(12,0);
            fx(()->{var n=loader.get().getNamespace();((RadioButton)n.get("local")).fire();((CheckBox)n.get("agendar")).setSelected(true);((DatePicker)n.get("dataAgendamento")).setValue(futuro.toLocalDate());((TextField)n.get("horaAgendamento")).setText(futuro.toLocalTime().toString());@SuppressWarnings("unchecked") ComboBox<FormaPagamento> forma=(ComboBox<FormaPagamento>)n.get("formaPagamento");forma.setValue(FormaPagamento.PIX);return null;});
            aguardar(()->fx(()->!((Button)loader.get().getNamespace().get("finalizar")).isDisabled()));
            Timeline timer=fx(()->{Timeline t=new Timeline(new KeyFrame(Duration.millis(50),e->{for(var w:List.copyOf(javafx.stage.Window.getWindows()))if(w instanceof Stage s&&s!=stage.get()&&s.getScene()!=null&&s.getScene().getRoot() instanceof DialogPane pane){var ok=pane.lookupButton(ButtonType.OK);if(ok instanceof Button b)b.fire();}}));t.setCycleCount(Animation.INDEFINITE);t.play();return t;});
            fx(()->{((Button)loader.get().getNamespace().get("finalizar")).fire();return null;});
            aguardar(()->pedidos.enviado.get()!=null);
            assertEquals(TipoEntrega.LOCAL,pedidos.enviado.get().getTipoEntrega());assertEquals(futuro,pedidos.enviado.get().getAgendadoPara());
            fx(()->{timer.stop();return null;});
        }finally{fx(()->{stage.get().close();return null;});}
    }
    @Test void comboRecalculaPrecoCaixaEscondeSorteioEGaleriaExibeFoto(@TempDir Path pasta) throws Exception {
        Produto empada=new Produto(1L,null,"Empada especial",null,new BigDecimal("8.00"),BigDecimal.ONE,new BigDecimal("12"),BigDecimal.ONE,null);
        Produto molho=new Produto(2L,null,"Molho da casa",null,new BigDecimal("2.00"),BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ONE,null);
        Produto suco=new Produto(3L,new Categoria(12L,"Bebidas",null),"Suco",null,new BigDecimal("5.00"),BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,null);
        Path original=pasta.resolve("foto.png");
        assertTrue(ImageIO.write(new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB),"png",original.toFile()));
        var storage=new FotoAvaliacaoStorage();
        String foto=storage.salvar(original);
        var avaliacoes=new AvaliacoesTeste(){
            public List<AvaliacaoFoto> fotosRecentes(){return List.of(new AvaliacaoFoto(123L,empada.getId(),empada.getNome(),5,foto));}
        };
        var loader=new java.util.concurrent.atomic.AtomicReference<FXMLLoader>();var stage=new java.util.concurrent.atomic.AtomicReference<Stage>();
        try {
            fx(()->{
                FXMLLoader l=new FXMLLoader(getClass().getResource("/fxml/cliente-vitrine.fxml"));
                l.setControllerFactory(c->vitrineTeste(new ProdutosTeste(List.of(empada,molho,suco)),new PedidosTeste(List.of()),avaliacoes));
                Stage s=new Stage();s.setScene(new Scene(l.load(),1100,800));s.show();stage.set(s);loader.set(l);
                Cliente cliente=new Cliente(10L,"Ana",null,null,null,null,null);
                ((ClienteVitrineController)l.getController()).definirUsuario(new UsuarioCliente(20L,"Ana","ana","senha",true,cliente));return null;
            });
            aguardar(()->fx(()->stage.get().getScene().getRoot().lookup("#foto-avaliacao-123")!=null));
            fx(()->{Platform.runLater(()->((Button)loader.get().getNamespace().get("montarCombo")).fire());return null;});
            aguardar(()->fx(()->dialogoAberto(stage.get()).map(p->p.lookup("#combo-previa")!=null).orElse(false)));
            fx(()->{
                DialogPane p=dialogoAberto(stage.get()).orElseThrow();
                selecionar(p,"#combo-principal",0);selecionar(p,"#combo-acompanhamento",1);selecionar(p,"#combo-bebida",0);
                String previa=((Label)p.lookup("#combo-previa")).getText();
                assertTrue(previa.contains("13,50")&&previa.contains("Empada especial + Molho da casa + Suco"));
                confirmar(p);return null;
            });
            aguardar(()->fx(()->((Label)loader.get().getNamespace().get("total")).getText().contains("13,50")));
            fx(()->{Platform.runLater(()->((Button)loader.get().getNamespace().get("caixaMisteriosa")).fire());return null;});
            aguardar(()->fx(()->dialogoAberto(stage.get()).map(p->p.lookup("#caixa-orcamento")!=null).orElse(false)));
            fx(()->{
                DialogPane p=dialogoAberto(stage.get()).orElseThrow();
                ((CheckBox)p.lookup("#excluir-categoria-12")).setSelected(true);
                ((CheckBox)p.lookup("#excluir-produto-2")).setSelected(true);
                // Sem o molho ou a bebida, R$ 40,00 é a primeira faixa preenchível apenas com empadas de R$ 8.
                selecionar(p,"#caixa-orcamento",1);
                confirmar(p);return null;
            });
            aguardar(()->fx(()->dialogoAberto(stage.get()).map(p->p.getContent() instanceof Label).orElse(false)));
            fx(()->{
                DialogPane p=dialogoAberto(stage.get()).orElseThrow();String previa=((Label)p.getContent()).getText();
                assertTrue(previa.contains("Preço fechado: R$ 40,00")||previa.contains("Preço fechado: R$ 40,00"));assertFalse(previa.contains(empada.getNome()));
                confirmar(p);return null;
            });
            aguardar(()->fx(()->((Label)loader.get().getNamespace().get("total")).getText().contains("53,50")));
        }finally{
            try{fx(()->{if(stage.get()!=null){dialogoAberto(stage.get()).ifPresent(p->p.getScene().getWindow().hide());stage.get().close();}return null;});}
            finally{storage.excluir(foto);}
        }
    }
    @Test void clubeExibeVigenciaESelosERoletaAnimaOPremioPersistido() throws Exception {
        Categoria categoria=new Categoria(1L,"Fritos",null);
        Produto produto=new Produto(1L,categoria,"Coxinha",null,BigDecimal.TEN,BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ONE,null);
        Cliente cliente=new Cliente(10L,"Ana",null,null,null,null,null);
        UsuarioCliente usuario=new UsuarioCliente(20L,"Ana","ana","senha",true,cliente);
        Pedido pedido=new Pedido(42L,usuario,cliente,StatusPedido.PENDENTE,TipoEntrega.RETIRADA,null,LocalDateTime.now(),
                List.of(new PedidoItem(null,produto,BigDecimal.ONE,produto.getPrecoVenda())));
        pedido.setRoletaLiberada(true);
        ClubeService clube=new ClubeService(){
            public Vitrine vitrine(UsuarioCliente u){
                return new Vitrine(List.of(new PlanoAssinatura(1L,PlanoAssinatura.NOME_VIP,"Plano pré-pago",PlanoAssinatura.MENSALIDADE_VIP,PlanoAssinatura.DESCONTO_VIP,true)),
                        new AssinaturaCliente(u.getId(),u.getNome(),1,PlanoAssinatura.NOME_VIP,LocalDate.now().plusDays(10),PlanoAssinatura.DESCONTO_VIP,false),null,LocalDate.now());
            }
            public Painel painel(UsuarioAdmin u){throw new UnsupportedOperationException();}
            public void solicitar(UsuarioCliente u,long p){throw new UnsupportedOperationException();}
            public void cancelar(UsuarioCliente u){throw new UnsupportedOperationException();}
            public void confirmar(UsuarioAdmin u,long c,long m){throw new UnsupportedOperationException();}
        };
        GamificacaoService jogo=new GamificacaoService(){
            public Perfil perfil(UsuarioCliente u){return new Perfil(List.of(new ConquistaCliente("PRIMEIRO_PEDIDO","Primeiro Pedido","Fez um pedido",LocalDateTime.now())),
                    20,List.of(new CupomRoleta("FOME-CUPOM-TESTE","Cupom 10% OFF",new BigDecimal("25"),LocalDateTime.now().plusHours(20))));}
            public PremioRoleta girar(UsuarioCliente u,long id){return new PremioRoleta(id,PremioRoleta.Tipo.CUPOM_10,"FOME-CUPOM-TESTE",true);}
        };
        var loader=new java.util.concurrent.atomic.AtomicReference<FXMLLoader>();var stage=new java.util.concurrent.atomic.AtomicReference<Stage>();
        fx(()->{
            FXMLLoader l=new FXMLLoader(getClass().getResource("/fxml/cliente-vitrine.fxml"));
            l.setControllerFactory(c->new ClienteVitrineController(new ProdutosTeste(List.of(produto)),new PedidosTeste(List.of(pedido)),new AvaliacoesTeste(),clube,jogo,com.salgaderia.app.service.pix.PixPagamentoService.demonstracao()));
            Stage s=new Stage();s.setScene(new Scene(l.load(),1100,800));s.show();stage.set(s);loader.set(l);
            ((ClienteVitrineController)l.getController()).definirUsuario(usuario);((Button)l.getNamespace().get("botaoClube")).fire();return null;
        });
        try {
            aguardar(()->fx(()->stage.get().getScene().getRoot().lookup("#clube-status") instanceof Label l&&l.getText().contains("Ativa")));
            fx(()->{
                assertTrue(stage.get().getScene().getRoot().lookupAll(".label").stream().filter(Label.class::isInstance)
                        .map(Label.class::cast).anyMatch(l->l.getText().contains("Primeiro Pedido")));
                assertTrue(((Label)stage.get().getScene().getRoot().lookup("#cupomUrgente")).getText().contains("expira em"));
                ((Button)loader.get().getNamespace().get("botaoPedidos")).fire();return null;
            });
            aguardar(()->fx(()->stage.get().getScene().getRoot().lookup("#roleta-42")!=null));
            fx(()->{Platform.runLater(()->((Button)stage.get().getScene().getRoot().lookup("#roleta-42")).fire());return null;});
            aguardar(()->fx(()->dialogoAberto(stage.get()).isPresent()));
            fx(()->{confirmar(dialogoAberto(stage.get()).orElseThrow());return null;});
            aguardar(()->fx(()->dialogoAberto(stage.get()).map(p->p.lookup("#roleta-resultado") instanceof Label l&&l.getText().contains("Você ganhou")).orElse(false)));
            fx(()->{
                DialogPane p=dialogoAberto(stage.get()).orElseThrow();
                assertEquals("FOME-CUPOM-TESTE",((TextField)p.lookup(".text-field")).getText());
                ((Button)p.lookupButton(ButtonType.CLOSE)).fire();return null;
            });
        }finally{fx(()->{dialogoAberto(stage.get()).ifPresent(p->p.getScene().getWindow().hide());stage.get().close();return null;});}
    }
    @Test void dialogoPixConsultaNovamenteELiberaRoletaSemConfirmacaoOffline() throws Exception {
        var consultas=new java.util.concurrent.atomic.AtomicInteger();
        var escolheuRoleta=new java.util.concurrent.atomic.AtomicBoolean();
        var referencias=new java.util.concurrent.atomic.AtomicReference<Stage>();
        fx(()->{
            Stage dona=new Stage();dona.setScene(new Scene(new javafx.scene.layout.VBox(),480,360));dona.show();referencias.set(dona);
            Platform.runLater(()->escolheuRoleta.set(com.salgaderia.app.util.PagamentoPixUi.abrir(dona,42,()->{
                var estado=consultas.incrementAndGet()==1?com.salgaderia.app.service.pix.PixPagamentoService.Estado.AGUARDANDO:
                        com.salgaderia.app.service.pix.PixPagamentoService.Estado.PAGO;
                return new com.salgaderia.app.service.pix.PixPagamentoService.Consulta(estado,new BigDecimal("72.15"),
                        estado==com.salgaderia.app.service.pix.PixPagamentoService.Estado.AGUARDANDO?"000201TESTE":null,false,
                        estado==com.salgaderia.app.service.pix.PixPagamentoService.Estado.AGUARDANDO?"Aguardando confirmação automática…":"Pagamento confirmado.");
            },()->{throw new AssertionError("Offline não pode confirmar PIX real.");})));return null;
        });
        try {
            aguardar(()->fx(()->dialogoAberto(referencias.get()).map(p->p.lookup("#pix-copiar")!=null&&p.lookup("#pix-copiar").isVisible()).orElse(false)));
            fx(()->{var p=dialogoAberto(referencias.get()).orElseThrow();assertFalse(p.lookup("#pix-confirmar-offline").isVisible());return null;});
            aguardar(()->fx(()->dialogoAberto(referencias.get()).map(p->p.lookup("#pix-abrir-roleta").isVisible()).orElse(false)));
            fx(()->{((Button)dialogoAberto(referencias.get()).orElseThrow().lookup("#pix-abrir-roleta")).fire();return null;});
            aguardar(escolheuRoleta::get);assertEquals(2,consultas.get());
        }finally{fx(()->{referencias.get().close();return null;});}
    }
    private static Optional<DialogPane> dialogoAberto(Stage dona){
        for(var w:List.copyOf(javafx.stage.Window.getWindows()))
            if(w instanceof Stage s&&s.getOwner()==dona&&s.getScene()!=null&&s.getScene().getRoot() instanceof DialogPane pane)return Optional.of(pane);
        return Optional.empty();
    }
    private static void selecionar(DialogPane pane,String seletor,int indice){
        ((ComboBox<?>)pane.lookup(seletor)).getSelectionModel().select(indice);
    }
    private static void confirmar(DialogPane p){
        var tipo=p.getButtonTypes().stream().filter(t->t.getButtonData()==ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow();
        ((Button)p.lookupButton(tipo)).fire();
    }
    private static ClienteVitrineController vitrineTeste(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AvaliacaoService avaliacoes){
        return new ClienteVitrineController(produtos,pedidos,avaliacoes,new com.salgaderia.app.service.impl.ClubeServiceImpl(),
                new com.salgaderia.app.service.impl.GamificacaoServiceImpl(),com.salgaderia.app.service.pix.PixPagamentoService.demonstracao(),VitrineJavaFXTest::lojaAberta, () -> List.of(new BairroEntrega(1L,"Centro",new BigDecimal("3.00"),25,true)));
    }
    private static ConfiguracaoLoja lojaAberta(){
        return new ConfiguracaoLoja(true,java.time.LocalTime.of(8,0),java.time.LocalTime.of(23,0),false);
    }
    private static <T> T fx(Callable<T> c) throws Exception{FutureTask<T> t=new FutureTask<>(c);Platform.runLater(t);return t.get(15,TimeUnit.SECONDS);}
    private static void aguardar(Callable<Boolean> condicao) throws Exception{long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(System.nanoTime()<fim){if(condicao.call())return;Thread.sleep(25);}fail("Operação assíncrona não concluiu");}
    private record ProdutosTeste(List<Produto> dados) implements ProdutoEstoqueService {
        public List<Categoria> listarCategorias(){return listarCategoriasAtivas();}
        public List<Categoria> listarCategoriasAtivas(){return dados.stream().map(Produto::getCategoria).filter(java.util.Objects::nonNull).distinct().toList();}
        public void salvarCatalogo(Produto p,String c,BigDecimal q){throw new UnsupportedOperationException();} public List<Produto> listar(){return dados;}public List<Produto> listarEstoqueBaixo(){return List.of();}public Optional<Produto> buscarPorId(long id){return dados.stream().filter(p->p.getId()==id).findFirst();}
        public long cadastrar(Produto p){throw new UnsupportedOperationException();}public boolean atualizar(Produto p){throw new UnsupportedOperationException();}public boolean atualizar(Produto p,BigDecimal q){throw new UnsupportedOperationException();} public boolean alternarDisponibilidade(long id){throw new UnsupportedOperationException();} public boolean excluir(long id){throw new UnsupportedOperationException();}
        public void entradaLote(long p,long f,BigDecimal q,String o){throw new UnsupportedOperationException();}public void registrarPerda(long p,BigDecimal q,String o){throw new UnsupportedOperationException();}
    }
    private static class PedidosTeste implements PedidoVendaService {
        public void confirmarCartaoApp(long pedido,UsuarioAdmin admin){throw new UnsupportedOperationException();}
        public void cancelarDoCliente(UsuarioCliente u,long id){throw new UnsupportedOperationException();}
        public long contarFilaCozinha(){return 0;}
        public List<PedidoItem> repetirUltimoPedido(UsuarioCliente u){return dados.stream().filter(p->p.getStatus()==StatusPedido.ENTREGUE).findFirst().orElseThrow().getItens();}
        final List<Pedido> dados;final java.util.concurrent.atomic.AtomicReference<Pedido> enviado=new java.util.concurrent.atomic.AtomicReference<>();final java.util.concurrent.atomic.AtomicReference<String> codigo=new java.util.concurrent.atomic.AtomicReference<>();
        PedidosTeste(List<Pedido> dados){this.dados=dados;}
        public List<PedidoFila> listarFila(){return List.of();} public long finalizarPdv(Pedido p,FormaPagamento f,BigDecimal r){throw new UnsupportedOperationException();}
        public List<Pedido> listarDoCliente(UsuarioCliente u){return dados;}public long finalizar(Pedido p){enviado.set(p);return 77;}public void transicionar(long id,StatusPedido s){throw new UnsupportedOperationException();} public String despachar(long id, long parceiro){throw new UnsupportedOperationException();}
        public com.salgaderia.app.service.desconto.ResultadoDesconto simular(Pedido p,String c){var cupom="UIKFOMI10".equals(c)?new com.salgaderia.app.service.desconto.Cupom(c,BigDecimal.TEN,BigDecimal.ZERO):null;return new com.salgaderia.app.service.desconto.CalculadoraDescontos().calcular(new com.salgaderia.app.service.desconto.ContextoDesconto(p.getItens(),0,cupom,BigDecimal.ZERO, p.getTipoEntrega()==TipoEntrega.ENTREGA?new BigDecimal("3.00"):BigDecimal.ZERO)).comPrazo(25);}
        public long finalizar(Pedido p,String c,com.salgaderia.app.service.desconto.ResultadoDesconto d){codigo.set(c);return finalizar(p);}
        public long finalizarPdv(Pedido p,FormaPagamento f,BigDecimal r,String c,com.salgaderia.app.service.desconto.ResultadoDesconto d){return finalizarPdv(p,f,r);}
    }
    private static class NodeBusca {
        @SuppressWarnings("unchecked") static void preencherNota(DialogPane pane){
            javafx.scene.Node node=pane.lookup(".combo-box");if(node instanceof ComboBox<?> combo)((ComboBox<Integer>)combo).setValue(5);
        }
    }
    private static class AvaliacoesTeste implements AvaliacaoService {
        public List<AvaliacaoResumo> listar(){return List.of();}
        public List<AvaliacaoFoto> fotosRecentes(){return List.of();}
        final java.util.concurrent.atomic.AtomicReference<Avaliacao> salva=new java.util.concurrent.atomic.AtomicReference<>();
        public Map<Long,BigDecimal> mediasPorProduto(){return Map.of(1L,new BigDecimal("4.5"));}public long salvar(Avaliacao a,java.nio.file.Path foto){salva.set(a);return 1;}
        public boolean responder(Usuario u,long id,String r){throw new UnsupportedOperationException();}public boolean excluir(Usuario u,long id){throw new UnsupportedOperationException();}
    }
}
