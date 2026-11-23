package com.salgaderia.app.service;

import com.salgaderia.app.model.Pedido;
import com.salgaderia.app.model.PedidoItem;
import com.salgaderia.app.model.enums.StatusPedido;
import java.awt.AWTError;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Cria links wa.me; abrir o link não envia mensagens automaticamente. */
public final class WhatsAppLinkService {
    public static final String LOJA = "5585989095223";
    private final String telefoneLoja;
    public WhatsAppLinkService() { this(carregarTelefoneLoja(WhatsAppLinkService.class.getClassLoader())); }
    public WhatsAppLinkService(String telefoneLoja) { this.telefoneLoja = normalizarTelefone(telefoneLoja); }

    static String carregarTelefoneLoja(ClassLoader loader) {
        try (var arquivo=loader.getResourceAsStream("database.local.properties")) {
            if(arquivo==null)return LOJA;
            var propriedades=new java.util.Properties();propriedades.load(arquivo);
            String telefone=propriedades.getProperty("whatsapp.loja");
            return telefone==null||telefone.isBlank()?LOJA:telefone;
        } catch(IOException e) { throw new IllegalStateException("Não foi possível ler a configuração do WhatsApp",e); }
    }
    public enum ResultadoAbertura { ABERTO, COPIADO, INDISPONIVEL }

    public static String normalizarTelefone(String numero) {
        if (numero == null || numero.isBlank()) throw new IllegalArgumentException("Telefone não cadastrado");
        String digitos = numero.replaceAll("[^0-9]", "");
        if (digitos.length() == 10 || digitos.length() == 11) digitos = "55" + digitos;
        if (!digitos.matches("[1-9][0-9]{9,14}")) throw new IllegalArgumentException("Telefone inválido: informe DDD e número");
        return digitos;
    }

    public static final class Builder {
        private final String telefone;
        private final StringBuilder texto = new StringBuilder();
        public Builder(String telefone) { this.telefone = normalizarTelefone(telefone); }
        public Builder linha(String linha) {
            if (texto.length() > 0) texto.append('\n');
            texto.append(Objects.requireNonNull(linha));
            return this;
        }
        public URI construir() {
            return URI.create("https://wa.me/" + telefone + "?text=" + URLEncoder.encode(texto.toString(), StandardCharsets.UTF_8).replace("+", "%20"));
        }
    }

    public URI acompanhar(Pedido pedido) {
        if (pedido.getId() == null) throw new IllegalArgumentException("Pedido ainda não registrado");
        Builder builder = new Builder(telefoneLoja).linha("Olá! Gostaria de acompanhar o pedido #" + pedido.getId() + ".").linha("Itens:");
        Set<UUID> caixas = new HashSet<>();
        for (PedidoItem item : pedido.getItens()) {
            var personalizacao = item.getPersonalizacao();
            if (personalizacao != null && personalizacao.tipo().caixa() && pedido.getStatus() != StatusPedido.ENTREGUE) {
                if (caixas.add(personalizacao.grupo())) builder.linha("- " + item.getObservacaoPersonalizada());
            } else {
                String observacao = item.getObservacaoPersonalizada();
                builder.linha("- " + item.getQuantidade().stripTrailingZeros().toPlainString() + " x " +
                        item.getProduto().getNome() + (observacao == null ? "" : " [" + observacao + "]"));
            }
        }
        builder.linha("Modalidade: " + switch(pedido.getTipoEntrega()) {
            case ENTREGA -> "Entrega - " + pedido.getEnderecoEntrega();
            case RETIRADA -> "Retirada no balcão";
            case LOCAL -> "Consumir no local";
        });
        if(pedido.getAgendadoPara()!=null)builder.linha("Agendado para: "+pedido.getAgendadoPara().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        var moeda=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        if(pedido.getTaxaEntrega().signum()>0)builder.linha("Taxa de entrega: "+moeda.format(pedido.getTaxaEntrega()));
        if(pedido.getBrinde()!=null)builder.linha("Brinde: "+pedido.getBrinde());
        builder.linha("Pagamento: " + (pedido.getFormaPagamento()==null?"não informado":pedido.getFormaPagamento().descricao())).linha("Total: " + moeda.format(pedido.calcularTotal()));
        return builder.construir();
    }
    public URI status(String telefone, long pedido, StatusPedido status) {
        return status(telefone,pedido,status,null);
    }
    public URI status(String telefone, long pedido, StatusPedido status, String entregador) {
        Builder mensagem = new Builder(telefone);
        if (status == StatusPedido.SAIU_ENTREGA) {
            mensagem.linha("Olá! Seu pedido #" + pedido + " saiu para entrega!");
            if (entregador != null && !entregador.isBlank()) mensagem.linha("Entregador: " + entregador);
        } else mensagem.linha("Olá! Seu pedido #" + pedido + " está com status: " + status.name() + ".");
        return mensagem.linha("Equipe Ui K'fomi").construir();
    }
    public URI contato(String telefone, String nome) {
        return new Builder(telefone).linha("Olá, " + nome + "! Aqui é da Ui K'fomi.").construir();
    }
    public CompletableFuture<ResultadoAbertura> abrir(URI uri) {
        return abrir(uri, WhatsAppLinkService::abrirNoNavegador, link ->
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(link), null));
    }

    static CompletableFuture<ResultadoAbertura> abrir(URI uri, Consumer<URI> navegador, Consumer<String> clipboard) {
        Objects.requireNonNull(uri);
        if (uri.getHost() == null || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Informe um link HTTP ou HTTPS válido");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                clipboard.accept(uri.toASCIIString());
                return true;
            } catch (RuntimeException | AWTError e) {
                return false;
            }
        }).thenCompose(copiado -> CompletableFuture.runAsync(() -> navegador.accept(uri))
                .handle((ignorado, erro) -> erro == null ? ResultadoAbertura.ABERTO
                        : copiado ? ResultadoAbertura.COPIADO : ResultadoAbertura.INDISPONIVEL));
    }

    private static void abrirNoNavegador(URI uri) {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("linux")) {
            try {
                Process processo = new ProcessBuilder("xdg-open", uri.toASCIIString())
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                // Um navegador iniciado por xdg-open pode manter o processo ativo.
                if (!processo.waitFor(2, TimeUnit.SECONDS) || processo.exitValue() == 0) return;
            } catch (IOException | SecurityException ignored) {
                // Tenta a API Desktop se o lançador não estiver disponível.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Abertura do navegador interrompida", e);
            }
        }
        try {
            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        throw new UnsupportedOperationException("Navegador indisponível");
    }
}
