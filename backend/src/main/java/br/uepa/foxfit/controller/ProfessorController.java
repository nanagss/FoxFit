package br.uepa.foxfit.controller;

import br.uepa.foxfit.dao.ProfessorDAO;
import br.uepa.foxfit.model.Professor;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller responsável pelo módulo de Professores / Instrutores (RF03):
 * /pages/professores.html, /professores/cadastrar, /professores/editar,
 * /professores/atualizar e /professores/desativar.
 */
public class ProfessorController extends BaseHandler {

    private static final Set<String> STATUS_VALIDOS = Set.of("ATIVO", "INATIVO");

    private final ProfessorDAO professorDAO = new ProfessorDAO();

    @Override
    protected void processarGet(HttpExchange requisicao) throws IOException, SQLException {
        switch (obterRota(requisicao)) {
            case "/pages/professores.html" -> exibirPagina(requisicao);
            case "/professores/editar" -> exibirFormularioEdicao(requisicao);
            default -> responderTexto(requisicao, 404, "Rota de professores não encontrada.");
        }
    }

    @Override
    protected void processarPost(HttpExchange requisicao) throws IOException, SQLException {
        switch (obterRota(requisicao)) {
            case "/professores/cadastrar" -> cadastrar(requisicao);
            case "/professores/atualizar" -> atualizar(requisicao);
            case "/professores/desativar" -> desativar(requisicao);
            default -> responderTexto(requisicao, 404, "Ação de professores não encontrada.");
        }
    }

    private void exibirPagina(HttpExchange requisicao) throws IOException, SQLException {
        List<Professor> professores = professorDAO.listar();
        String html = lerPaginaHtml("professores.html");

        String inicioTabela = "<tbody id=\"corpo-tabela-professores\">";
        int inicio = html.indexOf(inicioTabela);
        int fim = html.indexOf("</tbody>", inicio);

        if (inicio < 0 || fim < 0) {
            throw new IOException("Tabela de professores não encontrada no HTML.");
        }

        html = html.substring(0, inicio + inicioTabela.length())
                + "\n"
                + gerarLinhas(professores)
                + "\n"
                + html.substring(fim);

        int inicioRodape = html.indexOf("<tfoot>");
        int fimRodape = html.indexOf("</tfoot>", inicioRodape);

        if (inicioRodape < 0 || fimRodape < 0) {
            throw new IOException("Rodapé da tabela não encontrado.");
        }

        long ativos = professores.stream().filter(Professor::isAtivo).count();
        String total = ativos == 1 ? "1 professor ativo" : ativos + " professores ativos";

        String rodape = """
                <tfoot>
                  <tr>
                    <td colspan="7">Total de profissionais técnicos habilitados na unidade</td>
                    <td colspan="2">%s</td>
                  </tr>
                </tfoot>
                """.formatted(total);

        html = html.substring(0, inicioRodape)
                + rodape
                + html.substring(fimRodape + "</tfoot>".length());

        responderHtml(requisicao, 200, html);
    }

    private void exibirFormularioEdicao(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> query = lerQuery(requisicao);
        int id = lerIdPositivo(query.get("idProfessor"), "ID de professor inválido.");

        Professor professor = professorDAO.buscarPorId(id);
        if (professor == null) {
            responderTexto(requisicao, 404, "Professor não encontrado.");
            return;
        }

        String htmlOriginal = lerPaginaHtml("professores.html");
        int inicioMain = htmlOriginal.indexOf("<main");
        if (inicioMain < 0) {
            responderTexto(requisicao, 500, "Estrutura HTML não encontrada.");
            return;
        }

        String formulario = """
                <main class="conteudo-principal">
                  <section class="modulo-sistema">
                    <header class="cabecalho-modulo">
                      <hgroup>
                        <h2>Editar Professor (%s)</h2>
                      </hgroup>
                    </header>

                    <form class="formulario-padrao" action="/professores/atualizar" method="post">
                      <input type="hidden" name="idProfessor" value="%d">

                      <fieldset>
                        <legend>Informações do Professor</legend>

                        <label>Nome Completo
                          <input type="text" name="nome" value="%s" maxlength="150" required>
                        </label>

                        <label>CPF
                          <input type="text" name="cpf" value="%s" maxlength="14" required>
                        </label>

                        <label>CREF
                          <input type="text" name="cref" value="%s" maxlength="30" required>
                        </label>

                        <label>Especialidade
                          <input type="text" name="especialidade" value="%s" maxlength="100" required>
                        </label>

                        <label>Turno
                          <select name="turno" required>
                            <option value="Manhã" %s>Manhã</option>
                            <option value="Tarde" %s>Tarde</option>
                            <option value="Noite" %s>Noite</option>
                          </select>
                        </label>

                        <label>Telefone
                          <input type="tel" name="telefone" value="%s" maxlength="20" required>
                        </label>

                        <label>Status
                          <select name="status" required>
                            <option value="ATIVO" %s>Ativo</option>
                            <option value="INATIVO" %s>Inativo</option>
                          </select>
                        </label>

                        <footer class="acoes-formulario">
                          <a href="/pages/professores.html" class="btn btn-secundario">Cancelar</a>
                          <button type="submit" class="btn btn-primario">Salvar Alterações</button>
                        </footer>
                      </fieldset>
                    </form>
                  </section>
                </main>
                """.formatted(
                escapar(professor.getIdentificadorExibicao()),
                professor.getIdProfessor(),
                escapar(professor.getNomeCompleto()),
                escapar(professor.getCpf()),
                escapar(professor.getCref()),
                escapar(professor.getEspecialidade()),
                selecionar(professor.getTurno(), "Manhã"),
                selecionar(professor.getTurno(), "Tarde"),
                selecionar(professor.getTurno(), "Noite"),
                escapar(professor.getTelefone()),
                selecionar(professor.getStatus(), "ATIVO"),
                selecionar(professor.getStatus(), "INATIVO")
        );

        String pagina = htmlOriginal.substring(0, inicioMain) + formulario + "</body>\n</html>";
        responderHtml(requisicao, 200, pagina);
    }

    private void cadastrar(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);

        Professor professor = new Professor(
                obrigatorio(dados, "nome"),
                obrigatorio(dados, "cpf"),
                obrigatorio(dados, "cref"),
                obrigatorio(dados, "especialidade"),
                obrigatorio(dados, "turno"),
                obrigatorio(dados, "telefone")
        );

        professorDAO.cadastrar(professor);

        redirecionar(requisicao, "/pages/professores.html");
    }

    private void atualizar(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);
        int id = lerIdPositivo(dados.get("idProfessor"), "ID de professor inválido.");

        Professor professor = new Professor(
                obrigatorio(dados, "nome"),
                obrigatorio(dados, "cpf"),
                obrigatorio(dados, "cref"),
                obrigatorio(dados, "especialidade"),
                obrigatorio(dados, "turno"),
                obrigatorio(dados, "telefone")
        );

        String status = obrigatorio(dados, "status");
        if (!STATUS_VALIDOS.contains(status)) {
            throw new IllegalArgumentException("Status inválido.");
        }

        professor.setIdProfessor(id);
        professor.setStatus(status);

        if (!professorDAO.atualizar(professor)) {
            responderTexto(requisicao, 404, "Professor não encontrado.");
            return;
        }

        redirecionar(requisicao, "/pages/professores.html");
    }

    private void desativar(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);
        int id = lerIdPositivo(dados.get("idProfessor"), "ID de professor inválido.");

        if (!professorDAO.desativar(id)) {
            responderTexto(requisicao, 404, "Professor não encontrado ou já inativo.");
            return;
        }

        redirecionar(requisicao, "/pages/professores.html");
    }

    private String gerarLinhas(List<Professor> professores) {
        if (professores.isEmpty()) {
            return "<tr><td colspan=\"9\">Nenhum professor cadastrado.</td></tr>";
        }

        StringBuilder linhas = new StringBuilder();
        for (Professor professor : professores) {
            boolean ativo = professor.isAtivo();
            String classe = ativo ? "status-ativo" : "status-inativo";
            String textoStatus = ativo ? "Ativo" : "Inativo";

            linhas.append("""
                    <tr>
                      <th scope="row">%s</th>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td><span class="badge-status %s">%s</span></td>
                      <td class="celula-acoes">
                        <form action="/professores/editar" method="get" style="display:inline;">
                          <input type="hidden" name="idProfessor" value="%d">
                          <button type="submit" class="btn-editar">Editar</button>
                        </form>
                        <form action="/professores/desativar" method="post" style="display:inline;">
                          <input type="hidden" name="idProfessor" value="%d">
                          <button type="submit" class="btn-excluir" %s>Desativar</button>
                        </form>
                      </td>
                    </tr>
                    """.formatted(
                    escapar(professor.getIdentificadorExibicao()),
                    escapar(professor.getNomeCompleto()),
                    escapar(professor.getCpf()),
                    escapar(professor.getCref()),
                    escapar(professor.getEspecialidade()),
                    escapar(professor.getTurno()),
                    escapar(professor.getTelefone()),
                    classe,
                    textoStatus,
                    professor.getIdProfessor(),
                    professor.getIdProfessor(),
                    ativo ? "" : "disabled"
            ));
        }

        return linhas.toString();
    }

    private String selecionar(String atual, String esperado) {
        return esperado.equals(atual) ? "selected" : "";
    }

    @Override
    protected String mensagemDuplicidade() {
        return "Já existe um professor com este CPF ou CREF.";
    }
}
