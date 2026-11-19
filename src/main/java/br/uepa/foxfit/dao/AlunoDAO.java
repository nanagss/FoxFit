package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.Aluno;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlunoDAO {

    public int cadastrar(Aluno aluno) throws SQLException {

        String sql = """
                INSERT INTO alunos
                (nome_completo, cpf, data_nascimento,
                 telefone, email, endereco, objetivo,
                 observacoes_medicas, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id_aluno
                """;

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando =
                     conexao.prepareStatement(sql)) {

            comando.setString(1, aluno.getNomeCompleto());
            comando.setString(2, aluno.getCpf());
            comando.setDate(3, Date.valueOf(aluno.getDataNascimento()));
            comando.setString(4, aluno.getTelefone());
            comando.setString(5, aluno.getEmail());
            comando.setString(6, aluno.getEndereco());
            comando.setString(7, aluno.getObjetivo());
            comando.setString(8, aluno.getObservacoesMedicas());
            comando.setString(9, aluno.getStatus());

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return resultado.getInt("id_aluno");
                }

                throw new SQLException("Aluno não foi cadastrado.");
            }
        }
    }


    public List<Aluno> listar() throws SQLException {

        List<Aluno> alunos = new ArrayList<>();

        String sql = "SELECT * FROM alunos ORDER BY id_aluno";

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando =
                     conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {

                Aluno aluno = new Aluno(
                        resultado.getString("nome_completo"),
                        resultado.getString("cpf"),
                        resultado.getDate("data_nascimento").toLocalDate(),
                        resultado.getString("telefone"),
                        resultado.getString("email"),
                        resultado.getString("endereco")
                );

                aluno.setIdAluno(resultado.getInt("id_aluno"));
                aluno.setObjetivo(resultado.getString("objetivo"));
                aluno.setObservacoesMedicas(
                        resultado.getString("observacoes_medicas")
                );
                aluno.setStatus(resultado.getString("status"));

                alunos.add(aluno);
            }
        }

        return alunos;
    }

    public Aluno buscarPorId(int id) throws SQLException {

        String sql = "SELECT * FROM alunos WHERE id_aluno = ?";

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando =
                     conexao.prepareStatement(sql)) {

            comando.setInt(1, id);

            try (ResultSet resultado = comando.executeQuery()) {

                if (resultado.next()) {

                    Aluno aluno = new Aluno(
                            resultado.getString("nome_completo"),
                            resultado.getString("cpf"),
                            resultado.getDate("data_nascimento").toLocalDate(),
                            resultado.getString("telefone"),
                            resultado.getString("email"),
                            resultado.getString("endereco")
                    );

                    aluno.setIdAluno(resultado.getInt("id_aluno"));
                    aluno.setObjetivo(resultado.getString("objetivo"));
                    aluno.setObservacoesMedicas(
                            resultado.getString("observacoes_medicas")
                    );
                    aluno.setStatus(resultado.getString("status"));

                    return aluno;
                }
            }
        }

        return null;
    }


    public boolean atualizar(Aluno aluno) throws SQLException {

        String sql = """
            UPDATE alunos
            SET nome_completo = ?,
                cpf = ?,
                data_nascimento = ?,
                telefone = ?,
                email = ?,
                endereco = ?,
                objetivo = ?,
                observacoes_medicas = ?,
                status = ?
            WHERE id_aluno = ?
            """;

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando =
                     conexao.prepareStatement(sql)) {

            comando.setString(1, aluno.getNomeCompleto());
            comando.setString(2, aluno.getCpf());
            comando.setDate(3, Date.valueOf(aluno.getDataNascimento()));
            comando.setString(4, aluno.getTelefone());
            comando.setString(5, aluno.getEmail());
            comando.setString(6, aluno.getEndereco());
            comando.setString(7, aluno.getObjetivo());
            comando.setString(8, aluno.getObservacoesMedicas());
            comando.setString(9, aluno.getStatus());
            comando.setInt(10, aluno.getIdAluno());

            int linhasAfetadas = comando.executeUpdate();

            return linhasAfetadas > 0;
        }
    }


    public boolean excluir(int id) throws SQLException {

        String sql = "DELETE FROM alunos WHERE id_aluno = ?";

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando =
                     conexao.prepareStatement(sql)) {

            comando.setInt(1, id);

            int linhasAfetadas = comando.executeUpdate();

            return linhasAfetadas > 0;
        }
    }

}
