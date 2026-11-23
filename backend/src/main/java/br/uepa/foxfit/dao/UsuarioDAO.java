package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsuarioDAO {

    private static final List<Usuario> USUARIOS_BASE = new CopyOnWriteArrayList<>(List.of(
            new Usuario(1, "Fernanda Sousa", "fernanda.sousa@foxfit.com.br", "345.890.123-00", "ADMIN", "ATIVO", "vasco2026"),
            new Usuario(2, "Carlos Eduardo Almeida", "carlos.almeida@foxfit.com.br", "418.220.910-34", "OPERADOR", "ATIVO", "trembala"),
            new Usuario(3, "Beatriz Helena Mendes", "beatriz.mendes@foxfit.com.br", "289.401.655-71", "OPERADOR", "ATIVO", "conhecimento")
    ));

    public Usuario autenticar(String email, String senha) {
        if (email == null || senha == null || email.isBlank() || senha.isBlank()) {
            return null;
        }

        String sql = """
            SELECT id, nome_completo, email, cpf, cargo, senha, status
            FROM usuarios
            WHERE LOWER(email) = LOWER(?) AND senha = ? AND status = 'ATIVO'
            LIMIT 1
            """;

        try (Connection con = Conexao.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

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
                    rs.getString("senha")
                
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
            SELECT id, nome_completo, email, cpf, cargo, senha, status
            FROM usuarios
            WHERE id = ?
            LIMIT 1
            """;

        try (Connection con = Conexao.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

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
                    rs.getString("senha")
                
                );
                }
                return null;
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar usuário por id no banco: " + e.getMessage());
            return buscarPorIdFallback(id);
        }
    }

    public boolean atualizarSenha(int id, String novaSenha) {
        if (novaSenha == null || novaSenha.isBlank()) {
            return false;
        }

        // 1. Atualiza no PostgreSQL se a conexão estiver disponível
        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";
        try (Connection con = Conexao.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, novaSenha);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Aviso: Conexão direta com o banco offline ao atualizar senha: " + e.getMessage());
        }

        // 2. Atualiza na lista de fallback em memória
        Usuario usuarioAlvo = null;
        for (int i = 0; i < USUARIOS_BASE.size(); i++) {
            Usuario u = USUARIOS_BASE.get(i);
            if (u.getId() == id) {
                Usuario atualizado = new Usuario(
                        u.getId(),
                        u.getNomeCompleto(),
                        u.getEmail(),
                        u.getCpf(),
                        u.getCargo(),
                        u.getStatus(),
                        novaSenha
                );
                USUARIOS_BASE.set(i, atualizado);
                usuarioAlvo = atualizado;
                break;
            }
        }

        // 3. Atualiza nos registros do arquivo database/init.sql
        if (usuarioAlvo != null) {
            atualizarInitSql(usuarioAlvo.getEmail(), novaSenha);
        }

        return true;
    }

    private void atualizarInitSql(String email, String novaSenha) {
        try {
            Path pathInit = Path.of("database", "init.sql");
            if (Files.isRegularFile(pathInit)) {
                String conteudo = Files.readString(pathInit, StandardCharsets.UTF_8);
                // Procura a linha que contém o email do usuário e substitui a senha entre aspas simples
                // Exemplo: ('Fernanda Sousa', 'fernanda.sousa@foxfit.com.br', '345.890.123-00', 'ADMIN', 'vasco2026', 'ATIVO')
                String regex = "('" + Pattern.quote(email) + "',\\s*'[^']+',\\s*'[^']+',\\s*')([^']+)(',\\s*'[^']+'\\))";
                String substituido = conteudo.replaceAll(regex, "$1" + Matcher.quoteReplacement(novaSenha) + "$3");
                if (!substituido.equals(conteudo)) {
                    Files.writeString(pathInit, substituido, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao atualizar init.sql: " + e.getMessage());
        }
    }

    private Usuario autenticarFallback(String email, String senha) {
            for (Usuario u : USUARIOS_BASE) {
                if (u.getEmail().equalsIgnoreCase(email.trim())
                        && u.getSenhaOriginal().equals(senha)
                        && "ATIVO".equalsIgnoreCase(u.getStatus())) {
                    return u;
                }
            }
            return null;
        }

    

    private Usuario buscarPorIdFallback(int id) {
            for (Usuario u : USUARIOS_BASE) {
                if (u.getId() == id) {
                    return u;
                }
            }
            return null;
        }

    

    public static List<Usuario> getUsuariosBase() {
        return USUARIOS_BASE;
    }
}
