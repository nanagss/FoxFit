package br.uepa.foxfit.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utilitários compartilhados entre os Handlers HTTP do sistema FOX FIT.
 */
public final class WebUtils {

    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");

    private WebUtils() {
    }

    /**
     * Faz o parse de formulários enviados como application/x-www-form-urlencoded.
     */
    public static Map<String, String> lerFormulario(HttpExchange requisicao) throws IOException {
        String corpo = new String(
                requisicao.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
        return parseParametros(corpo);
    }

    /**
     * Faz o parse da query string de uma URL (ex.: ?statusFinanceiro=PAGO&termoAluno=Lucas).
     */
    public static Map<String, String> lerQuery(HttpExchange requisicao) {
        return parseParametros(requisicao.getRequestURI().getRawQuery());
    }

    private static Map<String, String> parseParametros(String texto) {
        Map<String, String> dados = new HashMap<>();

        if (texto == null || texto.isBlank()) {
            return dados;
        }

        for (String parte : texto.split("&")) {
            if (parte.isBlank()) {
                continue;
            }
            String[] campo = parte.split("=", 2);
            String chave = URLDecoder.decode(campo[0], StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(
                    campo.length > 1 ? campo[1] : "",
                    StandardCharsets.UTF_8
            );
            dados.put(chave, valor.trim());
        }

        return dados;
    }

    /**
     * Garante que um campo obrigatório foi enviado e não está vazio.
     */
    public static String obrigatorio(Map<String, String> dados, String campo) {
        String valor = dados.get(campo);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: " + campo);
        }
        return valor;
    }

    /**
     * Escapa caracteres especiais HTML para evitar XSS ao renderizar dados do banco.
     */
    public static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Formata valores monetários no padrão brasileiro (ex.: R$ 1.198,80).
     */
    public static String formatarMoeda(BigDecimal valor) {
        BigDecimal seguro = valor != null ? valor : BigDecimal.ZERO;
        return NumberFormat.getCurrencyInstance(LOCALE_BR).format(seguro);
    }

    /**
     * Remove acentos e converte para minúsculas para facilitar buscas textuais.
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Envia uma resposta HTML UTF-8 sem cache.
     */
    public static void responderHtml(HttpExchange requisicao, int codigo, String html) throws IOException {
        byte[] conteudo = html.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        requisicao.getResponseHeaders().set("Cache-Control", "no-store");
        requisicao.sendResponseHeaders(codigo, conteudo.length);
        try (var saida = requisicao.getResponseBody()) {
            saida.write(conteudo);
        }
    }

    /**
     * Envia uma resposta de texto simples UTF-8 (usada para erros 400, 404, 405, 409, 500).
     */
    public static void responderTexto(HttpExchange requisicao, int codigo, String mensagem) throws IOException {
        byte[] conteudo = mensagem.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        requisicao.sendResponseHeaders(codigo, conteudo.length);
        try (var saida = requisicao.getResponseBody()) {
            saida.write(conteudo);
        }
    }

    /**
     * Redireciona o navegador após uma ação POST (padrão Post/Redirect/Get - HTTP 303).
     */
    public static void redirecionar(HttpExchange requisicao, String destino) throws IOException {
        requisicao.getResponseHeaders().set("Location", destino);
        requisicao.sendResponseHeaders(303, -1);
        requisicao.close();
    }
}
