[🏠 Início](../../../../../../../README.md) / [⬆️ app](../README.md) / 📂 service

# 🧪 service — testes

Reúne testes automatizados dos componentes presentes neste pacote.

Execute os testes compatíveis com o ambiente; os testes de integração podem exigir banco ou display.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [desconto/](desconto/) | Estratégias de desconto e testes correspondentes. |
| [pix/](pix/) | Testes HTTP em loopback para a integração PIX. [Abrir guia](pix/README.md). |
| [ClubeGamificacaoIntegracaoTest.java](ClubeGamificacaoIntegracaoTest.java) | Valida o VIP único, mensalidades antigas, frete, descontos, roleta e PIX no MySQL. |
| [ComandaIntegracaoTest.java](ComandaIntegracaoTest.java) | Valida operações de mesa, pagamentos concorrentes, baixa e fechamento no MySQL. |
| [CozinhaStateTest.java](CozinhaStateTest.java) | Verifica mensagens e progresso dos estados da cozinha. |
| [DescontoIntegracaoTest.java](DescontoIntegracaoTest.java) | Integração de descontos e pagamentos no MySQL. |
| [Etapa4IntegracaoTest.java](Etapa4IntegracaoTest.java) | Verifica cadastro por e-mail e transações de estoque, vendas, avaliações e financeiro no MySQL. |
| [FotoAvaliacaoStorageTest.java](FotoAvaliacaoStorageTest.java) | Testa a cópia validada, limites, caminhos locais e remoção de anexos. |
| [OperacaoLojaServiceTest.java](OperacaoLojaServiceTest.java) | Testa os limites do expediente e a combinação dos modos manual e automático. |
| [OperacaoV162IntegracaoTest.java](OperacaoV162IntegracaoTest.java) | Valida configuração persistida, agendamento, venda sob demanda, bloqueio e despacho no MySQL. |
| [PedidoAgendamentoTest.java](PedidoAgendamentoTest.java) | Verifica rejeição de agendamento passado. |
| [PersonalizacaoServiceTest.java](PersonalizacaoServiceTest.java) | Verifica as três faixas da caixa, o teto de custo e a composição do combo. |
| [PixPayloadServiceTest.java](PixPayloadServiceTest.java) | Valida valor, referência e CRC16 e decodifica a imagem QR que foi gerada. |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |
| [RelatorioCsvTest.java](RelatorioCsvTest.java) | Verifica o balanço exportado em CSV. |
| [VendaTeste.java](VendaTeste.java) | Fornece cadastro isolado, relógio fixo e configuração de loja para testes de venda. |
| [VitrineV150IntegracaoTest.java](VitrineV150IntegracaoTest.java) | Valida persistência de exclusões e rechecagem de custos, preços e combos antes da venda. |
| [VitrineV170IntegracaoTest.java](VitrineV170IntegracaoTest.java) | Valida categorias, opcionais, mínimo e cancelamento no MySQL. |
| [WhatsAppLinkServiceTest.java](WhatsAppLinkServiceTest.java) | Testa links, mensagens, configuração e abertura assíncrona do WhatsApp. |

## Árvore estrutural comentada

O ponto inicial é `src/test/java/com/salgaderia/app/service/`.

```text
src/test/java/com/salgaderia/app/service/  # Reúne testes automatizados dos componentes presentes neste pacote.
├── desconto/  # Estratégias de desconto e testes correspondentes.
│   └── EstrategiasDescontoTest.java  # Testa as estratégias e a escolha do melhor desconto.
├── pix/  # Testes HTTP em loopback para a integração PIX.
│   ├── MercadoPagoPixGatewayTest.java  # Verifica a integração HTTP com um servidor local, sem emitir cobranças.
│   └── README.md  # Guia dos testes HTTP locais.
├── ClubeGamificacaoIntegracaoTest.java  # Valida o VIP único, mensalidades antigas, frete, descontos, roleta e PIX no MySQL.
├── ComandaIntegracaoTest.java  # Valida operações de mesa, pagamentos concorrentes, baixa e fechamento no MySQL.
├── CozinhaStateTest.java  # Verifica mensagens e progresso dos estados da cozinha.
├── DescontoIntegracaoTest.java  # Integração de descontos e pagamentos no MySQL.
├── Etapa4IntegracaoTest.java  # Verifica cadastro por e-mail e transações de estoque, vendas, avaliações e financeiro no MySQL.
├── FotoAvaliacaoStorageTest.java  # Testa a cópia validada, limites, caminhos locais e remoção de anexos.
├── OperacaoLojaServiceTest.java  # Testa os limites do expediente e a combinação dos modos manual e automático.
├── OperacaoV162IntegracaoTest.java  # Valida configuração persistida, agendamento, venda sob demanda, bloqueio e despacho no MySQL.
├── PedidoAgendamentoTest.java  # Verifica rejeição de agendamento passado.
├── PersonalizacaoServiceTest.java  # Verifica as três faixas da caixa, o teto de custo e a composição do combo.
├── PixPayloadServiceTest.java  # Valida valor, referência e CRC16 e decodifica a imagem QR que foi gerada.
├── README.md  # Guia de navegação e explicações desta pasta.
├── RelatorioCsvTest.java  # Verifica o balanço exportado em CSV.
├── VendaTeste.java  # Fornece cadastro isolado, relógio fixo e configuração de loja para testes de venda.
├── VitrineV150IntegracaoTest.java  # Valida persistência de exclusões e rechecagem de custos, preços e combos antes da venda.
├── VitrineV170IntegracaoTest.java  # Valida categorias, opcionais, mínimo e cancelamento no MySQL.
└── WhatsAppLinkServiceTest.java  # Testa links, mensagens, configuração e abertura assíncrona do WhatsApp.
```

[🏠 Voltar ao README principal](../../../../../../../README.md)
