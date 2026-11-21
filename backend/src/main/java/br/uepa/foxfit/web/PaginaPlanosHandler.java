package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.PlanoDAO;
import br.uepa.foxfit.model.Plano;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Handler responsável por renderizar a página /pages/planos.html com os dados do PostgreSQL.
 */
public class PaginaPlanosHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"GET".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            PlanoDAO dao = new PlanoDAO();
            List<Plano> planos = dao.listar();

            String html = Files.readString(
                    Path.of("frontend", "pages", "planos.html"),
                    StandardCharsets.UTF_8
            );

            if (!html.contains("<!-- CARDS_PLANOS -->")
                    || !html.contains("<!-- LINHAS_PLANOS -->")
                    || !html.contains("<!-- TOTAL_PLANOS -->")) {
                throw new IOException("Marcadores não encontrados em planos.html");
            }

            StringBuilder cards = new StringBuilder();
            StringBuilder linhas = new StringBuilder();
            int ativos = 0;

            for (Plano plano : planos) {
                boolean isAtivo = "ATIVO".equalsIgnoreCase(plano.getStatus());
                if (isAtivo) {
                    ativos++;
                    cards.append(construirCardPlano(plano));
                }

                String duracaoTexto = plano.getDuracaoMeses() == 1
                        ? "1 mês"
                        : plano.getDuracaoMeses() + " meses";

                String classeStatus = isAtivo ? "status-ativo" : "status-inativo";
                String rotuloStatus = isAtivo ? "Ativo" : "Inativo";

                linhas.append("<tr>")
                        .append("<th scope=\"row\">").append(WebUtils.escapar(plano.getCodigo())).append("</th>")
                        .append("<td>").append(WebUtils.escapar(plano.getNome())).append("</td>")
                        .append("<td>").append(duracaoTexto).append("</td>")
                        .append("<td>").append(WebUtils.formatarMoeda(plano.getValorMensal())).append("</td>")
                        .append("<td>").append(WebUtils.formatarMoeda(plano.getValorTotal())).append("</td>")
                        .append("<td><span class=\"badge-status ").append(classeStatus).append("\">")
                        .append(rotuloStatus).append("</span></td>")
                        .append("<td class=\"celula-acoes\">")
                        .append("<a class=\"btn-editar\" href=\"/pages/planos.html?editar=")
                        .append(plano.getId())
                        .append("#form-editar-plano-dinamico\">Editar</a> ")
                        .append("<a class=\"btn-excluir\" href=\"/pages/planos.html?excluir=")
                        .append(plano.getId())
                        .append("#confirmar-exclusao-plano\">Excluir</a>")
                        .append("</td>")
                        .append("</tr>");
            }

            if (cards.isEmpty()) {
                cards.append("<article class=\"card-plano\"><p>Nenhum plano ativo cadastrado no momento.</p></article>");
            }

            if (linhas.isEmpty()) {
                linhas.append("<tr><td colspan=\"7\">Nenhum plano cadastrado no catálogo.</td></tr>");
            }

            String formularioDinamico = construirFormularioDinamico(requisicao, dao);

            String totalTexto = planos.size() + (planos.size() == 1 ? " plano cadastrado" : " planos cadastrados")
                    + " (" + ativos + (ativos == 1 ? " ativo)" : " ativos)");

            html = html.replace("<!-- CARDS_PLANOS -->", cards.toString());
            html = html.replace("<!-- FORMULARIO_EDICAO_PLANO -->", formularioDinamico);
            html = html.replace("<!-- LINHAS_PLANOS -->", linhas.toString());
            html = html.replace("<!-- TOTAL_PLANOS -->", totalTexto);

            WebUtils.responderHtml(requisicao, 200, html);

        } catch (SQLException | IOException erro) {
            erro.printStackTrace();
            WebUtils.responderTexto(requisicao, 500, "Erro ao carregar os planos de assinatura.");
        }
    }

    private String construirCardPlano(Plano plano) {
        String duracao = plano.getDuracaoMeses() == 1
                ? "Duração: 1 Mês"
                : "Duração: " + plano.getDuracaoMeses() + " Meses";

        String complementoPreco = plano.getDuracaoMeses() == 1
                ? "/ mês"
                : "/ mês (Total " + WebUtils.formatarMoeda(plano.getValorTotal()) + ")";

        StringBuilder listaBeneficios = new StringBuilder();
        String textoBeneficios = plano.getBeneficios() != null ? plano.getBeneficios() : "";

        for (String linha : textoBeneficios.split("\\r?\\n")) {
            String item = linha.trim();
            if (!item.isEmpty()) {
                listaBeneficios.append("<li>").append(WebUtils.escapar(item)).append("</li>");
            }
        }

        return """
                <article class="card-plano">
                  <header class="cabecalho-plano">
                    <h3>%s</h3>
                    <span class="duracao-plano">%s</span>
                  </header>
                  <p class="preco-plano">
                    %s
                    <small>%s</small>
                  </p>
                  <ul class="lista-beneficios">
                    %s
                  </ul>
                  <footer>
                    <a href="matriculas.html?plano=%d" class="btn btn-secundario btn-bloco">Selecionar no Balcão</a>
                  </footer>
                </article>
                """.formatted(
                WebUtils.escapar(plano.getNome()),
                duracao,
                WebUtils.formatarMoeda(plano.getValorMensal()),
                complementoPreco,
                listaBeneficios,
                plano.getId()
        );
    }

    private String construirFormularioDinamico(HttpExchange requisicao, PlanoDAO dao) throws SQLException {
        Map<String, String> query = WebUtils.lerQuery(requisicao);

        String editarParam = query.get("editar");
        if (editarParam != null && editarParam.matches("[1-9][0-9]{0,8}")) {
            Plano selecionado = dao.buscarPorId(Integer.parseInt(editarParam));
            if (selecionado != null) {
                String selAtivo = "ATIVO".equalsIgnoreCase(selecionado.getStatus()) ? "selected" : "";
                String selInativo = "INATIVO".equalsIgnoreCase(selecionado.getStatus()) ? "selected" : "";

                return """
                        <section class="modulo-sistema">
                          <header class="cabecalho-modulo">
                            <h2>Editar Plano de Assinatura (%s)</h2>
                          </header>
                          <form id="form-editar-plano-dinamico" class="formulario-padrao" action="/planos/atualizar" method="post">
                            <input type="hidden" name="idPlano" value="%d">
                            <fieldset>
                              <legend>Parâmetros do Plano Selecionado</legend>
                              <label>Nome do Plano
                                <input type="text" name="nomePlano" value="%s" required>
                              </label>
                              <label>Valor R$ (Mensal)
                                <input type="number" name="valor" value="%s" step="0.01" min="0" required>
                              </label>
                              <label>Duração em Meses
                                <input type="number" name="duracaoMeses" value="%d" min="1" max="36" required>
                              </label>
                              <label>Status do Plano
                                <select name="status" required>
                                  <option value="ATIVO" %s>Ativo</option>
                                  <option value="INATIVO" %s>Inativo</option>
                                </select>
                              </label>
                              <label class="campo-completo">Benefícios do Plano (um benefício por linha)
                                <textarea name="beneficios" required>%s</textarea>
                              </label>
                              <footer class="acoes-formulario">
                                <a href="/pages/planos.html" class="btn btn-secundario">Cancelar</a>
                                <button type="submit" class="btn btn-primario">Salvar Alterações</button>
                              </footer>
                            </fieldset>
                          </form>
                        </section>
                        """.formatted(
                        WebUtils.escapar(selecionado.getCodigo()),
                        selecionado.getId(),
                        WebUtils.escapar(selecionado.getNome()),
                        selecionado.getValorMensal().toPlainString(),
                        selecionado.getDuracaoMeses(),
                        selAtivo,
                        selInativo,
                        WebUtils.escapar(selecionado.getBeneficios())
                );
            }
        }

        String excluirParam = query.get("excluir");
        if (excluirParam != null && excluirParam.matches("[1-9][0-9]{0,8}")) {
            Plano selecionado = dao.buscarPorId(Integer.parseInt(excluirParam));
            if (selecionado != null) {
                return """
                        <section class="modulo-sistema" id="confirmar-exclusao-plano">
                          <header class="cabecalho-modulo">
                            <h2>Confirmar Exclusão de Plano</h2>
                          </header>
                          <p>Deseja realmente excluir o plano <strong>%s</strong> (%s)?</p>
                          <p>Planos que possuam matrículas vinculadas não podem ser excluídos — nesse caso, altere o status para <strong>Inativo</strong>.</p>
                          <form action="/planos/excluir" method="post">
                            <input type="hidden" name="idPlano" value="%d">
                            <footer class="acoes-formulario">
                              <a href="/pages/planos.html" class="btn btn-secundario">Cancelar</a>
                              <button type="submit" class="btn btn-excluir">Confirmar Exclusão</button>
                            </footer>
                          </form>
                        </section>
                        """.formatted(
                        WebUtils.escapar(selecionado.getNome()),
                        WebUtils.escapar(selecionado.getCodigo()),
                        selecionado.getId()
                );
            }
        }

        return "";
    }
}
