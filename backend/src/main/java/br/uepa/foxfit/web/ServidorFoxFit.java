package br.uepa.foxfit.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServidorFoxFit {

    public static void main(String[] args) throws IOException {

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 8080), 0
        );

        servidor.createContext(
                "/pages/alunos.html",
                new PaginaAlunosHandler()
        );

        servidor.createContext(
                "/alunos/cadastrar",
                new CadastroAlunoHandler()
        );

        servidor.createContext(
                "/alunos/excluir",
                new ExcluirAlunoHandler()
        );

        servidor.createContext(
                "/alunos/atualizar",
                new AtualizarAlunoHandler()
        );

        servidor.createContext(
                "/alunos/listar",
                new ListarAlunosHandler()
        );

        // Rotas de Dashboard
        servidor.createContext(
                "/pages/dashboard.html",
                new PaginaDashboardHandler()
        );

        // Rotas de Planos de Assinatura
        servidor.createContext(
                "/pages/planos.html",
                new PaginaPlanosHandler()
        );

        servidor.createContext(
                "/planos/cadastrar",
                new CadastroPlanoHandler()
        );

        servidor.createContext(
                "/planos/atualizar",
                new AtualizarPlanoHandler()
        );

        servidor.createContext(
                "/planos/excluir",
                new ExcluirPlanoHandler()
        );

        // Rotas de Fluxo Financeiro
        servidor.createContext(
                "/pages/financeiro.html",
                new PaginaFinanceiroHandler()
        );

        servidor.createContext(
                "/financeiro/pagar",
                new ConfirmarPagamentoHandler()
        );

        // Rotas de Autenticação e Sessão
        PaginaLoginHandler loginHandler = new PaginaLoginHandler();
        servidor.createContext(
                "/pages/login.html",
                loginHandler
        );
        servidor.createContext(
                "/login",
                loginHandler
        );

        LogoutHandler logoutHandler = new LogoutHandler();
        servidor.createContext(
                "/logout",
                logoutHandler
        );

        // Rotas de Meu Perfil
        PaginaPerfilHandler perfilHandler = new PaginaPerfilHandler();
        servidor.createContext(
                "/pages/perfil.html",
                perfilHandler
        );
        servidor.createContext(
                "/perfil/atualizar",
                new AtualizarPerfilHandler()
        );
        servidor.createContext(
                "/perfil/alterar-senha",
                new AlterarSenhaHandler()
        );

        Path raiz = Path.of("frontend").toAbsolutePath().normalize();

        servidor.createContext("/", requisicao -> {

            String metodo = requisicao.getRequestMethod();

            if (!metodo.equals("GET")) {
                responder(requisicao, 405, "Método não permitido.");
                return;
            }

            String rota = requisicao.getRequestURI().getPath();

            if (rota.equals("/")) {
                rota = "/index.html";
            }

            boolean permitido =
                    rota.equals("/index.html")
                            || rota.startsWith("/pages/")
                            || rota.startsWith("/assets/");

            Path arquivo = raiz.resolve(rota.substring(1)).normalize();

            if (!permitido || !arquivo.startsWith(raiz)
                    || !Files.isRegularFile(arquivo)) {
                responder(requisicao, 404, "Página não encontrada.");
                return;
            }

            if (rota.endsWith(".html")) {
                String html = Files.readString(arquivo, StandardCharsets.UTF_8);
                WebUtils.responderHtml(requisicao, 200, html);
                return;
            }

            byte[] conteudo = Files.readAllBytes(arquivo);

            String tipo = "application/octet-stream";

            if (rota.endsWith(".html")) {
                tipo = "text/html; charset=UTF-8";
            } else if (rota.endsWith(".css")) {
                tipo = "text/css; charset=UTF-8";
            } else if (rota.endsWith(".js")) {
                tipo = "text/javascript; charset=UTF-8";
            } else if (rota.endsWith(".png")) {
                tipo = "image/png";
            } else if (rota.endsWith(".jpeg")
                    || rota.endsWith(".jpg")) {
                tipo = "image/jpeg";
            } else if (rota.endsWith(".svg")) {
                tipo = "image/svg+xml";
            }

            requisicao.getResponseHeaders()
                    .set("Content-Type", tipo);

            requisicao.sendResponseHeaders(200, conteudo.length);

            try (var resposta = requisicao.getResponseBody()) {
                resposta.write(conteudo);
            }
        });

        servidor.start();

        System.out.println("Servidor FoxFit iniciado!");
        System.out.println("Acesse: http://127.0.0.1:8080");
    }

    private static void responder(
            HttpExchange requisicao,
            int codigo,
            String mensagem
    ) throws IOException {

        byte[] conteudo = mensagem.getBytes(StandardCharsets.UTF_8);

        requisicao.getResponseHeaders()
                .set("Content-Type", "text/plain; charset=UTF-8");

        requisicao.sendResponseHeaders(codigo, conteudo.length);

        try (var resposta = requisicao.getResponseBody()) {
            resposta.write(conteudo);
        }
    }
}
