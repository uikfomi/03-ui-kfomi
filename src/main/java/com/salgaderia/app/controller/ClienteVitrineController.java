package com.salgaderia.app.controller;

import com.salgaderia.app.exception.AvaliacaoNaoPermitidaException;
import com.salgaderia.app.exception.ProdutoIndisponivelException;
import com.salgaderia.app.model.*;
import com.salgaderia.app.model.PersonalizacaoItem.Tipo;
import com.salgaderia.app.model.enums.StatusPedido;
import com.salgaderia.app.model.enums.TipoEntrega;
import com.salgaderia.app.model.enums.FormaPagamento;
import com.salgaderia.app.model.enums.StatusPagamento;
import com.salgaderia.app.service.*;
import com.salgaderia.app.service.impl.*;
import com.salgaderia.app.service.desconto.ResultadoDesconto;
import com.salgaderia.app.service.desconto.DescontoCombo;
import com.salgaderia.app.service.desconto.ContextoDesconto;
import com.salgaderia.app.service.impl.AutenticacaoServiceImpl;
import com.salgaderia.app.service.cozinha.EtapaCozinha;
import com.salgaderia.app.service.pix.PixPagamentoService;
import com.salgaderia.app.util.Navegacao;
import com.salgaderia.app.util.FotoAvaliacaoUi;
import com.salgaderia.app.util.PixPainel;
import com.salgaderia.app.util.PagamentoPixUi;
import com.salgaderia.app.util.RoletaUi;
import com.salgaderia.app.util.TelaAutenticada;
import com.salgaderia.app.util.WhatsAppUi;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.concurrent.Task;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class ClienteVitrineController implements TelaAutenticada {
    @FXML private BorderPane raiz;
    @FXML private Label saudacao, mensagemProdutos, mensagemPedidos, endereco, total, contador;
    @FXML private TextField busca, cupom;
    @FXML private Label mensagemDesconto, saldoResgate, resumoMensal, orientacaoCartao;
    @FXML private CheckBox meusFavoritos;
    @FXML private VBox sugestaoCarrinho;
    private final FidelidadeService fidelidade = new FidelidadeService();
    private final InteligenciaComercialService comercial = new InteligenciaComercialService();
    private Set<Long> favoritos = Set.of();
    private Long pontosResgate;
    private boolean carregandoCarteira, alterandoFavorito;
    private long geracaoResumo;
    private long ultimoRelogioCarteira;
    @FXML private Label mensagemFrete,cupomUrgente,statusLoja,orientacaoLoja,mensagemMinimo;
    @FXML private VBox dadosPagadorPix;
    @FXML private TextField emailPix,documentoPix;
    @FXML private ComboBox<FiltroCategoria> categorias;
    @FXML private VBox selecaoBairro;
    @FXML private ComboBox<OpcaoBairro> bairroEntrega;
    @FXML private ComboBox<FormaPagamento> formaPagamento;
    @FXML private FlowPane cards;
    @FXML private FlowPane galeriaFotos;
    @FXML private Label mensagemFotos;
    @FXML private VBox painelVitrine, painelPedidos, listaPedidos, itensCarrinho;
    @FXML private VBox painelClube, conteudoClube;
    @FXML private PixPainel pixCheckout;
    @FXML private ScrollPane rolagemProdutos;
    @FXML private RadioButton retirada, entrega, local;
    @FXML private CheckBox agendar;
    @FXML private DatePicker dataAgendamento;
    @FXML private TextField horaAgendamento;
    @FXML private Button finalizar;
    private final ProdutoEstoqueService produtosService;
    private final PedidoVendaService pedidosService;
    private final AvaliacaoService avaliacoesService;
    private final ClubeService clube;
    private final AutenticacaoService autenticacao=new AutenticacaoServiceImpl();
    private final GamificacaoService gamificacao;
    private final PixPagamentoService pagamentosPix;
    private final OperacaoLojaService operacao = new OperacaoLojaService();
    private final java.util.function.Supplier<ConfiguracaoLoja> consultarOperacao;
    private final Timeline atualizacaoLoja = new Timeline(new KeyFrame(Duration.seconds(5), e -> atualizarLoja()));
    private ConfiguracaoLoja configuracao;
    private boolean consultandoLoja;
    private long geracaoLoja;
    private final java.util.function.Supplier<List<BairroEntrega>> consultarBairros;
    private List<BairroEntrega> bairrosAtivos = List.of();
    private boolean carregouBairros, atualizandoBairros;
    private record OpcaoBairro(BairroEntrega bairro) {
        @Override public String toString() { return bairro == null ? "Outro bairro / Fora da área" : bairro.nome(); }
    }
    private record OperacaoAtual(ConfiguracaoLoja config, Set<Long> produtosLiberados, List<BairroEntrega> bairros) { }
    private final Map<ChaveCarrinho,List<PedidoItem>> carrinho = new LinkedHashMap<>();
    private final PersonalizacaoService personalizacoes = new PersonalizacaoService();
    private List<Produto> produtos = List.of();
    private Map<Long,BigDecimal> medias = Map.of();
    private UsuarioCliente usuario;
    private long geracaoProdutos;
    private long geracaoPedidos;
    private long geracaoClube;
    private boolean carregouProdutos;
    private final WhatsAppLinkService whatsapp = new WhatsAppLinkService();
    private final Timeline atualizacaoPedidos = new Timeline(new KeyFrame(Duration.seconds(5), e -> atualizarPedidos(false)));
    private final Timeline contagemCupons=new Timeline(new KeyFrame(Duration.seconds(1),e->atualizarContagemCupons()));
    private List<CupomRoleta> cuponsDaRoleta=List.of();
    private long ultimoRelogioCupons;
    private boolean consultandoCupons;
    private boolean consultaPedidosEmAndamento;
    private long geracaoDesconto;
    private ResultadoDesconto descontoAtual;
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    public ClienteVitrineController() { this(new ProdutoEstoqueServiceImpl(),new PedidoVendaServiceImpl(),new AvaliacaoServiceImpl()); }
    public ClienteVitrineController(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AvaliacaoService avaliacoes) {
        this(produtos,pedidos,avaliacoes,new ClubeServiceImpl(),new GamificacaoServiceImpl());
    }
    public ClienteVitrineController(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AvaliacaoService avaliacoes,
                                    ClubeService clube,GamificacaoService gamificacao) {
        this(produtos,pedidos,avaliacoes,clube,gamificacao,new PixPagamentoService());
    }
    public ClienteVitrineController(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AvaliacaoService avaliacoes,
                                    ClubeService clube,GamificacaoService gamificacao,PixPagamentoService pagamentosPix) {
        this(produtos,pedidos,avaliacoes,clube,gamificacao,pagamentosPix,new OperacaoLojaService()::consultar);
    }
    ClienteVitrineController(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AvaliacaoService avaliacoes,
                             ClubeService clube,GamificacaoService gamificacao,PixPagamentoService pagamentosPix,
                             java.util.function.Supplier<ConfiguracaoLoja> consultarOperacao) {
        this(produtos,pedidos,avaliacoes,clube,gamificacao,pagamentosPix,consultarOperacao,() -> new LogisticaService().bairros(true));
    }
    ClienteVitrineController(ProdutoEstoqueService produtos,PedidoVendaService pedidos,AvaliacaoService avaliacoes,
                             ClubeService clube,GamificacaoService gamificacao,PixPagamentoService pagamentosPix,
                             java.util.function.Supplier<ConfiguracaoLoja> consultarOperacao,
                             java.util.function.Supplier<List<BairroEntrega>> consultarBairros) {
        this.consultarBairros=Objects.requireNonNull(consultarBairros);
        this.consultarOperacao=Objects.requireNonNull(consultarOperacao);
        this.produtosService=Objects.requireNonNull(produtos);this.pedidosService=Objects.requireNonNull(pedidos);this.avaliacoesService=Objects.requireNonNull(avaliacoes);
        this.clube=Objects.requireNonNull(clube);this.gamificacao=Objects.requireNonNull(gamificacao);
        this.pagamentosPix=Objects.requireNonNull(pagamentosPix);
    }
    private record FiltroCategoria(Long id,String nome) { }
    private record ChaveCarrinho(Long produtoId, UUID grupo) {
        static ChaveCarrinho de(PedidoItem item) {
            if (item.getPersonalizacao() != null) return new ChaveCarrinho(null,item.getPersonalizacao().grupo());
            return new ChaveCarrinho(item.getProduto().getId(),item.detalhesPreparo().isEmpty() ? null : UUID.randomUUID());
        }
    }
    private record Catalogo(List<Produto> produtos,List<Categoria> categorias,Map<Long,BigDecimal> medias,List<AvaliacaoFoto> fotos) { }
    private record Historico(List<Pedido> pedidos,long fila) { }
    private record DadosClube(ClubeService.Vitrine clube,GamificacaoService.Perfil gamificacao) { }

    @FXML private void initialize() {
        atualizacaoPedidos.setCycleCount(Timeline.INDEFINITE);
        atualizacaoLoja.setCycleCount(Timeline.INDEFINITE);
        contagemCupons.setCycleCount(Timeline.INDEFINITE);
        formaPagamento.getItems().setAll(FormaPagamento.PIX,FormaPagamento.DINHEIRO,FormaPagamento.CARTAO,FormaPagamento.CARTAO_APP);
        formaPagamento.setConverter(new StringConverter<>() {
            public String toString(FormaPagamento forma){return forma==null ? "" : forma==FormaPagamento.CARTAO
                    ? (entrega.isSelected() ? "Maquininha na Entrega" : "Cartão no Balcão") : forma.descricao();}
            public FormaPagamento fromString(String valor){return null;}
        });
        formaPagamento.valueProperty().addListener((o,a,n)->{atualizarCheckout();atualizarPix();});
        raiz.sceneProperty().addListener((obs, anterior, cena)->{
            if(cena==null){atualizacaoPedidos.stop();contagemCupons.stop();atualizacaoLoja.stop();geracaoLoja++;geracaoPedidos++;return;}
            cena.windowProperty().addListener((o, velha, janela)->{
                if(janela==null){atualizacaoPedidos.stop();contagemCupons.stop();atualizacaoLoja.stop();geracaoLoja++;geracaoPedidos++;}
                else janela.showingProperty().addListener((x,era,visivel)->{if(!visivel){atualizacaoPedidos.stop();contagemCupons.stop();atualizacaoLoja.stop();geracaoLoja++;geracaoPedidos++;}});
            });
        });
        ToggleGroup grupo=new ToggleGroup();retirada.setToggleGroup(grupo);entrega.setToggleGroup(grupo);local.setToggleGroup(grupo);retirada.setSelected(true);
        selecaoBairro.visibleProperty().bind(entrega.selectedProperty());
        selecaoBairro.managedProperty().bind(selecaoBairro.visibleProperty());
        bairroEntrega.valueProperty().addListener((o,a,n)->{if(!atualizandoBairros)simularDesconto();});
        agendar.selectedProperty().addListener((o,a,n)->atualizarCheckout());
        dataAgendamento.disableProperty().bind(agendar.selectedProperty().not());
        horaAgendamento.disableProperty().bind(agendar.selectedProperty().not());
        grupo.selectedToggleProperty().addListener((o,a,n)->{if(n==null)grupo.selectToggle(a);atualizarEndereco();simularDesconto();});
        categorias.setConverter(new StringConverter<>() {
            @Override public String toString(FiltroCategoria filtro) { return filtro==null?"Todas as categorias":filtro.nome(); }
            @Override public FiltroCategoria fromString(String texto) { return null; }
        });
        meusFavoritos.selectedProperty().addListener((o,a,n)->filtrar());
        categorias.valueProperty().addListener((o,a,n)->filtrar());
        busca.textProperty().addListener((o,a,n)->filtrar());
        cupom.textProperty().addListener((o,a,n)->atualizarCarrinho());
        cards.prefWrapLengthProperty().bind(rolagemProdutos.widthProperty().subtract(28));
        mostrarPainel(painelVitrine);atualizarCarrinho();
    }
    @Override public void definirUsuario(Usuario usuario) {
        if (!(usuario instanceof UsuarioCliente cliente) || !cliente.isAtivo()) throw new SecurityException("Acesso restrito a clientes ativos");
        this.usuario=cliente;saudacao.setText("Olá, "+usuario.getNome()+"!");atualizarEndereco();atualizarCarrinho();carregarProdutos();
        carregarCupons();carregarCarteira();contagemCupons.playFromStart();atualizarLoja();atualizacaoLoja.playFromStart();
    }
    private void atualizarLoja(){
        if (System.nanoTime()-ultimoRelogioCarteira>=30_000_000_000L) carregarCarteira();
        if(usuario==null||consultandoLoja)return;
        consultandoLoja=true;
        long geracao=geracaoLoja;
        long catalogo=geracaoProdutos;
        Task<OperacaoAtual> tarefa=new Task<>(){
            @Override protected OperacaoAtual call(){
                return new OperacaoAtual(consultarOperacao.get(),produtosService.listar().stream()
                        .filter(Produto::isDisponivelVenda).map(Produto::getId).collect(java.util.stream.Collectors.toSet()),consultarBairros.get());
            }
        };
        tarefa.setOnSucceeded(e->{
            consultandoLoja=false;if(geracao!=geracaoLoja)return;
            ConfiguracaoLoja anterior = configuracao;
            configuracao=tarefa.getValue().config();
            var bairros = tarefa.getValue().bairros();
            boolean mudouFrete = !carregouBairros || !bairrosAtivos.equals(bairros) || !Objects.equals(anterior, configuracao);
            if (!carregouBairros || !bairrosAtivos.equals(bairros)) atualizarBairros(bairros);
            if(carregouProdutos&&catalogo==geracaoProdutos){
                Set<Long> liberados=tarefa.getValue().produtosLiberados();
                boolean mudou=false;
                for(Produto p:produtos){
                    boolean disponivel=liberados.contains(p.getId());
                    mudou|=p.isDisponivelVenda()!=disponivel;
                    p.setDisponivelVenda(disponivel);
                }
                for(PedidoItem i:itensDoCarrinho()){
                    Produto p=i.getProduto();boolean disponivel=liberados.contains(p.getId());
                    mudou|=p.isDisponivelVenda()!=disponivel;p.setDisponivelVenda(disponivel);
                }
                if(mudou){filtrar();atualizarCarrinho();mudouFrete=false;}
            }
            statusLoja.setText(operacao.rotulo(configuracao));
            if (mudouFrete && entrega.isSelected()) simularDesconto(); else atualizarCheckout();
        });
        tarefa.setOnFailed(e->{consultandoLoja=false;if(geracao==geracaoLoja){
            configuracao=null;statusLoja.setText("Não foi possível consultar o horário da loja.");atualizarCheckout();
        }});
        iniciar(tarefa);
    }
    private void atualizarBairros(List<BairroEntrega> bairros) {
        var anterior = bairroEntrega.getValue();
        bairrosAtivos = List.copyOf(bairros);
        carregouBairros = true;
        atualizandoBairros = true;
        bairroEntrega.getItems().setAll(bairros.stream().map(OpcaoBairro::new).toList());
        var fora = new OpcaoBairro(null);
        bairroEntrega.getItems().add(fora);
        bairroEntrega.setValue(anterior == null ? null : anterior.bairro() == null ? fora : bairroEntrega.getItems().stream()
                .filter(o -> o.bairro() != null && Objects.equals(o.bairro().id(), anterior.bairro().id())).findFirst().orElse(null));
        atualizandoBairros = false;
    }
    private BairroEntrega bairroSelecionado() {
        var opcao = bairroEntrega.getValue();
        return opcao == null ? null : opcao.bairro();
    }
    private boolean entregaInvalida() {
        return entrega.isSelected() && (usuarioSemEndereco() || bairroSelecionado() == null);
    }
    private boolean usuarioSemEndereco() {
        return usuario == null || usuario.getCliente().getEndereco() == null || usuario.getCliente().getEndereco().isBlank();
    }
    private void atualizarCheckout(){
        boolean fechada=configuracao!=null&&!operacao.estaAberta(configuracao);
        if(fechada&&!agendar.isSelected())exibir(orientacaoLoja,"Pedidos imediatos pausados. Agende para um horário dentro do expediente.");
        else ocultar(orientacaoLoja);
        BigDecimal falta = configuracao == null || carrinho.isEmpty() ? BigDecimal.ZERO
                : OperacaoLojaService.faltaParaEntrega(configuracao, modalidade(), itensDoCarrinho());
        if (falta.signum() > 0) exibir(mensagemMinimo,"Adicione mais " + moeda(falta) + " para atingir o mínimo de entrega, sem frete.");
        else ocultar(mensagemMinimo);
        if (formaPagamento.getValue() == FormaPagamento.CARTAO_APP && (descontoAtual == null || descontoAtual.total().signum()>0))
            exibir(orientacaoCartao,"Combine o pagamento por link com a loja. O preparo começa após confirmação. Não informe dados do cartão aqui.");
        else ocultar(orientacaoCartao);
        finalizar.setDisable(descontoAtual==null||(descontoAtual.total().signum()>0 && formaPagamento.getValue()==null)||configuracao==null||(fechada&&!agendar.isSelected())||falta.signum()>0||entregaInvalida());
    }
    @FXML private void mostrarVitrine(){atualizacaoPedidos.stop();geracaoPedidos++;mostrarPainel(painelVitrine);simularDesconto();}
    @FXML private void mostrarPedidos(){mostrarPainel(painelPedidos);carregarCarteira();carregarPedidos();atualizacaoPedidos.playFromStart();}
    private void mostrarPainel(VBox visivel){
        painelVitrine.setVisible(visivel==painelVitrine);painelVitrine.setManaged(visivel==painelVitrine);
        painelPedidos.setVisible(visivel==painelPedidos);painelPedidos.setManaged(visivel==painelPedidos);
        painelClube.setVisible(visivel==painelClube);painelClube.setManaged(visivel==painelClube);
    }

    @FXML private void mostrarClube(){
        if(usuario==null)return;
        atualizacaoPedidos.stop();geracaoPedidos++;mostrarPainel(painelClube);
        long geracao=++geracaoClube;conteudoClube.getChildren().setAll(textoClube("Carregando Clube VIP…"));
        UsuarioCliente cliente=usuario;
        Task<DadosClube> tarefa=new Task<>(){@Override protected DadosClube call(){return new DadosClube(clube.vitrine(cliente),gamificacao.perfil(cliente));}};
        tarefa.setOnSucceeded(e->{if(geracao==geracaoClube&&painelClube.isVisible()){montarClube(tarefa.getValue());simularDesconto();}});
        tarefa.setOnFailed(e->{if(geracao==geracaoClube&&painelClube.isVisible())conteudoClube.getChildren().setAll(textoClube("Não foi possível carregar o clube. Tente novamente."));});
        iniciar(tarefa);
    }
    private void montarClube(DadosClube dados){
        ClubeService.Vitrine visao=dados.clube();
        AssinaturaCliente assinatura=visao.assinatura();
        MensalidadeClube pendencia=visao.pendencia();
        boolean ativa=assinatura!=null&&assinatura.ativa(visao.hoje());
        String status;
        if(ativa)status="Ativa · "+assinatura.plano()+" · "+percentual(assinatura.descontoVigente())+
                " OFF nos itens elegíveis + frete grátis · válida até "+assinatura.validaAte().minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        else if(pendencia!=null)status="Aguardando confirmação do pagamento.";
        else if(assinatura==null)status="Você ainda não tem assinatura.";
        else status=assinatura.cancelada()?"Assinatura cancelada.":"Assinatura vencida. Solicite a renovação para reativar.";
        Label estado=textoClube(status);estado.setId("clube-status");
        conteudoClube.getChildren().setAll(estado,textoClube("Vigência mensal. Não há débito automático: solicite a mensalidade e aguarde a confirmação da loja."));
        if(pendencia!=null){
            PixPainel pixMensalidade=new PixPainel();
            pixMensalidade.atualizar(true,pendencia.valor(),"C"+pendencia.id(),"Pague uma vez. A loja confere o recebimento antes de ativar ou renovar.");
            conteudoClube.getChildren().addAll(textoClube("Mensalidade #"+pendencia.id()+" · "+pendencia.plano()+" · "+moeda(pendencia.valor())),pixMensalidade);
        }
        if(assinatura!=null&&!assinatura.cancelada()){
            Button cancelar=new Button("Cancelar assinatura");
            cancelar.setOnAction(e->{
                Alert alerta=new Alert(Alert.AlertType.CONFIRMATION,"O cancelamento encerra o benefício e cancela a cobrança pendente. Ele não gera reembolso automático.",
                        ButtonType.CANCEL,ButtonType.OK);alerta.initOwner(raiz.getScene().getWindow());
                if(alerta.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK)operarClube(()->clube.cancelar(usuario));
            });
            conteudoClube.getChildren().add(cancelar);
        }
        if(ativa&&!assinatura.renovavel(visao.hoje()))conteudoClube.getChildren().add(textoClube("A renovação fica disponível nos sete dias anteriores ao vencimento."));
        conteudoClube.getChildren().add(new Label("Plano único pré-pago"));
        if(visao.planos().isEmpty())conteudoClube.getChildren().add(textoClube("Plano VIP indisponível no momento."));
        for(PlanoAssinatura plano:visao.planos()){
            Button assinar=new Button(ativa?"Solicitar renovação":"Assinar");
            assinar.setDisable(pendencia!=null||(ativa&&(assinatura.planoId()!=plano.id()||!assinatura.renovavel(visao.hoje()))));
            assinar.setOnAction(e->operarClube(()->clube.solicitar(usuario,plano.id())));
            VBox cartao=new VBox(6,textoClube(plano.nome()+" · "+moeda(plano.mensalidade())+"/mês · "+percentual(plano.descontoPercentual())+" OFF + Frete Grátis"),
                    textoClube(plano.descricao()),assinar);cartao.getStyleClass().add("pedido-card");conteudoClube.getChildren().add(cartao);
        }
        GamificacaoService.Perfil perfil=dados.gamificacao();
        conteudoClube.getChildren().addAll(new Separator(),new Label("Suas conquistas · "+perfil.pontos()+" pontos da Roleta (não resgatáveis)"));
        if(perfil.selos().isEmpty())conteudoClube.getChildren().add(textoClube("Nenhuma conquista desbloqueada ainda."));
        for(ConquistaCliente selo:perfil.selos())conteudoClube.getChildren().add(textoClube("🏅 "+selo.nome()+" — "+selo.descricao()));
        cuponsDaRoleta=perfil.cupons();
        atualizarContagemCupons();
        for(CupomRoleta premio:perfil.cupons()){
            Button usar=new Button("Usar "+premio.descricao()+" · "+premio.codigo());usar.setWrapText(true);
            usar.setOnAction(e->{cupom.setText(premio.codigo());mostrarVitrine();});
            conteudoClube.getChildren().addAll(textoClube("Válido até "+premio.validoAte().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                    +" · mínimo de "+moeda(premio.minimo())),usar);
        }
    }
    private void carregarCupons(){
        if(usuario==null||consultandoCupons)return;
        consultandoCupons=true;ultimoRelogioCupons=System.nanoTime();UsuarioCliente cliente=usuario;
        Task<List<CupomRoleta>> tarefa=new Task<>(){@Override protected List<CupomRoleta> call(){return gamificacao.perfil(cliente).cupons();}};
        tarefa.setOnSucceeded(e->{consultandoCupons=false;if(usuario==cliente){cuponsDaRoleta=tarefa.getValue();atualizarContagemCupons();}});
        tarefa.setOnFailed(e->consultandoCupons=false);iniciar(tarefa);
    }
    private void atualizarContagemCupons(){
        if(System.nanoTime()-ultimoRelogioCupons>=30_000_000_000L)carregarCupons();
        LocalDateTime agora=LocalDateTime.now();
        var proximo=cuponsDaRoleta.stream().filter(c->c.validoAte().isAfter(agora)&&c.validoAte().isBefore(agora.plusHours(24)))
                .min(Comparator.comparing(CupomRoleta::validoAte));
        if(proximo.isEmpty()){ocultar(cupomUrgente);return;}
        CupomRoleta premio=proximo.get();
        long segundos=Math.max(1,java.time.Duration.between(agora,premio.validoAte()).toSeconds()+1);
        exibir(cupomUrgente,String.format(BR,"⏳ %s expira em %dh %02dmin %02ds · veja em Clube VIP",
                premio.descricao(),segundos/3600,(segundos/60)%60,segundos%60));
    }
    private void operarClube(Runnable operacao){
        raiz.setDisable(true);
        Task<Void> tarefa=new Task<>(){@Override protected Void call(){operacao.run();return null;}};
        tarefa.setOnSucceeded(e->{raiz.setDisable(false);mostrarClube();});
        tarefa.setOnFailed(e->{raiz.setDisable(false);Throwable erro=tarefa.getException();avisar(Alert.AlertType.WARNING,"Operação não concluída",
                erro instanceof IllegalArgumentException?erro.getMessage():"Não foi possível atualizar a assinatura. Tente novamente.");});
        iniciar(tarefa);
    }
    private static String percentual(BigDecimal valor){return NumberFormat.getNumberInstance(BR).format(valor)+"%";}
    private static Label textoClube(String texto){Label l=new Label(texto);l.setWrapText(true);return l;}
    @FXML private void carregarProdutos(){
        long geracao=++geracaoProdutos;carregouProdutos=false;cards.getChildren().clear();exibir(mensagemProdutos,"Carregando produtos...");
        galeriaFotos.getChildren().clear();exibir(mensagemFotos,"Carregando fotos de avaliações…");
        Task<Catalogo> tarefa=new Task<>() { @Override protected Catalogo call(){return new Catalogo(produtosService.listar(),produtosService.listarCategoriasAtivas(),avaliacoesService.mediasPorProduto(),avaliacoesService.fotosRecentes());} };
        tarefa.setOnSucceeded(e->{
            if(geracao!=geracaoProdutos)return;
            produtos=List.copyOf(tarefa.getValue().produtos());medias=Map.copyOf(tarefa.getValue().medias());carregouProdutos=true;
            Long selecionada=categorias.getValue()==null?null:categorias.getValue().id();
            List<FiltroCategoria> filtros=new ArrayList<>();filtros.add(new FiltroCategoria(null,"Todas as categorias"));
            tarefa.getValue().categorias().stream()
                    .sorted(Comparator.comparing(Categoria::getNome,String.CASE_INSENSITIVE_ORDER)).forEach(c->filtros.add(new FiltroCategoria(c.getId(),c.getNome())));
            categorias.getItems().setAll(filtros);categorias.setValue(filtros.stream().filter(f->Objects.equals(f.id(),selecionada)).findFirst().orElse(filtros.get(0)));
            Map<Long,Produto> atuais=new HashMap<>();produtos.forEach(p->atuais.put(p.getId(),p));
            carrinho.replaceAll((chave,itens)->itens.stream().map(i->i.comProdutoAtual(atuais.getOrDefault(i.getProduto().getId(),i.getProduto()))).toList());
            montarGaleria(tarefa.getValue().fotos());atualizarCarrinho();filtrar();
        });
        tarefa.setOnFailed(e->{if(geracao!=geracaoProdutos)return;exibir(mensagemProdutos,"Não foi possível carregar os produtos. Verifique a conexão e tente novamente.");
            exibir(mensagemFotos,"Não foi possível carregar as fotos de avaliações.");});iniciar(tarefa);
    }
    private void montarGaleria(List<AvaliacaoFoto> fotos){
        galeriaFotos.getChildren().clear();
        for(AvaliacaoFoto foto:fotos){
            FotoAvaliacaoUi.miniatura(foto.fotoUrl(),110,82,"Foto de avaliação de "+foto.produto()).ifPresent(imagem->{
                imagem.setId("foto-avaliacao-"+foto.id());
                Label legenda=new Label(foto.produto()+" · ★ "+foto.nota());legenda.setWrapText(true);legenda.setMaxWidth(115);
                VBox miniatura=new VBox(5,imagem,legenda);miniatura.setAlignment(Pos.TOP_CENTER);galeriaFotos.getChildren().add(miniatura);
            });
        }
        if(galeriaFotos.getChildren().isEmpty())exibir(mensagemFotos,"Ainda não há fotos de avaliações disponíveis.");
        else ocultar(mensagemFotos);
    }
    private void carregarCarteira() {
        if (usuario == null || carregandoCarteira || alterandoFavorito) return;
        carregandoCarteira = true;
        ultimoRelogioCarteira = System.nanoTime();
        UsuarioCliente cliente = usuario;
        Task<FidelidadeService.Carteira> t = new Task<>() {
            @Override protected FidelidadeService.Carteira call() { return fidelidade.carteira(cliente); }
        };
        t.setOnSucceeded(e -> {
            carregandoCarteira = false;
            if (usuario != cliente) return;
            var carteira = t.getValue();
            boolean mudou = !Objects.equals(pontosResgate,carteira.saldo()) || !favoritos.equals(carteira.favoritos());
            pontosResgate = carteira.saldo(); favoritos = carteira.favoritos();
            saldoResgate.setText(pontosResgate + " pontos para resgate · validade de 60 dias");
            if (mudou) filtrar();
        });
        t.setOnFailed(e -> {
            carregandoCarteira = false;
            if (usuario != cliente) return;
            pontosResgate = null;
            saldoResgate.setText("Carteira e favoritos indisponíveis no momento.");
            filtrar();
        });
        iniciar(t);
    }
    private void alterarFavorito(Produto produto) {
        if (usuario == null || carregandoCarteira || alterandoFavorito || pontosResgate == null) return;
        boolean incluir = !favoritos.contains(produto.getId());
        UsuarioCliente cliente = usuario;
        alterandoFavorito = true;
        Task<Void> t = new Task<>() {
            @Override protected Void call() { fidelidade.definirFavorito(cliente,produto.getId(),incluir);return null; }
        };
        t.setOnSucceeded(e -> {
            alterandoFavorito = false;
            if (usuario != cliente) return;
            var atualizados = new HashSet<>(favoritos);
            if (incluir) atualizados.add(produto.getId()); else atualizados.remove(produto.getId());
            favoritos = Set.copyOf(atualizados);filtrar();
        });
        t.setOnFailed(e -> { alterandoFavorito=false; avisar(Alert.AlertType.WARNING,"Favorito não salvo","Tente novamente."); });
        iniciar(t);
    }
    private void resgatar(Produto produto) {
        if (pontosResgate == null || produto.getPontosResgate() == null || pontosResgate < produto.getPontosResgate() || !produto.isDisponivelVenda()) return;
        carrinho.values().removeIf(itens -> itens.get(0).isResgate());
        PedidoItem resgate = PedidoItem.resgate(produto);
        carrinho.put(ChaveCarrinho.de(resgate),List.of(resgate));
        atualizarCarrinho();
    }
    private void atualizarSugestao() {
        sugestaoCarrinho.getChildren().clear();
        FidelidadeService.sugestao(produtos,itensDoCarrinho()).ifPresent(produto -> {
            Button incluir = new Button("Adicionar " + produto.getNome() + " · " + moeda(produto.getPrecoVenda()));
            incluir.setId("adicionar-sugestao");incluir.setWrapText(true);incluir.setMaxWidth(Double.MAX_VALUE);
            incluir.getStyleClass().add("button-secondary");incluir.setOnAction(e -> alterarQuantidade(produto,1));
            sugestaoCarrinho.getChildren().addAll(textoClube("Quer algo para acompanhar?"),incluir);
        });
        sugestaoCarrinho.setVisible(!sugestaoCarrinho.getChildren().isEmpty());
        sugestaoCarrinho.setManaged(sugestaoCarrinho.isVisible());
    }
    private void filtrar(){
        if(!carregouProdutos)return;
        String termo=normalizar(busca.getText());FiltroCategoria filtro=categorias.getValue();
        List<Produto> selecionados=produtos.stream().filter(p->filtro==null||filtro.id()==null||(p.getCategoria()!=null&&Objects.equals(filtro.id(),p.getCategoria().getId())))
                .filter(p -> !meusFavoritos.isSelected() || pontosResgate != null && favoritos.contains(p.getId()))
                .filter(p->normalizar(p.getNome()).contains(termo)||normalizar(p.getDescricao()).contains(termo)).toList();
        cards.getChildren().setAll(selecionados.stream().map(this::cardProduto).toList());
        if(produtos.isEmpty())exibir(mensagemProdutos,"Nenhum salgado cadastrado ainda. Volte em breve!");
        else if (meusFavoritos.isSelected() && pontosResgate == null) exibir(mensagemProdutos,"Não foi possível exibir seus favoritos agora.");
        else if (meusFavoritos.isSelected() && favoritos.isEmpty()) exibir(mensagemProdutos,"Nenhum favorito ainda. Toque no coração de um item do cardápio.");
        else if(selecionados.isEmpty())exibir(mensagemProdutos,"Nenhum salgado encontrado para esta busca ou categoria.");
        else ocultar(mensagemProdutos);
    }
    private Node cardProduto(Produto produto){
        VBox card=new VBox(9);card.getStyleClass().add("produto-card");card.setPrefWidth(220);card.setMinWidth(200);card.setMaxWidth(240);
        StackPane imagem=new StackPane();imagem.getStyleClass().add("produto-imagem");imagem.setPrefHeight(132);imagem.setMinHeight(132);
        Label fallback=new Label("SALGADERIA");fallback.getStyleClass().add("imagem-fallback");imagem.getChildren().add(fallback);
        String url=produto.getUrlImagem();
        if(url!=null&&!url.isBlank()){
            try {
                URI uri=URI.create(url.trim());String esquema=uri.getScheme();
                if(esquema!=null && (esquema.equalsIgnoreCase("http")||esquema.equalsIgnoreCase("https")||esquema.equalsIgnoreCase("file"))){
                    Image foto=new Image(uri.toString(),440,264,true,true,true);ImageView view=new ImageView(foto);view.setFitWidth(200);view.setFitHeight(132);view.setPreserveRatio(true);view.setVisible(false);
                    foto.progressProperty().addListener((o,a,n)->{if(n.doubleValue()>=1&&!foto.isError())view.setVisible(true);});
                    if(foto.getProgress()>=1&&!foto.isError())view.setVisible(true);imagem.getChildren().add(view);
                }
            } catch(IllegalArgumentException ignored) { /* Mantém fallback para URL inválida. */ }
        }
        Label nome=new Label(produto.getNome());nome.setWrapText(true);nome.getStyleClass().add("produto-nome");
        Label descricao=new Label(produto.getDescricao()==null||produto.getDescricao().isBlank()?"Sem descrição":produto.getDescricao());descricao.setWrapText(true);descricao.setMaxHeight(48);descricao.getStyleClass().add("produto-descricao");
        Label preco=new Label(moeda(produto.getPrecoVenda()));preco.getStyleClass().add("produto-preco");
        BigDecimal media=medias.get(produto.getId());Label estrelas=new Label(media==null?"Sem avaliações":"★ "+String.format(BR,"%.1f",media));estrelas.getStyleClass().add("produto-estrelas");
        boolean disponivel=produto.isDisponivelVenda();
        Label estoque=new Label(disponivel?"Disponível":"Indisponível no momento");estoque.setWrapText(true);
        estoque.getStyleClass().add(disponivel?"estoque-disponivel":"estoque-esgotado");
        boolean temOpcionais = !produto.getOpcionais().isEmpty();
        Button adicionar=new Button(temOpcionais ? "Escolher opcionais" : "Adicionar +");adicionar.setId("adicionar-"+produto.getId());adicionar.getStyleClass().add("button-primary");adicionar.setMaxWidth(Double.MAX_VALUE);adicionar.setDisable(!disponivel);
        adicionar.setOnAction(e->{if (temOpcionais) personalizarProduto(produto); else alterarQuantidade(produto,1);});
        Button observacao = new Button("Adicionar com observação"); observacao.getStyleClass().add("button-secondary");
        observacao.setId("observar-"+produto.getId()); observacao.setMaxWidth(Double.MAX_VALUE); observacao.setDisable(!disponivel);
        observacao.setOnAction(e->personalizarProduto(produto));
        card.setOnMouseClicked(e -> {
            if (!disponivel || e.getButton() != javafx.scene.input.MouseButton.PRIMARY || !(e.getTarget() instanceof Node alvo)) return;
            for (Node atual = alvo; atual != null && atual != card; atual = atual.getParent()) {
                if (atual instanceof ButtonBase) return;
            }
            personalizarProduto(produto);
        });
        HBox info=new HBox(8,estrelas,estoque);info.setAlignment(Pos.CENTER_LEFT);
        Button favorito = new Button(favoritos.contains(produto.getId()) ? "❤️" : "♡");
        favorito.setId("favorito-"+produto.getId());favorito.setDisable(pontosResgate == null);
        favorito.setAccessibleText((favoritos.contains(produto.getId()) ? "Remover favorito: " : "Favoritar: ")+produto.getNome());
        favorito.setOnAction(e -> alterarFavorito(produto));
        HBox cabecalho = new HBox(5,nome,favorito);HBox.setHgrow(nome,Priority.ALWAYS);
        card.getChildren().addAll(imagem,cabecalho);
        if (produto.getEtiquetaDestaque() != null) {
            Label etiqueta = textoClube(produto.getEtiquetaDestaque());
            etiqueta.getStyleClass().add("etiqueta-destaque");card.getChildren().add(etiqueta);
        }
        card.getChildren().addAll(descricao,preco,info,adicionar);
        if (produto.getPontosResgate() != null) {
            Button resgate = new Button("Resgatar por " + produto.getPontosResgate() + " pts");
            resgate.getStyleClass().add("button-outline");resgate.setWrapText(true);resgate.setMaxWidth(Double.MAX_VALUE);
            resgate.setDisable(!disponivel || pontosResgate == null || pontosResgate < produto.getPontosResgate());
            resgate.setOnAction(e -> resgatar(produto));card.getChildren().add(resgate);
        }
        if (!temOpcionais) card.getChildren().add(observacao);
        return card;
    }
    private void personalizarProduto(Produto produto) {
        ButtonType incluir = new ButtonType("Adicionar ao carrinho", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = personalizar(produto.getNome(), incluir);
        dialog.getDialogPane().setId("personalizar-produto");
        List<ProdutoOpcional> catalogo = produto.getOpcionais();
        Map<Long, CheckBox> escolhas = new LinkedHashMap<>();
        VBox campos = new VBox(9); campos.setPadding(new Insets(10));
        Map<String,List<ProdutoOpcional>> grupos = new LinkedHashMap<>();
        catalogo.forEach(o -> grupos.computeIfAbsent(o.grupoNome(), g -> new ArrayList<>()).add(o));
        for (var grupo : grupos.entrySet()) {
            var regra = grupo.getValue().get(0);
            campos.getChildren().add(textoClube(grupo.getKey() + " · até " + regra.maximoEscolhas()
                    + (regra.gratisAteLimite() == 0 ? "" : " · " + regra.gratisAteLimite() + " com preço dispensado por ordem da lista")));
            for (var opcao : grupo.getValue()) {
                CheckBox escolha = new CheckBox(opcao.nome() + (opcao.precoAdicional().signum() == 0 ? " · grátis" : " · +" + moeda(opcao.precoAdicional())));
                escolha.setId("opcional-" + opcao.id());
                escolha.setWrapText(true);
                escolhas.put(opcao.id(), escolha); campos.getChildren().add(escolha);
            }
        }
        TextArea observacao = new TextArea(); observacao.setId("produto-observacao");
        observacao.setPromptText("Ex: sem cebola, bem assado"); observacao.setWrapText(true); observacao.setPrefRowCount(3);
        observacao.setTextFormatter(new TextFormatter<>(alteracao -> alteracao.getControlNewText().length() <= 512 ? alteracao : null));
        Label previa = textoClube("");
        Runnable atualizar = () -> {
            Set<Long> selecionadas = marcados(escolhas);
            for (var lista : grupos.values()) {
                long quantidade = lista.stream().filter(o -> selecionadas.contains(o.id())).count();
                for (var o : lista) escolhas.get(o.id()).setDisable(!selecionadas.contains(o.id()) && quantidade >= o.maximoEscolhas());
            }
            try {
                PedidoItem item = ProdutoPersonalizacaoService.montar(produto, catalogo, selecionadas, observacao.getText(), BigDecimal.ONE);
                previa.setText("Preço unitário: " + moeda(item.getPrecoUnitario()));
                dialog.getDialogPane().lookupButton(incluir).setDisable(false);
            } catch (IllegalArgumentException e) {
                previa.setText(e.getMessage()); dialog.getDialogPane().lookupButton(incluir).setDisable(true);
            }
        };
        escolhas.values().forEach(c -> c.selectedProperty().addListener((o,a,n)->atualizar.run()));
        campos.getChildren().addAll(new Label("Observação para a cozinha (opcional)"), observacao, previa);
        ScrollPane scroll = new ScrollPane(campos); scroll.setFitToWidth(true); scroll.setPrefViewportWidth(390);
        scroll.setPrefViewportHeight(Math.min(420, 155 + grupos.size()*32 + catalogo.size()*34));
        dialog.getDialogPane().setContent(scroll); atualizar.run();
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != incluir) return;
        PedidoItem item = ProdutoPersonalizacaoService.montar(produto, catalogo, marcados(escolhas), observacao.getText(), BigDecimal.ONE);
        if (item.detalhesPreparo().isEmpty()) alterarQuantidade(produto,1);
        else adicionarPersonalizado(List.of(item));
    }
    private void alterarQuantidade(Produto produto,int delta){
        ChaveCarrinho chave=new ChaveCarrinho(produto.getId(),null);
        var existente=carrinho.get(chave);
        BigDecimal nova=(existente==null?BigDecimal.ZERO:existente.get(0).getQuantidade()).add(BigDecimal.valueOf(delta));
        if(delta>0){
            try{produto.exigirDisponivelParaVenda();}
            catch(ProdutoIndisponivelException e){avisar(Alert.AlertType.WARNING,"Produto indisponível",e.getMessage());return;}
        }
        if(nova.signum()<=0)carrinho.remove(chave);
        else carrinho.put(chave,List.of(new PedidoItem(null,produto,nova,produto.getPrecoVenda())));
        atualizarCarrinho();
    }
    private List<PedidoItem> itensDoCarrinho(){return carrinho.values().stream().flatMap(List::stream).toList();}

    @FXML private void montarCombo(){
        List<Produto> disponiveis=produtosDisponiveis();
        List<Produto> bebidas=disponiveis.stream().filter(PersonalizacaoService::bebida).toList();
        List<Produto> comidas=disponiveis.stream().filter(p->!PersonalizacaoService.bebida(p)).toList();
        if(bebidas.isEmpty()||comidas.size()<2){
            avisar(Alert.AlertType.INFORMATION,"Monte seu Combo","O cardápio precisa de dois produtos diferentes e uma bebida disponíveis.");return;
        }
        ButtonType adicionar=new ButtonType("Adicionar ao carrinho",ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog=personalizar("Monte seu Combo",adicionar);
        ComboBox<Produto> principal=seletorCombo("combo-principal",comidas);
        ComboBox<Produto> acompanhamento=seletorCombo("combo-acompanhamento",comidas);
        ComboBox<Produto> bebida=seletorCombo("combo-bebida",bebidas);
        Label previa=new Label("Escolha um principal, um acompanhamento diferente e uma bebida.");
        previa.setId("combo-previa");previa.setWrapText(true);
        Node botao=dialog.getDialogPane().lookupButton(adicionar);botao.setDisable(true);
        java.util.function.Supplier<List<PedidoItem>> montar=()->new ComboBuilder().principal(principal.getValue())
                .acompanhamento(acompanhamento.getValue()).bebida(bebida.getValue()).montar();
        Runnable atualizar=()->{
            try{
                var itens=montar.get();
                previa.setText(itens.get(0).getObservacaoPersonalizada()+"\n"+moeda(precoEstimado(itens))+" · 10% OFF no combo");
                botao.setDisable(false);
            }catch(IllegalArgumentException erro){previa.setText(erro.getMessage());botao.setDisable(true);}
        };
        for(ComboBox<Produto> seletor:List.of(principal,acompanhamento,bebida))seletor.valueProperty().addListener((o,a,n)->atualizar.run());
        dialog.getDialogPane().setContent(new VBox(10,new Label("Principal"),principal,new Label("Acompanhamento"),acompanhamento,
                new Label("Bebida"),bebida,previa,textoClube("Ao finalizar, vale o melhor desconto não cumulativo.")));
        if(dialog.showAndWait().orElse(ButtonType.CANCEL)==adicionar){
            try{adicionarPersonalizado(montar.get());}
            catch(IllegalArgumentException erro){avisar(Alert.AlertType.WARNING,"Combo indisponível",erro.getMessage());}
        }
    }
    private ComboBox<Produto> seletorCombo(String id,List<Produto> opcoes){
        ComboBox<Produto> seletor=new ComboBox<>();seletor.setId(id);seletor.getItems().setAll(opcoes);
        seletor.setConverter(rotulos(p->p.getNome()+" · "+moeda(p.getPrecoVenda())));return seletor;
    }

    @FXML private void caixaMisteriosa(){
        List<Produto> catalogo=produtosDisponiveis();
        if(catalogo.isEmpty()){
            avisar(Alert.AlertType.INFORMATION,"Caixa Misteriosa","Não há produtos disponíveis para o sorteio no momento.");return;
        }
        ButtonType sortear=new ButtonType("Sortear caixa",ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog=personalizar("Caixa Misteriosa Ui K'Fomi",sortear);
        ComboBox<Tipo> orcamento=new ComboBox<>();orcamento.setId("caixa-orcamento");
        orcamento.getItems().setAll(Tipo.CAIXA_25,Tipo.CAIXA_40,Tipo.CAIXA_60);orcamento.setValue(Tipo.CAIXA_25);
        Map<Long,CheckBox> categoriasExcluidas=new LinkedHashMap<>(), produtosExcluidos=new LinkedHashMap<>();
        VBox categoriasBox=new VBox(5), produtosBox=new VBox(5);
        for(Produto produto:catalogo){
            Categoria categoria=produto.getCategoria();
            if(categoria!=null&&categoria.getId()!=null&&!categoriasExcluidas.containsKey(categoria.getId())){
                CheckBox excluir=new CheckBox(categoria.getNome());excluir.setId("excluir-categoria-"+categoria.getId());
                categoriasExcluidas.put(categoria.getId(),excluir);categoriasBox.getChildren().add(excluir);
            }
            CheckBox excluir=new CheckBox(produto.getNome());excluir.setId("excluir-produto-"+produto.getId());
            produtosExcluidos.put(produto.getId(),excluir);produtosBox.getChildren().add(excluir);
        }
        if(categoriasExcluidas.isEmpty())categoriasBox.getChildren().add(textoClube("Nenhuma categoria disponível para excluir."));
        var filtros=new ScrollPane(new VBox(10,new Label("Excluir categorias (opcional)"),categoriasBox,new Separator(),
                new Label("Excluir itens (opcional)"),produtosBox));
        filtros.setFitToWidth(true);filtros.setPrefViewportHeight(200);
        Label aviso=textoClube("Preço fechado, com até 15% a mais em produtos. Excluir itens não garante ausência de ingredientes ou contato cruzado.");
        aviso.setMaxWidth(380);
        dialog.getDialogPane().setContent(new VBox(10,aviso,new Label("Orçamento da caixa"),orcamento,filtros));
        if(dialog.showAndWait().orElse(ButtonType.CANCEL)!=sortear)return;
        Tipo tipo=orcamento.getValue();var categorias=marcados(categoriasExcluidas);var produtos=marcados(produtosExcluidos);
        raiz.setDisable(true);
        Task<List<PedidoItem>> tarefa=new Task<>(){
            @Override protected List<PedidoItem> call(){return personalizacoes.sortearCaixa(tipo,catalogo,categorias,produtos);}
        };
        tarefa.setOnFailed(e->{raiz.setDisable(false);Throwable erro=tarefa.getException();
            avisar(Alert.AlertType.INFORMATION,"Caixa não disponível",erro instanceof IllegalArgumentException?erro.getMessage():"Não foi possível sortear. Tente novamente.");});
        tarefa.setOnSucceeded(e->{raiz.setDisable(false);var itens=tarefa.getValue();
            ButtonType adicionar=new ButtonType("Adicionar ao carrinho",ButtonBar.ButtonData.OK_DONE);
            Dialog<ButtonType> confirmar=personalizar("Confirme sua Caixa Misteriosa",adicionar);
            confirmar.getDialogPane().setContent(textoClube("Preço fechado: "+moeda(tipo.orcamento())+
                    "\nValor de catálogo: "+moeda(subtotal(itens))+"\nO conteúdo é revelado após a entrega do pedido."));
            if(confirmar.showAndWait().orElse(ButtonType.CANCEL)==adicionar)adicionarPersonalizado(itens);
        });
        iniciar(tarefa);
    }
    private static Set<Long> marcados(Map<Long,CheckBox> opcoes){
        Set<Long> ids=new HashSet<>();opcoes.forEach((id,opcao)->{if(opcao.isSelected())ids.add(id);});return ids;
    }
    private static BigDecimal precoEstimado(List<PedidoItem> itens){
        var grupo=itens.get(0).getPersonalizacao();
        BigDecimal bruto=subtotal(itens);
        if(grupo==null)return bruto;
        if(grupo.tipo().orcamento()!=null)return grupo.tipo().orcamento();
        return bruto.subtract(new DescontoCombo().calcular(new ContextoDesconto(itens,0,null)));
    }

    private List<Produto> produtosDisponiveis(){
        return produtos.stream().filter(Produto::isDisponivelVenda).toList();
    }
    private void adicionarPersonalizado(List<PedidoItem> itens){
        try{itens.forEach(i->i.getProduto().exigirDisponivelParaVenda());}
        catch(ProdutoIndisponivelException e){avisar(Alert.AlertType.WARNING,"Produto indisponível",e.getMessage());return;}
        carrinho.put(ChaveCarrinho.de(itens.get(0)),List.copyOf(itens));atualizarCarrinho();
    }
    private Dialog<ButtonType> personalizar(String titulo,ButtonType confirmar){
        Dialog<ButtonType> dialog=new Dialog<>();dialog.initOwner(raiz.getScene().getWindow());dialog.setTitle(titulo);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL,confirmar);
        return dialog;
    }
    private static <T> StringConverter<T> rotulos(java.util.function.Function<T,String> texto){
        return new StringConverter<>(){
            public String toString(T valor){return valor==null?"":texto.apply(valor);}
            public T fromString(String texto){return null;}
        };
    }

    private void atualizarCarrinho(){
        itensCarrinho.getChildren().clear();BigDecimal soma=BigDecimal.ZERO;long qtd=0;
        for(var entrada:carrinho.entrySet()){
            List<PedidoItem> itens=entrada.getValue();
            PedidoItem primeiro=itens.get(0);Produto p=primeiro.getProduto();
            boolean personalizado=primeiro.getPersonalizacao()!=null;
            BigDecimal preco=precoEstimado(itens);
            qtd+=personalizado?1:primeiro.getQuantidade().intValueExact();soma=soma.add(preco);
            Label nome=new Label(personalizado?primeiro.getObservacaoPersonalizada():p.getNome());nome.setWrapText(true);nome.getStyleClass().add("carrinho-nome");
            Label subtotal=new Label(primeiro.isResgate() ? "Sem cobrança" : moeda(preco));subtotal.getStyleClass().add("subtitle");
            VBox detalhes=new VBox(4,nome,subtotal);HBox.setHgrow(detalhes,Priority.ALWAYS);
            boolean preparo = !primeiro.detalhesPreparo().isEmpty();
            if (preparo) detalhes.getChildren().add(textoClube(primeiro.getQuantidade().stripTrailingZeros().toPlainString() + " ×\n" + primeiro.detalhesPreparo()));
            HBox linha=new HBox(7,detalhes);
            if(personalizado || preparo){
                Button remover=new Button("Remover");remover.setOnAction(e->{carrinho.remove(entrada.getKey());atualizarCarrinho();});
                linha.getChildren().add(remover);
            }else{
                Button menos=new Button("−");menos.getStyleClass().add("quantidade-botao");menos.setAccessibleText("Remover uma unidade de "+p.getNome());menos.setOnAction(e->alterarQuantidade(p,-1));
                Button mais=new Button("+");mais.getStyleClass().add("quantidade-botao");mais.setAccessibleText("Adicionar uma unidade de "+p.getNome());mais.setOnAction(e->alterarQuantidade(p,1));
                linha.getChildren().addAll(menos,new Label(primeiro.getQuantidade().stripTrailingZeros().toPlainString()),mais);
            }
            linha.setAlignment(Pos.CENTER_LEFT);linha.getStyleClass().add("carrinho-item");itensCarrinho.getChildren().add(linha);
        }
        if(carrinho.isEmpty()){Label vazio=new Label("Seu carrinho está vazio. Escolha seus salgados favoritos.");vazio.setWrapText(true);vazio.getStyleClass().add("subtitle");itensCarrinho.getChildren().add(vazio);}
        atualizarSugestao();
        total.setText(moeda(soma));contador.setText(qtd+ (qtd==1?" item":" itens"));
        simularDesconto();
    }
    private static BigDecimal subtotal(List<PedidoItem> itens){
        return itens.stream().map(PedidoItem::calcularSubtotal).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    private Pedido pedidoAtual(){
        TipoEntrega tipo=modalidade();
        String enderecoEntrega=tipo==TipoEntrega.ENTREGA?usuario.getCliente().getEndereco():null;
        Pedido pedido = new Pedido(null,usuario,usuario.getCliente(),StatusPedido.PENDENTE,tipo,enderecoEntrega,LocalDateTime.now(),itensDoCarrinho());
        if (tipo == TipoEntrega.ENTREGA) pedido.setBairroId(bairroSelecionado().id());
        return pedido;
    }
    private void simularDesconto(){
        long geracao=++geracaoDesconto;descontoAtual=null;atualizarCheckout();
        atualizarPix();
        ocultar(mensagemFrete);
        if (entrega.isSelected()) {
            if (bairroSelecionado() == null) {
                exibir(mensagemFrete, !carregouBairros ? "Aguardando a lista de bairros atendidos."
                        : bairroEntrega.getValue() != null || bairrosAtivos.isEmpty() ? "Entrega indisponível para esta região. Retire seu pedido no local."
                        : "Selecione o bairro de entrega.");
            } else if (configuracao != null) {
                var cotacao = LogisticaService.cotar(bairroSelecionado(), configuracao);
                exibir(mensagemFrete, "Frete antes dos benefícios: " + moeda(cotacao.taxa()) + " · " + cotacao.minutos() + " min"
                        + (configuracao.modoChuvaAtivo() ? " · Chuva/Pico" : ""));
            }
        }
        if(usuario==null||carrinho.isEmpty()||entregaInvalida()){mensagemDesconto.setText("");return;}
        mensagemDesconto.setText("Calculando melhor desconto...");
        Pedido pedido=pedidoAtual();String codigo=cupom.getText();
        Task<ResultadoDesconto> tarefa=new Task<>(){@Override protected ResultadoDesconto call(){return pedidosService.simular(pedido,codigo);}};
        tarefa.setOnSucceeded(e->{if(geracao!=geracaoDesconto)return;descontoAtual=tarefa.getValue();total.setText(moeda(descontoAtual.total()));
            if (pedido.getTipoEntrega() == TipoEntrega.ENTREGA) {
                String frete = descontoAtual.freteGratisVip() ? "Frete grátis · Clube VIP"
                        : "FRETE".equals(descontoAtual.estrategia()) ? "Frete grátis · Roleta" : "Taxa de entrega: " + moeda(descontoAtual.taxaEntrega());
                exibir(mensagemFrete, frete + " · " + descontoAtual.tempoEstimadoMin() + " min"
                        + (configuracao != null && configuracao.modoChuvaAtivo() ? " · Chuva/Pico" : ""));
            }
            mensagemDesconto.setText(descontoAtual.brinde()!=null?"Brinde: "+descontoAtual.brinde():
                    "PONTOS_DOBRO".equals(descontoAtual.estrategia())?"Prêmio aplicado: pontos em dobro":
                    "Desconto: "+moeda(descontoAtual.desconto())+" ("+descontoAtual.estrategia()+")");
            atualizarCheckout();atualizarPix();});
        tarefa.setOnFailed(e->{if(geracao!=geracaoDesconto)return;Throwable erro=tarefa.getException();mensagemDesconto.setText(erro instanceof IllegalArgumentException?erro.getMessage():"Não foi possível calcular o desconto. Tente novamente.");});
        iniciar(tarefa);
    }
    private void atualizarPix(){
        boolean selecionado=formaPagamento.getValue()==FormaPagamento.PIX;
        BigDecimal valor=descontoAtual==null?null:descontoAtual.total();
        boolean real=selecionado&&pagamentosPix.usaMercadoPago()&&(valor==null||valor.signum()>0);
        dadosPagadorPix.setVisible(real);dadosPagadorPix.setManaged(real);
        if(!selecionado)pixCheckout.atualizar(false,null,null,"");
        else if(real)pixCheckout.exibirCodigo(null,"A cobrança Mercado Pago é gerada após finalizar o pedido.");
        else if(valor!=null&&valor.signum()>0)pixCheckout.exibirCodigo(
                new PixPayloadService(null,null,null).gerarPayload(valor,null),"Prévia PIX "+moeda(valor)+" · demonstração, não pague.");
        else pixCheckout.exibirCodigo(null,valor==null?"O código PIX aparece quando o total estiver disponível.":"Nenhum valor a pagar por PIX.");
    }
    @FXML private void meuPerfil(){
        UsuarioCliente atual=usuario;if(atual==null)return;
        ButtonType salvar=new ButtonType("Salvar",ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog=personalizar("Meu perfil",salvar);
        TextField apelido=new TextField(atual.getNome());apelido.setId("perfil-apelido");
        TextField whatsapp=new TextField(atual.getCliente().getWhatsapp());whatsapp.setId("perfil-whatsapp");
        TextField rua=new TextField(atual.getCliente().getEndereco());rua.setId("perfil-endereco");
        dialog.getDialogPane().setContent(new VBox(8,textoClube(atual.getEmail()==null?"Login: "+atual.getLogin():"E-mail: "+atual.getEmail()),
                new Label("Apelido (opcional)"),apelido,new Label("WhatsApp (opcional)"),whatsapp,new Label("Endereço para entrega"),rua));
        if(dialog.showAndWait().orElse(ButtonType.CANCEL)!=salvar)return;
        String nome=apelido.getText(), telefone=whatsapp.getText(), localEntrega=rua.getText();
        raiz.setDisable(true);
        Task<UsuarioCliente> tarefa=new Task<>(){
            @Override protected UsuarioCliente call(){return autenticacao.atualizarPerfil(atual,nome,telefone,localEntrega);}
        };
        tarefa.setOnSucceeded(e->{raiz.setDisable(false);usuario=tarefa.getValue();saudacao.setText("Olá, "+usuario.getNome()+"!");atualizarEndereco();simularDesconto();});
        tarefa.setOnFailed(e->{raiz.setDisable(false);Throwable erro=tarefa.getException();
            avisar(Alert.AlertType.WARNING,"Perfil não salvo",erro instanceof IllegalArgumentException?erro.getMessage():"Verifique a conexão e tente novamente.");});
        iniciar(tarefa);
    }
    private void atualizarEndereco(){
        if(usuario==null)return;
        String valor=usuario.getCliente().getEndereco();
        endereco.setText(entrega.isSelected()?(valor==null||valor.isBlank()?"Nenhum endereço cadastrado. Informe seu endereço em Meu perfil.":valor):local.isSelected()?"Consuma seu pedido no local.":"Retire seu pedido no balcão.");
    }
    private TipoEntrega modalidade(){return entrega.isSelected()?TipoEntrega.ENTREGA:local.isSelected()?TipoEntrega.LOCAL:TipoEntrega.RETIRADA;}
    @FXML private void finalizar(){
        if(carrinho.isEmpty()||usuario==null||descontoAtual==null||(descontoAtual.total().signum()>0 && formaPagamento.getValue()==null))return;
        TipoEntrega tipo=modalidade();
        if (entregaInvalida()) return;
        Pedido pedido = pedidoAtual();
        pedido.setFormaPagamento(formaPagamento.getValue());
        if(descontoAtual.total().signum()==0)pedido.setFormaPagamento(null);
        if(pedido.getFormaPagamento()==FormaPagamento.PIX&&pagamentosPix.usaMercadoPago())
            pedido.setPagadorPix(emailPix.getText(),documentoPix.getText());
        if(agendar.isSelected()){
            try {
                LocalDateTime data=LocalDateTime.of(Objects.requireNonNull(dataAgendamento.getValue()),LocalTime.parse(horaAgendamento.getText().trim()));
                pedido.setAgendadoPara(data);
            }catch(RuntimeException erro){avisar(Alert.AlertType.WARNING,"Agendamento inválido","Informe uma data e horário futuro no formato HH:mm.");return;}
        }
        try {
            if(configuracao==null)throw new IllegalArgumentException("Não foi possível verificar se a loja está aberta. Tente novamente.");
            operacao.validarPedido(configuracao,pedido.getAgendadoPara());
            if (OperacaoLojaService.faltaParaEntrega(configuracao, tipo, pedido.getItens()).signum() > 0) { atualizarCheckout(); return; }
        }catch(IllegalArgumentException erro){avisar(Alert.AlertType.WARNING,"Pedido não finalizado",erro.getMessage());return;}
        raiz.setDisable(true);
        ResultadoDesconto esperado=descontoAtual;String codigo=cupom.getText();
        Task<Long> tarefa=new Task<>() { @Override protected Long call(){return pedidosService.finalizar(pedido,codigo,esperado);} };
        tarefa.setOnSucceeded(e->{raiz.setDisable(false);if(pedido.getId()==null)pedido.setId(tarefa.getValue());
            pedido.setTaxaEntrega(esperado.taxaEntrega());pedido.setDesconto(esperado.desconto());pedido.setBrinde(esperado.brinde());
            carrinho.clear();cupom.clear();formaPagamento.setValue(null);agendar.setSelected(false);dataAgendamento.setValue(null);horaAgendamento.clear();
            emailPix.clear();documentoPix.clear();pedido.setPagadorPix(null,null);atualizarCarrinho();
            if(pedido.getStatusPagamento()==StatusPagamento.AGUARDANDO_PIX){
                abrirPix(pedido);carregarProdutos();mostrarPedidos();return;
            }
            Alert alerta=new Alert(Alert.AlertType.INFORMATION);alerta.initOwner(raiz.getScene().getWindow());alerta.setTitle("Pedido enviado");alerta.setHeaderText(null);alerta.setContentText("Pedido #"+tarefa.getValue()+" recebido com sucesso."
                    + (pedido.getStatusPagamento()==StatusPagamento.AGUARDANDO_CARTAO ? " Combine o pagamento por link com a loja pelo WhatsApp." : ""));
            ButtonType acompanhar=new ButtonType("Acompanhar no WhatsApp");alerta.getButtonTypes().add(acompanhar);
            ButtonType acao=alerta.showAndWait().orElse(ButtonType.OK);
            if(acao==acompanhar)abrirWhatsApp(pedido);
            carregarCupons();carregarProdutos();mostrarPedidos();});
        tarefa.setOnFailed(e->{raiz.setDisable(false);Throwable erro=tarefa.getException();
            if(erro instanceof IllegalArgumentException)avisar(Alert.AlertType.WARNING,"Pedido não finalizado",erro.getMessage());
            else avisar(Alert.AlertType.ERROR,"Não foi possível finalizar","Verifique sua conexão e tente novamente. Seu carrinho foi preservado.");carregarProdutos();atualizarLoja();simularDesconto();});iniciar(tarefa);
    }
    @FXML private void carregarPedidos(){atualizarPedidos(true);carregarResumoMensal();}
    private void carregarResumoMensal() {
        if (usuario == null) return;
        long geracao = ++geracaoResumo; UsuarioCliente cliente = usuario;
        Task<IndicadoresComerciais.ResumoCliente> tarefa = new Task<>() {
            @Override protected IndicadoresComerciais.ResumoCliente call() { return comercial.resumoDoMes(cliente); }
        };
        tarefa.setOnSucceeded(e -> {
            if (geracao!=geracaoResumo || usuario!=cliente) return;
            var resumo = tarefa.getValue();
            resumoMensal.setText("Este mês: você gastou " + moeda(resumo.gasto()) + " e economizou " + moeda(resumo.economia())
                    + (resumo.topComprador() ? " · 🏆 Você é Top Comprador do Mês!" : ""));
        });
        tarefa.setOnFailed(e -> { if (geracao==geracaoResumo) resumoMensal.setText("Resumo do mês indisponível no momento."); });
        iniciar(tarefa);
    }
    private void atualizarPedidos(boolean inicial){
        if(usuario==null||consultaPedidosEmAndamento)return;consultaPedidosEmAndamento=true;long geracao=++geracaoPedidos;
        if(inicial){listaPedidos.getChildren().clear();exibir(mensagemPedidos,"Carregando seus pedidos...");}
        Task<Historico> tarefa=new Task<>() { @Override protected Historico call(){return new Historico(pedidosService.listarDoCliente(usuario),pedidosService.contarFilaCozinha());} };
        tarefa.setOnSucceeded(e->{consultaPedidosEmAndamento=false;if(geracao!=geracaoPedidos)return;List<Pedido> pedidos=tarefa.getValue().pedidos();
            if(pedidos.isEmpty()){listaPedidos.getChildren().clear();exibir(mensagemPedidos,"Você ainda não fez nenhum pedido.");return;}
            ocultar(mensagemPedidos);listaPedidos.getChildren().setAll(pedidos.stream().map(p->cardPedido(p,tarefa.getValue().fila())).toList());
        });
        tarefa.setOnFailed(e->{consultaPedidosEmAndamento=false;if(geracao==geracaoPedidos)exibir(mensagemPedidos,"Não foi possível atualizar seus pedidos. Verifique a conexão e tente novamente.");});iniciar(tarefa);
    }
    private Node cardPedido(Pedido pedido,long fila){
        VBox card=new VBox(10);card.getStyleClass().add("pedido-card");
        Label numero=new Label("Pedido #"+pedido.getId()+"  ·  "+pedido.getCriadoEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));numero.getStyleClass().add("pedido-titulo");
        Label status=new Label(pedido.getStatus()==StatusPedido.PENDENTE?"Aguardando confirmação":pedido.getStatus().name());status.getStyleClass().addAll("status-pedido","status-"+pedido.getStatus().name().toLowerCase(Locale.ROOT));
        Region espaco=new Region();HBox.setHgrow(espaco,Priority.ALWAYS);HBox topo=new HBox(8,numero,espaco,status);topo.setAlignment(Pos.CENTER_LEFT);card.getChildren().add(topo);
        var etapa=EtapaCozinha.de(pedido.getStatus());
        ProgressBar termometro=new ProgressBar(etapa.progresso());termometro.setMaxWidth(Double.MAX_VALUE);
        boolean futuro=pedido.getAgendadoPara()!=null&&pedido.getAgendadoPara().isAfter(LocalDateTime.now());
        String estimativa=etapa.aguardaCozinha()?(futuro?" · aguardando horário agendado":" · estimativa: "+(Math.max(1,Math.min(fila,100000))*5)+" min (fila atual)"):"";
        if(pedido.getStatus()!=StatusPedido.CANCELADO&&pedido.getStatusPagamento()==StatusPagamento.AGUARDANDO_PIX)
            card.getChildren().add(textoClube("Aguardando PIX · a cozinha ainda não recebeu este pedido."));
        else if(pedido.getStatus()!=StatusPedido.CANCELADO&&pedido.getStatusPagamento()==StatusPagamento.AGUARDANDO_CARTAO)
            card.getChildren().add(textoClube("Cartão App aguardando confirmação da loja. Fale com a loja para combinar o pagamento."));
        else card.getChildren().addAll(new Label("🌡 Termômetro da Cozinha — "+etapa.mensagem(pedido.getTipoEntrega())+estimativa),termometro);
        if(pedido.getStatus()==StatusPedido.SAIU_ENTREGA && pedido.getTipoEntrega()==TipoEntrega.ENTREGA){
            Label banner=textoClube("🛵 Seu pedido saiu para entrega!"+(pedido.getNomeEntregador()==null?"":" Entregador: "+pedido.getNomeEntregador()));
            banner.setId("entrega-em-rota-"+pedido.getId());banner.getStyleClass().add("entrega-em-rota");
            banner.setMaxWidth(Double.MAX_VALUE);card.getChildren().add(banner);
        }
        if(pedido.getTempoEstimadoMin()!=null)card.getChildren().add(textoClube("Prazo estimado no pedido: " + pedido.getTempoEstimadoMin() + " min"));
        if(pedido.getAgendadoPara()!=null)card.getChildren().add(new Label("Agendado: "+pedido.getAgendadoPara().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        Set<UUID> gruposExibidos=new HashSet<>();
        for(PedidoItem item:pedido.getItens()){
            PersonalizacaoItem grupo=item.getPersonalizacao();
            if(grupo!=null&&gruposExibidos.add(grupo.grupo())){
                Label personalizado=new Label(item.getObservacaoPersonalizada());personalizado.setWrapText(true);card.getChildren().add(personalizado);
            }
            if(grupo!=null&&grupo.tipo().caixa()&&pedido.getStatus()!=StatusPedido.ENTREGUE)continue;
            Label descricao=new Label(item.getQuantidade().stripTrailingZeros().toPlainString()+" × "+item.getProduto().getNome()+"  ·  "+moeda(item.calcularSubtotal()));descricao.setWrapText(true);HBox.setHgrow(descricao,Priority.ALWAYS);
            Button avaliar=new Button("Avaliar produto");avaliar.setId("avaliar-"+pedido.getId()+"-"+item.getProduto().getId());avaliar.getStyleClass().add("button-secondary");avaliar.setDisable(pedido.getStatus()!=StatusPedido.ENTREGUE);avaliar.setOnAction(e->avaliar(item.getProduto()));
            VBox detalhes = new VBox(3, descricao); HBox.setHgrow(detalhes, Priority.ALWAYS);
            if (!item.detalhesPreparo().isEmpty()) detalhes.getChildren().add(textoClube(item.detalhesPreparo()));
            HBox linha=new HBox(8,detalhes,avaliar);linha.setAlignment(Pos.CENTER_LEFT);card.getChildren().add(linha);
        }
        Label totalPedido=new Label("Total: "+moeda(pedido.calcularTotal()));totalPedido.getStyleClass().add("produto-preco");
        if(pedido.getTaxaEntrega().signum()>0)card.getChildren().add(textoClube("Taxa de entrega: "+moeda(pedido.getTaxaEntrega())));
        if(pedido.getBrinde()!=null)card.getChildren().add(textoClube("Brinde: "+pedido.getBrinde()));
        if(pedido.getStatusPagamento()==StatusPagamento.PAGO&&pedido.getStatus()!=StatusPedido.CANCELADO)
            card.getChildren().add(textoClube(pedido.isPixDemonstracao()?"Pagamento simulado · nenhum PIX recebido.":"Pagamento confirmado."));
        Button whatsappPedido=new Button("Acompanhar no WhatsApp");whatsappPedido.setId("whatsapp-pedido-"+pedido.getId());whatsappPedido.getStyleClass().add("button-secondary");whatsappPedido.setOnAction(e->abrirWhatsApp(pedido));
        card.getChildren().addAll(totalPedido,whatsappPedido);
        if(pedido.isRoletaLiberada()&&pedido.getStatus()!=StatusPedido.CANCELADO){
            Button roleta=new Button(vip(pedido)?"Roleta VIP":"Roleta de Premiação");roleta.setId("roleta-"+pedido.getId());
            roleta.setOnAction(e->abrirRoleta(pedido));card.getChildren().add(roleta);
        }
        if(pedido.getStatusPagamento()==StatusPagamento.AGUARDANDO_PIX&&pedido.getStatus()==StatusPedido.PENDENTE){
            Button pix=new Button("Ver PIX do pedido");pix.setOnAction(e->{abrirPix(pedido);carregarPedidos();});card.getChildren().add(pix);
        }
        if (pedido.getStatus() == StatusPedido.PENDENTE && !(pedido.getFormaPagamento()==FormaPagamento.CARTAO_APP && pedido.getStatusPagamento()==StatusPagamento.PAGO)) {
            Button cancelar = new Button("Cancelar Pedido"); cancelar.setId("cancelar-pedido-" + pedido.getId());
            cancelar.getStyleClass().add("button-secondary"); cancelar.setOnAction(e -> cancelarPedido(pedido.getId()));
            card.getChildren().add(cancelar);
        }
        return card;
    }
    private void cancelarPedido(long id) {
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION, "Cancelar o pedido #" + id + "?", ButtonType.CANCEL, ButtonType.OK);
        confirmar.initOwner(raiz.getScene().getWindow()); confirmar.setHeaderText("Cancelar pedido");
        if (confirmar.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        raiz.setDisable(true); UsuarioCliente cliente = usuario;
        Task<Void> tarefa = new Task<>() { @Override protected Void call() { pedidosService.cancelarDoCliente(cliente,id); return null; } };
        tarefa.setOnSucceeded(e -> { raiz.setDisable(false); carregarPedidos(); carregarCupons(); carregarCarteira(); });
        tarefa.setOnFailed(e -> {
            raiz.setDisable(false); carregarPedidos();
            Throwable erro = tarefa.getException();
            avisar(Alert.AlertType.WARNING,"Pedido não cancelado",erro instanceof IllegalArgumentException ? erro.getMessage()
                    : "Não foi possível cancelar agora. Tente novamente ou entre em contato com a loja.");
        });
        iniciar(tarefa);
    }
    private void abrirPix(Pedido pedido){
        UsuarioCliente cliente=usuario;long id=pedido.getId();
        boolean roleta=PagamentoPixUi.abrir(raiz.getScene().getWindow(),id,()->pagamentosPix.consultar(cliente,id),
                ()->pagamentosPix.confirmarOffline(cliente,id));
        if(roleta)abrirRoleta(pedido);
        carregarCupons();carregarCarteira();carregarResumoMensal();
    }
    private static boolean vip(Pedido pedido){return pedido.calcularTotal().compareTo(PremioRoleta.LIMITE_VIP)>0;}
    private void abrirRoleta(Pedido pedido){
        UsuarioCliente cliente=usuario;
        long id=pedido.getId();
        RoletaUi.abrir(raiz.getScene().getWindow(),id,vip(pedido),()->gamificacao.girar(cliente,id),()->gamificacao.girarExtra(cliente,id));
        carregarCupons();
    }
    @FXML private void pedirDeSempre(){
        if(usuario==null)return;raiz.setDisable(true);
        Task<List<PedidoItem>> tarefa=new Task<>(){@Override protected List<PedidoItem> call(){return pedidosService.repetirUltimoPedido(usuario);}};
        tarefa.setOnSucceeded(e->{raiz.setDisable(false);carrinho.clear();for(PedidoItem i:tarefa.getValue())carrinho.computeIfAbsent(ChaveCarrinho.de(i),chave->new ArrayList<>()).add(i);atualizarCarrinho();mostrarVitrine();});
        tarefa.setOnFailed(e->{raiz.setDisable(false);Throwable erro=tarefa.getException();avisar(Alert.AlertType.WARNING,"Não foi possível repetir",erro instanceof IllegalArgumentException?erro.getMessage():"Verifique a disponibilidade e a conexão.");});
        iniciar(tarefa);
    }
    private void abrirWhatsApp(Pedido pedido){WhatsAppUi.abrir(raiz.getScene().getWindow(),whatsapp,whatsapp.acompanhar(pedido));}
    private void avaliar(Produto produto){
        Dialog<ButtonType> dialog=new Dialog<>();dialog.initOwner(raiz.getScene().getWindow());dialog.setTitle("Avaliar produto");dialog.setHeaderText(produto.getNome());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        ComboBox<Integer> nota=new ComboBox<>();nota.getItems().setAll(1,2,3,4,5);nota.setPromptText("Selecione de 1 a 5 estrelas");nota.setMaxWidth(Double.MAX_VALUE);
        nota.setConverter(new StringConverter<>() {public String toString(Integer n){return n==null?"":"★".repeat(n)+" ("+n+")";}public Integer fromString(String s){return null;}});
        TextArea comentario=new TextArea();comentario.setPromptText("Conte como foi sua experiência (opcional)");comentario.setWrapText(true);comentario.setPrefRowCount(3);
        javafx.beans.property.ObjectProperty<Path> foto=new javafx.beans.property.SimpleObjectProperty<>();
        Label arquivo=new Label("Nenhuma foto selecionada");arquivo.setWrapText(true);
        Button anexar=new Button("Selecionar foto…"), remover=new Button("Remover foto");
        remover.disableProperty().bind(foto.isNull());
        foto.addListener((o,a,n)->arquivo.setText(n==null?"Nenhuma foto selecionada":n.getFileName().toString()));
        remover.setOnAction(e->foto.set(null));
        anexar.setOnAction(e->{
            FileChooser seletor=new FileChooser();seletor.setTitle("Selecionar foto da avaliação");
            seletor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fotos JPEG ou PNG","*.jpg","*.jpeg","*.png","*.JPG","*.JPEG","*.PNG"));
            var selecionado=seletor.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if(selecionado!=null)foto.set(selecionado.toPath());
        });
        Label privacidade=new Label("Opcional: a foto aparece na vitrine sem seu nome. Evite incluir pessoas ou dados pessoais.");privacidade.setWrapText(true);
        VBox conteudo=new VBox(10,new Label("Sua nota"),nota,new Label("Comentário"),comentario,
                privacidade,new HBox(8,anexar,remover),arquivo);
        conteudo.setPadding(new Insets(10,0,0,0));conteudo.setPrefWidth(400);dialog.getDialogPane().setContent(conteudo);
        Node confirmar=dialog.getDialogPane().lookupButton(ButtonType.OK);confirmar.disableProperty().bind(nota.valueProperty().isNull());
        if(dialog.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK)return;
        Avaliacao avaliacao=new Avaliacao(null,usuario,produto,nota.getValue(),comentario.getText(),null,LocalDateTime.now());
        Path escolhida=foto.get();
        raiz.setDisable(true);Task<Long> tarefa=new Task<>() { @Override protected Long call(){return avaliacoesService.salvar(avaliacao,escolhida);} };
        tarefa.setOnSucceeded(e->{raiz.setDisable(false);avisar(Alert.AlertType.INFORMATION,"Avaliação enviada","Obrigado por compartilhar sua experiência!");carregarProdutos();});
        tarefa.setOnFailed(e->{raiz.setDisable(false);Throwable erro=tarefa.getException();
            if(erro instanceof AvaliacaoNaoPermitidaException)avisar(Alert.AlertType.WARNING,"Avaliação não permitida","Você precisa ter recebido um pedido com este produto para avaliá-lo.");
            else if(erro instanceof IllegalArgumentException||erro instanceof java.io.UncheckedIOException)avisar(Alert.AlertType.WARNING,"Não foi possível salvar a foto",erro.getMessage());
            else avisar(Alert.AlertType.ERROR,"Não foi possível avaliar","Verifique sua conexão e tente novamente.");});iniciar(tarefa);
    }
    @FXML private void sair(){
        atualizacaoPedidos.stop();contagemCupons.stop();atualizacaoLoja.stop();geracaoLoja++;geracaoProdutos++;geracaoPedidos++;
        try {Navegacao.abrirLogin((Stage)raiz.getScene().getWindow());}
        catch(IOException e){avisar(Alert.AlertType.ERROR,"Não foi possível sair","Tente novamente.");}
    }
    private static String moeda(BigDecimal valor){return NumberFormat.getCurrencyInstance(BR).format(valor);}
    private static String normalizar(String texto){return texto==null?"":Normalizer.normalize(texto,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(BR).trim();}
    private static void exibir(Label label,String texto){label.setText(texto);label.setVisible(true);label.setManaged(true);}
    private static void ocultar(Label label){label.setVisible(false);label.setManaged(false);}
    private static void iniciar(Task<?> tarefa){Thread thread=new Thread(tarefa,"vitrine-banco");thread.setDaemon(true);thread.start();}
    private void avisar(Alert.AlertType tipo,String titulo,String mensagem){Alert alerta=new Alert(tipo);alerta.initOwner(raiz.getScene().getWindow());alerta.setTitle(titulo);alerta.setHeaderText(null);alerta.setContentText(mensagem);alerta.showAndWait();}
}
