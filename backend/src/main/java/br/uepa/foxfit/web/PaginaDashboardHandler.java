package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.DashboardDAO;
import br.uepa.foxfit.model.MatriculaRecente;
import br.uepa.foxfit.model.ResumoDashboard;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Handler responsável por renderizar /pages/dashboard.html com os 4 KPIs
 * e as 5 matrículas mais recentes vindas diretamente do PostgreSQL.
 */
public class PaginaDashboardHandler implements HttpHandler {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"GET".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            ResumoDashboard resumo = new DashboardDAO().carregarResumo();

            String html = Files.readString(
                    Path.of("frontend", "pages", "dashboard.html"),
                    StandardCharsets.UTF_8
            );

            StringBuilder linhas = new StringBuilder();
            BigDecimal totalLoteRecente = BigDecimal.ZERO;

            for (MatriculaRecente item : resumo.getMatriculasRecentes()) {
                totalLoteRecente = totalLoteRecente.add(item.getValorTotal());

                String statusUpper = item.getStatus().toUpperCase(Locale.ROOT);
                String classeStatus;
                String rotuloStatus;

                switch (statusUpper) {
                    case "ATIVA" -> {
                        classeStatus = "status-ativo";
                        rotuloStatus = "Ativa";
                    }
                    case "VENCIDA" -> {
                        classeStatus = "status-vencido";
                        rotuloStatus = "Vencida";
                    }
                    case "CANCELADA" -> {
                        classeStatus = "status-cancelado";
                        rotuloStatus = "Cancelada";
                    }
                    default -> {
                        classeStatus = "status-pendente";
                        rotuloStatus = item.getStatus();
                    }
                }

                linhas.append("<tr>")
                        .append("<th scope=\"row\">").append(WebUtils.escapar(item.getCodigo())).append("</th>")
                        .append("<td>").append(WebUtils.escapar(item.getNomeAluno())).append("</td>")
                        .append("<td>").append(WebUtils.escapar(item.getNomePlano())).append("</td>")
                        .append("<td><time datetime=\"").append(item.getDataInicio()).append("\">")
                        .append(item.getDataInicio().format(FORMATO_DATA)).append("</time></td>")
                        .append("<td><time datetime=\"").append(item.getDataVencimento()).append("\">")
                        .append(item.getDataVencimento().format(FORMATO_DATA)).append("</time></td>")
                        .append("<td>").append(WebUtils.escapar(item.getFormaPagamento())).append("</td>")
                        .append("<td>").append(WebUtils.formatarMoeda(item.getValorTotal())).append("</td>")
                        .append("<td><span class=\"badge-status ").append(classeStatus).append("\">")
                        .append(WebUtils.escapar(rotuloStatus)).append("</span></td>")
                        .append("</tr>");
            }

            if (linhas.isEmpty()) {
                linhas.append("<tr><td colspan=\"8\">Nenhuma matrícula recente registrada.</td></tr>");
            }

            String taxaFormatada = String.format(LOCALE_BR, "%.1f%%", resumo.getTaxaAdimplencia());

            html = html.replace("<!-- KPI_ALUNOS_ATIVOS -->", String.valueOf(resumo.getTotalAlunosAtivos()));
            html = html.replace("<!-- KPI_MATRICULAS_MES -->", String.valueOf(resumo.getMatriculasNoMes()));
            html = html.replace("<!-- KPI_RECEITA_MENSAL -->", WebUtils.formatarMoeda(resumo.getReceitaMensal()));
            html = html.replace("<!-- KPI_TAXA_ADIMPLENCIA -->", taxaFormatada);
            html = html.replace("<!-- LINHAS_MATRICULAS_RECENTES -->", linhas.toString());
            html = html.replace("<!-- TOTAL_MATRICULAS_RECENTES -->", WebUtils.formatarMoeda(totalLoteRecente));

            WebUtils.responderHtml(requisicao, 200, html);

        } catch (SQLException | IOException erro) {
            erro.printStackTrace();
            WebUtils.responderTexto(requisicao, 500, "Erro ao carregar o painel operacional (Dashboard).");
        }
    }
}
