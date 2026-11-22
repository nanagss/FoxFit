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

        String sqlProximoId = """
            SELECT nextval(
                pg_get_serial_sequence('public.alunos', 'id')
            )
            """;

        String sqlCadastrar = """
            INSERT INTO alunos
                (id, codigo, nome, cpf, data_nascimento,
                 telefone, email, endereco, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conexao = Conexao.conectar()) {

            conexao.setAutoCommit(false);

            try {
                int id;

                // 1. Solicita um identificador ao PostgreSQL
                try (PreparedStatement comandoId =
                             conexao.prepareStatement(sqlProximoId);
                     ResultSet resultado = comandoId.executeQuery()) {

                    if (!resultado.next()) {
                        throw new SQLException(
                                "Não foi possível gerar o ID do aluno."
                        );
                    }

                    id = resultado.getInt(1);
                }

                // 2. Gera o código de identificação do aluno
                String codigo = String.format("#ALU-%03d", id);

                // 3. Salva o aluno no PostgreSQL
                try (PreparedStatement comando =
                             conexao.prepareStatement(sqlCadastrar)) {

                    comando.setInt(1, id);
                    comando.setString(2, codigo);
                    comando.setString(3, aluno.getNomeCompleto());
                    comando.setString(4, aluno.getCpf());
                    comando.setDate(5,
                            Date.valueOf(aluno.getDataNascimento()));
                    comando.setString(6, aluno.getTelefone());
                    comando.setString(7, aluno.getEmail());
                    comando.setString(8, aluno.getEndereco());
                    comando.setString(9, aluno.getStatus());

                    comando.executeUpdate();
                }

                // 4. Confirma o cadastro
                conexao.commit();

                aluno.setIdAluno(id);
                aluno.setCodigo(codigo);

                return id;

            } catch (SQLException erro) {
                try {
                    conexao.rollback();
                } catch (SQLException erroRollback) {
                    erro.addSuppressed(erroRollback);
                }

                throw erro;
            }
        }
    }





    public List<Aluno> listar() throws SQLException {

        List<Aluno> alunos = new ArrayList<>();

        String sql = """
            SELECT id, codigo, nome, cpf,
                   data_nascimento, telefone,
                   email, endereco, status
            FROM alunos
            ORDER BY id
            """;

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando =
                     conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                alunos.add(mapearAluno(resultado));
            }
        }

        return alunos;
    }



    public Aluno buscarPorId(int id) throws SQLException {

        String sql = """
            SELECT id, codigo, nome, cpf,
                   data_nascimento, telefone,
                   email, endereco, status
            FROM alunos
            WHERE id = ?
            """;

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, id);

            try (ResultSet resultado = comando.executeQuery()) {

                if (resultado.next()) {
                    return mapearAluno(resultado);
                }
            }
        }

        return null;
    }




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

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setString(1, aluno.getNomeCompleto());
            comando.setString(2, aluno.getCpf());
            comando.setDate(3, Date.valueOf(aluno.getDataNascimento()));
            comando.setString(4, aluno.getTelefone());
            comando.setString(5, aluno.getEmail());
            comando.setString(6, aluno.getEndereco());
            comando.setString(7, aluno.getStatus());
            comando.setInt(8, aluno.getIdAluno());

            int linhasAfetadas = comando.executeUpdate();

            return linhasAfetadas == 1;
        }
    }




    public boolean excluir(int id) throws SQLException {

        String sql = "DELETE FROM alunos WHERE id = ?";

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, id);

            int linhasAfetadas = comando.executeUpdate();

            return linhasAfetadas == 1;
        }
    }


    private Aluno mapearAluno(ResultSet resultado) throws SQLException {

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
