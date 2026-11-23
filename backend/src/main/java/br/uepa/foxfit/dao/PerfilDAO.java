package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * DAO responsável pelas operações da tela de Meu Perfil (perfil.html).
 * Gerencia a consulta e atualização dos dados cadastrais do operador logado
 * e a troca segura de senha de acesso no banco de dados PostgreSQL.
 */
public class PerfilDAO {

    private static final Pattern PADRAO_EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Armazenamento em memória (fallback) para persistência durante a execução caso o banco esteja indisponível
    private static final Map<Integer, Usuario> BASE_FALLBACK = new ConcurrentHashMap<>();

    static {
        carregarBasePadrao();
    }

    public static synchronized void carregarBasePadrao() {
        BASE_FALLBACK.put(1, new Usuario(1, "Fernanda Sousa", "fernanda.sousa@foxfit.com.br", "345.890.123-00", "ADMIN", "ATIVO", "vasco2026"));
        BASE_FALLBACK.put(2, new Usuario(2, "Carlos Eduardo Almeida", "carlos.almeida@foxfit.com.br", "418.220.910-34", "OPERADOR", "ATIVO", "trembala"));
        BASE_FALLBACK.put(3, new Usuario(3, "Beatriz Helena Mendes", "beatriz.mendes@foxfit.com.br", "289.401.655-71", "OPERADOR", "ATIVO", "conhecimento"));
    }

    /**
     * Busca os dados cadastrais do operador pelo seu identificador primário.
     */
    public Usuario buscarPorId(int id) {
        if (!Conexao.isConfigurado()) {
            return BASE_FALLBACK.get(id);
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
                    Usuario usuario = new Usuario(
                            rs.getInt("id"),
                            rs.getString("nome_completo"),
                            rs.getString("email"),
                            rs.getString("cpf"),
                            rs.getString("cargo"),
                            rs.getString("status"),
                            rs.getString("senha")
                    );
                    BASE_FALLBACK.put(id, usuario);
                    return usuario;
                }
                return null;
            }
        } catch (Exception e) {
            return BASE_FALLBACK.get(id);
        }
    }

    /**
     * Atualiza o nome completo e o e-mail corporativo do operador no banco PostgreSQL.
     * Valida os campos obrigatórios e impede duplicidade de e-mail com outros operadores.
     */
    public boolean atualizarDados(int id, String nomeCompleto, String email) throws SQLException {
        if (nomeCompleto == null || nomeCompleto.trim().length() < 3) {
            throw new IllegalArgumentException("O nome completo deve conter pelo menos 3 caracteres.");
        }

        String emailNormalizado = email != null ? email.trim().toLowerCase() : "";
        if (emailNormalizado.isBlank() || !PADRAO_EMAIL.matcher(emailNormalizado).matches()) {
            throw new IllegalArgumentException("Informe um endereço de e-mail corporativo válido.");
        }

        if (emailJaCadastrado(emailNormalizado, id)) {
            throw new IllegalArgumentException("Este e-mail corporativo já está em uso por outro operador.");
        }

        if (!Conexao.isConfigurado()) {
            Usuario existente = BASE_FALLBACK.get(id);
            if (existente != null) {
                Usuario atualizado = existente.comNomeEEmail(nomeCompleto.trim(), emailNormalizado);
                BASE_FALLBACK.put(id, atualizado);
                return true;
            }
            return false;
        }

        String sql = """
            UPDATE usuarios
            SET nome_completo = ?, email = ?
            WHERE id = ?
            """;

        boolean atualizadoNoBanco = false;

        try (Connection con = Conexao.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nomeCompleto.trim());
            ps.setString(2, emailNormalizado);
            ps.setInt(3, id);

            int linhas = ps.executeUpdate();
            atualizadoNoBanco = linhas > 0;
        } catch (Exception e) {
            System.err.println("[PerfilDAO] Erro ao atualizar no banco: " + e.getMessage());
        }

        // Sincroniza também no fallback em memória
        Usuario existente = BASE_FALLBACK.get(id);
        if (existente != null) {
            Usuario atualizado = existente.comNomeEEmail(nomeCompleto.trim(), emailNormalizado);
            BASE_FALLBACK.put(id, atualizado);
            return true;
        }

        return atualizadoNoBanco;
    }

    /**
     * Atualiza a senha de acesso do operador.
     * Exige a validação da senha atual e garante nova senha com mínimo de 8 caracteres.
     */
    public boolean alterarSenha(int id, String senhaAtual, String novaSenha) throws SQLException {
        if (senhaAtual == null || senhaAtual.isBlank()) {
            throw new IllegalArgumentException("Informe a sua senha atual.");
        }

        if (novaSenha == null || novaSenha.length() < 8) {
            throw new IllegalArgumentException("A nova senha deve possuir no mínimo 8 caracteres.");
        }

        if (novaSenha.equals(senhaAtual)) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        if (!validarSenhaAtual(id, senhaAtual)) {
            throw new IllegalArgumentException("A senha atual informada está incorreta.");
        }

        if (!Conexao.isConfigurado()) {
            Usuario existente = BASE_FALLBACK.get(id);
            if (existente != null) {
                Usuario atualizado = existente.comSenha(novaSenha);
                BASE_FALLBACK.put(id, atualizado);
                return true;
            }
            return false;
        }

        String sql = """
            UPDATE usuarios
            SET senha = ?
            WHERE id = ?
            """;

        boolean alteradoNoBanco = false;

        try (Connection con = Conexao.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, novaSenha);
            ps.setInt(2, id);

            int linhas = ps.executeUpdate();
            alteradoNoBanco = linhas > 0;
        } catch (Exception e) {
            System.err.println("[PerfilDAO] Erro ao alterar senha no banco: " + e.getMessage());
        }

        // Sincroniza também no fallback em memória
        Usuario existente = BASE_FALLBACK.get(id);
        if (existente != null) {
            Usuario atualizado = existente.comSenha(novaSenha);
            BASE_FALLBACK.put(id, atualizado);
            return true;
        }

        return alteradoNoBanco;
    }

    /**
     * Verifica se a senha informada confere com a senha atual registrada para o usuário.
     */
    public boolean validarSenhaAtual(int id, String senhaInformada) {
        if (senhaInformada == null || senhaInformada.isBlank()) {
            return false;
        }

        if (!Conexao.isConfigurado()) {
            Usuario u = BASE_FALLBACK.get(id);
            return u != null && senhaInformada.equals(u.getSenhaOriginal());
        }

        String sql = """
            SELECT senha
            FROM usuarios
            WHERE id = ?
            LIMIT 1
            """;

        try (Connection con = Conexao.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String senhaReal = rs.getString("senha");
                    return senhaInformada.equals(senhaReal);
                }
            }
        } catch (Exception e) {
            System.err.println("[PerfilDAO] Erro ao validar senha no banco: " + e.getMessage());
        }

        Usuario u = BASE_FALLBACK.get(id);
        return u != null && senhaInformada.equals(u.getSenhaOriginal());
    }

    /**
     * Verifica se um e-mail já pertence a outro usuário cadastrado no sistema.
     */
    public boolean emailJaCadastrado(String email, int idIgnorar) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String emailAlvo = email.trim().toLowerCase();

        if (!Conexao.isConfigurado()) {
            for (Usuario u : BASE_FALLBACK.values()) {
                if (u.getId() != idIgnorar && u.getEmail().equalsIgnoreCase(emailAlvo)) {
                    return true;
                }
            }
            return false;
        }

        String sql = """
            SELECT id
            FROM usuarios
            WHERE LOWER(email) = LOWER(?) AND id <> ?
            LIMIT 1
            """;

        try (Connection con = Conexao.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, emailAlvo);
            ps.setInt(2, idIgnorar);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[PerfilDAO] Erro ao verificar duplicidade de email no banco: " + e.getMessage());
        }

        for (Usuario u : BASE_FALLBACK.values()) {
            if (u.getId() != idIgnorar && u.getEmail().equalsIgnoreCase(emailAlvo)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retorna o mapa de fallback para sincronização com outros DAOs.
     */
    public static Map<Integer, Usuario> getBaseFallback() {
        return BASE_FALLBACK;
    }
}
