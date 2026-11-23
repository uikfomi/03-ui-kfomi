[🏠 Início](../../../../../../../README.md) / [⬆️ app](../README.md) / 📂 util

# 🔧 Infraestrutura

Concentra conexão, navegação, autenticação auxiliar e suporte às telas, quando presentes.

Mantenha as responsabilidades desta camada separadas das demais e consulte os arquivos relacionados antes de alterações.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [.gitkeep](.gitkeep) | Marcador que mantém a pasta no controle de versão; não contém dados de aplicação. |
| [ConnectionFactory.java](ConnectionFactory.java) | Pool HikariCP e transações JDBC. |
| [FotoAvaliacaoUi.java](FotoAvaliacaoUi.java) | Carrega miniaturas JavaFX de fotos de avaliações guardadas localmente. |
| [Navegacao.java](Navegacao.java) | Navegação entre telas FXML. |
| [PagamentoPixUi.java](PagamentoPixUi.java) | Mostra QR Code e consulta o pagamento com Timeline, sem bloquear a interface. |
| [PixPainel.java](PixPainel.java) | Desenha o QR local ou do gateway e disponibiliza o código PIX para cópia. |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |
| [RoletaUi.java](RoletaUi.java) | Anima o prêmio transacional em seis fatias regulares ou VIP, com giro extra quando disponível. |
| [Senhas.java](Senhas.java) | Geração e verificação de hashes de senha. |
| [TelaAutenticada.java](TelaAutenticada.java) | Contrato para telas com usuário autenticado. |
| [WhatsAppUi.java](WhatsAppUi.java) | Mostra na JavaFX a alternativa quando o WhatsApp não abre. |

## Árvore estrutural comentada

O ponto inicial é `src/main/java/com/salgaderia/app/util/`.

```text
src/main/java/com/salgaderia/app/util/  # Concentra conexão, navegação, autenticação auxiliar e suporte às telas, quando presentes.
├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
├── ConnectionFactory.java  # Pool HikariCP e transações JDBC.
├── FotoAvaliacaoUi.java  # Carrega miniaturas JavaFX de fotos de avaliações guardadas localmente.
├── Navegacao.java  # Navegação entre telas FXML.
├── PagamentoPixUi.java  # Mostra QR Code e consulta o pagamento com Timeline, sem bloquear a interface.
├── PixPainel.java  # Desenha o QR local ou do gateway e disponibiliza o código PIX para cópia.
├── README.md  # Guia de navegação e explicações desta pasta.
├── RoletaUi.java  # Anima o prêmio transacional em seis fatias regulares ou VIP, com giro extra quando disponível.
├── Senhas.java  # Geração e verificação de hashes de senha.
├── TelaAutenticada.java  # Contrato para telas com usuário autenticado.
└── WhatsAppUi.java  # Mostra na JavaFX a alternativa quando o WhatsApp não abre.
```

[🏠 Voltar ao README principal](../../../../../../../README.md)
