package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.ProfessorDAO;
import br.uepa.foxfit.model.Professor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public class EditarProfessorHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"GET".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(
                    requisicao, 405, "Método não permitido."
            );
            return;
        }

        try {
            String query = requisicao.getRequestURI().getRawQuery();
            String idTexto = null;

            if (query != null) {
                for (String parte : query.split("&")) {
                    String[] campo = parte.split("=", 2);

                    String chave = URLDecoder.decode(
                            campo[0], StandardCharsets.UTF_8
                    );

                    if ("idProfessor".equals(chave)) {
                        idTexto = URLDecoder.decode(
                                campo.length > 1 ? campo[1] : "",
                                StandardCharsets.UTF_8
                        );
                        break;
                    }
                }
            }

            if (idTexto == null) {
                throw new IllegalArgumentException("ID ausente.");
            }

            int id = Integer.parseInt(idTexto);

            if (id <= 0) {
                throw new IllegalArgumentException("ID inválido.");
            }

            Professor professor = new ProfessorDAO().buscarPorId(id);

            if (professor == null) {
                WebUtils.responderTexto(
                        requisicao, 404, "Professor não encontrado."
                );
                return;
            }

            String htmlOriginal = Files.readString(
                    Path.of("frontend", "pages", "professores.html"),
                    StandardCharsets.UTF_8
            );

            int inicioMain = htmlOriginal.indexOf("<main");

            if (inicioMain < 0) {
                WebUtils.responderTexto(
                        requisicao, 500, "Estrutura HTML não encontrada."
                );
                return;
            }

            String formulario = """
                    <main>
                      <section class="modulo-sistema">
                        <header class="cabecalho-modulo">
                          <hgroup>
                            <h2>Editar Professor</h2>
                          </hgroup>
                        </header>

                        <form class="formulario-padrao"
                              action="/professores/atualizar"
                              method="post">

                          <input type="hidden"
                                 name="idProfessor" value="%d">

                          <fieldset>
                            <legend>Informações do Professor</legend>

                            <label>Nome Completo
                              <input type="text" name="nome"
                                     value="%s" maxlength="150" required>
                            </label>

                            <label>CPF
                              <input type="text" name="cpf"
                                     value="%s" maxlength="14" required>
                            </label>

                            <label>CREF
                              <input type="text" name="cref"
                                     value="%s" maxlength="30" required>
                            </label>

                            <label>Especialidade
                              <input type="text" name="especialidade"
                                     value="%s" maxlength="100" required>
                            </label>

                            <label>Turno
                              <select name="turno" required>
                                <option value="Manhã" %s>Manhã</option>
                                <option value="Tarde" %s>Tarde</option>
                                <option value="Noite" %s>Noite</option>
                              </select>
                            </label>

                            <label>Telefone
                              <input type="tel" name="telefone"
                                     value="%s" maxlength="20" required>
                            </label>

                            <label>Status
                              <select name="status" required>
                                <option value="ATIVO" %s>Ativo</option>
                                <option value="INATIVO" %s>Inativo</option>
                              </select>
                            </label>

                            <footer class="acoes-formulario">
                              <a href="/pages/professores.html"
                                 class="btn btn-secundario">Cancelar</a>
                              <button type="submit" class="btn btn-primario">
                                Salvar Alterações
                              </button>
                            </footer>
                          </fieldset>
                        </form>
                      </section>
                    </main>
                    """.formatted(
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

            String pagina = htmlOriginal.substring(0, inicioMain)
                    + formulario
                    + "</body>\n</html>";

            WebUtils.responderHtml(requisicao, 200, pagina);

        } catch (IllegalArgumentException erro) {
            WebUtils.responderTexto(
                    requisicao, 400, "ID de professor inválido."
            );

        } catch (SQLException erro) {
            erro.printStackTrace();

            WebUtils.responderTexto(
                    requisicao, 500,
                    "Erro ao consultar professor no banco."
            );
        }
    }

    private static String selecionar(String atual, String esperado) {
        return esperado.equals(atual) ? "selected" : "";
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }

        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
