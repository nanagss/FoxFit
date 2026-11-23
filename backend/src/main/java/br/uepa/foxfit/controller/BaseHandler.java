package br.uepa.foxfit.controller;

import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Superclasse abstrata para todos os Handlers HTTP do sistema FOX FIT.
 * Aplica o padrão Template Method (despacho de GET/POST e tratamento unificado de erros)
 * e encapsula métodos protegidos reutilizados por todas as rotas.
 */
public abstract class BaseHandler implements HttpHandler {

    @Override
    public final void handle(HttpExchange requisicao) throws IOException {
        String metodo = requisicao.getRequestMethod().toUpperCase();

        try {
            if ("GET".equals(metodo)) {
                processarGet(requisicao);
            } else if ("POST".equals(metodo)) {
                processarPost(requisicao);
            } else {
                responderTexto(requisicao, 405, "Método não permitido.");
            }
        } catch (DateTimeParseException | IllegalArgumentException erro) {
            responderTexto(
                    requisicao,
                    400,
                    erro.getMessage() != null && !erro.getMessage().isBlank()
                            ? erro.getMessage()
                            : "Dados inválidos. Verifique os campos do formulário."
            );
        } catch (SQLException erro) {
            tratarErroSql(requisicao, erro);
        }
    }

    protected void processarGet(HttpExchange requisicao) throws IOException, SQLException {
        responderTexto(requisicao, 405, "Método não permitido.");
    }

    protected void processarPost(HttpExchange requisicao) throws IOException, SQLException {
        responderTexto(requisicao, 405, "Método não permitido.");
    }

    protected void tratarErroSql(HttpExchange requisicao, SQLException erro) throws IOException {
        if ("23505".equals(erro.getSQLState())) {
            responderTexto(requisicao, 409, mensagemDuplicidade());
        } else if ("23503".equals(erro.getSQLState())) {
            responderTexto(requisicao, 409, mensagemVinculoAtivo());
        } else {
            erro.printStackTrace();
            responderTexto(requisicao, 500, "Erro interno ao processar operação no banco de dados.");
        }
    }

    protected String mensagemDuplicidade() {
        return "Registro duplicado: já existe um cadastro com os dados informados.";
    }

    protected String mensagemVinculoAtivo() {
        return "Não é possível excluir este registro pois existem vínculos ativos no sistema.";
    }

    protected String obterRota(HttpExchange requisicao) {
        return requisicao.getRequestURI().getPath();
    }

    protected String lerPaginaHtml(String nomeArquivo) throws IOException {
        return Files.readString(
                Path.of("frontend", "pages", nomeArquivo),
                StandardCharsets.UTF_8
        );
    }

    protected Map<String, String> lerFormulario(HttpExchange requisicao) throws IOException {
        return WebUtils.lerFormulario(requisicao);
    }

    protected Map<String, String> lerQuery(HttpExchange requisicao) {
        return WebUtils.lerQuery(requisicao);
    }

    protected String obrigatorio(Map<String, String> dados, String campo) {
        return WebUtils.obrigatorio(dados, campo);
    }

    protected int lerIdPositivo(String valor, String mensagemErro) {
        if (valor == null || !valor.matches("[1-9][0-9]{0,8}")) {
            throw new IllegalArgumentException(mensagemErro);
        }
        return Integer.parseInt(valor);
    }

    protected String escapar(String valor) {
        return WebUtils.escapar(valor);
    }

    protected String formatarMoeda(BigDecimal valor) {
        return WebUtils.formatarMoeda(valor);
    }

    protected String normalizar(String texto) {
        return WebUtils.normalizar(texto);
    }

    protected Usuario obterUsuarioLogado(HttpExchange requisicao) {
        return WebUtils.obterUsuarioLogado(requisicao);
    }

    protected void responderHtml(HttpExchange requisicao, int codigo, String html) throws IOException {
        WebUtils.responderHtml(requisicao, codigo, html);
    }

    protected void responderTexto(HttpExchange requisicao, int codigo, String texto) throws IOException {
        WebUtils.responderTexto(requisicao, codigo, texto);
    }

    protected void responderJson(HttpExchange requisicao, int codigo, String json) throws IOException {
        WebUtils.responderJson(requisicao, codigo, json);
    }

    protected void redirecionar(HttpExchange requisicao, String destino) throws IOException {
        WebUtils.redirecionar(requisicao, destino);
    }
}
