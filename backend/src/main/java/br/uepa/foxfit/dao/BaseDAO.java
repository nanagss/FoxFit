package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.EntidadeBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Superclasse abstrata para a camada de persistência (DAOs) do sistema FOX FIT.
 * Centraliza abertura de conexão, geração segura de sequências, rollback transacional
 * e define o método abstrato polimórfico de mapeamento ResultSet -> Entidade.
 *
 * @param <T> Tipo da entidade gerenciada pelo DAO.
 */
public abstract class BaseDAO<T extends EntidadeBase> {

    private static final Set<String> TABELAS_PERMITIDAS = Set.of(
            "usuarios", "alunos", "professores", "planos", "matriculas", "mensalidades"
    );

    protected Connection abrirConexao() throws SQLException {
        return Conexao.conectar();
    }

    protected int gerarProximoId(Connection conexao, String tabela) throws SQLException {
        if (!TABELAS_PERMITIDAS.contains(tabela)) {
            throw new IllegalArgumentException("Tabela não permitida para sequência: " + tabela);
        }

        String sql = "SELECT nextval(pg_get_serial_sequence('public." + tabela + "', 'id'))";

        try (PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            if (!resultado.next()) {
                throw new SQLException("Não foi possível gerar o próximo ID para " + tabela + ".");
            }
            return resultado.getInt(1);
        }
    }

    protected void desfazerTransacao(Connection conexao, SQLException erroOriginal) {
        try {
            conexao.rollback();
        } catch (SQLException erroRollback) {
            erroOriginal.addSuppressed(erroRollback);
        }
    }

    /**
     * Converte a linha atual do ResultSet na entidade correspondente (polimorfismo).
     */
    protected abstract T mapear(ResultSet resultado) throws SQLException;
}
