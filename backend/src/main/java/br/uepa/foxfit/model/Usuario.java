package br.uepa.foxfit.model;

/**
 * Representa um usuário autenticado do sistema (Administrador ou Operador).
 * Herda de Pessoa e aplica encapsulamento de credenciais e controle de papel (RBAC).
 */
public class Usuario extends Pessoa {

    private String email;
    private String cargo;
    private String senhaOriginal;

    public Usuario(String nomeCompleto, String email, String cpf, String cargo, String senhaOriginal) {
        this(0, nomeCompleto, email, cpf, cargo, "ATIVO", senhaOriginal);
    }

    public Usuario(int id, String nomeCompleto, String email, String cargo, String status) {
        this(id, nomeCompleto, email, "", cargo, status, null);
    }

    public Usuario(int id, String nomeCompleto, String email, String cargo, String status, String senhaOriginal) {
        this(id, nomeCompleto, email, "", cargo, status, senhaOriginal);
    }

    public Usuario(int id, String nomeCompleto, String email, String cpf,
                   String cargo, String status, String senhaOriginal) {
        super(id, nomeCompleto, cpf, status);
        this.email = email != null ? email.trim() : "";
        this.cargo = cargo != null ? cargo.trim() : "OPERADOR";
        this.senhaOriginal = senhaOriginal;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = validarTextoObrigatorio(email, "email");
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        String valor = validarTextoObrigatorio(cargo, "cargo").toUpperCase();
        if (!"ADMIN".equals(valor) && !"OPERADOR".equals(valor)) {
            throw new IllegalArgumentException("Cargo inválido. Use ADMIN ou OPERADOR.");
        }
        this.cargo = valor;
    }

    public String getSenhaOriginal() {
        return senhaOriginal;
    }

    public String getSenha() {
        return senhaOriginal;
    }

    public void setSenha(String senha) {
        this.senhaOriginal = senha;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(cargo);
    }

    public Usuario comNomeEEmail(String novoNome, String novoEmail) {
        return new Usuario(getId(), novoNome, novoEmail, getCpf(), this.cargo, getStatus(), this.senhaOriginal);
    }

    public Usuario comSenha(String novaSenha) {
        return new Usuario(getId(), getNomeCompleto(), this.email, getCpf(), this.cargo, getStatus(), novaSenha);
    }

    public String getCargoFormatado() {
        if (cargo == null) {
            return "Operador";
        }
        if ("ADMIN".equalsIgnoreCase(cargo)) {
            return "Administrador Geral";
        }
        if ("OPERADOR".equalsIgnoreCase(cargo)) {
            return "Operador";
        }
        return cargo;
    }

    @Override
    public String getPapel() {
        return getCargoFormatado();
    }

    @Override
    public String getIdentificadorExibicao() {
        return String.format("#USR-%03d", getId() != null ? getId() : 0);
    }

    @Override
    public String getResumo() {
        return getIdentificadorExibicao() + " | " + getPapel() + ": " + getNomeCompleto()
                + " <" + email + ">";
    }
}
