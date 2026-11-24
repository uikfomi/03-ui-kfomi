package com.salgaderia.app.dao.impl;

import com.salgaderia.app.dao.CupomDAO;
import com.salgaderia.app.service.desconto.Cupom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public final class CupomDAOImpl implements CupomDAO {
    @Override public Optional<Cupom> buscarAtivo(Connection conexao,String codigo,Long usuarioVitrine) throws SQLException {
        boolean reativacao = "VOLTA10".equals(codigo);
        String filtro = reativacao ? "codigo LIKE 'VOLTA10-%' AND usuario_exclusivo_id=?" :
                "codigo=? AND (usuario_exclusivo_id IS NULL OR usuario_exclusivo_id=?)";
        try (var ps = conexao.prepareStatement("""
                SELECT codigo,percentual,valor_minimo,beneficio,brinde_descricao,valor_beneficio
                FROM cupons WHERE ativo=TRUE AND (valido_ate IS NULL OR valido_ate>NOW()) AND usado_pedido_id IS NULL
                AND """ + filtro + " ORDER BY id DESC LIMIT 1")) {
            int indice = 1;
            if (!reativacao) ps.setString(indice++,codigo);
            ps.setObject(indice,usuarioVitrine,java.sql.Types.BIGINT);
            try (var rs = ps.executeQuery()) {
                return rs.next()?Optional.of(new Cupom(rs.getString(1),rs.getBigDecimal(2),rs.getBigDecimal(3),
                        Cupom.Beneficio.valueOf(rs.getString(4)),rs.getString(5),rs.getBigDecimal(6))):Optional.empty();
            }
        }
    }
    @Override public void reservar(Connection c,long usuarioVitrine,String codigo,long pedidoId)throws SQLException{
        try(var ps=c.prepareStatement("""
                UPDATE cupons SET usado_pedido_id=? WHERE codigo=? AND usuario_exclusivo_id=?
                AND ativo AND usado_pedido_id IS NULL AND (valido_ate IS NULL OR valido_ate>NOW())
                """)){
            ps.setLong(1,pedidoId);ps.setString(2,codigo);ps.setLong(3,usuarioVitrine);
            int atualizados=ps.executeUpdate();
            if(atualizados==0){
                try(var verificar=c.prepareStatement("SELECT id FROM cupons WHERE codigo=? AND usuario_exclusivo_id=?")){
                    verificar.setString(1,codigo);verificar.setLong(2,usuarioVitrine);
                    try(var rs=verificar.executeQuery()){if(rs.next())throw new IllegalArgumentException("Cupom exclusivo expirado ou já utilizado.");}
                }
            }
        }
    }
    @Override public void liberarReserva(Connection c,long pedidoId)throws SQLException{
        try(var ps=c.prepareStatement("UPDATE cupons SET usado_pedido_id=NULL WHERE usado_pedido_id=?")){
            ps.setLong(1,pedidoId);ps.executeUpdate();
        }
    }
}
