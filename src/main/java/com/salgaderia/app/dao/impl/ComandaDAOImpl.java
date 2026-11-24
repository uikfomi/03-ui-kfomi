package com.salgaderia.app.dao.impl;

import com.salgaderia.app.dao.ComandaDAO;
import com.salgaderia.app.model.comanda.*;
import com.salgaderia.app.model.enums.FormaPagamento;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public final class ComandaDAOImpl implements ComandaDAO {
    @Override public List<Mesa> listarMesas(Connection c)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("SELECT id,numero,nome,status FROM comandas_mesa ORDER BY numero");ResultSet rs=ps.executeQuery()){
            List<Mesa> mesas=new ArrayList<>();while(rs.next())mesas.add(mesa(rs));return mesas;
        }
    }
    @Override public Mesa bloquearMesa(Connection c,long id)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("SELECT id,numero,nome,status FROM comandas_mesa WHERE id=? FOR UPDATE")){
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Mesa não encontrada");return mesa(rs);}
        }
    }
    private Mesa mesa(ResultSet rs)throws SQLException{return new Mesa(rs.getLong("id"),rs.getInt("numero"),rs.getString("nome"),StatusMesa.valueOf(rs.getString("status")));}
    @Override public long abrir(Connection c,long mesaId)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("UPDATE comandas_mesa SET status='OCUPADA' WHERE id=? AND status='LIVRE'")){ps.setLong(1,mesaId);if(ps.executeUpdate()!=1)throw new SQLException("Mesa não está livre");}
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO comanda_atendimentos(mesa_id,aberto_em) VALUES (?,NOW())",Statement.RETURN_GENERATED_KEYS)){
            ps.setLong(1,mesaId);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("ID da comanda não retornado");return rs.getLong(1);}
        }
    }
    @Override public Optional<Comanda> buscarAberta(Connection c,Mesa mesa)throws SQLException{
        long atendimento;
        try(PreparedStatement ps=c.prepareStatement("SELECT id FROM comanda_atendimentos WHERE mesa_id=? AND fechado_em IS NULL")){
            ps.setLong(1,mesa.id());try(ResultSet rs=ps.executeQuery()){if(!rs.next())return Optional.empty();atendimento=rs.getLong(1);if(rs.next())throw new SQLException("Mais de uma comanda aberta para a mesa");}
        }
        List<ItemComanda> itens=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement("SELECT i.id,i.produto_id,p.nome,i.quantidade,i.preco_unitario,COALESCE((SELECT SUM(pg.quantidade) FROM comanda_pagamentos_parciais pg WHERE pg.item_id=i.id),0) pago FROM comanda_itens i JOIN produtos p ON p.id=i.produto_id WHERE i.atendimento_id=? ORDER BY i.id")){
            ps.setLong(1,atendimento);try(ResultSet rs=ps.executeQuery()){while(rs.next())itens.add(new ItemComanda(rs.getLong("id"),rs.getLong("produto_id"),rs.getString("nome"),rs.getBigDecimal("quantidade"),rs.getBigDecimal("preco_unitario"),rs.getBigDecimal("pago")));}
        }
        List<PagamentoComanda> pagamentos=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement("SELECT id,pessoa,forma_pagamento,valor,item_id,quantidade FROM comanda_pagamentos_parciais WHERE atendimento_id=? ORDER BY id")){
            ps.setLong(1,atendimento);try(ResultSet rs=ps.executeQuery()){while(rs.next())pagamentos.add(new PagamentoComanda(rs.getLong("id"),rs.getString("pessoa"),FormaPagamento.valueOf(rs.getString("forma_pagamento")),rs.getBigDecimal("valor"),rs.getObject("item_id",Long.class),rs.getBigDecimal("quantidade")));}
        }
        return Optional.of(new Comanda(mesa,atendimento,itens,pagamentos));
    }
    @Override public void inserirItem(Connection c,long atendimento,long produto,BigDecimal quantidade,BigDecimal preco,BigDecimal custo)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO comanda_itens(atendimento_id,produto_id,quantidade,preco_unitario,custo_unitario) VALUES (?,?,?,?,?)")){
            ps.setLong(1,atendimento);ps.setLong(2,produto);ps.setBigDecimal(3,quantidade);ps.setBigDecimal(4,preco);ps.setBigDecimal(5,custo);ps.executeUpdate();
        }
    }
    @Override public void inserirPagamento(Connection c,long atendimento,String pessoa,FormaPagamento forma,BigDecimal valor,Long item,BigDecimal quantidade)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO comanda_pagamentos_parciais(atendimento_id,pessoa,forma_pagamento,valor,item_id,quantidade,criado_em) VALUES (?,?,?,?,?,?,NOW())")){
            ps.setLong(1,atendimento);ps.setString(2,pessoa);ps.setString(3,forma.name());ps.setBigDecimal(4,valor);
            if(item==null)ps.setNull(5,Types.BIGINT);else ps.setLong(5,item);ps.setBigDecimal(6,quantidade);ps.executeUpdate();
        }
    }
    @Override public long concluirVenda(Connection c,long atendimento,long usuarioId)throws SQLException{
        long pedido;
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO pedidos(usuario_id,status,tipo_entrega,criado_em,origem,status_pagamento,pago_em) VALUES (?,'ENTREGUE','LOCAL',NOW(),'COMANDA','PAGO',NOW())",Statement.RETURN_GENERATED_KEYS)){
            ps.setLong(1,usuarioId);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("ID de venda não retornado");pedido=rs.getLong(1);}
        }
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO pedido_itens(pedido_id,produto_id,quantidade,preco_unitario,custo_unitario) SELECT ?,produto_id,quantidade,preco_unitario,custo_unitario FROM comanda_itens WHERE atendimento_id=?")){
            ps.setLong(1,pedido);ps.setLong(2,atendimento);if(ps.executeUpdate()==0)throw new SQLException("Comanda sem itens");
        }return pedido;
    }
    @Override public void fechar(Connection c,long mesa,long atendimento,Long pedido)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("UPDATE comanda_atendimentos SET fechado_em=NOW(),pedido_id=? WHERE id=? AND fechado_em IS NULL")){if(pedido==null)ps.setNull(1,Types.BIGINT);else ps.setLong(1,pedido);ps.setLong(2,atendimento);if(ps.executeUpdate()!=1)throw new SQLException("Comanda não foi fechada");}
        try(PreparedStatement ps=c.prepareStatement("UPDATE comandas_mesa SET status='LIVRE' WHERE id=? AND status='OCUPADA'")){ps.setLong(1,mesa);if(ps.executeUpdate()!=1)throw new SQLException("Mesa não foi liberada");}
    }
}
