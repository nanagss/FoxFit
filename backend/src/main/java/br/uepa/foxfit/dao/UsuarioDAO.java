package br.uepa.foxfit.dao;

import br.uepa.foxfit.database.Conexao;
import br.uepa.foxfit.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * DAO único responsável pela tabela "usuarios": autenticação, gestão completa (CRUD),
 * atualização de perfil e alteração segura de senha.
 * Estende BaseDAO e implementa RepositorioCRUD.
 */
public class UsuarioDAO extends BaseDAO<Usuario> implements RepositorioCRUD<Usuario> {

    private static final Pattern PADRAO_EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Map<Integer, Usuario> BASE_FALLBACK = new ConcurrentHashMap<>();

    static {
        carregarBasePadrao();
    }

    public static synchronized void carregarBasePadrao() {
        BASE_FALLBACK.clear();
        BASE_FALLBACK.put(1, new Usuario(1, "Fernanda Sousa", "fernanda.sousa@foxfit.com.br", "345.890.123-00", "ADMIN", "ATIVO", "fernanda26"));
        BASE_FALLBACK.put(2, new Usuario(2, "Aryel Jesus", "aryel.jesus@foxfit.com.br", "418.220.910-34", "OPERADOR", "ATIVO", "trembala"));
        BASE_FALLBACK.put(3, new Usuario(3, "Beatriz Helena Mendes", "beatriz.mendes@foxfit.com.br", "289.401.655-71", "OPERADOR", "ATIVO", "conhecimento"));
    }

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

        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            ps.setString(2, senha);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = mapear(rs);
                    BASE_FALLBACK.put(u.getId(), u);
                    return u;
                }
                return null;
            }
        } catch (Exception e) {
            return autenticarFallback(email, senha);
        }
    }

    @Override
    public int cadastrar(Usuario usuario) throws SQLException {
        if (usuario.getSenha() == null || usuario.getSenha().length() < 8) {
            throw new IllegalArgumentException("A senha inicial deve possuir no mínimo 8 caracteres.");
        }

        if (!Conexao.isConfigurado()) {
            int proximoId = BASE_FALLBACK.keySet().stream()
                    .max(Integer::compareTo).orElse(0) + 1;
            Usuario salvo = new Usuario(
                    proximoId,
                    usuario.getNomeCompleto(),
                    usuario.getEmail(),
                    usuario.getCpf(),
                    usuario.getCargo(),
                    usuario.getStatus(),
                    usuario.getSenha()
            );
            BASE_FALLBACK.put(proximoId, salvo);
            usuario.setId(proximoId);
            return proximoId;
        }

        String sql = """
                INSERT INTO usuarios
                    (nome_completo, email, cpf, cargo, senha, status)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario.getNomeCompleto());
            ps.setString(2, usuario.getEmail().toLowerCase());
            ps.setString(3, usuario.getCpf());
            ps.setString(4, usuario.getCargo().toUpperCase());
            ps.setString(5, usuario.getSenha());
            ps.setString(6, usuario.getStatus().toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    usuario.setId(id);
                    BASE_FALLBACK.put(id, usuario);
                    return id;
                }
                throw new SQLException("Usuário não foi cadastrado.");
            }
        }
    }

    @Override
    public List<Usuario> listar() throws SQLException {
        if (!Conexao.isConfigurado()) {
            List<Usuario> fallback = new ArrayList<>(BASE_FALLBACK.values());
            fallback.sort(Comparator.comparingInt(Usuario::getId));
            return fallback;
        }

        List<Usuario> usuarios = new ArrayList<>();
        String sql = """
                SELECT id, nome_completo, email, cpf, cargo, status, senha
                FROM usuarios
                ORDER BY id ASC
                """;

        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Usuario u = mapear(rs);
                BASE_FALLBACK.put(u.getId(), u);
                usuarios.add(u);
            }
        }

        return usuarios;
    }

    @Override
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

        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = mapear(rs);
                    BASE_FALLBACK.put(u.getId(), u);
                    return u;
                }
                return null;
            }
        } catch (Exception e) {
            return BASE_FALLBACK.get(id);
        }
    }

    @Override
    public boolean atualizar(Usuario usuario) throws SQLException {
        boolean atualizarSenha = usuario.getSenha() != null && !usuario.getSenha().isBlank();
        if (atualizarSenha && usuario.getSenha().length() < 8) {
            throw new IllegalArgumentException("A nova senha deve possuir no mínimo 8 caracteres.");
        }

        if (!Conexao.isConfigurado()) {
            Usuario existente = BASE_FALLBACK.get(usuario.getId());
            if (existente == null) {
                return false;
            }
            String senhaFinal = atualizarSenha ? usuario.getSenha() : existente.getSenhaOriginal();
            Usuario atualizado = new Usuario(
                    usuario.getId(),
                    usuario.getNomeCompleto(),
                    usuario.getEmail(),
                    usuario.getCpf(),
                    usuario.getCargo(),
                    usuario.getStatus(),
                    senhaFinal
            );
            BASE_FALLBACK.put(usuario.getId(), atualizado);
            return true;
        }

        String sql = atualizarSenha
                ? """
                  UPDATE usuarios
                  SET nome_completo = ?, email = ?, cpf = ?, cargo = ?, status = ?, senha = ?
                  WHERE id = ?
                  """
                : """
                  UPDATE usuarios
                  SET nome_completo = ?, email = ?, cpf = ?, cargo = ?, status = ?
                  WHERE id = ?
                  """;

        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario.getNomeCompleto());
            ps.setString(2, usuario.getEmail().toLowerCase());
            ps.setString(3, usuario.getCpf());
            ps.setString(4, usuario.getCargo().toUpperCase());
            ps.setString(5, usuario.getStatus().toUpperCase());

            if (atualizarSenha) {
                ps.setString(6, usuario.getSenha());
                ps.setInt(7, usuario.getId());
            } else {
                ps.setInt(6, usuario.getId());
            }

            boolean ok = ps.executeUpdate() == 1;
            if (ok) {
                BASE_FALLBACK.put(usuario.getId(), usuario);
            }
            return ok;
        }
    }

    @Override
    public boolean excluir(int id) throws SQLException {
        if (!Conexao.isConfigurado()) {
            return BASE_FALLBACK.remove(id) != null;
        }

        String sql = "DELETE FROM usuarios WHERE id = ?";

        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            boolean excluido = ps.executeUpdate() == 1;
            if (excluido) {
                BASE_FALLBACK.remove(id);
            }
            return excluido;
        }
    }

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
                BASE_FALLBACK.put(id, existente.comNomeEEmail(nomeCompleto.trim(), emailNormalizado));
                return true;
            }
            return false;
        }

        String sql = "UPDATE usuarios SET nome_completo = ?, email = ? WHERE id = ?";
        boolean atualizadoNoBanco = false;

        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nomeCompleto.trim());
            ps.setString(2, emailNormalizado);
            ps.setInt(3, id);
            atualizadoNoBanco = ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UsuarioDAO] Erro ao atualizar perfil no banco: " + e.getMessage());
        }

        Usuario existente = BASE_FALLBACK.get(id);
        if (existente != null) {
            BASE_FALLBACK.put(id, existente.comNomeEEmail(nomeCompleto.trim(), emailNormalizado));
            return true;
        }

        return atualizadoNoBanco;
    }

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
                BASE_FALLBACK.put(id, existente.comSenha(novaSenha));
                return true;
            }
            return false;
        }

        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";
        boolean alteradoNoBanco = false;

        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, novaSenha);
            ps.setInt(2, id);
            alteradoNoBanco = ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UsuarioDAO] Erro ao alterar senha no banco: " + e.getMessage());
        }

        Usuario existente = BASE_FALLBACK.get(id);
        if (existente != null) {
            BASE_FALLBACK.put(id, existente.comSenha(novaSenha));
            return true;
        }

        return alteradoNoBanco;
    }

    public boolean validarSenhaAtual(int id, String senhaInformada) {
        if (senhaInformada == null || senhaInformada.isBlank()) {
            return false;
        }

        if (!Conexao.isConfigurado()) {
            Usuario u = BASE_FALLBACK.get(id);
            return u != null && senhaInformada.equals(u.getSenhaOriginal());
        }

        String sql = "SELECT senha FROM usuarios WHERE id = ? LIMIT 1";
        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return senhaInformada.equals(rs.getString("senha"));
                }
            }
        } catch (Exception e) {
            System.err.println("[UsuarioDAO] Erro ao validar senha no banco: " + e.getMessage());
        }

        Usuario u = BASE_FALLBACK.get(id);
        return u != null && senhaInformada.equals(u.getSenhaOriginal());
    }

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

        String sql = "SELECT id FROM usuarios WHERE LOWER(email) = LOWER(?) AND id <> ? LIMIT 1";
        try (Connection con = abrirConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, emailAlvo);
            ps.setInt(2, idIgnorar);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[UsuarioDAO] Erro ao verificar e-mail no banco: " + e.getMessage());
        }

        for (Usuario u : BASE_FALLBACK.values()) {
            if (u.getId() != idIgnorar && u.getEmail().equalsIgnoreCase(emailAlvo)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected Usuario mapear(ResultSet rs) throws SQLException {
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

    private Usuario autenticarFallback(String email, String senha) {
        for (Usuario u : BASE_FALLBACK.values()) {
            if (u.getEmail().equalsIgnoreCase(email.trim())
                    && u.getSenhaOriginal().equals(senha)
                    && "ATIVO".equalsIgnoreCase(u.getStatus())) {
                return u;
            }
        }
        return null;
    }

    public static List<Usuario> getUsuariosBase() {
        return List.copyOf(BASE_FALLBACK.values());
    }
}
