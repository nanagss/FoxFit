package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.Mensalidade;

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
 * DAO responsável pelas consultas e baixa de pagamentos do Fluxo Financeiro (tabela "mensalidades").
 */
public class FinanceiroDAO {

    /**
     * Calcula a soma de valores das mensalidades por status ("PAGO", "PENDENTE" ou "ATRASADO").
     * Se mesCompetencia (formato "YYYY-MM") for informado, filtra pelo mês de vencimento.
     */
    public BigDecimal somarPorStatus(String status, String mesCompetencia) throws SQLException {

        boolean filtrarMes = mesCompetencia != null && mesCompetencia.matches("\\d{4}-\\d{2}");

        String sql = filtrarMes
                ? "SELECT COALESCE(SUM(valor), 0) AS total FROM mensalidades WHERE UPPER(status) = ? AND TO_CHAR(data_vencimento, 'YYYY-MM') = ?"
                : "SELECT COALESCE(SUM(valor), 0) AS total FROM mensalidades WHERE UPPER(status) = ?";

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setString(1, status.toUpperCase());
            if (filtrarMes) {
                comando.setString(2, mesCompetencia);
            }

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return resultado.getBigDecimal("total");
                }
            }
        }

        return BigDecimal.ZERO;
    }

    /**
     * Lista mensalidades combinadas com nome do aluno, nome do plano e código da matrícula,
     * suportando filtros por status e mês de competência (YYYY-MM).
     */
    public List<Mensalidade> listar(String statusFiltro, String mesCompetencia) throws SQLException {

        List<Mensalidade> mensalidades = new ArrayList<>();

        boolean filtrarStatus = statusFiltro != null
                && !statusFiltro.isBlank()
                && !statusFiltro.equalsIgnoreCase("TODOS");

        boolean filtrarMes = mesCompetencia != null
                && mesCompetencia.matches("\\d{4}-\\d{2}");

        StringBuilder sql = new StringBuilder("""
                SELECT m.id,
                       m.matricula_id,
                       m.aluno_id,
                       a.nome AS nome_aluno,
                       p.nome AS nome_plano,
                       mat.codigo AS codigo_matricula,
                       m.valor,
                       m.data_vencimento,
                       m.data_pagamento,
                       m.forma_pagamento,
                       m.status
                FROM mensalidades m
                JOIN alunos a ON m.aluno_id = a.id
                JOIN matriculas mat ON m.matricula_id = mat.id
                JOIN planos p ON mat.plano_id = p.id
                WHERE 1 = 1
                """);

        if (filtrarStatus) {
            sql.append(" AND UPPER(m.status) = ? ");
        }

        if (filtrarMes) {
            sql.append(" AND TO_CHAR(m.data_vencimento, 'YYYY-MM') = ? ");
        }

        sql.append(" ORDER BY m.data_vencimento ASC, m.id ASC");

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql.toString())) {

            int indice = 1;
            if (filtrarStatus) {
                comando.setString(indice++, statusFiltro.toUpperCase());
            }
            if (filtrarMes) {
                comando.setString(indice, mesCompetencia);
            }

            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    Date dtVenc = resultado.getDate("data_vencimento");
                    Date dtPag = resultado.getDate("data_pagamento");

                    LocalDate vencimento = dtVenc != null ? dtVenc.toLocalDate() : null;
                    LocalDate pagamento = dtPag != null ? dtPag.toLocalDate() : null;

                    mensalidades.add(new Mensalidade(
                            resultado.getInt("id"),
                            resultado.getInt("matricula_id"),
                            resultado.getInt("aluno_id"),
                            resultado.getString("nome_aluno"),
                            resultado.getString("nome_plano"),
                            resultado.getString("codigo_matricula"),
                            resultado.getBigDecimal("valor"),
                            vencimento,
                            pagamento,
                            resultado.getString("forma_pagamento"),
                            resultado.getString("status")
                    ));
                }
            }
        }

        return mensalidades;
    }

    /**
     * Registra a baixa de pagamento de uma mensalidade, alterando o status para "PAGO"
     * e definindo data_pagamento como a data atual do banco (CURRENT_DATE).
     */
    public boolean confirmarPagamento(int idMensalidade) throws SQLException {

        String sql = """
                UPDATE mensalidades
                SET status = 'PAGO',
                    data_pagamento = CURRENT_DATE
                WHERE id = ?
                """;

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, idMensalidade);

            return comando.executeUpdate() > 0;
        }
    }
}
