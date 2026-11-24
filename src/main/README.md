[🏠 Início](../../README.md) / [⬆️ src](../README.md) / 📂 main

# 🚀 Aplicação principal

Contém o código executável e os recursos necessários à aplicação JavaFX.

Os arquivos Java e os recursos FXML/CSS são compilados e copiados pelo Maven em etapas distintas.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [java/](java/) | Pacotes de código Java. [Abrir guia](java/README.md). |
| [resources/](resources/) | Recursos institucionais ou recursos JavaFX, conforme a localização. [Abrir guia](resources/README.md). |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |

## Árvore estrutural comentada

O ponto inicial é `src/main/`.

```text
src/main/  # Contém o código executável e os recursos necessários à aplicação JavaFX.
├── java/  # Pacotes de código Java.
│   ├── com/  # Raiz de pacotes Java.
│   │   ├── salgaderia/  # Pacote da aplicação Salgaderia.
│   │   │   ├── app/  # Componentes da aplicação e suas camadas.
│   │   │   │   ├── controller/  # Controllers das telas e testes de interface, conforme a localização.
│   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   ├── AdminDashboardController.java  # Coordena PDV, expediente, despacho, catálogo e confirmação do Clube VIP.
│   │   │   │   │   ├── AgendaController.java  # Interface da agenda de clientes e fornecedores.
│   │   │   │   │   ├── ClienteVitrineController.java  # Coordena perfil, vitrine, combos, caixas por orçamento, checkout e Clube VIP.
│   │   │   │   │   ├── LoginController.java  # Autentica contas e coordena o cadastro com e-mail e senha.
│   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   └── SelecaoModulosController.java  # Seleção de módulos administrativos.
│   │   │   │   ├── dao/  # Contratos de acesso a dados ou testes JDBC.
│   │   │   │   │   ├── impl/  # Implementações JDBC dos contratos DAO, com comandos parametrizados.
│   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   ├── AvaliacaoDAOImpl.java  # Persiste avaliações e fotos e consulta a galeria e dados de moderação.
│   │   │   │   │   │   ├── CategoriaDAOImpl.java  # Operações JDBC de cadastro e consulta de categorias.
│   │   │   │   │   │   ├── ClubeDAOImpl.java  # Persiste assinaturas VIP, vigências e referências das mensalidades atuais e antigas.
│   │   │   │   │   │   ├── ComandaDAOImpl.java  # Implementa operações JDBC de mesas e comandas com bloqueio de linha.
│   │   │   │   │   │   ├── ConfiguracaoLojaDAOImpl.java  # Persiste expediente e estado manual separadamente com JDBC.
│   │   │   │   │   │   ├── LogisticaDAO.java  # Persiste bairros, entregadores e repasses via JDBC.
│   │   │   │   │   │   ├── ContatoDAOImpl.java  # Persiste contatos e atualiza dados do cliente sem sobrescrever os demais campos da agenda.
│   │   │   │   │   │   ├── CupomDAOImpl.java  # Consulta elegibilidade, reserva cupons exclusivos e os devolve no cancelamento.
│   │   │   │   │   │   ├── GamificacaoDAOImpl.java  # Persiste selos, giros, resgates e trava de 24 horas usando o relógio do banco.
│   │   │   │   │   │   ├── MovimentacaoEstoqueDAOImpl.java  # Grava movimentações de estoque e consulta perdas por período via JDBC.
│   │   │   │   │   │   ├── PedidoDAOImpl.java  # Persiste preços, exclusões da caixa, baixa física e despacho; consulta a fila e o histórico.
│   │   │   │   │   │   ├── PixPagamentoDAOImpl.java  # Consulta pagamentos e vincula tentativas, IDs remotos e aprovação ao pedido em JDBC.
│   │   │   │   │   │   ├── ProdutoDAOImpl.java  # Persiste catálogo, disponibilidade de venda, estoque e fichas de personalização.
│   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   └── UsuarioDAOImpl.java  # Persiste login, e-mail e apelido e bloqueia a conta ativa nas transações.
│   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   ├── AvaliacaoDAO.java  # Contrato JDBC para avaliações, médias, galeria de fotos e moderação.
│   │   │   │   │   ├── CategoriaDAO.java  # Contrato para cadastrar, atualizar, buscar, listar e excluir categorias.
│   │   │   │   │   ├── ClubeDAO.java  # Define consultas do plano VIP, vigências e pagamentos de mensalidade.
│   │   │   │   │   ├── ComandaDAO.java  # Contrato JDBC para mesas, itens, pagamentos e fechamento transacional.
│   │   │   │   │   ├── ConfiguracaoLojaDAO.java  # Define leitura e gravação da configuração operacional única da loja.
│   │   │   │   │   ├── ContatoDAO.java  # Contrato para cadastro, edição, exclusão e busca filtrada de contatos.
│   │   │   │   │   ├── CupomDAO.java  # Contrato de consulta, reserva e devolução de cupons com uso limitado.
│   │   │   │   │   ├── GamificacaoDAO.java  # Contrato para selos, elegibilidade por pagamento, prêmios, prazos e pontos.
│   │   │   │   │   ├── MovimentacaoEstoqueDAO.java  # Contrato para registrar movimentações e calcular perdas por período.
│   │   │   │   │   ├── PedidoDAO.java  # Define operações transacionais de pedidos, despacho, relatórios e baixa física.
│   │   │   │   │   ├── PixPagamentoDAO.java  # Contrato de persistência de cobranças, conciliação e status de pagamento.
│   │   │   │   │   ├── ProdutoDAO.java  # Define cadastro, consulta, bloqueio de venda e atualização de estoque físico.
│   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   └── UsuarioDAO.java  # Define a persistência da identidade, credenciais e apelido das contas.
│   │   │   │   ├── exception/  # Exceções personalizadas de domínio e persistência.
│   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   ├── AutenticacaoInvalidaException.java  # Falha de autenticação.
│   │   │   │   │   ├── AvaliacaoNaoPermitidaException.java  # Avaliação sem compra elegível.
│   │   │   │   │   ├── EstoqueInsuficienteException.java  # Indica saldo físico insuficiente para uma movimentação de estoque.
│   │   │   │   │   ├── PersistenciaException.java  # Encapsula erro de persistência.
│   │   │   │   │   ├── ProdutoIndisponivelException.java  # Indica que um produto foi bloqueado para venda, independentemente do estoque.
│   │   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   ├── model/  # Entidades de domínio ou testes dessas entidades.
│   │   │   │   │   ├── comanda/  # Entidades de mesa, comanda, itens e pagamentos parciais.
│   │   │   │   │   │   ├── Comanda.java  # Sessão da mesa e cálculo monetário de total, pagamentos e saldo.
│   │   │   │   │   │   ├── ItemComanda.java  # Item lançado na comanda, com quantidade disponível e cotação por unidade.
│   │   │   │   │   │   ├── Mesa.java  # Identificação, número e status da mesa.
│   │   │   │   │   │   ├── PagamentoComanda.java  # Dados de pagamento parcial por valor ou item.
│   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   └── StatusMesa.java  # Estados livre e ocupada e suas permissões.
│   │   │   │   │   ├── enums/  # Enumerações com comportamento e respectivos testes.
│   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   ├── CategoriaInsumo.java  # Categorias de insumos de fornecedores.
│   │   │   │   │   │   ├── FormaPagamento.java  # Formas de pagamento e regra de troco.
│   │   │   │   │   │   ├── PerfilUsuario.java  # Perfis de acesso do usuário.
│   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   ├── StatusPagamento.java  # Distingue PIX pendente, pagamento confirmado e cobrança na entrega.
│   │   │   │   │   │   ├── StatusPedido.java  # Define as transições por modalidade, incluindo saída exclusiva para entrega.
│   │   │   │   │   │   ├── TipoEntrega.java  # Modalidades de entrega.
│   │   │   │   │   │   └── TipoMovimentacaoEstoque.java  # Tipos de movimentação de estoque.
│   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   ├── AssinaturaCliente.java  # Representa o plano do cliente, o desconto vigente e o estado da assinatura.
│   │   │   │   │   ├── Avaliacao.java  # Registra nota, comentário, resposta oficial e referência opcional da foto.
│   │   │   │   │   ├── AvaliacaoFoto.java  # Projeção pública de fotos de avaliações que não expõe dados da conta.
│   │   │   │   │   ├── AvaliacaoResumo.java  # Projeção de avaliação com comentário, resposta oficial e foto para moderação.
│   │   │   │   │   ├── Categoria.java  # Categoria de produtos.
│   │   │   │   │   ├── Cliente.java  # Contato especializado em cliente.
│   │   │   │   │   ├── ConfiguracaoLoja.java  # Representa o expediente, o modo automático e a chave manual da loja.
│   │   │   │   │   ├── BairroEntrega.java  # Representa uma região atendida com taxa e prazo.
│   │   │   │   │   ├── Entregador.java  # Representa um parceiro disponível ou inativo.
│   │   │   │   │   ├── ConquistaCliente.java  # Representa um selo e a data em que foi desbloqueado.
│   │   │   │   │   ├── Contato.java  # Abstração comum de contatos.
│   │   │   │   │   ├── CupomRoleta.java  # Projeção de código de resgate com benefício, mínimo e data de expiração.
│   │   │   │   │   ├── FichaVitrine.java  # Preserva campos legados do catálogo e identifica categorias de salgados.
│   │   │   │   │   ├── Fornecedor.java  # Contato especializado em fornecedor.
│   │   │   │   │   ├── MensalidadeClube.java  # Cotação imutável de valor e desconto de uma mensalidade pendente.
│   │   │   │   │   ├── MovimentacaoEstoque.java  # Registro de entrada ou saída de estoque.
│   │   │   │   │   ├── Pedido.java  # Mantém itens, total, frete, pagamento, status e liberação da roleta.
│   │   │   │   │   ├── PedidoFila.java  # Projeção de preparo com pagamento, modalidade, itens, entrega e agendamento.
│   │   │   │   │   ├── PedidoItem.java  # Registra produto, quantidade, preço histórico e eventual personalização do item.
│   │   │   │   │   ├── RepasseEntregador.java  # Agrupa entregas e taxas cobradas por parceiro.
│   │   │   │   │   ├── PersonalizacaoItem.java  # Identifica o grupo, a faixa de orçamento e as exclusões da caixa; preserva os tipos antigos.
│   │   │   │   │   ├── PlanoAssinatura.java  # Modela os termos de um plano e define o nome, a mensalidade e o desconto do VIP.
│   │   │   │   │   ├── PremioRoleta.java  # Define fatias comuns/VIP e registra resultado, prazo e giro extra.
│   │   │   │   │   ├── Produto.java  # Representa preço, custos, saldo físico, disponibilidade de venda e ficha do catálogo.
│   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   ├── Usuario.java  # Abstração de usuário e permissões.
│   │   │   │   │   ├── UsuarioAdmin.java  # Usuário administrativo.
│   │   │   │   │   ├── UsuarioCliente.java  # Representa a conta de cliente vinculada ao contato, com e-mail para novos cadastros.
│   │   │   │   │   └── Validacao.java  # Validações compartilhadas das entidades.
│   │   │   │   ├── service/  # Contratos, regras de negócio e testes de serviços.
│   │   │   │   │   ├── cozinha/  # Estados do termômetro visual de preparo dos pedidos.
│   │   │   │   │   │   ├── EstadoCozinha.java  # Contrato State de mensagem, progresso e espera.
│   │   │   │   │   │   ├── EtapaCozinha.java  # Mapeia os status aos estados visuais de preparo, saída em rota e conclusão.
│   │   │   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   ├── desconto/  # Estratégias de desconto e testes correspondentes.
│   │   │   │   │   │   ├── CalculadoraDescontos.java  # Mantém caixas no preço fechado, isenta frete VIP e escolhe o melhor benefício elegível.
│   │   │   │   │   │   ├── ContextoDesconto.java  # Agrupa itens, cupom, frete e fidelidade e separa mercadorias elegíveis para descontos.
│   │   │   │   │   │   ├── Cupom.java  # Representa percentual, frete, brinde ou bônus vinculado ao cupom.
│   │   │   │   │   │   ├── DescontoAssinante.java  # Calcula o desconto do membro VIP nos itens elegíveis.
│   │   │   │   │   │   ├── DescontoCaixaMisteriosa.java  # Ajusta o valor de catálogo de cada caixa ao preço fechado da faixa escolhida.
│   │   │   │   │   │   ├── DescontoCentoSalgados.java  # Estratégia de desconto para cento de salgados.
│   │   │   │   │   │   ├── DescontoCombo.java  # Calcula 10% de desconto sobre os itens do combo simples.
│   │   │   │   │   │   ├── DescontoCupomPercentual.java  # Estratégia percentual por cupom.
│   │   │   │   │   │   ├── DescontoFidelidade.java  # Estratégia para clientes fiéis.
│   │   │   │   │   │   ├── EstrategiaDesconto.java  # Contrato Strategy para descontos.
│   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   └── ResultadoDesconto.java  # Informa total, desconto, frete efetivo, isenção VIP e eventual brinde.
│   │   │   │   │   ├── impl/  # Implementações JDBC dos contratos DAO, com comandos parametrizados.
│   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   ├── AgendaServiceImpl.java  # Valida e coordena cadastro, busca e alterações de contatos pela camada DAO.
│   │   │   │   │   │   ├── AutenticacaoServiceImpl.java  # Cria contas por e-mail em transação, gera hashes e atualiza apelido e dados de entrega.
│   │   │   │   │   │   ├── AvaliacaoServiceImpl.java  # Valida a entrega e coordena avaliações, upload e exclusão de fotos na moderação.
│   │   │   │   │   │   ├── ClubeServiceImpl.java  # Coordena pagamento pré-pago, vigência e renovação do plano VIP único.
│   │   │   │   │   │   ├── ComandaServiceImpl.java  # Orquestra estoque, validação do operador, cotação de pagamentos e fechamento atômico.
│   │   │   │   │   │   ├── GamificacaoServiceImpl.java  # Controla cooldown, separa sorteios de demonstração e aplica validade e giro extra.
│   │   │   │   │   │   ├── PedidoVendaServiceImpl.java  # Revalida custo, descontos VIP, disponibilidade, expediente e preços antes de gravar a venda.
│   │   │   │   │   │   ├── ProdutoEstoqueServiceImpl.java  # Gerencia disponibilidade, ajustes, lotes e perdas do estoque físico.
│   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   └── RelatorioFinanceiroServiceImpl.java  # Calcula indicadores financeiros e gera o balanço CSV.
│   │   │   │   │   ├── pix/  # Porta, adapters e conciliação da cobrança PIX na vitrine.
│   │   │   │   │   │   ├── LocalDemoPixGateway.java  # Gera um BR Code de demonstração sem pagamento ou confirmação externa.
│   │   │   │   │   │   ├── MercadoPagoPixGateway.java  # Traduz cobranças para JSON e usa HttpClient para chamar a API Payments com idempotência.
│   │   │   │   │   │   ├── PixConfiguracao.java  # Lê o token local do Mercado Pago.
│   │   │   │   │   │   ├── PixGatewayService.java  # Porta de criação e consulta das cobranças PIX do checkout.
│   │   │   │   │   │   ├── PixPagamentoService.java  # Concilia a resposta do gateway com a cobrança local, sem aprovar valores divergentes.
│   │   │   │   │   │   └── README.md  # Guia da integração PIX da vitrine.
│   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   ├── AgendaService.java  # Contrato de operações e busca da agenda de clientes e fornecedores.
│   │   │   │   │   ├── AutenticacaoService.java  # Define login, cadastro por e-mail e edição dos dados de perfil do cliente.
│   │   │   │   │   ├── AvaliacaoService.java  # Contrato para avaliações, médias, fotos e moderação administrativa.
│   │   │   │   │   ├── BarramentoPedido.java  # Notifica observadores na mesma transação JDBC do evento de pedido.
│   │   │   │   │   ├── ClubeService.java  # Define adesão ao plano VIP, renovação, cancelamento e confirmação da mensalidade.
│   │   │   │   │   ├── ComandaService.java  # Contrato para abertura, lançamento, rateio e pagamentos de comandas.
│   │   │   │   │   ├── ComboBuilder.java  # Monta um combo com principal, acompanhamento distinto e bebida do cardápio.
│   │   │   │   │   ├── ConquistasObserver.java  # Avalia selos, libera a roleta após pagamento ou entrega e revoga prêmios cancelados.
│   │   │   │   │   ├── FotoAvaliacaoStorage.java  # Valida, redimensiona, salva e remove imagens locais de avaliações.
│   │   │   │   │   ├── GamificacaoService.java  # Contrato de selos, pontos, cupons, giro regular e giro extra.
│   │   │   │   │   ├── OperacaoLojaService.java  # Avalia a abertura e valida agendamentos pelo expediente local, inclusive após a meia-noite.
│   │   │   │   │   ├── LogisticaService.java  # Valida cadastros e calcula taxa e prazo por bairro.
│   │   │   │   │   ├── PedidoCancelamentoService.java  # Devolve somente o saldo físico baixado e libera o cupom ao cancelar.
│   │   │   │   │   ├── PedidoConcluidoObserver.java  # Contrato Observer para finalização, confirmação PIX, entrega e cancelamento.
│   │   │   │   │   ├── PedidoVendaService.java  # Contrato para simular descontos, finalizar pedidos e alterar status de vendas.
│   │   │   │   │   ├── PersonalizacaoService.java  # Sorteia e revalida caixas por preço, custo e exclusões; valida a composição do combo.
│   │   │   │   │   ├── PixPayloadService.java  # Monta BR Code com CRC16 e gera a imagem Pix em memória com ZXing.
│   │   │   │   │   ├── ProdutoEstoqueService.java  # Define operações do catálogo, bloqueio de venda e movimentações do estoque físico.
│   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   ├── RelatorioFinanceiroService.java  # Contrato de cálculos de balanço, gráficos e exportação CSV.
│   │   │   │   │   └── WhatsAppLinkService.java  # Gera links do WhatsApp e os abre e copia em segundo plano.
│   │   │   │   ├── util/  # Conexão, autenticação auxiliar e navegação.
│   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   ├── ConnectionFactory.java  # Pool HikariCP e transações JDBC.
│   │   │   │   │   ├── FotoAvaliacaoUi.java  # Carrega miniaturas JavaFX de fotos de avaliações guardadas localmente.
│   │   │   │   │   ├── Navegacao.java  # Navegação entre telas FXML.
│   │   │   │   │   ├── PagamentoPixUi.java  # Mostra QR Code e consulta o pagamento com Timeline, sem bloquear a interface.
│   │   │   │   │   ├── PixPainel.java  # Desenha o QR local ou do gateway e disponibiliza o código PIX para cópia.
│   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   ├── RoletaUi.java  # Anima o prêmio transacional em seis fatias regulares ou VIP, com giro extra quando disponível.
│   │   │   │   │   ├── Senhas.java  # Geração e verificação de hashes de senha.
│   │   │   │   │   ├── TelaAutenticada.java  # Contrato para telas com usuário autenticado.
│   │   │   │   │   └── WhatsAppUi.java  # Mostra na JavaFX a alternativa quando o WhatsApp não abre.
│   │   │   │   ├── Main.java  # Inicialização da aplicação JavaFX.
│   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   └── README.md  # Guia de navegação e explicações desta pasta.
├── resources/  # Recursos institucionais ou recursos JavaFX, conforme a localização.
│   ├── css/  # Folhas de estilo das telas JavaFX.
│   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   ├── admin.css  # Estilos do painel administrativo.
│   │   ├── agenda.css  # Estilos da agenda.
│   │   ├── cliente-vitrine.css  # Estiliza catálogo, checkout, disponibilidade e banner de entrega em rota.
│   │   ├── global.css  # Estilos compartilhados da aplicação.
│   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   ├── fxml/  # Descrições de interface JavaFX.
│   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   ├── admin-dashboard.fxml  # Estrutura a navegação administrativa, incluindo expediente e confirmação do Clube VIP.
│   │   ├── agenda-contatos.fxml  # Estrutura visual da agenda.
│   │   ├── cliente-vitrine.fxml  # Estrutura vitrine, perfil, checkout, acompanhamento e Clube VIP.
│   │   ├── login.fxml  # Estrutura as telas de login e cadastro com e-mail e senha.
│   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   └── selecao-modulos.fxml  # Estrutura visual da seleção de módulos.
│   ├── icons/  # Espaço reservado para ícones.
│   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── images/  # Espaço reservado para imagens.
│   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── database.properties  # Template JDBC e propriedades opcionais de pagamento PIX.
│   └── README.md  # Guia de navegação e explicações desta pasta.
└── README.md  # Guia de navegação e explicações desta pasta.
```

## 📌 Status do Conteúdo

As pastas abaixo estão reservadas e aguardam materiais finais; os arquivos `.gitkeep` apenas preservam a estrutura:

- `src/main/resources/icons/` — Espaço reservado para ícones.
- `src/main/resources/images/` — Espaço reservado para imagens.

[🏠 Voltar ao README principal](../../README.md)
