-- v1.9.0. Aplique uma vez após a etapa 18, com backup.
USE salgaderia_db;

ALTER TABLE produtos
    ADD COLUMN etiqueta_destaque VARCHAR(40) NULL,
    ADD COLUMN pontos_resgate INT NULL CHECK (pontos_resgate > 0);

CREATE TABLE cliente_favoritos (
    cliente_id BIGINT UNSIGNED NOT NULL,
    produto_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (cliente_id,produto_id),
    FOREIGN KEY (cliente_id) REFERENCES contatos(id) ON DELETE CASCADE,
    FOREIGN KEY (produto_id) REFERENCES produtos(id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE pedidos
    ADD COLUMN pago_em DATETIME NULL,
    ADD COLUMN frete_isento_vip DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (frete_isento_vip >= 0),
    ADD INDEX idx_pedidos_pago_em (pago_em),
    MODIFY COLUMN forma_pagamento ENUM('DINHEIRO','PIX','CARTAO_CREDITO','CARTAO_DEBITO','CARTAO','CARTAO_APP') NULL,
    MODIFY COLUMN status_pagamento ENUM('AGUARDANDO_PIX','PAGO','NA_ENTREGA','AGUARDANDO_CARTAO') NOT NULL DEFAULT 'NA_ENTREGA';

ALTER TABLE pedido_itens
    ADD COLUMN pontos_resgate INT NULL CHECK (pontos_resgate > 0),
    ADD COLUMN valor_resgate DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (valor_resgate >= 0);

CREATE TABLE cliente_pontos_lotes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT UNSIGNED NOT NULL,
    pedido_id BIGINT UNSIGNED NOT NULL UNIQUE,
    pontos_disponiveis BIGINT UNSIGNED NOT NULL,
    expira_em DATETIME NOT NULL,
    INDEX idx_lotes_consumo (cliente_id,expira_em,id),
    FOREIGN KEY (cliente_id) REFERENCES contatos(id) ON DELETE CASCADE,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE cliente_pontos_resgates (
    pedido_id BIGINT UNSIGNED NOT NULL,
    lote_id BIGINT UNSIGNED NOT NULL,
    pontos INT UNSIGNED NOT NULL CHECK (pontos > 0),
    PRIMARY KEY (pedido_id,lote_id),
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    FOREIGN KEY (lote_id) REFERENCES cliente_pontos_lotes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Pedidos históricos não registravam a confirmação. Usa-se a criação como aproximação, sem renovar a validade.
UPDATE pedidos SET pago_em=criado_em, status_pagamento='PAGO'
WHERE status_pagamento='PAGO' OR (status='ENTREGUE' AND status_pagamento='NA_ENTREGA');

INSERT INTO cliente_pontos_lotes (cliente_id,pedido_id,pontos_disponiveis,expira_em)
SELECT p.contato_id,p.id,FLOOR(ROUND(SUM(i.quantidade*i.preco_unitario)+p.taxa_entrega-p.desconto,2)),
       DATE_ADD(p.pago_em, INTERVAL 60 DAY)
FROM pedidos p JOIN pedido_itens i ON i.pedido_id=p.id JOIN contatos c ON c.id=p.contato_id AND c.tipo='CLIENTE'
WHERE p.status<>'CANCELADO' AND (p.pix_provedor IS NULL OR p.pix_provedor<>'DEMO')
    AND p.pago_em>NOW()-INTERVAL 60 DAY
GROUP BY p.id,p.contato_id,p.pago_em,p.taxa_entrega,p.desconto
HAVING FLOOR(ROUND(SUM(i.quantidade*i.preco_unitario)+p.taxa_entrega-p.desconto,2))>0;
