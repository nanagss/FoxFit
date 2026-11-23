package br.uepa.foxfit.controller;

import br.uepa.foxfit.dao.UsuarioDAO;
import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

/**
 * Controller responsável pelo módulo Meu Perfil
 * (/pages/perfil.html, /perfil/atualizar e /perfil/alterar-senha).
 */
public class PerfilController extends BaseHandler {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void processarGet(HttpExchange requisicao) throws IOException {
        Path arquivo = Path.of("frontend", "pages", "perfil.html").toAbsolutePath().normalize();

        if (!Files.isRegularFile(arquivo)) {
            responderTexto(requisicao, 404, "Página perfil.html não encontrada.");
            return;
        }

        Usuario usuarioLogado = obterUsuarioLogado(requisicao);
        int idUsuario = (usuarioLogado != null) ? usuarioLogado.getId() : 1;

        Usuario usuarioAtualizado = usuarioDAO.buscarPorId(idUsuario);
        if (usuarioAtualizado == null && usuarioLogado != null) {
            usuarioAtualizado = usuarioLogado;
        }

        String html = lerPaginaHtml("perfil.html");
        Map<String, String> query = lerQuery(requisicao);
        html = html.replace("<!-- MENSAGEM_FEEDBACK_PERFIL -->", gerarFeedbackHtml(query));

        if (usuarioAtualizado != null) {
            html = WebUtils.injetarPerfilOperador(html, usuarioAtualizado);
        }

        responderHtml(requisicao, 200, html);
    }

    @Override
    protected void processarPost(HttpExchange requisicao) throws IOException {
        Map<String, String> dados = lerFormulario(requisicao);
        String accept = requisicao.getRequestHeaders().getFirst("Accept");
        boolean querJson = accept != null && accept.contains("application/json");

        Usuario usuarioLogado = obterUsuarioLogado(requisicao);
        int idUsuario = (usuarioLogado != null) ? usuarioLogado.getId() : 1;

        if (dados.containsKey("idOperador")) {
            try {
                idUsuario = Integer.parseInt(dados.get("idOperador"));
            } catch (NumberFormatException ignored) {
            }
        }

        String rota = obterRota(requisicao);
        if ("/perfil/alterar-senha".equals(rota)
                || ("/pages/perfil.html".equals(rota) && (dados.containsKey("senhaAtual") || dados.containsKey("novaSenha")))) {
            processarAlteracaoSenha(requisicao, dados, idUsuario, querJson);
            return;
        }

        processarAtualizacaoDados(requisicao, dados, idUsuario, querJson);
    }

    private void processarAtualizacaoDados(HttpExchange requisicao, Map<String, String> dados, int idUsuario, boolean querJson) throws IOException {
        String nomeCompleto = dados.getOrDefault("nomeCompleto", "").trim();
        String email = dados.getOrDefault("email", "").trim();

        try {
            usuarioDAO.atualizarDados(idUsuario, nomeCompleto, email);

            if (querJson) {
                Usuario u = usuarioDAO.buscarPorId(idUsuario);
                String json = String.format(
                        "{\"sucesso\":true,\"mensagem\":\"Alterações do perfil salvas com sucesso!\",\"usuario\":{\"id\":%d,\"nomeCompleto\":\"%s\",\"email\":\"%s\"}}",
                        u.getId(),
                        escapar(u.getNomeCompleto()),
                        escapar(u.getEmail())
                );
                responderJson(requisicao, 200, json);
            } else {
                redirecionar(requisicao, "/pages/perfil.html?sucesso=perfil");
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
                responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"" + escapar(msg) + "\"}");
            } else {
                redirecionar(requisicao, "/pages/perfil.html?erro=" + codigoErro);
            }
        } catch (SQLException erro) {
            erro.printStackTrace();
            if (querJson) {
                responderJson(requisicao, 500, "{\"sucesso\":false,\"mensagem\":\"Erro interno ao atualizar perfil no banco.\"}");
            } else {
                redirecionar(requisicao, "/pages/perfil.html?erro=interno");
            }
        }
    }

    private void processarAlteracaoSenha(HttpExchange requisicao, Map<String, String> dados, int idUsuario, boolean querJson) throws IOException {
        String senhaAtual = dados.getOrDefault("senhaAtual", "");
        String novaSenha = dados.getOrDefault("novaSenha", "");
        String confirmarNovaSenha = dados.getOrDefault("confirmarNovaSenha", "");

        if (!novaSenha.equals(confirmarNovaSenha)) {
            if (querJson) {
                responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"A confirmação da nova senha não coincide.\"}");
            } else {
                redirecionar(requisicao, "/pages/perfil.html?erro=senha_divergente");
            }
            return;
        }

        try {
            usuarioDAO.alterarSenha(idUsuario, senhaAtual, novaSenha);

            if (querJson) {
                responderJson(requisicao, 200, "{\"sucesso\":true,\"mensagem\":\"Senha atualizada com sucesso!\"}");
            } else {
                redirecionar(requisicao, "/pages/perfil.html?sucesso=senha");
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
                responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"" + escapar(msg) + "\"}");
            } else {
                redirecionar(requisicao, "/pages/perfil.html?erro=" + codigoErro);
            }
        } catch (SQLException erro) {
            erro.printStackTrace();
            if (querJson) {
                responderJson(requisicao, 500, "{\"sucesso\":false,\"mensagem\":\"Erro interno ao atualizar senha no banco.\"}");
            } else {
                redirecionar(requisicao, "/pages/perfil.html?erro=interno");
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
                """, escapar(mensagem));
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
                """, escapar(mensagem));
        }

        return "";
    }
}
