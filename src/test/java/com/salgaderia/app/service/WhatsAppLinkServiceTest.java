package com.salgaderia.app.service;

import com.salgaderia.app.model.*;
import com.salgaderia.app.model.enums.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WhatsAppLinkServiceTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path diretorio;

    @Test void aberturaNaoBloqueiaChamadorECopiaLinkMesmoQuandoNavegadorAbre() throws Exception {
        URI link = new WhatsAppLinkService.Builder("5585989095223").linha("Olá & bom dia!").construir();
        Thread chamador = Thread.currentThread();
        CountDownLatch iniciou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        AtomicReference<String> copiado = new AtomicReference<>();
        var resultado = WhatsAppLinkService.abrir(link, uri -> {
            assertNotSame(chamador, Thread.currentThread());
            assertEquals(link, uri);
            iniciou.countDown();
            try {
                assertTrue(liberar.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }, copiado::set);
        try {
            assertTrue(iniciou.await(5, TimeUnit.SECONDS));
            assertFalse(resultado.isDone());
            assertEquals(link.toASCIIString(), copiado.get());
        } finally {
            liberar.countDown();
        }
        assertEquals(WhatsAppLinkService.ResultadoAbertura.ABERTO, resultado.get(5, TimeUnit.SECONDS));
    }

    @Test void informaCopiaOuAlternativaManualQuandoNavegadorFalha() throws Exception {
        URI link = URI.create("https://wa.me/5585989095223");
        java.util.function.Consumer<URI> indisponivel = uri -> { throw new UnsupportedOperationException(); };
        java.util.function.Consumer<String> clipboardIndisponivel = texto -> { throw new IllegalStateException(); };
        assertEquals(WhatsAppLinkService.ResultadoAbertura.COPIADO,
                WhatsAppLinkService.abrir(link, indisponivel, texto -> {}).get(5, TimeUnit.SECONDS));
        assertEquals(WhatsAppLinkService.ResultadoAbertura.INDISPONIVEL,
                WhatsAppLinkService.abrir(link, indisponivel, clipboardIndisponivel).get(5, TimeUnit.SECONDS));
        assertEquals(WhatsAppLinkService.ResultadoAbertura.ABERTO,
                WhatsAppLinkService.abrir(link, uri -> {}, clipboardIndisponivel).get(5, TimeUnit.SECONDS));
    }

    @Test void rejeitaUrisQueNaoApontamParaUmaPaginaWeb() {
        var servico = new WhatsAppLinkService();
        assertThrows(IllegalArgumentException.class, () -> servico.abrir(URI.create("file:///etc/passwd")));
        assertThrows(IllegalArgumentException.class, () -> servico.abrir(URI.create("https:sem-host")));
    }

    @Test void configuracaoLocalSobrescrevePadrao() throws Exception {
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{diretorio.toUri().toURL()},null)) {
            assertEquals("5585989095223",WhatsAppLinkService.carregarTelefoneLoja(loader));
            java.nio.file.Files.writeString(diretorio.resolve("database.local.properties"),"whatsapp.loja=5585111112222\n");
            assertEquals("5585111112222",WhatsAppLinkService.carregarTelefoneLoja(loader));
        }
    }
    @Test void builderCodificaTextoEValidaTelefone() {
        var uri=new WhatsAppLinkService.Builder("(85) 99999-9999").linha("Olá & bom dia!").linha("Pronto?").construir();
        assertEquals("wa.me",uri.getHost());
        assertEquals("/5585999999999",uri.getPath());
        assertEquals("text=Olá & bom dia!\nPronto?",URLDecoder.decode(uri.getRawQuery(),StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class,()->new WhatsAppLinkService.Builder("123").construir());
    }
    @Test void mensagemDoPedidoUsaValorRealEDadosDaEntrega() {
        Produto produto=new Produto(1L,null,"Coxinha & queijo",null,new BigDecimal("10"),BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ONE,null);
        Pedido pedido=new Pedido(5L,null,null,StatusPedido.PENDENTE,TipoEntrega.ENTREGA,"Rua São José",LocalDateTime.now(),List.of(new PedidoItem(null,produto,new BigDecimal("2"),BigDecimal.TEN)));
        pedido.setDesconto(new BigDecimal("2"));
        pedido.setFormaPagamento(FormaPagamento.PIX);
        var link=new WhatsAppLinkService("5585989095223").acompanhar(pedido);
        String msg=URLDecoder.decode(link.getRawQuery(),StandardCharsets.UTF_8);
        assertEquals("/5585989095223",link.getPath());assertTrue(msg.contains("#5"));assertTrue(msg.contains("2 x Coxinha & queijo"));
        assertTrue(msg.contains("Rua São José"));assertTrue(msg.contains("Pagamento: PIX"));assertTrue(msg.contains("18,00"));
        pedido.setFormaPagamento(null);
        assertTrue(URLDecoder.decode(new WhatsAppLinkService("5585989095223").acompanhar(pedido).getRawQuery(),StandardCharsets.UTF_8).contains("Pagamento: não informado"));
    }
    @Test void statusUsaTelefoneDoClienteEStatusInformado() {
        String query=URLDecoder.decode(new WhatsAppLinkService().status("5585988887777",42,StatusPedido.CANCELADO).getRawQuery(),StandardCharsets.UTF_8);
        assertTrue(query.contains("#42"));assertTrue(query.contains("CANCELADO"));
    }
    @Test void despachoIncluiNomeETelefoneDoCliente() {
        var uri=new WhatsAppLinkService().status("(85) 98888-7777",42,StatusPedido.SAIU_ENTREGA,"José & Ana");
        assertEquals("/5585988887777",uri.getPath());
        String texto=URLDecoder.decode(uri.getRawQuery(),StandardCharsets.UTF_8);
        assertTrue(texto.contains("Seu pedido #42 saiu para entrega!"));
        assertTrue(texto.contains("Entregador: José & Ana"));
    }
    @Test void acompanhamentoNaoRevelaOsItensDaCaixaAntesDaEntrega() {
        Produto produto=new Produto(1L,new Categoria(1L,"Assados",null),"Empada da casa",null,new BigDecimal("5.00"),BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ONE,null);
        List<PedidoItem> itens=new PersonalizacaoService().sortearCaixa(PersonalizacaoItem.Tipo.CAIXA_25,List.of(produto),Set.of(),Set.of());
        Pedido pedido=new Pedido(99L,null,null,StatusPedido.PRONTO,TipoEntrega.RETIRADA,null,LocalDateTime.now(),itens);
        var whatsapp=new WhatsAppLinkService();
        String mensagem=URLDecoder.decode(whatsapp.acompanhar(pedido).getRawQuery(),StandardCharsets.UTF_8);
        assertTrue(mensagem.contains("R$ 25,00"));
        assertFalse(mensagem.contains("Empada da casa"));
        pedido.transicionarPara(StatusPedido.ENTREGUE);
        assertTrue(URLDecoder.decode(whatsapp.acompanhar(pedido).getRawQuery(),StandardCharsets.UTF_8).contains("Empada da casa"));
    }
}
