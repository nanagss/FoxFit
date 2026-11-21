package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.Plano;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pelas operações CRUD da tabela "planos" no PostgreSQL.
 */
public class PlanoDAO {

    /**
     * Cadastra um novo plano de assinatura e gera automaticamente o código (#PLN-XX).
     */
    public int cadastrar(Plano plano) throws SQLException {

        String sqlProximoId = "SELECT COALESCE(MAX(id), 0) + 1 AS proximo FROM planos";

        String sqlInsert = """
                INSERT INTO planos
                (codigo, nome, duracao_meses, valor_mensal, valor_total, beneficios, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection conexao = Conexao.conectar()) {

            String codigo = plano.getCodigo();
            if (codigo == null || codigo.isBlank()) {
                try (PreparedStatement cmdSeq = conexao.prepareStatement(sqlProximoId);
                     ResultSet rsSeq = cmdSeq.executeQuery()) {
                    int numero = rsSeq.next() ? rsSeq.getInt("proximo") : 1;
                    codigo = String.format("#PLN-%02d", numero);
                }
            }

            try (PreparedStatement comando = conexao.prepareStatement(sqlInsert)) {
                comando.setString(1, codigo);
                comando.setString(2, plano.getNome());
                comando.setInt(3, plano.getDuracaoMeses());
                comando.setBigDecimal(4, plano.getValorMensal());
                comando.setBigDecimal(5, plano.getValorTotal());
                comando.setString(6, plano.getBeneficios());
                comando.setString(7, plano.getStatus());

                try (ResultSet resultado = comando.executeQuery()) {
                    if (resultado.next()) {
                        return resultado.getInt("id");
                    }
                    throw new SQLException("Plano não foi cadastrado.");
                }
            }
        }
    }

    /**
     * Lista todos os planos ordenados por duração e ID.
     */
    public List<Plano> listar() throws SQLException {

        List<Plano> planos = new ArrayList<>();

        String sql = "SELECT * FROM planos ORDER BY duracao_meses ASC, id ASC";

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                planos.add(mapear(resultado));
            }
        }

        return planos;
    }

    /**
     * Busca um plano específico pelo seu ID.
     */
    public Plano buscarPorId(int id) throws SQLException {

        String sql = "SELECT * FROM planos WHERE id = ?";

        try (Connection conexao = Conexao.conectar();
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
     * Atualiza os dados comerciais, benefícios e status de um plano existente.
     */
    public boolean atualizar(Plano plano) throws SQLException {

        String sql = """
                UPDATE planos
                SET nome = ?,
                    duracao_meses = ?,
                    valor_mensal = ?,
                    valor_total = ?,
                    beneficios = ?,
                    status = ?
                WHERE id = ?
                """;

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setString(1, plano.getNome());
            comando.setInt(2, plano.getDuracaoMeses());
            comando.setBigDecimal(3, plano.getValorMensal());
            comando.setBigDecimal(4, plano.getValorTotal());
            comando.setString(5, plano.getBeneficios());
            comando.setString(6, plano.getStatus());
            comando.setInt(7, plano.getId());

            return comando.executeUpdate() > 0;
        }
    }

    /**
     * Exclui um plano pelo ID (se houver matrículas vinculadas, o PostgreSQL retornará SQLState 23503).
     */
    public boolean excluir(int id) throws SQLException {

        String sql = "DELETE FROM planos WHERE id = ?";

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, id);

            return comando.executeUpdate() > 0;
        }
    }

    private Plano mapear(ResultSet resultado) throws SQLException {
        Plano plano = new Plano(
                resultado.getString("nome"),
                resultado.getInt("duracao_meses"),
                resultado.getBigDecimal("valor_mensal"),
                resultado.getString("beneficios")
        );

        plano.setId(resultado.getInt("id"));
        plano.setCodigo(resultado.getString("codigo"));
        plano.setValorTotal(resultado.getBigDecimal("valor_total"));
        plano.setStatus(resultado.getString("status"));

        return plano;
    }
}
