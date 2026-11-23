package br.uepa.foxfit.controller;

import br.uepa.foxfit.dao.AlunoDAO;
import br.uepa.foxfit.dao.MatriculaDAO;
import br.uepa.foxfit.dao.PlanoDAO;
import br.uepa.foxfit.dao.RepositorioCRUD;
import br.uepa.foxfit.model.Aluno;
import br.uepa.foxfit.model.Matricula;
import br.uepa.foxfit.model.Plano;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller responsável pelo módulo de Controle de Matrículas (RF05):
 * /pages/matriculas.html, /matriculas/cadastrar, /matriculas/renovar e /matriculas/cancelar.
 */
public class MatriculaController extends BaseHandler {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RepositorioCRUD<Aluno> alunoDAO = new AlunoDAO();
    private final RepositorioCRUD<Plano> planoDAO = new PlanoDAO();
    private final MatriculaDAO matriculaDAO = new MatriculaDAO();

    @Override
    protected void processarGet(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> query = lerQuery(requisicao);
        String planoPreSelecionado = query.getOrDefault("plano", "").trim();

        List<Aluno> alunos = alunoDAO.listar();
        List<Plano> planos = planoDAO.listar();
        List<Matricula> matriculas = matriculaDAO.listar();

        String html = lerPaginaHtml("matriculas.html");

        StringBuilder opcoesAlunos = new StringBuilder();
        for (Aluno aluno : alunos) {
            if (aluno.isAtivo()) {
                opcoesAlunos.append("<option value=\"")
                        .append(aluno.getIdAluno())
                        .append("\">")
                        .append(escapar(aluno.getNomeCompleto()))
                        .append(" (CPF: ")
                        .append(escapar(aluno.getCpf()))
                        .append(")</option>");
            }
        }

        StringBuilder opcoesPlanos = new StringBuilder();
        LocalDate hoje = LocalDate.now();
        LocalDate vencimentoPrevisto = hoje.plusMonths(1);

        for (Plano plano : planos) {
            if (plano.isAtivo()) {
                boolean selecionado = String.valueOf(plano.getId()).equals(planoPreSelecionado);
                if (selecionado) {
                    vencimentoPrevisto = Matricula.calcularVencimento(hoje, plano.getDuracaoMeses());
                }

                String unidade = plano.getDuracaoMeses() == 1 ? "Mês" : "Meses";
                opcoesPlanos.append("<option value=\"")
                        .append(plano.getId())
                        .append("\"")
                        .append(selecionado ? " selected" : "")
                        .append(">")
                        .append(escapar(plano.getNome()))
                        .append(" — ")
                        .append(formatarMoeda(plano.getValorTotal()))
                        .append(" (")
                        .append(plano.getDuracaoMeses())
                        .append(" ")
                        .append(unidade)
                        .append(")</option>");
            }
        }

        StringBuilder linhas = new StringBuilder();
        int ativas = 0;
        int vencidas = 0;
        int canceladas = 0;

        for (Matricula mat : matriculas) {
            String statusUpper = mat.getStatus().toUpperCase(Locale.ROOT);
            String classeBadge;
            String rotuloStatus;

            switch (statusUpper) {
                case "ATIVA" -> {
                    ativas++;
                    classeBadge = "status-ativo";
                    rotuloStatus = "Ativa";
                }
                case "VENCIDA" -> {
                    vencidas++;
                    classeBadge = "status-vencido";
                    rotuloStatus = "Vencida";
                }
                case "CANCELADA" -> {
                    canceladas++;
                    classeBadge = "status-cancelado";
                    rotuloStatus = "Cancelada";
                }
                default -> {
                    classeBadge = "status-pendente";
                    rotuloStatus = mat.getStatus();
                }
            }

            String botaoCancelar = "CANCELADA".equals(statusUpper)
                    ? ""
                    : """
                      <form action="/matriculas/cancelar" method="post" style="display:inline;">
                        <input type="hidden" name="idMatricula" value="%d">
                        <button type="submit" class="btn-cancelar" aria-label="Cancelar matrícula de %s">Cancelar</button>
                      </form>
                      """.formatted(mat.getId(), escapar(mat.getNomeAluno()));

            linhas.append("""
                    <tr>
                      <th scope="row">%s</th>
                      <td>%s</td>
                      <td>%s</td>
                      <td><time datetime="%s">%s</time></td>
                      <td><time datetime="%s">%s</time></td>
                      <td>%s</td>
                      <td><span class="badge-status %s">%s</span></td>
                      <td class="celula-acoes">
                        <form action="/matriculas/renovar" method="post" style="display:inline;">
                          <input type="hidden" name="idMatricula" value="%d">
                          <button type="submit" class="btn-renovar" aria-label="Renovar matrícula de %s">Renovar</button>
                        </form>
                        %s
                      </td>
                    </tr>
                    """.formatted(
                    escapar(mat.getIdentificadorExibicao()),
                    escapar(mat.getNomeAluno()),
                    escapar(mat.getNomePlano()),
                    mat.getDataInicio(),
                    mat.getDataInicio().format(FORMATO_DATA),
                    mat.getDataVencimento(),
                    mat.getDataVencimento().format(FORMATO_DATA),
                    escapar(mat.getFormaPagamento()),
                    classeBadge,
                    rotuloStatus,
                    mat.getId(),
                    escapar(mat.getNomeAluno()),
                    botaoCancelar
            ));
        }

        if (linhas.isEmpty()) {
            linhas.append("<tr><td colspan=\"8\">Nenhuma matrícula registrada na unidade.</td></tr>");
        }

        String resumo = ativas + (ativas == 1 ? " Ativa" : " Ativas")
                + " • " + vencidas + (vencidas == 1 ? " Vencida" : " Vencidas")
                + " • " + canceladas + (canceladas == 1 ? " Cancelada" : " Canceladas");

        html = html.replace("<!-- OPCOES_ALUNOS -->", opcoesAlunos.toString());
        html = html.replace("<!-- OPCOES_PLANOS -->", opcoesPlanos.toString());
        html = html.replace("<!-- DATA_INICIO_PADRAO -->", hoje.toString());
        html = html.replace("<!-- DATA_VENCIMENTO_PADRAO -->", vencimentoPrevisto.toString());
        html = html.replace("<!-- LINHAS_MATRICULAS -->", linhas.toString());
        html = html.replace("<!-- RESUMO_MATRICULAS -->", resumo);

        responderHtml(requisicao, 200, html);
    }

    @Override
    protected void processarPost(HttpExchange requisicao) throws IOException, SQLException {
        switch (obterRota(requisicao)) {
            case "/matriculas/cadastrar" -> cadastrar(requisicao);
            case "/matriculas/renovar" -> renovar(requisicao);
            case "/matriculas/cancelar" -> cancelar(requisicao);
            default -> responderTexto(requisicao, 404, "Ação de matrículas não encontrada.");
        }
    }

    private void cadastrar(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);

        int idAluno = lerIdPositivo(dados.get("idAluno"), "Selecione um aluno válido.");
        int idPlano = lerIdPositivo(dados.get("idPlano"), "Selecione um plano válido.");
        LocalDate dataInicio = LocalDate.parse(obrigatorio(dados, "dataInicio"));
        String formaPagamento = obrigatorio(dados, "formaPagamento");

        Matricula matricula = new Matricula(idAluno, idPlano, dataInicio, dataInicio, formaPagamento);
        matriculaDAO.cadastrar(matricula);

        redirecionar(requisicao, "/pages/matriculas.html");
    }

    private void renovar(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);
        int id = lerIdPositivo(dados.get("idMatricula"), "ID de matrícula inválido.");

        if (!matriculaDAO.renovar(id)) {
            responderTexto(requisicao, 404, "Matrícula não encontrada.");
            return;
        }

        redirecionar(requisicao, "/pages/matriculas.html");
    }

    private void cancelar(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);
        int id = lerIdPositivo(dados.get("idMatricula"), "ID de matrícula inválido.");

        if (!matriculaDAO.cancelar(id)) {
            responderTexto(requisicao, 404, "Matrícula não encontrada.");
            return;
        }

        redirecionar(requisicao, "/pages/matriculas.html");
    }
}
