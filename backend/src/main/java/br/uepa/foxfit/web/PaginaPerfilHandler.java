package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.PerfilDAO;
import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

/**
 * Handler responsável por renderizar dinamicamente a tela de Meu Perfil (/pages/perfil.html)
 * e também por aceitar submissões de formulário direto na mesma rota.
 */
public class PaginaPerfilHandler implements HttpHandler {

    private final PerfilDAO perfilDAO = new PerfilDAO();

    @Override
    public void handle(HttpExchange requisicao) throws IOException {
        String metodo = requisicao.getRequestMethod().toUpperCase();

        if ("GET".equals(metodo)) {
            tratarGet(requisicao);
        } else if ("POST".equals(metodo)) {
            tratarPost(requisicao);
        } else {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
        }
    }

    private void tratarGet(HttpExchange requisicao) throws IOException {
        Path arquivo = Path.of("frontend", "pages", "perfil.html").toAbsolutePath().normalize();

        if (!Files.isRegularFile(arquivo)) {
            WebUtils.responderTexto(requisicao, 404, "Página perfil.html não encontrada.");
            return;
        }

        Usuario usuarioLogado = WebUtils.obterUsuarioLogado(requisicao);
        int idUsuario = (usuarioLogado != null) ? usuarioLogado.getId() : 1;

        Usuario usuarioAtualizado = perfilDAO.buscarPorId(idUsuario);
        if (usuarioAtualizado == null && usuarioLogado != null) {
            usuarioAtualizado = usuarioLogado;
        }

        String html = Files.readString(arquivo, StandardCharsets.UTF_8);

        // Feedback / Alertas via Query Params
        Map<String, String> query = WebUtils.lerQuery(requisicao);
        String feedbackHtml = gerarFeedbackHtml(query);

        html = html.replace("<!-- MENSAGEM_FEEDBACK_PERFIL -->", feedbackHtml);

        if (usuarioAtualizado != null) {
            html = WebUtils.injetarPerfilOperador(html, usuarioAtualizado);
        }

        WebUtils.responderHtml(requisicao, 200, html);
    }

    private void tratarPost(HttpExchange requisicao) throws IOException {
        Map<String, String> dados = WebUtils.lerFormulario(requisicao);
        String accept = requisicao.getRequestHeaders().getFirst("Accept");
        boolean querJson = accept != null && accept.contains("application/json");

        Usuario usuarioLogado = WebUtils.obterUsuarioLogado(requisicao);
        int idUsuario = (usuarioLogado != null) ? usuarioLogado.getId() : 1;

        if (dados.containsKey("idOperador")) {
            try {
                idUsuario = Integer.parseInt(dados.get("idOperador"));
            } catch (NumberFormatException ignored) {
            }
        }

        // Se for requisição de troca de senha
        if (dados.containsKey("senhaAtual") || dados.containsKey("novaSenha")) {
            processarAlteracaoSenha(requisicao, dados, idUsuario, querJson);
            return;
        }

        // Senão, é atualização dos dados funcionais do operador
        processarAtualizacaoDados(requisicao, dados, idUsuario, querJson);
    }

    private void processarAtualizacaoDados(HttpExchange requisicao, Map<String, String> dados, int idUsuario, boolean querJson) throws IOException {
        String nomeCompleto = dados.getOrDefault("nomeCompleto", "").trim();
        String email = dados.getOrDefault("email", "").trim();

        try {
            perfilDAO.atualizarDados(idUsuario, nomeCompleto, email);

            if (querJson) {
                Usuario u = perfilDAO.buscarPorId(idUsuario);
                String json = String.format(
                        "{\"sucesso\":true,\"mensagem\":\"Alterações do perfil salvas com sucesso!\",\"usuario\":{\"id\":%d,\"nomeCompleto\":\"%s\",\"email\":\"%s\"}}",
                        u.getId(),
                        WebUtils.escapar(u.getNomeCompleto()),
                        WebUtils.escapar(u.getEmail())
                );
                WebUtils.responderJson(requisicao, 200, json);
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?sucesso=perfil");
            }
        } catch (IllegalArgumentException erro) {
            String msg = erro.getMessage();
            String codigoErro = "dados_invalidos";
            if (msg.contains("já está em uso")) {
                codigoErro = "email_duplicado";
            } else if (msg.contains("e-mail corporativo válido")) {
                codigoErro = "email_invalido";
            } else if (msg.contains("nome completo")) {
                codigoErro = "nome_invalido";
            }

            if (querJson) {
                WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"" + WebUtils.escapar(msg) + "\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=" + codigoErro);
            }
        } catch (SQLException erro) {
            erro.printStackTrace();
            if (querJson) {
                WebUtils.responderJson(requisicao, 500, "{\"sucesso\":false,\"mensagem\":\"Erro interno ao atualizar perfil no banco.\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=interno");
            }
        }
    }

    private void processarAlteracaoSenha(HttpExchange requisicao, Map<String, String> dados, int idUsuario, boolean querJson) throws IOException {
        String senhaAtual = dados.getOrDefault("senhaAtual", "");
        String novaSenha = dados.getOrDefault("novaSenha", "");
        String confirmarNovaSenha = dados.getOrDefault("confirmarNovaSenha", "");

        if (!novaSenha.equals(confirmarNovaSenha)) {
            if (querJson) {
                WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"A confirmação da nova senha não coincide.\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=senha_divergente");
            }
            return;
        }

        try {
            perfilDAO.alterarSenha(idUsuario, senhaAtual, novaSenha);

            if (querJson) {
                WebUtils.responderJson(requisicao, 200, "{\"sucesso\":true,\"mensagem\":\"Senha atualizada com sucesso!\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?sucesso=senha");
            }
        } catch (IllegalArgumentException erro) {
            String msg = erro.getMessage();
            String codigoErro = "senha_invalida";
            if (msg.contains("incorreta")) {
                codigoErro = "senha_atual";
            } else if (msg.contains("mínimo 8")) {
                codigoErro = "senha_curta";
            } else if (msg.contains("diferente da senha atual")) {
                codigoErro = "senha_igual";
            }

            if (querJson) {
                WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"" + WebUtils.escapar(msg) + "\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=" + codigoErro);
            }
        } catch (SQLException erro) {
            erro.printStackTrace();
            if (querJson) {
                WebUtils.responderJson(requisicao, 500, "{\"sucesso\":false,\"mensagem\":\"Erro interno ao atualizar senha no banco.\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=interno");
            }
        }
    }

    private String gerarFeedbackHtml(Map<String, String> query) {
        String sucesso = query.get("sucesso");
        String erro = query.get("erro");

        if (sucesso != null) {
            String mensagem = switch (sucesso) {
                case "perfil" -> "Alterações do perfil salvas com sucesso!";
                case "senha" -> "Senha de acesso atualizada com sucesso!";
                default -> "Operação realizada com sucesso!";
            };
            return String.format("""
                <div class="alerta-perfil alerta-sucesso" role="status" style="background-color: #def7ec; color: #03543f; border: 1px solid #bcf0da; padding: 14px 18px; border-radius: 8px; margin-bottom: 24px; font-size: 15px; font-weight: 500; display: flex; align-items: center; gap: 10px;">
                  <span aria-hidden="true" style="font-size: 18px;">✓</span>
                  <span>%s</span>
                </div>
                """, WebUtils.escapar(mensagem));
        }

        if (erro != null) {
            String mensagem = switch (erro) {
                case "senha_atual" -> "A senha atual informada está incorreta.";
                case "senha_curta" -> "A nova senha deve possuir no mínimo 8 caracteres.";
                case "senha_divergente" -> "A confirmação da nova senha não coincide.";
                case "senha_igual" -> "A nova senha deve ser diferente da senha atual.";
                case "email_duplicado" -> "Este e-mail corporativo já está em uso por outro operador.";
                case "email_invalido" -> "Informe um endereço de e-mail corporativo válido.";
                case "nome_invalido" -> "O nome completo deve conter pelo menos 3 caracteres.";
                case "dados_invalidos" -> "Preencha todos os campos obrigatórios corretamente.";
                case "interno" -> "Ocorreu um erro interno ao processar a solicitação no banco.";
                default -> "Ocorreu um erro ao processar a solicitação.";
            };
            return String.format("""
                <div class="alerta-perfil alerta-erro" role="alert" style="background-color: #fde8e8; color: #9b1c1c; border: 1px solid #f8b4b4; padding: 14px 18px; border-radius: 8px; margin-bottom: 24px; font-size: 15px; font-weight: 500; display: flex; align-items: center; gap: 10px;">
                  <span aria-hidden="true" style="font-size: 18px;">⚠</span>
                  <span>%s</span>
                </div>
                """, WebUtils.escapar(mensagem));
        }

        return "";
    }
}
