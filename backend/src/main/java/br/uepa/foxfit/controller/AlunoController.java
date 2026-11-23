package br.uepa.foxfit.controller;

import br.uepa.foxfit.dao.AlunoDAO;
import br.uepa.foxfit.dao.RepositorioCRUD;
import br.uepa.foxfit.model.Aluno;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller responsável pelo módulo de Alunos (RF01 / RF02):
 * /pages/alunos.html, /alunos/cadastrar, /alunos/atualizar e /alunos/excluir.
 */
public class AlunoController extends BaseHandler {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RepositorioCRUD<Aluno> alunoDAO = new AlunoDAO();

    @Override
    protected void processarGet(HttpExchange requisicao) throws IOException, SQLException {
        exibirPagina(requisicao);
    }

    @Override
    protected void processarPost(HttpExchange requisicao) throws IOException, SQLException {
        switch (obterRota(requisicao)) {
            case "/alunos/cadastrar" -> cadastrar(requisicao);
            case "/alunos/atualizar" -> atualizar(requisicao);
            case "/alunos/excluir" -> excluir(requisicao);
            default -> responderTexto(requisicao, 404, "Ação de alunos não encontrada.");
        }
    }

    private void exibirPagina(HttpExchange requisicao) throws IOException, SQLException {
        List<Aluno> alunos = alunoDAO.listar();
        Map<String, String> filtros = lerQuery(requisicao);

        String termo = filtros.getOrDefault("termoBusca", "").trim();
        String statusSolicitado = filtros.getOrDefault("statusFiltro", "TODOS").toUpperCase(Locale.ROOT);
        String filtroStatus = statusSolicitado.equals("ATIVO") || statusSolicitado.equals("INATIVO")
                ? statusSolicitado
                : "TODOS";

        String termoNormalizado = normalizar(termo);
        String cpfPesquisado = termo.replaceAll("\\D", "");

        alunos = alunos.stream().filter(aluno -> {
            boolean nomeEncontrado = normalizar(aluno.getNomeCompleto()).contains(termoNormalizado);
            boolean cpfEncontrado = !cpfPesquisado.isEmpty()
                    && aluno.getCpf().replaceAll("\\D", "").contains(cpfPesquisado);
            boolean correspondeTermo = termo.isEmpty() || nomeEncontrado || cpfEncontrado;
            boolean correspondeStatus = filtroStatus.equals("TODOS")
                    || aluno.getStatus().equalsIgnoreCase(filtroStatus);
            return correspondeTermo && correspondeStatus;
        }).toList();

        String html = lerPaginaHtml("alunos.html");

        html = html.replace("<!-- TERMO_BUSCA -->", escapar(termo));
        html = html.replace("<!-- SEL_TODOS -->", filtroStatus.equals("TODOS") ? "selected" : "");
        html = html.replace("<!-- SEL_ATIVO -->", filtroStatus.equals("ATIVO") ? "selected" : "");
        html = html.replace("<!-- SEL_INATIVO -->", filtroStatus.equals("INATIVO") ? "selected" : "");

        StringBuilder linhas = new StringBuilder();
        int ativos = 0;
        int inativos = 0;

        for (Aluno aluno : alunos) {
            boolean ativo = aluno.isAtivo();
            if (ativo) {
                ativos++;
            } else {
                inativos++;
            }

            String classeStatus = ativo ? "status-ativo" : "status-inativo";
            String rotuloStatus = ativo ? "Ativo" : "Inativo";
            String nascimento = aluno.getDataNascimento().toString();

            linhas.append("<tr>")
                    .append("<th scope=\"row\">").append(escapar(aluno.getIdentificadorExibicao())).append("</th>")
                    .append("<td>").append(escapar(aluno.getNomeCompleto())).append("</td>")
                    .append("<td>").append(escapar(aluno.getCpf())).append("</td>")
                    .append("<td><time datetime=\"").append(nascimento).append("\">")
                    .append(aluno.getDataNascimento().format(FORMATO)).append("</time></td>")
                    .append("<td>").append(escapar(aluno.getTelefone())).append("</td>")
                    .append("<td>").append(escapar(aluno.getEmail())).append("</td>")
                    .append("<td>").append(escapar(aluno.getEndereco())).append("</td>")
                    .append("<td><span class=\"badge-status ").append(classeStatus).append("\">")
                    .append(rotuloStatus).append("</span></td>")
                    .append("<td class=\"celula-acoes\">")
                    .append("<a class=\"btn-editar\" href=\"/pages/alunos.html?editar=")
                    .append(aluno.getIdAluno()).append("#form-editar-dinamico\">Editar</a> ")
                    .append("<a class=\"btn-excluir\" href=\"/pages/alunos.html?excluir=")
                    .append(aluno.getIdAluno()).append("#confirmar-exclusao\">Excluir</a>")
                    .append("</td>")
                    .append("</tr>");
        }

        if (!html.contains("<!-- LINHAS_ALUNOS -->") || !html.contains("<!-- TOTAL_ALUNOS -->")) {
            throw new IOException("Marcadores não encontrados em alunos.html");
        }

        String total = alunos.size() + " alunos (" + ativos + " ativos, " + inativos + " inativos)";
        String formularioEdicao = construirFormularioDinamico(filtros);

        html = html.replace("<!-- FORMULARIO_EDICAO_ALUNO -->", formularioEdicao);
        html = html.replace("<!-- LINHAS_ALUNOS -->", linhas.toString());
        html = html.replace("<!-- TOTAL_ALUNOS -->", total);

        responderHtml(requisicao, 200, html);
    }

    private void cadastrar(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);

        Aluno aluno = new Aluno(
                obrigatorio(dados, "nome"),
                obrigatorio(dados, "cpf"),
                LocalDate.parse(obrigatorio(dados, "dataNascimento")),
                obrigatorio(dados, "telefone"),
                obrigatorio(dados, "email"),
                obrigatorio(dados, "endereco")
        );

        alunoDAO.cadastrar(aluno);

        redirecionar(requisicao, "/pages/alunos.html");
    }

    private void atualizar(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);
        int id = lerIdPositivo(dados.get("idAluno"), "ID de aluno inválido.");

        Aluno aluno = alunoDAO.buscarPorId(id);
        if (aluno == null) {
            responderTexto(requisicao, 404, "Aluno não encontrado.");
            return;
        }

        aluno.setNomeCompleto(obrigatorio(dados, "nome"));
        aluno.setCpf(obrigatorio(dados, "cpf"));
        aluno.setDataNascimento(LocalDate.parse(obrigatorio(dados, "dataNascimento")));
        aluno.setTelefone(obrigatorio(dados, "telefone"));
        aluno.setEmail(obrigatorio(dados, "email"));
        aluno.setEndereco(obrigatorio(dados, "endereco"));

        String status = obrigatorio(dados, "status");
        if (!"ATIVO".equals(status) && !"INATIVO".equals(status)) {
            throw new IllegalArgumentException("Status inválido.");
        }
        aluno.setStatus(status);


        if (!alunoDAO.atualizar(aluno)) {
            responderTexto(requisicao, 404, "Aluno não encontrado.");
            return;
        }

        redirecionar(requisicao, "/pages/alunos.html");
    }

    private void excluir(HttpExchange requisicao) throws IOException, SQLException {
        Map<String, String> dados = lerFormulario(requisicao);
        int id = lerIdPositivo(dados.get("idAluno"), "ID inválido.");

        if (!alunoDAO.excluir(id)) {
            responderTexto(requisicao, 404, "Aluno não encontrado.");
            return;
        }

        redirecionar(requisicao, "/pages/alunos.html");
    }

    private String construirFormularioDinamico(Map<String, String> query) throws SQLException {
        String editarParam = query.get("editar");
        if (editarParam != null && editarParam.matches("[1-9][0-9]{0,8}")) {
            Aluno selecionado = alunoDAO.buscarPorId(Integer.parseInt(editarParam));
            if (selecionado != null) {
                String ativo = selecionado.isAtivo() ? "selected" : "";
                String inativo = !selecionado.isAtivo() ? "selected" : "";

                return """
                        <section class="modulo-sistema">
                            <header class="cabecalho-modulo">
                                <h2>Editar Cadastro do Aluno</h2>
                            </header>
                            <form id="form-editar-dinamico" class="formulario-padrao" action="/alunos/atualizar" method="post">
                                <input type="hidden" name="idAluno" value="%d">
                                <fieldset>
                                    <legend>Dados do Aluno Selecionado</legend>
                                    <label>Nome Completo
                                        <input type="text" name="nome" value="%s" required>
                                    </label>
                                    <label>CPF
                                        <input type="text" name="cpf" value="%s" required>
                                    </label>
                                    <label>Data de Nascimento
                                        <input type="date" name="dataNascimento" value="%s" required>
                                    </label>
                                    <label>Telefone
                                        <input type="tel" name="telefone" value="%s" required>
                                    </label>
                                    <label>E-mail
                                        <input type="email" name="email" value="%s" required>
                                    </label>
                                    <label>Endereço
                                        <input type="text" name="endereco" value="%s" required>
                                    </label>
                                    <label>Status
                                        <select name="status" required>
                                            <option value="ATIVO" %s>Ativo</option>
                                            <option value="INATIVO" %s>Inativo</option>
                                        </select>
                                    </label>
                                    <footer class="acoes-formulario">
                                        <a href="/pages/alunos.html" class="btn btn-secundario">Cancelar</a>
                                        <button type="submit" class="btn btn-primario">Salvar Alterações</button>
                                    </footer>
                                </fieldset>
                            </form>
                        </section>
                        """.formatted(
                        selecionado.getIdAluno(),
                        escapar(selecionado.getNomeCompleto()),
                        escapar(selecionado.getCpf()),
                        selecionado.getDataNascimento(),
                        escapar(selecionado.getTelefone()),
                        escapar(selecionado.getEmail()),
                        escapar(selecionado.getEndereco()),
                        ativo,
                        inativo
                );
            }
        }

        String excluirParam = query.get("excluir");
        if (excluirParam != null && excluirParam.matches("[1-9][0-9]{0,8}")) {
            Aluno selecionado = alunoDAO.buscarPorId(Integer.parseInt(excluirParam));
            if (selecionado != null) {
                return """
                        <section class="modulo-sistema" id="confirmar-exclusao">
                            <h2>Confirmar Exclusão de Aluno</h2>
                            <p>Deseja realmente excluir o aluno <strong>%s</strong> (ID %d)?</p>
                            <p>Esta ação é permanente. Alunos com matrículas vinculadas não poderão ser excluídos.</p>
                            <form action="/alunos/excluir" method="post">
                                <input type="hidden" name="idAluno" value="%d">
                                <footer class="acoes-formulario">
                                    <a href="/pages/alunos.html" class="btn btn-secundario">Cancelar</a>
                                    <button type="submit" class="btn-excluir">Confirmar Exclusão</button>
                                </footer>
                            </form>
                        </section>
                        """.formatted(
                        escapar(selecionado.getNomeCompleto()),
                        selecionado.getIdAluno(),
                        selecionado.getIdAluno()
                );
            }
        }

        return "";
    }

    @Override
    protected String mensagemDuplicidade() {
        return "Já existe outro aluno cadastrado com este CPF, código ou e-mail.";
    }

    @Override
    protected String mensagemVinculoAtivo() {
        return "Não é possível excluir este aluno: ele possui registros vinculados. Considere alterar seu status para Inativo.";
    }
}
