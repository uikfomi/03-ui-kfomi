package com.salgaderia.app.dao.impl;
import com.salgaderia.app.dao.PedidoDAO;
import com.salgaderia.app.model.*;
import com.salgaderia.app.model.enums.StatusPagamento;
import com.salgaderia.app.model.enums.StatusPedido;
import com.salgaderia.app.model.enums.TipoEntrega;
import java.util.HashMap;
import com.google.gson.Gson;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;
public class PedidoDAOImpl implements PedidoDAO {
    private static final Gson JSON=new Gson();
    private record FiltrosCaixa(java.util.Set<Long> categorias,java.util.Set<Long> produtos) { }

    @Override public java.util.List<Pedido> listarDoCliente(Connection c,UsuarioCliente usuario) throws SQLException {
        String sql="SELECT p.id pedido_id,p.status,p.tipo_entrega,p.endereco_entrega,p.criado_em,p.agendado_para,p.desconto,p.forma_pagamento,p.roleta_liberada,"
                + "p.status_pagamento,p.pix_provedor,p.taxa_entrega,p.brinde_descricao,p.nome_entregador,"
                + "i.id item_id,i.quantidade,i.preco_unitario,i.observacao_personalizada,i.grupo_personalizado,i.tipo_personalizacao,i.restricoes_personalizadas,i.filtros_caixa,i.adicionais_resumo,i.opcionais_ids,pr.id produto_id,pr.nome produto_nome,pr.descricao produto_descricao,"
                + "pr.preco_venda,pr.custo_producao,pr.estoque_atual,pr.estoque_minimo,pr.url_imagem,pr.disponivel_venda,"
                + "cat.id categoria_id,cat.nome categoria_nome,cat.descricao categoria_descricao "
                + "FROM pedidos p JOIN pedido_itens i ON i.pedido_id=p.id JOIN produtos pr ON pr.id=i.produto_id "
                + "LEFT JOIN categorias cat ON cat.id=pr.categoria_id WHERE p.usuario_id=? ORDER BY p.criado_em DESC,p.id DESC,i.id";
        record Dados(com.salgaderia.app.model.enums.StatusPedido status,com.salgaderia.app.model.enums.TipoEntrega entrega,String endereco,LocalDateTime data,LocalDateTime agendado,BigDecimal desconto,String pagamento,boolean roleta,
                     StatusPagamento situacao,boolean demo,BigDecimal taxa,String brinde,String entregador,java.util.List<PedidoItem> itens) { }
        java.util.Map<Long,Dados> grupos=new java.util.LinkedHashMap<>();
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setLong(1,usuario.getId());
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    long id=rs.getLong("pedido_id");
                    Dados dados=grupos.get(id);
                    if(dados==null){Timestamp agendado=rs.getTimestamp("agendado_para");dados=new Dados(com.salgaderia.app.model.enums.StatusPedido.valueOf(rs.getString("status")),com.salgaderia.app.model.enums.TipoEntrega.valueOf(rs.getString("tipo_entrega")),rs.getString("endereco_entrega"),rs.getTimestamp("criado_em").toLocalDateTime(),agendado==null?null:agendado.toLocalDateTime(),rs.getBigDecimal("desconto"),rs.getString("forma_pagamento"),rs.getBoolean("roleta_liberada")&&("ENTREGUE".equals(rs.getString("status"))||"PAGO".equals(rs.getString("status_pagamento"))),
                            StatusPagamento.valueOf(rs.getString("status_pagamento")),"DEMO".equals(rs.getString("pix_provedor")),
                            rs.getBigDecimal("taxa_entrega"),rs.getString("brinde_descricao"),rs.getString("nome_entregador"),new java.util.ArrayList<>());grupos.put(id,dados);}
                    Long categoriaId=rs.getObject("categoria_id",Long.class);
                    Categoria categoria=categoriaId==null?null:new Categoria(categoriaId,rs.getString("categoria_nome"),rs.getString("categoria_descricao"));
                    Produto produto=new Produto(rs.getLong("produto_id"),categoria,rs.getString("produto_nome"),rs.getString("produto_descricao"),rs.getBigDecimal("preco_venda"),rs.getBigDecimal("custo_producao"),rs.getBigDecimal("estoque_atual"),rs.getBigDecimal("estoque_minimo"),rs.getString("url_imagem"));
                    produto.setDisponivelVenda(rs.getBoolean("disponivel_venda"));
                    dados.itens().add(new PedidoItem(rs.getLong("item_id"),produto,rs.getBigDecimal("quantidade"),rs.getBigDecimal("preco_unitario"),
                            rs.getString("observacao_personalizada"),personalizacao(rs),opcionaisIds(rs),rs.getString("adicionais_resumo")));
                }
            }
        }
        java.util.List<Pedido> resultado=new java.util.ArrayList<>();
        grupos.forEach((id,d)->{Pedido pedido=new Pedido(id,usuario,usuario.getCliente(),d.status(),d.entrega(),d.endereco(),d.data(),d.itens());
            pedido.setAgendadoPara(d.agendado());pedido.setNomeEntregador(d.entregador());pedido.setTaxaEntrega(d.taxa());pedido.setDesconto(d.desconto());pedido.setBrinde(d.brinde());
            pedido.setStatusPagamento(d.situacao());pedido.setPixDemonstracao(d.demo());pedido.setRoletaLiberada(d.roleta());
            if(d.pagamento()!=null)pedido.setFormaPagamento(com.salgaderia.app.model.enums.FormaPagamento.valueOf(d.pagamento()));resultado.add(pedido);});
        return resultado;
    }
    private static java.util.Set<Long> opcionaisIds(ResultSet rs) throws SQLException {
        String ids = rs.getString("opcionais_ids");
        return ids == null ? java.util.Set.of() : java.util.Set.copyOf(java.util.Arrays.asList(JSON.fromJson(ids, Long[].class)));
    }
    private static PersonalizacaoItem personalizacao(ResultSet rs) throws SQLException {
        String grupo=rs.getString("grupo_personalizado");
        if(grupo==null)return null;
        var restricoes=EnumSet.noneOf(PersonalizacaoItem.Restricao.class);
        String armazenadas=rs.getString("restricoes_personalizadas");
        if(armazenadas!=null&&!armazenadas.isBlank())
            for(String nome:armazenadas.split(","))restricoes.add(PersonalizacaoItem.Restricao.valueOf(nome));
        var tipo=PersonalizacaoItem.Tipo.valueOf(rs.getString("tipo_personalizacao"));
        FiltrosCaixa filtros=JSON.fromJson(rs.getString("filtros_caixa"),FiltrosCaixa.class);
        return filtros==null?new PersonalizacaoItem(UUID.fromString(grupo),tipo,restricoes):
                new PersonalizacaoItem(UUID.fromString(grupo),tipo,restricoes,filtros.categorias(),filtros.produtos());
    }
    @Override public java.util.List<PedidoFila> listarFila(Connection c) throws SQLException {
        String sql="SELECT p.id,p.status,p.tipo_entrega,p.endereco_entrega,p.criado_em,p.agendado_para,p.desconto,p.status_pagamento,p.pix_provedor,p.taxa_entrega,p.brinde_descricao,p.nome_entregador,COALESCE(ct.nome,u.nome,'Cliente') cliente,COALESCE(NULLIF(TRIM(ct.whatsapp),''),NULLIF(TRIM(ct.telefone),'')) telefone,"
                + "i.quantidade,i.preco_unitario,i.observacao_personalizada,i.grupo_personalizado,i.adicionais_resumo,pr.nome produto FROM pedidos p JOIN usuarios u ON u.id=p.usuario_id "
                + "LEFT JOIN contatos ct ON ct.id=p.contato_id LEFT JOIN pedido_itens i ON i.pedido_id=p.id "
                + "LEFT JOIN produtos pr ON pr.id=i.produto_id WHERE p.origem='VITRINE' AND u.perfil='CLIENTE' "
                + "AND p.status IN ('PENDENTE','PREPARANDO','PRONTO','SAIU_ENTREGA') AND p.status_pagamento<>'AGUARDANDO_PIX' ORDER BY p.criado_em,p.id,i.id";
        class Grupo {
            long id;String cliente,endereco,telefone;com.salgaderia.app.model.enums.StatusPedido status;com.salgaderia.app.model.enums.TipoEntrega entrega;
            LocalDateTime data,agendado;BigDecimal total=BigDecimal.ZERO, desconto;
            StatusPagamento pagamento;String provedor,brinde,entregador;
            final java.util.List<StringJoiner> itens=new java.util.ArrayList<>();
            final Map<String,StringJoiner> personalizados=new java.util.HashMap<>();

            final java.util.List<String> preparo = new java.util.ArrayList<>();

            void adicionar(String grupo,String observacao,String adicionais,String item) {
                if(grupo==null){
                    itens.add(new StringJoiner("").add(item));
                    if (adicionais != null || observacao != null)
                        preparo.add(item + (adicionais == null ? "" : "\n" + adicionais)
                                + (observacao == null ? "" : "\nObservação: " + observacao));
                    return;
                }
                personalizados.computeIfAbsent(grupo,chave->{
                    StringJoiner componentes=new StringJoiner(", ",observacao+": ","");
                    itens.add(componentes);
                    return componentes;
                }).add(item);
            }
        }
        java.util.Map<Long,Grupo> grupos=new java.util.LinkedHashMap<>();
        try(PreparedStatement ps=c.prepareStatement(sql);ResultSet rs=ps.executeQuery()){
            while(rs.next()){
                long id=rs.getLong("id");Grupo g=grupos.get(id);
                if(g==null){g=new Grupo();g.id=id;g.cliente=rs.getString("cliente");g.telefone=rs.getString("telefone");g.endereco=rs.getString("endereco_entrega");
                    g.status=com.salgaderia.app.model.enums.StatusPedido.valueOf(rs.getString("status"));g.entrega=com.salgaderia.app.model.enums.TipoEntrega.valueOf(rs.getString("tipo_entrega"));g.data=rs.getTimestamp("criado_em").toLocalDateTime();Timestamp agendado=rs.getTimestamp("agendado_para");g.agendado=agendado==null?null:agendado.toLocalDateTime();g.desconto=rs.getBigDecimal("desconto");
                    g.total=rs.getBigDecimal("taxa_entrega");g.pagamento=StatusPagamento.valueOf(rs.getString("status_pagamento"));
                    g.entregador=rs.getString("nome_entregador");g.provedor=rs.getString("pix_provedor");g.brinde=rs.getString("brinde_descricao");grupos.put(id,g);}
                BigDecimal quantidade=rs.getBigDecimal("quantidade");if(quantidade!=null){
                    g.adicionar(rs.getString("grupo_personalizado"),rs.getString("observacao_personalizada"),rs.getString("adicionais_resumo"),
                            quantidade.stripTrailingZeros().toPlainString()+" × "+rs.getString("produto"));
                    g.total=g.total.add(quantidade.multiply(rs.getBigDecimal("preco_unitario")));
                }
            }
        }
        java.util.List<PedidoFila> resultado=new java.util.ArrayList<>();
        for(Grupo g:grupos.values())resultado.add(new PedidoFila(g.id,g.cliente,g.status,g.entrega,g.endereco,g.data,g.total.subtract(g.desconto),
                g.itens.stream().map(StringJoiner::toString).collect(Collectors.joining("\n"))+(g.brinde==null?"":"\nBrinde: "+g.brinde),
                g.telefone,g.agendado,g.pagamento,"DEMO".equals(g.provedor),"MERCADO_PAGO".equals(g.provedor),g.entregador,g.preparo));
        return resultado;
    }
    @Override public void registrarPdv(Connection c,long id,com.salgaderia.app.model.enums.FormaPagamento forma,BigDecimal recebido,BigDecimal troco) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("UPDATE pedidos SET origem='PDV',status='ENTREGUE',status_pagamento='PAGO',forma_pagamento=?,valor_recebido=?,troco=? WHERE id=? AND status='PENDENTE'")){
            ps.setString(1,forma.name());ps.setBigDecimal(2,recebido);ps.setBigDecimal(3,troco);ps.setLong(4,id);if(ps.executeUpdate()!=1)throw new SQLException("Venda PDV não atualizada");
        }
    }
    @Override public java.util.Map<String,BigDecimal> produtosMaisVendidos(Connection c,LocalDateTime inicio,LocalDateTime fim) throws SQLException {
        return agrupar(c,"SELECT CONCAT(pr.nome,' (#',pr.id,')') rotulo,SUM(i.quantidade) valor FROM pedidos p JOIN pedido_itens i ON i.pedido_id=p.id JOIN produtos pr ON pr.id=i.produto_id WHERE p.status='ENTREGUE' AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO') AND p.criado_em>=? AND p.criado_em<? GROUP BY pr.id,pr.nome ORDER BY valor DESC,pr.id LIMIT 10",inicio,fim);
    }
    @Override public java.util.Map<String,BigDecimal> receitaPorCategoria(Connection c,LocalDateTime inicio,LocalDateTime fim) throws SQLException {
        return agrupar(c,"SELECT COALESCE(cat.nome,'Sem categoria') rotulo,SUM(i.quantidade*i.preco_unitario*(1-COALESCE((CASE WHEN p.estrategia_desconto='FRETE' THEN GREATEST(p.desconto-p.taxa_entrega,0) ELSE p.desconto END)/NULLIF(t.subtotal,0),0))) valor FROM pedidos p JOIN pedido_itens i ON i.pedido_id=p.id JOIN (SELECT pedido_id,SUM(quantidade*preco_unitario) subtotal FROM pedido_itens GROUP BY pedido_id) t ON t.pedido_id=p.id JOIN produtos pr ON pr.id=i.produto_id LEFT JOIN categorias cat ON cat.id=pr.categoria_id WHERE p.status='ENTREGUE' AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO') AND p.criado_em>=? AND p.criado_em<? GROUP BY cat.id,cat.nome ORDER BY valor DESC",inicio,fim);
    }
    private java.util.Map<String,BigDecimal> agrupar(Connection c,String sql,LocalDateTime inicio,LocalDateTime fim) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setTimestamp(1,Timestamp.valueOf(inicio));ps.setTimestamp(2,Timestamp.valueOf(fim));try(ResultSet rs=ps.executeQuery()){
            java.util.Map<String,BigDecimal> dados=new java.util.LinkedHashMap<>();while(rs.next())dados.merge(rs.getString("rotulo"),rs.getBigDecimal("valor"),BigDecimal::add);return dados;
        }}
    }
    @Override public long inserir(Connection c,Pedido pedido,Map<Long,BigDecimal> custos,Map<Long,BigDecimal> baixas) throws SQLException {
        long id;
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO pedidos(usuario_id,contato_id,status,tipo_entrega,endereco_entrega,criado_em,forma_pagamento,agendado_para) VALUES (?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            if(pedido.getUsuario()==null)ps.setNull(1,Types.BIGINT);else ps.setLong(1,pedido.getUsuario().getId());
            if(pedido.getCliente()==null)ps.setNull(2,Types.BIGINT);else ps.setLong(2,pedido.getCliente().getId());
            ps.setString(3,pedido.getStatus().name());ps.setString(4,pedido.getTipoEntrega().name());ps.setString(5,pedido.getEnderecoEntrega());ps.setTimestamp(6,Timestamp.valueOf(pedido.getCriadoEm()));ps.setString(7,pedido.getFormaPagamento()==null?null:pedido.getFormaPagamento().name());ps.setTimestamp(8,pedido.getAgendadoPara()==null?null:Timestamp.valueOf(pedido.getAgendadoPara()));ps.executeUpdate();
            try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("ID de pedido não retornado");id=rs.getLong(1);}
        }
        Map<Long,BigDecimal> restante=new HashMap<>(baixas);
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO pedido_itens(pedido_id,produto_id,quantidade,preco_unitario,custo_unitario,observacao_personalizada,grupo_personalizado,tipo_personalizacao,restricoes_personalizadas,quantidade_baixada,filtros_caixa,adicionais_resumo,opcionais_ids) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")){
            for(PedidoItem item:pedido.getItens()){
                long produtoId=item.getProduto().getId();BigDecimal custo=custos.get(produtoId);if(custo==null)throw new SQLException("Custo do produto não informado");
                ps.setLong(1,id);ps.setLong(2,produtoId);ps.setBigDecimal(3,item.getQuantidade());ps.setBigDecimal(4,item.getPrecoUnitario());ps.setBigDecimal(5,custo);
                PersonalizacaoItem personalizacao=item.getPersonalizacao();
                ps.setString(6,item.getObservacaoPersonalizada());
                ps.setString(7,personalizacao==null?null:personalizacao.grupo().toString());
                ps.setString(8,personalizacao==null?null:personalizacao.tipo().name());
                ps.setString(9,personalizacao==null?null:personalizacao.restricoes().stream().sorted().map(Enum::name).collect(Collectors.joining(",")));
                BigDecimal saldo=restante.getOrDefault(produtoId,BigDecimal.ZERO);
                BigDecimal baixa=saldo.min(item.getQuantidade());
                restante.put(produtoId,saldo.subtract(baixa));
                ps.setBigDecimal(10,baixa);
                ps.setString(11,personalizacao==null||personalizacao.tipo().orcamento()==null?null:
                        JSON.toJson(new FiltrosCaixa(personalizacao.categoriasExcluidas(),personalizacao.produtosExcluidos())));
                ps.setString(12, item.getAdicionaisResumo());
                ps.setString(13, item.getOpcionaisIds().isEmpty() ? null : JSON.toJson(item.getOpcionaisIds()));
                ps.addBatch();
            } ps.executeBatch();
        } return id;
    }
    @Override public Fluxo bloquearFluxo(Connection c,long id) throws SQLException {
        return bloquearFluxo(c, id, null);
    }
    @Override public Fluxo bloquearFluxoDoCliente(Connection c,long id,long usuarioId) throws SQLException {
        return bloquearFluxo(c, id, usuarioId);
    }
    private Fluxo bloquearFluxo(Connection c, long id, Long usuarioId) throws SQLException {
        String filtro = usuarioId == null ? "" : " AND usuario_id=? AND origem='VITRINE'";
        try(var ps=c.prepareStatement("SELECT status,tipo_entrega FROM pedidos WHERE id=?" + filtro + " FOR UPDATE")) {
            ps.setLong(1,id);
            if (usuarioId != null) ps.setLong(2, usuarioId);
            try(var rs=ps.executeQuery()) {
                if(!rs.next())throw new IllegalArgumentException("Pedido não encontrado");
                return new Fluxo(StatusPedido.valueOf(rs.getString(1)),TipoEntrega.valueOf(rs.getString(2)));
            }
        }
    }
    @Override public void registrarSaidaEntrega(Connection c,long id,String entregador) throws SQLException {
        try(var ps=c.prepareStatement("UPDATE pedidos SET status='SAIU_ENTREGA',nome_entregador=? WHERE id=? AND status='PRONTO' AND tipo_entrega='ENTREGA'")) {
            ps.setString(1,entregador);ps.setLong(2,id);
            if(ps.executeUpdate()!=1)throw new SQLException("Saída para entrega não registrada.");
        }
    }
    @Override public StatusPagamento statusPagamento(Connection c,long id)throws SQLException{
        try(var ps=c.prepareStatement("SELECT status_pagamento FROM pedidos WHERE id=? FOR UPDATE")){
            ps.setLong(1,id);try(var rs=ps.executeQuery()){
                if(!rs.next())throw new IllegalArgumentException("Pedido não encontrado.");
                return StatusPagamento.valueOf(rs.getString(1));
            }
        }
    }
    @Override public Map<Long,BigDecimal> quantidadesBaixadas(Connection c,long id) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT produto_id,SUM(COALESCE(quantidade_baixada,quantidade)) AS baixa FROM pedido_itens WHERE pedido_id=? GROUP BY produto_id HAVING baixa>0 ORDER BY produto_id")) {
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){Map<Long,BigDecimal> resultado=new java.util.LinkedHashMap<>();while(rs.next())resultado.put(rs.getLong(1),rs.getBigDecimal(2));return resultado;}
        }
    }
    @Override public void atualizarStatus(Connection c,long id,com.salgaderia.app.model.enums.StatusPedido status) throws SQLException {
        String anonimizar=status==com.salgaderia.app.model.enums.StatusPedido.CANCELADO?",pix_payload=NULL,pix_pagador_email=NULL,pix_pagador_documento=NULL":"";
        try(PreparedStatement ps=c.prepareStatement("UPDATE pedidos SET status=?,status_pagamento=IF(?='ENTREGUE' AND status_pagamento='NA_ENTREGA','PAGO',status_pagamento)"+anonimizar+" WHERE id=?")){
            ps.setString(1,status.name());ps.setString(2,status.name());ps.setLong(3,id);if(ps.executeUpdate()!=1)throw new SQLException("Pedido não atualizado");}
    }
    @Override public boolean possuiPedidoEntregue(Connection c,long usuarioId,long produtoId) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM pedidos p JOIN pedido_itens i ON i.pedido_id=p.id WHERE p.usuario_id=? AND p.status='ENTREGUE' AND i.produto_id=? LIMIT 1")){
            ps.setLong(1,usuarioId);ps.setLong(2,produtoId);try(ResultSet rs=ps.executeQuery()){return rs.next();}
        }
    }
    @Override public BigDecimal[] somarVendasEntregues(Connection c,LocalDateTime inicio,LocalDateTime fim) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT COALESCE(SUM(t.subtotal+p.taxa_entrega-p.desconto),0),COALESCE(SUM(t.custo),0),COALESCE(SUM(t.sem_custo),0),COALESCE(SUM(t.subtotal+p.taxa_entrega),0) FROM pedidos p JOIN (SELECT pedido_id,SUM(quantidade*preco_unitario) subtotal,SUM(quantidade*custo_unitario) custo,SUM(custo_unitario IS NULL) sem_custo FROM pedido_itens GROUP BY pedido_id) t ON p.id=t.pedido_id WHERE p.status='ENTREGUE' AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO') AND p.criado_em>=? AND p.criado_em<?")){
            ps.setTimestamp(1,Timestamp.valueOf(inicio));ps.setTimestamp(2,Timestamp.valueOf(fim));try(ResultSet rs=ps.executeQuery()){rs.next();if(rs.getLong(3)>0)throw new SQLException("Vendas sem custo histórico");return new BigDecimal[]{rs.getBigDecimal(1),rs.getBigDecimal(2),rs.getBigDecimal(4)};}
        }
    }
    @Override public long contarEntreguesDoCliente(Connection c,long clienteId) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM pedidos WHERE contato_id=? AND status='ENTREGUE' AND (pix_provedor IS NULL OR pix_provedor<>'DEMO')")) {
            ps.setLong(1,clienteId); try (ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }
    @Override public void registrarDesconto(Connection c,long id,com.salgaderia.app.service.desconto.ResultadoDesconto desconto) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("UPDATE pedidos SET desconto=?,estrategia_desconto=?,cupom_codigo=?,taxa_entrega=?,brinde_descricao=? WHERE id=?")) {
            ps.setBigDecimal(1,desconto.desconto());ps.setString(2,desconto.estrategia());ps.setString(3,desconto.cupom());
            ps.setBigDecimal(4,desconto.taxaEntrega());ps.setString(5,desconto.brinde());ps.setLong(6,id);
            if(ps.executeUpdate()!=1)throw new SQLException("Desconto não registrado");
        }
    }
    @Override public long contarFilaCozinha(Connection c) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM pedidos WHERE status IN ('PENDENTE','PREPARANDO') AND status_pagamento<>'AGUARDANDO_PIX' AND (agendado_para IS NULL OR agendado_para<=NOW())");ResultSet rs=ps.executeQuery()){rs.next();return rs.getLong(1);}
    }
}
