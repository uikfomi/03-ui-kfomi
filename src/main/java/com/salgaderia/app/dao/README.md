[🏠 Início](../../../../../../../README.md) / [⬆️ app](../README.md) / 📂 dao

# 🗃️ Contratos de persistência

Define operações de acesso a dados independentes da implementação JDBC.

Mantenha as responsabilidades desta camada separadas das demais e consulte os arquivos relacionados antes de alterações.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [impl/](impl/) | Implementações JDBC dos contratos DAO, com comandos parametrizados. [Abrir guia](impl/README.md). |
| [.gitkeep](.gitkeep) | Marcador que mantém a pasta no controle de versão; não contém dados de aplicação. |
| [AvaliacaoDAO.java](AvaliacaoDAO.java) | Contrato JDBC para avaliações, médias, galeria de fotos e moderação. |
| [CategoriaDAO.java](CategoriaDAO.java) | Contrato para cadastrar, atualizar, buscar, listar e excluir categorias. |
| [ClubeDAO.java](ClubeDAO.java) | Define consultas do plano VIP, vigências e pagamentos de mensalidade. |
| [ComandaDAO.java](ComandaDAO.java) | Contrato JDBC para mesas, itens, pagamentos e fechamento transacional. |
| [ConfiguracaoLojaDAO.java](ConfiguracaoLojaDAO.java) | Define leitura e gravação da configuração operacional única da loja. |
| [ContatoDAO.java](ContatoDAO.java) | Contrato para cadastro, edição, exclusão e busca filtrada de contatos. |
| [CupomDAO.java](CupomDAO.java) | Contrato de consulta, reserva e devolução de cupons com uso limitado. |
| [GamificacaoDAO.java](GamificacaoDAO.java) | Contrato para selos, elegibilidade por pagamento, prêmios, prazos e pontos. |
| [MovimentacaoEstoqueDAO.java](MovimentacaoEstoqueDAO.java) | Contrato para registrar movimentações e calcular perdas por período. |
| [PedidoDAO.java](PedidoDAO.java) | Define operações transacionais de pedidos, despacho, relatórios e baixa física. |
| [PixPagamentoDAO.java](PixPagamentoDAO.java) | Contrato de persistência de cobranças, conciliação e status de pagamento. |
| [ProdutoDAO.java](ProdutoDAO.java) | Define cadastro, consulta, bloqueio de venda e atualização de estoque físico. |
| [ProdutoOpcionalDAO.java](ProdutoOpcionalDAO.java) | Define consulta e gravação dos opcionais do catálogo. |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |
| [UsuarioDAO.java](UsuarioDAO.java) | Define a persistência da identidade, credenciais e apelido das contas. |

## Árvore estrutural comentada

O ponto inicial é `src/main/java/com/salgaderia/app/dao/`.

```text
src/main/java/com/salgaderia/app/dao/  # Define operações de acesso a dados independentes da implementação JDBC.
├── impl/  # Implementações JDBC dos contratos DAO, com comandos parametrizados.
│   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── AvaliacaoDAOImpl.java  # Persiste avaliações e fotos e consulta a galeria e dados de moderação.
│   ├── CategoriaDAOImpl.java  # Operações JDBC de cadastro e consulta de categorias.
│   ├── ClubeDAOImpl.java  # Persiste assinaturas VIP, vigências e referências das mensalidades atuais e antigas.
│   ├── ComandaDAOImpl.java  # Implementa operações JDBC de mesas e comandas com bloqueio de linha.
│   ├── ConfiguracaoLojaDAOImpl.java  # Persiste expediente e estado manual separadamente com JDBC.
│   ├── ContatoDAOImpl.java  # Persiste contatos e atualiza dados do cliente sem sobrescrever os demais campos da agenda.
│   ├── CupomDAOImpl.java  # Consulta elegibilidade, reserva cupons exclusivos e os devolve no cancelamento.
│   ├── GamificacaoDAOImpl.java  # Persiste selos, giros, resgates e trava de 24 horas usando o relógio do banco.
│   ├── MovimentacaoEstoqueDAOImpl.java  # Grava movimentações de estoque e consulta perdas por período via JDBC.
│   ├── PedidoDAOImpl.java  # Persiste preços, exclusões da caixa, baixa física e despacho; consulta a fila e o histórico.
│   ├── PixPagamentoDAOImpl.java  # Consulta pagamentos e vincula tentativas, IDs remotos e aprovação ao pedido em JDBC.
│   ├── ProdutoDAOImpl.java  # Persiste catálogo, disponibilidade de venda, estoque e fichas de personalização.
│   ├── ProdutoOpcionalDAOImpl.java  # Consulta e sincroniza os opcionais de cada produto via JDBC.
│   ├── README.md  # Guia de navegação e explicações desta pasta.
│   └── UsuarioDAOImpl.java  # Persiste login, e-mail e apelido e bloqueia a conta ativa nas transações.
├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
├── AvaliacaoDAO.java  # Contrato JDBC para avaliações, médias, galeria de fotos e moderação.
├── CategoriaDAO.java  # Contrato para cadastrar, atualizar, buscar, listar e excluir categorias.
├── ClubeDAO.java  # Define consultas do plano VIP, vigências e pagamentos de mensalidade.
├── ComandaDAO.java  # Contrato JDBC para mesas, itens, pagamentos e fechamento transacional.
├── ConfiguracaoLojaDAO.java  # Define leitura e gravação da configuração operacional única da loja.
├── ContatoDAO.java  # Contrato para cadastro, edição, exclusão e busca filtrada de contatos.
├── CupomDAO.java  # Contrato de consulta, reserva e devolução de cupons com uso limitado.
├── GamificacaoDAO.java  # Contrato para selos, elegibilidade por pagamento, prêmios, prazos e pontos.
├── MovimentacaoEstoqueDAO.java  # Contrato para registrar movimentações e calcular perdas por período.
├── PedidoDAO.java  # Define operações transacionais de pedidos, despacho, relatórios e baixa física.
├── PixPagamentoDAO.java  # Contrato de persistência de cobranças, conciliação e status de pagamento.
├── ProdutoDAO.java  # Define cadastro, consulta, bloqueio de venda e atualização de estoque físico.
├── ProdutoOpcionalDAO.java  # Define consulta e gravação dos opcionais do catálogo.
├── README.md  # Guia de navegação e explicações desta pasta.
└── UsuarioDAO.java  # Define a persistência da identidade, credenciais e apelido das contas.
```

[🏠 Voltar ao README principal](../../../../../../../README.md)
