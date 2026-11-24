package com.salgaderia.app.service.impl;

import com.salgaderia.app.dao.*;
import com.salgaderia.app.dao.impl.*;
import com.salgaderia.app.model.*;
import com.salgaderia.app.model.comanda.*;
import com.salgaderia.app.model.enums.*;
import com.salgaderia.app.service.ComandaService;
import com.salgaderia.app.util.ConnectionFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public final class ComandaServiceImpl implements ComandaService {
    private final ComandaDAO comandas;private final ProdutoDAO produtos;private final MovimentacaoEstoqueDAO movimentos;private final UsuarioDAO usuarios;
    public ComandaServiceImpl(){this(new ComandaDAOImpl(),new ProdutoDAOImpl(),new MovimentacaoEstoqueDAOImpl(),new UsuarioDAOImpl(new ContatoDAOImpl()));}
    public ComandaServiceImpl(ComandaDAO c,ProdutoDAO p,MovimentacaoEstoqueDAO m,UsuarioDAO u){comandas=Objects.requireNonNull(c);produtos=Objects.requireNonNull(p);movimentos=Objects.requireNonNull(m);usuarios=Objects.requireNonNull(u);}
    @Override public List<Mesa> listarMesas(){return ConnectionFactory.executarEmTransacao(comandas::listarMesas);}
    private void validarAdmin(Connection c,Usuario usuario)throws SQLException{
        if(!(usuario instanceof UsuarioAdmin)||usuario.getId()==null||!usuario.isAtivo())throw new IllegalArgumentException("Administrador ativo obrigatório");
        Usuario atual=usuarios.buscarPorId(c,usuario.getId()).orElseThrow(()->new IllegalArgumentException("Administrador não encontrado"));
        if(!(atual instanceof UsuarioAdmin)||!atual.isAtivo())throw new IllegalArgumentException("Administrador ativo obrigatório");
    }
    private Comanda aberta(Connection c,Mesa mesa)throws SQLException{
        if(!mesa.status().permiteLancamento())throw new IllegalStateException("Mesa não possui comanda aberta");
        return comandas.buscarAberta(c,mesa).orElseThrow(()->new IllegalStateException("Comanda aberta não encontrada"));
    }
    @Override public Comanda abrirOuConsultar(long id,Usuario usuario){
        return ConnectionFactory.executarEmTransacao(c->{Mesa mesa=comandas.bloquearMesa(c,id);validarAdmin(c,usuario);
            if(mesa.status().podeAbrir()){comandas.abrir(c,id);mesa=new Mesa(mesa.id(),mesa.numero(),mesa.nome(),StatusMesa.OCUPADA);}
            return aberta(c,mesa);});
    }
    private Comanda atendimentoEsperado(Connection c,Mesa mesa,long id)throws SQLException{
        Comanda atual=aberta(c,mesa);
        if(atual.atendimentoId()!=id)throw new IllegalStateException("A mesa foi reaberta; atualize a tela antes de operar");
        return atual;
    }
    @Override public Comanda consultar(long id,long atendimentoId,Usuario usuario){
        return ConnectionFactory.executarEmTransacao(c->{Mesa mesa=comandas.bloquearMesa(c,id);validarAdmin(c,usuario);return atendimentoEsperado(c,mesa,atendimentoId);});
    }
    @Override public Comanda adicionarItem(long mesaId,long atendimentoId,long produtoId,BigDecimal quantidade,Usuario usuario){
        validarUnidades(quantidade);
        return ConnectionFactory.executarEmTransacao(c->{Mesa mesa=comandas.bloquearMesa(c,mesaId);validarAdmin(c,usuario);Comanda comanda=atendimentoEsperado(c,mesa,atendimentoId);
            Produto produto=produtos.buscarPorId(c,produtoId,true).orElseThrow(()->new IllegalArgumentException("Produto não encontrado"));
            produto.exigirDisponivelParaVenda();
            produto.removerEstoque(quantidade);
            if(!produtos.atualizarEstoque(c,produtoId,produto.getEstoqueAtual()))throw new IllegalStateException("Estoque não atualizado");
            comandas.inserirItem(c,comanda.atendimentoId(),produtoId,quantidade,produto.getPrecoVenda(),produto.getCustoProducao());
            movimentos.inserir(c,new MovimentacaoEstoque(null,produto,TipoMovimentacaoEstoque.SAIDA,quantidade,"Comanda "+comanda.atendimentoId(),LocalDateTime.now()),null,"VENDA",produto.getCustoProducao());
            return aberta(c,mesa);});
    }
    @Override public boolean pagarValor(long mesa,long atendimentoId,String pessoa,FormaPagamento forma,BigDecimal valor,Usuario usuario){return pagar(mesa,atendimentoId,pessoa,forma,valor,null,null,usuario);}
    @Override public boolean pagarItem(long mesa,long atendimentoId,String pessoa,FormaPagamento forma,long item,BigDecimal quantidade,Usuario usuario){return pagar(mesa,atendimentoId,pessoa,forma,null,item,quantidade,usuario);}
    private boolean pagar(long mesaId,long atendimentoId,String pessoa,FormaPagamento forma,BigDecimal valor,Long itemId,BigDecimal quantidade,Usuario usuario){
        if(pessoa==null||pessoa.isBlank()||pessoa.trim().length()>100)throw new IllegalArgumentException("Informe o nome da pessoa");
        Objects.requireNonNull(forma,"forma de pagamento");
        if (forma==FormaPagamento.CARTAO_APP) throw new IllegalArgumentException("Cartão App é exclusivo da vitrine.");
        return ConnectionFactory.executarEmTransacao(c->{Mesa mesa=comandas.bloquearMesa(c,mesaId);validarAdmin(c,usuario);Comanda comanda=atendimentoEsperado(c,mesa,atendimentoId);
            BigDecimal valorPagamento=valor;
            if(itemId!=null){
                validarUnidades(quantidade);
                ItemComanda item=comanda.itens().stream().filter(i->i.id()==itemId).findFirst().orElseThrow(()->new IllegalArgumentException("Item não pertence à comanda"));
                valorPagamento=item.cotarPagamento(quantidade);
            }
            if(!comanda.aceitaPagamento(valorPagamento))throw new IllegalArgumentException("Valor deve ser positivo e não ultrapassar o saldo");
            comandas.inserirPagamento(c,comanda.atendimentoId(),pessoa.trim(),forma,valorPagamento,itemId,quantidade);
            if(comanda.saldo().compareTo(valorPagamento)==0){concluir(c,mesa,comanda,usuario);return true;}return false;
        });
    }
    @Override public List<BigDecimal> dividirIgualmente(BigDecimal saldo,int pessoas){
        if(saldo==null||saldo.signum()<0||saldo.scale()>2||pessoas<1||pessoas>100)throw new IllegalArgumentException("Saldo ou número de pessoas inválido");
        long centavos=saldo.movePointRight(2).longValueExact();long base=centavos/pessoas,resto=centavos%pessoas;
        List<BigDecimal> partes=new ArrayList<>();for(int i=0;i<pessoas;i++)partes.add(BigDecimal.valueOf(base+(i<resto?1:0),2));return List.copyOf(partes);
    }
    @Override public void fecharSemSaldo(long mesaId,long atendimentoId,Usuario usuario){
        ConnectionFactory.executarEmTransacao(c->{Mesa mesa=comandas.bloquearMesa(c,mesaId);validarAdmin(c,usuario);Comanda comanda=atendimentoEsperado(c,mesa,atendimentoId);if(comanda.saldo().signum()!=0)throw new IllegalStateException("A comanda ainda possui saldo");if(comanda.itens().isEmpty())comandas.fechar(c,mesa.id(),comanda.atendimentoId(),null);else concluir(c,mesa,comanda,usuario);return null;});
    }
    private void validarUnidades(BigDecimal quantidade){
        ProdutoEstoqueServiceImpl.validarQuantidade(quantidade);
        if(quantidade.stripTrailingZeros().scale()>0)throw new IllegalArgumentException("Informe uma quantidade inteira de unidades");
    }
    private void concluir(Connection c,Mesa mesa,Comanda comanda,Usuario usuario)throws SQLException{
        if(comanda.itens().isEmpty())throw new IllegalStateException("Não é possível fechar uma comanda sem itens");
        long pedido=comandas.concluirVenda(c,comanda.atendimentoId(),usuario.getId());comandas.fechar(c,mesa.id(),comanda.atendimentoId(),pedido);
    }
}
