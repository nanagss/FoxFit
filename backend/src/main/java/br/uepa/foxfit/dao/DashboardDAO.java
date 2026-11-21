package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.MatriculaRecente;
import br.uepa.foxfit.model.ResumoDashboard;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pelas consultas consolidadas exibidas na tela de Dashboard.
 */
public class DashboardDAO {

    /**
     * Carrega em uma única chamada todos os 4 indicadores e as 5 matrículas mais recentes.
     */
    public ResumoDashboard carregarResumo() throws SQLException {

        try (Connection conexao = Conexao.conectar()) {
            int alunosAtivos = contarAlunosAtivos(conexao);
            int matriculasNoMes = contarMatriculasNoMes(conexao);
            BigDecimal receitaMensal = calcularReceitaMensal(conexao);
            double taxaAdimplencia = calcularTaxaAdimplencia(conexao);
            List<MatriculaRecente> recentes = listarMatriculasRecentes(conexao);

            return new ResumoDashboard(
                    alunosAtivos,
                    matriculasNoMes,
                    receitaMensal,
                    taxaAdimplencia,
                    recentes
            );
        }
    }

    private int contarAlunosAtivos(Connection conexao) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM alunos WHERE UPPER(status) = 'ATIVO'";

        try (PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            return resultado.next() ? resultado.getInt("total") : 0;
        }
    }

    private int contarMatriculasNoMes(Connection conexao) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total
                FROM matriculas
                WHERE EXTRACT(MONTH FROM data_inicio) = EXTRACT(MONTH FROM CURRENT_DATE)
                  AND EXTRACT(YEAR FROM data_inicio) = EXTRACT(YEAR FROM CURRENT_DATE)
                """;

        try (PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            return resultado.next() ? resultado.getInt("total") : 0;
        }
    }

    private BigDecimal calcularReceitaMensal(Connection conexao) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(valor), 0) AS receita
                FROM mensalidades
                WHERE UPPER(status) = 'PAGO'
                  AND EXTRACT(MONTH FROM data_pagamento) = EXTRACT(MONTH FROM CURRENT_DATE)
                  AND EXTRACT(YEAR FROM data_pagamento) = EXTRACT(YEAR FROM CURRENT_DATE)
                """;

        try (PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            return resultado.next() ? resultado.getBigDecimal("receita") : BigDecimal.ZERO;
        }
    }

    private double calcularTaxaAdimplencia(Connection conexao) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total_geral,
                       COUNT(*) FILTER (WHERE UPPER(status) <> 'ATRASADO') AS total_em_dia
                FROM mensalidades
                """;

        try (PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            if (resultado.next()) {
                int totalGeral = resultado.getInt("total_geral");
                int totalEmDia = resultado.getInt("total_em_dia");

                if (totalGeral == 0) {
                    return 100.0;
                }
                return (totalEmDia * 100.0) / totalGeral;
            }
        }

        return 100.0;
    }

    private List<MatriculaRecente> listarMatriculasRecentes(Connection conexao) throws SQLException {
        List<MatriculaRecente> lista = new ArrayList<>();

        String sql = """
                SELECT mat.codigo,
                       a.nome AS nome_aluno,
                       p.nome AS nome_plano,
                       mat.data_inicio,
                       mat.data_vencimento,
                       mat.forma_pagamento,
                       p.valor_total,
                       mat.status
                FROM matriculas mat
                JOIN alunos a ON mat.aluno_id = a.id
                JOIN planos p ON mat.plano_id = p.id
                ORDER BY mat.data_inicio DESC, mat.id DESC
                LIMIT 5
                """;

        try (PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                lista.add(new MatriculaRecente(
                        resultado.getString("codigo"),
                        resultado.getString("nome_aluno"),
                        resultado.getString("nome_plano"),
                        resultado.getDate("data_inicio").toLocalDate(),
                        resultado.getDate("data_vencimento").toLocalDate(),
                        resultado.getString("forma_pagamento"),
                        resultado.getBigDecimal("valor_total"),
                        resultado.getString("status")
                ));
            }
        }

        return lista;
    }
}
