[🏠 Início](../../../README.md) / [⬆️ main](../README.md) / 📂 resources

# 🎨 Recursos da aplicação

Agrupa telas FXML, estilos e configuração padrão consumidos pelo JavaFX e pela infraestrutura.

O arquivo database.local.properties é local e ignorado pelo Git; verifique se artefatos distribuídos não contêm credenciais.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [css/](css/) | Folhas de estilo das telas JavaFX. [Abrir guia](css/README.md). |
| [fxml/](fxml/) | Descrições de interface JavaFX. [Abrir guia](fxml/README.md). |
| [icons/](icons/) | Espaço reservado para ícones. |
| [images/](images/) | Espaço reservado para imagens. |
| [database.properties](database.properties) | Template JDBC e propriedades opcionais de pagamento PIX. |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |

## Árvore estrutural comentada

O ponto inicial é `src/main/resources/`.

```text
src/main/resources/  # Agrupa telas FXML, estilos e configuração padrão consumidos pelo JavaFX e pela infraestrutura.
├── css/  # Folhas de estilo das telas JavaFX.
│   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── admin.css  # Estilos do painel administrativo.
│   ├── agenda.css  # Estilos da agenda.
│   ├── cliente-vitrine.css  # Estiliza catálogo, checkout, disponibilidade e banner de entrega em rota.
│   ├── global.css  # Estilos compartilhados da aplicação.
│   └── README.md  # Guia de navegação e explicações desta pasta.
├── fxml/  # Descrições de interface JavaFX.
│   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── admin-dashboard.fxml  # Estrutura a navegação administrativa, incluindo expediente e confirmação do Clube VIP.
│   ├── agenda-contatos.fxml  # Estrutura visual da agenda.
│   ├── cliente-vitrine.fxml  # Estrutura vitrine, perfil, checkout, acompanhamento e Clube VIP.
│   ├── login.fxml  # Estrutura as telas de login e cadastro com e-mail e senha.
│   ├── README.md  # Guia de navegação e explicações desta pasta.
│   └── selecao-modulos.fxml  # Estrutura visual da seleção de módulos.
├── icons/  # Espaço reservado para ícones.
│   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
├── images/  # Espaço reservado para imagens.
│   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
├── database.properties  # Template JDBC e propriedades opcionais de pagamento PIX.
└── README.md  # Guia de navegação e explicações desta pasta.
```

## 📌 Status do Conteúdo

As pastas abaixo estão reservadas e aguardam materiais finais; os arquivos `.gitkeep` apenas preservam a estrutura:

- `src/main/resources/icons/` — Espaço reservado para ícones.
- `src/main/resources/images/` — Espaço reservado para imagens.

[🏠 Voltar ao README principal](../../../README.md)
