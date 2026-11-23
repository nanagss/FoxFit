package br.uepa.foxfit.controller;

import br.uepa.foxfit.dao.RepositorioCRUD;
import br.uepa.foxfit.dao.UsuarioDAO;
import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller responsável pelo módulo de Gestão de Usuários (Admin RBAC):
 * /pages/cadastro.html, /usuarios/cadastrar, /usuarios/atualizar e /usuarios/excluir.
 */
public class UsuarioController extends BaseHandler {

    private final RepositorioCRUD<Usuario> usuarioDAO = new UsuarioDAO();

    @Override
    protected void processarGet(HttpExchange requisicao) throws IOException, SQLException {
        if (!autorizarAdmin(requisicao, "gerenciar usuários")) {
            return;
        }

        List<Usuario> usuarios = usuarioDAO.listar();
        String html = lerPaginaHtml("cadastro.html");

        StringBuilder linhas = new StringBuilder();
        int ativos = 0;

        for (Usuario u : usuarios) {
            boolean ativo = u.isAtivo();
            if (ativo) {
                ativos++;
            }

            String classeBadge = ativo ? "status-ativo" : "status-inativo";
            String rotuloStatus = ativo ? "Ativo" : "Inativo";

            linhas.append("""
                    <tr>
                      <th scope="row">%s</th>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td><span class="badge-status %s">%s</span></td>
                      <td class="celula-acoes">
                        <a class="btn-editar" href="/pages/cadastro.html?editar=%d#form-editar-usuario-dinamico">Editar</a>
                        <a class="btn-excluir" href="/pages/cadastro.html?excluir=%d#confirmar-exclusao-usuario">Excluir</a>
                      </td>
                    </tr>
                    """.formatted(
                    escapar(u.getIdentificadorExibicao()),
                    escapar(u.getNomeCompleto()),
                    escapar(u.getEmail()),
                    escapar(u.getCpf()),
                    escapar(u.getCargoFormatado()),
                    classeBadge,
                    rotuloStatus,
                    u.getId(),
                    u.getId()
            ));
        }

        if (linhas.isEmpty()) {
            linhas.append("<tr><td colspan=\"7\">Nenhum usuário cadastrado.</td></tr>");
        }

        String formularioDinamico = construirFormularioDinamico(requisicao);
        String total = usuarios.size() + (usuarios.size() == 1 ? " usuário" : " usuários")
                + " (" + ativos + (ativos == 1 ? " ativo)" : " ativos)");

        html = html.replace("<!-- FORMULARIO_EDICAO_USUARIO -->", formularioDinamico);
        html = html.replace("<!-- LINHAS_USUARIOS -->", linhas.toString());
        html = html.replace("<!-- TOTAL_USUARIOS -->", total);

        responderHtml(requisicao, 200, html);
    }

    @Override
    protected void processarPost(HttpExchange requisicao) throws IOException, SQLException {
        switch (obterRota(requisicao)) {
            case "/usuarios/cadastrar" -> cadastrar(requisicao);
            case "/usuarios/atualizar" -> atualizar(requisicao);
            case "/usuarios/excluir" -> excluir(requisicao);
            default -> responderTexto(requisicao, 404, "Ação de usuários não encontrada.");
        }
    }

    private void cadastrar(HttpExchange requisicao) throws IOException, SQLException {
        if (!autorizarAdmin(requisicao, "cadastrar usuários")) {
            return;
        }

        Map<String, String> dados = lerFormulario(requisicao);

        String nome = obrigatorio(dados, "nomeCompleto");
        String email = obrigatorio(dados, "email").toLowerCase(Locale.ROOT);
        String cpf = obrigatorio(dados, "cpf");
        String cargo = obrigatorio(dados, "cargo").toUpperCase(Locale.ROOT);
        String senha = obrigatorio(dados, "senha");
        String confirmarSenha = obrigatorio(dados, "confirmarSenha");

        if (!"ADMIN".equals(cargo) && !"OPERADOR".equals(cargo)) {
            throw new IllegalArgumentException("Cargo inválido. Use ADMIN ou OPERADOR.");
        }
        if (senha.length() < 8) {
            throw new IllegalArgumentException("A senha inicial deve possuir no mínimo 8 caracteres.");
        }
        if (!senha.equals(confirmarSenha)) {
            throw new IllegalArgumentException("A confirmação da senha não coincide com a senha informada.");
        }

        Usuario novoUsuario = new Usuario(nome, email, cpf, cargo, senha);
        usuarioDAO.cadastrar(novoUsuario);

        redirecionar(requisicao, "/pages/cadastro.html");
    }

    private void atualizar(HttpExchange requisicao) throws IOException, SQLException {
        if (!autorizarAdmin(requisicao, "editar usuários")) {
            return;
        }

        Map<String, String> dados = lerFormulario(requisicao);
        int id = lerIdPositivo(dados.get("idUsuario"), "ID de usuário inválido.");

        Usuario existente = usuarioDAO.buscarPorId(id);
        if (existente == null) {
            responderTexto(requisicao, 404, "Usuário não encontrado.");
            return;
        }

        String nome = obrigatorio(dados, "nomeCompleto");
        String email = obrigatorio(dados, "email").toLowerCase(Locale.ROOT);
        String cpf = obrigatorio(dados, "cpf");
        String cargo = obrigatorio(dados, "cargo").toUpperCase(Locale.ROOT);
        String status = obrigatorio(dados, "status").toUpperCase(Locale.ROOT);
        String novaSenha = dados.getOrDefault("novaSenha", "").trim();

        if (!"ADMIN".equals(cargo) && !"OPERADOR".equals(cargo)) {
            throw new IllegalArgumentException("Cargo inválido.");
        }
        if (!"ATIVO".equals(status) && !"INATIVO".equals(status)) {
            throw new IllegalArgumentException("Status inválido.");
        }

        existente.setNomeCompleto(nome);
        existente.setEmail(email);
        existente.setCpf(cpf);
        existente.setCargo(cargo);
        existente.setStatus(status);
        existente.setSenha(novaSenha.isBlank() ? null : novaSenha);

        if (!usuarioDAO.atualizar(existente)) {
            responderTexto(requisicao, 404, "Usuário não encontrado.");
            return;
        }

        redirecionar(requisicao, "/pages/cadastro.html");
    }

    private void excluir(HttpExchange requisicao) throws IOException, SQLException {
        if (!autorizarAdmin(requisicao, "excluir usuários")) {
            return;
        }

        Usuario logado = obterUsuarioLogado(requisicao);
        Map<String, String> dados = lerFormulario(requisicao);
        int id = lerIdPositivo(dados.get("idUsuario"), "ID de usuário inválido.");

        if (logado != null && logado.getId() == id) {
            responderTexto(requisicao, 409, "Você não pode excluir a própria conta enquanto estiver logado.");
            return;
        }

        if (!usuarioDAO.excluir(id)) {
            responderTexto(requisicao, 404, "Usuário não encontrado.");
            return;
        }

        redirecionar(requisicao, "/pages/cadastro.html");
    }

    private boolean autorizarAdmin(HttpExchange requisicao, String acao) throws IOException {
        Usuario logado = obterUsuarioLogado(requisicao);
        if (logado != null && !logado.isAdmin()) {
            responderTexto(requisicao, 403, "Acesso restrito: apenas Administradores podem " + acao + ".");
            return false;
        }
        return true;
    }

    private String construirFormularioDinamico(HttpExchange requisicao) throws SQLException {
        Map<String, String> query = lerQuery(requisicao);

        String editarParam = query.get("editar");
        if (editarParam != null && editarParam.matches("[1-9][0-9]{0,8}")) {
            Usuario selecionado = usuarioDAO.buscarPorId(Integer.parseInt(editarParam));
            if (selecionado != null) {
                String selOp = "OPERADOR".equalsIgnoreCase(selecionado.getCargo()) ? "selected" : "";
                String selAdm = "ADMIN".equalsIgnoreCase(selecionado.getCargo()) ? "selected" : "";
                String selAtivo = selecionado.isAtivo() ? "selected" : "";
                String selInativo = !selecionado.isAtivo() ? "selected" : "";

                return """
                        <section class="modulo-sistema">
                          <header class="cabecalho-modulo">
                            <h2>Editar Usuário / Redefinir Senha (%s)</h2>
                          </header>
                          <form id="form-editar-usuario-dinamico" class="formulario-padrao" action="/usuarios/atualizar" method="post">
                            <input type="hidden" name="idUsuario" value="%d">
                            <fieldset>
                              <legend>Informações do Usuário Selecionado</legend>
                              <label>Nome Completo
                                <input type="text" name="nomeCompleto" value="%s" required>
                              </label>
                              <label>E-mail Corporativo
                                <input type="email" name="email" value="%s" required>
                              </label>
                              <label>CPF
                                <input type="text" name="cpf" value="%s" maxlength="14" required>
                              </label>
                              <label>Cargo / Função (Nível de Acesso)
                                <select name="cargo" required>
                                  <option value="OPERADOR" %s>Operador (Recepção / Atendimento)</option>
                                  <option value="ADMIN" %s>Administrador (Acesso Total)</option>
                                </select>
                              </label>
                              <label>Nova Senha (Opcional para redefinição)
                                <input type="password" name="novaSenha" placeholder="Deixe em branco para manter a atual" minlength="8">
                              </label>
                              <label>Status da Conta
                                <select name="status" required>
                                  <option value="ATIVO" %s>Ativo</option>
                                  <option value="INATIVO" %s>Inativo</option>
                                </select>
                              </label>
                              <footer class="acoes-formulario">
                                <a href="/pages/cadastro.html" class="btn btn-secundario">Cancelar</a>
                                <button type="submit" class="btn btn-primario">Salvar Alterações</button>
                              </footer>
                            </fieldset>
                          </form>
                        </section>
                        """.formatted(
                        escapar(selecionado.getIdentificadorExibicao()),
                        selecionado.getId(),
                        escapar(selecionado.getNomeCompleto()),
                        escapar(selecionado.getEmail()),
                        escapar(selecionado.getCpf()),
                        selOp,
                        selAdm,
                        selAtivo,
                        selInativo
                );
            }
        }

        String excluirParam = query.get("excluir");
        if (excluirParam != null && excluirParam.matches("[1-9][0-9]{0,8}")) {
            Usuario selecionado = usuarioDAO.buscarPorId(Integer.parseInt(excluirParam));
            if (selecionado != null) {
                return """
                        <section class="modulo-sistema" id="confirmar-exclusao-usuario">
                          <header class="cabecalho-modulo">
                            <h2>Confirmar Exclusão de Usuário</h2>
                          </header>
                          <p>Deseja realmente remover o usuário <strong>%s</strong> (%s)?</p>
                          <form action="/usuarios/excluir" method="post">
                            <input type="hidden" name="idUsuario" value="%d">
                            <footer class="acoes-formulario">
                              <a href="/pages/cadastro.html" class="btn btn-secundario">Cancelar</a>
                              <button type="submit" class="btn btn-excluir">Confirmar Exclusão</button>
                            </footer>
                          </form>
                        </section>
                        """.formatted(
                        escapar(selecionado.getNomeCompleto()),
                        escapar(selecionado.getIdentificadorExibicao()),
                        selecionado.getId()
                );
            }
        }

        return "";
    }

    @Override
    protected String mensagemDuplicidade() {
        return "Já existe um usuário cadastrado com este e-mail ou CPF.";
    }
}
