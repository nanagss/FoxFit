package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.ProfessorDAO;
import br.uepa.foxfit.model.Professor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public class PaginaProfessoresHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"GET".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(
                    requisicao, 405, "Método não permitido."
            );
            return;
        }

        try {
            List<Professor> professores = new ProfessorDAO().listar();

            String html = Files.readString(
                    Path.of("frontend", "pages", "professores.html"),
                    StandardCharsets.UTF_8
            );

            String inicioTabela =
                    "<tbody id=\"corpo-tabela-professores\">";

            int inicio = html.indexOf(inicioTabela);
            int fim = html.indexOf("</tbody>", inicio);

            if (inicio < 0 || fim < 0) {
                throw new IOException(
                        "Tabela de professores não encontrada no HTML."
                );
            }

            html = html.substring(0, inicio + inicioTabela.length())
                    + "\n"
                    + gerarLinhas(professores)
                    + "\n"
                    + html.substring(fim);

            int inicioRodape = html.indexOf("<tfoot>");
            int fimRodape = html.indexOf("</tfoot>", inicioRodape);

            if (inicioRodape < 0 || fimRodape < 0) {
                throw new IOException(
                        "Rodapé da tabela não encontrado."
                );
            }

            long ativos = professores.stream()
                    .filter(p -> "ATIVO".equalsIgnoreCase(p.getStatus()))
                    .count();

            String total = ativos == 1
                    ? "1 professor ativo"
                    : ativos + " professores ativos";

            String rodape = """
                    <tfoot>
                      <tr>
                        <td colspan="7">
                          Total de profissionais técnicos habilitados na unidade
                        </td>
                        <td colspan="2">%s</td>
                      </tr>
                    </tfoot>
                    """.formatted(total);

            html = html.substring(0, inicioRodape)
                    + rodape
                    + html.substring(fimRodape + "</tfoot>".length());

            WebUtils.responderHtml(requisicao, 200, html);

        } catch (SQLException erro) {
            erro.printStackTrace();

            WebUtils.responderTexto(
                    requisicao, 500,
                    "Erro ao consultar professores no banco."
            );
        }
    }

    private static String gerarLinhas(List<Professor> professores) {

        if (professores.isEmpty()) {
            return """
                    <tr>
                      <td colspan="9">Nenhum professor cadastrado.</td>
                    </tr>
                    """;
        }

        StringBuilder linhas = new StringBuilder();

        for (Professor professor : professores) {

            boolean ativo = "ATIVO".equalsIgnoreCase(
                    professor.getStatus()
            );

            String classe = ativo ? "status-ativo" : "status-inativo";
            String textoStatus = ativo ? "Ativo" : "Inativo";

            String linha = """
                    <tr>
                      <th scope="row">%s</th>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>
                        <span class="badge-status %s">%s</span>
                      </td>
                      <td class="celula-acoes">
                        <form action="/professores/editar"
                              method="get"
                              style="display:inline;">
                            <input type="hidden"
                                   name="idProfessor"
                                   value="%d">
                            <button type="submit" class="btn-editar">
                                Editar
                            </button>
                        </form>
                        <form action="/professores/desativar"
                              method="post"
                              style="display:inline;">
                          <input type="hidden"
                                 name="idProfessor"
                                 value="%d">
                          <button type="submit"
                                  class="btn-excluir" %s>
                            Desativar
                          </button>
                        </form>
                      </td>
                    </tr>
                    """.formatted(
                    escapar(professor.getCodigo()),
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
            );

            linhas.append(linha);
        }

        return linhas.toString();
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }

        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
