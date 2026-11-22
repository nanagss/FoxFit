package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.AlunoDAO;
import br.uepa.foxfit.model.Aluno;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.net.URLDecoder;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaginaAlunosHandler implements HttpHandler {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"GET".equals(requisicao.getRequestMethod())) {
            responder(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            List<Aluno> alunos = new AlunoDAO().listar();

            String parametrosFiltro =
                    requisicao.getRequestURI().getRawQuery();

            Map<String, String> filtros = new HashMap<>();

            if (parametrosFiltro != null) {
                for (String parte : parametrosFiltro.split("&")) {

                    String[] campo = parte.split("=", 2);

                    String nome = URLDecoder.decode(
                            campo[0], StandardCharsets.UTF_8
                    );

                    String valor = URLDecoder.decode(
                            campo.length > 1 ? campo[1] : "",
                            StandardCharsets.UTF_8
                    );

                    filtros.put(nome, valor);
                }
            }

            String termo = filtros.getOrDefault(
                    "termoBusca", ""
            ).trim();

            String statusSolicitado = filtros.getOrDefault(
                    "statusFiltro", "TODOS"
            ).toUpperCase(Locale.ROOT);

            String filtroStatus =
                    statusSolicitado.equals("ATIVO")
                            || statusSolicitado.equals("INATIVO")
                            ? statusSolicitado : "TODOS";

            String termoNormalizado = normalizar(termo);
            String cpfPesquisado = termo.replaceAll("\\D", "");

            alunos = alunos.stream().filter(aluno -> {

                boolean nomeEncontrado = normalizar(
                        aluno.getNomeCompleto()
                ).contains(termoNormalizado);

                boolean cpfEncontrado = !cpfPesquisado.isEmpty()
                        && aluno.getCpf().replaceAll("\\D", "")
                        .contains(cpfPesquisado);

                boolean correspondeTermo = termo.isEmpty()
                        || nomeEncontrado || cpfEncontrado;

                boolean correspondeStatus =
                        filtroStatus.equals("TODOS")
                                || aluno.getStatus().equalsIgnoreCase(filtroStatus);

                return correspondeTermo && correspondeStatus;

            }).toList();


            String html = Files.readString(
                    Path.of("frontend", "pages", "alunos.html"),
                    StandardCharsets.UTF_8
            );


            html = html.replace(
                    "<!-- TERMO_BUSCA -->",
                    escapar(termo)
            );

            html = html.replace(
                    "<!-- SEL_TODOS -->",
                    filtroStatus.equals("TODOS") ? "selected" : ""
            );

            html = html.replace(
                    "<!-- SEL_ATIVO -->",
                    filtroStatus.equals("ATIVO") ? "selected" : ""
            );

            html = html.replace(
                    "<!-- SEL_INATIVO -->",
                    filtroStatus.equals("INATIVO") ? "selected" : ""
            );

            StringBuilder linhas = new StringBuilder();
            int ativos = 0;
            int inativos = 0;

            for (Aluno aluno : alunos) {

                String status = aluno.getStatus();

                if ("ATIVO".equalsIgnoreCase(status)) {
                    ativos++;
                } else if ("INATIVO".equalsIgnoreCase(status)) {
                    inativos++;
                }

                String classeStatus = "ATIVO".equalsIgnoreCase(status)
                        ? "status-ativo"
                        : "status-inativo";

                String nascimento =
                        aluno.getDataNascimento().toString();

                linhas.append("<tr>")
                        .append("<th scope=\"row\">")
                        .append(escapar(aluno.getCodigo()))
                        .append("</th>")
                        .append("<td>")
                        .append(escapar(aluno.getNomeCompleto()))
                        .append("</td>")
                        .append("<td>")
                        .append(escapar(aluno.getCpf()))
                        .append("</td>")
                        .append("<td><time datetime=\"")
                        .append(nascimento)
                        .append("\">")
                        .append(aluno.getDataNascimento().format(FORMATO))
                        .append("</time></td>")
                        .append("<td>")
                        .append(escapar(aluno.getTelefone()))
                        .append("</td>")
                        .append("<td>")
                        .append(escapar(aluno.getEmail()))
                        .append("</td>")
                        .append("<td>")
                        .append(escapar(aluno.getEndereco()))
                        .append("</td>")
                        .append("<td><span class=\"badge-status ")
                        .append(classeStatus)
                        .append("\">")
                        .append(escapar(status))
                        .append("</span></td>")
                        .append("<td class=\"celula-acoes\">")
                        .append("<a class=\"btn-editar\" href=\"/pages/alunos.html?editar=")
                        .append(aluno.getIdAluno())
                        .append("#form-editar-dinamico\">Editar</a> ")
                        .append("<a class=\"btn-excluir\" href=\"/pages/alunos.html?excluir=")
                        .append(aluno.getIdAluno())
                        .append("#confirmar-exclusao\">Excluir</a>")
                        .append("</td>")
                        .append("</tr>");
            }

            if (!html.contains("<!-- LINHAS_ALUNOS -->")
                    || !html.contains("<!-- TOTAL_ALUNOS -->")) {
                throw new IOException(
                        "Marcadores não encontrados em alunos.html"
                );
            }

            String total = alunos.size() + " alunos ("
                    + ativos + " ativos, "
                    + inativos + " inativos)";


            String formularioEdicao = "";

            String consulta = requisicao.getRequestURI().getRawQuery();

            if (consulta != null &&
                    consulta.matches("editar=[1-9][0-9]{0,8}")) {

                int idEdicao = Integer.parseInt(consulta.substring(7));

                Aluno selecionado = new AlunoDAO().buscarPorId(idEdicao);

                if (selecionado != null) {

                    String ativo = "ATIVO".equals(selecionado.getStatus())
                            ? "selected" : "";

                    String inativo = "INATIVO".equals(selecionado.getStatus())
                            ? "selected" : "";

                    formularioEdicao = """
                <section class="modulo-sistema">
                    <header class="cabecalho-modulo">
                        <h2>Editar Cadastro do Aluno</h2>
                    </header>

                    <form id="form-editar-dinamico"
                          class="formulario-padrao"
                          action="/alunos/atualizar"
                          method="post">

                        <input type="hidden"
                               name="idAluno"
                               value="%d">

                        <fieldset>
                            <legend>Dados do Aluno Selecionado</legend>

                            <label>Nome Completo
                                <input type="text" name="nome"
                                       value="%s" required>
                            </label>

                            <label>CPF
                                <input type="text" name="cpf"
                                       value="%s" required>
                            </label>

                            <label>Data de Nascimento
                                <input type="date" name="dataNascimento"
                                       value="%s" required>
                            </label>

                            <label>Telefone
                                <input type="tel" name="telefone"
                                       value="%s" required>
                            </label>

                            <label>E-mail
                                <input type="email" name="email"
                                       value="%s" required>
                            </label>

                            <label>Endereço
                                <input type="text" name="endereco"
                                       value="%s" required>
                            </label>

                            <label>Status
                                <select name="status" required>
                                      <option value="ATIVO" %s>Ativo</option>
                                      <option value="INATIVO" %s>Inativo</option>
                                </select>
                            </label>

                            <footer class="acoes-formulario">
                                <a href="/pages/alunos.html"
                                   class="btn btn-secundario">
                                    Cancelar
                                </a>

                                <button type="submit"
                                        class="btn btn-primario">
                                    Salvar Alterações
                                </button>
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

            if (consulta != null &&
                    consulta.matches("excluir=[1-9][0-9]{0,8}")) {

                int idExclusao = Integer.parseInt(
                        consulta.substring("excluir=".length())
                );

                Aluno selecionado =
                        new AlunoDAO().buscarPorId(idExclusao);

                if (selecionado != null) {

                    formularioEdicao = """
                <section class="modulo-sistema"
                         id="confirmar-exclusao">

                    <h2>Confirmar Exclusão de Aluno</h2>

                    <p>
                        Deseja realmente excluir o aluno
                        <strong>%s</strong> (ID %d)?
                    </p>

                    <p>
                        Esta ação é permanente. Alunos com
                        matrículas vinculadas não poderão
                        ser excluídos.
                    </p>

                    <form action="/alunos/excluir"
                          method="post">

                        <input type="hidden"
                               name="idAluno"
                               value="%d">

                        <a href="/pages/alunos.html">
                            Cancelar
                        </a>

                        <button type="submit"
                                class="btn-excluir">
                            Confirmar Exclusão
                        </button>
                    </form>
                </section>
                """.formatted(
                            escapar(selecionado.getNomeCompleto()),
                            selecionado.getIdAluno(),
                            selecionado.getIdAluno()
                    );
                }
            }


            html = html.replace(
                    "<!-- FORMULARIO_EDICAO_ALUNO -->",
                    formularioEdicao
            );

            html = html.replace(
                    "<!-- LINHAS_ALUNOS -->",
                    linhas.toString()
            );

            html = html.replace(
                    "<!-- TOTAL_ALUNOS -->",
                    total
            );

            responder(requisicao, 200, html);

        } catch (SQLException | IOException erro) {
            erro.printStackTrace();
            responder(requisicao, 500,
                    "Erro ao carregar os alunos.");
        }
    }

    private static String escapar(String valor) {

        if (valor == null) {
            return "-";
        }

        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void responder(
            HttpExchange requisicao,
            int codigo,
            String mensagem) throws IOException {

        byte[] conteudo = mensagem.getBytes(StandardCharsets.UTF_8);

        requisicao.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8"
        );

        requisicao.getResponseHeaders().set(
                "Cache-Control", "no-store"
        );

        requisicao.sendResponseHeaders(codigo, conteudo.length);

        try (var resposta = requisicao.getResponseBody()) {
            resposta.write(conteudo);
        }
    }

    private static String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(
                        texto, Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}