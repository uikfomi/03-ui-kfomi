[🏠 Início](../../README.md) / [⬆️ database](../README.md) / 📂 scripts

# 📜 Scripts de banco

Contém o schema e os scripts SQL de evolução da persistência.

Revise o conteúdo e a ordem das migrações; instruções ALTER podem não ser idempotentes e devem ser aplicadas após backup.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [etapa10_pagamento_vitrine.sql](etapa10_pagamento_vitrine.sql) | Amplia forma de pagamento para cartão genérico. |
| [etapa11_salao_agendamento.sql](etapa11_salao_agendamento.sql) | Migração de agendamento, mesas, comandas e pagamentos parciais. |
| [etapa12_combo_avaliacoes.sql](etapa12_combo_avaliacoes.sql) | Acrescenta a ficha do catálogo, personalização dos itens e caminhos das fotos de avaliações. |
| [etapa13_clube_gamificacao.sql](etapa13_clube_gamificacao.sql) | Cria planos, assinaturas, mensalidades, conquistas e histórico da roleta; habilita cupons exclusivos. |
| [etapa14_pix_roleta_vip.sql](etapa14_pix_roleta_vip.sql) | Migra estados de pagamento, referências PIX, cupons com validade e prêmios VIP. |
| [etapa15_operacao_loja.sql](etapa15_operacao_loja.sql) | Adiciona expediente da loja, bloqueio de venda, baixa física e saída para entrega. |
| [etapa16_cadastro_caixa_clube_vip.sql](etapa16_cadastro_caixa_clube_vip.sql) | Migra o cadastro por e-mail, os filtros da caixa e o plano VIP único. |
| [etapa17_personalizacao_entrega.sql](etapa17_personalizacao_entrega.sql) | Cria opcionais e registra categorias ativas, instruções do pedido e mínimo de entrega. |
| [etapa18_logistica_entregadores_chuva.sql](etapa18_logistica_entregadores_chuva.sql) | Adiciona bairros, entregadores, prazos e configuração de chuva/pico. |
| [etapa4_migracao.sql](etapa4_migracao.sql) | Migração de contatos, usuários e custos históricos. |
| [etapa8_pdv.sql](etapa8_pdv.sql) | Colunas de origem e pagamento do PDV. |
| [etapa9_descontos.sql](etapa9_descontos.sql) | Cupons e registro de descontos por pedido. |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |
| [schema.sql](schema.sql) | Schema inicial MySQL usado pelo respectivo repositório. |

## Árvore estrutural comentada

O ponto inicial é `database/scripts/`.

```text
database/scripts/  # Contém o schema e os scripts SQL de evolução da persistência.
├── etapa10_pagamento_vitrine.sql  # Amplia forma de pagamento para cartão genérico.
├── etapa11_salao_agendamento.sql  # Migração de agendamento, mesas, comandas e pagamentos parciais.
├── etapa12_combo_avaliacoes.sql  # Acrescenta a ficha do catálogo, personalização dos itens e caminhos das fotos de avaliações.
├── etapa13_clube_gamificacao.sql  # Cria planos, assinaturas, mensalidades, conquistas e histórico da roleta; habilita cupons exclusivos.
├── etapa14_pix_roleta_vip.sql  # Migra estados de pagamento, referências PIX, cupons com validade e prêmios VIP.
├── etapa15_operacao_loja.sql  # Adiciona expediente da loja, bloqueio de venda, baixa física e saída para entrega.
├── etapa16_cadastro_caixa_clube_vip.sql  # Migra o cadastro por e-mail, os filtros da caixa e o plano VIP único.
├── etapa17_personalizacao_entrega.sql  # Cria opcionais e registra categorias ativas, instruções do pedido e mínimo de entrega.
├── etapa18_logistica_entregadores_chuva.sql  # Adiciona bairros, entregadores, prazos e configuração de chuva/pico.
├── etapa4_migracao.sql  # Migração de contatos, usuários e custos históricos.
├── etapa8_pdv.sql  # Colunas de origem e pagamento do PDV.
├── etapa9_descontos.sql  # Cupons e registro de descontos por pedido.
├── README.md  # Guia de navegação e explicações desta pasta.
└── schema.sql  # Schema inicial MySQL usado pelo respectivo repositório.
```

[🏠 Voltar ao README principal](../../README.md)
