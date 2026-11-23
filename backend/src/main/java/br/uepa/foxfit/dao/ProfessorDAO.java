package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.Professor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfessorDAO {

    // CADASTRAR PROFESSOR
    public int cadastrar(Professor professor) throws SQLException {

        String sqlProximoId = """
            SELECT nextval(
                pg_get_serial_sequence('public.professores', 'id')
            )
            """;

        String sqlCadastrar = """
            INSERT INTO professores
                (id, codigo, nome, cpf, cref,
                 especialidade, turno, telefone, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conexao = Conexao.conectar()) {

            conexao.setAutoCommit(false);

            try {
                int id;

                try (PreparedStatement comandoId =
                             conexao.prepareStatement(sqlProximoId);
                     ResultSet resultado = comandoId.executeQuery()) {

                    if (!resultado.next()) {
                        throw new SQLException(
                                "Não foi possível gerar o ID do professor."
                        );
                    }

                    id = resultado.getInt(1);
                }

                String codigo = String.format("#PRF-%03d", id);

                try (PreparedStatement comando =
                             conexao.prepareStatement(sqlCadastrar)) {

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
                try {
                    conexao.rollback();
                } catch (SQLException erroRollback) {
                    erro.addSuppressed(erroRollback);
                }
                throw erro;
            }
        }
    }

    // LISTAR PROFESSORES
    public List<Professor> listar() throws SQLException {

        List<Professor> professores = new ArrayList<>();

        String sql = """
            SELECT id, codigo, nome, cpf, cref,
                   especialidade, turno, telefone, status
            FROM professores
            ORDER BY id
            """;

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                professores.add(mapearProfessor(resultado));
            }
        }

        return professores;
    }

    // BUSCAR PROFESSOR PELO ID
    public Professor buscarPorId(int id) throws SQLException {

        String sql = """
            SELECT id, codigo, nome, cpf, cref,
                   especialidade, turno, telefone, status
            FROM professores
            WHERE id = ?
            """;

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, id);

            try (ResultSet resultado = comando.executeQuery()) {

                if (resultado.next()) {
                    return mapearProfessor(resultado);
                }
            }
        }

        return null;
    }

    // ATUALIZAR PROFESSOR
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

        try (Connection conexao = Conexao.conectar();
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

    // DESATIVAR PROFESSOR
    public boolean desativar(int id) throws SQLException {

        String sql = """
            UPDATE professores
            SET status = 'INATIVO'
            WHERE id = ? AND status = 'ATIVO'
            """;

        try (Connection conexao = Conexao.conectar();
             PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setInt(1, id);

            return comando.executeUpdate() == 1;
        }
    }

    // CONVERTER RESULTADO SQL EM OBJETO JAVA
    private Professor mapearProfessor(ResultSet resultado)
            throws SQLException {

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
