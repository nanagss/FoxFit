package br.uepa.foxfit.controller;

import br.uepa.foxfit.dao.FinanceiroDAO;
import br.uepa.foxfit.model.Mensalidade;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller responsável pelo módulo Financeiro
 * (/pages/financeiro.html e /financeiro/pagar).
 */
public class FinanceiroController extends BaseHandler {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinanceiroDAO financeiroDAO = new FinanceiroDAO();

    @Override
    protected void processarGet(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> filtros = lerQuery(requisicao);

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
        String termoNormalizado = normalizar(termoAluno);

        BigDecimal totalRecebido = financeiroDAO.somarPorStatus("PAGO", mesCompetencia);
        BigDecimal totalPendente = financeiroDAO.somarPorStatus("PENDENTE", mesCompetencia);
        BigDecimal totalAtraso = financeiroDAO.somarPorStatus("ATRASADO", mesCompetencia);

        List<Mensalidade> mensalidades = financeiroDAO.listar(filtroStatus, mesCompetencia);

        if (!termoNormalizado.isEmpty()) {
            mensalidades = mensalidades.stream()
                    .filter(m -> normalizar(m.getNomeAluno()).contains(termoNormalizado)
                            || normalizar(m.getCodigoMatricula()).contains(termoNormalizado)
                            || normalizar(m.getNomePlano()).contains(termoNormalizado))
                    .toList();
        }

        String html = lerPaginaHtml("financeiro.html");

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

            String colunaAcao = m.isPago()
                    ? "<span class=\"badge-status status-pago\">Quitado</span>"
                    : """
                      <form action="/financeiro/pagar" method="post">
                        <input type="hidden" name="idMensalidade" value="%d">
                        <button type="submit" class="btn-pagar" aria-label="Confirmar pagamento de %s">
                          Confirmar Pagamento
                        </button>
                      </form>
                      """.formatted(m.getId(), escapar(m.getNomeAluno()));

            linhas.append("<tr>")
                    .append("<th scope=\"row\">").append(escapar(m.getNomeAluno())).append("</th>")
                    .append("<td>").append(escapar(m.getNomePlano()))
                    .append(" (").append(escapar(m.getCodigoMatricula())).append(")</td>")
                    .append("<td>").append(formatarMoeda(m.getValor())).append("</td>")
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

        String resumoRodape = formatarMoeda(somaListada)
                + " (" + formatarMoeda(somaPagaListada) + " pagos • "
                + formatarMoeda(somaPendenteListada) + " pendentes • "
                + formatarMoeda(somaAtrasadaListada) + " em atraso)";

        html = html.replace("<!-- FIN_TOTAL_RECEBIDO -->", formatarMoeda(totalRecebido));
        html = html.replace("<!-- FIN_TOTAL_PENDENTE -->", formatarMoeda(totalPendente));
        html = html.replace("<!-- FIN_TOTAL_ATRASO -->", formatarMoeda(totalAtraso));

        html = html.replace("<!-- SEL_FIN_TODOS -->", filtroStatus.equals("TODOS") ? "selected" : "");
        html = html.replace("<!-- SEL_FIN_PAGO -->", filtroStatus.equals("PAGO") ? "selected" : "");
        html = html.replace("<!-- SEL_FIN_PENDENTE -->", filtroStatus.equals("PENDENTE") ? "selected" : "");
        html = html.replace("<!-- SEL_FIN_ATRASADO -->", filtroStatus.equals("ATRASADO") ? "selected" : "");
        html = html.replace("<!-- MES_COMPETENCIA -->", escapar(mesCompetencia));
        html = html.replace("<!-- TERMO_ALUNO -->", escapar(termoAluno));

        html = html.replace("<!-- LINHAS_MENSALIDADES -->", linhas.toString());
        html = html.replace("<!-- QTD_MENSALIDADES -->", String.valueOf(mensalidades.size()));
        html = html.replace("<!-- RESUMO_LISTAGEM_MENSALIDADES -->", resumoRodape);

        responderHtml(requisicao, 200, html);
    }

    @Override
    protected void processarPost(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);
        int id = lerIdPositivo(dados.get("idMensalidade"), "ID de mensalidade inválido.");

        if (!financeiroDAO.confirmarPagamento(id)) {
            responderTexto(requisicao, 404, "Mensalidade não encontrada.");
            return;
        }

        redirecionar(requisicao, "/pages/financeiro.html");
    }
}
