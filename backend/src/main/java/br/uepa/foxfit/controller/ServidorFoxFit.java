package br.uepa.foxfit.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ponto de entrada HTTP do sistema FOX FIT.
 * Utiliza polimorfismo (Map<String, HttpHandler>) para registrar os 9 Controllers
 * (1 por módulo, todos estendendo BaseHandler) e servir arquivos estáticos de frontend/.
 */
public class ServidorFoxFit {

    private static final String HOST = System.getenv().getOrDefault("FOXFIT_HOST", "127.0.0.1");
    private static final int PORTA = Integer.parseInt(System.getenv().getOrDefault("FOXFIT_PORT", "8080"));

    public static void main(String[] args) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(HOST, PORTA), 0);

        Map<String, HttpHandler> rotas = construirMapaDeRotas();
        for (Map.Entry<String, HttpHandler> rota : rotas.entrySet()) {
            servidor.createContext(rota.getKey(), rota.getValue());
        }

        Path raiz = Path.of("frontend").toAbsolutePath().normalize();
        servidor.createContext("/", requisicao -> servirArquivoEstatico(requisicao, raiz));

        servidor.start();

        System.out.println("Servidor FoxFit iniciado!");
        System.out.println("Acesse: http://" + HOST + ":" + PORTA);
    }

    /**
     * Registra polimorficamente os 9 Controllers MVC (subclasses de BaseHandler / HttpHandler).
     */
    public static Map<String, HttpHandler> construirMapaDeRotas() {
        Map<String, HttpHandler> rotas = new LinkedHashMap<>();

        // 1. Autenticação e Sessão
        BaseHandler auth = new AuthController();
        rotas.put("/pages/login.html", auth);
        rotas.put("/login", auth);
        rotas.put("/logout", auth);

        // 2. Meu Perfil
        BaseHandler perfil = new PerfilController();
        rotas.put("/pages/perfil.html", perfil);
        rotas.put("/perfil/atualizar", perfil);
        rotas.put("/perfil/alterar-senha", perfil);

        // 3. Dashboard
        rotas.put("/pages/dashboard.html", new DashboardController());

        // 4. Alunos (RF01 / RF02)
        BaseHandler alunos = new AlunoController();
        rotas.put("/pages/alunos.html", alunos);
        rotas.put("/alunos/cadastrar", alunos);
        rotas.put("/alunos/atualizar", alunos);
        rotas.put("/alunos/excluir", alunos);

        // 5. Professores (RF03)
        BaseHandler professores = new ProfessorController();
        rotas.put("/pages/professores.html", professores);
        rotas.put("/professores/cadastrar", professores);
        rotas.put("/professores/editar", professores);
        rotas.put("/professores/atualizar", professores);
        rotas.put("/professores/desativar", professores);

        // 6. Planos de Assinatura (RF04)
        BaseHandler planos = new PlanoController();
        rotas.put("/pages/planos.html", planos);
        rotas.put("/planos/cadastrar", planos);
        rotas.put("/planos/atualizar", planos);
        rotas.put("/planos/excluir", planos);

        // 7. Controle de Matrículas (RF05)
        BaseHandler matriculas = new MatriculaController();
        rotas.put("/pages/matriculas.html", matriculas);
        rotas.put("/matriculas/cadastrar", matriculas);
        rotas.put("/matriculas/renovar", matriculas);
        rotas.put("/matriculas/cancelar", matriculas);

        // 8. Fluxo Financeiro
        BaseHandler financeiro = new FinanceiroController();
        rotas.put("/pages/financeiro.html", financeiro);
        rotas.put("/financeiro/pagar", financeiro);

        // 9. Gestão de Usuários (Admin)
        BaseHandler usuarios = new UsuarioController();
        rotas.put("/pages/cadastro.html", usuarios);
        rotas.put("/usuarios/cadastrar", usuarios);
        rotas.put("/usuarios/atualizar", usuarios);
        rotas.put("/usuarios/excluir", usuarios);

        return rotas;
    }

    private static void servirArquivoEstatico(HttpExchange requisicao, Path raiz) throws IOException {
        if (!"GET".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        String rota = requisicao.getRequestURI().getPath();
        if ("/".equals(rota)) {
            rota = "/index.html";
        }

        boolean permitido = rota.equals("/index.html")
                || rota.startsWith("/pages/")
                || rota.startsWith("/assets/");

        Path arquivo = raiz.resolve(rota.substring(1)).normalize();

        if (!permitido || !arquivo.startsWith(raiz) || !Files.isRegularFile(arquivo)) {
            WebUtils.responderTexto(requisicao, 404, "Página não encontrada.");
            return;
        }

        if (rota.endsWith(".html")) {
            String html = Files.readString(arquivo, StandardCharsets.UTF_8);
            WebUtils.responderHtml(requisicao, 200, html);
            return;
        }

        byte[] conteudo = Files.readAllBytes(arquivo);
        requisicao.getResponseHeaders().set("Content-Type", detectarTipoConteudo(rota));
        requisicao.sendResponseHeaders(200, conteudo.length);

        try (var resposta = requisicao.getResponseBody()) {
            resposta.write(conteudo);
        }
    }

    private static String detectarTipoConteudo(String rota) {
        if (rota.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (rota.endsWith(".js")) {
            return "text/javascript; charset=UTF-8";
        }
        if (rota.endsWith(".png")) {
            return "image/png";
        }
        if (rota.endsWith(".jpeg") || rota.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if (rota.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
