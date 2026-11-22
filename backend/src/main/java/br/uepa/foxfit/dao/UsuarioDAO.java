package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class UsuarioDAO {

    public Usuario autenticar(String email, String senha) {
        if (email == null || senha == null || email.isBlank() || senha.isBlank()) {
            return null;
        }

        String sql = """
            SELECT id, nome_completo, email, cpf, cargo, status
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
                    return new Usuario(
                            rs.getInt("id"),
                            rs.getString("nome_completo"),
                            rs.getString("email"),
                            rs.getString("cpf"),
                            rs.getString("cargo"),
                            rs.getString("status"),
                            null
                    );
                }
                return null;
            }
        } catch (Exception e) {
            System.err.println("Erro ao autenticar no banco: " + e.getMessage());
            return autenticarFallback(email, senha);
        }
    }

    public Usuario buscarPorId(int id) {
        String sql = """
            SELECT id, nome_completo, email, cpf, cargo, status
            FROM usuarios
            WHERE id = ?
            LIMIT 1
            """;

        try (Connection con = Conexao.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("id"),
                            rs.getString("nome_completo"),
                            rs.getString("email"),
                            rs.getString("cpf"),
                            rs.getString("cargo"),
                            rs.getString("status"),
                            null
                    );
                }
                return null;
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar usuário por id no banco: " + e.getMessage());
            return buscarPorIdFallback(id);
        }
    }

    private Usuario autenticarFallback(String email, String senha) {
        for (Usuario u : getUsuariosBase()) {
            if (u.getEmail().equalsIgnoreCase(email.trim())
                    && u.getSenhaOriginal().equals(senha)
                    && "ATIVO".equalsIgnoreCase(u.getStatus())) {
                return u;
            }
        }
        return null;
    }

    private Usuario buscarPorIdFallback(int id) {
        for (Usuario u : getUsuariosBase()) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    private static List<Usuario> getUsuariosBase() {
        return List.of(
                new Usuario(1, "Fernanda Sousa", "fernanda.sousa@foxfit.com.br", "345.890.123-00", "ADMIN", "ATIVO", "vasco2026"),
                new Usuario(2, "Carlos Eduardo Almeida", "carlos.almeida@foxfit.com.br", "418.220.910-34", "OPERADOR", "ATIVO", "trembala"),
                new Usuario(3, "Beatriz Helena Mendes", "beatriz.mendes@foxfit.com.br", "289.401.655-71", "OPERADOR", "ATIVO", "conhecimento")
        );
    }
}
