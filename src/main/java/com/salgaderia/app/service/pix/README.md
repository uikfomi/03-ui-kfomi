[🏠 Início](../../../../../../../../README.md) / [⬆️ service](../README.md) / 📂 pix

# 💠 Gateways de pagamento PIX

Separa as chamadas HTTP do checkout e da persistência JDBC. A venda reserva o estoque sem acessar a rede; o serviço concilia a transação com base nos dados gravados, sem converter falhas da API em aprovações.

## Explore as subpastas e arquivos

| Caminho | Conteúdo e responsabilidade |
|---|---|
| [LocalDemoPixGateway.java](LocalDemoPixGateway.java) | Gera BR Code de demonstração não pagável e não retorna aprovação automática. |
| [MercadoPagoPixGateway.java](MercadoPagoPixGateway.java) | Traduz a solicitação para JSON e chama a API com `HttpClient`, autenticação e idempotência. |
| [PixConfiguracao.java](PixConfiguracao.java) | Lê o token ignorado pelo Git sem enviar requisições. |
| [PixGatewayService.java](PixGatewayService.java) | Define a porta de criação e consulta utilizada pelo serviço de pagamento. |
| [PixPagamentoService.java](PixPagamentoService.java) | Valida os dados e a titularidade, vincula o ID, concilia a cobrança e aplica os efeitos locais em transação. |
| [README.md](README.md) | Guia da integração PIX da vitrine. |

## Árvore estrutural comentada

```text
src/main/java/com/salgaderia/app/service/pix/  # Integração de pagamento da vitrine e de demonstração.
├── LocalDemoPixGateway.java  # Gera BR Code não pagável para demonstração.
├── MercadoPagoPixGateway.java  # Adapta a operação PIX para a API de pagamentos.
├── PixConfiguracao.java  # Lê a configuração local sem expor a credencial.
├── PixGatewayService.java  # Porta de criação e consulta de cobranças.
├── PixPagamentoService.java  # Concilia o pagamento e confirma o pedido de forma transacional.
└── README.md  # Guia da integração PIX da vitrine.
```

## 📌 Operação

O diálogo da vitrine consulta a cada três segundos. O painel aberto também consulta transações conhecidas. Não há webhook nem estorno automático. Para testes, use o [adapter de teste](../../../../../../../test/java/com/salgaderia/app/service/pix/README.md) sem usar a API pública.
