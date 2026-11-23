package br.uepa.foxfit.dao;

import br.uepa.foxfit.model.Aluno;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pelo CRUD da tabela "alunos".
 * Estende BaseDAO e implementa RepositorioCRUD.
 */
public class AlunoDAO extends BaseDAO<Aluno> implements RepositorioCRUD<Aluno> {

    @Override
    public int cadastrar(Aluno aluno) throws SQLException {

        String sqlCadastrar = """
                INSERT INTO alunos
                    (id, codigo, nome, cpf, data_nascimento,
                     telefone, email, endereco, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);

            try {
                int id = gerarProximoId(conexao, "alunos");
                String codigo = String.format("#ALU-%03d", id);

                try (PreparedStatement comando = conexao.prepareStatement(sqlCadastrar)) {
                    comando.setInt(1, id);
                    comando.setString(2, codigo);
                    comando.setString(3, aluno.getNomeCompleto());
                    comando.setString(4, aluno.getCpf());
                    comando.setDate(5, Date.valueOf(aluno.getDataNascimento()));
                    comando.setString(6, aluno.getTelefone());
                    comando.setString(7, aluno.getEmail());
                    comando.setString(8, aluno.getEndereco());
                    comando.setString(9, aluno.getStatus());

                    comando.executeUpdate();
                }

                conexao.commit();

                aluno.setIdAluno(id);
                aluno.setCodigo(codigo);

                return id;

            } catch (SQLException erro) {
                desfazerTransacao(conexao, erro);
                throw erro;
            }
        }
    }

    @Override
    public List<Aluno> listar() throws SQLException {
        List<Aluno> alunos = new ArrayList<>();

        String sql = """
                SELECT id, codigo, nome, cpf,
                       data_nascimento, telefone,
                       email, endereco, status
                FROM alunos
                ORDER BY id
                """;

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                alunos.add(mapear(resultado));
            }
        }

        return alunos;
    }

    @Override
    public Aluno buscarPorId(int id) throws SQLException {
        String sql = """
                SELECT id, codigo, nome, cpf,
                       data_nascimento, telefone,
                       email, endereco, status
                FROM alunos
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
    public boolean atualizar(Aluno aluno) throws SQLException {
        String sql = """
                UPDATE alunos
                SET nome = ?,
                    cpf = ?,
                    data_nascimento = ?,
                    telefone = ?,
                    email = ?,
                    endereco = ?,
                    status = ?
                WHERE id = ?
                """;

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setString(1, aluno.getNomeCompleto());
            comando.setString(2, aluno.getCpf());
            comando.setDate(3, Date.valueOf(aluno.getDataNascimento()));
            comando.setString(4, aluno.getTelefone());
            comando.setString(5, aluno.getEmail());
            comando.setString(6, aluno.getEndereco());
            comando.setString(7, aluno.getStatus());
            comando.setInt(8, aluno.getIdAluno());

            return comando.executeUpdate() == 1;
        }
    }

    @Override
    public boolean excluir(int id) throws SQLException {
        String sql = "DELETE FROM alunos WHERE id = ?";

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, id);
            return comando.executeUpdate() == 1;
        }
    }

    @Override
    protected Aluno mapear(ResultSet resultado) throws SQLException {
        Aluno aluno = new Aluno(
                resultado.getString("nome"),
                resultado.getString("cpf"),
                resultado.getDate("data_nascimento").toLocalDate(),
                resultado.getString("telefone"),
                resultado.getString("email"),
                resultado.getString("endereco")
        );

        aluno.setIdAluno(resultado.getInt("id"));
        aluno.setCodigo(resultado.getString("codigo"));
        aluno.setStatus(resultado.getString("status"));

        return aluno;
    }
}
