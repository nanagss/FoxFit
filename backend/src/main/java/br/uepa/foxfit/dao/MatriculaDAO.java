package br.uepa.foxfit.dao;

import br.uepa.foxfit.model.Matricula;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pelo ciclo de vida das Matrículas (RF05):
 * realização de contrato (com geração automática de mensalidade), listagem, renovação e cancelamento.
 */
public class MatriculaDAO extends BaseDAO<Matricula> implements RepositorioCRUD<Matricula> {

    @Override
    public int cadastrar(Matricula matricula) throws SQLException {
        String sqlPlano = "SELECT duracao_meses, valor_mensal FROM planos WHERE id = ?";

        String sqlInsertMatricula = """
                INSERT INTO matriculas
                    (id, codigo, aluno_id, plano_id, data_inicio, data_vencimento, forma_pagamento, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String sqlInsertMensalidade = """
                INSERT INTO mensalidades
                    (matricula_id, aluno_id, valor, data_vencimento, data_pagamento, forma_pagamento, status)
                VALUES (?, ?, ?, ?, NULL, ?, 'PENDENTE')
                """;

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);

            try {
                int duracaoMeses;
                BigDecimal valorMensal;

                try (PreparedStatement cmdPlano = conexao.prepareStatement(sqlPlano)) {
                    cmdPlano.setInt(1, matricula.getPlanoId());
                    try (ResultSet rsPlano = cmdPlano.executeQuery()) {
                        if (!rsPlano.next()) {
                            throw new IllegalArgumentException("Plano selecionado não encontrado.");
                        }
                        duracaoMeses = rsPlano.getInt("duracao_meses");
                        valorMensal = rsPlano.getBigDecimal("valor_mensal");
                    }
                }

                LocalDate dataInicio = matricula.getDataInicio() != null
                        ? matricula.getDataInicio()
                        : LocalDate.now();
                LocalDate dataVencimento = Matricula.calcularVencimento(dataInicio, duracaoMeses);

                int id = gerarProximoId(conexao, "matriculas");
                String codigo = String.format("#MAT-%d-%03d", dataInicio.getYear(), id);

                try (PreparedStatement cmdMat = conexao.prepareStatement(sqlInsertMatricula)) {
                    cmdMat.setInt(1, id);
                    cmdMat.setString(2, codigo);
                    cmdMat.setInt(3, matricula.getAlunoId());
                    cmdMat.setInt(4, matricula.getPlanoId());
                    cmdMat.setDate(5, Date.valueOf(dataInicio));
                    cmdMat.setDate(6, Date.valueOf(dataVencimento));
                    cmdMat.setString(7, matricula.getFormaPagamento());
                    cmdMat.setString(8, "ATIVA");
                    cmdMat.executeUpdate();
                }

                // Gera automaticamente a cobrança correspondente no Fluxo Financeiro
                try (PreparedStatement cmdMens = conexao.prepareStatement(sqlInsertMensalidade)) {
                    cmdMens.setInt(1, id);
                    cmdMens.setInt(2, matricula.getAlunoId());
                    cmdMens.setBigDecimal(3, valorMensal);
                    cmdMens.setDate(4, Date.valueOf(dataInicio));
                    cmdMens.setString(5, matricula.getFormaPagamento());
                    cmdMens.executeUpdate();
                }

                conexao.commit();

                matricula.setId(id);
                matricula.setCodigo(codigo);
                matricula.setDataInicio(dataInicio);
                matricula.setDataVencimento(dataVencimento);
                matricula.setStatus("ATIVA");

                return id;

            } catch (SQLException erro) {
                desfazerTransacao(conexao, erro);
                throw erro;
            }
        }
    }

    @Override
    public List<Matricula> listar() throws SQLException {
        List<Matricula> matriculas = new ArrayList<>();

        String sql = """
                SELECT m.id,
                       m.codigo,
                       m.aluno_id,
                       m.plano_id,
                       a.nome AS nome_aluno,
                       p.nome AS nome_plano,
                       p.valor_mensal,
                       p.duracao_meses,
                       m.data_inicio,
                       m.data_vencimento,
                       m.forma_pagamento,
                       m.status
                FROM matriculas m
                JOIN alunos a ON m.aluno_id = a.id
                JOIN planos p ON m.plano_id = p.id
                ORDER BY m.data_inicio DESC, m.id DESC
                """;

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                matriculas.add(mapear(resultado));
            }
        }

        return matriculas;
    }

    @Override
    public Matricula buscarPorId(int id) throws SQLException {
        String sql = """
                SELECT m.id,
                       m.codigo,
                       m.aluno_id,
                       m.plano_id,
                       a.nome AS nome_aluno,
                       p.nome AS nome_plano,
                       p.valor_mensal,
                       p.duracao_meses,
                       m.data_inicio,
                       m.data_vencimento,
                       m.forma_pagamento,
                       m.status
                FROM matriculas m
                JOIN alunos a ON m.aluno_id = a.id
                JOIN planos p ON m.plano_id = p.id
                WHERE m.id = ?
                """;

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, id);

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return mapear(resultado);
                }
            }
        }

        return null;
    }

    /**
     * Renova o contrato de matrícula adicionando a duração do plano contratado,
     * reativando o status para ATIVA e gerando a nova cobrança em mensalidades.
     */
    public boolean renovar(int idMatricula) throws SQLException {
        String sqlUpdate = """
                UPDATE matriculas
                SET data_vencimento = ?,
                    status = 'ATIVA'
                WHERE id = ?
                """;

        String sqlInsertMensalidade = """
                INSERT INTO mensalidades
                    (matricula_id, aluno_id, valor, data_vencimento, data_pagamento, forma_pagamento, status)
                VALUES (?, ?, ?, ?, NULL, ?, 'PENDENTE')
                """;

        Matricula atual = buscarPorId(idMatricula);
        if (atual == null) {
            return false;
        }

        LocalDate hoje = LocalDate.now();
        LocalDate base = (atual.getDataVencimento() != null && atual.getDataVencimento().isAfter(hoje))
                ? atual.getDataVencimento()
                : hoje;

        int duracao = Math.max(1, atual.getDuracaoMesesPlano());
        LocalDate novoVencimento = Matricula.calcularVencimento(base, duracao);

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);

            try {
                boolean atualizado;
                try (PreparedStatement cmdUpd = conexao.prepareStatement(sqlUpdate)) {
                    cmdUpd.setDate(1, Date.valueOf(novoVencimento));
                    cmdUpd.setInt(2, idMatricula);
                    atualizado = cmdUpd.executeUpdate() == 1;
                }

                if (atualizado) {
                    try (PreparedStatement cmdMens = conexao.prepareStatement(sqlInsertMensalidade)) {
                        cmdMens.setInt(1, idMatricula);
                        cmdMens.setInt(2, atual.getAlunoId());
                        cmdMens.setBigDecimal(3, atual.getValorMensalPlano());
                        cmdMens.setDate(4, Date.valueOf(hoje));
                        cmdMens.setString(5, atual.getFormaPagamento());
                        cmdMens.executeUpdate();
                    }
                }

                conexao.commit();
                return atualizado;

            } catch (SQLException erro) {
                desfazerTransacao(conexao, erro);
                throw erro;
            }
        }
    }

    /**
     * Cancela a matrícula alterando seu status para CANCELADA.
     */
    public boolean cancelar(int idMatricula) throws SQLException {
        String sql = "UPDATE matriculas SET status = 'CANCELADA' WHERE id = ?";

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, idMatricula);
            return comando.executeUpdate() == 1;
        }
    }

    @Override
    public boolean atualizar(Matricula matricula) throws SQLException {
        String sql = """
                UPDATE matriculas
                SET plano_id = ?,
                    data_inicio = ?,
                    data_vencimento = ?,
                    forma_pagamento = ?,
                    status = ?
                WHERE id = ?
                """;

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, matricula.getPlanoId());
            comando.setDate(2, Date.valueOf(matricula.getDataInicio()));
            comando.setDate(3, Date.valueOf(matricula.getDataVencimento()));
            comando.setString(4, matricula.getFormaPagamento());
            comando.setString(5, matricula.getStatus());
            comando.setInt(6, matricula.getId());

            return comando.executeUpdate() == 1;
        }
    }

    @Override
    public boolean excluir(int id) throws SQLException {
        return cancelar(id);
    }

    @Override
    protected Matricula mapear(ResultSet resultado) throws SQLException {
        Matricula matricula = new Matricula(
                resultado.getInt("aluno_id"),
                resultado.getInt("plano_id"),
                resultado.getDate("data_inicio").toLocalDate(),
                resultado.getDate("data_vencimento").toLocalDate(),
                resultado.getString("forma_pagamento")
        );

        matricula.setId(resultado.getInt("id"));
        matricula.setCodigo(resultado.getString("codigo"));
        matricula.setNomeAluno(resultado.getString("nome_aluno"));
        matricula.setNomePlano(resultado.getString("nome_plano"));
        matricula.setValorMensalPlano(resultado.getBigDecimal("valor_mensal"));
        matricula.setDuracaoMesesPlano(resultado.getInt("duracao_meses"));
        matricula.setStatus(resultado.getString("status"));

        return matricula;
    }
}
