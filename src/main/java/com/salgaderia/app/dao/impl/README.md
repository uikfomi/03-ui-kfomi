[🏠 Início](../../../../../../../../README.md) / [⬆️ dao](../README.md) / 📂 impl

# ⚙️ Implementações

Reúne implementações dos contratos da camada correspondente; DAOs usam JDBC e serviços concentram orquestração e regras.

Mantenha as responsabilidades desta camada separadas das demais e consulte os arquivos relacionados antes de alterações.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [.gitkeep](.gitkeep) | Marcador que mantém a pasta no controle de versão; não contém dados de aplicação. |
| [AvaliacaoDAOImpl.java](AvaliacaoDAOImpl.java) | Persiste avaliações e fotos e consulta a galeria e dados de moderação. |
| [CategoriaDAOImpl.java](CategoriaDAOImpl.java) | Operações JDBC de cadastro e consulta de categorias. |
| [ClubeDAOImpl.java](ClubeDAOImpl.java) | Persiste assinaturas VIP, vigências e referências das mensalidades atuais e antigas. |
| [ComandaDAOImpl.java](ComandaDAOImpl.java) | Implementa operações JDBC de mesas e comandas com bloqueio de linha. |
| [ConfiguracaoLojaDAOImpl.java](ConfiguracaoLojaDAOImpl.java) | Persiste expediente e estado manual separadamente com JDBC. |
| [ContatoDAOImpl.java](ContatoDAOImpl.java) | Persiste contatos e atualiza dados do cliente sem sobrescrever os demais campos da agenda. |
| [CupomDAOImpl.java](CupomDAOImpl.java) | Consulta elegibilidade, reserva cupons exclusivos e os devolve no cancelamento. |
| [GamificacaoDAOImpl.java](GamificacaoDAOImpl.java) | Persiste selos, giros, resgates e trava de 24 horas usando o relógio do banco. |
| [MovimentacaoEstoqueDAOImpl.java](MovimentacaoEstoqueDAOImpl.java) | Grava movimentações de estoque e consulta perdas por período via JDBC. |
| [PedidoDAOImpl.java](PedidoDAOImpl.java) | Persiste preços, exclusões da caixa, baixa física e despacho; consulta a fila e o histórico. |
| [PixPagamentoDAOImpl.java](PixPagamentoDAOImpl.java) | Consulta pagamentos e vincula tentativas, IDs remotos e aprovação ao pedido em JDBC. |
| [ProdutoDAOImpl.java](ProdutoDAOImpl.java) | Persiste catálogo, disponibilidade de venda, estoque e fichas de personalização. |
| [ProdutoOpcionalDAOImpl.java](ProdutoOpcionalDAOImpl.java) | Consulta e sincroniza os opcionais de cada produto via JDBC. |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |
| [UsuarioDAOImpl.java](UsuarioDAOImpl.java) | Persiste login, e-mail e apelido e bloqueia a conta ativa nas transações. |

## Árvore estrutural comentada

O ponto inicial é `src/main/java/com/salgaderia/app/dao/impl/`.

```text
src/main/java/com/salgaderia/app/dao/impl/  # Reúne implementações dos contratos da camada correspondente; DAOs usam JDBC e serviços concentram orquestração e regras.
├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
├── AvaliacaoDAOImpl.java  # Persiste avaliações e fotos e consulta a galeria e dados de moderação.
├── CategoriaDAOImpl.java  # Operações JDBC de cadastro e consulta de categorias.
├── ClubeDAOImpl.java  # Persiste assinaturas VIP, vigências e referências das mensalidades atuais e antigas.
├── ComandaDAOImpl.java  # Implementa operações JDBC de mesas e comandas com bloqueio de linha.
├── ConfiguracaoLojaDAOImpl.java  # Persiste expediente e estado manual separadamente com JDBC.
├── ContatoDAOImpl.java  # Persiste contatos e atualiza dados do cliente sem sobrescrever os demais campos da agenda.
├── CupomDAOImpl.java  # Consulta elegibilidade, reserva cupons exclusivos e os devolve no cancelamento.
├── GamificacaoDAOImpl.java  # Persiste selos, giros, resgates e trava de 24 horas usando o relógio do banco.
├── MovimentacaoEstoqueDAOImpl.java  # Grava movimentações de estoque e consulta perdas por período via JDBC.
├── PedidoDAOImpl.java  # Persiste preços, exclusões da caixa, baixa física e despacho; consulta a fila e o histórico.
├── PixPagamentoDAOImpl.java  # Consulta pagamentos e vincula tentativas, IDs remotos e aprovação ao pedido em JDBC.
├── ProdutoDAOImpl.java  # Persiste catálogo, disponibilidade de venda, estoque e fichas de personalização.
├── ProdutoOpcionalDAOImpl.java  # Consulta e sincroniza os opcionais de cada produto via JDBC.
├── README.md  # Guia de navegação e explicações desta pasta.
└── UsuarioDAOImpl.java  # Persiste login, e-mail e apelido e bloqueia a conta ativa nas transações.
```

[🏠 Voltar ao README principal](../../../../../../../../README.md)
