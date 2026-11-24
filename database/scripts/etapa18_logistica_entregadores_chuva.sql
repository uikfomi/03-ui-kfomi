-- Migração v1.8.0 — Logística inteligente de delivery, entregadores, modo chuva/pico e alertas de atraso
-- Aplique após etapa17_personalizacao_entrega.sql
USE salgaderia_db;

-- 1) Tabela de bairros de entrega
CREATE TABLE IF NOT EXISTS bairros_entrega (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    taxa_entrega DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (taxa_entrega >= 0),
    tempo_estimado_min INT UNSIGNED NOT NULL DEFAULT 30 CHECK (tempo_estimado_min > 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bairros iniciais de referência (Maranguape/Região Metropolitana de Fortaleza)
INSERT INTO bairros_entrega (nome, taxa_entrega, tempo_estimado_min, ativo) VALUES
    ('Centro', 3.00, 25, TRUE),
    ('Outrabanda', 5.00, 35, TRUE),
    ('Parque Iracema', 6.00, 40, TRUE),
    ('Urucará', 7.00, 45, TRUE)
ON DUPLICATE KEY UPDATE id = id;

-- 2) Tabela de entregadores
CREATE TABLE IF NOT EXISTS entregadores (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    telefone VARCHAR(30),
    placa_veiculo VARCHAR(10),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) Colunas na tabela pedidos para bairro, entregador e tempo estimado
ALTER TABLE pedidos
    ADD COLUMN bairro_id BIGINT UNSIGNED NULL,
    ADD COLUMN entregador_id BIGINT UNSIGNED NULL,
    ADD COLUMN tempo_estimado_min INT UNSIGNED NULL,
    ADD CHECK (tempo_estimado_min > 0),
    ADD FOREIGN KEY (bairro_id) REFERENCES bairros_entrega(id) ON DELETE SET NULL,
    ADD FOREIGN KEY (entregador_id) REFERENCES entregadores(id) ON DELETE SET NULL;

-- 4) Colunas de modo chuva/pico na configuracao_loja
ALTER TABLE configuracao_loja
    ADD COLUMN modo_chuva_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN acrescimo_tempo_chuva_min INT UNSIGNED NOT NULL DEFAULT 15,
    ADD COLUMN acrescimo_taxa_chuva DECIMAL(10,2) NOT NULL DEFAULT 2.00 CHECK (acrescimo_taxa_chuva >= 0);
