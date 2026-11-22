package br.uepa.foxfit.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Handler responsável por encerrar a sessão do operador autenticado.
 */
public class LogoutHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {
        requisicao.getResponseHeaders().set(
                "Set-Cookie",
                "FOXFIT_AUTH_USER=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
        );
        WebUtils.redirecionar(requisicao, "/pages/login.html");
    }
}
