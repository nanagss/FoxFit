package br.uepa.foxfit.controller;

import br.uepa.foxfit.dao.DashboardDAO;
import br.uepa.foxfit.model.ResumoDashboard;
import br.uepa.foxfit.model.ResumoDashboard.MatriculaRecente;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller responsável pelo painel gerencial (/pages/dashboard.html),
 * exibindo os 4 KPIs e as 5 matrículas mais recentes vindas do PostgreSQL.
 */
public class DashboardController extends BaseHandler {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");

    private final DashboardDAO dashboardDAO = new DashboardDAO();

    @Override
    protected void processarGet(HttpExchange requisicao) throws IOException, SQLException {
        ResumoDashboard resumo = dashboardDAO.carregarResumo();
        String html = lerPaginaHtml("dashboard.html");

        StringBuilder linhas = new StringBuilder();
        BigDecimal totalLoteRecente = BigDecimal.ZERO;

        for (MatriculaRecente item : resumo.getMatriculasRecentes()) {
            totalLoteRecente = totalLoteRecente.add(item.valorTotal());

            String statusUpper = item.status().toUpperCase(Locale.ROOT);
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
                    rotuloStatus = item.status();
                }
            }

            linhas.append("<tr>")
                    .append("<th scope=\"row\">").append(escapar(item.codigo())).append("</th>")
                    .append("<td>").append(escapar(item.nomeAluno())).append("</td>")
                    .append("<td>").append(escapar(item.nomePlano())).append("</td>")
                    .append("<td><time datetime=\"").append(item.dataInicio()).append("\">")
                    .append(item.dataInicio().format(FORMATO_DATA)).append("</time></td>")
                    .append("<td><time datetime=\"").append(item.dataVencimento()).append("\">")
                    .append(item.dataVencimento().format(FORMATO_DATA)).append("</time></td>")
                    .append("<td>").append(escapar(item.formaPagamento())).append("</td>")
                    .append("<td>").append(formatarMoeda(item.valorTotal())).append("</td>")
                    .append("<td><span class=\"badge-status ").append(classeStatus).append("\">")
                    .append(escapar(rotuloStatus)).append("</span></td>")
                    .append("</tr>");
        }

        if (linhas.isEmpty()) {
            linhas.append("<tr><td colspan=\"8\">Nenhuma matrícula recente registrada.</td></tr>");
        }

        String taxaFormatada = String.format(LOCALE_BR, "%.1f%%", resumo.getTaxaAdimplencia());

        html = html.replace("<!-- KPI_ALUNOS_ATIVOS -->", String.valueOf(resumo.getTotalAlunosAtivos()));
        html = html.replace("<!-- KPI_MATRICULAS_MES -->", String.valueOf(resumo.getMatriculasNoMes()));
        html = html.replace("<!-- KPI_RECEITA_MENSAL -->", formatarMoeda(resumo.getReceitaMensal()));
        html = html.replace("<!-- KPI_TAXA_ADIMPLENCIA -->", taxaFormatada);
        html = html.replace("<!-- LINHAS_MATRICULAS_RECENTES -->", linhas.toString());
        html = html.replace("<!-- TOTAL_MATRICULAS_RECENTES -->", formatarMoeda(totalLoteRecente));

        responderHtml(requisicao, 200, html);
    }
}
