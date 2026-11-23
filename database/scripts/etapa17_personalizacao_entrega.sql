USE salgaderia_db;

ALTER TABLE categorias ADD COLUMN ativa BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE configuracao_loja
    ADD COLUMN pedido_minimo_entrega DECIMAL(10,2) NOT NULL DEFAULT 20.00,
    ADD CONSTRAINT ck_loja_pedido_minimo CHECK (pedido_minimo_entrega >= 0);

CREATE TABLE produto_opcionais (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT UNSIGNED NOT NULL,
    grupo_nome VARCHAR(100) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    preco_adicional DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (preco_adicional >= 0),
    gratis_ate_limite SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    maximo_escolhas SMALLINT UNSIGNED NOT NULL,
    UNIQUE KEY uk_opcional_nome (produto_id, grupo_nome, nome),
    CHECK (maximo_escolhas > 0 AND gratis_ate_limite <= maximo_escolhas),
    FOREIGN KEY (produto_id) REFERENCES produtos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- O nome físico existente é pedido_itens. O resumo é histórico; os IDs permitem refazer a compra com preços atuais.
ALTER TABLE pedido_itens ADD COLUMN adicionais_resumo TEXT NULL, ADD COLUMN opcionais_ids JSON NULL;
