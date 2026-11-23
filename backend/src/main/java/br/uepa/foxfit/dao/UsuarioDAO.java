package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UsuarioDAO {

    private final PerfilDAO perfilDAO = new PerfilDAO();

    public Usuario autenticar(String email, String senha) {
        if (email == null || senha == null || email.isBlank() || senha.isBlank()) {
            return null;
        }

        if (!Conexao.isConfigurado()) {
            return autenticarFallback(email, senha);
        }

        String sql = """
            SELECT id, nome_completo, email, cpf, cargo, status, senha
            FROM usuarios
            WHERE LOWER(email) = LOWER(?) AND senha = ? AND status = 'ATIVO'
            LIMIT 1
            """;

        try (Connection con = Conexao.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            ps.setString(2, senha);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario(
                            rs.getInt("id"),
                            rs.getString("nome_completo"),
                            rs.getString("email"),
                            rs.getString("cpf"),
                            rs.getString("cargo"),
                            rs.getString("status"),
                            rs.getString("senha")
                    );
                    PerfilDAO.getBaseFallback().put(u.getId(), u);
                    return u;
                }
                return null;
            }
        } catch (Exception e) {
            return autenticarFallback(email, senha);
        }
    }

    public Usuario buscarPorId(int id) {
        if (!Conexao.isConfigurado()) {
            return buscarPorIdFallback(id);
        }

        String sql = """
            SELECT id, nome_completo, email, cpf, cargo, status, senha
            FROM usuarios
            WHERE id = ?
            LIMIT 1
            """;

        try (Connection con = Conexao.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario(
                            rs.getInt("id"),
                            rs.getString("nome_completo"),
                            rs.getString("email"),
                            rs.getString("cpf"),
                            rs.getString("cargo"),
                            rs.getString("status"),
                            rs.getString("senha")
                    );
                    PerfilDAO.getBaseFallback().put(u.getId(), u);
                    return u;
                }
                return null;
            }
        } catch (Exception e) {
            return buscarPorIdFallback(id);
        }
    }

    /**
     * Atualiza dados de perfil do usuário (nome completo e e-mail).
     */
    public boolean atualizarPerfil(int id, String nomeCompleto, String email) throws SQLException {
        return perfilDAO.atualizarDados(id, nomeCompleto, email);
    }

    /**
     * Altera a senha do usuário após validação de regras e senha atual.
     */
    public boolean alterarSenha(int id, String senhaAtual, String novaSenha) throws SQLException {
        return perfilDAO.alterarSenha(id, senhaAtual, novaSenha);
    }

    /**
     * Verifica se o e-mail informado já está em uso por outro usuário.
     */
    public boolean emailJaCadastrado(String email, int idIgnorar) {
        return perfilDAO.emailJaCadastrado(email, idIgnorar);
    }

    /**
     * Valida se a senha atual confere.
     */
    public boolean validarSenha(int id, String senha) {
        return perfilDAO.validarSenhaAtual(id, senha);
    }

    private Usuario autenticarFallback(String email, String senha) {
        for (Usuario u : PerfilDAO.getBaseFallback().values()) {
            if (u.getEmail().equalsIgnoreCase(email.trim())
                    && u.getSenhaOriginal().equals(senha)
                    && "ATIVO".equalsIgnoreCase(u.getStatus())) {
                return u;
            }
        }
        return null;
    }

    private Usuario buscarPorIdFallback(int id) {
        return PerfilDAO.getBaseFallback().get(id);
    }

    public static List<Usuario> getUsuariosBase() {
        return List.copyOf(PerfilDAO.getBaseFallback().values());
    }
}
