package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.ProfessorDAO;
import br.uepa.foxfit.model.Professor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CadastroProfessorHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"POST".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(
                    requisicao, 405, "Método não permitido."
            );
            return;
        }

        try {
            String corpo = new String(
                    requisicao.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            Map<String, String> dados = new HashMap<>();

            for (String parte : corpo.split("&")) {
                String[] campo = parte.split("=", 2);

                String chave = URLDecoder.decode(
                        campo[0], StandardCharsets.UTF_8
                );

                String valor = URLDecoder.decode(
                        campo.length > 1 ? campo[1] : "",
                        StandardCharsets.UTF_8
                );

                dados.put(chave, valor.trim());
            }

            Professor professor = new Professor(
                    obrigatorio(dados, "nome"),
                    obrigatorio(dados, "cpf"),
                    obrigatorio(dados, "cref"),
                    obrigatorio(dados, "especialidade"),
                    obrigatorio(dados, "turno"),
                    obrigatorio(dados, "telefone")
            );

            validar(professor);

            ProfessorDAO professorDAO = new ProfessorDAO();
            professorDAO.cadastrar(professor);

            requisicao.getResponseHeaders().set(
                    "Location", "/pages/professores.html"
            );

            requisicao.sendResponseHeaders(303, -1);
            requisicao.close();

        } catch (IllegalArgumentException erro) {

            WebUtils.responderTexto(
                    requisicao, 400,
                    "Dados inválidos. Verifique os campos do formulário."
            );

        } catch (SQLException erro) {

            if ("23505".equals(erro.getSQLState())) {
                WebUtils.responderTexto(
                        requisicao, 409,
                        "Já existe um professor com este CPF ou CREF."
                );
            } else {
                erro.printStackTrace();

                WebUtils.responderTexto(
                        requisicao, 500,
                        "Erro interno ao cadastrar professor."
                );
            }
        }
    }

    private static String obrigatorio(
            Map<String, String> dados, String nome) {

        String valor = dados.get(nome);

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Campo obrigatório: " + nome
            );
        }

        return valor;
    }

    private static void validar(Professor professor) {

        if (professor.getNomeCompleto().length() > 150
                || professor.getCpf().length() > 14
                || professor.getCref().length() > 30
                || professor.getEspecialidade().length() > 100
                || professor.getTelefone().length() > 20) {

            throw new IllegalArgumentException(
                    "Um ou mais campos excedem o tamanho permitido."
            );
        }

        String turno = professor.getTurno();

        if (!turno.equals("Manhã")
                && !turno.equals("Tarde")
                && !turno.equals("Noite")) {

            throw new IllegalArgumentException(
                    "Turno inválido."
            );
        }
    }
}
