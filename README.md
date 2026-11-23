# Ui K'Fomi — Salgaderia Pro

![Versão](https://img.shields.io/badge/vers%C3%A3o-v1.7.0-8b4513)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1)
[![Licença MIT](https://img.shields.io/badge/licen%C3%A7a-MIT-green)](LICENSE)

Projeto livre da **Ui K'fomi**, startup de Maranguape, Ceará, dedicada a soluções digitais para o setor de alimentação. Este repositório reúne o sistema integrado, incluindo os módulos de autenticação e agenda.

Sistema desktop para uma salgaderia, com login único, vitrine de pedidos, agenda de contatos e painel administrativo de vendas e estoque. O projeto adota **Java 17**, **JavaFX 21**, **MySQL** com **JDBC puro**, pool HikariCP e arquitetura **MVC + Service + DAO**, sem ORM.

## Índice

- [Funcionalidades](#funcionalidades)
- [Operação da loja](#operação-da-loja)
- [Categorias, opcionais e cancelamento](#categorias-opcionais-e-cancelamento)
- [Combos, caixas e fotos](#combos-caixas-e-fotos)
- [Clube VIP, conquistas e roleta](#clube-vip-conquistas-e-roleta)
- [Pagamento PIX](#pagamento-pix)
- [Pré-requisitos](#pré-requisitos)
- [Instalação e execução](#instalação-e-execução)
- [Credenciais iniciais de teste](#credenciais-iniciais-de-teste)
- [Arquitetura e organização](#arquitetura-e-organização)
- [Organização do Projeto](#-organização-do-projeto)
- [Diagramas](#diagramas)
- [Testes](#testes)
- [Licença](#licença)

## Funcionalidades

- **Autenticação e cadastro público:** em **Criar conta**, clientes informam apenas e-mail e senha. O e-mail normalizado vira o login e o texto antes do `@` vira o apelido inicial. Em **Meu perfil**, é possível personalizar o apelido e informar WhatsApp e endereço após entrar. Endereço é necessário para escolher **Entrega**; contas antigas e administrativas mantêm o login existente. A tela inicial é escolhida conforme o perfil.
- **Agenda:** clientes e fornecedores, busca, filtros, cadastro, edição e exclusão. Contatos vinculados a outros registros podem ter a exclusão impedida pelo banco.
- **Vitrine do cliente:** catálogo com status **Disponível** ou **Indisponível no momento**, carrinho, combos, caixas misteriosas e galeria de fotos de avaliações. O checkout aceita entrega, retirada ou consumo no local, pagamento e agendamento opcional. Também é possível repetir o último pedido concluído mediante revalidação da disponibilidade de venda.
- **Termômetro da cozinha:** estados visuais de preparo e estimativa informativa baseada na quantidade de pedidos ativos; pedidos futuros indicam que aguardam o horário agendado.
- **Administração:** PDV com formas de pagamento e troco, fila de pedidos da vitrine, status e horário da loja, bloqueio rápido de produtos, estoque interno, entrada por fornecedor, perdas, indicadores financeiros, gráficos e moderação de avaliações.
- **Mesas e comandas:** dez mesas, lançamento de itens sem cliente, divisão igual do saldo e pagamentos parciais por valor ou item; a mesa é liberada quando a conta é quitada.
- **Clube VIP Ui K'Fomi:** plano único pré-pago de **R$ 99,90/mês**, com frete grátis e **15% OFF** nos produtos elegíveis durante a vigência.
- **Gamificação:** selos persistentes, roleta animada com giro extra e modo VIP, prêmios de uso único e aviso de expiração.
- **PIX:** cobrança Mercado Pago e confirmação automática para pedidos da vitrine; QR local e confirmação manual no PDV, nas comandas e nas mensalidades.
- **Estados vazios:** as telas não exibem cadastros fictícios; mostras de ausência de dados são distintas de falhas de conexão.
- **WhatsApp:** após finalizar e no histórico, o cliente pode abrir uma conversa com a loja pelo botão de acompanhamento. Na agenda e na fila, o administrador pode abrir uma conversa com o contato; ao mudar o status, a opção de notificação vem marcada. A abertura é assíncrona para não bloquear a interface. No Ubuntu/Linux, o aplicativo tenta `xdg-open` antes da API Desktop. O link com a mensagem preenchida é copiado automaticamente para a área de transferência, quando disponível. Se o navegador não abrir, a interface informa a cópia ou oferece o link para cópia manual. O envio continua dependendo de confirmação no WhatsApp.
- **Acompanhamento:** a lista “Meus Pedidos” e o indicador administrativo de pedidos pendentes consultam o banco a cada cinco segundos enquanto as respectivas telas estão abertas. Os alertas recentes são locais à sessão administrativa.
- **Balanço CSV:** o painel financeiro permite exportar o período exibido, em UTF-8 com BOM e separador `;`, incluindo faturamento bruto, descontos, custos, perdas e lucro.

### Regras importantes

- Pedidos da vitrine avançam de `PENDENTE` para `PREPARANDO` e `PRONTO`. Em **Entrega**, passam por `SAIU_ENTREGA` antes de `ENTREGUE`; **Retirada** e **Local** vão direto de `PRONTO` para `ENTREGUE`. O PDV registra `ENTREGUE` na própria transação de venda.
- Agendamentos são opcionais e devem estar no futuro e dentro do expediente de Maranguape (`America/Fortaleza`); não há despacho automático. O termômetro usa uma estimativa simples de cinco minutos por pedido ativo, não uma previsão garantida.
- Itens de comanda usam quantidades inteiras, respeitam o bloqueio de venda e continuam baixando estoque no lançamento. A divisão igual distribui os centavos restantes entre as primeiras pessoas; os pagamentos são registrados individualmente por valor ou item. A venda entra no financeiro ao quitar o saldo, sem nova baixa de estoque. Comanda vazia pode ser fechada sem gerar venda.
- **Descontos não cumulativos:** entre combo (10% sobre os itens do combo), cupom, fidelidade (5% após três pedidos reais entregues), cento (12% nos itens elegíveis a partir de 100 salgados) e desconto VIP (15%), vale o melhor abatimento único sobre os itens elegíveis. Frete grátis VIP é um benefício separado. Brindes ou pontos não substituem o desconto VIP, e um cupom percentual só o substitui se for mais vantajoso. Caixas por orçamento têm preço fechado e não recebem abatimento adicional nem contam para o valor mínimo de cupons. Para o cento, categorias com “salgad”, “frit” ou “assad” no nome são elegíveis, exceto “bebid” ou “doc”, sem distinção de caixa e acentos. Vendas de PDV identificadas também contam para a fidelidade.
- Os cupons `UIKFOMI10` (10%) e `FESTA15` (15% a partir de R$ 80) são exemplos iniciais. Cupons inválidos, inativos ou abaixo do mínimo bloqueiam a finalização até correção ou remoção. O sistema revalida preços, elegibilidade e desconto ao finalizar; alterações exigem nova conferência.
- Finalização, itens, baixa física e movimentações compartilham uma transação JDBC. Na vitrine e no PDV, produtos liberados podem ser vendidos mesmo com saldo zero. Se houver saldo, a venda baixa apenas a quantidade física disponível; o cancelamento repõe somente a baixa registrada. Lotes, perdas e comandas continuam controlando o estoque físico.
- Produtos são criados com estoque zero. Use **Entrada de lote** para registrar unidades recebidas de um fornecedor ou altere explicitamente o estoque físico em **Editar produto** para gerar um ajuste. O saldo numérico permanece restrito à administração.
- Uma avaliação só é aceita quando há pedido `ENTREGUE` do cliente contendo o produto. O acesso aos controles administrativos é restrito ao perfil de administrador na interface.
- O financeiro considera pedidos entregues pela **data de criação**, incluindo a taxa de entrega e excluindo PIX de demonstração; perdas entram pela data da movimentação. O lucro é faturamento após descontos menos custo dos itens vendidos e perdas; **não inclui impostos, custo de entrega, custo de brindes ou outras despesas**. As categorias mostram receita de produtos, sem a taxa de entrega ou seu abatimento, e rateiam os demais descontos. O painel mostra o mês atual. Custos históricos desconhecidos não são estimados.
- O número padrão de WhatsApp da loja é `5585989095223` e pode ser substituído por `whatsapp.loja` em `database.local.properties`. A mensagem inclui a forma de pagamento escolhida; pedidos históricos sem essa informação permanecem como “não informado”. Em ambientes sem navegador, tenta-se copiar o link; caso a área de transferência também esteja indisponível, o link é mostrado para cópia manual.

### Operação da loja

- Em **Status da Loja**, o Admin pode editar abertura e fechamento (padrão `08:00–23:00`), ativar/desativar o horário automático e usar **Fechar Loja Agora / Abrir Loja Agora**. Com o automático ativo, a chave manual aberta permite pedidos imediatos somente dentro do expediente. Para abrir fora do horário, desative o automático. A abertura é inclusiva, o fechamento é exclusivo; `18:00–01:00` atravessa a meia-noite.
- O selo da vitrine consulta o estado a cada cinco segundos. Quando fechada, a loja aceita na vitrine somente encomendas futuras dentro do expediente configurado, mesmo em pausa manual ou com o automático desligado. O serviço revalida a regra no banco ao finalizar; a pausa não desativa o PDV interno.
- Em **Produtos e estoque**, selecione um produto e use **Bloquear / Liberar Venda**. Estoque zero não esgota o cardápio: produtos liberados são feitos sob demanda. Uma mudança é revalidada antes de finalizar, inclusive para itens que já estejam no carrinho. Para a foto, prefira **Selecionar imagem do computador**.
- Na **Fila de pedidos**, **Despachar entrega** solicita o nome do entregador. A lista **Meus Pedidos** passa a mostrar o banner de entrega em rota. Se a notificação em WhatsApp estiver ativada e houver telefone, o sistema abre uma mensagem preenchida com o nome do entregador; o operador confirma o envio no WhatsApp.

### Categorias, opcionais e cancelamento

- No **Estoque**, ao cadastrar ou editar um produto, selecione uma categoria existente ou digite um novo nome. O filtro da vitrine é carregado do banco com todas as categorias ativas, mesmo quando uma categoria ainda não tem produtos.
- Na seção **Bordas e adicionais** do produto, informe o grupo, a opção, o acréscimo, quantas escolhas com preço cadastrado são grátis e o máximo de escolhas por unidade. Use os mesmos limites em todas as opções do grupo. Ex.: **Borda Recheada**, máximo 1; **Adicionais**, máximo 3 com o primeiro grátis. A franquia segue a ordem do catálogo.
- Na vitrine, o produto abre uma janela para selecionar opcionais e/ou escrever uma observação. O preço é mostrado antes de adicionar, e as instruções ficam em destaque na **Fila de pedidos**. O histórico mantém o resumo da compra, mesmo se o catálogo mudar.
- Em **Status da Loja**, configure o **Pedido mínimo para entrega** (padrão **R$ 20,00**). O mínimo usa o subtotal dos itens e opcionais antes de descontos promocionais, sem frete. **Retirada** e **Local** não exigem mínimo.
- Em **Meus Pedidos**, **Cancelar Pedido** aparece somente enquanto o pedido estiver pendente. Depois que a cozinha começa a preparar, o cliente deve falar com a loja. Se uma cobrança PIX real já foi iniciada, o cancelamento também precisa ser tratado com a loja; o aplicativo não faz estornos.

## Combos, caixas e fotos

1. **Configurar o catálogo:** cadastre os produtos normalmente em **Produtos e estoque**, com preço, custo e disponibilidade. Para aparecer na seleção de bebidas do combo, o produto deve pertencer a uma categoria cujo nome contenha “bebid”, como **Bebidas**. Não é preciso preencher ficha técnica de massa ou recheio.
2. **Montar um combo:** escolha um principal, um acompanhamento diferente e uma bebida. São três produtos reais, liberados para venda, com prévia de **10% OFF** no combo. O checkout aplica a regra do maior desconto único.
3. **Sortear uma caixa:** escolha **R$ 25,00**, **R$ 40,00** ou **R$ 60,00** e, se desejar, exclua categorias e produtos na própria tela. O sorteio usa produtos liberados com preço e custo positivo cadastrados. O valor de catálogo fica entre **100% e 115% do preço fechado**; o custo de produção somado não pode ultrapassar esse preço. A proteção considera o custo cadastrado, não frete, impostos ou despesas externas. Sem combinação válida, ajuste os filtros ou o orçamento. O sistema não troca itens fora dos filtros e essas exclusões não garantem ausência de ingredientes ou contato cruzado.
4. **Revalidar antes de cobrar:** os itens da caixa ficam ocultos até a entrega; disponibilidade, preço, custo e exclusões são conferidos novamente ao fechar o pedido. **Pedir o de Sempre** revalida escolhas atuais. Caixas e combos de formatos antigos permanecem no histórico, mas precisam ser remontados nas opções atuais para nova compra.
5. **Avaliar com foto:** em **Meus Pedidos**, abra um pedido entregue e avalie um produto. A imagem opcional (JPEG ou PNG, até 8 MB e 12 megapixels) passa por validação e é armazenada como PNG sem metadados. Até 12 fotos recentes aparecem na galeria sem o nome do cliente; o Admin vê a miniatura na moderação e, ao excluir a avaliação, remove a cópia local. Veja [como guardar os arquivos](uploads/README.md).

### Clube VIP, conquistas e roleta

- **Plano único:** o **Clube VIP Ui K'Fomi** custa **R$ 99,90 por mês**. Solicite a mensalidade e pague usando a referência exibida. A vigência começa quando o operador verifica e confirma o recebimento. Não há débito automático. A renovação abre nos sete dias anteriores ao vencimento e, se confirmada antes dele, estende o período já pago. Cancelar encerra o benefício e não gera reembolso automático.
- **Benefícios:** durante a vigência, entregas da vitrine têm frete grátis e os itens elegíveis recebem **15% OFF** automaticamente, também no PDV quando a venda é vinculada ao assinante. Caixas de preço fechado não recebem desconto adicional, mas têm frete grátis na vitrine para membros VIP.
- **Administração e migração:** a aba **Clube VIP** lista o plano, assinantes e mensalidades a confirmar. As vigências existentes são preservadas e passam ao VIP. Mensalidades antigas já emitidas mantêm valor e referência; o prazo começa ou é estendido pela confirmação manual. Os planos anteriores ficam apenas no histórico, sem novas adesões. As confirmações não entram no balanço de vendas.
- **Selos:** **Primeiro Pedido** é concedido na finalização, **Rei do Cento** após receber um pedido com 100 salgados elegíveis e **Cliente Fiel** após a terceira entrega. O barramento Observer grava as conquistas na mesma transação do pedido, sem duplicar selos.
- **Roleta:** pagamentos PIX confirmados liberam a roleta; pedidos com pagamento na entrega liberam ao marcar `ENTREGUE`. O limite é **um pedido por conta a cada 24 horas**, inclusive se cair “Não foi dessa vez”. Um giro extra fica no mesmo pedido e não pode gerar outro giro extra. A roleta regular oferece mini porção surpresa, frete grátis, pontos em dobro, 10% OFF, giro extra e uma fatia sem prêmio. Acima de **R$ 60,00 no total final**, a roleta VIP oferece também combo brinde e 15% OFF, no lugar da fatia sem prêmio e do cupom de 10%.
- **Resgate:** cada cupom é exclusivo da conta, exige **R$ 25,00 em itens elegíveis antes do desconto** e vence **cinco dias corridos** após o primeiro giro. A vitrine mostra a contagem regressiva quando faltam menos de 24 horas. O cupom é reservado somente quando seu benefício é escolhido. Cancelar o pedido de resgate devolve o cupom ainda válido; cancelar o pedido premiado revoga o prêmio não utilizado.
- **Benefícios:** frete grátis abate apenas a taxa de entrega. A taxa padrão é **R$ 5,00** para novos pedidos com entrega e pode ser alterada com `entrega.taxa` (`0.00` desativa a tarifa). A cozinha vê o brinde como observação para separar a cortesia. Para escolher a maior vantagem, o sistema compara a mini porção e o combo com valores indicativos de R$ 4 e R$ 8; eles não reduzem o total a pagar. Pontos em dobro não vencem um desconto em dinheiro: o pedido elegível acumula 10 pontos, ou 20 se o prêmio for selecionado. Os pontos não são dinheiro.

### Pagamento PIX

**Na vitrine:** configure `mercadopago.access.token` somente em `database.local.properties` e reinicie o app. Ao selecionar PIX, informe o e-mail e o CPF/CNPJ do pagador. Finalize para gerar a cobrança com o valor final. O app mostra o código oficial retornado pela API e consulta o status a cada **três segundos** enquanto o diálogo estiver aberto. Em **Meus Pedidos**, use **Ver PIX do pedido** para reabrir a mesma cobrança. O painel administrativo aberto também concilia transações conhecidas. O saldo físico baixado fica reservado e a cozinha só recebe o pedido após a aprovação. O fluxo segue a [documentação oficial de PIX da API de Pagamentos do Mercado Pago](https://www.mercadopago.com.br/developers/pt/docs/checkout-api-payments/integration-configuration/integrate-pix).

**Segurança e retomada:** o pedido usa a mesma referência e chave de idempotência em toda repetição da criação. Após vincular o ID remoto, o app só consulta essa transação, validando titularidade local, referência, valor, moeda e método antes de marcar `PAGO`. Os dados do pagador são removidos do pedido ao vincular o ID ou cancelar. Uma transação rejeitada ou cancelada pelo provedor devolve a reserva. Se o envio ficar sem resposta por 20 minutos e não houver ID conhecido, confira a referência no painel Mercado Pago antes de nova cobrança. O app não estorna PIX nem usa um webhook; se ambos os painéis estiverem fechados, a confirmação aguarda a retomada da consulta.

**Demonstração:** sem token, o checkout gera um BR Code não pagável e oferece **Confirmar Pagamento (Modo Offline)** apenas para pedidos de demonstração. A simulação é identificada no Admin e não gera receita, pontos nem cupons resgatáveis. Se a conexão cair com token configurado, o app preserva o pedido real; se ainda não tiver o QR oficial, mostra apenas uma prévia de demonstração, sem confirmação offline.

**No PDV, nas comandas e nas mensalidades:** continua o BR Code local com valor, configurado por `pix.chave`, `pix.beneficiario` e `pix.cidade`. O operador confere o recebimento no banco antes de registrar. Sem `pix.chave`, é demonstração (`uikfomi.demonstracao@example.invalid`). Na comanda é possível pagar o saldo, uma parcela digitada ou as unidades selecionadas. Não há débito recorrente automático.

## Pré-requisitos

- JDK **17 ou superior** e Maven instalados, com `java`, `javac` e `mvn` disponíveis no terminal.
- MySQL Server **8.0.16 ou superior** e cliente `mysql`; os scripts utilizam `CHECK` e recursos do MySQL 8. A aplicação usa `mysql-connector-j` 8.0.33.
- Ambiente gráfico compatível com JavaFX. A primeira execução Maven pode precisar de acesso à internet para baixar dependências.

Confira as ferramentas:

```bash
java -version
mvn -version
mysql --version
```

## Instalação e execução

Execute os comandos na raiz do repositório. Os exemplos usam um usuário administrativo do MySQL chamado `root`; ele é **diferente** do usuário `root` da aplicação.

### 1. Criar banco e aplicar migrações

Em uma instalação nova, com uma conta MySQL que possa criar o banco e alterar tabelas:

```bash
mysql -u root -p < database/scripts/schema.sql
mysql -u root -p < database/scripts/etapa4_migracao.sql
mysql -u root -p < database/scripts/etapa8_pdv.sql
mysql -u root -p < database/scripts/etapa9_descontos.sql
mysql -u root -p < database/scripts/etapa10_pagamento_vitrine.sql
mysql -u root -p < database/scripts/etapa11_salao_agendamento.sql
mysql -u root -p < database/scripts/etapa12_combo_avaliacoes.sql
mysql -u root -p < database/scripts/etapa13_clube_gamificacao.sql
mysql -u root -p < database/scripts/etapa14_pix_roleta_vip.sql
mysql -u root -p < database/scripts/etapa15_operacao_loja.sql
mysql -u root -p < database/scripts/etapa16_cadastro_caixa_clube_vip.sql
mysql -u root -p < database/scripts/etapa17_personalizacao_entrega.sql
```

**Execute as migrações na ordem e somente uma vez.** Embora o schema inicial use `IF NOT EXISTS`, os scripts de migração usam `ALTER TABLE` não idempotente. Antes de atualizar um banco existente, faça backup, confira quais migrações já foram aplicadas e revise dados legados; DDL no MySQL pode causar commit implícito. Usuários clientes antigos sem contato vinculado e fornecedores sem os campos adicionais precisam de saneamento manual. Consulte [as notas de persistência](docs/etapa4-persistencia.md).

### 2. Criar uma conta local para a aplicação

No cliente MySQL, com uma conta que tenha privilégio de criar usuários e conceder permissões, por exemplo:

```sql
CREATE USER 'salgaderia'@'localhost' IDENTIFIED BY '123456';
GRANT SELECT, INSERT, UPDATE, DELETE ON salgaderia_db.* TO 'salgaderia'@'localhost';
```

Se a conta já existir, não execute `CREATE USER` novamente: ajuste a configuração e os privilégios conforme o ambiente. As credenciais acima são **somente um exemplo local de desenvolvimento**.

### 3. Configurar a conexão

Copie o template versionado e edite a cópia:

```bash
cp src/main/resources/database.properties src/main/resources/database.local.properties
```

Exemplo de `src/main/resources/database.local.properties`:

```properties
url=jdbc:mysql://localhost:3306/salgaderia_db
user=salgaderia
password=123456
whatsapp.loja=5585989095223
# Opcionais; sem chave real, o PIX permanece em demonstração
# pix.chave=
pix.beneficiario=UI KFOMI SALGADERIA
pix.cidade=MARANGUAPE
entrega.taxa=5.00
# Ativa PIX real via Mercado Pago na vitrine; mantenha a credencial fora do Git
# mercadopago.access.token=
```

`ConnectionFactory` procura `database.local.properties` no **classpath** e, se ele não existir, usa `database.properties`. O arquivo local é ignorado pelo Git. O Maven copia recursos para `target/classes`; portanto, a configuração local pode também entrar em artefatos empacotados: **não distribua um build contendo credenciais**. Revise a configuração de recursos antes de qualquer distribuição.

### 4. Compilar e iniciar

```bash
mvn clean test-compile
mvn javafx:run
```

A aplicação abre na tela única de login. Em caso de falha, verifique se o servidor MySQL está ativo, se o schema e todas as migrações foram aplicados e se a conta tem acesso ao banco. Para renovar os recursos copiados ao `target/classes` após editar a configuração, repita a compilação.

## Credenciais iniciais de teste

| Login da aplicação | Senha inicial | Perfil | Destino após login |
|---|---|---|---|
| `root` | `toor` | `CLIENTE` | Vitrine de pedidos |
| `toor` | `root` | `ADMIN` | Seleção de módulos |

As credenciais só correspondem às **sementes iniciais**, desde que ainda não tenham sido alteradas no banco. O schema as insere inicialmente em texto puro para desenvolvimento; após um login bem-sucedido, `AutenticacaoServiceImpl.login()` converte a senha legada para PBKDF2. Novos cadastros usam PBKDF2. Não use essas contas ou senhas em produção; reexecutar o schema **não redefine** as senhas existentes.

> **Segurança:** este é um projeto de desenvolvimento/estudo. A interface verifica o perfil e algumas operações consultam o administrador no banco, mas os serviços ainda recebem objetos de usuário fornecidos pelo chamador. É necessária uma sessão autenticada não falsificável e uma revisão de autorização antes de expor o sistema a clientes ou APIs não confiáveis.

## Arquitetura e organização

```text
src/main/java/com/salgaderia/app/
  Main.java               inicialização JavaFX
  model/                  entidades, enums e regras de domínio
  controller/             controladores das telas FXML
  service/ e service/impl/ contratos e regras de aplicação
  dao/ e dao/impl/        contratos e implementações JDBC
  exception/              exceções de domínio e persistência
  util/                   conexão, transações, senhas e navegação
src/main/resources/       FXML, CSS e templates de configuração
database/scripts/         schema e migrações SQL
database/DER/ e DL/       modelos de dados
docs/uml/                 diagrama de classes
```

Os controllers coordenam a interface; os services orquestram regras de negócio; DAOs executam SQL com `PreparedStatement` e `try-with-resources`. `ConnectionFactory.executarEmTransacao()` abre uma conexão do HikariCP, desativa auto-commit, confirma em caso de sucesso e reverte em caso de falha.

### Mapeamento de conceitos de orientação a objetos

A tabela apresenta pontos concretos da implementação; métodos herdados e regras adicionais podem existir fora dos exemplos indicados.

| Conceito | Classes e métodos exatos | Aplicação |
|---|---|---|
| Builder | `service.ComboBuilder.principal()`, `acompanhamento()`, `bebida()` e `montar()` | Compõe o combo com três produtos reais do catálogo e valida os papéis e a disponibilidade. |
| Observer | `service.BarramentoPedido.publicar()` e `service.PedidoConcluidoObserver.aoAlterarPedido()` | Propaga eventos transacionais do pedido; `ConquistasObserver` registra selos, liberação da roleta e revogação de cupons. |
| Adapter | `service.pix.PixGatewayService`, `MercadoPagoPixGateway` e `LocalDemoPixGateway` | A vitrine consulta um serviço de pagamento comum; o adapter traduz sua solicitação para HTTP e JSON ou gera uma demonstração sem cobrança. |
| Abstração | `model.Usuario.getTelaInicialFxml()` e `model.Usuario.podeAcessarPainelGerencial()`; `model.Contato.getResumoContato()` | Métodos abstratos definem comportamentos comuns sem implementar as variações dos subtipos. |
| Encapsulamento | `model.Produto.setPrecoVenda()`, `setCustoProducao()`, `setEstoqueAtual()`, `setEstoqueMinimo()`, `adicionarEstoque()` e `removerEstoque()` | Atributos privados e validações controlam valores monetários e quantidades de estoque. |
| Encapsulamento | `model.Pedido.getItens()`, `adicionarItem()`, `removerItem()` e `transicionarPara()`; `model.PedidoItem` e `calcularSubtotal()` | A lista retornada é uma cópia não modificável; o pedido controla alterações de itens e transições. Os campos de `PedidoItem` são finais. |
| Encapsulamento | `model.Avaliacao.setNota()`; `model.Fornecedor.setCnpj()` e `setPrazoEntregaDias()` | A nota deve estar entre 1 e 5; o CNPJ deve conter 14 dígitos após remoção da formatação e o prazo não pode ser negativo. A validação do CNPJ não calcula dígitos verificadores. |
| Herança | Construtores `model.UsuarioCliente(...)` e `model.UsuarioAdmin(...)`; `model.Cliente(...)` e `model.Fornecedor(...)` | `UsuarioCliente` e `UsuarioAdmin` estendem `Usuario`; `Cliente` e `Fornecedor` estendem `Contato`. |
| Polimorfismo | `model.UsuarioCliente.getTelaInicialFxml()` / `model.UsuarioAdmin.getTelaInicialFxml()`; `util.Navegacao.abrirTelaInicial()` | A navegação chama o método da referência `Usuario`, que retorna a tela específica do subtipo. |
| Polimorfismo | `model.Cliente.getResumoContato()` e `model.Fornecedor.getResumoContato()`; `model.enums.FormaPagamento.isPermiteTroco()` | Os subtipos formatam resumos diferentes; cada constante de pagamento implementa o método, permitindo troco apenas em `DINHEIRO`. |
| Interfaces | `service.PedidoVendaService.finalizar()` e `finalizarPdv()` implementados por `service.impl.PedidoVendaServiceImpl`; `service.AgendaService.buscar()` implementado por `service.impl.AgendaServiceImpl` | As interfaces definem contratos de aplicação independentes das implementações. |
| Interfaces | `dao.ContatoDAO.buscar()` implementado por `dao.impl.ContatoDAOImpl`; `dao.PedidoDAO.inserir()` implementado por `dao.impl.PedidoDAOImpl`; `util.ConnectionFactory.OperacaoTransacional.executar()` | Contratos DAO separam persistência da orquestração; a interface funcional recebe operações executadas em transação. |
| Exceções personalizadas | `model.Produto.exigirDisponivelParaVenda()` lança `exception.ProdutoIndisponivelException`; `removerEstoque()` lança `exception.EstoqueInsuficienteException` | Distingue o bloqueio de venda da falta de saldo para movimentações físicas. |
| Exceções personalizadas | `service.impl.AutenticacaoServiceImpl.login()` lança `exception.AutenticacaoInvalidaException`; `controller.LoginController.executar()` a trata | Credenciais inválidas ou conta inativa resultam em mensagem amigável. |
| Exceções personalizadas | `service.impl.AvaliacaoServiceImpl.salvar()` lança `exception.AvaliacaoNaoPermitidaException`; `controller.ClienteVitrineController.avaliar()` a trata | Uma avaliação é negada sem pedido entregue elegível. |
| Exceções personalizadas | `util.ConnectionFactory.executarEmTransacao()` encapsula `SQLException` em `exception.PersistenciaException` | Erros SQL transacionais são propagados com a causa original após tentativa de rollback. |

Os prefixos da tabela são relativos a `com.salgaderia.app`.

## 📂 Organização do Projeto

A árvore mostra todos os arquivos versionados e os guias desta documentação. Credenciais locais, arquivos ignorados e artefatos de compilação não fazem parte do inventário.

### Guias navegáveis

| Diretório | Guia |
|---|---|
| `database/` | [Abrir README](database/README.md) |
| `database/scripts/` | [Abrir README](database/scripts/README.md) |
| `docs/` | [Abrir README](docs/README.md) |
| `docs/ui-ux/` | [Abrir README](docs/ui-ux/README.md) |
| `resources/` | [Abrir README](resources/README.md) |
| `src/` | [Abrir README](src/README.md) |
| `src/main/` | [Abrir README](src/main/README.md) |
| `src/main/java/` | [Abrir README](src/main/java/README.md) |
| `src/main/java/com/` | [Abrir README](src/main/java/com/README.md) |
| `src/main/java/com/salgaderia/` | [Abrir README](src/main/java/com/salgaderia/README.md) |
| `src/main/java/com/salgaderia/app/` | [Abrir README](src/main/java/com/salgaderia/app/README.md) |
| `src/main/java/com/salgaderia/app/controller/` | [Abrir README](src/main/java/com/salgaderia/app/controller/README.md) |
| `src/main/java/com/salgaderia/app/dao/` | [Abrir README](src/main/java/com/salgaderia/app/dao/README.md) |
| `src/main/java/com/salgaderia/app/dao/impl/` | [Abrir README](src/main/java/com/salgaderia/app/dao/impl/README.md) |
| `src/main/java/com/salgaderia/app/exception/` | [Abrir README](src/main/java/com/salgaderia/app/exception/README.md) |
| `src/main/java/com/salgaderia/app/model/` | [Abrir README](src/main/java/com/salgaderia/app/model/README.md) |
| `src/main/java/com/salgaderia/app/model/comanda/` | [Abrir README](src/main/java/com/salgaderia/app/model/comanda/README.md) |
| `src/main/java/com/salgaderia/app/model/enums/` | [Abrir README](src/main/java/com/salgaderia/app/model/enums/README.md) |
| `src/main/java/com/salgaderia/app/service/` | [Abrir README](src/main/java/com/salgaderia/app/service/README.md) |
| `src/main/java/com/salgaderia/app/service/cozinha/` | [Abrir README](src/main/java/com/salgaderia/app/service/cozinha/README.md) |
| `src/main/java/com/salgaderia/app/service/desconto/` | [Abrir README](src/main/java/com/salgaderia/app/service/desconto/README.md) |
| `src/main/java/com/salgaderia/app/service/impl/` | [Abrir README](src/main/java/com/salgaderia/app/service/impl/README.md) |
| `src/main/java/com/salgaderia/app/service/pix/` | [Abrir README](src/main/java/com/salgaderia/app/service/pix/README.md) |
| `src/main/java/com/salgaderia/app/util/` | [Abrir README](src/main/java/com/salgaderia/app/util/README.md) |
| `src/main/resources/` | [Abrir README](src/main/resources/README.md) |
| `src/main/resources/css/` | [Abrir README](src/main/resources/css/README.md) |
| `src/main/resources/fxml/` | [Abrir README](src/main/resources/fxml/README.md) |
| `src/test/` | [Abrir README](src/test/README.md) |
| `src/test/java/` | [Abrir README](src/test/java/README.md) |
| `src/test/java/com/` | [Abrir README](src/test/java/com/README.md) |
| `src/test/java/com/salgaderia/` | [Abrir README](src/test/java/com/salgaderia/README.md) |
| `src/test/java/com/salgaderia/app/` | [Abrir README](src/test/java/com/salgaderia/app/README.md) |
| `src/test/java/com/salgaderia/app/controller/` | [Abrir README](src/test/java/com/salgaderia/app/controller/README.md) |
| `src/test/java/com/salgaderia/app/service/` | [Abrir README](src/test/java/com/salgaderia/app/service/README.md) |
| `src/test/java/com/salgaderia/app/service/pix/` | [Abrir README](src/test/java/com/salgaderia/app/service/pix/README.md) |
| `support/` | [Abrir README](support/README.md) |
| `uploads/` | [Abrir README](uploads/README.md) |

### Árvore completa comentada

Os comentários descrevem a responsabilidade de cada item.

```text
03-ui-kfomi/  # Raiz do repositório Ui K'Fomi
├── database/  # Modelos, schema e migrações para o MySQL.
│   ├── DER/  # Modelo entidade-relacionamento e seus materiais.
│   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   └── modelo-entidade-relacionamento.md  # Diagrama entidade-relacionamento Mermaid.
│   ├── DL/  # Modelo lógico das tabelas e relações.
│   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   └── modelo-logico.md  # Modelo lógico do banco.
│   ├── scripts/  # Schema e migrações SQL ordenadas pela evolução do sistema.
│   │   ├── etapa10_pagamento_vitrine.sql  # Amplia forma de pagamento para cartão genérico.
│   │   ├── etapa11_salao_agendamento.sql  # Migração de agendamento, mesas, comandas e pagamentos parciais.
│   │   ├── etapa12_combo_avaliacoes.sql  # Acrescenta a ficha do catálogo, personalização dos itens e caminhos das fotos de avaliações.
│   │   ├── etapa13_clube_gamificacao.sql  # Cria planos, assinaturas, mensalidades, conquistas e histórico da roleta; habilita cupons exclusivos.
│   │   ├── etapa14_pix_roleta_vip.sql  # Migra estados de pagamento, referências PIX, cupons com validade e prêmios VIP.
│   │   ├── etapa15_operacao_loja.sql  # Adiciona expediente da loja, bloqueio de venda, baixa física e saída para entrega.
│   │   ├── etapa16_cadastro_caixa_clube_vip.sql  # Migra o cadastro por e-mail, os filtros da caixa e o plano VIP único.
│   │   ├── etapa17_personalizacao_entrega.sql  # Cria opcionais e registra categorias ativas, instruções do pedido e mínimo de entrega.
│   │   ├── etapa4_migracao.sql  # Migração de contatos, usuários e custos históricos.
│   │   ├── etapa8_pdv.sql  # Colunas de origem e pagamento do PDV.
│   │   ├── etapa9_descontos.sql  # Cupons e registro de descontos por pedido.
│   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   └── schema.sql  # Schema inicial MySQL usado pelo respectivo repositório.
│   └── README.md  # Guia de navegação e explicações desta pasta.
├── docs/  # Documentação, diagramas, interfaces e apresentações.
│   ├── diagrams/  # Espaço para diagramas adicionais.
│   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── presentations/  # Espaço reservado para slides e apresentações.
│   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── ui-ux/  # Artefatos de experiência do usuário.
│   │   ├── mockups/  # Espaço reservado para mockups.
│   │   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   ├── prototypes/  # Espaço reservado para protótipos.
│   │   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   ├── wireframes/  # Espaço reservado para wireframes.
│   │   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   ├── uml/  # Materiais para diagramas de classes e modelagem UML.
│   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   └── diagrama-de-classes.md  # Diagrama UML de classes Mermaid.
│   ├── etapa4-persistencia.md  # Notas de persistência e migração.
│   └── README.md  # Guia de navegação e explicações desta pasta.
├── resources/  # Recursos institucionais ou recursos JavaFX, conforme a localização.
│   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   └── README.md  # Guia de navegação e explicações desta pasta.
├── src/  # Fontes, recursos de execução e testes do projeto.
│   ├── main/  # Aplicação principal e seus recursos.
│   │   ├── java/  # Pacotes de código Java.
│   │   │   ├── com/  # Raiz de pacotes Java.
│   │   │   │   ├── salgaderia/  # Pacote da aplicação Salgaderia.
│   │   │   │   │   ├── app/  # Componentes da aplicação e suas camadas.
│   │   │   │   │   │   ├── controller/  # Controllers das telas e testes de interface, conforme a localização.
│   │   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   │   ├── AdminDashboardController.java  # Coordena PDV, expediente, despacho, catálogo e confirmação do Clube VIP.
│   │   │   │   │   │   │   ├── AgendaController.java  # Interface da agenda de clientes e fornecedores.
│   │   │   │   │   │   │   ├── ClienteVitrineController.java  # Coordena perfil, vitrine, combos, caixas por orçamento, checkout e Clube VIP.
│   │   │   │   │   │   │   ├── LoginController.java  # Autentica contas e coordena o cadastro com e-mail e senha.
│   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   └── SelecaoModulosController.java  # Seleção de módulos administrativos.
│   │   │   │   │   │   ├── dao/  # Contratos de acesso a dados ou testes JDBC.
│   │   │   │   │   │   │   ├── impl/  # Implementações JDBC dos contratos DAO, com comandos parametrizados.
│   │   │   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   │   │   ├── AvaliacaoDAOImpl.java  # Persiste avaliações e fotos e consulta a galeria e dados de moderação.
│   │   │   │   │   │   │   │   ├── CategoriaDAOImpl.java  # Operações JDBC de cadastro e consulta de categorias.
│   │   │   │   │   │   │   │   ├── ClubeDAOImpl.java  # Persiste assinaturas VIP, vigências e referências das mensalidades atuais e antigas.
│   │   │   │   │   │   │   │   ├── ComandaDAOImpl.java  # Implementa operações JDBC de mesas e comandas com bloqueio de linha.
│   │   │   │   │   │   │   │   ├── ConfiguracaoLojaDAOImpl.java  # Persiste expediente e estado manual separadamente com JDBC.
│   │   │   │   │   │   │   │   ├── ContatoDAOImpl.java  # Persiste contatos e atualiza dados do cliente sem sobrescrever os demais campos da agenda.
│   │   │   │   │   │   │   │   ├── CupomDAOImpl.java  # Consulta elegibilidade, reserva cupons exclusivos e os devolve no cancelamento.
│   │   │   │   │   │   │   │   ├── GamificacaoDAOImpl.java  # Persiste selos, giros, resgates e trava de 24 horas usando o relógio do banco.
│   │   │   │   │   │   │   │   ├── MovimentacaoEstoqueDAOImpl.java  # Grava movimentações de estoque e consulta perdas por período via JDBC.
│   │   │   │   │   │   │   │   ├── PedidoDAOImpl.java  # Persiste preços, exclusões da caixa, baixa física e despacho; consulta a fila e o histórico.
│   │   │   │   │   │   │   │   ├── PixPagamentoDAOImpl.java  # Consulta pagamentos e vincula tentativas, IDs remotos e aprovação ao pedido em JDBC.
│   │   │   │   │   │   │   │   ├── ProdutoDAOImpl.java  # Persiste catálogo, disponibilidade de venda, estoque e fichas de personalização.
│   │   │   │   │   │   │   │   ├── ProdutoOpcionalDAOImpl.java  # Consulta e sincroniza os opcionais de cada produto via JDBC.
│   │   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   │   └── UsuarioDAOImpl.java  # Persiste login, e-mail e apelido e bloqueia a conta ativa nas transações.
│   │   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   │   ├── AvaliacaoDAO.java  # Contrato JDBC para avaliações, médias, galeria de fotos e moderação.
│   │   │   │   │   │   │   ├── CategoriaDAO.java  # Contrato para cadastrar, atualizar, buscar, listar e excluir categorias.
│   │   │   │   │   │   │   ├── ClubeDAO.java  # Define consultas do plano VIP, vigências e pagamentos de mensalidade.
│   │   │   │   │   │   │   ├── ComandaDAO.java  # Contrato JDBC para mesas, itens, pagamentos e fechamento transacional.
│   │   │   │   │   │   │   ├── ConfiguracaoLojaDAO.java  # Define leitura e gravação da configuração operacional única da loja.
│   │   │   │   │   │   │   ├── ContatoDAO.java  # Contrato para cadastro, edição, exclusão e busca filtrada de contatos.
│   │   │   │   │   │   │   ├── CupomDAO.java  # Contrato de consulta, reserva e devolução de cupons com uso limitado.
│   │   │   │   │   │   │   ├── GamificacaoDAO.java  # Contrato para selos, elegibilidade por pagamento, prêmios, prazos e pontos.
│   │   │   │   │   │   │   ├── MovimentacaoEstoqueDAO.java  # Contrato para registrar movimentações e calcular perdas por período.
│   │   │   │   │   │   │   ├── PedidoDAO.java  # Define operações transacionais de pedidos, despacho, relatórios e baixa física.
│   │   │   │   │   │   │   ├── PixPagamentoDAO.java  # Contrato de persistência de cobranças, conciliação e status de pagamento.
│   │   │   │   │   │   │   ├── ProdutoDAO.java  # Define cadastro, consulta, bloqueio de venda e atualização de estoque físico.
│   │   │   │   │   │   │   ├── ProdutoOpcionalDAO.java  # Define consulta e gravação dos opcionais do catálogo.
│   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   └── UsuarioDAO.java  # Define a persistência da identidade, credenciais e apelido das contas.
│   │   │   │   │   │   ├── exception/  # Exceções personalizadas de domínio e persistência.
│   │   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   │   ├── AutenticacaoInvalidaException.java  # Falha de autenticação.
│   │   │   │   │   │   │   ├── AvaliacaoNaoPermitidaException.java  # Avaliação sem compra elegível.
│   │   │   │   │   │   │   ├── EstoqueInsuficienteException.java  # Indica saldo físico insuficiente para uma movimentação de estoque.
│   │   │   │   │   │   │   ├── PersistenciaException.java  # Encapsula erro de persistência.
│   │   │   │   │   │   │   ├── ProdutoIndisponivelException.java  # Indica que um produto foi bloqueado para venda, independentemente do estoque.
│   │   │   │   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   ├── model/  # Entidades de domínio ou testes dessas entidades.
│   │   │   │   │   │   │   ├── comanda/  # Entidades de mesa, comanda, itens e pagamentos parciais.
│   │   │   │   │   │   │   │   ├── Comanda.java  # Sessão da mesa e cálculo monetário de total, pagamentos e saldo.
│   │   │   │   │   │   │   │   ├── ItemComanda.java  # Item lançado na comanda, com quantidade disponível e cotação por unidade.
│   │   │   │   │   │   │   │   ├── Mesa.java  # Identificação, número e status da mesa.
│   │   │   │   │   │   │   │   ├── PagamentoComanda.java  # Dados de pagamento parcial por valor ou item.
│   │   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   │   └── StatusMesa.java  # Estados livre e ocupada e suas permissões.
│   │   │   │   │   │   │   ├── enums/  # Enumerações com comportamento e respectivos testes.
│   │   │   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   │   │   ├── CategoriaInsumo.java  # Categorias de insumos de fornecedores.
│   │   │   │   │   │   │   │   ├── FormaPagamento.java  # Formas de pagamento e regra de troco.
│   │   │   │   │   │   │   │   ├── PerfilUsuario.java  # Perfis de acesso do usuário.
│   │   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   │   ├── StatusPagamento.java  # Distingue PIX pendente, pagamento confirmado e cobrança na entrega.
│   │   │   │   │   │   │   │   ├── StatusPedido.java  # Define as transições por modalidade, incluindo saída exclusiva para entrega.
│   │   │   │   │   │   │   │   ├── TipoEntrega.java  # Modalidades de entrega.
│   │   │   │   │   │   │   │   └── TipoMovimentacaoEstoque.java  # Tipos de movimentação de estoque.
│   │   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   │   ├── AssinaturaCliente.java  # Representa o plano do cliente, o desconto vigente e o estado da assinatura.
│   │   │   │   │   │   │   ├── Avaliacao.java  # Registra nota, comentário, resposta oficial e referência opcional da foto.
│   │   │   │   │   │   │   ├── AvaliacaoFoto.java  # Projeção pública de fotos de avaliações que não expõe dados da conta.
│   │   │   │   │   │   │   ├── AvaliacaoResumo.java  # Projeção de avaliação com comentário, resposta oficial e foto para moderação.
│   │   │   │   │   │   │   ├── Categoria.java  # Categoria de produtos.
│   │   │   │   │   │   │   ├── Cliente.java  # Contato especializado em cliente.
│   │   │   │   │   │   │   ├── ConfiguracaoLoja.java  # Representa expediente, modo automático, chave manual e mínimo para entrega.
│   │   │   │   │   │   │   ├── ConquistaCliente.java  # Representa um selo e a data em que foi desbloqueado.
│   │   │   │   │   │   │   ├── Contato.java  # Abstração comum de contatos.
│   │   │   │   │   │   │   ├── CupomRoleta.java  # Projeção de código de resgate com benefício, mínimo e data de expiração.
│   │   │   │   │   │   │   ├── FichaVitrine.java  # Preserva campos legados do catálogo e identifica categorias de salgados.
│   │   │   │   │   │   │   ├── Fornecedor.java  # Contato especializado em fornecedor.
│   │   │   │   │   │   │   ├── MensalidadeClube.java  # Cotação imutável de valor e desconto de uma mensalidade pendente.
│   │   │   │   │   │   │   ├── MovimentacaoEstoque.java  # Registro de entrada ou saída de estoque.
│   │   │   │   │   │   │   ├── Pedido.java  # Mantém itens, total, frete, pagamento, status e liberação da roleta.
│   │   │   │   │   │   │   ├── PedidoFila.java  # Projeção da fila com modalidade, pagamento e instruções de preparo.
│   │   │   │   │   │   │   ├── PedidoItem.java  # Registra quantidade, preço histórico, opcionais e observações do item.
│   │   │   │   │   │   │   ├── PersonalizacaoItem.java  # Identifica o grupo, a faixa de orçamento e as exclusões da caixa; preserva os tipos antigos.
│   │   │   │   │   │   │   ├── PlanoAssinatura.java  # Modela os termos de um plano e define o nome, a mensalidade e o desconto do VIP.
│   │   │   │   │   │   │   ├── PremioRoleta.java  # Define fatias comuns/VIP e registra resultado, prazo e giro extra.
│   │   │   │   │   │   │   ├── Produto.java  # Representa preço, custos, saldo físico, disponibilidade de venda e ficha do catálogo.
│   │   │   │   │   │   │   ├── ProdutoOpcional.java  # Representa uma borda ou adicional e suas regras de preço e quantidade.
│   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   ├── Usuario.java  # Abstração de usuário e permissões.
│   │   │   │   │   │   │   ├── UsuarioAdmin.java  # Usuário administrativo.
│   │   │   │   │   │   │   ├── UsuarioCliente.java  # Representa a conta de cliente vinculada ao contato, com e-mail para novos cadastros.
│   │   │   │   │   │   │   └── Validacao.java  # Validações compartilhadas das entidades.
│   │   │   │   │   │   ├── service/  # Contratos, regras de negócio e testes de serviços.
│   │   │   │   │   │   │   ├── cozinha/  # Estados do termômetro visual de preparo dos pedidos.
│   │   │   │   │   │   │   │   ├── EstadoCozinha.java  # Contrato State de mensagem, progresso e espera.
│   │   │   │   │   │   │   │   ├── EtapaCozinha.java  # Mapeia os status aos estados visuais de preparo, saída em rota e conclusão.
│   │   │   │   │   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   ├── desconto/  # Estratégias de desconto e testes correspondentes.
│   │   │   │   │   │   │   │   ├── CalculadoraDescontos.java  # Mantém caixas no preço fechado, isenta frete VIP e escolhe o melhor benefício elegível.
│   │   │   │   │   │   │   │   ├── ContextoDesconto.java  # Agrupa itens, cupom, frete e fidelidade e separa mercadorias elegíveis para descontos.
│   │   │   │   │   │   │   │   ├── Cupom.java  # Representa percentual, frete, brinde ou bônus vinculado ao cupom.
│   │   │   │   │   │   │   │   ├── DescontoAssinante.java  # Calcula o desconto do membro VIP nos itens elegíveis.
│   │   │   │   │   │   │   │   ├── DescontoCaixaMisteriosa.java  # Ajusta o valor de catálogo de cada caixa ao preço fechado da faixa escolhida.
│   │   │   │   │   │   │   │   ├── DescontoCentoSalgados.java  # Estratégia de desconto para cento de salgados.
│   │   │   │   │   │   │   │   ├── DescontoCombo.java  # Calcula 10% de desconto sobre os itens do combo simples.
│   │   │   │   │   │   │   │   ├── DescontoCupomPercentual.java  # Estratégia percentual por cupom.
│   │   │   │   │   │   │   │   ├── DescontoFidelidade.java  # Estratégia para clientes fiéis.
│   │   │   │   │   │   │   │   ├── EstrategiaDesconto.java  # Contrato Strategy para descontos.
│   │   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   │   └── ResultadoDesconto.java  # Informa total, desconto, frete efetivo, isenção VIP e eventual brinde.
│   │   │   │   │   │   │   ├── impl/  # Implementações JDBC dos contratos DAO, com comandos parametrizados.
│   │   │   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   │   │   ├── AgendaServiceImpl.java  # Valida e coordena cadastro, busca e alterações de contatos pela camada DAO.
│   │   │   │   │   │   │   │   ├── AutenticacaoServiceImpl.java  # Cria contas por e-mail em transação, gera hashes e atualiza apelido e dados de entrega.
│   │   │   │   │   │   │   │   ├── AvaliacaoServiceImpl.java  # Valida a entrega e coordena avaliações, upload e exclusão de fotos na moderação.
│   │   │   │   │   │   │   │   ├── ClubeServiceImpl.java  # Coordena pagamento pré-pago, vigência e renovação do plano VIP único.
│   │   │   │   │   │   │   │   ├── ComandaServiceImpl.java  # Orquestra estoque, validação do operador, cotação de pagamentos e fechamento atômico.
│   │   │   │   │   │   │   │   ├── GamificacaoServiceImpl.java  # Controla cooldown, separa sorteios de demonstração e aplica validade e giro extra.
│   │   │   │   │   │   │   │   ├── PedidoVendaServiceImpl.java  # Revalida custo, descontos VIP, disponibilidade, expediente e preços antes de gravar a venda.
│   │   │   │   │   │   │   │   ├── ProdutoEstoqueServiceImpl.java  # Gerencia disponibilidade, ajustes, lotes e perdas do estoque físico.
│   │   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   │   └── RelatorioFinanceiroServiceImpl.java  # Calcula indicadores financeiros e gera o balanço CSV.
│   │   │   │   │   │   │   ├── pix/  # Porta, adapters e conciliação da cobrança PIX na vitrine.
│   │   │   │   │   │   │   │   ├── LocalDemoPixGateway.java  # Gera um BR Code de demonstração sem pagamento ou confirmação externa.
│   │   │   │   │   │   │   │   ├── MercadoPagoPixGateway.java  # Traduz cobranças para JSON e usa HttpClient para chamar a API Payments com idempotência.
│   │   │   │   │   │   │   │   ├── PixConfiguracao.java  # Lê o token local do Mercado Pago e a taxa de entrega configurável.
│   │   │   │   │   │   │   │   ├── PixGatewayService.java  # Porta de criação e consulta das cobranças PIX do checkout.
│   │   │   │   │   │   │   │   ├── PixPagamentoService.java  # Concilia a resposta do gateway com a cobrança local, sem aprovar valores divergentes.
│   │   │   │   │   │   │   │   └── README.md  # Guia da integração PIX da vitrine.
│   │   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   │   ├── AgendaService.java  # Contrato de operações e busca da agenda de clientes e fornecedores.
│   │   │   │   │   │   │   ├── AutenticacaoService.java  # Define login, cadastro por e-mail e edição dos dados de perfil do cliente.
│   │   │   │   │   │   │   ├── AvaliacaoService.java  # Contrato para avaliações, médias, fotos e moderação administrativa.
│   │   │   │   │   │   │   ├── BarramentoPedido.java  # Notifica observadores na mesma transação JDBC do evento de pedido.
│   │   │   │   │   │   │   ├── ClubeService.java  # Define adesão ao plano VIP, renovação, cancelamento e confirmação da mensalidade.
│   │   │   │   │   │   │   ├── ComandaService.java  # Contrato para abertura, lançamento, rateio e pagamentos de comandas.
│   │   │   │   │   │   │   ├── ComboBuilder.java  # Monta um combo com principal, acompanhamento distinto e bebida do cardápio.
│   │   │   │   │   │   │   ├── ConquistasObserver.java  # Avalia selos, libera a roleta após pagamento ou entrega e revoga prêmios cancelados.
│   │   │   │   │   │   │   ├── FotoAvaliacaoStorage.java  # Valida, redimensiona, salva e remove imagens locais de avaliações.
│   │   │   │   │   │   │   ├── GamificacaoService.java  # Contrato de selos, pontos, cupons, giro regular e giro extra.
│   │   │   │   │   │   │   ├── OperacaoLojaService.java  # Avalia a abertura e valida agendamentos pelo expediente local, inclusive após a meia-noite.
│   │   │   │   │   │   │   ├── PedidoCancelamentoService.java  # Devolve somente o saldo físico baixado e libera o cupom ao cancelar.
│   │   │   │   │   │   │   ├── PedidoConcluidoObserver.java  # Contrato Observer para finalização, confirmação PIX, entrega e cancelamento.
│   │   │   │   │   │   │   ├── PedidoVendaService.java  # Contrato para simular descontos, finalizar pedidos e alterar status de vendas.
│   │   │   │   │   │   │   ├── PersonalizacaoService.java  # Sorteia e revalida caixas por preço, custo e exclusões; valida a composição do combo.
│   │   │   │   │   │   │   ├── PixPayloadService.java  # Monta BR Code com CRC16 e gera a imagem Pix em memória com ZXing.
│   │   │   │   │   │   │   ├── ProdutoEstoqueService.java  # Define operações do catálogo, bloqueio de venda e movimentações do estoque físico.
│   │   │   │   │   │   │   ├── ProdutoPersonalizacaoService.java  # Valida grupos, calcula a franquia e monta o resumo para o preparo.
│   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   ├── RelatorioFinanceiroService.java  # Contrato de cálculos de balanço, gráficos e exportação CSV.
│   │   │   │   │   │   │   └── WhatsAppLinkService.java  # Gera links do WhatsApp e os abre e copia em segundo plano.
│   │   │   │   │   │   ├── util/  # Conexão, autenticação auxiliar e navegação.
│   │   │   │   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   │   │   │   ├── ConnectionFactory.java  # Pool HikariCP e transações JDBC.
│   │   │   │   │   │   │   ├── FotoAvaliacaoUi.java  # Carrega miniaturas JavaFX de fotos de avaliações guardadas localmente.
│   │   │   │   │   │   │   ├── Navegacao.java  # Navegação entre telas FXML.
│   │   │   │   │   │   │   ├── PagamentoPixUi.java  # Mostra QR Code e consulta o pagamento com Timeline, sem bloquear a interface.
│   │   │   │   │   │   │   ├── PixPainel.java  # Desenha o QR local ou do gateway e disponibiliza o código PIX para cópia.
│   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   ├── RoletaUi.java  # Anima o prêmio transacional em seis fatias regulares ou VIP, com giro extra quando disponível.
│   │   │   │   │   │   │   ├── Senhas.java  # Geração e verificação de hashes de senha.
│   │   │   │   │   │   │   ├── TelaAutenticada.java  # Contrato para telas com usuário autenticado.
│   │   │   │   │   │   │   └── WhatsAppUi.java  # Mostra na JavaFX a alternativa quando o WhatsApp não abre.
│   │   │   │   │   │   ├── Main.java  # Inicialização da aplicação JavaFX.
│   │   │   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   ├── resources/  # Recursos institucionais ou recursos JavaFX, conforme a localização.
│   │   │   ├── css/  # Folhas de estilo das telas JavaFX.
│   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   ├── admin.css  # Estilos do painel administrativo.
│   │   │   │   ├── agenda.css  # Estilos da agenda.
│   │   │   │   ├── cliente-vitrine.css  # Estiliza catálogo, checkout, disponibilidade e banner de entrega em rota.
│   │   │   │   ├── global.css  # Estilos compartilhados da aplicação.
│   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   ├── fxml/  # Descrições de interface JavaFX.
│   │   │   │   ├── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   │   ├── admin-dashboard.fxml  # Estrutura a navegação administrativa, incluindo expediente e confirmação do Clube VIP.
│   │   │   │   ├── agenda-contatos.fxml  # Estrutura visual da agenda.
│   │   │   │   ├── cliente-vitrine.fxml  # Estrutura vitrine, perfil, checkout, acompanhamento e Clube VIP.
│   │   │   │   ├── login.fxml  # Estrutura as telas de login e cadastro com e-mail e senha.
│   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   └── selecao-modulos.fxml  # Estrutura visual da seleção de módulos.
│   │   │   ├── icons/  # Espaço reservado para ícones.
│   │   │   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   ├── images/  # Espaço reservado para imagens.
│   │   │   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   │   │   ├── database.properties  # Template JDBC e propriedades opcionais de taxa de entrega e pagamentos PIX.
│   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   ├── test/  # Testes automatizados.
│   │   ├── java/  # Pacotes de código Java.
│   │   │   ├── com/  # Raiz de pacotes Java.
│   │   │   │   ├── salgaderia/  # Pacote da aplicação Salgaderia.
│   │   │   │   │   ├── app/  # Componentes da aplicação e suas camadas.
│   │   │   │   │   │   ├── controller/  # Controllers das telas e testes de interface, conforme a localização.
│   │   │   │   │   │   │   ├── AdminDashboardJavaFXTest.java  # Testa navegação administrativa, PDV, mesas, Pix e alertas de novos pedidos.
│   │   │   │   │   │   │   ├── AgendaJavaFXTest.java  # Testes JavaFX da agenda.
│   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   ├── TelasJavaFXTest.java  # Verifica cadastro com dois campos e navegação pelo perfil de acesso.
│   │   │   │   │   │   │   └── VitrineJavaFXTest.java  # Verifica vitrine, combo simplificado, exclusões da caixa, Clube VIP e rastreio de entrega.
│   │   │   │   │   │   ├── dao/  # Contratos de acesso a dados ou testes JDBC.
│   │   │   │   │   │   │   └── AgendaJDBCTest.java  # Teste de integração JDBC da agenda.
│   │   │   │   │   │   ├── model/  # Entidades de domínio ou testes dessas entidades.
│   │   │   │   │   │   │   ├── enums/  # Enumerações com comportamento e respectivos testes.
│   │   │   │   │   │   │   │   └── EnumsTest.java  # Testes de comportamento dos enums.
│   │   │   │   │   │   │   └── DominioTest.java  # Testes das entidades e regras de domínio.
│   │   │   │   │   │   ├── service/  # Contratos, regras de negócio e testes de serviços.
│   │   │   │   │   │   │   ├── desconto/  # Estratégias de desconto e testes correspondentes.
│   │   │   │   │   │   │   │   └── EstrategiasDescontoTest.java  # Testa as estratégias e a escolha do melhor desconto.
│   │   │   │   │   │   │   ├── pix/  # Testes HTTP em loopback para a integração PIX.
│   │   │   │   │   │   │   │   ├── MercadoPagoPixGatewayTest.java  # Verifica a integração HTTP com um servidor local, sem emitir cobranças.
│   │   │   │   │   │   │   │   └── README.md  # Guia dos testes HTTP locais.
│   │   │   │   │   │   │   ├── ClubeGamificacaoIntegracaoTest.java  # Valida o VIP único, mensalidades antigas, frete, descontos, roleta e PIX no MySQL.
│   │   │   │   │   │   │   ├── ComandaIntegracaoTest.java  # Valida operações de mesa, pagamentos concorrentes, baixa e fechamento no MySQL.
│   │   │   │   │   │   │   ├── CozinhaStateTest.java  # Verifica mensagens e progresso dos estados da cozinha.
│   │   │   │   │   │   │   ├── DescontoIntegracaoTest.java  # Integração de descontos e pagamentos no MySQL.
│   │   │   │   │   │   │   ├── Etapa4IntegracaoTest.java  # Verifica cadastro por e-mail e transações de estoque, vendas, avaliações e financeiro no MySQL.
│   │   │   │   │   │   │   ├── FotoAvaliacaoStorageTest.java  # Testa a cópia validada, limites, caminhos locais e remoção de anexos.
│   │   │   │   │   │   │   ├── OperacaoLojaServiceTest.java  # Testa os limites do expediente e a combinação dos modos manual e automático.
│   │   │   │   │   │   │   ├── OperacaoV162IntegracaoTest.java  # Valida configuração persistida, agendamento, venda sob demanda, bloqueio e despacho no MySQL.
│   │   │   │   │   │   │   ├── PedidoAgendamentoTest.java  # Verifica rejeição de agendamento passado.
│   │   │   │   │   │   │   ├── PersonalizacaoServiceTest.java  # Verifica as três faixas da caixa, o teto de custo e a composição do combo.
│   │   │   │   │   │   │   ├── PixPayloadServiceTest.java  # Valida valor, referência e CRC16 e decodifica a imagem QR que foi gerada.
│   │   │   │   │   │   │   ├── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   │   │   ├── RelatorioCsvTest.java  # Verifica o balanço exportado em CSV.
│   │   │   │   │   │   │   ├── VendaTeste.java  # Fornece cadastro isolado, relógio fixo e configuração de loja para testes de venda.
│   │   │   │   │   │   │   ├── VitrineV150IntegracaoTest.java  # Valida persistência de exclusões e rechecagem de custos, preços e combos antes da venda.
│   │   │   │   │   │   │   ├── VitrineV170IntegracaoTest.java  # Valida categorias, opcionais, mínimo e cancelamento no MySQL.
│   │   │   │   │   │   │   └── WhatsAppLinkServiceTest.java  # Testa links, mensagens, configuração e abertura assíncrona do WhatsApp.
│   │   │   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   │   └── README.md  # Guia de navegação e explicações desta pasta.
│   └── README.md  # Guia de navegação e explicações desta pasta.
├── support/  # Documentação de apoio, referências e materiais complementares.
│   ├── documents/  # Espaço para documentos de apoio.
│   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── references/  # Espaço para referências.
│   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── tutorials/  # Espaço para tutoriais.
│   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   ├── videos/  # Espaço para materiais em vídeo.
│   │   └── .gitkeep  # Marcador que mantém a pasta no controle de versão; não contém dados de aplicação.
│   └── README.md  # Guia de navegação e explicações desta pasta.
├── uploads/  # Armazena os anexos locais fora do código-fonte.
│   ├── avaliacoes/  # Guarda as fotos localmente; apenas o marcador é versionado.
│   │   └── .gitkeep  # Preserva a pasta vazia no repositório.
│   └── README.md  # Guia de armazenamento, privacidade e backup das fotos de avaliações.
├── .gitignore  # Ignora arquivos de configuração local, saídas de compilação e fotos de clientes.
├── AGENTS.md  # Instruções locais aplicáveis aos agentes no repositório.
├── CLAUDE.md  # Orientações adicionais sobre o projeto.
├── LICENSE  # Texto da licença MIT.
├── pom.xml  # Configuração Maven, dependências e build do módulo.
└── README.md  # Guia de navegação e explicações desta pasta.
```

## Diagramas

- [Modelo entidade-relacionamento](database/DER/modelo-entidade-relacionamento.md)
- [Modelo lógico do banco](database/DL/modelo-logico.md)
- [Diagrama de classes UML](docs/uml/diagrama-de-classes.md)

Os diagramas são blocos Mermaid em Markdown; use um visualizador que suporte Mermaid.

## Testes

```bash
mvn test
mvn clean test-compile
```

`test-compile` compila os testes, mas **não os executa**. Testes opcionais exigem recursos locais:

```bash
# Testes padrão e integração com MySQL local, sem exigir sessão gráfica
SALGADERIA_TESTE_MYSQL=true mvn clean test

# Servidor gráfico disponível
SALGADERIA_TESTE_JAVAFX=true mvn -Dtest=TelasJavaFXTest,AgendaJavaFXTest,VitrineJavaFXTest,AdminDashboardJavaFXTest test

# Suíte completa com ambos os recursos locais disponíveis
SALGADERIA_TESTE_MYSQL=true SALGADERIA_TESTE_JAVAFX=true mvn clean test
```

Os testes MySQL se conectam diretamente ao `salgaderia_db` de desenvolvimento em `localhost:3306` com a configuração local do projeto, sem iniciar um servidor temporário. Deixe o banco migrado e em execução; os testes criam e limpam registros temporários. Nunca use dados de produção.
O teste do adapter usa um servidor HTTP em loopback. A suíte não acessa o Mercado Pago nem cria cobranças reais, mesmo que exista token local.

## Licença

Distribuído sob a [licença MIT](LICENSE).
