package com.salgaderia.app.dao.impl;

import com.salgaderia.app.model.IndicadoresComerciais.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InteligenciaComercialDAO {
    public ResumoCliente resumo(Connection c, long cliente, LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        try (var ps = c.prepareStatement("""
                WITH compras AS (
                    SELECT p.id,p.contato_id,ROUND(SUM(i.quantidade*i.preco_unitario)+p.taxa_entrega-p.desconto,2) total,
                           p.desconto+p.frete_isento_vip+SUM(i.valor_resgate) economia
                    FROM pedidos p JOIN pedido_itens i ON i.pedido_id=p.id
                    JOIN usuarios u ON u.cliente_id=p.contato_id AND u.ativo AND u.perfil='CLIENTE'
                    WHERE p.status_pagamento='PAGO' AND p.status<>'CANCELADO' AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO')
                        AND p.pago_em>=? AND p.pago_em<?
                    GROUP BY p.id,p.contato_id,p.taxa_entrega,p.desconto,p.frete_isento_vip
                ), totais AS (
                    SELECT contato_id,SUM(total) gasto,SUM(economia) economia FROM compras GROUP BY contato_id
                )
                SELECT gasto,economia,(gasto>0 AND gasto=(SELECT MAX(gasto) FROM totais)) FROM totais WHERE contato_id=?
                """)) {
            intervalo(ps, inicio, fim, 1); ps.setLong(3, cliente);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? new ResumoCliente(rs.getBigDecimal(1),rs.getBigDecimal(2),rs.getBoolean(3))
                        : new ResumoCliente(BigDecimal.ZERO,BigDecimal.ZERO,false);
            }
        }
    }
    public List<ClienteRadar> radar(Connection c, LocalDate hoje, LocalDateTime limite, Long usuario) throws SQLException {
        try (var ps = c.prepareStatement("""
                WITH atividade AS (
                    SELECT p.contato_id,MAX(p.criado_em) ultimo
                    FROM pedidos p WHERE p.contato_id IS NOT NULL AND p.status<>'CANCELADO'
                        AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO')
                    GROUP BY p.contato_id
                ), segmentos AS (
                    SELECT u.id,u.cliente_id,ct.nome,COALESCE(NULLIF(TRIM(ct.whatsapp),''),NULLIF(TRIM(ct.telefone),'')) telefone,
                        EXISTS (SELECT 1 FROM assinaturas_clientes a JOIN planos_assinatura pl ON pl.id=a.plano_id
                            WHERE a.usuario_id=u.id AND NOT a.cancelada AND a.valida_ate>? AND pl.ativo) vip,
                        (atividade.ultimo IS NOT NULL AND atividade.ultimo<?) sumido,atividade.ultimo
                    FROM usuarios u JOIN contatos ct ON ct.id=u.cliente_id LEFT JOIN atividade ON atividade.contato_id=u.cliente_id
                    WHERE u.ativo AND u.perfil='CLIENTE' AND (? IS NULL OR u.id=?)
                )
                SELECT * FROM segmentos WHERE vip OR sumido ORDER BY vip DESC,sumido DESC,nome,id
                """)) {
            ps.setObject(1,hoje); ps.setObject(2,limite); ps.setObject(3,usuario,Types.BIGINT); ps.setObject(4,usuario,Types.BIGINT);
            try (var rs = ps.executeQuery()) {
                List<ClienteRadar> clientes = new ArrayList<>();
                while (rs.next()) clientes.add(new ClienteRadar(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),
                        rs.getBoolean(5),rs.getBoolean(6),rs.getObject(7,LocalDateTime.class)));
                return clientes;
            }
        }
    }
    public Optional<CupomReativacao> cupomValido(Connection c, long usuario) throws SQLException {
        try (var ps = c.prepareStatement("""
                SELECT codigo,valido_ate FROM cupons WHERE usuario_exclusivo_id=? AND codigo LIKE 'VOLTA10-%' AND ativo
                  AND usado_pedido_id IS NULL AND valido_ate>NOW() ORDER BY id DESC LIMIT 1
                """)) {
            ps.setLong(1,usuario);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new CupomReativacao(rs.getString(1),rs.getObject(2,LocalDateTime.class))) : Optional.empty();
            }
        }
    }
    public void inserirReativacao(Connection c, long usuario, String codigo, LocalDateTime validade) throws SQLException {
        try (var ps = c.prepareStatement("INSERT INTO cupons(codigo,percentual,usuario_exclusivo_id,valido_ate) VALUES (?,10,?,?)")) {
            ps.setString(1,codigo); ps.setLong(2,usuario); ps.setObject(3,validade); ps.executeUpdate();
        }
    }
    public List<EntradaCaixa> entradas(Connection c, LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        try (var ps = c.prepareStatement("""
                WITH recebimentos AS (
                    SELECT CASE
                        WHEN p.forma_pagamento='PIX' THEN 'PIX' WHEN p.forma_pagamento='DINHEIRO' THEN 'Dinheiro'
                        WHEN p.forma_pagamento='CARTAO_APP' THEN 'Cartão App'
                        WHEN p.forma_pagamento IS NULL THEN 'Não informado'
                        WHEN p.tipo_entrega='ENTREGA' THEN 'Maquininha na Entrega' ELSE 'Cartão no Balcão' END canal,
                        ROUND(SUM(i.quantidade*i.preco_unitario)+p.taxa_entrega-p.desconto,2) valor
                    FROM pedidos p JOIN pedido_itens i ON i.pedido_id=p.id
                    WHERE p.origem<>'COMANDA' AND p.status_pagamento='PAGO' AND p.status<>'CANCELADO'
                        AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO') AND p.pago_em>=? AND p.pago_em<?
                    GROUP BY p.id,p.forma_pagamento,p.tipo_entrega,p.taxa_entrega,p.desconto
                    UNION ALL
                    SELECT CASE WHEN forma_pagamento='PIX' THEN 'PIX' WHEN forma_pagamento='DINHEIRO' THEN 'Dinheiro'
                            ELSE 'Cartão no Balcão' END, valor
                    FROM comanda_pagamentos_parciais WHERE criado_em>=? AND criado_em<?
                )
                SELECT canal,COUNT(*),SUM(valor) FROM recebimentos WHERE valor>0 GROUP BY canal ORDER BY canal
                """)) {
            intervalo(ps,inicio,fim,1); intervalo(ps,inicio,fim,3);
            try (var rs = ps.executeQuery()) {
                List<EntradaCaixa> entradas = new ArrayList<>();
                while(rs.next()) entradas.add(new EntradaCaixa(rs.getString(1),rs.getLong(2),rs.getBigDecimal(3)));
                return entradas;
            }
        }
    }
    public List<BairroRanking> bairros(Connection c, LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        try (var ps = c.prepareStatement("""
                SELECT b.nome,COUNT(*) quantidade FROM pedidos p JOIN bairros_entrega b ON b.id=p.bairro_id
                WHERE p.tipo_entrega='ENTREGA' AND p.status_pagamento='PAGO' AND p.status<>'CANCELADO'
                      AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO') AND p.pago_em>=? AND p.pago_em<?
                GROUP BY b.id,b.nome ORDER BY quantidade DESC,b.nome
                """)) {
            intervalo(ps,inicio,fim,1);
            try (var rs = ps.executeQuery()) {
                List<BairroRanking> bairros = new ArrayList<>();
                while(rs.next()) bairros.add(new BairroRanking(rs.getString(1),rs.getLong(2)));
                return bairros;
            }
        }
    }
    private static void intervalo(PreparedStatement ps, LocalDateTime inicio, LocalDateTime fim, int primeiro) throws SQLException {
        ps.setObject(primeiro,inicio); ps.setObject(primeiro+1,fim);
    }
}
