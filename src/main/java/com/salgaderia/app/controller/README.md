[🏠 Início](../../../../../../../README.md) / [⬆️ app](../README.md) / 📂 controller

# 🖥️ Controllers JavaFX

Coordena a interação das telas e delega operações de negócio aos serviços.

Mantenha as responsabilidades desta camada separadas das demais e consulte os arquivos relacionados antes de alterações.

Na v1.7.0, o painel administrativo edita categorias livres e grupos de opcionais no cadastro de produtos, configura o mínimo de entrega e destaca as instruções de preparo. A vitrine coordena o diálogo de personalização, explica quanto falta para o mínimo e oferece cancelamento apenas em pedidos pendentes.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [.gitkeep](.gitkeep) | Marcador que mantém a pasta no controle de versão; não contém dados de aplicação. |
| [AdminDashboardController.java](AdminDashboardController.java) | Coordena PDV, expediente, despacho, catálogo e confirmação do Clube VIP. |
| [AgendaController.java](AgendaController.java) | Interface da agenda de clientes e fornecedores. |
| [ClienteVitrineController.java](ClienteVitrineController.java) | Coordena perfil, vitrine, combos, caixas por orçamento, checkout e Clube VIP. |
| [LoginController.java](LoginController.java) | Autentica contas e coordena o cadastro com e-mail e senha. |
| [README.md](README.md) | Guia de navegação e explicações desta pasta. |
| [SelecaoModulosController.java](SelecaoModulosController.java) | Seleção de módulos administrativos. |

## Árvore estrutural comentada

O ponto inicial é `src/main/java/com/salgaderia/app/controller/`.

```text
src/main/java/com/salgaderia/app/controller/  # Coordena a interação das telas e delega operações de negócio aos serviços.
├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
├── AdminDashboardController.java  # Coordena PDV, expediente, despacho, catálogo e confirmação do Clube VIP.
├── AgendaController.java  # Interface da agenda de clientes e fornecedores.
├── ClienteVitrineController.java  # Coordena perfil, vitrine, combos, caixas por orçamento, checkout e Clube VIP.
├── LoginController.java  # Autentica contas e coordena o cadastro com e-mail e senha.
├── README.md  # Guia de navegação e explicações desta pasta.
└── SelecaoModulosController.java  # Seleção de módulos administrativos.
```

[🏠 Voltar ao README principal](../../../../../../../README.md)
