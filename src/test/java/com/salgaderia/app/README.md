[🏠 Início](../../../../../../README.md) / [⬆️ salgaderia](../README.md) / 📂 app

# 🧪 app — testes

Reúne testes automatizados dos componentes presentes neste pacote.

Execute os testes compatíveis com o ambiente; os testes de integração podem exigir banco ou display.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [controller/](controller/) | Controllers das telas e testes de interface, conforme a localização. [Abrir guia](controller/README.md). |
| [dao/](dao/) | Contratos de acesso a dados ou testes JDBC. |
| [model/](model/) | Entidades de domínio ou testes dessas entidades. |
| [service/](service/) | Contratos, regras de negócio e testes de serviços. [Abrir guia](service/README.md). |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |

## Árvore estrutural comentada

O ponto inicial é `src/test/java/com/salgaderia/app/`.

```text
src/test/java/com/salgaderia/app/  # Reúne testes automatizados dos componentes presentes neste pacote.
├── controller/  # Controllers das telas e testes de interface, conforme a localização.
│   ├── AdminDashboardJavaFXTest.java  # Testa navegação administrativa, PDV, mesas, Pix e alertas de novos pedidos.
│   ├── AgendaJavaFXTest.java  # Testes JavaFX da agenda.
│   ├── README.md  # Guia de navegação e explicações desta pasta.
│   ├── TelasJavaFXTest.java  # Verifica cadastro com dois campos e navegação pelo perfil de acesso.
│   └── VitrineJavaFXTest.java  # Verifica vitrine, combo simplificado, exclusões da caixa, Clube VIP e rastreio de entrega.
├── dao/  # Contratos de acesso a dados ou testes JDBC.
│   └── AgendaJDBCTest.java  # Teste de integração JDBC da agenda.
├── model/  # Entidades de domínio ou testes dessas entidades.
│   ├── enums/  # Enumerações com comportamento e respectivos testes.
│   │   └── EnumsTest.java  # Testes de comportamento dos enums.
│   └── DominioTest.java  # Testes das entidades e regras de domínio.
├── service/  # Contratos, regras de negócio e testes de serviços.
│   ├── desconto/  # Estratégias de desconto e testes correspondentes.
│   │   └── EstrategiasDescontoTest.java  # Testa as estratégias e a escolha do melhor desconto.
│   ├── pix/  # Testes HTTP em loopback para a integração PIX.
│   │   ├── MercadoPagoPixGatewayTest.java  # Verifica a integração HTTP com um servidor local, sem emitir cobranças.
│   │   └── README.md  # Guia dos testes HTTP locais.
│   ├── ClubeGamificacaoIntegracaoTest.java  # Valida o VIP único, mensalidades antigas, frete, descontos, roleta e PIX no MySQL.
│   ├── ComandaIntegracaoTest.java  # Valida operações de mesa, pagamentos concorrentes, baixa e fechamento no MySQL.
│   ├── CozinhaStateTest.java  # Verifica mensagens e progresso dos estados da cozinha.
│   ├── DescontoIntegracaoTest.java  # Integração de descontos e pagamentos no MySQL.
│   ├── Etapa4IntegracaoTest.java  # Verifica cadastro por e-mail e transações de estoque, vendas, avaliações e financeiro no MySQL.
│   ├── FotoAvaliacaoStorageTest.java  # Testa a cópia validada, limites, caminhos locais e remoção de anexos.
│   ├── OperacaoLojaServiceTest.java  # Testa os limites do expediente e a combinação dos modos manual e automático.
│   ├── OperacaoV162IntegracaoTest.java  # Valida configuração persistida, agendamento, venda sob demanda, bloqueio e despacho no MySQL.
│   ├── PedidoAgendamentoTest.java  # Verifica rejeição de agendamento passado.
│   ├── PersonalizacaoServiceTest.java  # Verifica as três faixas da caixa, o teto de custo e a composição do combo.
│   ├── PixPayloadServiceTest.java  # Valida valor, referência e CRC16 e decodifica a imagem QR que foi gerada.
│   ├── README.md  # Guia de navegação e explicações desta pasta.
│   ├── RelatorioCsvTest.java  # Verifica o balanço exportado em CSV.
│   ├── VendaTeste.java  # Fornece cadastro isolado, relógio fixo e configuração de loja para testes de venda.
│   ├── VitrineV150IntegracaoTest.java  # Valida persistência de exclusões e rechecagem de custos, preços e combos antes da venda.
│   └── WhatsAppLinkServiceTest.java  # Testa links, mensagens, configuração e abertura assíncrona do WhatsApp.
└── README.md  # Guia de navegação e explicações desta pasta.
```

[🏠 Voltar ao README principal](../../../../../../README.md)
