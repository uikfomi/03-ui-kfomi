[🏠 Início](../../../../../../../README.md) / [⬆️ app](../README.md) / 📂 model

# 🧱 Modelo de domínio

Agrupa entidades, tipos de domínio e comportamentos encapsulados do sistema.

Mantenha as responsabilidades desta camada separadas das demais e consulte os arquivos relacionados antes de alterações.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [comanda/](comanda/) | Entidades de mesa, comanda, itens e pagamentos parciais. [Abrir guia](comanda/README.md). |
| [enums/](enums/) | Enumerações com comportamento e respectivos testes. [Abrir guia](enums/README.md). |
| [.gitkeep](.gitkeep) | Marcador que mantém a pasta no controle de versão; não contém dados de aplicação. |
| [AssinaturaCliente.java](AssinaturaCliente.java) | Representa o plano do cliente, o desconto vigente e o estado da assinatura. |
| [Avaliacao.java](Avaliacao.java) | Registra nota, comentário, resposta oficial e referência opcional da foto. |
| [AvaliacaoFoto.java](AvaliacaoFoto.java) | Projeção pública de fotos de avaliações que não expõe dados da conta. |
| [AvaliacaoResumo.java](AvaliacaoResumo.java) | Projeção de avaliação com comentário, resposta oficial e foto para moderação. |
| [Categoria.java](Categoria.java) | Categoria de produtos. |
| [Cliente.java](Cliente.java) | Contato especializado em cliente. |
| [ConfiguracaoLoja.java](ConfiguracaoLoja.java) | Representa expediente, modo automático, chave manual e mínimo para entrega. |
| [BairroEntrega.java](BairroEntrega.java) | Representa uma região atendida com taxa e prazo. |
| [Entregador.java](Entregador.java) | Representa um parceiro disponível ou inativo. |
| [ConquistaCliente.java](ConquistaCliente.java) | Representa um selo e a data em que foi desbloqueado. |
| [Contato.java](Contato.java) | Abstração comum de contatos. |
| [CupomRoleta.java](CupomRoleta.java) | Projeção de código de resgate com benefício, mínimo e data de expiração. |
| [FichaVitrine.java](FichaVitrine.java) | Preserva campos legados do catálogo e identifica categorias de salgados. |
| [Fornecedor.java](Fornecedor.java) | Contato especializado em fornecedor. |
| [MensalidadeClube.java](MensalidadeClube.java) | Cotação imutável de valor e desconto de uma mensalidade pendente. |
| [MovimentacaoEstoque.java](MovimentacaoEstoque.java) | Registro de entrada ou saída de estoque. |
| [Pedido.java](Pedido.java) | Mantém itens, total, frete, pagamento, status e liberação da roleta. |
| [PedidoFila.java](PedidoFila.java) | Projeção da fila com modalidade, pagamento e instruções de preparo. |
| [PedidoItem.java](PedidoItem.java) | Registra quantidade, preço histórico, opcionais e observações do item. |
| [RepasseEntregador.java](RepasseEntregador.java) | Agrupa entregas e taxas cobradas por parceiro. |
| [PersonalizacaoItem.java](PersonalizacaoItem.java) | Identifica o grupo, a faixa de orçamento e as exclusões da caixa; preserva os tipos antigos. |
| [PlanoAssinatura.java](PlanoAssinatura.java) | Modela os termos de um plano e define o nome, a mensalidade e o desconto do VIP. |
| [PremioRoleta.java](PremioRoleta.java) | Define fatias comuns/VIP e registra resultado, prazo e giro extra. |
| [Produto.java](Produto.java) | Representa preço, custos, saldo físico, disponibilidade de venda e ficha do catálogo. |
| [ProdutoOpcional.java](ProdutoOpcional.java) | Representa uma borda ou adicional e suas regras de preço e quantidade. |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |
| [Usuario.java](Usuario.java) | Abstração de usuário e permissões. |
| [UsuarioAdmin.java](UsuarioAdmin.java) | Usuário administrativo. |
| [UsuarioCliente.java](UsuarioCliente.java) | Representa a conta de cliente vinculada ao contato, com e-mail para novos cadastros. |
| [Validacao.java](Validacao.java) | Validações compartilhadas das entidades. |

## Árvore estrutural comentada

O ponto inicial é `src/main/java/com/salgaderia/app/model/`.

```text
src/main/java/com/salgaderia/app/model/  # Agrupa entidades, tipos de domínio e comportamentos encapsulados do sistema.
├── comanda/  # Entidades de mesa, comanda, itens e pagamentos parciais.
│   ├── Comanda.java  # Sessão da mesa e cálculo monetário de total, pagamentos e saldo.
│   ├── ItemComanda.java  # Item lançado na comanda, com quantidade disponível e cotação por unidade.
│   ├── Mesa.java  # Identificação, número e status da mesa.
│   ├── PagamentoComanda.java  # Dados de pagamento parcial por valor ou item.
│   ├── README.md  # Guia de navegação e explicações desta pasta.
│   └── StatusMesa.java  # Estados livre e ocupada e suas permissões.
├── enums/  # Enumerações com comportamento e respectivos testes.
│   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── CategoriaInsumo.java  # Categorias de insumos de fornecedores.
│   ├── FormaPagamento.java  # Formas de pagamento e regra de troco.
│   ├── PerfilUsuario.java  # Perfis de acesso do usuário.
│   ├── README.md  # Guia de navegação e explicações desta pasta.
│   ├── StatusPagamento.java  # Distingue PIX pendente, pagamento confirmado e cobrança na entrega.
│   ├── StatusPedido.java  # Define as transições por modalidade, incluindo saída exclusiva para entrega.
│   ├── TipoEntrega.java  # Modalidades de entrega.
│   └── TipoMovimentacaoEstoque.java  # Tipos de movimentação de estoque.
├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
├── AssinaturaCliente.java  # Representa o plano do cliente, o desconto vigente e o estado da assinatura.
├── Avaliacao.java  # Registra nota, comentário, resposta oficial e referência opcional da foto.
├── AvaliacaoFoto.java  # Projeção pública de fotos de avaliações que não expõe dados da conta.
├── AvaliacaoResumo.java  # Projeção de avaliação com comentário, resposta oficial e foto para moderação.
├── Categoria.java  # Categoria de produtos.
├── Cliente.java  # Contato especializado em cliente.
├── ConfiguracaoLoja.java  # Representa expediente, modo automático, chave manual e mínimo para entrega.
├── BairroEntrega.java  # Representa uma região atendida com taxa e prazo.
├── Entregador.java  # Representa um parceiro disponível ou inativo.
├── ConquistaCliente.java  # Representa um selo e a data em que foi desbloqueado.
├── Contato.java  # Abstração comum de contatos.
├── CupomRoleta.java  # Projeção de código de resgate com benefício, mínimo e data de expiração.
├── FichaVitrine.java  # Preserva campos legados do catálogo e identifica categorias de salgados.
├── Fornecedor.java  # Contato especializado em fornecedor.
├── MensalidadeClube.java  # Cotação imutável de valor e desconto de uma mensalidade pendente.
├── MovimentacaoEstoque.java  # Registro de entrada ou saída de estoque.
├── Pedido.java  # Mantém itens, total, frete, pagamento, status e liberação da roleta.
├── PedidoFila.java  # Projeção da fila com modalidade, pagamento e instruções de preparo.
├── PedidoItem.java  # Registra quantidade, preço histórico, opcionais e observações do item.
├── RepasseEntregador.java  # Agrupa entregas e taxas cobradas por parceiro.
├── PersonalizacaoItem.java  # Identifica o grupo, a faixa de orçamento e as exclusões da caixa; preserva os tipos antigos.
├── PlanoAssinatura.java  # Modela os termos de um plano e define o nome, a mensalidade e o desconto do VIP.
├── PremioRoleta.java  # Define fatias comuns/VIP e registra resultado, prazo e giro extra.
├── Produto.java  # Representa preço, custos, saldo físico, disponibilidade de venda e ficha do catálogo.
├── ProdutoOpcional.java  # Representa uma borda ou adicional e suas regras de preço e quantidade.
├── README.md  # Guia de navegação e explicações desta pasta.
├── Usuario.java  # Abstração de usuário e permissões.
├── UsuarioAdmin.java  # Usuário administrativo.
├── UsuarioCliente.java  # Representa a conta de cliente vinculada ao contato, com e-mail para novos cadastros.
└── Validacao.java  # Validações compartilhadas das entidades.
```

[🏠 Voltar ao README principal](../../../../../../../README.md)
