package com.salgaderia.app.controller;

import com.salgaderia.app.dao.ContatoDAO.FiltroContato;
import com.salgaderia.app.dao.impl.ContatoDAOImpl;
import com.salgaderia.app.exception.EstoqueInsuficienteException;
import com.salgaderia.app.exception.ProdutoIndisponivelException;
import java.time.LocalTime;
import com.salgaderia.app.model.*;
import com.salgaderia.app.model.enums.*;
import com.salgaderia.app.model.comanda.*;
import com.salgaderia.app.service.*;
import com.salgaderia.app.service.impl.*;
import com.salgaderia.app.service.desconto.ResultadoDesconto;
import com.salgaderia.app.service.pix.PixPagamentoService;
import com.salgaderia.app.util.Navegacao;
import com.salgaderia.app.util.FotoAvaliacaoUi;
import com.salgaderia.app.util.PixPainel;
import com.salgaderia.app.util.TelaAutenticada;
import com.salgaderia.app.util.WhatsAppUi;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/** Painel coordena os serviços; nenhuma consulta SQL é executada pela interface. */
public class AdminDashboardController implements TelaAutenticada {
    @FXML private BorderPane raiz;
    @FXML private Label titulo, usuarioNome, badgePendentes, avisoPedido;
    @FXML private VBox conteudo, alertasRecentes;
    private final LogisticaService logistica = new LogisticaService();
    private final OperacaoLojaService operacaoLoja = new OperacaoLojaService();
    private final ProdutoEstoqueService produtos;
    private final PedidoVendaService pedidos;
    private final AgendaService agenda;
    private final RelatorioFinanceiroService financeiro;
    private final AvaliacaoService avaliacoes;
    private final ComandaService comandas;
    private final ClubeService clube;
    private final PixPagamentoService pagamentosPix;
    private Usuario usuario;
    private long telaAtual;
    private final WhatsAppLinkService whatsapp = new WhatsAppLinkService();
    private final List<Runnable> alertasAtraso = new ArrayList<>();
    private final Timeline notificacoes = new Timeline(new KeyFrame(Duration.seconds(5),e->{atualizarNotificacoes();conciliarPix();alertasAtraso.forEach(Runnable::run);}));
    private final Set<Long> pedidosConhecidos = new HashSet<>();
    private boolean primeiraConsulta = true, consultaNotificacoes;
    private boolean notificacoesAtivas;
    private boolean conciliandoPix;
    private final CheckBox notificarStatus = new CheckBox("Notificar via WhatsApp ao mudar status");
    private final Map<Long,ItemVenda> carrinho = new LinkedHashMap<>();
    private static final Locale BR=Locale.forLanguageTag("pt-BR");
    private record ItemVenda(Produto produto,int quantidade) { }
    private record DadosPdv(List<Produto> produtos,List<Contato> contatos) { }
    private record DadosEstoque(List<Produto> produtos,List<Categoria> categorias,List<Contato> fornecedores) { }
    private record DadosFinanceiros(RelatorioFinanceiroService.Relatorio totais,Map<String,BigDecimal> produtos,Map<String,BigDecimal> categorias,List<RepasseEntregador> repasses) { }
    private record DadosMesa(Comanda comanda,List<Produto> produtos) { }

    public AdminDashboardController(){this(new ProdutoEstoqueServiceImpl(),new PedidoVendaServiceImpl(),new AgendaServiceImpl(new ContatoDAOImpl()),new RelatorioFinanceiroServiceImpl(),new AvaliacaoServiceImpl());}
    public AdminDashboardController(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AgendaService agenda,RelatorioFinanceiroService financeiro,AvaliacaoService avaliacoes){
        this(produtos,pedidos,agenda,financeiro,avaliacoes,new ComandaServiceImpl());
    }
    public AdminDashboardController(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AgendaService agenda,RelatorioFinanceiroService financeiro,AvaliacaoService avaliacoes,ComandaService comandas){
        this(produtos,pedidos,agenda,financeiro,avaliacoes,comandas,new ClubeServiceImpl());
    }
    public AdminDashboardController(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AgendaService agenda,RelatorioFinanceiroService financeiro,AvaliacaoService avaliacoes,ComandaService comandas,ClubeService clube){
        this(produtos,pedidos,agenda,financeiro,avaliacoes,comandas,clube,new PixPagamentoService());
    }
    public AdminDashboardController(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AgendaService agenda,RelatorioFinanceiroService financeiro,AvaliacaoService avaliacoes,ComandaService comandas,ClubeService clube,PixPagamentoService pagamentosPix){
        this.produtos=Objects.requireNonNull(produtos);this.pedidos=Objects.requireNonNull(pedidos);this.agenda=Objects.requireNonNull(agenda);this.financeiro=Objects.requireNonNull(financeiro);this.avaliacoes=Objects.requireNonNull(avaliacoes);this.comandas=Objects.requireNonNull(comandas);
        this.clube=Objects.requireNonNull(clube);this.pagamentosPix=Objects.requireNonNull(pagamentosPix);
    }
    @FXML private void initialize(){
        notificarStatus.setSelected(true);notificarStatus.setId("notificar-status");
        notificacoes.setCycleCount(Timeline.INDEFINITE);
        raiz.sceneProperty().addListener((obs,anterior,cena)->{
            if(cena==null){notificacoesAtivas=false;notificacoes.stop();return;}
            cena.windowProperty().addListener((o,velha,janela)->{
                if(janela==null){notificacoesAtivas=false;notificacoes.stop();}
                else janela.showingProperty().addListener((x,era,visivel)->{if(!visivel){notificacoesAtivas=false;notificacoes.stop();}});
            });
        });
    }
    @Override public void definirUsuario(Usuario usuario){
        if(usuario==null||!usuario.isAtivo()||!usuario.podeAcessarPainelGerencial())throw new SecurityException("Acesso gerencial restrito");
        this.usuario=usuario;usuarioNome.setText(usuario.getNome());notificacoesAtivas=true;atualizarNotificacoes();conciliarPix();notificacoes.playFromStart();pdv();
    }
    private void atualizarNotificacoes(){
        if(!notificacoesAtivas||consultaNotificacoes)return;consultaNotificacoes=true;
        Task<List<PedidoFila>> tarefa=new Task<>(){@Override protected List<PedidoFila> call(){return pedidos.listarFila();}};
        tarefa.setOnSucceeded(e->{consultaNotificacoes=false;if(!notificacoesAtivas)return;
            List<PedidoFila> pendentes=tarefa.getValue().stream().filter(p->p.status()==StatusPedido.PENDENTE).toList();
            badgePendentes.setText("Pedidos pendentes: "+pendentes.size());
            boolean mudou=false;
            for(PedidoFila pedido:pendentes)if(pedidosConhecidos.add(pedido.id())&&!primeiraConsulta){
                mudou=true;
                if(alertasRecentes.getChildren().size()==1 && alertasRecentes.getChildren().get(0) instanceof Label l && l.getText().equals("Nenhum alerta recente."))alertasRecentes.getChildren().clear();
                String aviso=(pedido.pixReal()?"PIX confirmado · pedido #":pedido.pixDemonstracao()?"Pedido simulado #":"Novo pedido #")+pedido.id();
                alertasRecentes.getChildren().add(0,new Label(aviso+" · "+pedido.cliente()));
                avisoPedido.setText(aviso);
                if(alertasRecentes.getChildren().size()>5)alertasRecentes.getChildren().remove(5);
            }
            primeiraConsulta=false;
            if(mudou&&"Fila de pedidos".equals(titulo.getText()))fila();
        });
        tarefa.setOnFailed(e->{consultaNotificacoes=false;if(notificacoesAtivas)badgePendentes.setText("Pedidos pendentes: indisponível");});
        Thread t=new Thread(tarefa,"admin-notificacoes");t.setDaemon(true);t.start();
    }
    private void conciliarPix(){
        if(!notificacoesAtivas||conciliandoPix||!pagamentosPix.usaMercadoPago())return;
        conciliandoPix=true;
        Task<Void> tarefa=new Task<>(){@Override protected Void call(){pagamentosPix.reconciliarPendentes();return null;}};
        tarefa.setOnSucceeded(e->{conciliandoPix=false;atualizarNotificacoes();});
        tarefa.setOnFailed(e->conciliandoPix=false);
        Thread t=new Thread(tarefa,"admin-conciliacao-pix");t.setDaemon(true);t.start();
    }
    private long iniciarTela(String nome){alertasAtraso.clear();titulo.setText(nome);conteudo.getChildren().setAll(estado("Carregando..."));return ++telaAtual;}
    private <T> void carregar(long versao,Supplier<T> operacao,Consumer<T> sucesso){
        Task<T> tarefa=new Task<>(){@Override protected T call(){return operacao.get();}};
        tarefa.setOnSucceeded(e->{if(versao==telaAtual)sucesso.accept(tarefa.getValue());});
        tarefa.setOnFailed(e->{if(versao==telaAtual)conteudo.getChildren().setAll(estado("Não foi possível carregar os dados. Verifique a conexão e tente novamente."));});
        Thread t=new Thread(tarefa,"admin-consulta");t.setDaemon(true);t.start();
    }
    private <T> void executar(Supplier<T> operacao,Consumer<T> sucesso){
        executar(operacao,sucesso,()->{});
    }
    private <T> void executar(Supplier<T> operacao,Consumer<T> sucesso,Runnable aoFalhar){
        raiz.setDisable(true);Task<T> tarefa=new Task<>(){@Override protected T call(){return operacao.get();}};
        tarefa.setOnSucceeded(e->{raiz.setDisable(false);sucesso.accept(tarefa.getValue());});
        tarefa.setOnFailed(e->{raiz.setDisable(false);Throwable erro=tarefa.getException();
            if(erro instanceof EstoqueInsuficienteException estoque)avisar(Alert.AlertType.WARNING,"Estoque insuficiente",estoque.getNomeProduto()+": disponível "+estoque.getEstoqueDisponivel()+", solicitado "+estoque.getQuantidadeSolicitada()+".");
            else if(erro instanceof IllegalArgumentException || erro instanceof IllegalStateException)avisar(Alert.AlertType.WARNING,"Operação não concluída",erro.getMessage());
            else avisar(Alert.AlertType.ERROR,"Operação não concluída","Verifique os dados, vínculos e conexão com o banco.");aoFalhar.run();});
        Thread t=new Thread(tarefa,"admin-operacao");t.setDaemon(true);t.start();
    }

    @FXML private void pdv(){
        long versao=iniciarTela("PDV rápido");
        carregar(versao,()->new DadosPdv(produtos.listar(),agenda.buscar(FiltroContato.CLIENTES,"")),dados->montarPdv(dados));
    }
    private void montarPdv(DadosPdv dados){
        conteudo.getChildren().clear();
        Map<Long,Produto> catalogo=new HashMap<>();dados.produtos().forEach(p->catalogo.put(p.getId(),p));
        carrinho.replaceAll((id,item)->new ItemVenda(catalogo.getOrDefault(id,item.produto()),item.quantidade()));
        Label subtitulo=texto("Selecione os produtos e conclua a venda no balcão.","subtitle");
        FlowPane grade=new FlowPane(10,10);grade.setPrefWrapLength(520);
        VBox listaCarrinho=new VBox(8);Label total=new Label();total.getStyleClass().add("admin-valor");
        ComboBox<Contato> cliente=new ComboBox<>();cliente.setId("pdv-cliente");cliente.setPromptText("Cliente opcional");cliente.setMaxWidth(Double.MAX_VALUE);cliente.getItems().setAll(dados.contatos());
        cliente.setConverter(new StringConverter<>(){public String toString(Contato c){return c==null?"Venda sem identificação":c.getNome();}public Contato fromString(String s){return null;}});
        Button limparCliente=botao("Limpar cliente",()->cliente.getSelectionModel().clearSelection(),"button-outline");
        ComboBox<FormaPagamento> forma=new ComboBox<>();forma.setId("pdv-forma");forma.getItems().setAll(FormaPagamento.DINHEIRO,FormaPagamento.PIX,FormaPagamento.CARTAO_CREDITO,FormaPagamento.CARTAO_DEBITO);forma.setValue(FormaPagamento.DINHEIRO);forma.setMaxWidth(Double.MAX_VALUE);
        TextField recebido=new TextField();recebido.setId("pdv-recebido");recebido.setPromptText("Valor recebido em dinheiro");Label troco=new Label();troco.setId("pdv-troco");
        TextField cupom=new TextField();cupom.setId("pdv-cupom");cupom.setPromptText("Cupom opcional");
        Label mensagemDesconto=new Label();mensagemDesconto.setWrapText(true);
        ResultadoDesconto[] desconto={null};long[] geracao={0};Button[] botaoConcluir={null};
        PixPainel pix=new PixPainel();pix.setId("pdv-pix");
        Runnable atualizarTroco=()->{
            recebido.setDisable(!forma.getValue().isPermiteTroco());
            pix.atualizar(forma.getValue()==FormaPagamento.PIX,desconto[0]==null?null:desconto[0].total(),null,
                    desconto[0]==null?"O código PIX aparece quando o total estiver disponível.":"Confira o recebimento no banco antes de finalizar.");
            try {BigDecimal valor=decimal(recebido.getText());troco.setText(forma.getValue().isPermiteTroco()?(desconto[0]==null?"Calcule o total.":"Troco: "+moeda(valor.subtract(desconto[0].total()).max(BigDecimal.ZERO))):"Sem troco para esta forma.");}
            catch(RuntimeException e){troco.setText(forma.getValue().isPermiteTroco()?"Informe o valor recebido.":"Sem troco para esta forma.");}
        };
        Runnable[] atualizador=new Runnable[1];
        Runnable atualizar=()->{
            listaCarrinho.getChildren().clear();BigDecimal soma=BigDecimal.ZERO;
            for(ItemVenda item:carrinho.values()){
                soma=soma.add(item.produto().getPrecoVenda().multiply(BigDecimal.valueOf(item.quantidade())));
                Label nome=new Label(item.produto().getNome()+" × "+item.quantidade());nome.setWrapText(true);HBox.setHgrow(nome,Priority.ALWAYS);
                Button menos=botao("−",()->{alterar(item.produto(),-1);atualizador[0].run();},"button-secondary");
                Button mais=botao("+",()->{alterar(item.produto(),1);atualizador[0].run();},"button-secondary");
                listaCarrinho.getChildren().add(new HBox(6,nome,menos,mais));
            }
            if(carrinho.isEmpty())listaCarrinho.getChildren().add(estado("Nenhum produto na venda."));
            total.setText(moeda(soma));long atual=++geracao[0];desconto[0]=null;
            if(botaoConcluir[0]!=null)botaoConcluir[0].setDisable(true);atualizarTroco.run();
            if(carrinho.isEmpty()){mensagemDesconto.setText("");return;}
            mensagemDesconto.setText("Calculando melhor desconto...");
            Cliente contato=cliente.getValue() instanceof Cliente c?c:null;
            List<PedidoItem> itens=carrinho.values().stream().map(i->new PedidoItem(null,i.produto(),BigDecimal.valueOf(i.quantidade()),i.produto().getPrecoVenda())).toList();
            Pedido pedido=new Pedido(null,usuario,contato,StatusPedido.PENDENTE,TipoEntrega.RETIRADA,null,LocalDateTime.now(),itens);
            String codigo=cupom.getText();
            Task<ResultadoDesconto> tarefa=new Task<>(){@Override protected ResultadoDesconto call(){return pedidos.simular(pedido,codigo);}};
            tarefa.setOnSucceeded(e->{if(atual!=geracao[0] || !conteudo.getChildren().contains(subtitulo))return;
                desconto[0]=tarefa.getValue();total.setText(moeda(desconto[0].total()));
                mensagemDesconto.setText("Desconto: "+moeda(desconto[0].desconto())+" ("+desconto[0].estrategia()+")");
                botaoConcluir[0].setDisable(false);atualizarTroco.run();});
            tarefa.setOnFailed(e->{if(atual!=geracao[0] || !conteudo.getChildren().contains(subtitulo))return;
                Throwable erro=tarefa.getException();mensagemDesconto.setText(erro instanceof IllegalArgumentException?erro.getMessage():"Não foi possível calcular o desconto.");});
            Thread t=new Thread(tarefa,"pdv-desconto");t.setDaemon(true);t.start();
        };
        atualizador[0]=atualizar;
        recebido.textProperty().addListener((o,a,n)->atualizarTroco.run());forma.valueProperty().addListener((o,a,n)->atualizarTroco.run());
        cliente.valueProperty().addListener((o,a,n)->atualizar.run());cupom.textProperty().addListener((o,a,n)->atualizar.run());
        for(Produto produto:dados.produtos()){
            Button card=botao(produto.getNome()+"\n"+moeda(produto.getPrecoVenda())+"\n"+(produto.isDisponivelVenda()?"Disponível":"Indisponível no momento"),()->{alterar(produto,1);atualizar.run();},"admin-produto");
            card.setId("pdv-produto-"+produto.getId());card.setPrefWidth(160);card.setWrapText(true);card.setDisable(!produto.isDisponivelVenda());grade.getChildren().add(card);
        }
        VBox produtosBox=new VBox(12,texto("Produtos disponíveis","admin-subtitulo"),grade.getChildren().isEmpty()?estado("Nenhum produto cadastrado."):grade);HBox.setHgrow(produtosBox,Priority.ALWAYS);
        Button concluir=botao("Finalizar venda",()->{
            if(carrinho.isEmpty()||desconto[0]==null){avisar(Alert.AlertType.WARNING,"Carrinho não calculado","Aguarde o cálculo do desconto.");return;}
            FormaPagamento selecionada=forma.getValue();BigDecimal valor;
            if(selecionada==FormaPagamento.PIX&&desconto[0].total().signum()>0&&
                    !confirmar("Você conferiu o recebimento PIX de "+moeda(desconto[0].total())+" no banco?"))return;
            try{valor=selecionada.isPermiteTroco()?decimal(recebido.getText()):desconto[0].total();if(valor.compareTo(desconto[0].total())<0)throw new IllegalArgumentException();}
            catch(RuntimeException e){avisar(Alert.AlertType.WARNING,"Valor inválido","Informe um valor recebido suficiente para a venda.");return;}
            Cliente contato=cliente.getValue() instanceof Cliente c?c:null;
            List<PedidoItem> itens=carrinho.values().stream().map(i->new PedidoItem(null,i.produto(),BigDecimal.valueOf(i.quantidade()),i.produto().getPrecoVenda())).toList();
            Pedido pedido=new Pedido(null,usuario,contato,StatusPedido.PENDENTE,TipoEntrega.RETIRADA,null,LocalDateTime.now(),itens);
            String codigo=cupom.getText();ResultadoDesconto esperado=desconto[0];
            executar(()->pedidos.finalizarPdv(pedido,selecionada,valor,codigo,esperado),id->{carrinho.clear();avisar(Alert.AlertType.INFORMATION,"Venda concluída","Venda #"+id+" registrada.");pdv();},atualizar);
        },"button-primary");concluir.setId("pdv-concluir");concluir.setMaxWidth(Double.MAX_VALUE);
        botaoConcluir[0]=concluir;
        VBox resumo=new VBox(10,texto("Venda atual","admin-subtitulo"),listaCarrinho,new Separator(),new Label("Cliente"),cliente,limparCliente,new Label("Cupom"),cupom,mensagemDesconto,new Label("Pagamento"),forma,recebido,troco,new Label("Total"),total,pix,concluir);resumo.getStyleClass().add("admin-card");resumo.setPrefWidth(290);
        HBox corpo=new HBox(16,produtosBox,resumo);conteudo.getChildren().addAll(subtitulo,corpo);atualizar.run();
    }
    private BigDecimal totalCarrinho(){return carrinho.values().stream().map(i->i.produto().getPrecoVenda().multiply(BigDecimal.valueOf(i.quantidade()))).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private void alterar(Produto produto,int delta){ItemVenda item=carrinho.get(produto.getId());int qtd=(item==null?0:item.quantidade())+delta;
        if(delta>0){
            try{produto.exigirDisponivelParaVenda();}
            catch(ProdutoIndisponivelException e){avisar(Alert.AlertType.WARNING,"Produto indisponível",e.getMessage());return;}
        }
        if(qtd<=0)carrinho.remove(produto.getId());else carrinho.put(produto.getId(),new ItemVenda(produto,qtd));
    }

    @FXML private void mesas(){
        long versao=iniciarTela("Mesas & Comanda Coletiva");
        carregar(versao,comandas::listarMesas,lista->{
            conteudo.getChildren().clear();
            if(lista.isEmpty()){conteudo.getChildren().add(estado("Nenhuma mesa cadastrada."));return;}
            FlowPane grade=new FlowPane(12,12);grade.setPrefWrapLength(700);
            for(Mesa mesa:lista){Button card=botao(mesa.nome()+"\n"+mesa.status(),()->abrirMesa(mesa.id()),mesa.status()==StatusMesa.LIVRE?"mesa-livre":"mesa-ocupada");card.setId("mesa-"+mesa.numero());card.setPrefWidth(155);grade.getChildren().add(card);}
            conteudo.getChildren().addAll(texto("Selecione uma mesa para abrir ou consultar a comanda.","subtitle"),grade);
        });
    }
    private void abrirMesa(long mesaId){carregarMesa(mesaId,null);}
    private void atualizarMesa(long mesaId,long atendimentoId){carregarMesa(mesaId,atendimentoId);}
    private void carregarMesa(long mesaId,Long atendimentoId){
        long versao=iniciarTela("Comanda da mesa");
        carregar(versao,()->new DadosMesa(atendimentoId==null?comandas.abrirOuConsultar(mesaId,usuario):comandas.consultar(mesaId,atendimentoId,usuario),produtos.listar()),dados->montarMesa(dados));
    }
    private void montarMesa(DadosMesa dados){
        Comanda comanda=dados.comanda();long mesaId=comanda.mesa().id(),atendimentoId=comanda.atendimentoId();conteudo.getChildren().clear();
        VBox linhas=new VBox(8);for(ItemComanda item:comanda.itens())linhas.getChildren().add(texto(item.produto()+" · "+item.quantidade().stripTrailingZeros().toPlainString()+" × "+moeda(item.precoUnitario())+" = "+moeda(item.subtotal())+" · quantidade paga "+item.quantidadePaga().stripTrailingZeros().toPlainString(),"subtitle"));
        if(comanda.itens().isEmpty())linhas.getChildren().add(estado("Nenhum item lançado nesta comanda."));
        ComboBox<Produto> produto=new ComboBox<>();produto.setId("comanda-produto");
        produto.getItems().setAll(dados.produtos().stream().filter(Produto::isDisponivelVenda).toList());
        produto.setPromptText(produto.getItems().isEmpty()?"Nenhum produto liberado":"Selecione um produto");
        produto.setConverter(new StringConverter<>(){public String toString(Produto p){return p==null?"":p.getNome()+" · "+moeda(p.getPrecoVenda());}public Produto fromString(String s){return null;}});
        TextField quantidade=campo("1","Quantidade");quantidade.setId("comanda-quantidade");
        Button adicionar=botao("Lançar item",()->{
            try{Produto p=Objects.requireNonNull(produto.getValue());BigDecimal qtd=decimal(quantidade.getText());executar(()->comandas.adicionarItem(mesaId,atendimentoId,p.getId(),qtd,usuario),x->atualizarMesa(mesaId,atendimentoId));}
            catch(RuntimeException e){avisar(Alert.AlertType.WARNING,"Dados inválidos","Selecione um produto e informe uma quantidade válida.");}
        },"button-primary");adicionar.setId("comanda-adicionar");
        VBox pagamentos=new VBox(8);for(PagamentoComanda p:comanda.pagamentos())pagamentos.getChildren().add(new Label(p.pessoa()+" · "+p.forma().descricao()+" · "+moeda(p.valor())));
        if(comanda.pagamentos().isEmpty())pagamentos.getChildren().add(estado("Nenhum pagamento registrado."));
        VBox fechamento=new VBox(8);fechamento.setVisible(false);fechamento.setManaged(false);
        TextField pessoas=campo("2","Pessoas");Label rateio=new Label();rateio.setWrapText(true);
        Button dividir=botao("Calcular divisão igual",()->{try{List<BigDecimal> partes=comandas.dividirIgualmente(comanda.saldo(),Integer.parseInt(pessoas.getText().trim()));rateio.setText("Valores por pessoa: "+partes.stream().map(AdminDashboardController::moeda).collect(java.util.stream.Collectors.joining(" · ")));}catch(RuntimeException e){avisar(Alert.AlertType.WARNING,"Divisão inválida",e.getMessage());}},"button-secondary");
        TextField pessoa=campo("","Nome da pessoa");pessoa.setId("comanda-pessoa");TextField valor=campo("","Deixe em branco para pagar o saldo");valor.setId("comanda-valor");
        ComboBox<FormaPagamento> forma=new ComboBox<>();forma.setId("comanda-forma");forma.getItems().setAll(FormaPagamento.PIX,FormaPagamento.DINHEIRO,FormaPagamento.CARTAO);forma.setValue(FormaPagamento.PIX);
        ComboBox<ItemComanda> item=new ComboBox<>();item.getItems().setAll(comanda.itens());item.setPromptText("Item opcional");
        item.setId("comanda-item-pagamento");
        item.setConverter(new StringConverter<>(){public String toString(ItemComanda i){return i==null?"":i.produto()+" · disponível "+i.quantidadeDisponivel().stripTrailingZeros().toPlainString();}public ItemComanda fromString(String s){return null;}});
        TextField qtdItem=campo("1","Quantidade do item");
        RadioButton porValor=new RadioButton("Por valor / saldo"),porItem=new RadioButton("Por item");
        porItem.setId("comanda-por-item");
        ToggleGroup modo=new ToggleGroup();porValor.setToggleGroup(modo);porItem.setToggleGroup(modo);porValor.setSelected(true);
        PixPainel pix=new PixPainel();pix.setId("comanda-pix");
        Supplier<BigDecimal> cotar=()->porItem.isSelected()?Objects.requireNonNull(item.getValue()).cotarPagamento(decimal(qtdItem.getText())):
                valor.getText().isBlank()?comanda.saldo():decimal(valor.getText());
        Runnable atualizarPix=()->{
            BigDecimal quantia=null;
            try{BigDecimal calculada=cotar.get();if(comanda.aceitaPagamento(calculada))quantia=calculada;}catch(RuntimeException ignorada){/* Input ainda incompleto. */}
            pix.atualizar(forma.getValue()==FormaPagamento.PIX,quantia,null,
                    quantia==null?"Escolha um valor válido ou um item para gerar o PIX.":"Confira o recebimento no banco antes de registrar.");
        };
        valor.textProperty().addListener((o,a,n)->atualizarPix.run());qtdItem.textProperty().addListener((o,a,n)->atualizarPix.run());
        item.valueProperty().addListener((o,a,n)->atualizarPix.run());forma.valueProperty().addListener((o,a,n)->atualizarPix.run());
        modo.selectedToggleProperty().addListener((o,a,n)->{if(n==null)modo.selectToggle(a);atualizarPix.run();});
        VBox entradaValor=new VBox(4,new Label("Valor do pagamento"),valor),entradaItem=new VBox(4,new Label("Item e quantidade"),item,qtdItem);
        entradaValor.visibleProperty().bind(porValor.selectedProperty());entradaValor.managedProperty().bind(entradaValor.visibleProperty());
        entradaItem.visibleProperty().bind(porItem.selectedProperty());entradaItem.managedProperty().bind(entradaItem.visibleProperty());
        Button registrar=botao("Registrar pagamento",()->{
            try{
                String nome=pessoa.getText();FormaPagamento pagamento=forma.getValue();BigDecimal quantia=cotar.get();
                if(nome.isBlank()||nome.trim().length()>100||!comanda.aceitaPagamento(quantia))throw new IllegalArgumentException();
                if(pagamento==FormaPagamento.PIX&&!confirmar("Você conferiu o recebimento PIX de "+moeda(quantia)+" no banco?"))return;
                ItemComanda escolhido=porItem.isSelected()?item.getValue():null;
                BigDecimal qtd=escolhido==null?null:decimal(qtdItem.getText());
                executar(()->escolhido==null?comandas.pagarValor(mesaId,atendimentoId,nome,pagamento,quantia,usuario):
                        comandas.pagarItem(mesaId,atendimentoId,nome,pagamento,escolhido.id(),qtd,usuario),
                        fechada->{if(fechada){avisar(Alert.AlertType.INFORMATION,"Comanda encerrada","Saldo quitado e mesa liberada.");mesas();}else atualizarMesa(mesaId,atendimentoId);});
            }catch(RuntimeException e){avisar(Alert.AlertType.WARNING,"Pagamento inválido","Confira a pessoa, o valor ou o item e o saldo disponível.");}
        },"button-primary");
        Button fecharZero=botao("Fechar com saldo zero",()->executar(()->{comandas.fecharSemSaldo(mesaId,atendimentoId,usuario);return null;},x->mesas()),"button-outline");fecharZero.setDisable(comanda.saldo().signum()!=0);
        fechamento.getChildren().addAll(new Label("Divisão igual"),new HBox(8,pessoas,dividir),rateio,new Label("Pagamento"),pessoa,forma,
                new HBox(8,porValor,porItem),entradaValor,entradaItem,pix,registrar,fecharZero);atualizarPix.run();
        Button abrirFechamento=botao("Fechar / Dividir Conta",()->{fechamento.setVisible(!fechamento.isVisible());fechamento.setManaged(fechamento.isVisible());},"button-secondary");abrirFechamento.setId("comanda-fechar");
        conteudo.getChildren().addAll(texto(comanda.mesa().nome()+" · "+comanda.mesa().status(),"admin-subtitulo"),linhas,new HBox(8,produto,quantidade,adicionar),new Label("Pagamentos"),pagamentos,texto("Total: "+moeda(comanda.total())+" · Pago: "+moeda(comanda.pago())+" · Saldo: "+moeda(comanda.saldo()),"admin-valor"),abrirFechamento,fechamento,botao("Voltar às mesas",this::mesas,"button-outline"));
    }

    @FXML private void fila(){
        long versao=iniciarTela("Fila de pedidos");carregar(versao,pedidos::listarFila,lista->{
            conteudo.getChildren().clear();conteudo.getChildren().addAll(texto("Pedidos da vitrine aguardando preparo ou entrega.","subtitle"),notificarStatus);
            if(lista.isEmpty()){conteudo.getChildren().add(estado("Nenhum pedido aguardando atendimento."));return;}
            for(PedidoFila p:lista){
                String pagamento=p.pixDemonstracao()?" · SIMULAÇÃO (nenhum pagamento real)":p.pixReal()?" · PIX confirmado":" · Pagamento na entrega";
                Label info=new Label("#"+p.id()+"  ·  "+p.cliente()+"  ·  "+p.status()+"  ·  "+p.entrega()+"  ·  "+moeda(p.total())+pagamento+"\n"+p.itens()+(p.enderecoEntrega()==null||p.enderecoEntrega().isBlank()?"":"\nEndereço: "+p.enderecoEntrega())+(p.agendadoPara()==null?"":"\nAgendado: "+p.agendadoPara().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));info.setWrapText(true);HBox.setHgrow(info,Priority.ALWAYS);
                if(p.bairro()!=null)info.setText(info.getText()+"\nBairro: "+p.bairro());
                if(p.nomeEntregador()!=null)info.setText(info.getText()+"\nEntregador: "+p.nomeEntregador());
                StatusPedido proximo=p.status().proximo(p.entrega());
                Button avancar=botao(proximo==StatusPedido.SAIU_ENTREGA?"Despachar entrega":"Avançar para "+proximo,()->mudarStatus(p,proximo),"button-primary");
                avancar.setDisable(proximo==null);
                Button cancelar=botao("Cancelar",()->{if(confirmar("Cancelar pedido #"+p.id()+"? O saldo físico baixado será devolvido."))mudarStatus(p,StatusPedido.CANCELADO);},"button-outline");
                cancelar.setDisable(p.pixReal());
                if(p.pixReal())cancelar.setTooltip(new Tooltip("Estornos de PIX real devem ser tratados no Mercado Pago."));
                Button chamar=botao("Chamar no WhatsApp",()->chamarCliente(p,p.status(),p.nomeEntregador()),"button-secondary");
                chamar.setDisable(p.telefone()==null||p.telefone().isBlank());
                Label atraso = texto("⚠️ ATRASADO", "pedido-atraso-titulo");
                atraso.managedProperty().bind(atraso.visibleProperty());
                VBox dados = new VBox(8, atraso, info); HBox.setHgrow(dados, Priority.ALWAYS);
                if (!p.detalhesPreparo().isEmpty()) {
                    VBox preparo = new VBox(6, texto("ATENÇÃO AO PREPARO", "preparo-titulo"));
                    preparo.getStyleClass().add("preparo-destaque");
                    for (String detalhe : p.detalhesPreparo()) {
                        Label instrucoes = texto(detalhe, "preparo-instrucoes"); instrucoes.setWrapText(true);
                        preparo.getChildren().add(instrucoes);
                    }
                    dados.getChildren().add(preparo);
                }
                HBox linha=new HBox(10,dados,avancar,cancelar,chamar);linha.setAlignment(Pos.CENTER_LEFT);linha.getStyleClass().add("admin-card");conteudo.getChildren().add(linha);
                Runnable atualizarAtraso = () -> {
                    boolean atrasado = p.atrasadoEm(LocalDateTime.now());
                    atraso.setVisible(atrasado);
                    linha.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("atrasado"), atrasado);
                };
                atualizarAtraso.run();alertasAtraso.add(atualizarAtraso);
            }
        });
    }
    private void mudarStatus(PedidoFila pedido,StatusPedido status){
        if (status == StatusPedido.SAIU_ENTREGA) {
            executar(() -> logistica.entregadores(true), ativos -> escolherEntregador(pedido, ativos));
        } else {
            executar(() -> { pedidos.transicionar(pedido.id(), status); return pedido.nomeEntregador(); },
                    nome -> aposStatus(pedido, status, nome));
        }
    }
    private void escolherEntregador(PedidoFila pedido, List<Entregador> ativos) {
        if (ativos.isEmpty()) {
            avisar(Alert.AlertType.INFORMATION,"Nenhum entregador ativo","Cadastre ou reative um parceiro antes de despachar.");
            return;
        }
        Dialog<ButtonType> dialog = dialog("Despachar pedido #" + pedido.id());
        ComboBox<Entregador> parceiros = new ComboBox<>();
        parceiros.getItems().setAll(ativos);
        parceiros.setPromptText("Selecione o entregador parceiro");
        dialog.getDialogPane().setContent(parceiros);
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(parceiros.valueProperty().isNull());
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        long id = parceiros.getValue().id();
        executar(() -> pedidos.despachar(pedido.id(), id), nome -> aposStatus(pedido, StatusPedido.SAIU_ENTREGA, nome));
    }
    private void aposStatus(PedidoFila pedido, StatusPedido status, String nome) {
        if(notificarStatus.isSelected()&&pedido.telefone()!=null&&!pedido.telefone().isBlank())chamarCliente(pedido,status,nome);
        atualizarNotificacoes();fila();
    }
    private void chamarCliente(PedidoFila pedido,StatusPedido status,String entregador){
        try{WhatsAppUi.abrir(raiz.getScene().getWindow(),whatsapp,whatsapp.status(pedido.telefone(),pedido.id(),status,entregador));}
        catch(IllegalArgumentException e){avisar(Alert.AlertType.WARNING,"Telefone indisponível",e.getMessage());}
    }

    @FXML private void statusLoja(){
        long versao=iniciarTela("Status da Loja");
        carregar(versao,operacaoLoja::consultar,this::montarStatusLoja);
    }
    private void montarStatusLoja(ConfiguracaoLoja config){
        Label status=texto(operacaoLoja.rotulo(config),"admin-subtitulo");
        var formato=DateTimeFormatter.ofPattern("HH:mm");
        TextField abertura=campo(config.abertura().format(formato),"HH:mm");abertura.setId("loja-abertura");
        TextField fechamento=campo(config.fechamento().format(formato),"HH:mm");fechamento.setId("loja-fechamento");
        CheckBox automatico=new CheckBox("Respeitar horário automático");automatico.setSelected(config.respeitarHorarioAutomatico());
        automatico.setId("loja-automatico");
        Button salvar=botao("Salvar horário",()->{
            try{
                LocalTime inicio=LocalTime.parse(abertura.getText().strip());
                LocalTime fim=LocalTime.parse(fechamento.getText().strip());
                boolean ativo=automatico.isSelected();
                executar(()->{operacaoLoja.salvarExpediente(inicio,fim,ativo);return null;},x->statusLoja());
            }catch(java.time.format.DateTimeParseException e){avisar(Alert.AlertType.WARNING,"Horário inválido","Informe os horários no formato HH:mm.");}
        },"button-secondary");
        Button alternar=botao(config.abertaManualmente()?"Fechar Loja Agora":"Abrir Loja Agora",()->{
            executar(()->{operacaoLoja.definirAberturaManual(!config.abertaManualmente());return null;},x->statusLoja());
        },"button-primary");alternar.setId("loja-alternar");
        Label regra=texto("O fechamento manual pausa pedidos imediatos. Para abrir fora do expediente, desative o horário automático. Encomendas futuras continuam sujeitas ao expediente.","subtitle");
        regra.setWrapText(true);
        TextField minimo = campo(config.pedidoMinimoEntrega().toPlainString(), "20,00"); minimo.setId("loja-pedido-minimo");
        Button salvarMinimo = botao("Salvar mínimo", () -> {
            try {
                BigDecimal valor = decimal(minimo.getText());
                executar(() -> { operacaoLoja.definirPedidoMinimo(valor); return null; }, x -> statusLoja());
            } catch (IllegalArgumentException e) { avisar(Alert.AlertType.WARNING, "Valor inválido", "Informe o mínimo em reais, por exemplo 20,00."); }
        }, "button-secondary");
        CheckBox chuva = new CheckBox("🌧️ Modo Chuva / Horário de Pico");
        chuva.setId("loja-modo-chuva");
        chuva.setSelected(config.modoChuvaAtivo());
        chuva.setOnAction(e -> {
            boolean ativo = chuva.isSelected();
            executar(() -> { operacaoLoja.definirModoChuva(ativo); return null; }, x -> statusLoja(),
                    () -> chuva.setSelected(config.modoChuvaAtivo()));
        });
        Label acrescimo = texto("Quando ativo: +" + config.acrescimoTempoChuvaMin() + " min e +" + moeda(config.acrescimoTaxaChuva()) + " no frete.", "subtitle");
        VBox painel=new VBox(10,status,regra,chuva,acrescimo,new HBox(12,new VBox(4,new Label("Abertura"),abertura),new VBox(4,new Label("Fechamento"),fechamento)),automatico,new HBox(10,salvar,alternar),
                new Label("Pedido mínimo para entrega (R$, sem frete)"), new HBox(10, minimo, salvarMinimo));
        painel.getStyleClass().add("admin-card");conteudo.getChildren().setAll(painel);
    }

    @FXML private void estoque(){
        long versao=iniciarTela("Produtos e estoque");
        carregar(versao,()->new DadosEstoque(produtos.listar(),produtos.listarCategorias(),agenda.buscar(FiltroContato.FORNECEDORES,"")),dados->montarEstoque(dados));
    }
    private void montarEstoque(DadosEstoque dados){
        conteudo.getChildren().clear();TableView<Produto> tabela=new TableView<>();tabela.setId("tabela-produtos");tabela.setPlaceholder(estado("Nenhum produto cadastrado."));
        coluna(tabela,"Produto",p->p.getNome(),180);coluna(tabela,"Preço",p->moeda(p.getPrecoVenda()),100);coluna(tabela,"Custo",p->moeda(p.getCustoProducao()),100);
        coluna(tabela,"Estoque",p->p.getEstoqueAtual().stripTrailingZeros().toPlainString(),90);coluna(tabela,"Mínimo",p->p.getEstoqueMinimo().stripTrailingZeros().toPlainString(),90);
        coluna(tabela,"Venda",p->p.isDisponivelVenda()?"Liberada":"Bloqueada",115);
        tabela.setRowFactory(t->new TableRow<>(){@Override protected void updateItem(Produto p,boolean vazio){super.updateItem(p,vazio);getStyleClass().remove("estoque-baixo");if(!vazio&&p!=null&&p.isEstoqueBaixo())getStyleClass().add("estoque-baixo");}});
        tabela.getItems().setAll(dados.produtos());tabela.setPrefHeight(380);
        Button novo=botao("Novo produto",()->formularioProduto(null,dados.categorias()),"button-primary");
        Button editar=botao("Editar",()->formularioProduto(tabela.getSelectionModel().getSelectedItem(),dados.categorias()),"button-secondary");
        Button excluir=botao("Excluir",()->{Produto p=tabela.getSelectionModel().getSelectedItem();if(p!=null&&confirmar("Excluir "+p.getNome()+"?"))executar(()->produtos.excluir(p.getId()),ok->{if(!ok)avisar(Alert.AlertType.WARNING,"Produto não encontrado","O produto pode ter sido removido.");estoque();});},"button-outline");
        Button lote=botao("Entrada de lote",()->entradaLote(tabela.getSelectionModel().getSelectedItem(),dados.fornecedores()),"button-secondary");
        Button perda=botao("Registrar perda",()->perda(tabela.getSelectionModel().getSelectedItem()),"button-outline");
        Button alternar=botao("Bloquear / Liberar Venda",()->{
            Produto p=tabela.getSelectionModel().getSelectedItem();
            executar(()->produtos.alternarDisponibilidade(p.getId()),ok->{
                if(!ok)avisar(Alert.AlertType.WARNING,"Produto não encontrado","O produto pode ter sido removido.");
                estoque();
            });
        },"button-secondary");alternar.setId("alternar-venda");
        for(Button b:List.of(editar,excluir,lote,perda,alternar))b.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        conteudo.getChildren().addAll(texto("O estoque físico é um controle interno; bloquear a venda impede novos pedidos.","subtitle"),tabela,new FlowPane(8,8,novo,editar,alternar,excluir,lote,perda));
    }
    private void formularioProduto(Produto atual,List<Categoria> categorias){
        Dialog<ButtonType> dialog=dialog(atual==null?"Novo produto":"Editar produto");
        TextField nome=campo(atual==null?"":atual.getNome(),"Nome");TextField descricao=campo(atual==null?"":atual.getDescricao(),"Descrição");
        TextField preco=campo(atual==null?"":atual.getPrecoVenda().toPlainString(),"Preço de venda");TextField custo=campo(atual==null?"":atual.getCustoProducao().toPlainString(),"Custo de produção");
        TextField minimo=campo(atual==null?"0":atual.getEstoqueMinimo().toPlainString(),"Estoque mínimo");TextField imagem=campo(atual==null?"":atual.getUrlImagem(),"URL ou arquivo da imagem");
        TextField estoqueInterno=campo(atual==null?"0":atual.getEstoqueAtual().stripTrailingZeros().toPlainString(),"Estoque físico");estoqueInterno.setDisable(atual==null);
        Button escolher=botao("Selecionar imagem do computador",()->{FileChooser fc=new FileChooser();fc.setTitle("Selecionar imagem");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens","*.png","*.jpg","*.jpeg","*.gif"));File arquivo=fc.showOpenDialog(raiz.getScene().getWindow());if(arquivo!=null)imagem.setText(arquivo.toURI().toString());},"button-secondary");
        ComboBox<String> categoria = new ComboBox<>(); categoria.setEditable(true); categoria.setId("produto-categoria");
        categoria.getItems().setAll(categorias.stream().map(Categoria::getNome).toList());
        categoria.setPromptText("Selecione ou digite uma categoria");
        if (atual != null && atual.getCategoria() != null) categoria.setValue(atual.getCategoria().getNome());
        VBox linhasOpcionais = new VBox(6);
        Label semOpcionais = texto("Nenhum opcional cadastrado.", "subtitle");
        semOpcionais.visibleProperty().bind(javafx.beans.binding.Bindings.isEmpty(linhasOpcionais.getChildren()));
        semOpcionais.managedProperty().bind(semOpcionais.visibleProperty());
        if (atual != null) atual.getOpcionais().forEach(o -> adicionarLinhaOpcional(linhasOpcionais, o));
        Button adicionarOpcional = botao("Adicionar opcional", () -> adicionarLinhaOpcional(linhasOpcionais, null), "button-secondary");
        VBox campos=new VBox(8,new Label("Nome"),nome,new Label("Descrição"),descricao,new Label("Categoria"),categoria,new Label("Preço de venda"),preco,new Label("Custo de produção"),custo,new Label("Estoque físico (controle interno)"),estoqueInterno,new Label("Estoque mínimo"),minimo,new Label("Imagem"),escolher,new Label("URL ou caminho (opcional)"),imagem);campos.setPadding(new Insets(10));
        Label ajuda = texto("Repita o nome do grupo, o número de escolhas grátis e o máximo em cada opção do mesmo grupo.", "subtitle"); ajuda.setWrapText(true);
        campos.getChildren().addAll(new Label("Bordas e adicionais"), ajuda, semOpcionais, linhasOpcionais, adicionarOpcional);
        ScrollPane scroll=new ScrollPane(campos);scroll.setFitToWidth(true);scroll.setPrefViewportHeight(470);scroll.setPrefViewportWidth(660);dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
            try{
                Produto p=new Produto(atual==null?null:atual.getId(),null,nome.getText(),descricao.getText(),decimal(preco.getText()),decimal(custo.getText()),atual==null?BigDecimal.ZERO:atual.getEstoqueAtual(),decimal(minimo.getText()),imagem.getText().isBlank()?null:imagem.getText());
                if(atual!=null)p.setFichaVitrine(atual.getFichaVitrine());
                BigDecimal digitado=decimal(estoqueInterno.getText());
                BigDecimal ajuste=atual==null||digitado.compareTo(atual.getEstoqueAtual())==0?null:digitado;
                p.setOpcionais(linhasOpcionais.getChildren().stream().map(LinhaOpcional.class::cast).map(LinhaOpcional::opcional).toList());
                ProdutoPersonalizacaoService.validarCatalogo(p.getOpcionais());
                String categoriaDigitada = categoria.getEditor().getText();
                executar(()->{produtos.salvarCatalogo(p,categoriaDigitada,ajuste);return null;},x->estoque());
            }catch(IllegalArgumentException e){evento.consume();avisar(Alert.AlertType.WARNING,"Dados inválidos",e.getMessage());}
        });
        dialog.showAndWait();
    }
    private void adicionarLinhaOpcional(VBox linhas, ProdutoOpcional opcional) {
        LinhaOpcional linha = new LinhaOpcional(opcional);
        linha.getChildren().add(botao("Remover", () -> linhas.getChildren().remove(linha), "button-outline"));
        linhas.getChildren().add(linha);
    }
    private static final class LinhaOpcional extends HBox {
        private final TextField grupo, nome, preco, gratis, maximo;
        LinhaOpcional(ProdutoOpcional o) {
            super(6);
            grupo = campo(o == null ? "" : o.grupoNome(), "Grupo");
            nome = campo(o == null ? "" : o.nome(), "Nome");
            preco = campo(o == null ? "0,00" : o.precoAdicional().toPlainString(), "Preço");
            gratis = campo(o == null ? "0" : Integer.toString(o.gratisAteLimite()), "Grátis");
            maximo = campo(o == null ? "1" : Integer.toString(o.maximoEscolhas()), "Máximo");
            grupo.setPrefColumnCount(12); nome.setPrefColumnCount(12); preco.setPrefColumnCount(5); gratis.setPrefColumnCount(3); maximo.setPrefColumnCount(3);
            setAlignment(Pos.BOTTOM_LEFT);
            getChildren().addAll(new VBox(3,new Label("Grupo"),grupo), new VBox(3,new Label("Opção"),nome),
                    new VBox(3,new Label("Preço (R$)"),preco), new VBox(3,new Label("Grátis"),gratis), new VBox(3,new Label("Máximo"),maximo));
        }
        ProdutoOpcional opcional() {
            int franquia, limite;
            try { franquia = Integer.parseInt(gratis.getText().strip()); limite = Integer.parseInt(maximo.getText().strip()); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Informe valores inteiros para a franquia e o máximo de escolhas."); }
            return new ProdutoOpcional(null, grupo.getText(), nome.getText(), decimal(preco.getText()), franquia, limite);
        }
    }
    private void entradaLote(Produto produto,List<Contato> contatos){
        if(produto==null)return;List<Fornecedor> fornecedores=contatos.stream().filter(Fornecedor.class::isInstance).map(Fornecedor.class::cast).toList();
        if(fornecedores.isEmpty()){avisar(Alert.AlertType.INFORMATION,"Nenhum fornecedor","Cadastre um fornecedor na agenda antes de lançar um lote.");return;}
        Dialog<ButtonType> dialog=dialog("Entrada de lote — "+produto.getNome());ComboBox<Fornecedor> fornecedor=new ComboBox<>();fornecedor.getItems().setAll(fornecedores);fornecedor.setPromptText("Selecione o fornecedor");
        fornecedor.setConverter(new StringConverter<>(){public String toString(Fornecedor f){return f==null?"":f.getNomeEmpresa();}public Fornecedor fromString(String s){return null;}});
        TextField quantidade=campo("","Quantidade");TextField observacao=campo("","Observação (opcional)");dialog.getDialogPane().setContent(new VBox(10,new Label("Fornecedor"),fornecedor,new Label("Quantidade"),quantidade,observacao));
        if(dialog.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK)return;
        try{Fornecedor f=Objects.requireNonNull(fornecedor.getValue());BigDecimal qtd=decimal(quantidade.getText());executar(()->{produtos.entradaLote(produto.getId(),f.getId(),qtd,observacao.getText());return null;},x->estoque());}
        catch(RuntimeException e){avisar(Alert.AlertType.WARNING,"Dados inválidos","Selecione um fornecedor e informe a quantidade.");}
    }
    private void perda(Produto produto){
        if(produto==null)return;Dialog<ButtonType> dialog=dialog("Registrar perda — "+produto.getNome());TextField quantidade=campo("","Quantidade");TextField motivo=campo("","Motivo da perda");dialog.getDialogPane().setContent(new VBox(10,new Label("Quantidade"),quantidade,new Label("Motivo"),motivo));
        if(dialog.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK)return;
        try{BigDecimal qtd=decimal(quantidade.getText());if(motivo.getText().isBlank())throw new IllegalArgumentException();executar(()->{produtos.registrarPerda(produto.getId(),qtd,motivo.getText());return null;},x->estoque());}
        catch(RuntimeException e){avisar(Alert.AlertType.WARNING,"Dados inválidos","Informe quantidade e motivo.");}
    }

    @FXML private void bairrosEntrega() {
        long versao = iniciarTela("Bairros & Taxas de Entrega");
        carregar(versao, () -> logistica.bairros(false), bairros -> {
            TableView<BairroEntrega> tabela = new TableView<>();
            tabela.setPlaceholder(estado("Nenhum bairro cadastrado."));
            coluna(tabela, "Bairro", BairroEntrega::nome, 220);
            coluna(tabela, "Taxa", b -> moeda(b.taxaEntrega()), 100);
            coluna(tabela, "Prazo", b -> b.tempoEstimadoMin() + " min", 100);
            coluna(tabela, "Situação", b -> b.ativo() ? "Ativo" : "Inativo", 100);
            montarCadastro(tabela, bairros, "Novo bairro", this::formularioBairro);
        });
    }
    private void formularioBairro(BairroEntrega atual) {
        Dialog<ButtonType> d = dialog(atual == null ? "Novo bairro" : "Editar bairro");
        TextField nome = campo(atual == null ? "" : atual.nome(), "Nome");
        TextField taxa = campo(atual == null ? "" : atual.taxaEntrega().toPlainString(), "Taxa em reais");
        TextField prazo = campo(atual == null ? "" : Integer.toString(atual.tempoEstimadoMin()), "Prazo em minutos");
        CheckBox ativo = new CheckBox("Atende entregas neste bairro"); ativo.setSelected(atual == null || atual.ativo());
        d.getDialogPane().setContent(new VBox(8, new Label("Bairro"), nome, new Label("Taxa (R$)"), taxa,
                new Label("Prazo (minutos)"), prazo, ativo));
        if (d.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            var b = new BairroEntrega(atual == null ? null : atual.id(), nome.getText(), decimal(taxa.getText()), Integer.parseInt(prazo.getText().strip()), ativo.isSelected());
            executar(() -> { logistica.salvar(b); return null; }, x -> bairrosEntrega());
        } catch (NumberFormatException e) {
            avisar(Alert.AlertType.WARNING, "Dados inválidos", "Informe uma taxa em reais e um prazo inteiro em minutos.");
        }
    }
    @FXML private void entregadoresParceiros() {
        long versao = iniciarTela("Entregadores Parceiros");
        carregar(versao, () -> logistica.entregadores(false), parceiros -> {
            TableView<Entregador> tabela = new TableView<>();
            tabela.setPlaceholder(estado("Nenhum entregador cadastrado."));
            coluna(tabela, "Nome", Entregador::nome, 220);
            coluna(tabela, "Telefone", e -> nulo(e.telefone()), 130);
            coluna(tabela, "Placa", e -> nulo(e.placaVeiculo()), 90);
            coluna(tabela, "Situação", e -> e.ativo() ? "Ativo" : "Inativo", 100);
            montarCadastro(tabela, parceiros, "Novo entregador", this::formularioEntregador);
        });
    }
    private void formularioEntregador(Entregador atual) {
        Dialog<ButtonType> d = dialog(atual == null ? "Novo entregador" : "Editar entregador");
        TextField nome = campo(atual == null ? "" : atual.nome(), "Nome");
        TextField telefone = campo(atual == null ? "" : atual.telefone(), "Telefone opcional");
        TextField placa = campo(atual == null ? "" : atual.placaVeiculo(), "Placa opcional");
        CheckBox ativo = new CheckBox("Disponível para novas entregas"); ativo.setSelected(atual == null || atual.ativo());
        d.getDialogPane().setContent(new VBox(8, new Label("Nome"), nome, new Label("Telefone"), telefone,
                new Label("Placa do veículo"), placa, ativo));
        if (d.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        var parceiro = new Entregador(atual == null ? null : atual.id(), nome.getText(), telefone.getText(), placa.getText(), ativo.isSelected());
        executar(() -> { logistica.salvar(parceiro); return null; }, x -> entregadoresParceiros());
    }
    private <T> void montarCadastro(TableView<T> tabela, List<T> itens, String rotuloNovo, Consumer<T> editar) {
        tabela.getItems().setAll(itens);
        tabela.setPrefHeight(320);
        Button novo = botao(rotuloNovo, () -> editar.accept(null), "button-primary");
        Button alterar = botao("Editar selecionado", () -> editar.accept(tabela.getSelectionModel().getSelectedItem()), "button-secondary");
        alterar.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        conteudo.getChildren().setAll(tabela, new HBox(8, novo, alterar));
    }

    @FXML private void financeiro(){
        long versao=iniciarTela("Financeiro e lucro líquido");
        LocalDateTime inicio=LocalDate.now().withDayOfMonth(1).atStartOfDay(),fim=LocalDate.now().plusDays(1).atStartOfDay();
        carregar(versao,()->new DadosFinanceiros(financeiro.calcular(inicio,fim),financeiro.produtosMaisVendidos(inicio,fim),financeiro.receitaPorCategoria(inicio,fim),financeiro.repasses(inicio,fim)),dados->{
            conteudo.getChildren().clear();conteudo.getChildren().add(texto("Mês atual · pedidos entregues pela data de criação; perdas pela data da movimentação.","subtitle"));
            var r=dados.totais();FlowPane indicadores=new FlowPane(10,10,indicador("Faturamento após descontos",r.faturamentoAposDescontos()),indicador("Custos de produção",r.custoTotalProducao()),indicador("Perdas",r.perdas()),indicador("Lucro líquido real",r.lucroLiquidoReal()));
            Button exportar=botao("Exportar Balanço (.CSV)",()->exportarBalanco(r),"button-secondary");exportar.setId("exportar-csv");
            conteudo.getChildren().addAll(indicadores,exportar,texto("Resumo de Repasses por Entregador", "admin-subtitulo"),
                    texto("Taxas cobradas em entregas concluídas, pela data de criação do pedido; frete grátis não soma taxa.", "subtitle"));
            TableView<RepasseEntregador> repasses = new TableView<>();
            repasses.setPrefHeight(180);
            repasses.setPlaceholder(estado("Nenhuma entrega concluída com parceiro no período."));
            coluna(repasses, "Entregador", e -> e.nome() + " (#" + e.entregadorId() + ")", 220);
            coluna(repasses, "Entregas", e -> Long.toString(e.entregas()), 100);
            coluna(repasses, "Total de taxas", e -> moeda(e.totalTaxas()), 130);
            repasses.getItems().setAll(dados.repasses());
            conteudo.getChildren().add(repasses);
            if(dados.produtos().isEmpty())conteudo.getChildren().add(estado("Nenhuma venda entregue no período para o gráfico de produtos."));
            else{CategoryAxis eixoX=new CategoryAxis();NumberAxis eixoY=new NumberAxis();eixoY.setLabel("Quantidade");BarChart<String,Number> grafico=new BarChart<>(eixoX,eixoY);grafico.setTitle("Produtos mais vendidos");grafico.setLegendVisible(false);XYChart.Series<String,Number> serie=new XYChart.Series<>();dados.produtos().forEach((nome,qtd)->serie.getData().add(new XYChart.Data<>(nome,qtd)));grafico.getData().add(serie);conteudo.getChildren().add(grafico);}
            if(dados.categorias().isEmpty())conteudo.getChildren().add(estado("Nenhuma receita por categoria no período."));
            else{PieChart grafico=new PieChart();grafico.setTitle("Receita por categoria");dados.categorias().forEach((nome,valor)->grafico.getData().add(new PieChart.Data(nome,valor.doubleValue())));conteudo.getChildren().add(grafico);}
        });
    }
    private void exportarBalanco(RelatorioFinanceiroService.Relatorio relatorio){
        FileChooser seletor=new FileChooser();seletor.setTitle("Exportar balanço");seletor.setInitialFileName("balanco.csv");seletor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo CSV","*.csv"));
        File arquivo=seletor.showSaveDialog(raiz.getScene().getWindow());if(arquivo==null)return;
        executar(()->{try{financeiro.exportarCsv(relatorio,arquivo.toPath());return null;}catch(IOException e){throw new java.io.UncheckedIOException(e);}},x->avisar(Alert.AlertType.INFORMATION,"Balanço exportado","Arquivo CSV salvo com sucesso."));
    }
    private Node indicador(String nome,BigDecimal valor){VBox caixa=new VBox(8,texto(nome,"subtitle"),texto(moeda(valor),"admin-valor"));caixa.getStyleClass().add("admin-card");caixa.setPrefWidth(190);return caixa;}

    @FXML private void clubeDaFome(){
        if(!(usuario instanceof UsuarioAdmin admin))return;
        long versao=iniciarTela("Clube VIP");
        carregar(versao,()->clube.painel(admin),painel->montarClube(admin,painel));
    }
    private void montarClube(UsuarioAdmin admin,ClubeService.Painel painel){
        TableView<PlanoAssinatura> planos=new TableView<>();planos.setId("clube-planos");planos.setPrefHeight(190);
        planos.setPlaceholder(estado("Plano VIP indisponível."));
        coluna(planos,"Plano",PlanoAssinatura::nome,145);coluna(planos,"Descrição",PlanoAssinatura::descricao,265);
        coluna(planos,"Mensalidade",p->moeda(p.mensalidade()),110);coluna(planos,"Desconto",p->percentual(p.descontoPercentual()),100);
        coluna(planos,"Estado",p->p.ativo()?"Disponível":"Desativado",105);planos.getItems().setAll(painel.planos());
        TableView<AssinaturaCliente> ativos=new TableView<>();ativos.setId("clube-ativos");ativos.setPrefHeight(180);
        ativos.setPlaceholder(estado("Nenhum assinante com vigência ativa."));
        DateTimeFormatter data=DateTimeFormatter.ofPattern("dd/MM/yyyy");
        coluna(ativos,"Cliente",AssinaturaCliente::cliente,190);coluna(ativos,"Plano",AssinaturaCliente::plano,150);
        coluna(ativos,"Benefício",a->"Frete grátis + "+percentual(a.descontoVigente()),170);
        coluna(ativos,"Válida até",a->a.validaAte().minusDays(1).format(data),140);
        ativos.getItems().setAll(painel.ativos());

        TableView<MensalidadeClube> pendentes=new TableView<>();pendentes.setId("clube-pendentes");pendentes.setPrefHeight(180);
        pendentes.setPlaceholder(estado("Nenhum pagamento aguardando confirmação."));
        coluna(pendentes,"Referência PIX",m->"C"+m.id(),125);coluna(pendentes,"Cliente",MensalidadeClube::cliente,170);
        coluna(pendentes,"Plano",MensalidadeClube::plano,150);coluna(pendentes,"Valor",m->moeda(m.valor()),110);
        pendentes.getItems().setAll(painel.pendencias());
        Button confirmar=botao("Confirmar mensalidade recebida",()->{
            MensalidadeClube m=pendentes.getSelectionModel().getSelectedItem();
            if(m!=null&&confirmar("Você conferiu no banco o recebimento de "+moeda(m.valor())+
                    " para "+m.cliente()+" (referência C"+m.id()+")?"))
                executar(()->{clube.confirmar(admin,m.usuarioId(),m.id());return null;},x->clubeDaFome());
        },"button-primary");
        confirmar.disableProperty().bind(pendentes.getSelectionModel().selectedItemProperty().isNull());
        conteudo.getChildren().setAll(texto("Plano único pré-pago", "admin-subtitulo"),planos,
                texto("Assinantes ativos","admin-subtitulo"),ativos,
                texto("Mensalidades a conferir","admin-subtitulo"),
                texto("Valide cada recebimento no banco antes de confirmar. Não há cobrança ou confirmação automática.","subtitle"),pendentes,confirmar);
    }
    private static String percentual(BigDecimal valor){return NumberFormat.getNumberInstance(BR).format(valor)+"%";}

    @FXML private void moderacao(){
        long versao=iniciarTela("Moderação de avaliações");carregar(versao,avaliacoes::listar,lista->{
            conteudo.getChildren().clear();TableView<AvaliacaoResumo> tabela=new TableView<>();tabela.setId("tabela-avaliacoes");tabela.setPlaceholder(estado("Nenhuma avaliação cadastrada."));
            coluna(tabela,"Cliente",AvaliacaoResumo::cliente,120);coluna(tabela,"Produto",AvaliacaoResumo::produto,120);coluna(tabela,"Nota",a->a.nota()+" / 5",65);coluna(tabela,"Comentário",a->nulo(a.comentario()),220);coluna(tabela,"Resposta oficial",a->nulo(a.resposta()),220);
            TableColumn<AvaliacaoResumo,String> fotos=new TableColumn<>("Foto anexada");fotos.setPrefWidth(115);
            fotos.setCellValueFactory(d->new ReadOnlyStringWrapper(d.getValue().fotoUrl()));
            fotos.setCellFactory(c->new TableCell<>(){
                @Override protected void updateItem(String foto,boolean vazia){
                    super.updateItem(foto,vazia);setGraphic(null);setText(null);
                    if(vazia)return;
                    var miniatura=FotoAvaliacaoUi.miniatura(foto,92,64,"Foto anexada à avaliação");
                    if(miniatura.isPresent())setGraphic(miniatura.get());else setText(foto==null?"Sem foto":"Arquivo indisponível");
                }
            });tabela.getColumns().add(2,fotos);
            tabela.getItems().setAll(lista);tabela.setPrefHeight(400);
            Button responder=botao("Responder oficialmente",()->{AvaliacaoResumo a=tabela.getSelectionModel().getSelectedItem();if(a==null)return;TextInputDialog d=new TextInputDialog(nulo(a.resposta()));d.initOwner(raiz.getScene().getWindow());d.setTitle("Resposta oficial");d.setHeaderText("Resposta para "+a.cliente());d.showAndWait().ifPresent(resposta->{if(resposta.isBlank()){avisar(Alert.AlertType.WARNING,"Resposta vazia","Escreva uma resposta antes de salvar.");return;}executar(()->avaliacoes.responder(usuario,a.id(),resposta),ok->{if(!ok)avisar(Alert.AlertType.WARNING,"Avaliação não encontrada","Atualize a listagem.");moderacao();});});},"button-primary");
            Button excluir=botao("Excluir avaliação",()->{AvaliacaoResumo a=tabela.getSelectionModel().getSelectedItem();if(a!=null&&confirmar("Excluir esta avaliação?"))executar(()->avaliacoes.excluir(usuario,a.id()),ok->{if(!ok)avisar(Alert.AlertType.WARNING,"Avaliação não encontrada","Atualize a listagem.");moderacao();});},"button-outline");
            responder.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());excluir.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
            conteudo.getChildren().addAll(tabela,new HBox(10,responder,excluir));
        });
    }
    @FXML private void voltar(){notificacoesAtivas=false;notificacoes.stop();telaAtual++;try{Navegacao.abrirTelaInicial((Stage)raiz.getScene().getWindow(),usuario);}catch(IOException e){avisar(Alert.AlertType.ERROR,"Não foi possível voltar","Tente novamente.");}}
    private static <T> void coluna(TableView<T> tabela,String titulo,java.util.function.Function<T,String> valor,int largura){TableColumn<T,String> c=new TableColumn<>(titulo);c.setCellValueFactory(d->new SimpleStringProperty(valor.apply(d.getValue())));c.setPrefWidth(largura);tabela.getColumns().add(c);}
    private static Label texto(String texto,String estilo){Label l=new Label(texto);l.setWrapText(true);l.getStyleClass().add(estilo);return l;}
    private static Label estado(String texto){Label l=texto(texto,"admin-empty");l.setMaxWidth(Double.MAX_VALUE);return l;}
    private static String nulo(String s){return s==null?"":s;}
    private static String moeda(BigDecimal n){return NumberFormat.getCurrencyInstance(BR).format(n);}
    private static BigDecimal decimal(String s){return new BigDecimal(s.trim().replace(',','.'));}
    private static TextField campo(String valor,String prompt){TextField t=new TextField(nulo(valor));t.setPromptText(prompt);return t;}
    private static Button botao(String texto,Runnable acao,String estilo){Button b=new Button(texto);b.getStyleClass().add(estilo);b.setOnAction(e->acao.run());return b;}
    private Dialog<ButtonType> dialog(String titulo){Dialog<ButtonType> d=new Dialog<>();d.initOwner(raiz.getScene().getWindow());d.setTitle(titulo);d.setHeaderText(titulo);d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);return d;}
    private boolean confirmar(String mensagem){Alert a=new Alert(Alert.AlertType.CONFIRMATION,mensagem,ButtonType.CANCEL,ButtonType.OK);a.initOwner(raiz.getScene().getWindow());a.setHeaderText(null);return a.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
    private void avisar(Alert.AlertType tipo,String titulo,String mensagem){Alert a=new Alert(tipo);a.initOwner(raiz.getScene().getWindow());a.setTitle(titulo);a.setHeaderText(null);a.setContentText(mensagem);a.showAndWait();}
}
