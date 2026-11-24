package com.salgaderia.app.dao.impl;

import com.salgaderia.app.dao.PixPagamentoDAO;
import com.salgaderia.app.model.enums.StatusPagamento;
import com.salgaderia.app.model.enums.StatusPedido;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PixPagamentoDAOImpl implements PixPagamentoDAO {
    private static final String SELECT="""
            SELECT p.id,p.usuario_id,p.contato_id,p.status,p.status_pagamento,p.pix_provedor,p.transacao_pix_id,
            p.pix_referencia,p.pix_pagador_email,p.pix_pagador_documento,p.pix_primeira_tentativa,p.pix_payload,
            DATE_ADD(p.pix_primeira_tentativa,INTERVAL 20 MINUTE)>NOW() pode_repetir,
            ROUND(COALESCE((SELECT SUM(quantidade*preco_unitario) FROM pedido_itens WHERE pedido_id=p.id),0)
            +p.taxa_entrega-p.desconto,2) total FROM pedidos p
            """;

    @Override public void preparar(Connection c,long pedidoId,Provedor provedor,String referencia,String email,String documento,StatusPagamento status)throws SQLException{
        try(var ps=c.prepareStatement("""
                UPDATE pedidos SET pix_provedor=?,pix_referencia=?,pix_pagador_email=?,pix_pagador_documento=?,
                    status_pagamento=? WHERE id=?
                """)){
            ps.setString(1,provedor==null?null:provedor.name());ps.setString(2,referencia);ps.setString(3,email);ps.setString(4,documento);
            ps.setString(5,status.name());ps.setLong(6,pedidoId);ps.executeUpdate();
        }
    }
    @Override public Optional<Dados> buscar(Connection c,long pedidoId,boolean bloquear)throws SQLException{
        try(var ps=c.prepareStatement(SELECT+" WHERE p.id=? AND p.pix_provedor IS NOT NULL"+(bloquear?" FOR UPDATE":""))){
            ps.setLong(1,pedidoId);try(var rs=ps.executeQuery()){return rs.next()?Optional.of(mapear(rs)):Optional.empty();}
        }
    }
    @Override public List<Dados> pendentesConhecidos(Connection c,long cursor)throws SQLException{
        try(var ps=c.prepareStatement(SELECT+"""
                 WHERE p.id>? AND p.status='PENDENTE' AND p.status_pagamento='AGUARDANDO_PIX'
                   AND p.pix_provedor='MERCADO_PAGO' AND p.transacao_pix_id IS NOT NULL
                 ORDER BY p.id LIMIT 3
                """)){
            ps.setLong(1,cursor);try(var rs=ps.executeQuery()){
                List<Dados> resultado=new ArrayList<>();
                while(rs.next())resultado.add(mapear(rs));return resultado;
            }
        }
    }
    private static Dados mapear(ResultSet rs)throws SQLException{
        return new Dados(rs.getLong("id"),rs.getLong("usuario_id"),rs.getLong("contato_id"),
                StatusPedido.valueOf(rs.getString("status")),StatusPagamento.valueOf(rs.getString("status_pagamento")),
                Provedor.valueOf(rs.getString("pix_provedor")),rs.getString("transacao_pix_id"),rs.getString("pix_referencia"),
                rs.getString("pix_pagador_email"),rs.getString("pix_pagador_documento"),rs.getObject("pix_primeira_tentativa",LocalDateTime.class),
                rs.getBoolean("pode_repetir"),rs.getBigDecimal("total"),rs.getString("pix_payload"));
    }
    @Override public void marcarTentativa(Connection c,long pedidoId)throws SQLException{
        try(var ps=c.prepareStatement("UPDATE pedidos SET pix_primeira_tentativa=COALESCE(pix_primeira_tentativa,NOW()) WHERE id=?")){
            ps.setLong(1,pedidoId);ps.executeUpdate();
        }
    }
    @Override public void vincular(Connection c,long pedidoId,String transacao,String payload)throws SQLException{
        try(var ps=c.prepareStatement("""
                UPDATE pedidos SET transacao_pix_id=?,pix_payload=COALESCE(?,pix_payload),
                    pix_pagador_email=NULL,pix_pagador_documento=NULL WHERE id=?
                """)){
            ps.setString(1,transacao);ps.setString(2,payload);ps.setLong(3,pedidoId);ps.executeUpdate();
        }
    }
    @Override public void pagar(Connection c,long pedidoId)throws SQLException{
        try(var ps=c.prepareStatement("UPDATE pedidos SET status_pagamento='PAGO',pago_em=NOW(),pix_payload=NULL WHERE id=? AND status_pagamento='AGUARDANDO_PIX'")){
            ps.setLong(1,pedidoId);if(ps.executeUpdate()!=1)throw new SQLException("Pedido PIX não estava aguardando confirmação.");
        }
    }
}
