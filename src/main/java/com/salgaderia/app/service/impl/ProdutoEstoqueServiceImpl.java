package com.salgaderia.app.service.impl;
import com.salgaderia.app.dao.*;
import com.salgaderia.app.dao.impl.*;
import com.salgaderia.app.model.*;
import com.salgaderia.app.model.enums.TipoMovimentacaoEstoque;
import com.salgaderia.app.service.ProdutoEstoqueService;
import com.salgaderia.app.service.ProdutoPersonalizacaoService;
import com.salgaderia.app.util.ConnectionFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
public class ProdutoEstoqueServiceImpl implements ProdutoEstoqueService {
    private final ProdutoDAO produtos;private final ContatoDAO contatos;private final MovimentacaoEstoqueDAO movimentos;
    private final CategoriaDAO categorias = new CategoriaDAOImpl();
    private final ProdutoOpcionalDAO opcionais = new ProdutoOpcionalDAOImpl();
    public ProdutoEstoqueServiceImpl(){this(new ProdutoDAOImpl(),new ContatoDAOImpl(),new MovimentacaoEstoqueDAOImpl());}
    public ProdutoEstoqueServiceImpl(ProdutoDAO produtos,ContatoDAO contatos,MovimentacaoEstoqueDAO movimentos){this.produtos=Objects.requireNonNull(produtos);this.contatos=Objects.requireNonNull(contatos);this.movimentos=Objects.requireNonNull(movimentos);}
    @Override public List<Categoria> listarCategorias(){return ConnectionFactory.executarEmTransacao(categorias::listar);}
    @Override public List<Categoria> listarCategoriasAtivas(){return ConnectionFactory.executarEmTransacao(categorias::listarAtivas);}
    @Override public void salvarCatalogo(Produto produto, String categoria, BigDecimal ajuste) {
        String nome = categoria == null ? "" : categoria.strip().replaceAll("\\s+", " ");
        if (nome.codePointCount(0, nome.length()) > 100 || nome.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Informe uma categoria com até 100 caracteres.");
        ProdutoPersonalizacaoService.validarCatalogo(produto.getOpcionais());
        if (ajuste != null && ajuste.signum() != 0) validarQuantidade(ajuste);
        if (produto.getId() == null && (produto.getEstoqueAtual().signum() != 0 || ajuste != null))
            throw new IllegalArgumentException("Novo produto deve iniciar com estoque zero.");
        long id = ConnectionFactory.executarEmTransacao(c -> {
            if (produto.getId() != null && produtos.buscarPorId(c, produto.getId(), true).isEmpty())
                throw new IllegalArgumentException("Produto não encontrado.");
            produto.setCategoria(nome.isEmpty() ? null : new Categoria(categorias.obterOuCriarAtiva(c, nome), nome, null));
            long produtoId;
            if (produto.getId() == null) produtoId = produtos.inserir(c, produto);
            else {
                produtoId = produto.getId();
                if (!atualizar(c, produto, ajuste)) throw new IllegalArgumentException("Produto não encontrado.");
            }
            opcionais.salvar(c, produtoId, produto.getOpcionais());
            return produtoId;
        });
        produto.setId(id);
    }
    @Override public long cadastrar(Produto produto){
        if(produto.getId()!=null||produto.getEstoqueAtual().signum()!=0)throw new IllegalArgumentException("Novo produto deve iniciar sem ID e com estoque zero");
        long id=ConnectionFactory.executarEmTransacao(c->produtos.inserir(c,produto));produto.setId(id);return id;
    }
    @Override public boolean atualizar(Produto produto){return atualizar(produto,null);}
    @Override public boolean atualizar(Produto produto, BigDecimal ajustePara){
        if(ajustePara!=null && ajustePara.signum()!=0)validarQuantidade(ajustePara);
        return ConnectionFactory.executarEmTransacao(c->atualizar(c,produto,ajustePara));
    }
    private boolean atualizar(Connection c, Produto produto, BigDecimal ajustePara) throws SQLException {
        if(ajustePara==null)return produtos.atualizar(c,produto);
        var atual=produtos.buscarPorId(c,Objects.requireNonNull(produto.getId()),true);
        if(atual.isEmpty() || !produtos.atualizar(c,produto))return false;
        BigDecimal diferenca=ajustePara.subtract(atual.get().getEstoqueAtual());
        if(diferenca.signum()!=0){
            if(!produtos.atualizarEstoque(c,produto.getId(),ajustePara))throw new IllegalStateException("Produto não atualizado");
            var tipo=diferenca.signum()>0?TipoMovimentacaoEstoque.ENTRADA:TipoMovimentacaoEstoque.SAIDA;
            movimentos.inserir(c,new MovimentacaoEstoque(null,produto,tipo,diferenca.abs(),"Ajuste manual do estoque",LocalDateTime.now()),null,"AJUSTE",produto.getCustoProducao());
        }
        return true;
    }
    @Override public boolean alternarDisponibilidade(long id){return ConnectionFactory.executarEmTransacao(c->produtos.alternarDisponibilidade(c,id));}
    @Override public boolean excluir(long id){return ConnectionFactory.executarEmTransacao(c->produtos.excluir(c,id));}
    @Override public Optional<Produto> buscarPorId(long id){return ConnectionFactory.executarEmTransacao(c->produtos.buscarPorId(c,id,false));}
    @Override public List<Produto> listar(){return ConnectionFactory.executarEmTransacao(c -> {
        var lista = produtos.listar(c);
        var opcoes = opcionais.listarPorProduto(c);
        lista.forEach(p -> p.setOpcionais(opcoes.getOrDefault(p.getId(), List.of())));
        return lista;
    });}
    @Override public List<Produto> listarEstoqueBaixo(){return listar().stream().filter(Produto::isEstoqueBaixo).toList();}
    @Override public void entradaLote(long produtoId,long fornecedorId,BigDecimal quantidade,String observacao){movimentar(produtoId,fornecedorId,quantidade,observacao,true);}
    @Override public void registrarPerda(long produtoId,BigDecimal quantidade,String observacao){movimentar(produtoId,null,quantidade,observacao,false);}
    private void movimentar(long id,Long fornecedor,BigDecimal quantidade,String observacao,boolean entrada){
        validarQuantidade(quantidade);
        ConnectionFactory.executarEmTransacao(c->{
            if(entrada && !(contatos.buscarPorId(c,fornecedor).orElseThrow(()->new IllegalArgumentException("Fornecedor não encontrado")) instanceof Fornecedor))throw new IllegalArgumentException("Contato não é fornecedor");
            Produto p=produtos.buscarPorId(c,id,true).orElseThrow(()->new IllegalArgumentException("Produto não encontrado"));
            if(entrada)p.adicionarEstoque(quantidade);else p.removerEstoque(quantidade);
            if(!produtos.atualizarEstoque(c,id,p.getEstoqueAtual()))throw new IllegalStateException("Produto não atualizado");
            movimentos.inserir(c,new MovimentacaoEstoque(null,p,entrada?TipoMovimentacaoEstoque.ENTRADA:TipoMovimentacaoEstoque.SAIDA,quantidade,observacao,LocalDateTime.now()),fornecedor,entrada?"LOTE":"PERDA",p.getCustoProducao());return null;
        });
    }
    static void validarQuantidade(BigDecimal q){if(q==null||q.signum()<=0||q.stripTrailingZeros().scale()>3||q.compareTo(new BigDecimal("999999999.999"))>0)throw new IllegalArgumentException("Quantidade inválida para DECIMAL(12,3)");}
}
