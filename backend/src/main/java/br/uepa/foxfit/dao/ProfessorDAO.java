package br.uepa.foxfit.dao;

import br.uepa.foxfit.model.Professor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pelas operações da tabela "professores".
 * Estende BaseDAO e implementa RepositorioCRUD.
 */
public class ProfessorDAO extends BaseDAO<Professor> implements RepositorioCRUD<Professor> {

    @Override
    public int cadastrar(Professor professor) throws SQLException {
        String sqlCadastrar = """
                INSERT INTO professores
                    (id, codigo, nome, cpf, cref,
                     especialidade, turno, telefone, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);

            try {
                int id = gerarProximoId(conexao, "professores");
                String codigo = String.format("#PRF-%03d", id);

                try (PreparedStatement comando = conexao.prepareStatement(sqlCadastrar)) {
                    comando.setInt(1, id);
                    comando.setString(2, codigo);
                    comando.setString(3, professor.getNomeCompleto());
                    comando.setString(4, professor.getCpf());
                    comando.setString(5, professor.getCref());
                    comando.setString(6, professor.getEspecialidade());
                    comando.setString(7, professor.getTurno());
                    comando.setString(8, professor.getTelefone());
                    comando.setString(9, "ATIVO");

                    comando.executeUpdate();
                }

                conexao.commit();

                professor.setIdProfessor(id);
                professor.setCodigo(codigo);
                professor.setStatus("ATIVO");

                return id;

            } catch (SQLException erro) {
                desfazerTransacao(conexao, erro);
                throw erro;
            }
        }
    }

    @Override
    public List<Professor> listar() throws SQLException {
        List<Professor> professores = new ArrayList<>();

        String sql = """
                SELECT id, codigo, nome, cpf, cref,
                       especialidade, turno, telefone, status
                FROM professores
                ORDER BY id
                """;

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                professores.add(mapear(resultado));
            }
        }

        return professores;
    }

    @Override
    public Professor buscarPorId(int id) throws SQLException {
        String sql = """
                SELECT id, codigo, nome, cpf, cref,
                       especialidade, turno, telefone, status
                FROM professores
                WHERE id = ?
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

    @Override
    public boolean atualizar(Professor professor) throws SQLException {
        String sql = """
                UPDATE professores
                SET nome = ?,
                    cpf = ?,
                    cref = ?,
                    especialidade = ?,
                    turno = ?,
                    telefone = ?,
                    status = ?
                WHERE id = ?
                """;

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setString(1, professor.getNomeCompleto());
            comando.setString(2, professor.getCpf());
            comando.setString(3, professor.getCref());
            comando.setString(4, professor.getEspecialidade());
            comando.setString(5, professor.getTurno());
            comando.setString(6, professor.getTelefone());
            comando.setString(7, professor.getStatus());
            comando.setInt(8, professor.getIdProfessor());

            return comando.executeUpdate() == 1;
        }
    }

    public boolean desativar(int id) throws SQLException {
        String sql = """
                UPDATE professores
                SET status = 'INATIVO'
                WHERE id = ? AND status = 'ATIVO'
                """;

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, id);
            return comando.executeUpdate() == 1;
        }
    }

    @Override
    public boolean excluir(int id) throws SQLException {
        return desativar(id);
    }

    @Override
    protected Professor mapear(ResultSet resultado) throws SQLException {
        Professor professor = new Professor(
                resultado.getString("nome"),
                resultado.getString("cpf"),
                resultado.getString("cref"),
                resultado.getString("especialidade"),
                resultado.getString("turno"),
                resultado.getString("telefone")
        );

        professor.setIdProfessor(resultado.getInt("id"));
        professor.setCodigo(resultado.getString("codigo"));
        professor.setStatus(resultado.getString("status"));

        return professor;
    }
}
