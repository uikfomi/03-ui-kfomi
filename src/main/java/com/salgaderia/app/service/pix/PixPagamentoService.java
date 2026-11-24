package com.salgaderia.app.service.pix;

import com.salgaderia.app.dao.PixPagamentoDAO;
import com.salgaderia.app.dao.PixPagamentoDAO.Dados;
import com.salgaderia.app.dao.PixPagamentoDAO.Provedor;
import com.salgaderia.app.dao.UsuarioDAO;
import com.salgaderia.app.dao.impl.*;
import com.salgaderia.app.model.Pedido;
import com.salgaderia.app.model.UsuarioCliente;
import com.salgaderia.app.model.enums.StatusPagamento;
import com.salgaderia.app.model.enums.StatusPedido;
import com.salgaderia.app.service.*;
import com.salgaderia.app.service.PedidoConcluidoObserver.*;
import com.salgaderia.app.util.ConnectionFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Vincula uma cobrança à venda, concilia o retorno e mantém a simulação separada da API real. */
public final class PixPagamentoService {
    public enum Estado { AGUARDANDO, INDISPONIVEL, PAGO, CANCELADO }
    public record Consulta(Estado estado,BigDecimal valor,String payload,boolean confirmarDemo,String mensagem) { }

    private final PixPagamentoDAO dados=new PixPagamentoDAOImpl();
    private final UsuarioDAO usuarios=new UsuarioDAOImpl(new ContatoDAOImpl());
    private final BarramentoPedido eventos=new BarramentoPedido(new ConquistasObserver(new GamificacaoDAOImpl()), new com.salgaderia.app.service.FidelidadeService());
    private final PedidoCancelamentoService cancelamento=new PedidoCancelamentoService(
            new PedidoDAOImpl(),new ProdutoDAOImpl(),new MovimentacaoEstoqueDAOImpl(),eventos);
    private final PixGatewayService demo=new LocalDemoPixGateway();
    private final Supplier<PixGatewayService> fabricaReal;
    private volatile PixGatewayService real;
    private long cursorAdmin;

    public PixPagamentoService(){
        this(PixConfiguracao.temMercadoPago()?PixConfiguracao::gatewayReal:null);
    }
    private PixPagamentoService(Supplier<PixGatewayService> fabrica){fabricaReal=fabrica;}
    public static PixPagamentoService demonstracao(){return new PixPagamentoService((Supplier<PixGatewayService>)null);}
    public PixPagamentoService(PixGatewayService gateway){
        real=Objects.requireNonNull(gateway);fabricaReal=()->gateway;
    }
    public boolean usaMercadoPago(){return fabricaReal!=null;}
    private synchronized PixGatewayService real(){
        if(real==null)real=fabricaReal.get();return real;
    }

    /** Executado na transação que reserva estoque. Não realiza HTTP. */
    public void registrar(Connection c,long pedidoId,Pedido pedido,BigDecimal valor)throws SQLException{
        if(valor.signum()==0){
            dados.preparar(c,pedidoId,null,null,null,null,StatusPagamento.PAGO);
            return;
        }
        String email=null,documento=null;
        if(usaMercadoPago()){
            email=pedido.getEmailPix()==null?"":pedido.getEmailPix().strip();
            if(email.length()>254||!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
                throw new IllegalArgumentException("Informe um e-mail válido para gerar a cobrança PIX.");
            documento=pedido.getDocumentoPix()==null?"":pedido.getDocumentoPix().replaceAll("[. /-]","");
            if(!documento.matches("[0-9]{11}|[0-9]{14}"))throw new IllegalArgumentException("Informe o CPF ou CNPJ do pagador para gerar a cobrança PIX.");
        }
        dados.preparar(c,pedidoId,usaMercadoPago()?Provedor.MERCADO_PAGO:Provedor.DEMO,
                UUID.randomUUID().toString(),email,documento,StatusPagamento.AGUARDANDO_PIX);
    }

    public Consulta consultar(UsuarioCliente cliente,long pedidoId){
        Dados local=ConnectionFactory.executarEmTransacao(c->{
            validarCliente(c,cliente);
            Dados atual=exigir(c,pedidoId,true);
            if(atual.usuarioId()!=cliente.getId())throw new IllegalArgumentException("Cobrança não encontrada.");
            if(atual.pagamento()!=StatusPagamento.PAGO&&atual.pedido()==StatusPedido.PENDENTE&&
                    atual.provedor()==Provedor.MERCADO_PAGO&&usaMercadoPago()&&atual.tentativa()==null){
                dados.marcarTentativa(c,pedidoId);return exigir(c,pedidoId,true);
            }
            return atual;
        });
        if(local.pagamento()==StatusPagamento.PAGO||local.pedido()==StatusPedido.CANCELADO)return resultadoLocal(local);
        if(local.provedor()==Provedor.DEMO){
            if(usaMercadoPago())return new Consulta(Estado.INDISPONIVEL,local.valor(),null,false,
                    "Pedido de demonstração. Com o gateway real ativo, não é permitido confirmá-lo offline.");
            return new Consulta(Estado.AGUARDANDO,local.valor(),gerarDemo(local),true,"Modo demonstração — não pague.");
        }
        if(!usaMercadoPago())return new Consulta(Estado.INDISPONIVEL,local.valor(),null,false,
                "Este pedido pertence ao Mercado Pago. Configure o token original para consultá-lo.");
        try{
            if(local.idRemoto()==null&&!local.podeRepetirCriacao())
                return new Consulta(Estado.INDISPONIVEL,local.valor(),null,false,
                        "A criação ficou sem resposta. Confira a referência "+local.referencia()+" no Mercado Pago antes de qualquer nova cobrança.");
            var resposta=local.idRemoto()==null?real().criar(solicitacao(local)):real().consultar(local.idRemoto());
            return aplicar(local,resposta);
        }catch(PixGatewayService.Indisponivel e){
            // Se não houver QR real já validado, a prévia local é apenas visual: não confirma nem substitui a cobrança.
            return new Consulta(Estado.INDISPONIVEL,local.valor(),local.payload()!=null?local.payload():gerarDemo(local),false,
                    local.payload()!=null?e.getMessage()+" Consulte novamente.":"Sem conexão: QR de demonstração. Não pague nem confirme offline. O pedido real continua pendente.");
        }
    }

    /** Só movimenta pedidos explicitamente criados em demonstração, sem credencial real ativa. */
    public Consulta confirmarOffline(UsuarioCliente cliente,long pedidoId){
        if(usaMercadoPago())throw new IllegalArgumentException("Confirmação offline bloqueada com o gateway real ativo.");
        return ConnectionFactory.executarEmTransacao(c->{
            validarCliente(c,cliente);Dados local=exigir(c,pedidoId,true);
            if(local.usuarioId()!=cliente.getId()||local.provedor()!=Provedor.DEMO||local.pedido()==StatusPedido.CANCELADO||local.idRemoto()!=null)
                throw new IllegalArgumentException("Esta cobrança não admite confirmação offline.");
            if(local.pagamento()==StatusPagamento.AGUARDANDO_PIX)confirmar(c,pedidoId);
            return resultadoLocal(exigir(c,pedidoId,true));
        });
    }

    /** Reconcilia apenas transações já conhecidas pelo painel aberto, sem criar novas cobranças. */
    public synchronized void reconciliarPendentes(){
        if(!usaMercadoPago())return;
        long cursor=cursorAdmin;
        var pendentes=ConnectionFactory.executarEmTransacao(c->dados.pendentesConhecidos(c,cursor));
        if(pendentes.isEmpty()){cursorAdmin=0;return;}
        for(Dados pedido:pendentes){
            cursorAdmin=pedido.id();
            try{aplicar(pedido,real().consultar(pedido.idRemoto()));}
            catch(PixGatewayService.Indisponivel e){break;}
            catch(PixGatewayService.RespostaInvalida|IllegalArgumentException e){
                System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,"PIX não conciliado para pedido #"+pedido.id());
            }
        }
    }

    private Consulta aplicar(Dados original,PixGatewayService.Cobranca remoto){
        conferirResposta(original,remoto);
        return ConnectionFactory.executarEmTransacao(c->{
            usuarios.bloquearClienteAtivo(c,original.usuarioId(),original.contatoId());
            Dados atual=exigir(c,original.id(),true);
            conferirResposta(atual,remoto);
            if(atual.pagamento()==StatusPagamento.PAGO||atual.pedido()==StatusPedido.CANCELADO)return resultadoLocal(atual);
            dados.vincular(c,atual.id(),remoto.id(),remoto.payload());
            if("approved".equals(remoto.status()))confirmar(c,atual.id());
            else if("rejected".equals(remoto.status())||"cancelled".equals(remoto.status()))cancelamento.cancelar(c,atual.id());
            return resultadoLocal(exigir(c,atual.id(),true));
        });
    }
    private static void conferirResposta(Dados esperado,PixGatewayService.Cobranca recebido){
        if(esperado.provedor()!=Provedor.MERCADO_PAGO||recebido==null||recebido.id()==null||!recebido.id().matches("[0-9]{1,100}")||
                (esperado.idRemoto()!=null&&!esperado.idRemoto().equals(recebido.id()))||
                !Objects.equals(esperado.referencia(),recebido.referencia())||recebido.valor()==null||esperado.valor().compareTo(recebido.valor())!=0||
                !"BRL".equals(recebido.moeda())||!"pix".equals(recebido.metodo())||
                (recebido.payload()!=null&&(recebido.payload().isBlank()||recebido.payload().length()>1024||!recebido.payload().startsWith("000201"))))
            throw new PixGatewayService.RespostaInvalida("O pagamento retornado não corresponde à cobrança deste pedido. Ele não foi confirmado.");
    }
    private void confirmar(Connection c,long pedidoId)throws SQLException{
        dados.pagar(c,pedidoId);eventos.publicar(c,new Evento(pedidoId,null,Etapa.PAGAMENTO_CONFIRMADO,true));
    }
    private static Consulta resultadoLocal(Dados d){
        if(d.pedido()==StatusPedido.CANCELADO)return new Consulta(Estado.CANCELADO,d.valor(),null,false,"Cobrança cancelada ou rejeitada. A reserva foi devolvida.");
        if(d.pagamento()==StatusPagamento.PAGO)return new Consulta(Estado.PAGO,d.valor(),null,false,
                d.provedor()==Provedor.DEMO?"Pagamento simulado confirmado. Nenhum PIX foi recebido.":"Pagamento confirmado pelo Mercado Pago.");
        return new Consulta(Estado.AGUARDANDO,d.valor(),d.payload(),false,"Aguardando confirmação automática do pagamento…");
    }
    private String gerarDemo(Dados d){return demo.criar(solicitacao(d)).payload();}
    private static PixGatewayService.Solicitacao solicitacao(Dados d){
        LocalDateTime inicio=d.tentativa()==null?agora():d.tentativa();
        return new PixGatewayService.Solicitacao(d.referencia(),d.valor(),d.email(),d.documento(),
                inicio.plusHours(1).atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
    private Dados exigir(Connection c,long id,boolean bloquear)throws SQLException{
        return dados.buscar(c,id,bloquear).orElseThrow(()->new IllegalArgumentException("Cobrança não encontrada."));
    }
    private void validarCliente(Connection c,UsuarioCliente cliente)throws SQLException{
        if(cliente==null||cliente.getId()==null||cliente.getCliente().getId()==null||!cliente.isAtivo()||
                !usuarios.bloquearClienteAtivo(c,cliente.getId(),cliente.getCliente().getId()))
            throw new IllegalArgumentException("Cliente ativo obrigatório.");
    }
    private static LocalDateTime agora(){return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);}
}
