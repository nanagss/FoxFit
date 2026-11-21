package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.FinanceiroDAO;
import br.uepa.foxfit.model.Mensalidade;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handler responsável por renderizar a página /pages/financeiro.html
 * com indicadores financeiros, filtros e baixa de mensalidades do PostgreSQL.
 */
public class PaginaFinanceiroHandler implements HttpHandler {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"GET".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            Map<String, String> filtros = WebUtils.lerQuery(requisicao);

            String statusSolicitado = filtros.getOrDefault("statusFinanceiro", "TODOS")
                    .toUpperCase(Locale.ROOT);

            String filtroStatus = statusSolicitado.equals("PAGO")
                    || statusSolicitado.equals("PENDENTE")
                    || statusSolicitado.equals("ATRASADO")
                    ? statusSolicitado
                    : "TODOS";

            String mesCompetencia = filtros.getOrDefault("mesCompetencia", "").trim();
            if (!mesCompetencia.matches("\\d{4}-\\d{2}")) {
                mesCompetencia = "";
            }

            String termoAluno = filtros.getOrDefault("termoAluno", "").trim();
            String termoNormalizado = WebUtils.normalizar(termoAluno);

            FinanceiroDAO dao = new FinanceiroDAO();

            BigDecimal totalRecebido = dao.somarPorStatus("PAGO", mesCompetencia);
            BigDecimal totalPendente = dao.somarPorStatus("PENDENTE", mesCompetencia);
            BigDecimal totalAtraso = dao.somarPorStatus("ATRASADO", mesCompetencia);

            List<Mensalidade> mensalidades = dao.listar(filtroStatus, mesCompetencia);

            if (!termoNormalizado.isEmpty()) {
                mensalidades = mensalidades.stream()
                        .filter(m -> WebUtils.normalizar(m.getNomeAluno()).contains(termoNormalizado)
                                || WebUtils.normalizar(m.getCodigoMatricula()).contains(termoNormalizado)
                                || WebUtils.normalizar(m.getNomePlano()).contains(termoNormalizado))
                        .toList();
            }

            String html = Files.readString(
                    Path.of("frontend", "pages", "financeiro.html"),
                    StandardCharsets.UTF_8
            );

            StringBuilder linhas = new StringBuilder();
            BigDecimal somaListada = BigDecimal.ZERO;
            BigDecimal somaPagaListada = BigDecimal.ZERO;
            BigDecimal somaPendenteListada = BigDecimal.ZERO;
            BigDecimal somaAtrasadaListada = BigDecimal.ZERO;

            for (Mensalidade m : mensalidades) {
                somaListada = somaListada.add(m.getValor());

                String statusUpper = m.getStatus().toUpperCase(Locale.ROOT);
                String classeBadge;
                String rotuloStatus;

                switch (statusUpper) {
                    case "PAGO" -> {
                        classeBadge = "status-pago";
                        rotuloStatus = "Pago";
                        somaPagaListada = somaPagaListada.add(m.getValor());
                    }
                    case "ATRASADO" -> {
                        classeBadge = "status-atrasado";
                        rotuloStatus = "Atrasado";
                        somaAtrasadaListada = somaAtrasadaListada.add(m.getValor());
                    }
                    default -> {
                        classeBadge = "status-pendente";
                        rotuloStatus = "Pendente";
                        somaPendenteListada = somaPendenteListada.add(m.getValor());
                    }
                }

                String colunaVencimento = m.getDataVencimento() != null
                        ? "<time datetime=\"" + m.getDataVencimento() + "\">" + m.getDataVencimento().format(FORMATO_DATA) + "</time>"
                        : "—";

                String colunaPagamento = m.getDataPagamento() != null
                        ? "<time datetime=\"" + m.getDataPagamento() + "\">" + m.getDataPagamento().format(FORMATO_DATA) + "</time>"
                        : "—";

                String colunaAcao;
                if ("PAGO".equals(statusUpper)) {
                    colunaAcao = "<span class=\"badge-status status-pago\">Quitado</span>";
                } else {
                    colunaAcao = """
                            <form action="/financeiro/pagar" method="post">
                              <input type="hidden" name="idMensalidade" value="%d">
                              <button type="submit" class="btn-pagar" aria-label="Confirmar pagamento de %s">
                                Confirmar Pagamento
                              </button>
                            </form>
                            """.formatted(m.getId(), WebUtils.escapar(m.getNomeAluno()));
                }

                linhas.append("<tr>")
                        .append("<th scope=\"row\">").append(WebUtils.escapar(m.getNomeAluno())).append("</th>")
                        .append("<td>").append(WebUtils.escapar(m.getNomePlano()))
                        .append(" (").append(WebUtils.escapar(m.getCodigoMatricula())).append(")</td>")
                        .append("<td>").append(WebUtils.formatarMoeda(m.getValor())).append("</td>")
                        .append("<td>").append(colunaVencimento).append("</td>")
                        .append("<td>").append(colunaPagamento).append("</td>")
                        .append("<td><span class=\"badge-status ").append(classeBadge).append("\">")
                        .append(rotuloStatus).append("</span></td>")
                        .append("<td class=\"celula-acoes\">").append(colunaAcao).append("</td>")
                        .append("</tr>");
            }

            if (linhas.isEmpty()) {
                linhas.append("<tr><td colspan=\"7\">Nenhuma mensalidade encontrada para os filtros selecionados.</td></tr>");
            }

            String resumoRodape = WebUtils.formatarMoeda(somaListada)
                    + " (" + WebUtils.formatarMoeda(somaPagaListada) + " pagos • "
                    + WebUtils.formatarMoeda(somaPendenteListada) + " pendentes • "
                    + WebUtils.formatarMoeda(somaAtrasadaListada) + " em atraso)";

            html = html.replace("<!-- FIN_TOTAL_RECEBIDO -->", WebUtils.formatarMoeda(totalRecebido));
            html = html.replace("<!-- FIN_TOTAL_PENDENTE -->", WebUtils.formatarMoeda(totalPendente));
            html = html.replace("<!-- FIN_TOTAL_ATRASO -->", WebUtils.formatarMoeda(totalAtraso));

            html = html.replace("<!-- SEL_FIN_TODOS -->", filtroStatus.equals("TODOS") ? "selected" : "");
            html = html.replace("<!-- SEL_FIN_PAGO -->", filtroStatus.equals("PAGO") ? "selected" : "");
            html = html.replace("<!-- SEL_FIN_PENDENTE -->", filtroStatus.equals("PENDENTE") ? "selected" : "");
            html = html.replace("<!-- SEL_FIN_ATRASADO -->", filtroStatus.equals("ATRASADO") ? "selected" : "");
            html = html.replace("<!-- MES_COMPETENCIA -->", WebUtils.escapar(mesCompetencia));
            html = html.replace("<!-- TERMO_ALUNO -->", WebUtils.escapar(termoAluno));

            html = html.replace("<!-- LINHAS_MENSALIDADES -->", linhas.toString());
            html = html.replace("<!-- QTD_MENSALIDADES -->", String.valueOf(mensalidades.size()));
            html = html.replace("<!-- RESUMO_LISTAGEM_MENSALIDADES -->", resumoRodape);

            WebUtils.responderHtml(requisicao, 200, html);

        } catch (SQLException | IOException erro) {
            erro.printStackTrace();
            WebUtils.responderTexto(requisicao, 500, "Erro ao carregar o fluxo financeiro.");
        }
    }
}
