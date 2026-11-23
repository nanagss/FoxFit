package br.uepa.foxfit.dao;

import br.uepa.foxfit.model.Plano;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pelas operações CRUD da tabela "planos" no PostgreSQL.
 * Estende BaseDAO e implementa RepositorioCRUD.
 */
public class PlanoDAO extends BaseDAO<Plano> implements RepositorioCRUD<Plano> {

    @Override
    public int cadastrar(Plano plano) throws SQLException {
        String sqlInsert = """
                INSERT INTO planos
                (id, codigo, nome, duracao_meses, valor_mensal, valor_total, beneficios, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);

            try {
                int id = gerarProximoId(conexao, "planos");
                String codigo = (plano.getCodigo() == null || plano.getCodigo().isBlank())
                        ? String.format("#PLN-%02d", id)
                        : plano.getCodigo();

                try (PreparedStatement comando = conexao.prepareStatement(sqlInsert)) {
                    comando.setInt(1, id);
                    comando.setString(2, codigo);
                    comando.setString(3, plano.getNome());
                    comando.setInt(4, plano.getDuracaoMeses());
                    comando.setBigDecimal(5, plano.getValorMensal());
                    comando.setBigDecimal(6, plano.getValorTotal());
                    comando.setString(7, plano.getBeneficios());
                    comando.setString(8, plano.getStatus());

                    comando.executeUpdate();
                }

                conexao.commit();

                plano.setId(id);
                plano.setCodigo(codigo);

                return id;

            } catch (SQLException erro) {
                desfazerTransacao(conexao, erro);
                throw erro;
            }
        }
    }

    @Override
    public List<Plano> listar() throws SQLException {
        List<Plano> planos = new ArrayList<>();
        String sql = "SELECT * FROM planos ORDER BY duracao_meses ASC, id ASC";

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                planos.add(mapear(resultado));
            }
        }

        return planos;
    }

    @Override
    public Plano buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM planos WHERE id = ?";

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

    @Override
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

        try (Connection conexao = abrirConexao();
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

    @Override
    public boolean excluir(int id) throws SQLException {
        String sql = "DELETE FROM planos WHERE id = ?";

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, id);
            return comando.executeUpdate() > 0;
        }
    }

    @Override
    protected Plano mapear(ResultSet resultado) throws SQLException {
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
