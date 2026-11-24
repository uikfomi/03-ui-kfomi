[🏠 Início](../../../../../../../../README.md) / [⬆️ service](../README.md) / 📂 impl

# ⚙️ Implementações

Reúne implementações dos contratos da camada correspondente; DAOs usam JDBC e serviços concentram orquestração e regras.

Mantenha as responsabilidades desta camada separadas das demais e consulte os arquivos relacionados antes de alterações.

O checkout revalida tarifa e prazo de entrega na mesma transação da venda. O despacho vincula um parceiro ativo ao pedido.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [.gitkeep](.gitkeep) | Marcador que mantém a pasta no controle de versão; não contém dados de aplicação. |
| [AgendaServiceImpl.java](AgendaServiceImpl.java) | Valida e coordena cadastro, busca e alterações de contatos pela camada DAO. |
| [AutenticacaoServiceImpl.java](AutenticacaoServiceImpl.java) | Cria contas por e-mail em transação, gera hashes e atualiza apelido e dados de entrega. |
| [AvaliacaoServiceImpl.java](AvaliacaoServiceImpl.java) | Valida a entrega e coordena avaliações, upload e exclusão de fotos na moderação. |
| [ClubeServiceImpl.java](ClubeServiceImpl.java) | Coordena pagamento pré-pago, vigência e renovação do plano VIP único. |
| [ComandaServiceImpl.java](ComandaServiceImpl.java) | Orquestra estoque, validação do operador, cotação de pagamentos e fechamento atômico. |
| [GamificacaoServiceImpl.java](GamificacaoServiceImpl.java) | Controla cooldown, separa sorteios de demonstração e aplica validade e giro extra. |
| [PedidoVendaServiceImpl.java](PedidoVendaServiceImpl.java) | Revalida custo, descontos VIP, disponibilidade, expediente e preços antes de gravar a venda. |
| [ProdutoEstoqueServiceImpl.java](ProdutoEstoqueServiceImpl.java) | Gerencia disponibilidade, ajustes, lotes e perdas do estoque físico. |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |
| [RelatorioFinanceiroServiceImpl.java](RelatorioFinanceiroServiceImpl.java) | Calcula indicadores financeiros e gera o balanço CSV. |

## Árvore estrutural comentada

O ponto inicial é `src/main/java/com/salgaderia/app/service/impl/`.

```text
src/main/java/com/salgaderia/app/service/impl/  # Reúne implementações dos contratos da camada correspondente; DAOs usam JDBC e serviços concentram orquestração e regras.
├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
├── AgendaServiceImpl.java  # Valida e coordena cadastro, busca e alterações de contatos pela camada DAO.
├── AutenticacaoServiceImpl.java  # Cria contas por e-mail em transação, gera hashes e atualiza apelido e dados de entrega.
├── AvaliacaoServiceImpl.java  # Valida a entrega e coordena avaliações, upload e exclusão de fotos na moderação.
├── ClubeServiceImpl.java  # Coordena pagamento pré-pago, vigência e renovação do plano VIP único.
├── ComandaServiceImpl.java  # Orquestra estoque, validação do operador, cotação de pagamentos e fechamento atômico.
├── GamificacaoServiceImpl.java  # Controla cooldown, separa sorteios de demonstração e aplica validade e giro extra.
├── PedidoVendaServiceImpl.java  # Revalida custo, descontos VIP, disponibilidade, expediente e preços antes de gravar a venda.
├── ProdutoEstoqueServiceImpl.java  # Gerencia disponibilidade, ajustes, lotes e perdas do estoque físico.
├── README.md  # Guia de navegação e explicações desta pasta.
└── RelatorioFinanceiroServiceImpl.java  # Calcula indicadores financeiros e gera o balanço CSV.
```

[🏠 Voltar ao README principal](../../../../../../../../README.md)
