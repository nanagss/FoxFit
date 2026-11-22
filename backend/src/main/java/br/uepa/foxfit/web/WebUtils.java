package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.UsuarioDAO;
import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Utilitários compartilhados entre os Handlers HTTP do sistema FOX FIT.
 */
public final class WebUtils {

    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");
    private static final UsuarioDAO USUARIO_DAO = new UsuarioDAO();

    private WebUtils() {
    }

    /**
     * Faz o parse de formulários enviados como
     * application/x-www-form-urlencoded.
     */
    public static Map<String, String> lerFormulario(HttpExchange requisicao) throws IOException {
        String corpo = new String(
                requisicao.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
        return parseParametros(corpo);
    }

    /**
     * uma URL.
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
     * Escapa caracteres especiais HTML para evitar XSS ao renderizar dados do
     * banco.
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
     * específico a partir da requisição HTTP.
     */
    public static String obterCookie(HttpExchange requisicao, String nomeCookie) {
        List<String> cookies = requisicao.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return null;
        }

        for (String cabecalho : cookies) {
            for (String parte : cabecalho.split(";")) {
                String[] par = parte.trim().split("=", 2);
                if (par.length == 2 && par[0].trim().equals(nomeCookie)) {
                    return par[1].trim();
                }
            }
        }

        return null;
    }

    /**
     * Identifica o usuário logado com base no cookie FOXFIT_AUTH_USER.
     */
    public static Usuario obterUsuarioLogado(HttpExchange requisicao) {
        String cookie = obterCookie(requisicao, "FOXFIT_AUTH_USER");
        if (cookie != null && !cookie.isBlank()) {
            try {
                int id = Integer.parseInt(cookie);
                Usuario usuario = USUARIO_DAO.buscarPorId(id);
                if (usuario != null && "ATIVO".equalsIgnoreCase(usuario.getStatus())) {
                    return usuario;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Injeta as informações do usuário autenticado no HTML da página. Atualiza
     * avatar-operador, operador-nome e operador-cargo (com mapeamento ADMIN ->
     * Administrador Geral).
     */
    public static String injetarPerfilOperador(String html, Usuario usuario) {
        if (html == null || usuario == null) {
            return html;
        }

        String iniciais = usuario.getIniciais();
        String nome = escapar(usuario.getNomeCompleto());
        String cargo = escapar(usuario.getCargoFormatado());

        // Iniciais em avatar-operador
        html = html.replaceAll(
                "(<span[^>]*class=[\"'][^\"']*avatar-operador[^\"']*[\"'][^>]*>)[^<]*(</span>)",
                "$1" + Matcher.quoteReplacement(iniciais) + "$2"
        );

        // Nome em operador-nome (ou id/classe operador nome)
        html = html.replaceAll(
                "(<strong[^>]*(?:id|class)=[\"'][^\"']*operador[- ]nome[^\"']*[\"'][^>]*>)[^<]*(</strong>)",
                "$1" + Matcher.quoteReplacement(nome) + "$2"
        );

        // Cargo em operador-cargo (ou id/classe operador cargo)
        html = html.replaceAll(
                "(<small[^>]*(?:id|class)=[\"'][^\"']*operador[- ]cargo[^\"']*[\"'][^>]*>)[^<]*(</small>)",
                "$1" + Matcher.quoteReplacement(cargo) + "$2"
        );

        // Preenche também dados do perfil se for a página perfil.html
        if (usuario.getNomeCompleto() != null && !usuario.getNomeCompleto().isBlank()) {
            html = html.replaceAll(
                    "(id=[\"']perfil-nome[\"'][^>]*value=[\"'])[^\"']*([\"'])",
                    "$1" + Matcher.quoteReplacement(nome) + "$2"
            );
        }
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            html = html.replaceAll(
                    "(id=[\"']perfil-email[\"'][^>]*value=[\"'])[^\"']*([\"'])",
                    "$1" + Matcher.quoteReplacement(escapar(usuario.getEmail())) + "$2"
            );
        }
        if (usuario.getCpf() != null && !usuario.getCpf().isBlank()) {
            html = html.replaceAll(
                    "(id=[\"']perfil-cpf[\"'][^>]*value=[\"'])[^\"']*([\"'])",
                    "$1" + Matcher.quoteReplacement(escapar(usuario.getCpf())) + "$2"
            );
        }
        if (usuario.getCargo() != null && !usuario.getCargo().isBlank()) {
            html = html.replaceAll(
                    "(id=[\"']perfil-cargo[\"'][^>]*value=[\"'])[^\"']*([\"'])",
                    "$1" + Matcher.quoteReplacement(cargo) + "$2"
            );
        }

        return html;
    }

    /**
     * Envia uma resposta HTML com charset UTF-8 e cabeçalhos contra cache.
     * Injeta automaticamente os dados do operador logado se houver sessão
     * ativa.
     */
    public static void responderHtml(HttpExchange requisicao, int codigo, String html) throws IOException {
        Usuario usuario = obterUsuarioLogado(requisicao);
        if (usuario != null) {
            html = injetarPerfilOperador(html, usuario);
        }

        byte[] conteudo = html.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        requisicao.getResponseHeaders().set("Cache-Control", "no-store");
        requisicao.sendResponseHeaders(codigo, conteudo.length);

        try (OutputStream resposta = requisicao.getResponseBody()) {
            resposta.write(conteudo);
        }
    }

    /**
     * charset UTF-8.
     */
    public static void responderTexto(HttpExchange requisicao, int codigo, String texto) throws IOException {
        byte[] conteudo = texto.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        requisicao.sendResponseHeaders(codigo, conteudo.length);

        try (OutputStream resposta = requisicao.getResponseBody()) {
            resposta.write(conteudo);
        }
    }

    public static void responderJson(HttpExchange requisicao, int codigo, String json) throws IOException {
        byte[] conteudo = json.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        requisicao.sendResponseHeaders(codigo, conteudo.length);

        try (OutputStream resposta = requisicao.getResponseBody()) {
            resposta.write(conteudo);
        }
    }

    /**
     * Other).
     */
    public static void redirecionar(HttpExchange requisicao, String urlDestino) throws IOException {
        requisicao.getResponseHeaders().set("Location", urlDestino);
        requisicao.sendResponseHeaders(303, -1);
        requisicao.close();
    }
}
